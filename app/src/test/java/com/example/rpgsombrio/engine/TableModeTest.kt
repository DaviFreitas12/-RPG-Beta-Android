package com.example.rpgsombrio.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableModeTest {
    private val engine = RpgEngine { 1 }
    private val vars = mapOf("FOR" to 3, "PROF" to 2, "DES" to 2)

    @Test fun flatPartIgnoresDiceAndKeepsTheRest() {
        assertEquals(5, flatPart(engine, "1d20 + FOR + PROF", vars))
        assertEquals(5, flatPart(engine, "+5", vars))
        assertEquals(-1, flatPart(engine, "-1", vars))
        assertEquals(0, flatPart(engine, "", vars))
    }

    @Test fun flatPartIsNullWhenTheFormulaCannotBeComputed() {
        assertNull(flatPart(engine, "1d20 + XYZ", vars))
    }

    @Test fun attackCardSeparatesToHitDiceAndFlatDamage() {
        val a = tableAttack(engine, AttackOption("Espada", "+5", "1d8 + FOR cortante"), vars)
        assertEquals(5, a.toHit)
        assertEquals("1d8", a.damageDice)
        assertEquals(3, a.damageFlat)
        assertEquals("cortante", a.damageType)
        assertEquals("Acerto +5 • Dano 1d8 +3 cortante", a.summary)
    }

    @Test fun bonusMayUseVariablesAndBareDiceGetACount() {
        val a = tableAttack(engine, AttackOption("Adaga", "DES + PROF", "d4 + DES perfurante"), vars)
        assertEquals(4, a.toHit)
        assertEquals("1d4", a.damageDice)
        assertEquals(2, a.damageFlat)
    }

    @Test fun autoHitActionsHaveNoToHit() {
        val a = tableAttack(engine, AttackOption("Bola de fogo", "", "8d6 fogo", autoHit = true), vars)
        assertNull(a.toHit)
        assertEquals("Sem teste de acerto • Dano 8d6 fogo", a.summary)
    }

    @Test fun severalDiceTermsAreListedTogether() {
        val a = tableAttack(engine, AttackOption("Fúria", "+4", "2d6 + 1d4 + 2"), vars)
        assertEquals("2d6 + 1d4", a.damageDice)
        assertEquals(2, a.damageFlat)
    }

    @Test fun complexFormulasFallBackToTheOriginalText() {
        val a = tableAttack(engine, AttackOption("Complexo", "+3", "(1d6+2)*2 fogo"), vars)
        assertNull(a.damageFlat)
        assertTrue(a.summary.contains("(1d6+2)*2"))
    }

    @Test fun attackRollAddsBonusAndFlagsNaturalExtremes() {
        val crit = tableAttackRoll(20, 5)
        assertEquals(25, crit.total)
        assertTrue(crit.critical)
        val fumble = tableAttackRoll(1, 5)
        assertEquals(6, fumble.total)
        assertTrue(fumble.fumble)
        assertFalse(tableAttackRoll(12, 5).critical)
    }

    @Test fun damageTotalNeverGoesBelowZero() {
        assertEquals(7, tableDamageTotal(4, 3))
        assertEquals(0, tableDamageTotal(1, -3))
    }
}
