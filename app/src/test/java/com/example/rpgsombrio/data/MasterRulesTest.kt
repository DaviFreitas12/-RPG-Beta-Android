package com.example.rpgsombrio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Poderes e exceções do mestre (0.10.5, etapa 6). */
class MasterRulesTest {
    private val rules = levelRulesFor("dnd5e")!!
    private val keys = rules.attributeKeys
    private val creationRule = creationRuleFor("dnd5e")

    private val master = "mestre-1"
    private val player = "jogador-1"
    private val other = "jogador-2"
    private val campaign = Campaign(id = "c1", name = "Sombras", masterUserId = master, systemId = "dnd5e")

    /** Ficha D&D nível 1, finalizada, com base 10 em tudo e 10 PV. */
    private fun sheet(vararg attrs: Pair<String, Int>, owner: String = "", campaignId: String = "c1") = CharacterSheet(
        id = "s-$owner", name = "Ana", campaignId = campaignId, ownerUserId = owner, creation = CreationState.DONE,
        attributes = keys.associateWith { 10 } + attrs.toMap(),
        baseAttributes = keys.associateWith { 10 } + attrs.toMap(),
        resources = mapOf("hp" to ResourceValue(10, 10)),
        proficiencyBonus = 2
    )

    /** Sobe e conclui um nível pela média fixa de um d8, gastando os pontos de atributo (se houver) em [asiKey]. */
    private fun upAndComplete(s: CharacterSheet, asiKey: String = "strength"): CharacterSheet {
        val leveled = grantLevelUp(s, rules)!!
        val points = pendingLevelUp(leveled)!!.asiGranted
        val out = completeLevelUp(leveled, rules, HpMethod.AVERAGE, 8, null, if (points > 0) mapOf(asiKey to points) else emptyMap())
        assertNotNull(out.error, out.sheet)
        return out.sheet!!
    }

    private fun levelTo(s: CharacterSheet, level: Int): CharacterSheet {
        var next = s
        while (next.level < level) next = upAndComplete(next)
        return next
    }

    // --- quem é o mestre ---

    @Test
    fun soOMestreDaCampanhaLigaAEdicaoLivre() {
        assertTrue(canUseFreeEdit(campaign, master))
        assertFalse(canUseFreeEdit(campaign, player))
        assertFalse("ficha solta não tem mestre", canUseFreeEdit(null, master))
        assertTrue(isCampaignMaster(campaign, master))
        assertFalse(isCampaignMaster(null, master))
    }

    @Test
    fun reabrirACriacaoContinuaSoParaOMestreOuFichaSolta() {
        assertTrue(canReopenCreation(campaign, master))
        assertFalse(canReopenCreation(campaign, player))
        assertTrue(canReopenCreation(null, player))
    }

    // --- edição livre ---

    @Test
    fun edicaoLivreIgnoraATravaDeAtributos() {
        val s = sheet()
        assertTrue(attributesLocked(s, creationRule))
        assertTrue(attributesLockedFor(s, creationRule, freeEdit = false))
        assertFalse(attributesLockedFor(s, creationRule, freeEdit = true))
    }

    @Test
    fun edicaoLivreNaoMudaFichaEmCriacao() {
        val drafting = sheet().copy(creation = CreationState.DRAFT)
        assertFalse(attributesLockedFor(drafting, creationRule, freeEdit = false))
        assertFalse(attributesLockedFor(drafting, creationRule, freeEdit = true))
    }

    @Test
    fun nivelLivreParaCimaMudaSoNivelEProficiencia() {
        val s = sheet()
        val up = freeSetLevel(s, rules, 9)
        assertEquals(9, up.level)
        assertEquals(4, up.proficiencyBonus)
        assertEquals(s.attributes, up.attributes)
        assertEquals(s.resources, up.resources)
        assertTrue(up.levelHistory.isEmpty())
    }

