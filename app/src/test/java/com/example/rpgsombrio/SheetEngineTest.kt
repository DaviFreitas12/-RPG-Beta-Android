package com.example.rpgsombrio

import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.DamageDefense
import com.example.rpgsombrio.data.DefenseKind
import com.example.rpgsombrio.data.adjustedDamage
import com.example.rpgsombrio.data.damageMultiplierFor
import com.example.rpgsombrio.data.immuneToCondition
import com.example.rpgsombrio.data.defensesSummary
import com.example.rpgsombrio.data.InventoryEntry
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.ResourceValue
import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.SheetResource
import com.example.rpgsombrio.data.SkillEntry
import com.example.rpgsombrio.data.SkillTemplate
import com.example.rpgsombrio.data.SpellEntry
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.fitsSystem
import com.example.rpgsombrio.data.ROLL_POOL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetEngineTest {
    private fun engineWith(vararg values: Int): RpgEngine {
        val queue = ArrayDeque(values.toList())
        return RpgEngine { sides -> queue.removeFirst().also { require(it in 1..sides) } }
    }

    private val dnd = SystemProfile(
        id = "dnd", name = "D&D",
        attributes = listOf(
            SheetAttr("str", "Força", "FOR", 10, true),
            SheetAttr("dex", "Destreza", "DES", 10, true)
        ),
        resources = listOf(SheetResource("hp", "Pontos de vida", 10), SheetResource("mana", "Mana", 5)),
        skillTemplates = listOf(SkillTemplate("Atletismo", "str")),
        usesLevels = true, usesArmorClass = true, usesSkills = true, autoProficiency = true,
        conceptLabel = "Classe", originLabel = "Raça",
        testFormula = "1d20 + FOR + PROF", initiativeFormula = "1d20 + DES"
    )

    private val vampire = SystemProfile(
        id = "v", name = "Vampiro",
        attributes = listOf(SheetAttr("str", "Força", "FOR", 1, false), SheetAttr("dex", "Destreza", "DES", 1, false)),
        resources = listOf(SheetResource("health", "Vitalidade", 5)),
        skillTemplates = emptyList(), usesLevels = false, usesArmorClass = false, usesSkills = true,
        autoProficiency = false, conceptLabel = "Clã", originLabel = "Conceito",
        rollStyle = ROLL_POOL, poolSides = 10, poolTarget = 6
    )

    private val hero = CharacterSheet(
        id = "h", name = "Davi", level = 3, armorClass = 14, proficiencyBonus = 2,
        attributes = mapOf("str" to 16, "dex" to 14),
        resources = mapOf("hp" to ResourceValue(18, 20), "mana" to ResourceValue(4, 5)),
        skills = listOf(SkillEntry("Atletismo", "str", proficient = true))
    )

    @Test fun variableNamesAreSafeForTheParser() {
        assertEquals("FORCA", variableName("Força"))
        assertEquals("LIDAR_COM_ANIMAIS", variableName("Lidar com Animais"))
        assertEquals("_D6", variableName("d6"))
        assertEquals("_1X", variableName("1x"))
        assertEquals("", variableName("  !! "))
    }

    @Test fun sheetVariablesUseModifiersAndExposeRawValues() {
        val v = characterVariables(hero, dnd)
        assertEquals(3, v["FOR"]); assertEquals(2, v["DES"])
        assertEquals(16, v["FOR_VAL"])
        assertEquals(2, v["PROF"]); assertEquals(3, v["NIVEL"]); assertEquals(14, v["CA"])
        assertEquals(18, v["PV"]); assertEquals(20, v["PV_MAX"])
        assertEquals(5, v["ATLETISMO"]) // mod 3 + prof 2
    }

    @Test fun formulaUsesTheSheet() {
        val v = characterVariables(hero, dnd)
        assertEquals(17, engineWith(12).roll("1d20 + FOR + PROF", v).total)
    }

    @Test fun skillRollMatchesDisplayedBonus() {
        val line = rollSkillCheck(engineWith(10), hero, dnd, hero.skills[0], 15)
        assertTrue(line.good == true)
        assertTrue(line.text.contains("= 15"))
    }

    @Test fun poolRollCountsSuccesses() {
        // Força 1 + 2 pontos de perícia = 3 dados: 6, 5, 10 -> 2 sucessos
        val sheet = hero.copy(attributes = mapOf("str" to 1), skills = listOf(SkillEntry("Briga", "str", extra = 2)))
        val line = rollSkillCheck(engineWith(6, 5, 10), sheet, vampire, sheet.skills[0], 2)
        assertEquals(true, line.good)
        assertTrue(line.text.contains("2 sucesso"))
    }

    @Test fun defaultFormulasComeFromTheSystem() {
        assertEquals("1d20 + FOR + PROF", defaultTestFormula(dnd))
        assertEquals("1d20 + DES", defaultInitiativeFormula(dnd))
        assertEquals("1d10 + DES", defaultInitiativeFormula(vampire))
        assertEquals("1d20", baseDie(null))
    }

    @Test fun splitFormulaSeparatesDamageTypeAndKnownVariables() {
        assertEquals("1d4+2" to "perfurante", splitFormula("1d4+2 perfurante", emptySet()))
        assertEquals("2d8 + INT" to "necrótico", splitFormula("2d8 + INT necrótico", setOf("INT")))
        assertEquals("1d6" to "fogo", splitFormula("1d6 + fogo", emptySet()))
        assertEquals("" to "texto livre", splitFormula("texto livre", emptySet()))
    }

    @Test fun parsesMonsterActionText() {
        val a = parseMonsterAction("Ataque corpo a corpo: +4 para acertar. Dano: 2d4+2 perfurante.")!!
        assertEquals(4, a.attackBonus); assertEquals("2d4+2", a.damage); assertEquals("perfurante", a.damageType)
        val b = parseMonsterAction("Sopro. Dano: 7 (2d6) fogo")!!
        assertNull(b.attackBonus); assertEquals("2d6", b.damage)
        assertNull(parseMonsterAction("Apenas descrição."))
    }

    @Test fun attackHitsCritsAndDoublesDice() {
        val v = mapOf("FOR" to 3)
        val miss = resolveAttack(engineWith(5), "Espada", "+4", "1d8+FOR slashing", v, dnd, 15)
        assertEquals(false, miss.hit); assertNull(miss.damage)
        val hit = resolveAttack(engineWith(12, 5), "Espada", "+4", "1d8+FOR", v, dnd, 15)
        assertEquals(true, hit.hit); assertEquals(8, hit.damage)
        val crit = resolveAttack(engineWith(20, 3, 4), "Espada", "+4", "1d8+FOR", v, dnd, 99)
        assertEquals(10, crit.damage)
    }

    @Test fun autoHitActionRollsOnlyDamage() {
        val out = rollDamageOnly(engineWith(4, 5), "Sopro", "2d6 fogo", emptyMap())
        assertEquals(9, out.damage); assertNull(out.hit)
    }

    @Test fun castingSpendsMatchingResourceAndRollsEffect() {
        val spell = SpellEntry("s", "Chama", damage = "2d8 + FOR necrótico", cost = "3 Mana")
        val out = castSpell(engineWith(4, 5), spell, hero, dnd)
        assertTrue(out.cast)
        assertEquals(1, out.sheet.resources["mana"]!!.current)
        assertTrue(out.line.text.contains("= 12"))
    }

    @Test fun castingFailsWithoutResourceAndSpendsNothing() {
        val spell = SpellEntry("s", "Chama", damage = "2d8", cost = "9 Mana")
        val out = castSpell(engineWith(), spell, hero, dnd)
        assertFalse(out.cast); assertEquals(hero, out.sheet)
    }

    @Test fun invalidEffectSpendsNothing() {
        val spell = SpellEntry("s", "Chama", damage = "1d6 + XYZ", cost = "1 Mana")
        // XYZ não é variável conhecida: a fórmula acaba em "1d6" e rola normalmente, sem erro
        val ok = castSpell(engineWith(3), spell, hero, dnd)
        assertTrue(ok.cast)
        val bad = castSpell(engineWith(), SpellEntry("s", "Vazia", damage = "só texto", cost = "1 Mana"), hero, dnd)
        assertFalse(bad.cast); assertEquals(4, bad.sheet.resources["mana"]!!.current)
    }

    @Test fun costWithoutMatchingResourceIsInformational() {
        assertNull(parseResourceCost("1 espaço de magia", dnd))
        assertNotNull(parseResourceCost("2 PV", dnd))
    }

    @Test fun manualVariableList() {
        assertEquals(mapOf("FOR" to 3, "DES" to -1), parseVariableList("FOR=3, des = -1; lixo, X=abc"))
    }

    @Test fun equippedItemsChangeArmorClass() {
        val chain = ItemEntry("cota", "Cota de malha", type = "Armadura", armorClass = 16)
        val leather = ItemEntry("couro", "Couro", type = "Armadura", armorClass = 11)
        val shield = ItemEntry("escudo", "Escudo", type = "Escudo", armorClass = 2)
        val ring = ItemEntry("anel", "Anel de proteção", type = "Mágico", armorClass = 1)
        val items = listOf(chain, leather, shield, ring)
        // Nada equipado: CA base da ficha (14).
        assertEquals(14, effectiveArmorClass(hero, items))
        val kit = hero.copy(inventory = listOf(
            InventoryEntry("cota", 1, true), InventoryEntry("couro", 1, true),
            InventoryEntry("escudo", 1, true), InventoryEntry("anel", 1, false)
        ))
        // Maior armadura substitui a base (16) + escudo (+2); anel não equipado não conta.
        assertEquals(18, effectiveArmorClass(kit, items))
        assertEquals(18, characterVariables(kit, dnd, items)["CA"])
        assertEquals(18, characterCombatant(engineWith(10), kit, dnd, items).armorClass)
        assertTrue(armorClassBreakdown(kit, items).modified)
        assertFalse(armorClassBreakdown(hero, items).modified)
    }

    @Test fun itemsAndSkillsFollowTheSheetSystem() {
        val sheet = hero.copy(
            systemId = "dnd",
            skills = listOf(SkillEntry("Atletismo", "str", true, systemId = "dnd"), SkillEntry("Briga", "str", systemId = "v"), SkillEntry("Livre", "str")),
            inventory = listOf(InventoryEntry("espada", 1, true), InventoryEntry("garra", 1, true), InventoryEntry("comum", 1, true))
        )
        val items = listOf(
            ItemEntry("espada", "Espada", damage = "1d8", systemId = "dnd"),
            ItemEntry("garra", "Garra", damage = "1d6", systemId = "v"),
            ItemEntry("comum", "Adaga", damage = "1d4")
        )
        val vars = characterVariables(sheet, dnd, items)
        assertTrue(vars.containsKey("ATLETISMO")); assertTrue(vars.containsKey("LIVRE")); assertFalse(vars.containsKey("BRIGA"))
        assertEquals(listOf("Espada (equipado)", "Adaga (equipado)"), characterAttacks(sheet, items).map { it.label })
        assertTrue(SpellEntry("s", "x").fitsSystem("qualquer"))
        assertFalse(SpellEntry("s", "x", systemId = "v").fitsSystem("dnd"))
    }

    @Test fun combatantsFromSheets() {
        val c = characterCombatant(engineWith(10), hero, dnd)
        assertEquals(12, c.initiative) // 1d20 + DES(+2)
        assertEquals(18, c.currentHp); assertEquals(20, c.maxHp); assertEquals(14, c.armorClass)
        assertEquals(CombatantSource.CHARACTER, c.sheetKind)
        assertEquals(0, c.withDamage(99).currentHp)
        assertEquals(20, c.withDamage(-99).currentHp)
    }

    // --- defesas de dano ---

    @Test fun defenseMultiplierFollowsKind() {
        val d = listOf(
            DamageDefense(DefenseKind.RESISTANCE, "Fogo"),
            DamageDefense(DefenseKind.VULNERABILITY, "Radiante"),
            DamageDefense(DefenseKind.IMMUNITY, "Venenoso")
        )
        assertEquals(0.5, damageMultiplierFor(d, "fogo"), 0.0)
        assertEquals(2.0, damageMultiplierFor(d, " Radiante "), 0.0)
        assertEquals(0.0, damageMultiplierFor(d, "Venenoso"), 0.0)
        assertEquals(1.0, damageMultiplierFor(d, "Frio"), 0.0)
        assertEquals(1.0, damageMultiplierFor(d, ""), 0.0)
    }

    @Test fun resistanceAndVulnerabilityCancelAndImmunityWins() {
        val both = listOf(DamageDefense(DefenseKind.RESISTANCE, "Fogo"), DamageDefense(DefenseKind.VULNERABILITY, "Fogo"))
        assertEquals(1.0, damageMultiplierFor(both, "Fogo"), 0.0)
        val immune = both + DamageDefense(DefenseKind.IMMUNITY, "Fogo")
        assertEquals(0.0, damageMultiplierFor(immune, "Fogo"), 0.0)
    }

    @Test fun defensesSummaryGroupsByKind() {
        val d = listOf(
            DamageDefense(DefenseKind.RESISTANCE, "Fogo"), DamageDefense(DefenseKind.RESISTANCE, "Frio"),
            DamageDefense(DefenseKind.VULNERABILITY, "Radiante")
        )
        assertEquals("Resiste: Fogo, Frio • Vulnerável: Radiante", defensesSummary(d))
        assertEquals("", defensesSummary(emptyList()))
    }

    // --- salvaguardas ---

    @Test fun saveBonusAddsProficiencyOnlyWhenProficient() {
        val str = dnd.attributes[0]; val dex = dnd.attributes[1]
        val sheet = hero.copy(saveProficiencies = listOf("str"))
        assertEquals(5, saveBonus(sheet, str))  // mod +3 e prof +2
        assertEquals(2, saveBonus(sheet, dex))  // só o mod +2
    }

    @Test fun saveRollUsesTheSaveBonus() {
        val sheet = hero.copy(saveProficiencies = listOf("str"))
        val line = rollSaveCheck(engineWith(10), sheet, dnd, dnd.attributes[0])
        assertTrue(line.text.contains("= 15"))
    }

    // --- defesas no combate ---

    @Test fun adjustedDamageAppliesResistanceVulnerabilityAndImmunity() {
        val d = listOf(
            DamageDefense(DefenseKind.RESISTANCE, "Fogo"),
            DamageDefense(DefenseKind.VULNERABILITY, "Radiante"),
            DamageDefense(DefenseKind.IMMUNITY, "Venenoso")
        )
        assertEquals(3, adjustedDamage(7, "fogo", d))       // metade, arredondando para baixo
        assertEquals(14, adjustedDamage(7, "radiante", d))
        assertEquals(0, adjustedDamage(7, "venenoso", d))
        assertEquals(7, adjustedDamage(7, "frio", d))
        assertEquals(3, adjustedDamage(7, "de fogo", d))    // tolera "de fogo" vindo do texto da ação
    }

    @Test fun conditionImmunityTextIsMatchedIgnoringCase() {
        assertTrue(immuneToCondition("Enfeitiçado, amedrontado", "Amedrontado"))
        assertTrue(immuneToCondition("Cego e Surdo", "Surdo"))
        assertFalse(immuneToCondition("Cego", "Surdo"))
        assertFalse(immuneToCondition("", "Cego"))
    }

    @Test fun damageOnlyOutcomeKeepsTheDamageType() {
        val out = rollDamageOnly(engineWith(3), "Mordida", "1d4 perfurante", emptyMap())
        assertEquals("perfurante", out.damageType)
    }
}
