package com.example.rpgsombrio.data

// Regras de criação de personagem (0.10.3, etapa 4). Kotlin puro, sem Android, para poder ser testado na JVM.
//
// Ideia central: a ficha guarda três coisas separadas durante a criação:
//   • baseAttributes  – o valor "comprado" (compra de pontos, array padrão ou rolagem);
//   • attributeBonuses – bônus de raça e antecedente, aplicados DEPOIS da compra;
//   • attributes       – o valor final (base + bônus, com teto), que é o que o resto do app já lê.
// Assim o motor de rolagens e a aba Jogar não mudam: continuam lendo `attributes`.

/** Métodos de criação de atributos. O texto vai para o JSON da campanha; "" significa "padrão do sistema". */
object CreationMethod {
    const val POINT_BUY = "POINT_BUY"
    const val STANDARD_ARRAY = "STANDARD_ARRAY"
    const val ROLL = "ROLL"
    const val FREE = "FREE"

    val all = listOf(POINT_BUY, STANDARD_ARRAY, ROLL, FREE)

    fun label(method: String): String = when (method) {
        POINT_BUY -> "Compra de pontos"
        STANDARD_ARRAY -> "Array padrão"
        ROLL -> "Rolagem"
        else -> "Livre"
    }

    fun hint(method: String): String = when (method) {
        POINT_BUY -> "Cada jogador distribui um orçamento de pontos entre os atributos."
        STANDARD_ARRAY -> "Valores fixos (15, 14, 13, 12, 10, 8), um para cada atributo."
        ROLL -> "Os valores saem dos dados da mesa (4d6, descartando o menor) e são digitados na ficha."
        else -> "Sem limite: o jogador digita o que quiser."
    }

    /** Texto vindo do JSON: valor desconhecido ou vazio vira "" (usa o padrão do sistema). */
    fun normalize(raw: String?): String = all.firstOrNull { it == raw } ?: ""
}

/** Estado da criação na ficha. "" = ficha sem controle de criação (fichas antigas e sistemas livres): edição livre. */
object CreationState {
    const val NONE = ""
    const val DRAFT = "DRAFT"   // em criação: calculadora aberta
    const val DONE = "DONE"     // finalizada: atributos travados (nos sistemas com regra)

    fun normalize(raw: String?): String = when (raw) {
        DRAFT -> DRAFT
        DONE -> DONE
        else -> NONE
    }
}

/** Valor mínimo de um bônus de raça/antecedente (raças antigas tinham penalidades) e o máximo. */
const val BONUS_MIN = -2
const val BONUS_MAX = 6

/** Custos da compra de pontos do D&D 5e / 2024 (valores 8 a 15). */
val DND_POINT_COSTS: Map<Int, Int> = mapOf(8 to 0, 9 to 1, 10 to 2, 11 to 3, 12 to 4, 13 to 5, 14 to 7, 15 to 9)

/** Array padrão do D&D 5e / 2024. */
val DND_STANDARD_ARRAY: List<Int> = listOf(15, 14, 13, 12, 10, 8)

/**
 * Regra de criação de um sistema.
 * @param costs custo de cada valor na compra de pontos; valor ausente cai em [costPerStep] por passo acima de [minBase].
 * @param finalCap teto do valor final (base + bônus); nulo = sem teto.
 */
data class CreationRule(
    val method: String,
    val budget: Int = 27,
    val minBase: Int = 8,
    val maxBase: Int = 15,
    val costs: Map<Int, Int> = DND_POINT_COSTS,
    val costPerStep: Int = 1,
    val array: List<Int> = DND_STANDARD_ARRAY,
    val rollMin: Int = 3,
    val rollMax: Int = 18,
    val finalCap: Int? = 20
) {
    val isFree: Boolean get() = method == CreationMethod.FREE

    /** Faixa de valores base aceitos (nula no método livre). */
    val baseRange: IntRange?
        get() = when (method) {
            CreationMethod.POINT_BUY -> minBase..maxBase
            CreationMethod.STANDARD_ARRAY -> (array.minOrNull() ?: 0)..(array.maxOrNull() ?: 0)
            CreationMethod.ROLL -> rollMin..rollMax
            else -> null
        }

    /** Custo de um valor na compra de pontos; nulo se está fora da faixa. */
    fun costOf(value: Int): Int? =
        if (value !in minBase..maxBase) null else costs[value] ?: ((value - minBase) * costPerStep)
}

/** Regra livre: nada é limitado e a ficha não entra em "criação". */
val FREE_CREATION_RULE = CreationRule(method = CreationMethod.FREE, budget = 0, finalCap = null)

/**
 * Compra de pontos genérica com orçamento linear, para um sistema que queira ativá-la depois
 * (ex.: 20 pontos, valores de 1 a 5, 1 ponto por passo).
 */
fun linearPointBuyRule(budget: Int, minBase: Int, maxBase: Int, costPerStep: Int = 1, finalCap: Int? = null) = CreationRule(
    method = CreationMethod.POINT_BUY, budget = budget, minBase = minBase, maxBase = maxBase,
    costs = emptyMap(), costPerStep = costPerStep, finalCap = finalCap
)

