package com.example.rpgsombrio.engine

/**
 * Modo mesa (0.9.15): numa sessão presencial o dado é físico. O app não rola nada; só soma os bônus fixos
 * ao que o jogador tirou e acompanha PV, iniciativa e condições. Este arquivo é Kotlin puro (sem Android) e testado.
 */

private val TABLE_DICE = Regex("\\d*[dD]\\d+")

private fun signedText(n: Int): String = if (n >= 0) "+$n" else "$n"

/**
 * Parte fixa de uma fórmula: os dados valem 0 e o resto é calculado com as [variables].
 * "1d20 + FOR + PROF" com FOR=3 e PROF=2 vale 5; "+5" vale 5; vazio vale 0.
 * Devolve null quando a fórmula não pode ser calculada (variável desconhecida, texto solto).
 */
fun flatPart(engine: RpgEngine, formula: String, variables: Map<String, Int>): Int? {
    val text = formula.trim()
    if (text.isEmpty()) return 0
    return try {
        engine.roll(TABLE_DICE.replace(text, "0"), variables).total
    } catch (_: RuntimeException) {
        null
    }
}

/** Uma ação já "mastigada" para o jogador: quanto somar ao d20 e o que rolar de dano. */
data class TableAttack(
    val name: String,
    val damageType: String,
    val autoHit: Boolean,
    /** Bônus fixo a somar ao dado de ataque; null em ações sem teste de acerto ou quando não deu para calcular. */
    val toHit: Int?,
    /** Dados de dano para rolar, como "1d8" ou "2d6 + 1d4"; vazio se a fórmula não tem dados. */
    val damageDice: String,
    /** Bônus fixo de dano; null quando a fórmula é complexa demais (multiplicação, parênteses) para separar. */
    val damageFlat: Int?,
    /** Fórmula de dano como o usuário escreveu, usada quando [damageFlat] é null. */
    val damageFormula: String
) {
    /** Linha curta para a carta: "Acerto +5 • Dano 1d8 +3 cortante". */
    val summary: String
        get() {
            val hit = when {
                autoHit -> "Sem teste de acerto"
                toHit != null -> "Acerto ${signedText(toHit)}"
                else -> "Acerto: confira a ficha"
            }
            val dmg = when {
                damageFormula.isBlank() -> ""
                damageFlat == null -> damageFormula
                damageDice.isEmpty() -> "$damageFlat"
                damageFlat == 0 -> damageDice
                else -> "$damageDice ${signedText(damageFlat)}"
            }
            val damageText = if (dmg.isBlank()) "" else "Dano $dmg${if (damageType.isBlank()) "" else " $damageType"}"
            return listOf(hit, damageText).filter { it.isNotBlank() }.joinToString(" • ")
        }
}

/** Separa, para uma [option] da ficha, o bônus de acerto e os dados/bônus de dano. */
fun tableAttack(engine: RpgEngine, option: AttackOption, variables: Map<String, Int>): TableAttack {
    val (formula, type) = splitFormula(option.damage, variables.keys)
    val simple = formula.none { it in "*/×÷()" }
    val dice = TABLE_DICE.findAll(formula).map { match ->
        if (match.value.startsWith("d", ignoreCase = true)) "1${match.value}" else match.value
    }.toList()
    val flat = if (simple) flatPart(engine, formula, variables) else null
    val toHit = if (option.autoHit) null else flatPart(engine, option.bonus, variables)
    return TableAttack(
        name = option.label, damageType = type, autoHit = option.autoHit, toHit = toHit,
        damageDice = if (simple) dice.joinToString(" + ") else "", damageFlat = flat, damageFormula = formula
    )
}

/** O que o jogador anunciou numa jogada de ataque: o d20 natural e o total depois de somar o bônus. */
data class TableRoll(val natural: Int, val total: Int, val critical: Boolean, val fumble: Boolean)

/** Soma o [toHit] ao d20 natural digitado. [natural] 20 é crítico e 1 é falha crítica (só faz sentido com d20). */
fun tableAttackRoll(natural: Int, toHit: Int): TableRoll =
    TableRoll(natural, natural + toHit, critical = natural == 20, fumble = natural == 1)

/** Dano total: soma dos dados que o jogador tirou mais o bônus fixo; nunca negativo. */
fun tableDamageTotal(diceSum: Int, flat: Int): Int = (diceSum + flat).coerceAtLeast(0)
