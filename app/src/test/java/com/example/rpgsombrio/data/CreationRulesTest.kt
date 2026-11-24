package com.example.rpgsombrio.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CreationRulesTest {
    private val keys = listOf("strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma")
    private val point = creationRuleFor("dnd5e")
    private val array = creationRuleFor("dnd5e", CreationMethod.STANDARD_ARRAY)
    private val roll = creationRuleFor("dnd2024", CreationMethod.ROLL)

    private fun blank(creation: String = "") = CharacterSheet(id = "s1", name = "Ana", creation = creation)
    private fun draft(rule: CreationRule = point) = startCreation(blank(), rule, keys)

    /** Aplica a base de uma vez (para testar a conferência sem passar pelos botões). */
    private fun withBase(sheet: CharacterSheet, vararg values: Int) =
        sheet.copy(baseAttributes = keys.zip(values.toList()).toMap())

    // --- tabela de custos e regra padrão ---

    @Test
    fun tabelaDeCustosDoDnd() {
        val esperado = mapOf(8 to 0, 9 to 1, 10 to 2, 11 to 3, 12 to 4, 13 to 5, 14 to 7, 15 to 9)
        esperado.forEach { (valor, custo) -> assertEquals("custo de $valor", custo, point.costOf(valor)) }
        assertNull(point.costOf(7))
        assertNull(point.costOf(16))
        assertEquals(27, point.budget)
        assertEquals(8..15, point.baseRange)
        assertEquals(20, point.finalCap)
    }

    @Test
    fun arrayPadraoCustaExatamenteOOrcamento() {
        assertEquals(listOf(15, 14, 13, 12, 10, 8), DND_STANDARD_ARRAY)
        assertEquals(27, DND_STANDARD_ARRAY.sumOf { point.costOf(it)!! })
    }

    @Test
    fun dndUsaCompraDePontosPorPadraoEOutrosSistemasSaoLivres() {
        assertEquals(CreationMethod.POINT_BUY, creationRuleFor("dnd5e").method)
        assertEquals(CreationMethod.POINT_BUY, creationRuleFor("dnd2024").method)
        assertTrue(creationRuleFor("vampire").isFree)
        assertTrue(creationRuleFor("custom").isFree)
        assertTrue(creationRuleFor("meu-sistema-criado-pelo-usuario").isFree)
    }

    @Test
    fun sistemaSemRegraIgnoraMetodoEscolhidoQueNaoSuporta() {
        assertTrue(creationRuleFor("vampire", CreationMethod.POINT_BUY).isFree)
        assertEquals(listOf(CreationMethod.FREE), availableCreationMethods("vampire"))
        assertEquals(CreationMethod.all, availableCreationMethods("dnd5e"))
        assertEquals(CreationMethod.ROLL, creationRuleFor("dnd5e", CreationMethod.ROLL).method)
        assertTrue(creationRuleFor("dnd5e", CreationMethod.FREE).isFree)
        // texto desconhecido no JSON volta ao padrão
        assertEquals("", CreationMethod.normalize("lixo"))
        assertEquals(CreationMethod.POINT_BUY, creationRuleFor("dnd5e", CreationMethod.normalize("lixo")).method)
    }

    // --- compra de pontos ---

    @Test
    fun criacaoNovaComecaComTodosEm8SemPontosGastos() {
        val s = draft()
        assertEquals(CreationState.DRAFT, s.creation)
        keys.forEach {
            assertEquals(8, s.baseAttributes[it])
            assertEquals(8, s.attributes[it])
        }
        val check = checkCreation(point, s.baseAttributes, keys)
        assertTrue(check.valid)
        assertEquals(0, check.spent)
        assertEquals(27, check.remaining)
    }

    @Test
    fun comprarPontosGastaEAtualizaOValorFinal() {
        var s = draft()
        repeat(7) { s = stepBaseScore(s, point, keys, "strength", +1) } // 8 -> 15 = 9 pontos
        assertEquals(15, s.baseAttributes["strength"])
        assertEquals(15, s.attributes["strength"])
        assertEquals(9, checkCreation(point, s.baseAttributes, keys).spent)
    }

    @Test
    fun naoDeixaPassarDe15NemDeAbaixoDe8() {
        var s = draft()
        assertFalse(canStepBase(s, point, keys, "strength", -1)) // já está em 8
        assertSame(s, stepBaseScore(s, point, keys, "strength", -1))
        repeat(7) { s = stepBaseScore(s, point, keys, "strength", +1) }
        assertFalse(canStepBase(s, point, keys, "strength", +1)) // já está em 15
        assertEquals(15, stepBaseScore(s, point, keys, "strength", +1).baseAttributes["strength"])
    }

    @Test
    fun naoDeixaGastarMaisDe27Pontos() {
        var s = draft()
        // três atributos em 15 = 27 pontos
        listOf("strength", "dexterity", "constitution").forEach { k -> repeat(7) { s = stepBaseScore(s, point, keys, k, +1) } }
        assertEquals(27, checkCreation(point, s.baseAttributes, keys).spent)
        assertFalse(canStepBase(s, point, keys, "intelligence", +1)) // 8 -> 9 custa 1 e não sobra nada
        val tentativa = stepBaseScore(s, point, keys, "intelligence", +1)
        assertEquals(8, tentativa.baseAttributes["intelligence"])
    }

    @Test
    fun passoCaroNaoCabeNoQueSobra() {
        var s = draft()
        listOf("strength", "dexterity", "constitution").forEach { k -> repeat(6) { s = stepBaseScore(s, point, keys, k, +1) } } // 3 x 14 = 21
        assertEquals(21, checkCreation(point, s.baseAttributes, keys).spent)
        repeat(5) { s = stepBaseScore(s, point, keys, "intelligence", +1) } // 8 -> 13 = 5 pontos (26)
        assertEquals(26, checkCreation(point, s.baseAttributes, keys).spent)
        assertTrue(canStepBase(s, point, keys, "wisdom", +1))   // 8 -> 9 custa 1 (27)
        assertFalse(canStepBase(s, point, keys, "intelligence", +1)) // 13 -> 14 custa 2 e só sobra 1
    }

    @Test
    fun devolverPontoLiberaOrcamento() {
        var s = draft()
        listOf("strength", "dexterity", "constitution").forEach { k -> repeat(7) { s = stepBaseScore(s, point, keys, k, +1) } }
        s = stepBaseScore(s, point, keys, "strength", -1) // 15 -> 14 devolve 2
        assertEquals(25, checkCreation(point, s.baseAttributes, keys).spent)
        assertTrue(canStepBase(s, point, keys, "intelligence", +1))
    }

    @Test
    fun naoFinalizaFichaComMaisDe27PontosGastos() {
        // base estourada de propósito (como se viesse de dado corrompido ou edição antiga)
        val s = withBase(draft(), 15, 15, 15, 15, 8, 8)
        val check = checkCreation(point, s.baseAttributes, keys)
        assertFalse(check.valid)
        assertEquals(36, check.spent)
        assertTrue(check.problems.any { it.contains("36 de 27") })
        assertNull(finalizeCreation(s, point, keys))
    }

    @Test
    fun valorForaDaFaixaTambemBarraAFinalizacao() {
        val s = withBase(draft(), 16, 8, 8, 8, 8, 8)
        assertFalse(checkCreation(point, s.baseAttributes, keys).valid)
        assertNull(finalizeCreation(s, point, keys))
        val abaixo = withBase(draft(), 7, 8, 8, 8, 8, 8)
        assertFalse(checkCreation(point, abaixo.baseAttributes, keys).valid)
    }

    @Test
    fun finalizaComExatamente27PontosEComSobra() {
        val exato = withBase(draft(), 15, 14, 13, 12, 10, 8)
        assertTrue(checkCreation(point, exato.baseAttributes, keys).valid)
        assertNotNull(finalizeCreation(exato, point, keys))
        val sobra = withBase(draft(), 10, 10, 10, 10, 10, 10) // 12 pontos
        assertEquals(12, checkCreation(point, sobra.baseAttributes, keys).spent)
        assertEquals(CreationState.DONE, finalizeCreation(sobra, point, keys)!!.creation)
    }

    // --- bônus de raça e antecedente ---

    @Test
    fun bonusEntraDepoisDaCompraEPontosContinuamExatos() {
        var s = draft()
        repeat(7) { s = stepBaseScore(s, point, keys, "strength", +1) }   // base 15
        s = setAttributeBonus(s, point, keys, "strength", 2)               // raça +2
        assertEquals(15, s.baseAttributes["strength"])
        assertEquals(17, s.attributes["strength"])
        assertEquals(9, checkCreation(point, s.baseAttributes, keys).spent) // pontos não mudam com o bônus
    }

    @Test
    fun valorFinalTemTetoDe20() {
        var s = draft()
        repeat(7) { s = stepBaseScore(s, point, keys, "strength", +1) }   // 15
        s = setAttributeBonus(s, point, keys, "strength", 6)               // 21 -> teto 20
        assertEquals(20, s.attributes["strength"])
        assertEquals(15, s.baseAttributes["strength"]) // a base continua exata
        assertEquals(20, finalScore(point, 15, 99.coerceAtMost(BONUS_MAX)))
        assertEquals(20, finalScore(point, 18, 2))
        assertEquals(19, finalScore(point, 18, 1))
    }

    @Test
    fun bonusTemLimiteEPodeSerNegativo() {
        val s = draft()
        assertEquals(BONUS_MAX, setAttributeBonus(s, point, keys, "dexterity", 50).attributeBonuses["dexterity"])
        assertEquals(BONUS_MIN, setAttributeBonus(s, point, keys, "dexterity", -50).attributeBonuses["dexterity"])
        assertEquals(6, setAttributeBonus(s, point, keys, "dexterity", -2).attributes["dexterity"]) // 8 - 2
    }

    @Test
    fun valorFinalNuncaFicaAbaixoDe1() {
        assertEquals(1, finalScore(point, 1, -2))
    }

    @Test
    fun finalizarGravaOValorFinalComBonus() {
        var s = withBase(draft(), 15, 14, 13, 12, 10, 8)
        s = setAttributeBonus(s, point, keys, "strength", 2)
        s = setAttributeBonus(s, point, keys, "constitution", 1)
        val done = finalizeCreation(s, point, keys)!!
        assertEquals(CreationState.DONE, done.creation)
        assertEquals(17, done.attributes["strength"])
        assertEquals(14, done.attributes["constitution"])
        assertEquals(8, done.attributes["charisma"])
        assertEquals(15, done.baseAttributes["strength"])
    }

    // --- array padrão ---

    @Test
    fun arrayPadraoComecaNaOrdemDoArrayEEhValido() {
        val s = draft(array)
        assertEquals(listOf(15, 14, 13, 12, 10, 8), keys.map { s.baseAttributes[it] })
        assertTrue(checkCreation(array, s.baseAttributes, keys).valid)
        assertNotNull(finalizeCreation(s, array, keys))
    }

    @Test
    fun arrayPadraoAcusaValorRepetidoEValorForaDoArray() {
        var s = draft(array)
        s = setBaseScore(s, array, keys, "dexterity", 15) // 15 repetido
        val check = checkCreation(array, s.baseAttributes, keys)
        assertFalse(check.valid)
        assertNull(finalizeCreation(s, array, keys))

        val fora = setBaseScore(draft(array), array, keys, "wisdom", 16)
        assertFalse(checkCreation(array, fora.baseAttributes, keys).valid)
    }

    @Test
    fun arrayPadraoAceitaTrocarValoresEntreAtributos() {
        var s = draft(array)
        s = setBaseScore(s, array, keys, "strength", 8)
        s = setBaseScore(s, array, keys, "charisma", 15)
        assertTrue(checkCreation(array, s.baseAttributes, keys).valid)
        assertEquals(8, s.attributes["strength"])
        assertEquals(15, s.attributes["charisma"])
    }

    @Test
    fun arrayPadraoNaoUsaPassoDeCompra() {
        val s = draft(array)
        assertFalse(canStepBase(s, array, keys, "strength", -1))
        assertSame(s, stepBaseScore(s, array, keys, "strength", -1))
    }

    // --- rolagem ---

    @Test
    fun rolagemAceitaDe3a18() {
        var s = draft(roll)
        assertTrue(checkCreation(roll, s.baseAttributes, keys).valid)
        s = setBaseScore(s, roll, keys, "strength", 18)
        s = setBaseScore(s, roll, keys, "dexterity", 3)
        assertTrue(checkCreation(roll, s.baseAttributes, keys).valid)
        assertEquals(3..18, roll.baseRange)
    }

    @Test
    fun rolagemRecusaForaDaFaixa() {
        val alto = setBaseScore(draft(roll), roll, keys, "strength", 19)
        assertFalse(checkCreation(roll, alto.baseAttributes, keys).valid)
        assertNull(finalizeCreation(alto, roll, keys))
        val baixo = setBaseScore(draft(roll), roll, keys, "strength", 2)
        assertFalse(checkCreation(roll, baixo.baseAttributes, keys).valid)
    }

    @Test
    fun rolagemTambemTemTetoFinalDe20() {
        var s = setBaseScore(draft(roll), roll, keys, "strength", 18)
        s = setAttributeBonus(s, roll, keys, "strength", 3)
        assertEquals(20, s.attributes["strength"])
    }

    // --- livre e outros sistemas ---

    @Test
    fun metodoLivreNaoCriaEstadoNemTrava() {
        val livre = creationRuleFor("vampire")
        val s = startCreation(blank(), livre, keys)
        assertEquals(CreationState.NONE, s.creation)
        assertFalse(inCreation(s, livre))
        assertFalse(attributesLocked(s, livre))
        assertTrue(checkCreation(livre, mapOf("strength" to 99), keys).valid)
        assertNull(livre.baseRange)
        assertNull(livre.finalCap)
    }

    @Test
    fun fichaAntigaSemEstadoFicaLivre() {
        val antiga = CharacterSheet(id = "x", attributes = mapOf("strength" to 18))
        assertEquals(CreationState.NONE, antiga.creation)
        assertFalse(inCreation(antiga, point))
        assertFalse(attributesLocked(antiga, point))
        // nenhuma função de edição da criação mexe numa ficha assim
        assertSame(antiga, setBaseScore(antiga, point, keys, "strength", 10))
        assertSame(antiga, stepBaseScore(antiga, point, keys, "strength", +1))
        assertSame(antiga, setAttributeBonus(antiga, point, keys, "strength", 2))
        assertNull(finalizeCreation(antiga, point, keys))
        assertSame(antiga, reopenCreation(antiga, point, keys))
    }

    @Test
    fun orcamentoDefinidoPeloSistemaFuncionaComACalculadoraGenerica() {
        val rule = linearPointBuyRule(budget = 20, minBase = 1, maxBase = 5)
        assertEquals(0, rule.costOf(1))
        assertEquals(4, rule.costOf(5))
        assertNull(rule.costOf(6))
        val k = listOf("a", "b", "c")
        var s = startCreation(CharacterSheet(id = "g"), rule, k)
        repeat(4) { s = stepBaseScore(s, rule, k, "a", +1) } // 1 -> 5 = 4 pontos
        assertEquals(4, checkCreation(rule, s.baseAttributes, k).spent)
        assertEquals(5, s.attributes["a"])
        assertNull(rule.finalCap) // sem teto
        assertEquals(9, finalScore(rule, 5, 4))
    }

    // --- estados: travar e reabrir ---

    @Test
    fun finalizadaTravaOsAtributos() {
        val done = finalizeCreation(withBase(draft(), 15, 14, 13, 12, 10, 8), point, keys)!!
        assertTrue(attributesLocked(done, point))
        assertFalse(inCreation(done, point))
        // e nenhuma edição da criação passa
        assertSame(done, setBaseScore(done, point, keys, "strength", 8))
        assertSame(done, stepBaseScore(done, point, keys, "strength", -1))
        assertSame(done, setAttributeBonus(done, point, keys, "strength", 2))
        assertNull(finalizeCreation(done, point, keys)) // já finalizada
    }

    @Test
    fun seOMestreTrocarParaLivreAFichaFinalizadaDestrava() {
        val done = finalizeCreation(withBase(draft(), 15, 14, 13, 12, 10, 8), point, keys)!!
        assertFalse(attributesLocked(done, creationRuleFor("dnd5e", CreationMethod.FREE)))
    }

    @Test
    fun reabrirMantemBaseEBonus() {
        var s = withBase(draft(), 15, 14, 13, 12, 10, 8)
        s = setAttributeBonus(s, point, keys, "strength", 2)
        val done = finalizeCreation(s, point, keys)!!
        val reaberta = reopenCreation(done, point, keys)
        assertEquals(CreationState.DRAFT, reaberta.creation)
        assertEquals(15, reaberta.baseAttributes["strength"])
        assertEquals(2, reaberta.attributeBonuses["strength"])
        assertEquals(17, reaberta.attributes["strength"])
        assertFalse(attributesLocked(reaberta, point))
    }

    @Test
    fun reabrirSemBaseGuardadaRecomecaDoZero() {
        val semBase = CharacterSheet(id = "x", creation = CreationState.DONE, attributes = keys.associateWith { 14 })
        val reaberta = reopenCreation(semBase, point, keys)
        assertEquals(CreationState.DRAFT, reaberta.creation)
        keys.forEach { assertEquals(8, reaberta.baseAttributes[it]) }
    }

    @Test
    fun soOMestreOuDonoDeFichaSoltaReabre() {
        val c = Campaign(id = "c", name = "x", masterUserId = "mestre")
        assertTrue(canReopenCreation(c, "mestre"))
        assertFalse(canReopenCreation(c, "jogador"))
        assertTrue(canReopenCreation(null, "qualquer"))
    }

    @Test
    fun recriarNaoApagaOutrosValoresDaFicha() {
        val s = CharacterSheet(id = "s", attributes = mapOf("extra" to 5))
        val d = startCreation(s, point, keys)
        assertEquals(5, d.attributes["extra"]) // chave de outro sistema guardada, como no rebindCharacter
    }

    @Test
    fun campanhaGuardaOMetodoEscolhido() {
        val c = Campaign(id = "c", name = "x", masterUserId = "m", creationMethod = CreationMethod.STANDARD_ARRAY)
        assertEquals(CreationMethod.STANDARD_ARRAY, creationRuleFor(c.systemId, c.creationMethod).method)
        assertEquals("", Campaign(id = "c", name = "x", masterUserId = "m").creationMethod)
    }

    @Test
    fun faixaDoMetodoEMensagensUsamAsSiglas() {
        val s = withBase(draft(), 16, 8, 8, 8, 8, 8)
        val check = checkCreation(point, s.baseAttributes, keys, mapOf("strength" to "FOR"))
        assertTrue(check.problems.first().startsWith("FOR:"))
    }
}
