package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.SkillEntry
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.fitsSystem
import java.text.Normalizer

// ---------------------------------------------------------------------------------------------
// Aba "Jogar" (0.9.20): tudo o que o jogador consulta na mesa, já calculado.
//
// Lógica pura, sem Compose nem Android, para poder ser testada. A tela só desenha o que sai daqui.
// ---------------------------------------------------------------------------------------------

private fun plusMinus(n: Int): String = if (n >= 0) "+$n" else "$n"

/** De onde vem o bônus de uma perícia: "FOR +3 • prof +2 • extra +1". Vazio quando não há o que explicar. */
fun skillBreakdown(profile: SystemProfile, attributes: Map<String, Int>, proficiencyBonus: Int, skill: SkillEntry): String {
    val attr = profile.attr(skill.attributeKey)
    if (profile.isPool) {
        val parts = mutableListOf<String>()
        if (attr != null) parts += "${attr.abbr} ${attributeValue(attributes, attr)}"
        if (skill.extra != 0) parts += "perícia ${plusMinus(skill.extra)}"
        return parts.joinToString(" • ")
    }
    if (!profile.usesProficiency) return ""
    val parts = mutableListOf<String>()
    if (attr != null && attr.hasModifier) parts += "${attr.abbr} ${plusMinus(attr.modifierOf(attributeValue(attributes, attr)))}"
    if (skill.proficient) parts += "prof ${plusMinus(proficiencyBonus)}"
    if (skill.extra != 0) parts += "extra ${plusMinus(skill.extra)}"
    return parts.joinToString(" • ")
}

/** De onde vem o bônus de uma salvaguarda: "DES +2 • prof +2". */
fun saveBreakdown(sheet: CharacterSheet, attr: SheetAttr): String {
    val parts = mutableListOf("${attr.abbr} ${plusMinus(attributeBonus(attr, attributeValue(sheet.attributes, attr)))}")
    if (attr.hasModifier && attr.key in sheet.saveProficiencies) parts += "prof ${plusMinus(sheet.proficiencyBonus)}"
    return parts.joinToString(" • ")
}

private fun plain(raw: String): String =
    Normalizer.normalize(raw.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

/** Percepção passiva (10 + bônus de Percepção) para sistemas de bônus fixo; null se o sistema ou a ficha não tem Percepção. */
fun passivePerception(profile: SystemProfile, sheet: CharacterSheet): Int? {
    if (!profile.usesProficiency || profile.isPool) return null
    val skill = sheet.skills.firstOrNull { it.fitsSystem(sheet.systemId) && plain(it.name) == "percepcao" } ?: return null
    return 10 + skillBonus(profile, sheet.attributes, sheet.proficiencyBonus, skill)
}

/** Perícias exibidas na aba Jogar: favoritas e proficientes; com [showAll], todas. Mantém o índice na ficha. */
fun playSkills(sheet: CharacterSheet, showAll: Boolean): List<IndexedValue<SkillEntry>> =
    sheet.skills.withIndex().filter { it.value.fitsSystem(sheet.systemId) && (showAll || it.value.favorite || it.value.proficient) }
        .sortedWith(compareBy({ !it.value.favorite }, { plain(it.value.name) }))

private fun isStrength(a: SheetAttr) = a.key == "strength" || a.abbr.uppercase() in setOf("FOR", "STR")
private fun isDexterity(a: SheetAttr) = a.key == "dexterity" || a.abbr.uppercase() in setOf("DES", "DEX")

/** Aviso curto para uma salvaguarda afetada pelas condições da ficha, ou null. Ex.: "falha automática (Atordoado)". */
fun saveConditionNote(conditions: List<String>, attr: SheetAttr): String? {
    if (!isStrength(attr) && !isDexterity(attr)) return null
    for (c in conditions) {
        val rule = conditionRule(c) ?: continue
        if (rule.failsStrDexSaves) return "falha automática (${rule.name})"
    }
    if (isDexterity(attr)) {
        for (c in conditions) {
            val rule = conditionRule(c) ?: continue
            if (rule.dexSaveDisadvantage) return "desvantagem (${rule.name})"
        }
    }
    return null
}

/** Selo dos ataques da própria ficha: "Vantagem", "Desvantagem" ou null quando as condições não mudam o d20. */
fun attackRollBadge(conditions: List<String>): String? = when (attackModifiers(conditions, emptyList()).mode) {
    RollMode.ADVANTAGE -> "Vantagem"
    RollMode.DISADVANTAGE -> "Desvantagem"
    RollMode.NORMAL -> null
}

/** Frases para o aviso de condições da aba Jogar. Vazio quando nenhuma condição muda as rolagens. */
fun conditionNotes(conditions: List<String>): List<String> = buildList {
    actionBlocker(conditions)?.let { add("Não pode agir: $it. O turno é pulado.") }
    addAll(attackModifiers(conditions, emptyList()).describe())
    for (c in conditions) {
        val rule = conditionRule(c) ?: continue
        if (rule.failsStrDexSaves) add("${rule.name}: falha automática em salvaguardas de Força e Destreza.")
        if (rule.dexSaveDisadvantage) add("${rule.name}: desvantagem em salvaguardas de Destreza.")
        if (rule.resistsAllDamage) add("${rule.name}: resistência a todo dano.")
    }
}.distinct()
