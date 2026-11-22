package com.example.rpgsombrio.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RpgEngineTest {
    /** Rolador determinístico: devolve os valores da fila, na ordem. */
    private fun engineWith(vararg values: Int): RpgEngine {
        val queue = ArrayDeque(values.toList())
        return RpgEngine { sides ->
            val v = queue.removeFirst()
            require(v in 1..sides) { "valor $v fora de d$sides" }
            v
        }
    }

    @Test fun arithmeticAndPrecedence() {
        val e = engineWith()
        assertEquals(14, e.roll("2 + 3 * 4").total)
        assertEquals(20, e.roll("(2 + 3) * 4").total)
        assertEquals(-3, e.roll("-3").total)
    }

    @Test fun diceUseRoller() {
        val e = engineWith(4, 6)
        val r = e.roll("2d6 + 1")
        assertEquals(11, r.total)
        assertEquals(listOf(4, 6), r.naturalRolls)
    }

    @Test fun implicitDiceCount() {
        assertEquals(7, engineWith(7).roll("d20").total)
    }

    @Test fun variablesStartingWithDAreNotDice() {
        val e = engineWith(10)
        assertEquals(15, e.roll("1d20 + DES", mapOf("DES" to 5)).total)
    }

    @Test fun variablesAreCaseAndAccentInsensitive() {
        val e = engineWith()
        assertEquals(3, e.roll("constituição", mapOf("CONSTITUIÇÃO" to 3)).total)
        assertEquals(2, e.roll("proficiência", mapOf("PROF" to 2)).total)
    }

    @Test fun unknownVariableFails() {
        val ex = assertThrows(IllegalArgumentException::class.java) { engineWith().roll("1 + XYZ") }
        assertTrue(ex.message!!.contains("XYZ"))
    }

    @Test fun divisionFloorsTowardNegativeInfinity() {
        val e = engineWith()
        assertEquals(-1, e.roll("(8 - 10) / 2").total)
        assertEquals(-2, e.roll("-3 / 2").total)
        assertEquals(2, e.roll("5 / 2").total)
    }

    @Test fun divisionByZeroFails() {
        assertThrows(IllegalArgumentException::class.java) { engineWith().roll("4 / 0") }
    }

    @Test fun invalidInputsFail() {
        val e = engineWith()
        assertThrows(IllegalArgumentException::class.java) { e.roll("") }
        assertThrows(IllegalArgumentException::class.java) { e.roll("(1 + 2") }
        assertThrows(IllegalArgumentException::class.java) { e.roll("1d1") }
        assertThrows(IllegalArgumentException::class.java) { e.roll("101d6") }
        assertThrows(IllegalArgumentException::class.java) { e.roll("1 + ") }
    }

    @Test fun checkComparesAgainstDifficulty() {
        val ok = engineWith(10).check("1d20 + 5", 15)
        assertTrue(ok.success); assertEquals(0, ok.margin)
        val bad = engineWith(9).check("1d20 + 5", 15)
        assertFalse(bad.success); assertEquals(-1, bad.margin)
    }

    @Test fun attackHitsAndRollsDamage() {
        // d20 = 12 (+3 = 15 >= CA 15), dano d8 = 5 (+3 = 8)
        val r = engineWith(12, 5).attack("1d20 + 3", "1d8 + 3", 15)
        assertTrue(r.hit); assertFalse(r.critical)
        assertEquals(8, r.damageRoll!!.total)
    }

    @Test fun attackMissesWithoutDamage() {
        val r = engineWith(5).attack("1d20 + 3", "1d8 + 3", 15)
        assertFalse(r.hit)
        assertNull(r.damageRoll)
    }

    @Test fun naturalTwentyAlwaysHitsAndDoublesDamageDice() {
        // d20 = 20 contra CA absurda; dano 1d8 vira 2d8 (3 + 4) + 2
        val r = engineWith(20, 3, 4).attack("1d20 - 5", "1d8 + 2", 99)
        assertTrue(r.hit); assertTrue(r.critical)
        assertEquals(9, r.damageRoll!!.total)
        assertEquals(listOf(3, 4), r.damageRoll!!.naturalRolls)
    }

    @Test fun naturalOneAlwaysMisses() {
        val r = engineWith(1).attack("1d20 + 30", "1d8", 5)
        assertFalse(r.hit); assertTrue(r.criticalMiss)
        assertNull(r.damageRoll)
    }

    @Test fun attackWithoutD20UsesPlainComparison() {
        val r = engineWith().attack("20", "5", 15)
        assertTrue(r.hit); assertFalse(r.critical)
        assertEquals(5, r.damageRoll!!.total)
    }

    @Test fun dndModifierTable() {
        assertEquals(-1, dndModifier(8))
        assertEquals(-1, dndModifier(9))
        assertEquals(0, dndModifier(10))
        assertEquals(0, dndModifier(11))
        assertEquals(1, dndModifier(12))
        assertEquals(5, dndModifier(20))
        assertEquals(-5, dndModifier(0))
    }
}
