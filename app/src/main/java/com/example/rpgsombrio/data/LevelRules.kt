package com.example.rpgsombrio.data

// Nível controlado pelo mestre (0.10.4, etapa 5). Kotlin puro, sem Android, para poder ser testado na JVM.
//
// Fluxo: o MESTRE sobe o nível ([grantLevelUp]) e a ficha fica com um level up pendente. O JOGADOR conclui no
// assistente ([completeLevelUp]): vida pelo método da campanha, aumento de atributo (só nos níveis 4, 8, 12, 16 e 19)
// e uma anotação sobre características e magias. Cada nível guarda um [LevelEntry] com o que foi concedido, e
// [removeLevel] desfaz exatamente isso, inclusive os pontos de atributo que o jogador já gastou.

/** Como a vida é ganha no level up. Escolhida pelo mestre em cada campanha; "" = média fixa (padrão). */
object HpMethod {
    const val AVERAGE = "AVERAGE"
    const val ROLL = "ROLL"

    fun label(method: String): String = if (method == ROLL) "Rolagem" else "Média fixa"

    fun hint(method: String): String =
        if (method == ROLL) "O jogador digita o valor tirado no dado de vida da mesa (o app não rola)."
        else "Metade do dado de vida arredondada para cima, mais o modificador de Constituição."

    /** Texto vindo do JSON: qualquer coisa que não seja rolagem vale como média fixa. */
    fun normalize(raw: String?): String = if (raw == ROLL) ROLL else ""

    fun effective(chosen: String): String = if (chosen == ROLL) ROLL else AVERAGE
}

/** Regras de nível de um sistema. Só o D&D tem regras prontas; nos outros o nível segue livre. */
data class LevelRules(
    val maxLevel: Int = 20,
    val asiLevels: List<Int> = listOf(4, 8, 12, 16, 19),
    val asiPoints: Int = 2,
    val attributeCap: Int = 20,
    val hpKey: String = "hp",
    val conKey: String = "constitution",
    val attributeKeys: List<String> = listOf("strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma"),
    val hitDice: List<Int> = listOf(6, 8, 10, 12)
)

fun levelRulesFor(systemId: String): LevelRules? =
    if (systemId == "dnd5e" || systemId == "dnd2024") LevelRules() else null

/** Bônus de proficiência do D&D pelo nível: 2 no nível 1, +1 a cada 4 níveis (máximo +6). */
fun proficiencyForLevel(level: Int): Int = 2 + (maxOf(level, 1) - 1) / 4

/** Pontos de atributo concedidos ao chegar neste nível (2 nos níveis 4, 8, 12, 16 e 19; senão 0). */
fun asiPointsAt(rules: LevelRules, level: Int): Int = if (level in rules.asiLevels) rules.asiPoints else 0

/**
 * O que um nível concedeu. [done] é falso enquanto o jogador não concluiu o assistente. [asiApplied] guarda o que
 * REALMENTE foi somado em cada atributo (já respeitando o teto), para o desfazer ser exato.
 */
data class LevelEntry(
    val level: Int,
    val hpGained: Int = 0,
    val asiGranted: Int = 0,
    val asiApplied: Map<String, Int> = emptyMap(),
    val done: Boolean = false,
    val note: String = ""
)

/** Atributos de campanha com regra de nível ficam travados para edição direta: o nível só muda por ação do mestre. */
fun levelLocked(sheet: CharacterSheet, campaign: Campaign?): Boolean =
    campaign != null && levelRulesFor(sheet.systemId) != null

/** Aumentos de atributo já concedidos: soma do histórico de níveis e das concessões extras do mestre (0.10.5). */
fun attributeImprovements(sheet: CharacterSheet): Map<String, Int> {
    val total = LinkedHashMap<String, Int>()
    sheet.levelHistory.forEach { e -> e.asiApplied.forEach { (k, v) -> total[k] = (total[k] ?: 0) + v } }
    sheet.asiGrants.forEach { g -> g.applied.forEach { (k, v) -> total[k] = (total[k] ?: 0) + v } }
    return total
}

/** Level up que o jogador ainda não concluiu (sempre o do nível atual), ou nulo. */
fun pendingLevelUp(sheet: CharacterSheet): LevelEntry? =
    sheet.levelHistory.lastOrNull()?.takeIf { !it.done && it.level == sheet.level }

/** Motivo pelo qual não dá para subir o nível desta ficha agora, ou nulo se dá. */
fun levelUpBlockReason(sheet: CharacterSheet, rules: LevelRules): String? = when {
    sheet.level >= rules.maxLevel -> "já está no nível ${rules.maxLevel}"
    pendingLevelUp(sheet) != null -> "ainda não concluiu o level up do nível ${sheet.level}"
    sheet.creation == CreationState.DRAFT -> "a criação ainda não foi finalizada"
    else -> null
}

/** Ação do mestre: sobe um nível. Vida e atributos ficam para o jogador concluir no assistente. Nulo se não dá. */
fun grantLevelUp(sheet: CharacterSheet, rules: LevelRules, autoProficiency: Boolean = true): CharacterSheet? {
    if (levelUpBlockReason(sheet, rules) != null) return null
    val level = sheet.level + 1
    return sheet.copy(
        level = level,
        proficiencyBonus = if (autoProficiency) proficiencyForLevel(level) else sheet.proficiencyBonus,
        levelHistory = sheet.levelHistory + LevelEntry(level = level, asiGranted = asiPointsAt(rules, level))
    )
}

