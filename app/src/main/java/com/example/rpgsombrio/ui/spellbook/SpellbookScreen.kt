package com.example.rpgsombrio.ui.spellbook

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SpellEntry
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.data.spellLevelLabel
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.data.systemOptions
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.characterVariables
import com.example.rpgsombrio.engine.rollSpellEffect
import com.example.rpgsombrio.ui.components.ANY_SYSTEM
import com.example.rpgsombrio.ui.components.ChipFilterRow
import com.example.rpgsombrio.ui.components.CollapsibleSection
import com.example.rpgsombrio.ui.components.ConfirmDeleteDialog
import com.example.rpgsombrio.ui.components.DuplicateToSystem
import com.example.rpgsombrio.ui.components.EditorTopBar
import com.example.rpgsombrio.ui.components.EmptyHint
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.RollLogBar
import com.example.rpgsombrio.ui.components.SystemFilterRow
import com.example.rpgsombrio.ui.components.SystemPickerDialog
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.components.ToggleRow
import com.example.rpgsombrio.ui.components.fitsFilter
import com.example.rpgsombrio.ui.components.listHeader
import com.example.rpgsombrio.ui.components.push
import com.example.rpgsombrio.ui.components.validSystemFilter
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel

@Composable
internal fun SpellbookScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val spells = remember { mutableStateListOf<SpellEntry>().apply { addAll(SheetStore.loadSpells(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var levelFilter by rememberSaveable { mutableStateOf("Todos") }
    var systemFilter by rememberSaveable { mutableStateOf(SheetStore.activeSystemId(prefs)) }
    val sysOptions = remember { systemOptions(prefs) }
    var casterId by rememberSaveable { mutableStateOf<String?>(null) }
    val characters = remember { SheetStore.loadCharacters(prefs) }
    val engine = remember { RpgEngine() }
    val log = remember { mutableStateListOf<RollLine>() }
    var deleteTarget by remember { mutableStateOf<SpellEntry?>(null) }
    fun persist() = SheetStore.saveSpells(prefs, spells.toList())

    val current = spells.firstOrNull { it.id == openId }
    BackHandler(enabled = current != null) { openId = null }

    if (current != null) {
        SpellEditor(
            modifier, current, prefs,
            onChange = { updated ->
                val i = spells.indexOfFirst { it.id == updated.id }
                if (i >= 0) { spells[i] = updated; persist() }
            },
            onDuplicate = { sid ->
                val copy = current.copy(id = SheetStore.newId(), systemId = sid)
                spells.add(0, copy); persist(); openId = copy.id
            },
            onBack = { openId = null },
            onDelete = { deleteTarget = current }
        )
    } else {
        val levelOptions = listOf("Todos") + spells.map { it.level }.distinct().sorted().map { spellLevelLabel(it) }
        val filter = if (levelFilter in levelOptions) levelFilter else "Todos"
        val sysFilter = validSystemFilter(sysOptions, systemFilter)
        // Magias sem sistema valem para todos; ao filtrar por um sistema elas continuam aparecendo.
        val filtered = spells.filter { s ->
            fitsFilter(s.systemId, sysFilter) &&
                (filter == "Todos" || spellLevelLabel(s.level) == filter) &&
                listOf(s.name, s.school, s.classes, s.description).any { it.contains(query, ignoreCase = true) }
        }
        val groups = filtered.sortedBy { it.name.lowercase() }.groupBy { it.level }.toSortedMap()
        val caster = characters.firstOrNull { it.id == casterId }
        Column(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listHeader("Magias e poderes", "Grimório", "Organizado por nível, com ficha completa de cada magia",
                query, { query = it }, "Nova magia") {
                val spell = SheetStore.newSpell(SheetStore.activeSystemId(prefs))
                spells.add(0, spell); persist(); openId = spell.id
            }
            item { SystemFilterRow(sysOptions, sysFilter) { systemFilter = it } }
            if (levelOptions.size > 2) item { ChipFilterRow(levelOptions, filter) { levelFilter = it } }
            if (characters.isNotEmpty()) item { CasterPicker(characters, caster) { casterId = it } }
            if (filtered.isEmpty()) item {
                EmptyHint(if (query.isBlank() && filter == "Todos" && sysFilter == ANY_SYSTEM) "Nenhuma magia ainda. Toque em Nova magia." else "Nenhum resultado.")
            }
            groups.forEach { (level, list) ->
                item(key = "header-$level") {
                    Text("${spellLevelLabel(level).uppercase()} • ${list.size}", color = Accent, fontSize = 12.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, modifier = Modifier.padding(top = 6.dp))
                }
                items(list, key = { it.id }) { spell ->
                    SpellCard(
                        spell, expanded = expandedId == spell.id,
                        onToggle = { expandedId = if (expandedId == spell.id) null else spell.id },
                        onEdit = { openId = spell.id },
                        onRoll = {
                            val variables = caster?.let { characterVariables(it, SystemProfiles.resolve(prefs, it.systemId), SheetStore.loadItems(prefs)) } ?: emptyMap()
                            val line = rollSpellEffect(engine, spell, variables)
                            log.push(RollLine((if (caster != null) "${caster.name} • " else "") + line.text, line.good))
                        }
                    )
                }
            }
        }
        RollLogBar(log) { log.clear() }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(target.name, "esta magia", onConfirm = {
            spells.removeAll { it.id == target.id }
            persist()
            if (openId == target.id) openId = null
            deleteTarget = null
        }, onDismiss = { deleteTarget = null })
    }
}

@Composable
private fun SpellCard(spell: SpellEntry, expanded: Boolean, onToggle: () -> Unit, onEdit: () -> Unit, onRoll: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(spell.name.ifBlank { "Sem nome" }, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    val subtitle = listOf(spellLevelLabel(spell.level), spell.school).filter { it.isNotBlank() }.joinToString(" • ")
                    Text(subtitle, color = Accent, fontSize = 12.sp)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Recolher" else "Expandir", tint = Muted)
            }
            if (spell.concentration || spell.ritual || spell.damage.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (spell.concentration) InfoPill("Concentração")
                    if (spell.ritual) InfoPill("Ritual")
                    if (spell.damage.isNotBlank()) InfoPill(spell.damage)
                }
            }
            if (expanded) {
                DetailLine("Execução", spell.castingTime)
                DetailLine("Alcance", spell.range)
                DetailLine("Componentes", spell.components)
                DetailLine("Duração", spell.duration)
                DetailLine("Custo", spell.cost)
                DetailLine("Classes / fontes", spell.classes)
                if (spell.description.isNotBlank()) Text(spell.description, color = Ink, fontSize = 13.sp)
                if (spell.higherLevels.isNotBlank()) {
                    Text("Em níveis superiores", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(spell.higherLevels, color = Muted, fontSize = 13.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (spell.damage.isNotBlank()) TextButton(onClick = onRoll) { Icon(Icons.Default.Casino, null); Text("Rolar efeito") }
                    TextButton(onClick = onEdit) { Icon(Icons.Default.Edit, null); Text("Editar") }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    if (value.isBlank()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Ink, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SpellEditor(
    modifier: Modifier,
    spell: SpellEntry,
    prefs: SharedPreferences,
    onChange: (SpellEntry) -> Unit,
    onDuplicate: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { EditorTopBar("Magia", onBack, onDelete) }
        item {
            var picking by remember { mutableStateOf(false) }
            CollapsibleSection("BÁSICO", summary = "${spellLevelLabel(spell.level)} • ${spell.school}") {
                TextInput("Nome", spell.name, { onChange(spell.copy(name = it)) })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Sistema", color = Muted, fontSize = 13.sp)
                        Text(if (spell.systemId.isBlank()) "Todos os sistemas" else systemDisplayName(prefs, spell.systemId),
                            color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { picking = true }) { Text("Alterar") }
                }
                DuplicateToSystem(systemOptions(prefs), spell.systemId, SheetStore.activeSystemId(prefs), onDuplicate)
                if (picking) {
                    SystemPickerDialog(
                        title = "Sistema da magia", options = systemOptions(prefs), currentId = spell.systemId,
                        campaignId = SheetStore.activeSystemId(prefs), allowAll = true,
                        onPick = { picking = false; onChange(spell.copy(systemId = it)) },
                        onDismiss = { picking = false }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Nível (0 = truque)", spell.level, { onChange(spell.copy(level = it.coerceAtLeast(0))) }, Modifier.weight(1f))
                    TextInput("Escola / tipo", spell.school, { onChange(spell.copy(school = it)) }, Modifier.weight(1.4f))
                }
                TextInput("Classes / fontes", spell.classes, { onChange(spell.copy(classes = it)) })
            }
        }
        item {
            CollapsibleSection("CONJURAÇÃO", summary = listOf(spell.castingTime, spell.range, spell.duration).filter { it.isNotBlank() }.joinToString(" • ")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput("Execução", spell.castingTime, { onChange(spell.copy(castingTime = it)) }, Modifier.weight(1f))
                    TextInput("Alcance", spell.range, { onChange(spell.copy(range = it)) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput("Componentes", spell.components, { onChange(spell.copy(components = it)) }, Modifier.weight(1f))
                    TextInput("Duração", spell.duration, { onChange(spell.copy(duration = it)) }, Modifier.weight(1f))
                }
                ToggleRow("Requer concentração", spell.concentration) { onChange(spell.copy(concentration = it)) }
                ToggleRow("Pode ser ritual", spell.ritual) { onChange(spell.copy(ritual = it)) }
            }
        }
        item {
            CollapsibleSection("EFEITO", summary = spell.damage.ifBlank { spell.description.lineSequence().firstOrNull().orEmpty() }) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput("Dano / efeito", spell.damage, { onChange(spell.copy(damage = it)) }, Modifier.weight(1f))
                    TextInput("Custo", spell.cost, { onChange(spell.copy(cost = it)) }, Modifier.weight(1f))
                }
                Text("Dano/efeito aceita fórmulas com os valores da ficha (ex.: 2d8 + INT necrótico). Custo como \"3 Mana\" é debitado do recurso de mesmo nome ao conjurar.",
                    color = Muted, fontSize = 13.sp)
                TextInput("Descrição", spell.description, { onChange(spell.copy(description = it)) }, singleLine = false, minLines = 5)
                TextInput("Em níveis superiores", spell.higherLevels, { onChange(spell.copy(higherLevels = it)) }, singleLine = false, minLines = 2)
            }
        }
    }
}

@Composable
private fun CasterPicker(characters: List<CharacterSheet>, selected: CharacterSheet?, onSelect: (String?) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Conjurador: ${selected?.name?.ifBlank { "Sem nome" } ?: "ninguém (só dados)"}", modifier = Modifier.weight(1f))
            Icon(Icons.Default.ExpandMore, contentDescription = "Escolher conjurador")
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Ninguém (só dados)") }, onClick = { onSelect(null); open = false })
            characters.forEach { c ->
                DropdownMenuItem(text = { Text(c.name.ifBlank { "Sem nome" }) }, onClick = { onSelect(c.id); open = false })
            }
        }
    }
}