private fun isDndSystem(systemId: String) = systemId == "dnd5e" || systemId == "dnd2024"

/** Métodos que o sistema sabe usar. Só o D&D tem regras prontas; os outros ficam livres por enquanto. */
fun availableCreationMethods(systemId: String): List<String> =
    if (isDndSystem(systemId)) CreationMethod.all else listOf(CreationMethod.FREE)

/** Método padrão do sistema: compra de pontos no D&D, livre nos demais. */
fun defaultCreationMethod(systemId: String): String =
    if (isDndSystem(systemId)) CreationMethod.POINT_BUY else CreationMethod.FREE

/** Método que vale de fato: o escolhido pelo mestre, se o sistema o suporta; senão o padrão do sistema. */
fun effectiveCreationMethod(systemId: String, chosen: String): String =
    chosen.takeIf { it in availableCreationMethods(systemId) } ?: defaultCreationMethod(systemId)

/** A regra de criação de um sistema, com o método escolhido na campanha ("" = padrão do sistema). */
fun creationRuleFor(systemId: String, chosenMethod: String = ""): CreationRule {
    val method = effectiveCreationMethod(systemId, chosenMethod)
    return if (method == CreationMethod.FREE) FREE_CREATION_RULE else CreationRule(method = method)
}

/** Resultado da conferência da criação. */
data class CreationCheck(val valid: Boolean, val spent: Int, val budget: Int, val problems: List<String>) {
    val remaining: Int get() = budget - spent
}

/** Pontos gastos na compra de pontos (valores fora da faixa não contam; a conferência os acusa). */
fun pointsSpent(rule: CreationRule, base: Map<String, Int>, keys: List<String>): Int =
    keys.sumOf { key -> rule.costOf(base[key] ?: rule.minBase) ?: 0 }

/** Confere se os valores base respeitam a regra. [labels] (chave -> sigla) só melhora as mensagens. */
fun checkCreation(
    rule: CreationRule, base: Map<String, Int>, keys: List<String>, labels: Map<String, String> = emptyMap()
): CreationCheck {
    fun name(key: String) = labels[key] ?: key
    if (rule.isFree) return CreationCheck(true, 0, 0, emptyList())
    val problems = mutableListOf<String>()
    when (rule.method) {
        CreationMethod.POINT_BUY -> {
            keys.forEach { key ->
                val v = base[key] ?: rule.minBase
                if (rule.costOf(v) == null) problems += "${name(key)}: $v fora de ${rule.minBase} a ${rule.maxBase}."
            }
            val spent = pointsSpent(rule, base, keys)
            if (spent > rule.budget) problems += "Pontos gastos: $spent de ${rule.budget}."
            return CreationCheck(problems.isEmpty(), spent, rule.budget, problems)
        }
        CreationMethod.STANDARD_ARRAY -> {
            val pool = rule.array.toMutableList()
            keys.forEach { key ->
                val v = base[key]
                if (v == null || !pool.remove(v)) problems += "${name(key)}: ${v ?: "sem valor"} não está disponível no array (${rule.array.joinToString(", ")})."
            }
        }
        CreationMethod.ROLL -> keys.forEach { key ->
            val v = base[key] ?: rule.rollMin - 1
            if (v !in rule.rollMin..rule.rollMax) problems += "${name(key)}: $v fora de ${rule.rollMin} a ${rule.rollMax}."
        }
    }
    return CreationCheck(problems.isEmpty(), 0, 0, problems)
}

/** Valor final de um atributo: base + bônus de raça/antecedente, com o teto da regra. A compra continua exata. */
fun finalScore(rule: CreationRule, base: Int, bonus: Int): Int {
    val total = base + bonus
    val capped = rule.finalCap?.let { minOf(total, it) } ?: total
    return capped.coerceAtLeast(1)
}

/** Valores iniciais de uma criação nova: todo mundo no mínimo (compra), na ordem do array, ou 10 (rolagem). */
fun initialBaseScores(rule: CreationRule, keys: List<String>): Map<String, Int> = when (rule.method) {
    CreationMethod.POINT_BUY -> keys.associateWith { rule.minBase }
    CreationMethod.STANDARD_ARRAY -> keys.withIndex().associate { (i, key) -> key to (rule.array.getOrNull(i) ?: rule.array.last()) }
    CreationMethod.ROLL -> keys.associateWith { 10.coerceIn(rule.rollMin, rule.rollMax) }
    else -> emptyMap()
}

/** Recalcula `attributes` (valor final) a partir de base e bônus para as chaves do sistema. */
fun applyCreationScores(sheet: CharacterSheet, rule: CreationRule, keys: List<String>): CharacterSheet {
    val improvements = attributeImprovements(sheet) // aumentos de atributo de level up (0.10.4)
    val finals = keys.associateWith { key ->
        val base = sheet.baseAttributes[key] ?: sheet.attributes[key] ?: rule.minBase
        finalScore(rule, base, (sheet.attributeBonuses[key] ?: 0) + (improvements[key] ?: 0))
    }
    return sheet.copy(attributes = sheet.attributes + finals)
}