    @Test
    fun nivelLivreParaBaixoDesfazExatamenteOQueOsNiveisDeram() {
        val l1 = sheet()
        val l5 = levelTo(l1, 5)
        val l3 = levelTo(l1, 3)
        assertEquals(l3, freeSetLevel(l5, rules, 3))
        val back = freeSetLevel(l5, rules, 1)
        assertEquals(1, back.level)
        assertEquals(l1.attributes, back.attributes)
        assertEquals(l1.resources, back.resources)
        assertTrue(back.levelHistory.isEmpty())
        assertEquals(2, back.proficiencyBonus)
    }

    @Test
    fun nivelLivreFicaEntre1EOMaximo() {
        val s = sheet()
        assertEquals(20, freeSetLevel(s, rules, 99).level)
        assertEquals(1, freeSetLevel(levelTo(s, 3), rules, 0).level)
        assertEquals(1, freeSetLevel(s, rules, -4).level)
        assertEquals(s, freeSetLevel(s, rules, 1))
    }

    @Test
    fun nivelLivreSemAutoProficienciaNaoMexeNaProficiencia() {
        val s = sheet().copy(proficiencyBonus = 5)
        assertEquals(5, freeSetLevel(s, rules, 8, autoProficiency = false).proficiencyBonus)
    }

    // --- aumento de atributo extra ---

    @Test
    fun soODndTemAumentoExtra() {
        assertNull(grantExtraAsi(sheet(), null, 2, "", "g1"))
        assertNotNull(asiGrantBlockReason(sheet(), null))
        assertNotNull(grantExtraAsi(sheet(), rules, 2, "", "g1"))
    }

    @Test
    fun naoConcedeEnquantoACriacaoNaoFoiFinalizada() {
        val drafting = sheet().copy(creation = CreationState.DRAFT)
        assertNotNull(asiGrantBlockReason(drafting, rules))
        assertNull(grantExtraAsi(drafting, rules, 2, "", "g1"))
    }

    @Test
    fun concessaoTemLimiteDePontosEIdUnico() {
        val s = sheet()
        assertNull(grantExtraAsi(s, rules, 0, "", "g1"))
        assertNull(grantExtraAsi(s, rules, MAX_ASI_GRANT + 1, "", "g1"))
        assertNotNull(grantExtraAsi(s, rules, MAX_ASI_GRANT, "", "g1"))
        val once = grantExtraAsi(s, rules, 2, "Guerreiro nível 6", "g1")!!
        assertNull("mesmo id não entra duas vezes", grantExtraAsi(once, rules, 2, "", "g1"))
        assertEquals("Guerreiro nível 6", once.asiGrants.single().reason)
        assertEquals(1, pendingAsiGrants(once).size)
    }

    @Test
    fun jogadorDistribuiOsPontosConcedidos() {
        val granted = grantExtraAsi(sheet(), rules, 2, "Guerreiro nível 6", "g1")!!
        val out = completeAsiGrant(granted, rules, "g1", mapOf("strength" to 1, "constitution" to 1))
        assertNotNull(out.error, out.sheet)
        val done = out.sheet!!
        assertEquals(11, done.attributes["strength"])
        assertEquals(11, done.attributes["constitution"])
        assertEquals(10, done.attributes["dexterity"])
        assertTrue(pendingAsiGrants(done).isEmpty())
        assertEquals(mapOf("strength" to 1, "constitution" to 1), done.asiGrants.single().applied)
    }

    @Test
    fun precisaGastarTodosOsPontosEnaoPassaDoTeto() {
        val granted = grantExtraAsi(sheet("strength" to 19), rules, 2, "", "g1")!!
        assertNotNull("sobrou ponto", completeAsiGrant(granted, rules, "g1", mapOf("strength" to 1)).error)
        assertNotNull("estourou o teto", completeAsiGrant(granted, rules, "g1", mapOf("strength" to 2)).error)
        assertNotNull("gastou mais que o concedido", completeAsiGrant(granted, rules, "g1", mapOf("dexterity" to 3)).error)
        assertNotNull("negativo", completeAsiGrant(granted, rules, "g1", mapOf("strength" to 3, "dexterity" to -1)).error)
        assertNotNull("atributo que não existe", completeAsiGrant(granted, rules, "g1", mapOf("sorte" to 2)).error)
        val ok = completeAsiGrant(granted, rules, "g1", mapOf("strength" to 1, "dexterity" to 1))
        assertEquals(20, ok.sheet!!.attributes["strength"])
    }

