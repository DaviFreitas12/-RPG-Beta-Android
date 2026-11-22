package com.example.rpgsombrio.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeathAndTempHpTest {
    private fun engineWith(vararg values: Int): RpgEngine {
        val queue = ArrayDeque(values.toList())
        return RpgEngine { sides -> queue.removeFirst().also { require(it in 1..sides) } }
    }

    private fun hero(hp: Int = 10, temp: Int = 0) =
        Combatant("h", "Davi", 10, hp, 10, sheetId = "s", sheetKind = CombatantSource.CHARACTER, tempHp = temp)

    private fun goblin(hp: Int = 7) = Combatant("g", "Goblin", 10, hp, 7, sheetKind = CombatantSource.MONSTER)

    // --- PV temporários -------------------------------------------------------------------------

    @Test fun tempHpAbsorbsDamageFirst() {
        val h = hero(10, 5).withDamage(3)
        assertEquals(10, h.currentHp); assertEquals(2, h.tempHp)
    }

    @Test fun overflowDamageGoesToRealHp() {
        val h = hero(10, 5).withDamage(8)
        assertEquals(0, h.tempHp); assertEquals(7, h.currentHp)
    }

    @Test fun healingDoesNotRestoreTempHp() {
        val h = hero(6, 4).withDamage(-10)
        assertEquals(10, h.currentHp); assertEquals(4, h.tempHp)
    }

    @Test fun damageWithoutTempHpWorksAsBefore() {
        assertEquals(0, hero(5).withDamage(99).currentHp)
        assertEquals(10, hero(5).withDamage(-99).currentHp)
    }

    // --- dano a 0 PV e cura ---------------------------------------------------------------------

    @Test fun damageAtZeroHpIsADeathFailure() {
        val h = hero(0).withDamage(4).withDamage(2)
        assertEquals(2, h.deathFailures); assertEquals(0, h.currentHp)
        assertTrue(h.withDamage(1).isDead())
    }

    @Test fun damageAbsorbedByTempHpAtZeroIsNotAFailure() {
        assertEquals(0, hero(0, 5).withDamage(3).deathFailures)
    }

    @Test fun monstersAtZeroDoNotAccumulateFailures() {
        assertEquals(0, goblin(0).withDamage(3).deathFailures)
    }

    @Test fun healingAboveZeroResetsDeathSaves() {
        val h = hero(0).copy(deathSuccesses = 2, deathFailures = 2).withDamage(-3)
        assertEquals(3, h.currentHp); assertEquals(0, h.deathSuccesses); assertEquals(0, h.deathFailures)
    }

    // --- salvaguarda contra a morte -------------------------------------------------------------

    @Test fun tenOrMoreIsASuccess() {
        val r = rollDeathSave(engineWith(10), hero(0))
        assertEquals(1, r.fighter.deathSuccesses); assertEquals(true, r.line.good)
    }

    @Test fun nineOrLessIsAFailure() {
        val r = rollDeathSave(engineWith(9), hero(0))
        assertEquals(1, r.fighter.deathFailures); assertEquals(false, r.line.good)
    }

    @Test fun naturalOneCountsTwoFailures() {
        assertEquals(2, rollDeathSave(engineWith(1), hero(0)).fighter.deathFailures)
    }

    @Test fun naturalTwentyBringsBackOneHp() {
        val r = rollDeathSave(engineWith(20), hero(0).copy(deathFailures = 2, deathSuccesses = 1))
        assertEquals(1, r.fighter.currentHp); assertEquals(0, r.fighter.deathFailures); assertEquals(0, r.fighter.deathSuccesses)
        assertFalse(r.fighter.isDying())
    }

    @Test fun threeSuccessesStabilize() {
        val r = rollDeathSave(engineWith(15), hero(0).copy(deathSuccesses = 2))
        assertTrue(r.fighter.isStable()); assertFalse(r.fighter.isDying()); assertTrue(r.line.text.contains("estabilizou"))
    }

    @Test fun threeFailuresKill() {
        val r = rollDeathSave(engineWith(5), hero(0).copy(deathFailures = 2))
        assertTrue(r.fighter.isDead()); assertTrue(r.line.text.contains("morreu"))
    }

    @Test fun stableCharacterTakingDamageStartsFailingAgain() {
        val h = hero(0).copy(deathSuccesses = 3).withDamage(2)
        assertEquals(1, h.deathFailures); assertTrue(h.isDying() || h.isStable())
    }

    @Test fun onlyDyingCharactersRoll() {
        val engine = engineWith()                       // fila vazia: rolar daria erro
        assertEquals(hero(5), rollDeathSave(engine, hero(5)).fighter)
        assertEquals(goblin(0), rollDeathSave(engine, goblin(0)).fighter)
        assertEquals(hero(0).copy(deathFailures = 3), rollDeathSave(engine, hero(0).copy(deathFailures = 3)).fighter)
    }

    @Test fun statusText() {
        assertEquals("", hero(5).deathStatus())
        assertEquals("☠ Sucessos 1/3 • Falhas 2/3", hero(0).copy(deathSuccesses = 1, deathFailures = 2).deathStatus())
        assertEquals("☠ morto", hero(0).copy(deathFailures = 3).deathStatus())
    }

    @Test fun dyingFighterGetsTheirRollWhenTheirTurnIsPassed() {
        val order = listOf(Combatant("a", "A", 12, 10, 10), hero(0))
        val step = nextTurn(order, 0)
        assertEquals(listOf("h"), step.passed)          // a tela rola a salvaguarda de quem está em `passed`
    }
}
