package com.example.rpgsombrio

import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.ROLL_POOL
import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.SheetResource
import com.example.rpgsombrio.data.SystemProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SavesAndDurationTest {
    private fun engineWith(vararg values: Int): RpgEngine {
        val queue = ArrayDeque(values.toList())
        return RpgEngine { sides -> queue.removeFirst().also { require(it in 1..sides) } }
    }

    private val str = SheetAttr("str", "Força", "FOR", 10, true)
    private val dex = SheetAttr("dex", "Destreza", "DES", 10, true)
    private val wis = SheetAttr("wis", "Sabedoria", "SAB", 10, true)

    private val dnd = SystemProfile(
        id = "dnd", name = "D&D", attributes = listOf(str, dex, wis),
        resources = listOf(SheetResource("hp", "Pontos de vida", 10)), skillTemplates = emptyList(),
        usesLevels = true, usesArmorClass = true, usesSkills = true, autoProficiency = true,
        conceptLabel = "Classe", originLabel = "Raça", testFormula = "1d20 + FOR + PROF"
    )

    private val vampire = SystemProfile(
        id = "v", name = "Vampiro", attributes = listOf(SheetAttr("str", "Força", "FOR", 1, false)),
        resources = listOf(SheetResource("health", "Vitalidade", 5)), skillTemplates = emptyList(),
        usesLevels = false, usesArmorClass = false, usesSkills = true, autoProficiency = false,
        conceptLabel = "Clã", originLabel = "Conceito", rollStyle = ROLL_POOL, poolSides = 10, poolTarget = 6
    )

    private fun fighter(name: String, hp: Int = 10, vararg conditions: String) =
        Combatant(id = name, name = name, initiative = 10, currentHp = hp, maxHp = 10, conditions = conditions.toList())

    // --- duração --------------------------------------------------------------------------------

    @Test fun withConditionAddsAndSetsTheTimer() {
        val f = fighter("A").withCondition("Atordoado", 2)
        assertEquals(listOf("Atordoado"), f.conditions)
        assertEquals(2, f.remainingRounds("Atordoado"))
        assertEquals("Atordoado (2 rodadas)", f.conditionLabel("Atordoado"))
    }

    @Test fun zeroOrNullRoundsMeansUntilRemoved() {
        assertNull(fighter("A").withCondition("Cego", 0).remainingRounds("Cego"))
        assertNull(fighter("A").withCondition("Cego").remainingRounds("Cego"))
        assertEquals("Cego", fighter("A").withCondition("Cego").conditionLabel("Cego"))
    }

    @Test fun oneRoundIsSingular() {
        assertEquals("Cego (1 rodada)", fighter("A").withCondition("Cego", 1).conditionLabel("Cego"))
        assertEquals("por 1 rodada", durationText(1)); assertEquals("por 3 rodadas", durationText(3))
        assertEquals("até ser removida", durationText(0))
    }

    @Test fun reapplyingReplacesTheDurationWithoutDuplicating() {
        val f = fighter("A").withCondition("Cego", 3).withCondition("cego", 5)
        assertEquals(listOf("Cego"), f.conditions)
        assertEquals(5, f.remainingRounds("Cego"))
        // Reaplicar sem duração torna a condição permanente.
        assertNull(f.withCondition("Cego").remainingRounds("Cego"))
    }

    @Test fun removingAConditionDropsItsTimer() {
        val f = fighter("A").withCondition("Cego", 3).withCondition("Surdo", 2).withoutCondition("cego")
        assertEquals(listOf("Surdo"), f.conditions)
        assertEquals(mapOf("Surdo" to 2), f.conditionTimers)
    }

    @Test fun tickCountsDownAndExpires() {
        var f = fighter("A").withCondition("Atordoado", 2).withCondition("Cego")
        val first = f.tickConditions()
        assertTrue(first.expired.isEmpty())
        assertEquals(1, first.fighter.remainingRounds("Atordoado"))
        f = first.fighter
        val second = f.tickConditions()
        assertEquals(listOf("Atordoado"), second.expired)
        assertEquals(listOf("Cego"), second.fighter.conditions)       // a sem duração continua
        assertTrue(second.fighter.conditionTimers.isEmpty())
    }

    @Test fun tickWithoutTimersChangesNothing() {
        val f = fighter("A", 10, "Cego")
        val tick = f.tickConditions()
        assertEquals(f, tick.fighter); assertTrue(tick.expired.isEmpty())
    }

    @Test fun tickIgnoresOrphanTimers() {
        val f = fighter("A").copy(conditionTimers = mapOf("Fantasma" to 1))
        val tick = f.tickConditions()
        assertTrue(tick.expired.isEmpty()); assertTrue(tick.fighter.conditionTimers.isEmpty())
    }

    // --- turnos e rodadas -----------------------------------------------------------------------

    @Test fun skippedAndDefeatedFightersArePassedForTheirTick() {
        val order = listOf(fighter("A"), fighter("B", 10, "Atordoado"), fighter("C", 0), fighter("D"))
        val step = nextTurn(order, 0)
        assertEquals(3, step.nextIndex)
        assertEquals(listOf("B", "C"), step.passed)
        assertFalse(step.newRound)
    }

    @Test fun roundAdvancesWhenTheTurnWrapsAround() {
        val order = listOf(fighter("A"), fighter("B"), fighter("C"))
        assertFalse(nextTurn(order, 0).newRound)
        assertFalse(nextTurn(order, 1).newRound)
        assertTrue(nextTurn(order, 2).newRound)
    }

    @Test fun roundAdvancesWhenSkippingAcrossTheEnd() {
        val order = listOf(fighter("A"), fighter("B"), fighter("C", 10, "Paralisado"))
        val step = nextTurn(order, 1)                 // C perde o turno e volta para A: nova rodada
        assertEquals(0, step.nextIndex); assertTrue(step.newRound)
        assertEquals(listOf("C"), step.passed)
    }

    @Test fun singleFighterStartsANewRoundEveryTurn() {
        val step = nextTurn(listOf(fighter("A")), 0)
        assertEquals(0, step.nextIndex); assertTrue(step.newRound); assertTrue(step.passed.isEmpty())
    }

    @Test fun nobodyCanActStillPassesEveryoneElse() {
        val order = listOf(fighter("A", 10, "Atordoado"), fighter("B", 10, "Inconsciente"))
        val step = nextTurn(order, 0)
        assertTrue(step.nobodyCanAct); assertEquals(listOf("B"), step.passed)
    }

    /** Percorre o turno como a tela faz: o fim do turno de quem jogou e de quem foi pulado desce as condições. */
    private fun advance(order: List<Combatant>, index: Int): Pair<List<Combatant>, Int> {
        val step = nextTurn(order, index)
        val updated = order.toMutableList()
        for (id in listOf(order[index].id) + step.passed) {
            val at = updated.indexOfFirst { it.id == id }
            updated[at] = updated[at].tickConditions().fighter
        }
        return updated to step.nextIndex
    }

    @Test fun stunnedForOneRoundLosesExactlyOneTurn() {
        // A atordoa B por 1 rodada durante o turno de A.
        var order = listOf(fighter("A"), fighter("B").withCondition("Atordoado", 1), fighter("C"))
        var index = 0
        var (o1, i1) = advance(order, index); order = o1; index = i1   // A termina: B (atordoado) é pulado e libera
        assertEquals(2, index)                                         // vez de C
        assertTrue(order[1].conditions.isEmpty())                      // o Atordoado de B acabou ao pular o turno
        val (o2, i2) = advance(order, index)                           // C termina, dá a volta: A
        assertEquals(0, i2)
        val (_, i3) = advance(o2, i2)                                  // A termina, agora B age
        assertEquals(1, i3)
    }

    @Test fun threeRoundCurseLastsThreeOfTheTargetsTurns() {
        var order = listOf(fighter("A"), fighter("B").withCondition("Amedrontado", 3))
        var index = 0
        val bTurnsWithFear = mutableListOf<Boolean>()
        repeat(8) {
            val (o, i) = advance(order, index)
            order = o; index = i
            if (index == 1) bTurnsWithFear += order[1].conditions.contains("Amedrontado")
        }
        // B joga os três primeiros turnos com medo; a condição cai ao fim do terceiro, então o quarto turno já é sem ela.
        assertEquals(listOf(true, true, true, false), bTurnsWithFear)
    }

    // --- regras de salvaguarda ------------------------------------------------------------------

    @Test fun paralyzedAutoFailsStrengthAndDexterityOnly() {
        listOf("Paralisado", "Atordoado", "Inconsciente", "Petrificado").forEach { c ->
            assertEquals(c, saveModifiers(listOf(c), str).autoFailReason)
            assertEquals(c, saveModifiers(listOf(c), dex).autoFailReason)
            assertFalse(saveModifiers(listOf(c), wis).autoFail)
        }
        assertFalse(saveModifiers(listOf("Cego", "Caído"), dex).autoFail)
    }

    @Test fun restrainedHasDisadvantageOnlyOnDexterity() {
        assertEquals(RollMode.DISADVANTAGE, saveModifiers(listOf("Imobilizado"), dex).mode)
        assertEquals(RollMode.NORMAL, saveModifiers(listOf("Imobilizado"), str).mode)
    }

    @Test fun manualAdvantageCancelsConditionDisadvantage() {
        val m = saveModifiers(listOf("Imobilizado"), dex, RollMode.ADVANTAGE)
        assertEquals(RollMode.NORMAL, m.mode)
        assertTrue(m.describe().single().contains("se anulam"))
        assertEquals(RollMode.ADVANTAGE, saveModifiers(emptyList(), wis, RollMode.ADVANTAGE).mode)
        assertTrue(saveModifiers(emptyList(), wis).describe().isEmpty())
    }

    @Test fun poolSystemsHaveNoSaves() {
        assertTrue(saveAttributes(vampire).isEmpty())
        assertEquals(3, saveAttributes(dnd).size)
    }

    // --- bônus ----------------------------------------------------------------------------------

    @Test fun characterSaveUsesProficiency() {
        val hero = CharacterSheet(id = "h", attributes = mapOf("str" to 16, "dex" to 14), proficiencyBonus = 3, saveProficiencies = listOf("dex"))
        assertEquals(3, fighterSaveBonus(hero, null, str))        // +3 do atributo
        assertEquals(5, fighterSaveBonus(hero, null, dex))        // +2 do atributo +3 de proficiência
    }

    @Test fun monsterSaveReadsTheListedValueOrFallsBackToTheModifier() {
        val goblin = MonsterSheet(id = "g", attributes = mapOf("str" to 8, "dex" to 14, "wis" to 12), savingThrows = "DES +6, Sabedoria: +1")
        assertEquals(6, monsterSaveBonus(goblin, dex))
        assertEquals(1, monsterSaveBonus(goblin, wis))
        assertEquals(-1, monsterSaveBonus(goblin, str))           // não listada: modificador de 8
    }

    @Test fun parseSaveListHandlesSeparatorsAndSigns() {
        assertEquals(listOf("DES" to 3, "SAB" to -2, "CON" to 5), parseSaveList("DES +3; Sab −2\nCon 5"))
        assertTrue(parseSaveList("sem números aqui, ???").isEmpty())
    }

    @Test fun avulsoFighterHasNoBaseBonus() {
        assertEquals(0, fighterSaveBonus(null, null, dex))
    }

    // --- rolagem --------------------------------------------------------------------------------

    @Test fun saveSucceedsWhenTheTotalReachesTheDc() {
        val out = resolveSave(engineWith(12), "Goblin", "Destreza", 3, 15, dnd)
        assertEquals(15, out.total); assertTrue(out.success)
        assertTrue(out.line.text.contains("sucesso")); assertEquals(true, out.line.good)
    }

    @Test fun saveFailsBelowTheDc() {
        val out = resolveSave(engineWith(5), "Goblin", "Destreza", 3, 15, dnd)
        assertFalse(out.success); assertEquals(false, out.line.good); assertTrue(out.line.text.contains("falha"))
    }

    @Test fun naturalTwentyDoesNotAutoSucceedOnASave() {
        val out = resolveSave(engineWith(20), "Goblin", "Destreza", -10, 15, dnd)
        assertFalse(out.success)
    }

    @Test fun saveAdvantageKeepsTheHigherRoll() {
        val mods = saveModifiers(emptyList(), wis, RollMode.ADVANTAGE)
        val out = resolveSave(engineWith(4, 17), "Goblin", "Sabedoria", 0, 15, dnd, mods)
        assertEquals(17, out.d20); assertEquals(4, out.otherD20); assertTrue(out.success)
        assertEquals(RollMode.ADVANTAGE, out.mode); assertTrue(out.line.text.contains("vantagem"))
    }

    @Test fun saveDisadvantageKeepsTheLowerRoll() {
        val mods = saveModifiers(listOf("Imobilizado"), dex)
        val out = resolveSave(engineWith(17, 4), "Goblin", "Destreza", 0, 15, dnd, mods)
        assertEquals(4, out.d20); assertFalse(out.success)
    }

    @Test fun autoFailDoesNotRollAtAll() {
        val mods = saveModifiers(listOf("Paralisado"), dex)
        val out = resolveSave(engineWith(), "Goblin", "Destreza", 5, 10, dnd, mods)   // fila vazia: rolar daria erro
        assertTrue(out.autoFailed); assertFalse(out.success); assertNull(out.total)
        assertTrue(out.line.text.contains("falha automática"))
    }

    @Test fun invalidFormulaIsAnErrorNotAFailure() {
        // Dado de 1 lado é inválido: o erro vira uma linha de texto, sem exceção e sem contar como falha.
        val out = resolveSave(RpgEngine { 10 }, "Goblin", "Destreza", 0, 10, dnd.copy(testFormula = "1d1"))
        assertTrue(out.error); assertFalse(out.success); assertNull(out.line.good)
    }

    // --- efeito da falha ------------------------------------------------------------------------

    @Test fun failedSaveAppliesTheConditionForTheChosenRounds() {
        val goblin = fighter("Goblin")
        val out = resolveSave(engineWith(2), "Goblin", "Sabedoria", 0, 15, dnd)
        val effect = applyFailedSaveEffect(goblin, out, "Amedrontado", 3)
        assertEquals(listOf("Amedrontado"), effect.target.conditions)
        assertEquals(3, effect.target.remainingRounds("Amedrontado"))
        assertTrue(effect.line!!.text.contains("por 3 rodadas"))
    }

    @Test fun passedSaveAppliesNothing() {
        val goblin = fighter("Goblin")
        val out = resolveSave(engineWith(19), "Goblin", "Sabedoria", 0, 15, dnd)
        val effect = applyFailedSaveEffect(goblin, out, "Amedrontado", 3)
        assertEquals(goblin, effect.target); assertNull(effect.line)
    }

    @Test fun immuneTargetsDoNotReceiveTheCondition() {
        val golem = fighter("Golem").copy(conditionImmunities = "Amedrontado, Envenenado")
        val out = resolveSave(engineWith(1), "Golem", "Sabedoria", 0, 15, dnd)
        val effect = applyFailedSaveEffect(golem, out, "Amedrontado", 2)
        assertTrue(effect.target.conditions.isEmpty()); assertTrue(effect.line!!.text.contains("imune"))
    }

    @Test fun noConditionOrErrorMeansNoEffect() {
        val goblin = fighter("Goblin")
        val failed = resolveSave(engineWith(1), "Goblin", "Sabedoria", 0, 15, dnd)
        assertEquals(goblin, applyFailedSaveEffect(goblin, failed, "", 2).target)
        val error = resolveSave(RpgEngine { 10 }, "Goblin", "Sabedoria", 0, 15, dnd.copy(testFormula = "1d1"))
        assertEquals(goblin, applyFailedSaveEffect(goblin, error, "Cego", 2).target)
    }

    @Test fun autoFailStillAppliesTheCondition() {
        val goblin = fighter("Goblin", 10, "Paralisado")
        val out = resolveSave(engineWith(), "Goblin", "Destreza", 0, 15, dnd, saveModifiers(goblin.conditions, dex))
        val effect = applyFailedSaveEffect(goblin, out, "Envenenado", 0)
        assertTrue("Envenenado" in effect.target.conditions)
        assertNull(effect.target.remainingRounds("Envenenado"))
        assertTrue(effect.line!!.text.contains("até ser removida"))
    }

    @Test fun saveToStunThenItExpiresAfterTheSkippedTurn() {
        // Fluxo completo: o goblin falha, fica atordoado por 1 rodada, perde o turno e se recupera.
        var order = listOf(fighter("Davi"), fighter("Goblin"))
        val out = resolveSave(engineWith(3), "Goblin", "Constituição", 0, 14, dnd)
        order = order.toMutableList().also { it[1] = applyFailedSaveEffect(it[1], out, "Atordoado", 1).target }
        val step = nextTurn(order, 0)
        assertEquals(0, step.nextIndex)                         // Goblin perdeu o turno, volta para Davi
        assertEquals(listOf("Goblin"), step.skipped.map { it.name })
        assertTrue(order[1].tickConditions().fighter.conditions.isEmpty())
    }
}
