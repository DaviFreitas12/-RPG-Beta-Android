package com.example.rpgsombrio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelRulesTest {
    private val rules = levelRulesFor("dnd5e")!!
    private val keys = rules.attributeKeys

    /** Ficha D&D nível 1, já finalizada, com atributos dados e 10 PV. */
    private fun sheet(vararg attrs: Pair<String, Int>, hp: Int = 10, campaignId: String = "c1") = CharacterSheet(
        id = "s", name = "Ana", campaignId = campaignId, creation = CreationState.DONE,
        attributes = keys.associateWith { 10 } + attrs.toMap(),
        resources = mapOf("hp" to ResourceValue(hp, hp)),
        proficiencyBonus = 2
    )

    private fun up(s: CharacterSheet) = grantLevelUp(s, rules)!!

    /** Sobe e conclui um nível pela média fixa de um d8, gastando os pontos de atributo (se houver) em [asiKey]. */
    private fun upAndComplete(s: CharacterSheet, asiKey: String = "strength", die: Int = 8): CharacterSheet {
        val leveled = up(s)
        val points = pendingLevelUp(leveled)!!.asiGranted
        val out = completeLevelUp(leveled, rules, HpMethod.AVERAGE, die, null, if (points > 0) mapOf(asiKey to points) else emptyMap())
        assertNotNull(out.error, out.sheet)
        return out.sheet!!
    }

    // --- regras básicas ---

    @Test
    fun soODndTemRegrasDeNivel() {
        assertNotNull(levelRulesFor("dnd5e"))
        assertNotNull(levelRulesFor("dnd2024"))
        assertNull(levelRulesFor("vampire"))
        assertNull(levelRulesFor("custom"))
    }

    @Test
    fun aumentoDeAtributoSoNosNiveis4_8_12_16_19() {
        val com = (1..20).filter { asiPointsAt(rules, it) > 0 }
        assertEquals(listOf(4, 8, 12, 16, 19), com)
        com.forEach { assertEquals(2, asiPointsAt(rules, it)) }
    }

    @Test
    fun proficienciaPeloNivel() {
        val esperado = mapOf(1 to 2, 4 to 2, 5 to 3, 8 to 3, 9 to 4, 12 to 4, 13 to 5, 16 to 5, 17 to 6, 20 to 6)
        esperado.forEach { (nivel, prof) -> assertEquals("nível $nivel", prof, proficiencyForLevel(nivel)) }
        assertEquals(2, proficiencyForLevel(0))
    }

    @Test
    fun vidaPelaMediaFixa() {
        assertEquals(4, averageHp(6)); assertEquals(5, averageHp(8)); assertEquals(6, averageHp(10)); assertEquals(7, averageHp(12))
    }

    @Test
    fun padraoDaCampanhaEhMediaFixa() {
        assertEquals(HpMethod.AVERAGE, HpMethod.effective(""))
        assertEquals(HpMethod.ROLL, HpMethod.effective(HpMethod.ROLL))
        assertEquals("", HpMethod.normalize("lixo"))
        assertEquals("", Campaign(id = "c", name = "x", masterUserId = "m").hpMethod)
    }

    // --- travas de nível ---

    @Test
    fun nivelFicaTravadoSoEmCampanhaComRegra() {
        val camp = Campaign(id = "c1", name = "x", masterUserId = "m")
        assertTrue(levelLocked(sheet(), camp))
        assertFalse(levelLocked(sheet(), null))                                   // solta: livre
        assertFalse(levelLocked(sheet().copy(systemId = "vampire"), camp.copy(systemId = "vampire"))) // outro sistema: livre
    }

    @Test
    fun naoSobeDeNivelEmCriacaoNemDepoisDoNivel20() {
        assertNotNull(levelUpBlockReason(sheet().copy(creation = CreationState.DRAFT), rules))
        assertNull(grantLevelUp(sheet().copy(creation = CreationState.DRAFT), rules))
        assertNotNull(levelUpBlockReason(sheet().copy(level = 20), rules))
        assertNull(grantLevelUp(sheet().copy(level = 20), rules))
    }

    @Test
    fun naoSobeOutroNivelComLevelUpPendente() {
        val pendente = up(sheet())
        assertNotNull(pendingLevelUp(pendente))
        assertNull(grantLevelUp(pendente, rules))
    }

    @Test
    fun subirNivelAtualizaNivelProficienciaEHistorico() {
        val s = up(sheet().copy(level = 4, proficiencyBonus = 2))
        assertEquals(5, s.level)
        assertEquals(3, s.proficiencyBonus)
        assertEquals(LevelEntry(level = 5, asiGranted = 0), s.levelHistory.single())
        assertEquals(4, up(sheet().copy(level = 3)).levelHistory.last().level)
    }

    @Test
    fun subirNaoMexeNaVidaNemNosAtributosAteOJogadorConcluir() {
        val s = up(sheet())
        assertEquals(10, s.resources["hp"]!!.max)
        assertEquals(sheet().attributes, s.attributes)
    }

    // --- assistente de level up ---

    @Test
    fun mediaFixaSomaMetadeDoDadoMaisCon() {
        val s = up(sheet("constitution" to 14)) // +2
        val out = completeLevelUp(s, rules, HpMethod.AVERAGE, 8, null, emptyMap())
        val c = out.sheet!!
        assertEquals(5 + 2, c.levelHistory.last().hpGained)
        assertEquals(17, c.resources["hp"]!!.max)
        assertEquals(17, c.resources["hp"]!!.current)
        assertEquals(8, c.hitDie)
        assertNull(pendingLevelUp(c))
    }

    @Test
    fun rolagemUsaOValorDigitadoEExigeFaixaDoDado() {
        val s = up(sheet())
        assertNotNull(completeLevelUp(s, rules, HpMethod.ROLL, 10, null, emptyMap()).error)  // faltou digitar
        assertNotNull(completeLevelUp(s, rules, HpMethod.ROLL, 10, 11, emptyMap()).error)    // acima do d10
        assertNotNull(completeLevelUp(s, rules, HpMethod.ROLL, 10, 0, emptyMap()).error)
        val ok = completeLevelUp(s, rules, HpMethod.ROLL, 10, 7, emptyMap()).sheet!!
        assertEquals(7, ok.levelHistory.last().hpGained)
        assertEquals(17, ok.resources["hp"]!!.max)
    }

    @Test
    fun vidaGanhaNuncaFicaAbaixoDe1() {
        val s = up(sheet("constitution" to 3)) // mod -4
        val c = completeLevelUp(s, rules, HpMethod.ROLL, 6, 1, emptyMap()).sheet!!
        assertEquals(1, c.levelHistory.last().hpGained)
    }

    @Test
    fun dadoDeVidaInvalidoEhRecusado() {
        assertNotNull(completeLevelUp(up(sheet()), rules, HpMethod.AVERAGE, 7, null, emptyMap()).error)
    }

    @Test
    fun semLevelUpPendenteNaoConclui() {
        assertNotNull(completeLevelUp(sheet(), rules, HpMethod.AVERAGE, 8, null, emptyMap()).error)
    }

    @Test
    fun aumentoDeAtributoNoNivel4DaDoisPontos() {
        var s = sheet()
        repeat(2) { s = upAndComplete(s) }                  // níveis 2 e 3
        val n4 = up(s)
        assertEquals(2, pendingLevelUp(n4)!!.asiGranted)
        val ok = completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, mapOf("strength" to 1, "dexterity" to 1)).sheet!!
        assertEquals(11, ok.attributes["strength"]); assertEquals(11, ok.attributes["dexterity"])
        assertEquals(mapOf("strength" to 1, "dexterity" to 1), ok.levelHistory.last().asiApplied)
    }

    @Test
    fun exigeGastarTodosOsPontosEnaoPassaDoLimite() {
        var s = sheet()
        repeat(2) { s = upAndComplete(s) }
        val n4 = up(s)
        assertNotNull(completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, mapOf("strength" to 1)).error)   // faltou 1
        assertNotNull(completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, emptyMap()).error)               // faltaram 2
        assertNotNull(completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, mapOf("strength" to 3)).error)   // passou de 2
        assertNotNull(completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, mapOf("strength" to -1, "dexterity" to 3)).error)
        assertNotNull(completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, mapOf("sorte" to 2)).error)      // atributo que não existe
    }

    @Test
    fun nivelSemAumentoNaoAceitaPontos() {
        val s = up(sheet()) // nível 2
        assertNotNull(completeLevelUp(s, rules, HpMethod.AVERAGE, 8, null, mapOf("strength" to 1)).error)
    }

    @Test
    fun nenhumAtributoPassaDe20() {
        var s = sheet("strength" to 19)
        repeat(2) { s = upAndComplete(s, "dexterity") }
        val n4 = up(s)
        assertNotNull(completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, mapOf("strength" to 2)).error) // 19 + 2 = 21
        val ok = completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, mapOf("strength" to 1, "dexterity" to 1)).sheet!!
        assertEquals(20, ok.attributes["strength"])
    }

    @Test
    fun comTodosEm20SemPontosParaGastar() {
        var s = sheet(*keys.map { it to 20 }.toTypedArray())
        repeat(2) { s = upAndComplete(s) }
        val n4 = up(s)
        val ok = completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, emptyMap()) // não cabe nada: libera
        assertNotNull(ok.error, ok.sheet)
        assertTrue(ok.sheet!!.attributes.values.all { it <= 20 })
    }

    @Test
    fun aumentoDeConstituicaoVaiParaAVidaDoMesmoNivel() {
        var s = sheet("constitution" to 13)
        repeat(2) { s = upAndComplete(s, "constitution") }
        val hpAntes = s.resources["hp"]!!.max
        val n4 = up(s)
        val ok = completeLevelUp(n4, rules, HpMethod.AVERAGE, 8, null, mapOf("constitution" to 1, "strength" to 1)).sheet!! // CON 13 -> 14 (+2)
        assertEquals(5 + 2, ok.levelHistory.last().hpGained)
        assertEquals(hpAntes + 7, ok.resources["hp"]!!.max)
    }

    @Test
    fun anotacaoDeCaracteristicasFicaNoHistorico() {
        val ok = completeLevelUp(up(sheet()), rules, HpMethod.AVERAGE, 8, null, emptyMap(), "  Ataque Extra, Golpe  ").sheet!!
        assertEquals("Ataque Extra, Golpe", ok.levelHistory.last().note)
    }

    // --- remover nível ---

    @Test
    fun removerNivelDesfazVidaEAtributosExatamente() {
        var s = sheet()
        repeat(2) { s = upAndComplete(s) }
        val antes = s
        val n4 = upAndComplete(s, "wisdom") // +2 SAB e vida
        assertEquals(12, n4.attributes["wisdom"])
        assertEquals(antes, removeLevel(n4, rules))
        assertEquals(8, removeLevel(n4, rules)!!.hitDie) // o dado de vida da classe continua guardado
    }

    @Test
    fun removerNivelPendenteVoltaAoEstadoAnterior() {
        val antes = sheet()
        assertEquals(antes, removeLevel(up(antes), rules))
    }

    @Test
    fun removerNaoPassaDoNivel1() {
        assertNull(removeLevel(sheet(), rules))
    }

    @Test
    fun removerNivelQuePassouPorTetoDesfazSoOQueFoiAplicado() {
        var s = sheet("strength" to 19)
        repeat(2) { s = upAndComplete(s, "dexterity") }
        val n4 = completeLevelUp(up(s), rules, HpMethod.AVERAGE, 8, null, mapOf("strength" to 1, "dexterity" to 1)).sheet!!
        val voltou = removeLevel(n4, rules)!!
        assertEquals(19, voltou.attributes["strength"])
        assertEquals(s.attributes, voltou.attributes)
    }

    @Test
    fun removerNivelNaoDeixaVidaAtualAcimaDoMaximo() {
        val s = upAndComplete(sheet(hp = 10))
        val ferido = s.copy(resources = s.resources + ("hp" to ResourceValue(15, 15)))
        val voltou = removeLevel(ferido, rules)!!
        assertEquals(10, voltou.resources["hp"]!!.max)
        assertEquals(10, voltou.resources["hp"]!!.current)
    }

    @Test
    fun removerNivelSemHistoricoSoTiraONivel() {
        val antiga = sheet().copy(level = 5, proficiencyBonus = 3)
        val r = removeLevel(antiga, rules)!!
        assertEquals(4, r.level); assertEquals(2, r.proficiencyBonus)
        assertEquals(antiga.resources, r.resources); assertEquals(antiga.attributes, r.attributes)
    }

    @Test
    fun removerComProficienciaManualNaoMexeNela() {
        val s = grantLevelUp(sheet().copy(proficiencyBonus = 5), rules, autoProficiency = false)!!
        assertEquals(5, s.proficiencyBonus)
        assertEquals(5, removeLevel(s, rules, autoProficiency = false)!!.proficiencyBonus)
    }

    // --- do nível 1 ao 20 ---

    @Test
    fun sobeDoNivel1Ao20ERemoveDeVoltaAoNivel1() {
        val inicial = sheet("constitution" to 12)
        val copias = mutableListOf(inicial)
        var s = inicial
        for (nivel in 2..20) {
            val points = asiPointsAt(rules, nivel)
            val leveled = up(s)
            val alvo = keys[nivel % keys.size]
            val dist = if (points == 2) mapOf(alvo to 2) else emptyMap()
            val out = completeLevelUp(leveled, rules, HpMethod.AVERAGE, 10, null, dist)
            assertNotNull("nível $nivel: ${out.error}", out.sheet)
            s = out.sheet!!
            assertEquals(nivel, s.level)
            assertEquals(proficiencyForLevel(nivel), s.proficiencyBonus)
            assertTrue("nível $nivel passou de 20", s.attributes.values.all { it <= 20 })
            copias += s
        }
        assertEquals(20, s.level)
        assertEquals(5, s.levelHistory.count { it.asiApplied.isNotEmpty() }) // 4, 8, 12, 16, 19
        assertNull(grantLevelUp(s, rules))                                   // não existe nível 21

        // volta passo a passo e compara com o estado exato de cada nível
        for (nivel in 19 downTo 1) {
            s = removeLevel(s, rules)!!
            assertEquals("ao voltar para o nível $nivel", copias[nivel - 1].copy(hitDie = 0), s.copy(hitDie = 0))
        }
        assertEquals(inicial, s.copy(hitDie = 0))
        assertNull(removeLevel(s, rules))
    }

    @Test
    fun nenhumAtributoPassaDe20AoLongoDeTodosOsNiveis() {
        // o jogador põe tudo em FOR: 12 +2*5 = 22 não cabe, então o teto segura em 20
        var s = sheet("strength" to 12)
        for (nivel in 2..20) {
            val leveled = up(s)
            val pontos = pendingLevelUp(leveled)!!.asiGranted
            val sobra = 20 - (leveled.attributes["strength"] ?: 0)
            val dist = if (pontos == 0) emptyMap() else if (sobra >= pontos) mapOf("strength" to pontos)
                else if (sobra > 0) mapOf("strength" to sobra, "dexterity" to pontos - sobra) else mapOf("dexterity" to pontos)
            val out = completeLevelUp(leveled, rules, HpMethod.AVERAGE, 8, null, dist)
            assertNotNull("nível $nivel: ${out.error}", out.sheet)
            s = out.sheet!!
            assertTrue(s.attributes.values.all { it <= 20 })
        }
        assertEquals(20, s.attributes["strength"])
    }

    // --- criação e histórico convivem ---

    @Test
    fun reabrirACriacaoNaoPerdeOsAumentosDeNivel() {
        val keysAttr = keys
        val creationRule = creationRuleFor("dnd5e")
        val s0 = finalizeCreation(startCreation(CharacterSheet(id = "s"), creationRule, keysAttr).copy(
            baseAttributes = keysAttr.zip(listOf(15, 14, 13, 12, 10, 8)).toMap()
        ), creationRule, keysAttr)!!
            .copy(resources = mapOf("hp" to ResourceValue(10, 10)), campaignId = "c1")
        var s = s0
        repeat(2) { s = upAndComplete(s, "strength") }
        s = upAndComplete(s, "strength") // nível 4: FOR 15 + 2
        assertEquals(17, s.attributes["strength"])
        val reaberta = reopenCreation(s, creationRule, keysAttr)
        val recalculada = applyCreationScores(reaberta, creationRule, keysAttr)
        assertEquals(17, recalculada.attributes["strength"]) // o aumento do nível continua
        assertEquals(15, recalculada.baseAttributes["strength"])
    }

    @Test
    fun resumoDoHistorico() {
        val e = LevelEntry(level = 4, hpGained = 9, asiGranted = 2, asiApplied = mapOf("strength" to 2), done = true)
        assertEquals("Nível 4: +9 PV, +2 FOR", levelEntrySummary(e, mapOf("strength" to "FOR")))
        assertEquals("Nível 5: aguardando o jogador", levelEntrySummary(LevelEntry(level = 5)))
    }
}
