package com.example.rpgsombrio.data

// Poderes e exceções do mestre (0.10.5, etapa 6). Kotlin puro, sem Android, para poder ser testado na JVM.
//
// Quatro ferramentas:
//   • edição livre – o mestre ignora as travas (atributos finalizados e nível) em qualquer ficha da campanha. O nível
//     é definido por [freeSetLevel], com confirmação na tela, para não desfazer níveis a cada tecla digitada;
//   • aumento de atributo extra – cobre exceções de classe (Guerreiro nos níveis 6 e 14, Ladino no 10), já que a
//     ficha não tem classe estruturada. Funciona como o level up: o mestre concede, o jogador distribui;
//   • reabrir a criação de uma ficha finalizada (a regra em si está em CreationRules.kt);
//   • visibilidade – o jogador vê só as próprias fichas, o mestre vê todas; Bestiário, encontros e combate são
//     ferramentas do mestre.
// Como a trava é de interface (decisão 10 do plano), nada aqui impede alguém de mexer no JSON: a revalidação
// pelo celular do mestre chega na etapa 7.

/** O usuário é o mestre desta campanha? Sem campanha (ficha solta) não há mestre. */
fun isCampaignMaster(campaign: Campaign?, userId: String): Boolean = campaign != null && campaign.masterUserId == userId

/** Pode ligar a edição livre? Só o mestre da campanha, em fichas que estão nela. */
fun canUseFreeEdit(campaign: Campaign?, userId: String): Boolean = isCampaignMaster(campaign, userId)

/** Os atributos estão travados para quem edita? A edição livre do mestre ignora a trava de criação. */
fun attributesLockedFor(sheet: CharacterSheet, rule: CreationRule, freeEdit: Boolean): Boolean =
    attributesLocked(sheet, rule) && !freeEdit

/**
 * Edição livre do nível: o mestre digita o nível e a ficha acompanha. Para baixo, desfaz exatamente o que cada nível
 * removido concedeu ([removeLevel], inclusive vida e pontos de atributo já gastos). Para cima, só sobe o nível e a
 * proficiência: o que o mestre quer conceder de vida e atributos ele faz pelo "Subir nível" ou pelo aumento extra.
 * O resultado fica entre 1 e o nível máximo do sistema.
 */
fun freeSetLevel(sheet: CharacterSheet, rules: LevelRules, level: Int, autoProficiency: Boolean = true): CharacterSheet {
    val target = level.coerceIn(1, rules.maxLevel)
    var next = sheet
    if (target < next.level) {
        while (next.level > target) next = removeLevel(next, rules, autoProficiency) ?: break
        return next
    }
    if (target == next.level) return next
    return next.copy(
        level = target,
        proficiencyBonus = if (autoProficiency) proficiencyForLevel(target) else next.proficiencyBonus
    )
}

// --- aumento de atributo extra ---

/** Máximo de pontos numa única concessão (o normal do D&D é 2; exceções raramente passam disso). */
const val MAX_ASI_GRANT = 8

/**
 * Uma concessão extra de pontos de atributo, dada pelo mestre fora dos níveis 4, 8, 12, 16 e 19.
 * [applied] guarda o que REALMENTE foi somado em cada atributo (já respeitando o teto), para o desfazer ser exato.
 */
data class AsiGrant(
    val id: String,
    val points: Int,
    val reason: String = "",
    val applied: Map<String, Int> = emptyMap(),
    val done: Boolean = false
)

/** Concessões que o jogador ainda não distribuiu. */
fun pendingAsiGrants(sheet: CharacterSheet): List<AsiGrant> = sheet.asiGrants.filter { !it.done }

/** Motivo pelo qual não dá para conceder aumento a esta ficha agora, ou nulo se dá. */
fun asiGrantBlockReason(sheet: CharacterSheet, rules: LevelRules?): String? = when {
    rules == null -> "este sistema não tem regra de atributos: a ficha é livre"
    sheet.creation == CreationState.DRAFT -> "a criação ainda não foi finalizada"
    else -> null
}

/** Ação do mestre: concede [points] pontos de atributo extras. O jogador distribui no assistente. Nulo se não dá. */
fun grantExtraAsi(sheet: CharacterSheet, rules: LevelRules?, points: Int, reason: String, id: String): CharacterSheet? {
    if (asiGrantBlockReason(sheet, rules) != null) return null
    if (points !in 1..MAX_ASI_GRANT) return null
    if (sheet.asiGrants.any { it.id == id }) return null
    return sheet.copy(asiGrants = sheet.asiGrants + AsiGrant(id = id, points = points, reason = reason.trim()))
}