/** Vida da média fixa para um dado de vida (d8 = 5). */
fun averageHp(hitDie: Int): Int = hitDie / 2 + 1

/** Modificador de Constituição da ficha. */
fun conModifier(sheet: CharacterSheet, rules: LevelRules): Int =
    Math.floorDiv((sheet.attributes[rules.conKey] ?: 10) - 10, 2)

/** Resultado do assistente: a ficha nova ou o motivo de não ter dado certo. */
data class LevelOutcome(val sheet: CharacterSheet?, val error: String? = null)

/**
 * Jogador conclui o level up pendente.
 * @param method método de vida da campanha ([HpMethod]).
 * @param hitDie dado de vida da classe (6, 8, 10 ou 12).
 * @param rolled valor digitado do dado (obrigatório na rolagem, de 1 ao dado).
 * @param asi pontos de atributo a somar, por atributo. Precisa gastar todos, a menos que não caiba (teto 20).
 */
fun completeLevelUp(
    sheet: CharacterSheet, rules: LevelRules, method: String, hitDie: Int, rolled: Int?,
    asi: Map<String, Int>, note: String = ""
): LevelOutcome {
    val entry = pendingLevelUp(sheet) ?: return LevelOutcome(null, "Não há level up pendente.")
    if (hitDie !in rules.hitDice) return LevelOutcome(null, "Escolha o dado de vida (d${rules.hitDice.joinToString("/d")}).")

    val spend = asi.filterValues { it != 0 }
    if (spend.any { it.value < 0 }) return LevelOutcome(null, "Os pontos de atributo não podem ser negativos.")
    val spent = spend.values.sum()
    if (spent > entry.asiGranted) return LevelOutcome(null, "Você tem ${entry.asiGranted} pontos de atributo neste nível.")
    val room = rules.attributeKeys.sumOf { maxOf(0, rules.attributeCap - (sheet.attributes[it] ?: 10)) }
    spend.forEach { (key, add) ->
        if (key !in rules.attributeKeys) return LevelOutcome(null, "Atributo desconhecido.")
        val current = sheet.attributes[key] ?: 10
        if (current + add > rules.attributeCap) return LevelOutcome(null, "Nenhum atributo passa de ${rules.attributeCap}.")
    }
    if (spent < minOf(entry.asiGranted, room)) {
        return LevelOutcome(null, "Distribua os ${minOf(entry.asiGranted, room)} pontos de atributo.")
    }

    // Os pontos entram primeiro: o modificador de Constituição novo já vale para a vida deste nível.
    val withAsi = sheet.copy(attributes = sheet.attributes + spend.mapValues { (k, add) -> (sheet.attributes[k] ?: 0) + add })
    val con = conModifier(withAsi, rules)
    val gained = if (HpMethod.effective(method) == HpMethod.ROLL) {
        if (rolled == null || rolled !in 1..hitDie) return LevelOutcome(null, "Digite o valor do dado de 1 a $hitDie.")
        (rolled + con).coerceAtLeast(1)
    } else (averageHp(hitDie) + con).coerceAtLeast(1)

    val hp = withAsi.resources[rules.hpKey] ?: ResourceValue()
    val done = entry.copy(hpGained = gained, asiApplied = spend, done = true, note = note.trim())
    return LevelOutcome(
        withAsi.copy(
            hitDie = hitDie,
            resources = withAsi.resources + (rules.hpKey to ResourceValue(hp.current + gained, hp.max + gained)),
            levelHistory = sheet.levelHistory.dropLast(1) + done
        )
    )
}

/**
 * Ação do mestre: remove o último nível e desfaz exatamente o que ele concedeu (vida e pontos de atributo, mesmo
 * que o jogador já os tenha gastado). Em ficha sem histórico (anterior à 0.10.4) só o nível e a proficiência voltam.
 * Nulo no nível 1.
 */
fun removeLevel(sheet: CharacterSheet, rules: LevelRules, autoProficiency: Boolean = true): CharacterSheet? {
    if (sheet.level <= 1) return null
    val level = sheet.level - 1
    val entry = sheet.levelHistory.lastOrNull()?.takeIf { it.level == sheet.level }
    var next = sheet.copy(
        level = level,
        proficiencyBonus = if (autoProficiency) proficiencyForLevel(level) else sheet.proficiencyBonus
    )
    if (entry != null) {
        next = next.copy(levelHistory = sheet.levelHistory.dropLast(1))
        if (entry.done) {
            next = next.copy(attributes = next.attributes + entry.asiApplied.mapValues { (k, add) -> (next.attributes[k] ?: 0) - add })
            val hp = next.resources[rules.hpKey]
            if (hp != null) {
                val max = (hp.max - entry.hpGained).coerceAtLeast(1)
                next = next.copy(resources = next.resources + (rules.hpKey to ResourceValue(minOf(hp.current, max), max)))
            }
        }
    }
    return next
}

/** Resumo de uma entrada para o histórico da ficha (ex.: "Nível 4: +9 PV, +2 FOR"). [labels] mapeia chave -> sigla. */
fun levelEntrySummary(entry: LevelEntry, labels: Map<String, String> = emptyMap()): String {
    if (!entry.done) return "Nível ${entry.level}: aguardando o jogador"
    val parts = mutableListOf("+${entry.hpGained} PV")
    entry.asiApplied.forEach { (k, v) -> parts += "+$v ${labels[k] ?: k}" }
    return "Nível ${entry.level}: " + parts.joinToString(", ")
}
