package com.example.rpgsombrio

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SpellEntry
import com.example.rpgsombrio.data.spellLevelLabel

@Composable
internal fun SpellbookScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val spells = remember { mutableStateListOf<SpellEntry>().apply { addAll(SheetStore.loadSpells(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var expandedId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var levelFilter by rememberSaveable { mutableStateOf("Todos") }
    var deleteTarget by remember { mutableStateOf<SpellEntry?>(null) }
    fun persist() = SheetStore.saveSpells(prefs, spells.toList())

    val current = spells.firstOrNull { it.id == openId }
    BackHandler(enabled = current != null) { openId = null }

    if (current != null) {
        SpellEditor(
            modifier, current,
            onChange = { updated ->
                val i = spells.indexOfFirst { it.id == updated.id }
                if (i >= 0) { spells[i] = updated; persist() }
            },
            onBack = { openId = null },
            onDelete = { deleteTarget = current }
        )
    } else {
        val levelOptions = listOf("Todos") + spells.map { it.level }.distinct().sorted().map { spellLevelLabel(it) }
        val filter = if (levelFilter in levelOptions) levelFilter else "Todos"
        val filtered = spells.filter { s ->
            (filter == "Todos" || spellLevelLabel(s.level) == filter) &&
                listOf(s.name, s.school, s.classes, s.description).any { it.contains(query, ignoreCase = true) }
        }
        val groups = filtered.sortedBy { it.name.lowercase() }.groupBy { it.level }.toSortedMap()
        LazyColumn(
            modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            listHeader("Magias e poderes", "Grimório", "Organizado por nível, com ficha completa de cada magia",
                query, { query = it }, "Nova magia") {
                val spell = SheetStore.newSpell()
                spells.add(0, spell); persist(); openId = spell.id
            }
            if (levelOptions.size > 2) item { ChipFilterRow(levelOptions, filter) { levelFilter = it } }
            if (filtered.isEmpty()) item {
                EmptyHint(if (query.isBlank() && filter == "Todos") "Nenhuma magia ainda. Toque em Nova magia." else "Nenhum resultado.")
            }
            groups.forEach { (level, list) ->
                item(key = "header-$level") {
                    Text("${spellLevelLabel(level).uppercase()} • ${list.size}", color = Gold, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp, modifier = Modifier.padding(top = 6.dp))
                }
                items(list, key = { it.id }) { spell ->
                    SpellCard(
                        spell, expanded = expandedId == spell.id,
                        onToggle = { expandedId = if (expandedId == spell.id) null else spell.id },
                        onEdit = { openId = spell.id }
                    )
                }
            }
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
private fun SpellCard(spell: SpellEntry, expanded: Boolean, onToggle: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = .22f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(spell.name.ifBlank { "Sem nome" }, color = Parchment, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    val subtitle = listOf(spellLevelLabel(spell.level), spell.school).filter { it.isNotBlank() }.joinToString(" • ")
                    Text(subtitle, color = Gold, fontSize = 12.sp)
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
                if (spell.description.isNotBlank()) Text(spell.description, color = Parchment, fontSize = 13.sp)
                if (spell.higherLevels.isNotBlank()) {
                    Text("Em níveis superiores", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(spell.higherLevels, color = Muted, fontSize = 12.sp)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
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
        Text("$label:", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(value, color = Parchment, fontSize = 12.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SpellEditor(
    modifier: Modifier,
    spell: SpellEntry,
    onChange: (SpellEntry) -> Unit,
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
            CollapsibleSection("BÁSICO", summary = "${spellLevelLabel(spell.level)} • ${spell.school}") {
                TextInput("Nome", spell.name, { onChange(spell.copy(name = it)) })
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
                TextInput("Descrição", spell.description, { onChange(spell.copy(description = it)) }, singleLine = false, minLines = 5)
                TextInput("Em níveis superiores", spell.higherLevels, { onChange(spell.copy(higherLevels = it)) }, singleLine = false, minLines = 2)
            }
        }
    }
}
