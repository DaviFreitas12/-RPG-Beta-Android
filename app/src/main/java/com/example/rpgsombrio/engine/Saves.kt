package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.immuneToCondition

// ---------------------------------------------------------------------------------------------
// Salvaguardas no combate (0.9.2).
//
// Lógica pura, sem Compose nem Android. Quem rola é o alvo de um efeito: o bônus vem da ficha
// (proficiência), do texto "Salvaguardas" da criatura ou é 0 para combatentes avulsos. As condições
// mexem na rolagem (Paralisado falha sozinho em Força/Destreza, Imobilizado tem desvantagem em Destreza)
// e uma falha pode aplicar uma condição por um número de rodadas.
// ---------------------------------------------------------------------------------------------

private val STRENGTH_NAMES = setOf("FOR", "FORCA", "STR", "STRENGTH")
private val DEXTERITY_NAMES = setOf("DES", "DESTREZA", "DEX", "DEXTERITY")

/** Nomes pelos quais um atributo pode ser citado (sigla, nome por extenso e chave), já normalizados. */
private fun attributeNames(attr: SheetAttr): Set<String> =
    setOf(variableName(attr.abbr), variableName(attr.label), variableName(attr.key)).filter { it.isNotEmpty() }.toSet()

fun isStrength(attr: SheetAttr): Boolean = attributeNames(attr).any { it in STRENGTH_NAMES }

fun isDexterity(attr: SheetAttr): Boolean = attributeNames(attr).any { it in DEXTERITY_NAMES }

/** Atributos que entram em salvaguardas: os que têm modificador (reservas de dados não têm salvaguarda). */
fun saveAttributes(profile: SystemProfile): List<SheetAttr> =
    if (profile.isPool) emptyList() else profile.attributes.filter { it.hasModifier }

// --- vantagem, desvantagem e falha automática ------------------------------------------------

data class SaveModifiers(
    val mode: RollMode,
    val advantageReasons: List<String> = emptyList(),
    val disadvantageReasons: List<String> = emptyList(),
    /** Condição que faz o alvo falhar sem rolar (ex.: "Paralisado"); vazio se rola normalmente. */
    val autoFailReason: String = ""
) {
    val autoFail: Boolean get() = autoFailReason.isNotEmpty()

    /** Linhas curtas para mostrar antes de rolar. Vazio quando nada muda. */
    fun describe(): List<String> = buildList {
        when {
            autoFail -> add("Falha automática: $autoFailReason.")
            advantageReasons.isNotEmpty() && disadvantageReasons.isNotEmpty() ->
                add("Vantagem (${advantageReasons.joinToString(", ")}) e desvantagem (${disadvantageReasons.joinToString(", ")}) se anulam.")
            advantageReasons.isNotEmpty() -> add("Vantagem: ${advantageReasons.joinToString(", ")}.")
            disadvantageReasons.isNotEmpty() -> add("Desvantagem: ${disadvantageReasons.joinToString(", ")}.")
        }
    }
}

/**
 * Vantagem/desvantagem e falha automática da salvaguarda de [attr] para quem tem [conditions].
 * [manual] é a escolha do mestre (resistência mágica, bênção…) e entra junto: vantagem e desvantagem se anulam.
 */
fun saveModifiers(conditions: List<String>, attr: SheetAttr, manual: RollMode = RollMode.NORMAL): SaveModifiers {
    val strength = isStrength(attr)
    val dexterity = isDexterity(attr)
    var autoFail = ""
    val advantage = mutableListOf<String>()
    val disadvantage = mutableListOf<String>()
    for (c in conditions) {
        val rule = conditionRule(c) ?: continue
        if (rule.failsStrDexSaves && (strength || dexterity) && autoFail.isEmpty()) autoFail = rule.name
        if (rule.dexSaveDisadvantage && dexterity) disadvantage += rule.name
    }
    when (manual) {
        RollMode.ADVANTAGE -> advantage += "escolha manual"
        RollMode.DISADVANTAGE -> disadvantage += "escolha manual"
        RollMode.NORMAL -> {}
    }
    val adv = advantage.distinct()
    val dis = disadvantage.distinct()
    val mode = when {
        adv.isNotEmpty() && dis.isEmpty() -> RollMode.ADVANTAGE
        dis.isNotEmpty() && adv.isEmpty() -> RollMode.DISADVANTAGE
        else -> RollMode.NORMAL
    }
    return SaveModifiers(mode, adv, dis, autoFail)
}

// --- bônus ----------------------------------------------------------------------------------

private val SAVE_ENTRY = Regex("^\\s*(\\p{L}[\\p{L}_ ]*?)\\s*[:=]?\\s*([+\\-−–]?\\s?\\d+)\\s*$")

