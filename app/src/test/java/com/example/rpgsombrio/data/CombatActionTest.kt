package com.example.rpgsombrio.data

import com.example.rpgsombrio.engine.AttackOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CombatActionTest {
    @Test
    fun separaFormulaETipoDeDano() {
        val action = AttackOption("Ataque Furioso", "+8", "1d12+4 cortante").toCombatAction()
        assertEquals("Ataque Furioso", action.name)
        assertEquals("+8", action.attackBonus)
        assertEquals("1d12+4", action.damageFormula)
        assertEquals("Cortante", action.type)
        assertFalse(action.autoHit)
    }

    @Test
    fun magiaSemBonusFicaAutomatica() {
        val action = AttackOption("Bola de Fogo", "", "8d6 fogo", autoHit = true).toCombatAction()
        assertEquals("8d6", action.damageFormula)
        assertEquals("Fogo", action.type)
        assertTrue(action.autoHit)
    }

    @Test
    fun siglasDeAtributoFicamNaFormula() {
        val action = AttackOption("Adaga", "+5", "1d4 + FOR perfurante").toCombatAction(setOf("FOR"))
        assertEquals("1d4 + FOR", action.damageFormula)
        assertEquals("Perfurante", action.type)
    }

    @Test
    fun voltaParaOMotorSemPerderDados() {
        val option = CombatAction("Espada", "Cortante", "+6", "1d8+3").toAttackOption()
        assertEquals("Espada", option.label)
        assertEquals("+6", option.bonus)
        assertEquals("1d8+3 Cortante", option.damage)
    }

    @Test
    fun papelDaFichaComecaComoJogadorEAceitaMestre() {
        assertEquals("jogador", CharacterSheet(id = "x").role)
        assertEquals(SheetRole.MASTER, SheetRole.normalize(" Mestre "))
        assertEquals(SheetRole.PLAYER, SheetRole.normalize("qualquer coisa"))
        assertEquals(SheetRole.PLAYER, SheetRole.normalize(null))
    }
}
