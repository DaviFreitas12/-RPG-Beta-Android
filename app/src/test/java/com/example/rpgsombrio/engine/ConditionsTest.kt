package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.CONDITION_SUGGESTIONS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionsTest {
    private fun engineWith(vararg values: Int): RpgEngine {
        val queue = ArrayDeque(values.toList())
        return RpgEngine { sides -> queue.removeFirst().also { require(it in 1..sides) } }
    }

    private fun fighter(name: String, hp: Int = 10, vararg conditions: String, init: Int = 10) =
        Combatant(id = name, name = name, initiative = init, currentHp = hp, maxHp = 10, conditions = conditions.toList())

    // --- regras ---------------------------------------------------------------------------------

    @Test fun ruleLookupIgnoresCaseAndAccents() {
        assertNotNull(conditionRule("caido"))
        assertNotNull(conditionRule("  ATORDOADO "))
        assertNull(conditionRule("Abençoado"))
        assertEquals("•", conditionEmoji("Abençoado"))
    }

    @Test fun everySuggestedConditionHasARule() {
        com.example.rpgsombrio.data.CONDITION_SUGGESTIONS.forEach { assertNotNull("sem regra: $it", conditionRule(it)) }
    }

    @Test fun incapacitatingConditionsBlockActing() {
        assertEquals("Atordoado", actionBlocker(listOf("Envenenado", "Atordoado")))
        listOf("Paralisado", "Inconsciente", "Incapacitado", "Petrificado").forEach {
            assertEquals(it, actionBlocker(listOf(it)))
        }
        assertNull(actionBlocker(listOf("Caído", "Cego", "Surdo")))
        assertFalse(fighter("A", 10, "Atordoado").canAct())
        assertFalse(fighter("B", 0).canAct())
        assertTrue(fighter("C", 5, "Caído").canAct())
    }

    // --- vantagem e desvantagem -----------------------------------------------------------------

    @Test fun proneAttackerHasDisadvantage() {
        assertEquals(RollMode.DISADVANTAGE, attackModifiers(listOf("Caído"), emptyList()).mode)
    }

    @Test fun proneTargetDependsOnRange() {
        assertEquals(RollMode.ADVANTAGE, attackModifiers(emptyList(), listOf("Caído"), ranged = false).mode)
        assertEquals(RollMode.DISADVANTAGE, attackModifiers(emptyList(), listOf("Caído"), ranged = true).mode)
    }

    @Test fun stunnedTargetGrantsAdvantage() {
        val m = attackModifiers(emptyList(), listOf("Atordoado"))
        assertEquals(RollMode.ADVANTAGE, m.mode)
        assertFalse(m.forceCrit)
    }

    @Test fun advantageAndDisadvantageCancel() {
        val m = attackModifiers(listOf("Envenenado"), listOf("Atordoado"))
        assertEquals(RollMode.NORMAL, m.mode)
        assertTrue(m.describe().single().contains("se anulam"))
    }

    @Test fun repeatedSourcesDoNotStack() {
        // Cego e Envenenado: duas desvantagens continuam sendo uma desvantagem.
        assertEquals(RollMode.DISADVANTAGE, attackModifiers(listOf("Cego", "Envenenado"), emptyList()).mode)
    }

    @Test fun invisibleAttackerHasAdvantageAndInvisibleTargetDisadvantage() {
        assertEquals(RollMode.ADVANTAGE, attackModifiers(listOf("Invisível"), emptyList()).mode)
        assertEquals(RollMode.DISADVANTAGE, attackModifiers(emptyList(), listOf("Invisível")).mode)
    }

    @Test fun paralyzedAndUnconsciousTargetsForceMeleeCrits() {
        listOf("Paralisado", "Inconsciente").forEach {
            assertTrue(attackModifiers(emptyList(), listOf(it)).forceCrit)
            assertFalse(attackModifiers(emptyList(), listOf(it), ranged = true).forceCrit)
        }
    }

    @Test fun markerOnlyConditionsChangeNothing() {
        val m = attackModifiers(listOf("Agarrado", "Surdo", "Exausto"), listOf("Enfeitiçado", "Condição livre"))
        assertEquals(RollMode.NORMAL, m.mode)
        assertTrue(m.describe().isEmpty())
    }

    // --- o dado de verdade ----------------------------------------------------------------------

    @Test fun advantageKeepsTheHigherRoll() {
        // d20 = 4 e 17 (+3): fica com 17 → 20 contra CA 15 acerta; dano 1d8 = 5.
        val out = resolveAttack(engineWith(4, 17, 5), "Espada", "+3", "1d8", emptyMap(), null, 15, RollMode.ADVANTAGE)
        assertEquals(17, out.d20); assertEquals(4, out.otherD20)
        assertEquals(20, out.attackTotal)
        assertEquals(true, out.hit); assertEquals(5, out.damage)
        assertEquals(RollMode.ADVANTAGE, out.mode)
        assertTrue(out.line.text.contains("vantagem"))
    }

    @Test fun disadvantageKeepsTheLowerRoll() {
        // d20 = 17 e 4 (+3): fica com 4 → 7 contra CA 15 erra e não rola dano.
        val out = resolveAttack(engineWith(17, 4), "Espada", "+3", "1d8", emptyMap(), null, 15, RollMode.DISADVANTAGE)
        assertEquals(4, out.d20); assertEquals(17, out.otherD20)
        assertEquals(false, out.hit); assertNull(out.damage)
    }

    @Test fun disadvantageCanTurnANaturalTwentyIntoAMiss() {
        val out = resolveAttack(engineWith(20, 2), "Espada", "+3", "1d8", emptyMap(), null, 12, RollMode.DISADVANTAGE)
        assertEquals(2, out.d20)
        assertEquals(false, out.hit); assertFalse(out.critical)
    }

    @Test fun advantageOnlyRollsTwiceWhenTheFormulaHasAD20() {
        // Sem d20 na fórmula (1d6 + 30): uma única rolagem de acerto, depois o dano.
        val out = resolveAttack(engineWith(3, 6), "Golpe", "1d6 + 30", "1d8", emptyMap(), null, 10, RollMode.ADVANTAGE)
        assertEquals(RollMode.NORMAL, out.mode); assertNull(out.otherD20)
        assertEquals(6, out.damage)
    }

    @Test fun forcedCritDoublesDamageDiceOnAHit() {
        // d20 = 12 (+3 = 15 >= 10) acerta; crítico forçado dobra 1d8 → 2d8 (3 + 4).
        val out = resolveAttack(engineWith(12, 3, 4), "Espada", "+3", "1d8", emptyMap(), null, 10, forceCrit = true)
        assertTrue(out.critical); assertEquals(7, out.damage); assertEquals(listOf(3, 4), out.damageDice)
        assertTrue(out.line.text.contains("CRÍTICO"))
    }

    @Test fun forcedCritDoesNotSaveAMiss() {
        val out = resolveAttack(engineWith(3), "Espada", "+3", "1d8", emptyMap(), null, 15, forceCrit = true)
        assertEquals(false, out.hit); assertFalse(out.critical); assertNull(out.damage)
    }

    @Test fun naturalOneStillMissesWithAdvantageWhenBothAreLow() {
        val out = resolveAttack(engineWith(1, 1), "Espada", "+10", "1d8", emptyMap(), null, 5, RollMode.ADVANTAGE)
        assertEquals(false, out.hit); assertTrue(out.fumble)
    }

    @Test fun outcomeExposesAnimationData() {
        val out = resolveAttack(engineWith(12, 5), "Espada", "+4", "1d8+2 cortante", emptyMap(), null, 15)
        assertEquals(16, out.attackTotal); assertEquals(12, out.d20); assertEquals(15, out.defense)
        assertEquals(8, out.damageSides); assertEquals(listOf(5), out.damageDice); assertEquals("cortante", out.damageType)
    }

    @Test fun rollOptionPassesModeAndCritThrough() {
        val option = AttackOption("Espada", "+3", "1d8")
        val out = rollOption(engineWith(4, 17, 3, 4), option, emptyMap(), null, 15, RollMode.ADVANTAGE, forceCrit = true)
        assertEquals(17, out.d20); assertTrue(out.critical); assertEquals(7, out.damage)
    }

    // --- dano -----------------------------------------------------------------------------------

    @Test fun petrifiedResistsAllDamage() {
        assertEquals(0.5, conditionDamageMultiplier(1.0, listOf("Petrificado")), 0.0)
        assertEquals(0.5, conditionDamageMultiplier(0.5, listOf("Petrificado")), 0.0)   // não acumula
        assertEquals(0.0, conditionDamageMultiplier(0.0, listOf("Petrificado")), 0.0)
        assertEquals(1.0, conditionDamageMultiplier(2.0, listOf("Petrificado")), 0.0)   // vulnerável + resistente
        assertEquals(1.0, conditionDamageMultiplier(1.0, listOf("Caído")), 0.0)
        assertEquals(3, scaleDamage(7, 0.5)); assertEquals(7, scaleDamage(7, 1.0)); assertEquals(0, scaleDamage(7, 0.0))
    }

    // --- turnos ---------------------------------------------------------------------------------

    @Test fun stunnedFightersLoseTheirTurn() {
        val order = listOf(fighter("A"), fighter("B", 10, "Atordoado"), fighter("C"))
        val step = nextTurn(order, 0)
        assertEquals(2, step.nextIndex)
        assertEquals(listOf(SkippedTurn("B", "Atordoado")), step.skipped)
    }

    @Test fun defeatedFightersAreSkippedSilently() {
        val order = listOf(fighter("A"), fighter("B", 0), fighter("C"))
        val step = nextTurn(order, 0)
        assertEquals(2, step.nextIndex); assertTrue(step.skipped.isEmpty())
    }

    @Test fun turnWrapsAroundAndCanReturnToTheSameFighter() {
        val order = listOf(fighter("A"), fighter("B", 10, "Paralisado"), fighter("C", 0))
        val step = nextTurn(order, 0)
        assertEquals(0, step.nextIndex)
        assertEquals(1, step.skipped.size)
        assertFalse(step.nobodyCanAct)
    }

    @Test fun nobodyCanActFallsBackToTheNextInLine() {
        val order = listOf(fighter("A", 10, "Atordoado"), fighter("B", 10, "Inconsciente"))
        val step = nextTurn(order, 0)
        assertTrue(step.nobodyCanAct); assertEquals(1, step.nextIndex); assertTrue(step.skipped.isEmpty())
    }

    @Test fun emptyOrderIsSafe() {
        assertEquals(0, nextTurn(emptyList(), 0).nextIndex)
    }
}
