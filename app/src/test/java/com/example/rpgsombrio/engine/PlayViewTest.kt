package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.SheetResource
import com.example.rpgsombrio.data.SkillEntry
import com.example.rpgsombrio.data.SkillTemplate
import com.example.rpgsombrio.data.SystemProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayViewTest {
    private val str = SheetAttr("strength", "Força", "FOR", 10, true)
    private val dex = SheetAttr("dexterity", "Destreza", "DES", 10, true)

    private val dnd = SystemProfile(
        id = "dnd", name = "D&D",
        attributes = listOf(str, dex),
        resources = listOf(SheetResource("hp", "Pontos de vida", 10)),
        skillTemplates = listOf(SkillTemplate("Atletismo", "strength"), SkillTemplate("Percepção", "dexterity")),
        usesLevels = true, usesArmorClass = true, usesSkills = true, autoProficiency = true,
        conceptLabel = "Classe", originLabel = "Raça",
        testFormula = "1d20 + FOR + PROF", initiativeFormula = "1d20 + DES"
    )

    private fun sheet(
        attrs: Map<String, Int> = mapOf("strength" to 16, "dexterity" to 14),
        skills: List<SkillEntry> = emptyList(),
        conditions: List<String> = emptyList(),
        saves: List<String> = emptyList()
    ) = CharacterSheet(
        id = "c1", systemId = "dnd", attributes = attrs, skills = skills,
        conditions = conditions, saveProficiencies = saves, proficiencyBonus = 2
    )

    @Test
    fun skillBreakdownShowsEachPart() {
        val skill = SkillEntry("Atletismo", "strength", proficient = true, extra = 1, systemId = "dnd")
        assertEquals("FOR +3 • prof +2 • extra +1", skillBreakdown(dnd, mapOf("strength" to 16), 2, skill))
        // Sem proficiência nem extra, só o atributo.
        val plain = SkillEntry("Atletismo", "strength", systemId = "dnd")
        assertEquals("FOR +3", skillBreakdown(dnd, mapOf("strength" to 16), 2, plain))
    }

    @Test
    fun breakdownAddsUpToTheSkillBonus() {
        val skill = SkillEntry("Atletismo", "strength", proficient = true, extra = 1, systemId = "dnd")
        assertEquals(6, skillBonus(dnd, mapOf("strength" to 16), 2, skill))
    }

    @Test
    fun saveBreakdownIncludesProficiencyOnlyWhenProficient() {
        val s = sheet(saves = listOf("strength"))
        assertEquals("FOR +3 • prof +2", saveBreakdown(s, str))
        assertEquals("DES +2", saveBreakdown(s, dex))
        assertEquals(5, saveBonus(s, str))
    }

    @Test
    fun passivePerceptionIsTenPlusBonus() {
        val skills = listOf(SkillEntry("Percepção", "dexterity", proficient = true, systemId = "dnd"))
        assertEquals(10 + 2 + 2, passivePerception(dnd, sheet(skills = skills)))
    }

    @Test
    fun passivePerceptionIsNullWithoutThePerceptionSkill() {
        assertNull(passivePerception(dnd, sheet(skills = listOf(SkillEntry("Atletismo", "strength", systemId = "dnd")))))
    }

    @Test
    fun playSkillsShowsFavoritesAndProficientFirstThenAllOnDemand() {
        val skills = listOf(
            SkillEntry("Atletismo", "strength", systemId = "dnd"),
            SkillEntry("Percepção", "dexterity", proficient = true, systemId = "dnd"),
            SkillEntry("Acrobacia", "dexterity", favorite = true, systemId = "dnd")
        )
        val main = playSkills(sheet(skills = skills), showAll = false).map { it.value.name }
        assertEquals(listOf("Acrobacia", "Percepção"), main)          // favorita primeiro
        assertEquals(3, playSkills(sheet(skills = skills), showAll = true).size)
        // O índice aponta para a posição na ficha, para a estrela gravar na perícia certa.
        assertEquals(2, playSkills(sheet(skills = skills), showAll = false).first().index)
    }

    @Test
    fun playSkillsHidesSkillsFromOtherSystems() {
        val skills = listOf(SkillEntry("Furtividade", "dexterity", proficient = true, systemId = "outro"))
        assertTrue(playSkills(sheet(skills = skills), showAll = true).isEmpty())
    }

    @Test
    fun poisonedAttacksRollWithDisadvantage() {
        assertEquals("Desvantagem", attackRollBadge(listOf("Envenenado")))
        assertEquals("Vantagem", attackRollBadge(listOf("Invisível")))
        assertNull(attackRollBadge(listOf("Surdo")))
        assertNull(attackRollBadge(emptyList()))
    }

    @Test
    fun advantageAndDisadvantageCancelOut() {
        assertNull(attackRollBadge(listOf("Invisível", "Envenenado")))
    }

    @Test
    fun stunnedAutoFailsStrengthAndDexteritySaves() {
        assertEquals("falha automática (Atordoado)", saveConditionNote(listOf("Atordoado"), str))
        assertEquals("falha automática (Atordoado)", saveConditionNote(listOf("Atordoado"), dex))
        val con = SheetAttr("constitution", "Constituição", "CON", 10, true)
        assertNull(saveConditionNote(listOf("Atordoado"), con))
    }

    @Test
    fun restrainedGivesDisadvantageOnDexteritySavesOnly() {
        assertEquals("desvantagem (Imobilizado)", saveConditionNote(listOf("Imobilizado"), dex))
        assertNull(saveConditionNote(listOf("Imobilizado"), str))
    }

    @Test
    fun conditionNotesExplainWhatChanges() {
        val notes = conditionNotes(listOf("Atordoado"))
        assertTrue(notes.any { it.startsWith("Não pode agir") })
        assertTrue(notes.any { it.contains("falha automática em salvaguardas de Força e Destreza") })
        assertTrue(conditionNotes(listOf("Surdo")).isEmpty())
        assertTrue(conditionNotes(emptyList()).isEmpty())
    }
}
