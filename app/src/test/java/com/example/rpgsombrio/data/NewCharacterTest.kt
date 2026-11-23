package com.example.rpgsombrio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewCharacterTest {
    private val str = SheetAttr("str", "Força", "FOR", 10, true)
    private val dex = SheetAttr("dex", "Destreza", "DES", 12, true)
    private val profile = SystemProfile(
        id = "dnd", name = "D&D",
        attributes = listOf(str, dex),
        resources = listOf(SheetResource("hp", "Pontos de vida", 10)),
        skillTemplates = listOf(SkillTemplate("Atletismo", "str")),
        usesLevels = true, usesArmorClass = true, usesSkills = true, autoProficiency = true,
        conceptLabel = "Classe", originLabel = "Raça"
    )

    @Test
    fun novaFichaTrazOsPadroesDoSistema() {
        val sheet = buildNewCharacter(profile, "id-1", "Davi")
        assertEquals("id-1", sheet.id)
        assertEquals("Davi", sheet.name)
        assertEquals("dnd", sheet.systemId)
        assertEquals(mapOf("str" to 10, "dex" to 12), sheet.attributes)
        assertEquals(ResourceValue(10, 10), sheet.resources["hp"])
        assertEquals(listOf("Atletismo"), sheet.skills.map { it.name })
        assertEquals("str", sheet.skills.first().attributeKey)
        assertTrue(sheet.hiddenAttributes.isEmpty())
    }

    @Test
    fun semOcultosMostraTodosOsAtributos() {
        val sheet = buildNewCharacter(profile, "id", "Davi")
        assertEquals(listOf("str", "dex"), profile.visibleAttributes(sheet).map { it.key })
    }

    @Test
    fun atributoOcultoSaiDaFichaMasOValorFica() {
        val sheet = buildNewCharacter(profile, "id", "Davi").copy(hiddenAttributes = listOf("dex"))
        assertEquals(listOf("str"), profile.visibleAttributes(sheet).map { it.key })
        assertEquals(12, sheet.attributes["dex"])
    }

    @Test
    fun ocultarTodosDeixaAListaVazia() {
        val sheet = buildNewCharacter(profile, "id", "Davi").copy(hiddenAttributes = listOf("str", "dex"))
        assertTrue(profile.visibleAttributes(sheet).isEmpty())
    }

    @Test
    fun abaInicialDaFicha() {
        assertEquals("Perfil", initialSheetTab(startOnProfile = true, name = "Davi"))
        assertEquals("Perfil", initialSheetTab(startOnProfile = false, name = "   "))
        assertEquals("Ficha", initialSheetTab(startOnProfile = false, name = "Davi"))
    }
}
