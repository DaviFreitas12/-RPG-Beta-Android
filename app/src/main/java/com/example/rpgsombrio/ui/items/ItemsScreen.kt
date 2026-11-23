package com.example.rpgsombrio.ui.items

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.ARMOR_TYPE
import com.example.rpgsombrio.data.ITEM_TYPES
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SystemOption
import com.example.rpgsombrio.data.formatWeight
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.data.systemOptions
import com.example.rpgsombrio.ui.components.ChipFilterRow
import com.example.rpgsombrio.ui.components.CollapsibleSection
import com.example.rpgsombrio.ui.components.ConfirmDeleteDialog
import com.example.rpgsombrio.ui.components.DecimalField
import com.example.rpgsombrio.ui.components.DuplicateToSystem
import com.example.rpgsombrio.ui.components.EditorTopBar
import com.example.rpgsombrio.ui.components.EmptyHint
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.SystemBinding
import com.example.rpgsombrio.ui.components.SystemFilterRow
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.components.ToggleRow
import com.example.rpgsombrio.ui.components.fitsFilter
import com.example.rpgsombrio.ui.components.listHeader
import com.example.rpgsombrio.ui.components.validSystemFilter
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel

@Composable
internal fun ItemsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val itemList = remember { mutableStateListOf<ItemEntry>().apply { addAll(SheetStore.loadItems(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("Todos") }
    var systemFilter by rememberSaveable { mutableStateOf(SheetStore.activeSystemId(prefs)) }
    val sysOptions = remember { systemOptions(prefs) }
    var deleteTarget by remember { mutableStateOf<ItemEntry?>(null) }
    fun persist() = SheetStore.saveItems(prefs, itemList.toList())

    val current = itemList.firstOrNull { it.id == openId }
    BackHandler(enabled = current != null) { openId = null }

    if (current != null) {
        ItemEditor(
            modifier, current, prefs, sysOptions,
            onChange = { updated ->
                val i = itemList.indexOfFirst { it.id == updated.id }
                if (i >= 0) { itemList[i] = updated; persist() }
            },
            onDuplicate = { sid ->
                val copy = current.copy(id = SheetStore.newId(), systemId = sid)
                itemList.add(0, copy); persist(); openId = copy.id
            },
            onBack = { openId = null },
            onDelete = { deleteTarget = current }
        )
    } else {
        val sysFilter = validSystemFilter(sysOptions, systemFilter)
        val typeOptions = listOf("Todos") + itemList.map { it.type.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        val filter = if (typeFilter in typeOptions) typeFilter else "Todos"
        val filtered = itemList
            .filter { row -> fitsFilter(row.systemId, sysFilter) }
            .filter { row -> filter == "Todos" || row.type.trim() == filter }
            .filter { row -> listOf(row.name, row.type, row.rarity, row.properties, row.description).any { v -> v.contains(query, ignoreCase = true) } }
            .sortedBy { it.name.lowercase() }
        LazyColumn(
            modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listHeader("Armas, armaduras e tesouros", "Itens", "Biblioteca de itens que as fichas podem carregar",
                query, { query = it }, "Novo item") {
                val created = SheetStore.newItem(SheetStore.activeSystemId(prefs))
                itemList.add(0, created); persist(); openId = created.id
            }
            item { SystemFilterRow(sysOptions, sysFilter) { systemFilter = it } }
            if (typeOptions.size > 2) item { ChipFilterRow(typeOptions, filter) { typeFilter = it } }
            if (filtered.isEmpty()) item {
                EmptyHint(if (query.isBlank() && filter == "Todos") "Nenhum item neste filtro. Toque em Novo item ou mude o sistema." else "Nenhum resultado.")
            }
            items(filtered, key = { it.id }) { entry ->
                ItemCard(entry, if (entry.systemId.isBlank()) "Todos os sistemas" else systemDisplayName(prefs, entry.systemId)) { openId = entry.id }
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(target.name, "este item da biblioteca (fichas que o carregam deixam de exibi-lo)", onConfirm = {
            itemList.removeAll { it.id == target.id }
            persist()
            if (openId == target.id) openId = null
            deleteTarget = null
        }, onDismiss = { deleteTarget = null })
    }
}

@Composable
private fun ItemCard(item: ItemEntry, systemLabel: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.name.ifBlank { "Sem nome" }, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            val subtitle = listOf(item.type, item.rarity, systemLabel).filter { it.isNotBlank() }.joinToString(" • ")
            if (subtitle.isNotBlank()) Text(subtitle, color = Accent, fontSize = 12.sp)
            if (item.bonus.isNotBlank() || item.damage.isNotBlank() || item.armorClass != 0 || item.weight > 0.0 || item.value.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (item.bonus.isNotBlank()) InfoPill(item.bonus)
                    if (item.damage.isNotBlank()) InfoPill(item.damage)
                    if (item.armorClass != 0) InfoPill(if (item.type == ARMOR_TYPE) "CA ${item.armorClass}" else "CA ${if (item.armorClass > 0) "+" else ""}${item.armorClass}")
                    if (item.weight > 0.0) InfoPill("${formatWeight(item.weight)} kg")
                    if (item.value.isNotBlank()) InfoPill(item.value)
                }
            }
        }
    }
}

@Composable
private fun ItemEditor(
    modifier: Modifier,
    entry: ItemEntry,
    prefs: android.content.SharedPreferences,
    sysOptions: List<com.example.rpgsombrio.data.SystemOption>,
    onChange: (ItemEntry) -> Unit,
    onDuplicate: (String) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { EditorTopBar("Item", onBack, onDelete) }
        item {
            CollapsibleSection("IDENTIDADE", summary = listOf(entry.name, entry.type).filter { it.isNotBlank() }.joinToString(" • ")) {
                TextInput("Nome", entry.name, { onChange(entry.copy(name = it)) })
                Text("Tipo", color = Muted, fontSize = 13.sp)
                ChipFilterRow(ITEM_TYPES, entry.type) { onChange(entry.copy(type = it)) }
                TextInput("Tipo (livre)", entry.type, { onChange(entry.copy(type = it)) })
                TextInput("Raridade", entry.rarity, { onChange(entry.copy(rarity = it)) })
            }
        }
        item {
            CollapsibleSection("SISTEMA", summary = if (entry.systemId.isBlank()) "Todos os sistemas" else systemDisplayName(prefs, entry.systemId)) {
                SystemBinding(
                    systemName = if (entry.systemId.isBlank()) "Todos os sistemas" else systemDisplayName(prefs, entry.systemId),
                    hint = "Cada sistema usa o item de um jeito. Só as fichas deste sistema (ou itens de todos) o veem no inventário.",
                    options = sysOptions, currentId = entry.systemId, campaignId = SheetStore.activeSystemId(prefs),
                    onPick = { onChange(entry.copy(systemId = it)) }
                )
                DuplicateToSystem(sysOptions, entry.systemId, SheetStore.activeSystemId(prefs), onDuplicate)
            }
        }
        item {
            CollapsibleSection("DETALHES", summary = listOf(entry.bonus, entry.damage).filter { it.isNotBlank() }.joinToString(" • ")) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DecimalField("Peso (kg)", entry.weight, { onChange(entry.copy(weight = it)) }, Modifier.weight(1f))
                    TextInput("Valor", entry.value, { onChange(entry.copy(value = it)) }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput("Bônus", entry.bonus, { onChange(entry.copy(bonus = it)) }, Modifier.weight(1f))
                    TextInput("Dano / efeito", entry.damage, { onChange(entry.copy(damage = it)) }, Modifier.weight(1.4f))
                }
                NumberField(
                    if (entry.type == ARMOR_TYPE) "CA da armadura (substitui a CA base; 0 = nenhuma)" else "Bônus de CA quando equipado (0 = nenhum)",
                    entry.armorClass, { onChange(entry.copy(armorClass = it)) }, Modifier.fillMaxWidth()
                )
                TextInput("Propriedades", entry.properties, { onChange(entry.copy(properties = it)) }, singleLine = false, minLines = 2)
                ToggleRow("Requer sintonização", entry.attunement) { onChange(entry.copy(attunement = it)) }
            }
        }
        item {
            CollapsibleSection("DESCRIÇÃO", summary = entry.description.lineSequence().firstOrNull().orEmpty()) {
                TextInput("Descrição", entry.description, { onChange(entry.copy(description = it)) }, singleLine = false, minLines = 5)
            }
        }
    }
}
