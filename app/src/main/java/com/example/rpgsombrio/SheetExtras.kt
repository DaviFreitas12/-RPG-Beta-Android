package com.example.rpgsombrio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CONDITION_SUGGESTIONS
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.ResourceValue
import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.immuneToCondition

/** Salvaguardas: proficiência por atributo, bônus final e botão de rolar. Só para sistemas com modificadores. */
@Composable
internal fun SavesSection(
    sheet: CharacterSheet, profile: SystemProfile,
    onChange: (CharacterSheet) -> Unit, onRoll: (SheetAttr) -> Unit
) {
    val attrs = profile.attributes.filter { it.hasModifier }
    val summary = attrs.filter { it.key in sheet.saveProficiencies }.joinToString(", ") { it.abbr }
        .ifBlank { "Sem proficiências" }
    CollapsibleSection("SALVAGUARDAS", summary = summary, startExpanded = false) {
        attrs.forEach { attr ->
            val proficient = attr.key in sheet.saveProficiencies
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = proficient,
                    onCheckedChange = { on ->
                        onChange(sheet.copy(saveProficiencies = if (on) sheet.saveProficiencies + attr.key else sheet.saveProficiencies - attr.key))
                    },
                    colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Night)
                )
                Text(attr.label, color = Ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                InfoPill(signed(saveBonus(sheet, attr)))
                IconButton(onClick = { onRoll(attr) }) {
                    Icon(Icons.Default.Casino, "Rolar salvaguarda de ${attr.label}", tint = Accent)
                }
            }
        }
    }
}

@Composable
private fun CountBoxes(label: String, count: Int, onCount: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        repeat(3) { i ->
            Checkbox(
                checked = i < count,
                onCheckedChange = { on -> onCount(if (on) i + 1 else i) },
                colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Night)
            )
        }
    }
}

/** Extras dentro de RECURSOS: PV temporários, inspiração, salvaguardas contra a morte (só com PV em 0) e condições. */
@Composable
internal fun VitalsExtras(sheet: CharacterSheet, profile: SystemProfile, onChange: (CharacterSheet) -> Unit) {
    val hp = primaryResource(profile)?.let { sheet.resources[it.key] ?: ResourceValue(it.defaultMax, it.defaultMax) }
    if (profile.autoProficiency) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField("PV temporários", sheet.tempHp, { onChange(sheet.copy(tempHp = it.coerceAtLeast(0))) }, Modifier.weight(1f))
            Column(Modifier.weight(1f)) {
                ToggleRow("Inspiração", sheet.inspiration) { onChange(sheet.copy(inspiration = it)) }
            }
        }
        if (hp != null && hp.max > 0 && hp.current == 0) {
            Text("Salvaguardas contra a morte", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            CountBoxes("Sucessos", sheet.deathSuccesses) { onChange(sheet.copy(deathSuccesses = it)) }
            CountBoxes("Falhas", sheet.deathFailures) { onChange(sheet.copy(deathFailures = it)) }
        }
    }
    Text("Condições ativas", color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(CONDITION_SUGGESTIONS) { c ->
            val active = c in sheet.conditions
            FilterChip(
                selected = active,
                onClick = { onChange(sheet.copy(conditions = if (active) sheet.conditions - c else sheet.conditions + c)) },
                label = { Text(c) }
            )
        }
    }
}

@Composable
internal fun ProficienciesSection(sheet: CharacterSheet, onChange: (CharacterSheet) -> Unit) {
    CollapsibleSection(
        "PROFICIÊNCIAS E IDIOMAS",
        summary = sheet.languages.ifBlank { sheet.proficiencies.lineSequence().firstOrNull().orEmpty() },
        startExpanded = false
    ) {
        TextInput("Idiomas", sheet.languages, { onChange(sheet.copy(languages = it)) })
        TextInput("Proficiências (armas, armaduras, ferramentas)", sheet.proficiencies, { onChange(sheet.copy(proficiencies = it)) },
            singleLine = false, minLines = 2)
        TextInput("Sentidos (ex.: visão no escuro 18 m)", sheet.senses, { onChange(sheet.copy(senses = it)) })
    }
}

@Composable
internal fun BackstorySection(sheet: CharacterSheet, onChange: (CharacterSheet) -> Unit) {
    CollapsibleSection(
        "HISTÓRIA E PERSONALIDADE",
        summary = listOf(sheet.background, sheet.alignment).filter { it.isNotBlank() }.joinToString(" • "),
        startExpanded = false
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput("Antecedente", sheet.background, { onChange(sheet.copy(background = it)) }, Modifier.weight(1f))
            TextInput("Alinhamento", sheet.alignment, { onChange(sheet.copy(alignment = it)) }, Modifier.weight(1f))
        }
        TextInput("Aparência", sheet.appearance, { onChange(sheet.copy(appearance = it)) }, singleLine = false, minLines = 2)
        TextInput("Traços de personalidade", sheet.personality, { onChange(sheet.copy(personality = it)) }, singleLine = false, minLines = 2)
        TextInput("Ideais", sheet.ideals, { onChange(sheet.copy(ideals = it)) }, singleLine = false, minLines = 2)
        TextInput("Vínculos", sheet.bonds, { onChange(sheet.copy(bonds = it)) }, singleLine = false, minLines = 2)
        TextInput("Defeitos", sheet.flaws, { onChange(sheet.copy(flaws = it)) }, singleLine = false, minLines = 2)
    }
}

/**
 * Marca e desmarca condições de um combatente. Condições às quais ele é imune ficam bloqueadas.
 * [onToggle] recebe a condição e a duração em rodadas escolhida no campo de cima (0 = até ser removida);
 * a duração só vale ao marcar. [fighter] mostra quantas rodadas faltam em cada condição ativa.
 */
@Composable
internal fun ConditionsDialog(
    fighter: Combatant, onToggle: (String, Int) -> Unit, onDismiss: () -> Unit
) {
    var rounds by remember { mutableStateOf(0) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Condições • ${fighter.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Duração ao marcar (rodadas, 0 = até remover)", rounds, { rounds = it.coerceIn(0, 999) }, Modifier.fillMaxWidth())
                Text("A contagem desce ao fim de cada turno de quem está afetado.", color = Muted, fontSize = 11.sp)
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    items(CONDITION_SUGGESTIONS) { c ->
                        val on = c in fighter.conditions
                        val immune = immuneToCondition(fighter.conditionImmunities, c)
                        val allowed = on || !immune
                        Row(
                            Modifier.fillMaxWidth().clickable(enabled = allowed) { onToggle(c, rounds) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = on, onCheckedChange = { onToggle(c, rounds) }, enabled = allowed,
                                colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Night)
                            )
                            Column(Modifier.padding(vertical = 4.dp)) {
                                val label = if (immune) "$c (imune)" else if (on) fighter.conditionLabel(c) else c
                                Text("${conditionEmoji(c)} $label", color = if (allowed) Ink else Muted)
                                Text(conditionSummary(c), color = Muted, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Pronto") } }
    )
}