/**
 * Jogador distribui uma concessão extra. Mesmas regras do level up: precisa gastar todos os pontos, a menos que não
 * caiba (teto de [LevelRules.attributeCap] em cada atributo), e nenhum atributo passa do teto.
 */
fun completeAsiGrant(sheet: CharacterSheet, rules: LevelRules, grantId: String, asi: Map<String, Int>): LevelOutcome {
    val grant = sheet.asiGrants.firstOrNull { it.id == grantId && !it.done }
        ?: return LevelOutcome(null, "Não há aumento pendente.")
    val spend = asi.filterValues { it != 0 }
    if (spend.any { it.value < 0 }) return LevelOutcome(null, "Os pontos de atributo não podem ser negativos.")
    val spent = spend.values.sum()
    if (spent > grant.points) return LevelOutcome(null, "Você tem ${grant.points} pontos de atributo nesta concessão.")
    spend.forEach { (key, add) ->
        if (key !in rules.attributeKeys) return LevelOutcome(null, "Atributo desconhecido.")
        if ((sheet.attributes[key] ?: 10) + add > rules.attributeCap) return LevelOutcome(null, "Nenhum atributo passa de ${rules.attributeCap}.")
    }
    val room = rules.attributeKeys.sumOf { maxOf(0, rules.attributeCap - (sheet.attributes[it] ?: 10)) }
    if (spent < minOf(grant.points, room)) return LevelOutcome(null, "Distribua os ${minOf(grant.points, room)} pontos de atributo.")

    val done = grant.copy(applied = spend, done = true)
    return LevelOutcome(
        sheet.copy(
            attributes = sheet.attributes + spend.mapValues { (k, add) -> (sheet.attributes[k] ?: 0) + add },
            asiGrants = sheet.asiGrants.map { if (it.id == grantId) done else it }
        )
    )
}

/**
 * Ação do mestre: retira uma concessão. Se o jogador já distribuiu, desfaz exatamente o que foi somado em cada
 * atributo. Nulo se a concessão não existe.
 */
fun revokeAsiGrant(sheet: CharacterSheet, grantId: String): CharacterSheet? {
    val grant = sheet.asiGrants.firstOrNull { it.id == grantId } ?: return null
    var next = sheet.copy(asiGrants = sheet.asiGrants.filter { it.id != grantId })
    if (grant.done) {
        next = next.copy(attributes = next.attributes + grant.applied.mapValues { (k, add) -> ((next.attributes[k] ?: 0) - add).coerceAtLeast(1) })
    }
    return next
}

/** Resumo de uma concessão para a ficha (ex.: "+2 pontos (Guerreiro nível 6): +1 FOR, +1 CON"). */
fun asiGrantSummary(grant: AsiGrant, labels: Map<String, String> = emptyMap()): String {
    val base = "+${grant.points} pontos" + if (grant.reason.isBlank()) "" else " (${grant.reason})"
    if (!grant.done) return "$base: aguardando o jogador"
    return base + ": " + grant.applied.entries.joinToString(", ") { (k, v) -> "+$v ${labels[k] ?: k}" }
}

// --- visibilidade e ferramentas de mestre ---

/**
 * O usuário pode ver esta ficha? Ficha solta: sim (é do aparelho). Em campanha: o mestre vê todas; o jogador vê as
 * próprias. Ficha sem dono gravado (criada antes da 0.10.5) vale como do aparelho que a tem.
 */
fun canViewSheet(sheet: CharacterSheet, campaign: Campaign?, userId: String): Boolean =
    campaign == null || campaign.masterUserId == userId || sheet.ownerUserId.isBlank() || sheet.ownerUserId == userId

/** As fichas que o usuário pode ver, cada uma avaliada na campanha a que pertence. */
fun visibleSheets(sheets: List<CharacterSheet>, campaigns: List<Campaign>, userId: String): List<CharacterSheet> {
    val byId = campaigns.associateBy { it.id }
    return sheets.filter { canViewSheet(it, byId[it.campaignId], userId) }
}

/**
 * Bestiário, encontros e combate são ferramentas do mestre. Sem campanha ativa o aparelho é do próprio usuário e
 * nada é escondido; numa campanha em que ele é jogador, ficam fechadas.
 */
fun masterToolsAvailable(activeCampaign: Campaign?, userId: String): Boolean =
    activeCampaign == null || activeCampaign.masterUserId == userId
