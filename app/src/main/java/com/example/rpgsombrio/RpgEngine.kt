package com.example.rpgsombrio

import com.example.rpgsombrio.data.DamageDefense
import java.text.Normalizer
import kotlin.random.Random

/**
 * Núcleo do motor. As regras são determinísticas; a aleatoriedade vem apenas do [roller],
 * que pode ser substituído em testes por valores fixos.
 */
data class EngineRoll(
    val total: Int,
    val breakdown: String,
    val naturalRolls: List<Int> = emptyList(),
    /** Primeiro d20 rolado na fórmula (usado para crítico/falha crítica), ou null se não houve d20. */
    val firstD20: Int? = null
)

data class EngineCheck(
    val roll: EngineRoll,
    val difficulty: Int,
    val success: Boolean,
    val margin: Int
)

data class AttackResult(
    val attackRoll: EngineRoll,
    val targetArmorClass: Int,
    val hit: Boolean,
    val critical: Boolean,
    val criticalMiss: Boolean,
    val damageRoll: EngineRoll?
)

data class Combatant(
    val id: String,
    val name: String,
    val initiative: Int,
    val currentHp: Int,
    val maxHp: Int,
    val armorClass: Int = 10,
    val conditions: List<String> = emptyList(),
    /** Ficha ou criatura de origem (vazio = combatente avulso, criado à mão). */
    val sheetId: String = "",
    /** [CombatantSource.CHARACTER], [CombatantSource.MONSTER] ou vazio. */
    val sheetKind: String = "",
    /** Defesas de dano e imunidades a condições do alvo (copiadas da ficha ao entrar no combate). */
    val defenses: List<DamageDefense> = emptyList(),
    val conditionImmunities: String = "",
    /**
     * Rodadas que faltam para cada condição expirar (chave = nome da condição). Condição sem entrada aqui
     * dura até alguém removê-la. A contagem desce ao fim de cada turno de quem está afetado (ver [tickConditions]).
     */
    val conditionTimers: Map<String, Int> = emptyMap(),
    /** PV temporários: absorvem o dano antes dos PV (ver [withDamage]). */
    val tempHp: Int = 0,
    /** Salvaguardas contra a morte (só personagens a 0 PV): 3 sucessos estabilizam, 3 falhas matam (ver Death.kt). */
    val deathSuccesses: Int = 0,
    val deathFailures: Int = 0
)

object CombatantSource {
    const val CHARACTER = "character"
    const val MONSTER = "monster"
}

/** Resultado de uma reserva de dados: cada dado >= [target] conta um sucesso. */
data class PoolResult(
    val rolls: List<Int>,
    val sides: Int,
    val target: Int,
    val successes: Int,
    /** Dados que tiraram o valor máximo. */
    val maxFaces: Int
)

fun interface DiceRoller {
    /** Retorna um valor entre 1 e [sides], inclusive. */
    fun roll(sides: Int): Int
}