/** Lê o texto "Salvaguardas" de uma criatura ("DES +3, Sab +2") em pares (nome normalizado, bônus). */
fun parseSaveList(text: String): List<Pair<String, Int>> =
    text.split(',', ';', '\n').mapNotNull { part ->
        val match = SAVE_ENTRY.find(part) ?: return@mapNotNull null
        val name = variableName(match.groupValues[1])
        val value = match.groupValues[2].replace("−", "-").replace("–", "-").replace(" ", "").toIntOrNull()
        if (name.isEmpty() || value == null) null else name to value
    }

/**
 * Bônus de salvaguarda de uma criatura: o valor escrito em "Salvaguardas" para esse atributo ou, se não está
 * na lista, o modificador do atributo (como no D&D 5e).
 */
fun monsterSaveBonus(monster: MonsterSheet, attr: SheetAttr): Int {
    val names = attributeNames(attr)
    parseSaveList(monster.savingThrows).firstOrNull { it.first in names }?.let { return it.second }
    return attributeBonus(attr, attributeValue(monster.attributes, attr))
}

/** Bônus de quem rola: ficha do personagem, criatura, ou 0 para combatentes avulsos (use o bônus extra). */
fun fighterSaveBonus(character: CharacterSheet?, monster: MonsterSheet?, attr: SheetAttr): Int = when {
    character != null -> saveBonus(character, attr)
    monster != null -> monsterSaveBonus(monster, attr)
    else -> 0
}

// --- rolagem --------------------------------------------------------------------------------

data class SaveOutcome(
    val line: RollLine,
    val success: Boolean,
    val dc: Int,
    val total: Int? = null,
    /** d20 que valeu e o descartado (só com vantagem/desvantagem). */
    val d20: Int? = null,
    val otherD20: Int? = null,
    val mode: RollMode = RollMode.NORMAL,
    val autoFailed: Boolean = false,
    /** A rolagem deu erro (fórmula inválida): não conta como falha nem aplica efeitos. */
    val error: Boolean = false
)

/**
 * Rola a salvaguarda de [name] contra a [dc]: dado-base do sistema + [bonus]. Com vantagem/desvantagem rola duas
 * vezes (só quando o dado-base é um d20). Sem 20/1 natural automático (como no D&D 5e). Se [mods] manda falhar
 * automaticamente, nem rola.
 */
fun resolveSave(
    engine: RpgEngine, name: String, attrLabel: String, bonus: Int, dc: Int,
    profile: SystemProfile?, mods: SaveModifiers = SaveModifiers(RollMode.NORMAL)
): SaveOutcome {
    val title = "$name: salvaguarda de $attrLabel"
    if (mods.autoFail) {
        return SaveOutcome(
            RollLine("$title • falha automática (${mods.autoFailReason}) • CD $dc", false),
            success = false, dc = dc, autoFailed = true
        )
    }
    return try {
        val formula = biasedExpression(baseDie(profile), bonus)
        val first = engine.roll(formula)
        var chosen = first
        var discarded: EngineRoll? = null
        if (mods.mode != RollMode.NORMAL && first.firstD20 != null) {
            val second = engine.roll(formula)
            val takeSecond = if (mods.mode == RollMode.ADVANTAGE) second.total > first.total else second.total < first.total
            chosen = if (takeSecond) second else first
            discarded = if (takeSecond) first else second
        }
        val success = chosen.total >= dc
        val modeText = if (discarded == null) "" else
            " • ${if (mods.mode == RollMode.ADVANTAGE) "vantagem" else "desvantagem"} (d20: ${chosen.firstD20} e ${discarded.firstD20})"
        val verdict = if (success) " • sucesso (CD $dc)" else " • falha (CD $dc)"
        SaveOutcome(
            RollLine("$title: ${describeRoll(chosen)}$modeText${naturalTag(chosen)}$verdict", success),
            success = success, dc = dc, total = chosen.total, d20 = chosen.firstD20, otherD20 = discarded?.firstD20,
            mode = if (discarded == null) RollMode.NORMAL else mods.mode
        )
    } catch (e: RuntimeException) {
        SaveOutcome(RollLine("$title: erro — ${e.message}", null), success = false, dc = dc, error = true)
    }
}

// --- efeito da falha ------------------------------------------------------------------------

/** O alvo depois do efeito e a linha do histórico (null quando nada aconteceu). */
data class SaveEffect(val target: Combatant, val line: RollLine?)

/**
 * Se a salvaguarda falhou e há uma [condition], aplica-a em [target] por [rounds] rodadas (0 = até ser removida).
 * Quem é imune à condição não a recebe. Passou na salvaguarda, houve erro ou não há condição: nada muda.
 */
fun applyFailedSaveEffect(target: Combatant, outcome: SaveOutcome, condition: String, rounds: Int): SaveEffect {
    val name = condition.trim()
    if (name.isEmpty() || outcome.success || outcome.error) return SaveEffect(target, null)
    if (immuneToCondition(target.conditionImmunities, name)) {
        return SaveEffect(target, RollLine("${target.name} é imune a $name.", null))
    }
    val updated = target.withCondition(name, rounds)
    return SaveEffect(updated, RollLine("${conditionEmoji(name)} ${target.name} fica $name ${durationText(rounds)}.", false))
}
