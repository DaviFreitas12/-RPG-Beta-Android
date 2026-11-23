package com.example.rpgsombrio.ui.traits

import com.example.rpgsombrio.ui.components.EmptyState
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.rpgsombrio.data.TraitEntry
import com.example.rpgsombrio.data.TraitKind
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.data.systemOptions
import com.example.rpgsombrio.data.traitCostLabel
import com.example.rpgsombrio.ui.components.ChipFilterRow
import com.example.rpgsombrio.ui.components.CollapsibleSection
import com.example.rpgsombrio.ui.components.ConfirmDeleteDialog
import com.example.rpgsombrio.ui.components.DuplicateToSystem
import com.example.rpgsombrio.ui.components.EditorTopBar
import com.example.rpgsombrio.ui.components.EmptyHint
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.SystemFilterRow
import com.example.rpgsombrio.ui.components.SystemPickerDialog
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.components.fitsFilter
import com.example.rpgsombrio.ui.components.listHeader
import com.example.rpgsombrio.ui.components.validSystemFilter
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Danger
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel

private const val KIND_ALL = "Todos"
private const val KIND_ADVANTAGE = "Vantagens"
private const val KIND_DISADVANTAGE = "Desvantagens"

@Composable
internal fun TraitsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val traits = remember { mutableStateListOf<TraitEntry>().apply { addAll(SheetStore.loadTraits(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var kindFilter by rememberSaveable { mutableStateOf(KIND_ALL) }
    var systemFilter by rememberSaveable { mutableStateOf(SheetStore.activeSystemId(prefs)) }
    val sysOptions = remember { systemOptions(prefs) }
    var deleteTarget by remember { mutableStateOf<TraitEntry?>(null) }
    fun persist() = SheetStore.saveTraits(prefs, traits.toList())

    val current = traits.firstOrNull { it.id == openId }
    BackHandler(enabled = current != null) { openId = null }

    if (current != null) {
        TraitEditor(
            modifier, current, prefs,
            onChange = { updated ->
                val i = traits.indexOfFirst { it.id == updated.id }
                if (i >= 0) { traits[i] = updated; persist() }
            },
            onDuplicate = { sid ->
                val copy = current.copy(id = SheetStore.newId(), systemId = sid)
                traits.add(0, copy); persist(); openId = copy.id
            },
            onBack = { openId = null },
            onDelete = { deleteTarget = current }
        )
    } else {
        val sysFilter = validSystemFilter(sysOptions, systemFilter)
        val filtered = traits
            .filter { t -> fitsFilter(t.systemId, sysFilter) }
            .filter { t ->
                when (kindFilter) {
                    KIND_ADVANTAGE -> t.isAdvantage
                    KIND_DISADVANTAGE -> !t.isAdvantage
                    else -> true
                }
            }
            .filter { t -> listOf(t.name, t.category, t.description).any { it.contains(query, ignoreCase = true) } }
            .sortedWith(compareBy<TraitEntry>({ !it.isAdvantage }, { it.name.lowercase() }))
        LazyColumn(
            modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listHeader("Pontos de personagem", "Vantagens e desvantagens",
                "Traços com custo, vinculados a um sistema e às fichas",
                query, { query = it }, "Novo traço") {
                val created = SheetStore.newTrait().copy(systemId = SheetStore.activeSystemId(prefs))
                traits.add(0, created); persist(); openId = created.id
            }
            item { SystemFilterRow(sysOptions, sysFilter) { systemFilter = it } }
            item { ChipFilterRow(listOf(KIND_ALL, KIND_ADVANTAGE, KIND_DISADVANTAGE), kindFilter) { kindFilter = it } }
            if (filtered.isEmpty()) item {
                if (query.isBlank() && kindFilter == KIND_ALL) EmptyState("traco", "Nenhum traço ainda", "Toque em Novo traço para criar vantagens e desvantagens.") else EmptyHint("Nenhum resultado.")
            }
            items(filtered, key = { it.id }) { trait -> TraitCard(trait, prefs) { openId = trait.id } }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(target.name, "este traço da biblioteca (fichas que o usam deixam de exibi-lo)", onConfirm = {
            traits.removeAll { it.id == target.id }
            persist()
            if (openId == target.id) openId = null
            deleteTarget = null
        }, onDismiss = { deleteTarget = null })
    }
}

@Composable
private fun TraitCard(trait: TraitEntry, prefs: android.content.SharedPreferences, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, (if (trait.isAdvantage) Accent else Danger).copy(alpha = .35f))
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(trait.name.ifBlank { "Sem nome" }, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val subtitle = listOf(if (trait.isAdvantage) "Vantagem" else "Desvantagem", trait.category)
                .filter { it.isNotBlank() }.joinToString(" • ")
            Text(subtitle, color = Accent, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoPill(traitCostLabel(trait))
                InfoPill(if (trait.systemId.isBlank()) "Todos os sistemas" else systemDisplayName(prefs, trait.systemId))
            }
        }
    }
}

@Composable
private fun TraitEditor(
    modifier: Modifier,
    entry: TraitEntry,
    prefs: android.content.SharedPreferences,
    onChange: (TraitEntry) -> Unit,
    onDuplicate: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    var pickingSystem by remember { mutableStateOf(false) }
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { EditorTopBar("Vantagem / desvantagem", onBack, onDelete) }
        item {
            CollapsibleSection("IDENTIDADE", summary = listOf(entry.name, traitCostLabel(entry)).filter { it.isNotBlank() }.joinToString(" • ")) {
                TextInput("Nome", entry.name, { onChange(entry.copy(name = it)) })
                Text("Tipo", color = Muted, fontSize = 13.sp)
                ChipFilterRow(
                    listOf("Vantagem", "Desvantagem"),
                    if (entry.isAdvantage) "Vantagem" else "Desvantagem"
                ) { onChange(entry.copy(kind = if (it == "Vantagem") TraitKind.ADVANTAGE else TraitKind.DISADVANTAGE)) }
                NumberField(
                    if (entry.isAdvantage) "Custo (pontos gastos)" else "Pontos devolvidos",
                    entry.cost, { onChange(entry.copy(cost = it.coerceAtLeast(0))) }
                )
                Text(
                    if (entry.isAdvantage) "Vantagens gastam pontos do orçamento da ficha."
                    else "Desvantagens devolvem pontos ao orçamento da ficha.",
                    color = Muted, fontSize = 13.sp
                )
                TextInput("Categoria (física, social, mental…)", entry.category, { onChange(entry.copy(category = it)) })
            }
        }
        item {
            CollapsibleSection("SISTEMA", summary = if (entry.systemId.isBlank()) "Todos os sistemas" else systemDisplayName(prefs, entry.systemId)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Disponível em", color = Muted, fontSize = 13.sp)
                        Text(
                            if (entry.systemId.isBlank()) "Todos os sistemas" else systemDisplayName(prefs, entry.systemId),
                            color = Ink, fontWeight = FontWeight.SemiBold
                        )
                    }
                    TextButton(onClick = { pickingSystem = true }) { Text("Alterar") }
                }
                Text("Só as fichas desse sistema veem este traço na hora de vincular.", color = Muted, fontSize = 13.sp)
                DuplicateToSystem(systemOptions(prefs), entry.systemId, SheetStore.activeSystemId(prefs), onDuplicate)
            }
        }
        item {
            CollapsibleSection("DESCRIÇÃO", summary = entry.description.lineSequence().firstOrNull().orEmpty()) {
                TextInput("Descrição e efeitos", entry.description, { onChange(entry.copy(description = it)) }, singleLine = false, minLines = 5)
            }
        }
    }
    if (pickingSystem) {
        SystemPickerDialog(
            title = "Disponível em", options = systemOptions(prefs), currentId = entry.systemId,
            campaignId = SheetStore.activeSystemId(prefs), allowAll = true,
            onPick = { pickingSystem = false; onChange(entry.copy(systemId = it)) },
            onDismiss = { pickingSystem = false }
        )
    }
}