    @Test
    fun quandoNaoCabeNaoExigeGastarTudo() {
        // Todos no teto menos um atributo com 1 ponto de folga: só 1 dos 2 pontos cabe.
        val almost = sheet(*keys.map { it to 20 }.toTypedArray(), "wisdom" to 19)
        val granted = grantExtraAsi(almost, rules, 2, "", "g1")!!
        val out = completeAsiGrant(granted, rules, "g1", mapOf("wisdom" to 1))
        assertNotNull(out.error, out.sheet)
        assertEquals(20, out.sheet!!.attributes["wisdom"])
        // Já no teto em tudo: pode concluir sem gastar nada.
        val capped = grantExtraAsi(sheet(*keys.map { it to 20 }.toTypedArray()), rules, 2, "", "g2")!!
        assertNotNull(completeAsiGrant(capped, rules, "g2", emptyMap()).sheet)
    }

    @Test
    fun naoDistribuiConcessaoQueNaoExisteOuJaFoiGasta() {
        val granted = grantExtraAsi(sheet(), rules, 2, "", "g1")!!
        assertNotNull(completeAsiGrant(granted, rules, "outra", mapOf("strength" to 2)).error)
        val done = completeAsiGrant(granted, rules, "g1", mapOf("strength" to 2)).sheet!!
        assertNotNull("não gasta duas vezes", completeAsiGrant(done, rules, "g1", mapOf("strength" to 2)).error)
    }

    @Test
    fun retirarConcessaoPendenteNaoMexeNosAtributos() {
        val granted = grantExtraAsi(sheet(), rules, 2, "", "g1")!!
        val back = revokeAsiGrant(granted, "g1")!!
        assertTrue(back.asiGrants.isEmpty())
        assertEquals(granted.attributes, back.attributes)
        assertNull(revokeAsiGrant(back, "g1"))
    }

    @Test
    fun retirarConcessaoGastaDesfazExatamente() {
        val original = sheet("strength" to 14)
        val granted = grantExtraAsi(original, rules, 2, "Ladino nível 10", "g1")!!
        val done = completeAsiGrant(granted, rules, "g1", mapOf("dexterity" to 1, "strength" to 1)).sheet!!
        val back = revokeAsiGrant(done, "g1")!!
        assertEquals(original.attributes, back.attributes)
        assertTrue(back.asiGrants.isEmpty())
    }

    @Test
    fun concessaoEntraNosAumentosEResisteAReabrirACriacao() {
        val granted = grantExtraAsi(sheet("strength" to 14), rules, 2, "", "g1")!!
        val done = completeAsiGrant(granted, rules, "g1", mapOf("strength" to 2)).sheet!!
        assertEquals(mapOf("strength" to 2), attributeImprovements(done))
        val reopened = reopenCreation(done, creationRule, keys)
        assertEquals(CreationState.DRAFT, reopened.creation)
        // Reabrir recalcula os valores finais a partir de base + bônus + aumentos: o aumento do mestre não se perde.
        assertEquals(16, applyCreationScores(reopened, creationRule, keys).attributes["strength"])
    }

    @Test
    fun aumentoDoNivelEConcessaoSomam() {
        val l4 = levelTo(sheet(), 4) // +2 FOR no nível 4
        val granted = grantExtraAsi(l4, rules, 2, "", "g1")!!
        val done = completeAsiGrant(granted, rules, "g1", mapOf("strength" to 2)).sheet!!
        assertEquals(mapOf("strength" to 4), attributeImprovements(done))
        assertEquals(14, done.attributes["strength"])
    }

