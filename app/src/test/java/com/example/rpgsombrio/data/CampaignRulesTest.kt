package com.example.rpgsombrio.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignRulesTest {
    private fun camp(id: String, name: String, master: String = "eu", system: String = "dnd5e", archived: Boolean = false) =
        Campaign(id = id, name = name, masterUserId = master, systemId = system, isArchived = archived)

    private fun sheet(id: String, campaignId: String = "", system: String = "dnd5e") =
        CharacterSheet(id = id, name = id, systemId = system, campaignId = campaignId)

    @Test
    fun quemCriaACampanhaEhOMestre() {
        val c = camp("c1", "Sombras", master = "eu")
        assertEquals(CampaignRole.MASTER, campaignRoleOf(c, "eu"))
        assertEquals(CampaignRole.PLAYER, campaignRoleOf(c, "outro"))
        assertEquals("Mestre", CampaignRole.label(CampaignRole.MASTER))
        assertEquals("Jogador", CampaignRole.label(CampaignRole.PLAYER))
    }

    @Test
    fun migracaoLevaNomeDescricaoESistemaAntigos() {
        val c = legacyCampaign("id-1", "  Noites Longas ", " Uma história. ", "vampire", "eu", "4321")
        assertEquals("id-1", c.id)
        assertEquals("Noites Longas", c.name)
        assertEquals("Uma história.", c.description)
        assertEquals("vampire", c.systemId)
        assertEquals("eu", c.masterUserId)
        assertEquals("4321", c.inviteCode)
        assertFalse(c.isArchived)
    }

    @Test
    fun migracaoSemDadosAntigosUsaOPadrao() {
        val c = legacyCampaign("id-1", null, null, "dnd5e", "eu", "1000")
        assertEquals(DEFAULT_CAMPAIGN_NAME, c.name)
        assertEquals("", c.description)
        val vazio = legacyCampaign("id-2", "   ", "", "dnd5e", "eu", "1000")
        assertEquals(DEFAULT_CAMPAIGN_NAME, vazio.name)
    }

    @Test
    fun codigoDeConviteTemQuatroDigitos() {
        val random = Random(7)
        repeat(100) {
            val code = newInviteCode(random)
            assertEquals(4, code.length)
            assertTrue(code.all { it.isDigit() })
        }
    }

    @Test
    fun campanhaAtivaPulaArquivadaEInexistente() {
        val a = camp("a", "A", archived = true)
        val b = camp("b", "B")
        val c = camp("c", "C")
        assertEquals("c", activeCampaignOf(listOf(a, b, c), "c")?.id)
        assertEquals("b", activeCampaignOf(listOf(a, b, c), "a")?.id)
        assertEquals("b", activeCampaignOf(listOf(a, b, c), "nao-existe")?.id)
        assertEquals("b", activeCampaignOf(listOf(a, b, c), null)?.id)
        assertNull(activeCampaignOf(listOf(a), "a"))
        assertNull(activeCampaignOf(emptyList(), null))
    }

    @Test
    fun listaAgrupaPorCampanhaComSoltasPorUltimo() {
        val alpha = camp("a", "Alpha")
        val beta = camp("b", "Beta")
        val velha = camp("c", "Velha", archived = true)
        val vazia = camp("d", "Vazia", archived = true)
        val sheets = listOf(
            sheet("s1", "a"), sheet("s2", "c"), sheet("s3"), sheet("s4", "campanha-apagada")
        )
        val groups = groupSheetsByCampaign(sheets, listOf(alpha, beta, velha, vazia), activeId = "b")
        // ativa primeiro, depois por nome, arquivadas depois; arquivada sem fichas some; soltas por último
        assertEquals(listOf("b", "a", "c", null), groups.map { it.campaign?.id })
        assertEquals(emptyList<String>(), groups[0].sheets.map { it.id })
        assertEquals(listOf("s1"), groups[1].sheets.map { it.id })
        assertEquals(listOf("s2"), groups[2].sheets.map { it.id })
        assertEquals(listOf("s3", "s4"), groups[3].sheets.map { it.id })
    }

    @Test
    fun semIncluirVaziasSoAparecemCampanhasComFichas() {
        val alpha = camp("a", "Alpha")
        val beta = camp("b", "Beta")
        val groups = groupSheetsByCampaign(listOf(sheet("s1", "a")), listOf(alpha, beta), includeEmpty = false)
        assertEquals(listOf<String?>("a"), groups.map { it.campaign?.id })
    }

    @Test
    fun semFichasSoltasNaoHaGrupoSemCampanha() {
        val groups = groupSheetsByCampaign(listOf(sheet("s1", "a")), listOf(camp("a", "Alpha")))
        assertEquals(1, groups.size)
        assertNull(groups.firstOrNull { it.campaign == null })
    }

    @Test
    fun sistemaDaCampanhaFicaFixoComPersonagens() {
        val sheets = listOf(sheet("s1", "a"), sheet("s2"))
        assertFalse(canChangeCampaignSystem("a", sheets))
        assertTrue(canChangeCampaignSystem("b", sheets))
        assertTrue(canChangeCampaignSystem("a", emptyList()))
    }

    @Test
    fun excluirCampanhaSoltaAsFichasSemApagar() {
        val sheets = listOf(sheet("s1", "a"), sheet("s2", "b"), sheet("s3"))
        val released = releaseSheets("a", sheets)
        assertEquals(3, released.size)
        assertEquals(listOf("", "b", ""), released.map { it.campaignId })
        assertEquals(listOf("s1", "s2", "s3"), released.map { it.id })
    }

    @Test
    fun moverParaCampanhaDeOutroSistemaTrocaOSistema() {
        val dnd = sheet("s1", system = "dnd5e")
        assertTrue(sheetMoveChangesSystem(dnd, camp("v", "Vampiros", system = "vampire")))
        assertFalse(sheetMoveChangesSystem(dnd, camp("d", "D&D", system = "dnd5e")))
        assertFalse(sheetMoveChangesSystem(dnd, null))
    }

    @Test
    fun fichaNovaNasceSemCampanha() {
        assertEquals("", CharacterSheet(id = "x").campaignId)
    }
}