class RpgEngine(
    private val roller: DiceRoller = DiceRoller { sides -> Random.Default.nextInt(1, sides + 1) }
) {
    /** [doubleDice] dobra a quantidade de dados de cada termo NdX (usado no dano de acertos críticos). */
    fun roll(
        expression: String,
        variables: Map<String, Int> = emptyMap(),
        doubleDice: Boolean = false
    ): EngineRoll {
        val parsed = FormulaParser(expression, variables, roller, doubleDice).parse()
        return EngineRoll(parsed.value, "$expression = ${parsed.value}", parsed.naturalRolls, parsed.firstD20)
    }

    fun check(
        expression: String,
        difficulty: Int,
        variables: Map<String, Int> = emptyMap()
    ): EngineCheck {
        val result = roll(expression, variables)
        val margin = result.total - difficulty
        return EngineCheck(result, difficulty, margin >= 0, margin)
    }

    /**
     * Reserva de dados: rola [dice] dados de [sides] lados e conta os que tiraram [successOn] ou mais.
     * Serve a sistemas que contam sucessos (como Vampiro) em vez de somar o resultado.
     */
    fun rollPool(dice: Int, sides: Int = 10, successOn: Int = 6): PoolResult {
        require(dice in 0..100) { "A reserva deve ter entre 0 e 100 dados." }
        require(sides in 2..1000) { "O dado precisa ter entre 2 e 1000 lados." }
        require(successOn in 2..sides) { "O valor de sucesso deve ficar entre 2 e $sides." }
        val rolls = List(dice) { roller.roll(sides) }
        return PoolResult(rolls, sides, successOn, rolls.count { it >= successOn }, rolls.count { it == sides })
    }

    /**
     * Resolve um ataque no estilo d20: 20 natural sempre acerta e dobra os dados do dano;
     * 1 natural sempre erra. Sem d20 na fórmula, vale apenas a comparação com a defesa.
     */
    fun attack(
        attackExpression: String,
        damageExpression: String,
        targetArmorClass: Int,
        variables: Map<String, Int> = emptyMap()
    ): AttackResult {
        val attack = roll(attackExpression, variables)
        val critical = attack.firstD20 == 20
        val criticalMiss = attack.firstD20 == 1
        val hit = critical || (!criticalMiss && attack.total >= targetArmorClass)
        val damage = if (hit) roll(damageExpression, variables, doubleDice = critical) else null
        return AttackResult(attack, targetArmorClass, hit, critical, criticalMiss, damage)
    }

    private data class Parsed(val value: Int, val naturalRolls: List<Int>, val firstD20: Int?)

    private class FormulaParser(
        private val source: String,
        variables: Map<String, Int>,
        private val roller: DiceRoller,
        private val doubleDice: Boolean
    ) {
        private val input = source.replace("−", "-").replace("×", "*").replace("÷", "/")
        private var index = 0
        private val natural = mutableListOf<Int>()
        private var firstD20: Int? = null
        private val normalizedVariables = variables.entries.associate { normalize(it.key) to it.value }

        fun parse(): Parsed {
            require(input.isNotBlank()) { "Digite uma fórmula, por exemplo: 1d20 + FOR + PROF." }
            val value = expression()
            skipSpaces()
            require(index == input.length) { "Trecho não reconhecido: '${input.substring(index)}'." }
            return Parsed(value, natural.toList(), firstD20)
        }

        private fun expression(): Int {
            var value = term()
            while (true) {
                skipSpaces()
                value = when {
                    consume('+') -> value + term()
                    consume('-') -> value - term()
                    else -> return value
                }
            }
        }

        private fun term(): Int {
            var value = factor()
            while (true) {
                skipSpaces()
                value = when {
                    consume('*') -> value * factor()
                    consume('/') -> {
                        val divisor = factor()
                        require(divisor != 0) { "Não é permitido dividir por zero." }
                        // Arredonda para baixo (floor), como no modificador de atributo: (8 - 10) / 2 = -1.
                        Math.floorDiv(value, divisor)
                    }
                    else -> return value
                }
            }
        }

        private fun factor(): Int {
            skipSpaces()
            if (consume('+')) return factor()
            if (consume('-')) return -factor()
            if (consume('(')) {
                val value = expression()
                skipSpaces()
                require(consume(')')) { "Faltou fechar um parêntese." }
                return value
            }
            val start = index
            val number = readDigits()
            // "d" só inicia um dado quando seguido de dígitos; assim "DES" continua sendo uma variável.
            if (isDiceMarker()) {
                index++ // consome o 'd'
                val baseCount = if (number.isBlank()) 1 else number.toIntOrNull() ?: 0
                val sides = readDigits().toIntOrNull() ?: error("Informe os lados do dado após o d.")
                require(baseCount in 1..100) { "A quantidade de dados deve ficar entre 1 e 100." }
                require(sides in 2..1000) { "O dado precisa ter entre 2 e 1000 lados." }
                val count = if (doubleDice) baseCount * 2 else baseCount
                var sum = 0
                repeat(count) {
                    val r = roller.roll(sides)
                    if (sides == 20 && firstD20 == null) firstD20 = r
                    natural.add(r)
                    sum += r
                }
                return sum
            }
            if (number.isNotBlank()) {
                return number.toIntOrNull() ?: error("Número grande demais: $number.")
            }
            index = start
            val identifier = readIdentifier()
            require(identifier.isNotBlank()) { "Esperava um número, dado, atributo ou parêntese na posição ${index + 1}." }
            return normalizedVariables[normalize(identifier)]
                ?: throw IllegalArgumentException("Variável '$identifier' não foi informada. Cadastre o valor ou use um número.")
        }

        private fun isDiceMarker(): Boolean =
            index + 1 < input.length && (input[index] == 'd' || input[index] == 'D') && input[index + 1].isDigit()

        private fun readDigits(): String {
            val start = index
            while (index < input.length && input[index].isDigit()) index++
            return input.substring(start, index)
        }

        private fun readIdentifier(): String {
            val start = index
            if (index < input.length && (input[index].isLetter() || input[index] == '_')) {
                index++
                while (index < input.length && (input[index].isLetterOrDigit() || input[index] == '_')) index++
            }
            return input.substring(start, index)
        }

        private fun skipSpaces() { while (index < input.length && input[index].isWhitespace()) index++ }

        private fun consume(char: Char): Boolean {
            if (index < input.length && input[index] == char) { index++; return true }
            return false
        }

        /** Maiúsculas, sem acentos; "PROFICIÊNCIA" vira "PROF". */
        private fun normalize(value: String): String {
            val plain = Normalizer.normalize(value.trim().uppercase(), Normalizer.Form.NFD)
                .replace(Regex("\\p{Mn}+"), "")
            return if (plain == "PROFICIENCIA") "PROF" else plain
        }
    }
}

/** Modificador de atributo estilo D&D: floor((valor - 10) / 2). */
fun dndModifier(score: Int): Int = Math.floorDiv(score - 10, 2)