/** Abre a criação (calculadora) numa ficha nova. No método livre não faz nada: a ficha segue sem controle. */
fun startCreation(sheet: CharacterSheet, rule: CreationRule, keys: List<String>): CharacterSheet {
    if (rule.isFree) return sheet
    return applyCreationScores(
        sheet.copy(creation = CreationState.DRAFT, baseAttributes = initialBaseScores(rule, keys), attributeBonuses = emptyMap()),
        rule, keys
    )
}

/** A ficha está com a calculadora aberta? (Só vale em sistemas com regra.) */
fun inCreation(sheet: CharacterSheet, rule: CreationRule): Boolean = sheet.creation == CreationState.DRAFT && !rule.isFree

/** Os atributos estão travados? Ficha finalizada em sistema com regra. Se a regra virar "livre", destrava. */
fun attributesLocked(sheet: CharacterSheet, rule: CreationRule): Boolean = sheet.creation == CreationState.DONE && !rule.isFree

/** Digitar um valor base (array e rolagem). Só limita o teto bruto; a faixa é conferida na hora de finalizar. */
fun setBaseScore(sheet: CharacterSheet, rule: CreationRule, keys: List<String>, key: String, value: Int): CharacterSheet {
    if (!inCreation(sheet, rule) || key !in keys) return sheet
    return applyCreationScores(sheet.copy(baseAttributes = sheet.baseAttributes + (key to value.coerceIn(0, 99))), rule, keys)
}

/** Dá para subir/descer um passo na compra de pontos sem sair da faixa nem estourar o orçamento? */
fun canStepBase(sheet: CharacterSheet, rule: CreationRule, keys: List<String>, key: String, delta: Int): Boolean {
    if (!inCreation(sheet, rule) || rule.method != CreationMethod.POINT_BUY || key !in keys) return false
    val current = sheet.baseAttributes[key] ?: rule.minBase
    val next = current + delta
    val nextCost = rule.costOf(next) ?: return false
    val currentCost = rule.costOf(current) ?: 0
    val spent = pointsSpent(rule, sheet.baseAttributes, keys)
    return spent - currentCost + nextCost <= rule.budget
}

/** Um passo na compra de pontos (+1 ou -1). Sem efeito quando não dá ([canStepBase]). */
fun stepBaseScore(sheet: CharacterSheet, rule: CreationRule, keys: List<String>, key: String, delta: Int): CharacterSheet {
    if (!canStepBase(sheet, rule, keys, key, delta)) return sheet
    val current = sheet.baseAttributes[key] ?: rule.minBase
    return applyCreationScores(sheet.copy(baseAttributes = sheet.baseAttributes + (key to current + delta)), rule, keys)
}

/** Define o bônus de raça/antecedente de um atributo (aplicado depois da compra, com o teto final). */
fun setAttributeBonus(sheet: CharacterSheet, rule: CreationRule, keys: List<String>, key: String, value: Int): CharacterSheet {
    if (!inCreation(sheet, rule) || key !in keys) return sheet
    val bonus = value.coerceIn(BONUS_MIN, BONUS_MAX)
    return applyCreationScores(sheet.copy(attributeBonuses = sheet.attributeBonuses + (key to bonus)), rule, keys)
}

/** Finaliza a criação: trava os atributos. Devolve nulo se a regra não está cumprida (ex.: mais de 27 pontos). */
fun finalizeCreation(
    sheet: CharacterSheet, rule: CreationRule, keys: List<String>, labels: Map<String, String> = emptyMap()
): CharacterSheet? {
    if (!inCreation(sheet, rule)) return null
    if (!checkCreation(rule, sheet.baseAttributes, keys, labels).valid) return null
    return applyCreationScores(sheet, rule, keys).copy(creation = CreationState.DONE)
}

/**
 * Reabre a criação de uma ficha finalizada, mantendo base e bônus. Se a ficha não tem base guardada, recomeça a
 * criação do zero. Sem efeito em ficha que não está finalizada ou em sistema livre.
 */
fun reopenCreation(sheet: CharacterSheet, rule: CreationRule, keys: List<String>): CharacterSheet {
    if (rule.isFree || sheet.creation != CreationState.DONE) return sheet
    if (keys.any { it !in sheet.baseAttributes }) return startCreation(sheet, rule, keys)
    return sheet.copy(creation = CreationState.DRAFT)
}

/**
 * Só o mestre da campanha (ou o dono de uma ficha solta, que não tem mestre) reabre a criação de uma ficha finalizada.
 * Faz parte dos poderes do mestre da etapa 6 (0.10.5).
 */
fun canReopenCreation(campaign: Campaign?, userId: String): Boolean = campaign == null || isCampaignMaster(campaign, userId)