    @Test
    fun removerNivelNaoMexeNasConcessoesDoMestre() {
        val l4 = levelTo(sheet(), 4)
        val granted = grantExtraAsi(l4, rules, 2, "", "g1")!!
        val done = completeAsiGrant(granted, rules, "g1", mapOf("dexterity" to 2)).sheet!!
        val lower = removeLevel(done, rules)!!
        assertEquals(3, lower.level)
        assertEquals(12, lower.attributes["dexterity"])
        assertEquals(10, lower.attributes["strength"])
        assertEquals(1, lower.asiGrants.size)
    }

    @Test
    fun resumoDaConcessao() {
        val labels = mapOf("strength" to "FOR", "constitution" to "CON")
        val granted = grantExtraAsi(sheet(), rules, 2, "Guerreiro nível 6", "g1")!!
        assertEquals("+2 pontos (Guerreiro nível 6): aguardando o jogador", asiGrantSummary(granted.asiGrants.single(), labels))
        val done = completeAsiGrant(granted, rules, "g1", mapOf("strength" to 1, "constitution" to 1)).sheet!!
        assertEquals("+2 pontos (Guerreiro nível 6): +1 FOR, +1 CON", asiGrantSummary(done.asiGrants.single(), labels))
        assertEquals("+3 pontos: aguardando o jogador", asiGrantSummary(AsiGrant("x", 3)))
    }

    // --- visibilidade ---

    @Test
    fun mestreVeTodasAsFichasDaCampanha() {
        val a = sheet(owner = player)
        val b = sheet(owner = other)
        assertTrue(canViewSheet(a, campaign, master))
        assertTrue(canViewSheet(b, campaign, master))
    }

    @Test
    fun jogadorVeSoAsProprias() {
        val mine = sheet(owner = player)
        val theirs = sheet(owner = other)
        val ownerless = sheet(owner = "")
        assertTrue(canViewSheet(mine, campaign, player))
        assertFalse(canViewSheet(theirs, campaign, player))
        assertTrue("ficha antiga, sem dono gravado, vale como do aparelho", canViewSheet(ownerless, campaign, player))
    }

    @Test
    fun fichaSoltaSempreApareceParaQuemTemOAparelho() {
        val loose = sheet(owner = other, campaignId = "")
        assertTrue(canViewSheet(loose, null, player))
        assertTrue(canViewSheet(loose, null, master))
    }

    @Test
    fun visibleSheetsAvaliaCadaFichaNaSuaCampanha() {
        val mine = campaign.copy(id = "mine", masterUserId = player) // aqui o usuário é mestre
        val sheets = listOf(
            sheet(owner = other, campaignId = "c1").copy(id = "1"),     // campanha de outro mestre, ficha de outro: escondida
            sheet(owner = player, campaignId = "c1").copy(id = "2"),    // própria: visível
            sheet(owner = other, campaignId = "mine").copy(id = "3"),   // minha campanha: visível (sou o mestre)
            sheet(owner = other, campaignId = "").copy(id = "4"),       // solta: visível
            sheet(owner = other, campaignId = "sumiu").copy(id = "5")   // campanha que não existe mais = solta: visível
        )
        assertEquals(listOf("2", "3", "4", "5"), visibleSheets(sheets, listOf(campaign, mine), player).map { it.id })
        // O mestre de c1 vê tudo de c1, mas não a ficha alheia da campanha "mine" (onde é só jogador).
        assertEquals(listOf("1", "2", "4", "5"), visibleSheets(sheets, listOf(campaign, mine), master).map { it.id })
    }

    @Test
    fun ferramentasDeMestreSoNaCampanhaOndeEMestre() {
        assertTrue(masterToolsAvailable(campaign, master))
        assertFalse(masterToolsAvailable(campaign, player))
        assertTrue("sem campanha ativa nada é escondido", masterToolsAvailable(null, player))
    }
}
