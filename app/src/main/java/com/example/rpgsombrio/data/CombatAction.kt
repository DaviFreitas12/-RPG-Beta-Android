package com.example.rpgsombrio.data

import com.example.rpgsombrio.engine.AttackOption
import com.example.rpgsombrio.engine.splitFormula

/**
 * Uma ação de combate (0.9.14): golpe, magia ou habilidade já separada em campos que o motor entende e a carta exibe.
 *
 * Substitui o texto solto ("espada longa, +8, 1d12+4 cortante") por um objeto padronizado:
 * [name] aparece no título, [type] identifica o dano (Cortante, Fogo…), [attackBonus] é o modificador de acerto
 * ("+8", "+6") e [damageFormula] é a expressão de dano ("1d12+4", "8d6").
 */
data class CombatAction(
    val name: String,
    val type: String = "",
    val attackBonus: String = "",
    val damageFormula: String = "",
    /** Ação sem teste de acerto (sopros, áreas): só o dano é rolado. */
    val autoHit: Boolean = false
)

/** Converte a ação no formato que o motor rola. O tipo de dano segue a fórmula, como em "1d12+4 cortante". */
fun CombatAction.toAttackOption(): AttackOption = AttackOption(
    label = name.ifBlank { "Ação" },
    bonus = attackBonus,
    damage = listOf(damageFormula, type).filter { it.isNotBlank() }.joinToString(" "),
    autoHit = autoHit
)

/**
 * Lê um [AttackOption] (como as fichas e itens guardam) e separa fórmula de dano e tipo.
 * [variableKeys] são as siglas válidas na fórmula (FOR, DES…), para que "1d8 + FOR perfurante" não seja cortado em FOR.
 */
fun AttackOption.toCombatAction(variableKeys: Set<String> = emptySet()): CombatAction {
    val (formula, type) = splitFormula(damage, variableKeys)
    return CombatAction(
        name = label,
        type = type.replaceFirstChar { it.uppercase() },
        attackBonus = bonus.trim(),
        damageFormula = formula,
        autoHit = autoHit
    )
}
