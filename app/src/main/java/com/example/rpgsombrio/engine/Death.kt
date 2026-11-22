package com.example.rpgsombrio.engine

// ---------------------------------------------------------------------------------------------
// Salvaguardas contra a morte (0.9.2). Lógica pura, regras no estilo D&D 5e: só personagens
// (criaturas a 0 PV simplesmente caem). A 0 PV, no começo do turno rola-se 1d20 sem modificadores:
// 10 ou mais é sucesso, 9 ou menos é falha, 1 natural conta 2 falhas e 20 natural volta com 1 PV.
// 3 sucessos estabilizam, 3 falhas matam. Dano a 0 PV é uma falha (ver [withDamage]).
// ---------------------------------------------------------------------------------------------

private fun Combatant.isCharacter(): Boolean = sheetKind == CombatantSource.CHARACTER

/** A 0 PV com 3 sucessos: inconsciente, mas fora de perigo. */
fun Combatant.isStable(): Boolean = isCharacter() && currentHp <= 0 && deathFailures < 3 && deathSuccesses >= 3

/** A 0 PV com 3 falhas. */
fun Combatant.isDead(): Boolean = isCharacter() && currentHp <= 0 && deathFailures >= 3

/** Personagem a 0 PV que ainda rola salvaguardas contra a morte. */
fun Combatant.isDying(): Boolean = isCharacter() && currentHp <= 0 && deathFailures < 3 && deathSuccesses < 3

/** "☠ Sucessos 1/3 • Falhas 2/3", "estável" ou "morto"; vazio se não se aplica. */
fun Combatant.deathStatus(): String = when {
    isDead() -> "☠ morto"
    isStable() -> "estável (inconsciente)"
    isDying() -> "☠ Sucessos $deathSuccesses/3 • Falhas $deathFailures/3"
    else -> ""
}

data class DeathSaveResult(val fighter: Combatant, val line: RollLine)

/** Rola uma salvaguarda contra a morte. Quem não está morrendo não rola e volta como está. */
fun rollDeathSave(engine: RpgEngine, fighter: Combatant): DeathSaveResult {
    if (!fighter.isDying()) return DeathSaveResult(fighter, RollLine("${fighter.name} não precisa de salvaguarda contra a morte.", null))
    val d20 = try { engine.roll("1d20").firstD20 ?: 10 } catch (e: RuntimeException) {
        return DeathSaveResult(fighter, RollLine("${fighter.name}: erro — ${e.message}", null))
    }
    val name = fighter.name
    if (d20 == 20) {
        return DeathSaveResult(
            fighter.copy(currentHp = 1.coerceAtMost(fighter.maxHp), deathSuccesses = 0, deathFailures = 0),
            RollLine("☠ $name: salvaguarda contra a morte [20 natural] • volta com 1 PV!", true)
        )
    }
    val success = d20 >= 10
    val failuresAdded = if (d20 == 1) 2 else if (success) 0 else 1
    val updated = fighter.copy(
        deathSuccesses = (fighter.deathSuccesses + if (success) 1 else 0).coerceAtMost(3),
        deathFailures = (fighter.deathFailures + failuresAdded).coerceAtMost(3)
    )
    val tag = if (d20 == 1) " [1 natural: 2 falhas]" else ""
    val outcome = when {
        updated.isDead() -> " • $name morreu."
        updated.isStable() -> " • $name estabilizou."
        else -> " • ${updated.deathStatus().removePrefix("☠ ")}"
    }
    return DeathSaveResult(
        updated,
        RollLine("☠ $name: salvaguarda contra a morte $d20$tag • ${if (success) "sucesso" else "falha"}$outcome", success)
    )
}
