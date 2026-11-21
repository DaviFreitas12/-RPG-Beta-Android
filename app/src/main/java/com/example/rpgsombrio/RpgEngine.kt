package com.example.rpgsombrio

import kotlin.random.Random

/** Núcleo determinístico em suas regras e aleatório apenas nos dados. */
data class EngineRoll(val total: Int, val breakdown: String, val naturalRolls: List<Int> = emptyList())

data class EngineCheck(
    val roll: EngineRoll,
    val difficulty: Int,
    val success: Boolean,
    val margin: Int
)

data class Combatant(
    val id: String,
    val name: String,
    val initiative: Int,
    val currentHp: Int,
    val maxHp: Int,
    val armorClass: Int = 10,
    val conditions: List<String> = emptyList()
)

class RpgEngine(private val random: Random = Random.Default) {
    fun roll(expression: String, variables: Map<String, Int> = emptyMap()): EngineRoll {
        val parser = FormulaParser(expression, variables, random)
        val result = parser.parse()
        return EngineRoll(result.value, result.parts.joinToString(" + "), result.naturalRolls)
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

    fun attack(
        attackExpression: String,
        damageExpression: String,
        targetArmorClass: Int,
        variables: Map<String, Int> = emptyMap()
    ): Pair<EngineRoll, EngineRoll?> {
        val attack = roll(attackExpression, variables)
        return if (attack.total >= targetArmorClass) attack to roll(damageExpression, variables) else attack to null
    }

    private data class Parsed(val value: Int, val parts: List<String>, val naturalRolls: List<Int> = emptyList())

    private class FormulaParser(
        private val source: String,
        private val variables: Map<String, Int>,
        private val random: Random
    ) {
        private val input = source.replace("−", "-").replace("×", "*").replace("÷", "/")
        private var index = 0
        private val natural = mutableListOf<Int>()
        private val normalizedVariables = variables.entries.associate { normalize(it.key) to it.value }

        fun parse(): Parsed {
            require(input.isNotBlank()) { "Digite uma fórmula, por exemplo: 1d20 + FOR + PROF." }
            val value = expression()
            skipSpaces()
            require(index == input.length) { "Trecho não reconhecido: '${input.substring(index)}'." }
            return Parsed(value, listOf("$source = $value"), natural.toList())
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
                        value / divisor
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
            skipSpaces()
            if (consume('d') || consume('D')) {
                val count = number.ifBlank { "1" }.toInt()
                val sides = readDigits().toIntOrNull() ?: error("Informe os lados do dado após o d.")
                require(count in 1..100) { "A quantidade de dados deve ficar entre 1 e 100." }
                require(sides in 2..1000) { "O dado precisa ter entre 2 e 1000 lados." }
                val rolls = List(count) { random.nextInt(1, sides + 1) }
                natural.addAll(rolls)
                return rolls.sum()
            }
            if (number.isNotBlank()) return number.toInt()
            index = start
            val identifier = readIdentifier()
            require(identifier.isNotBlank()) { "Esperava um número, dado, atributo ou parêntese na posição ${index + 1}." }
            val key = normalize(identifier)
            return normalizedVariables[key] ?: error("Variável '$identifier' não foi informada. Cadastre o valor ou use um número.")
        }

        private fun readDigits(): String {
            val start = index
            while (index < input.length && input[index].isDigit()) index++
            return input.substring(start, index)
        }

        private fun readIdentifier(): String {
            val start = index
            while (index < input.length && (input[index].isLetter() || input[index] == '_' || input[index] == 'ç' || input[index] == 'ã' || input[index] == 'õ')) index++
            return input.substring(start, index)
        }

        private fun skipSpaces() { while (index < input.length && input[index].isWhitespace()) index++ }
        private fun consume(char: Char): Boolean {
            if (index < input.length && input[index].equals(char, ignoreCase = true)) { index++; return true }
            return false
        }

        private fun normalize(value: String): String = value.trim().uppercase()
            .replace("Á", "A").replace("À", "A").replace("Â", "A").replace("Ã", "A")
            .replace("É", "E").replace("Ê", "E").replace("Í", "I").replace("Ó", "O")
            .replace("Ô", "O").replace("Õ", "O").replace("Ú", "U").replace("Ç", "C")
            .replace("PROFICIENCIA", "PROF").replace("PROFICIÊNCIA", "PROF")
    }
}

fun dndModifier(score: Int): Int = kotlin.math.floor((score - 10) / 2.0).toInt()
