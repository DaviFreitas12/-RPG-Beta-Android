package com.example.rpgsombrio

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.rpgsombrio.data.EntryKind
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.NamedEntry
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.SystemProfiles

@Composable
internal fun BestiaryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val monsters = remember { mutableStateListOf<MonsterSheet>().apply { addAll(SheetStore.loadMonsters(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("Todos") }
    var deleteTarget by remember { mutableStateOf<MonsterSheet?>(null) }
    fun persist() = SheetStore.saveMonsters(prefs, monsters.toList())

    val current = monsters.firstOrNull { it.id == openId }
    BackHandler(enabled = current != null) { openId = null }

    if (current != null) {
        MonsterEditor(
            modifier, current, prefs,
            onChange = { updated ->
                val i = monsters.indexOfFirst { it.id == updated.id }
                if (i >= 0) { monsters[i] = updated; persist() }
            },
            onBack = { openId = null },
            onDelete = { deleteTarget = current }
        )
    } else {
        val typeOptions = listOf("Todos") + monsters.map { it.type.trim() }.filter { it.isNotEmpty() }.distinct().sorted()
        val filter = if (typeFilter in typeOptions) typeFilter else "Todos"
        val filtered = monsters
            .filter { m -> filter == "Todos" || m.type.trim() == filter }
            .filter { m -> listOf(m.name, m.type, m.size, m.challenge).any { it.contains(query, ignoreCase = true) } }
            .sortedBy { it.name.lowercase() }
        LazyColumn(
            modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listHeader("Criaturas", "Bestiário", "Fichas de criaturas e ameaças",
                query, { query = it }, "Nova criatura") {
                val monster = SheetStore.newMonster(prefs)
                monsters.add(0, monster); persist(); openId = monster.id
            }
            if (typeOptions.size > 2) item { ChipFilterRow(typeOptions, filter) { typeFilter = it } }
            if (filtered.isEmpty()) item {
                EmptyHint(if (query.isBlank() && filter == "Todos") "Nenhuma criatura ainda. Toque em Nova criatura." else "Nenhum resultado.")
            }
            items(filtered, key = { it.id }) { monster -> MonsterCard(monster, prefs) { openId = monster.id } }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(target.name, "esta criatura", onConfirm = {
            deletePortraitFile(context, target.imageUri)
            monsters.removeAll { it.id == target.id }
            persist()
            if (openId == target.id) openId = null
            deleteTarget = null
        }, onDismiss = { deleteTarget = null })
    }
}

@Composable
private fun MonsterCard(monster: MonsterSheet, prefs: SharedPreferences, onClick: () -> Unit) {
    val profile = remember(monster.systemId) { SystemProfiles.resolve(prefs, monster.systemId) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = .22f))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (monster.imageUri.isNotBlank()) Portrait(monster.imageUri, monster.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(monster.name.ifBlank { "Sem nome" }, color = Parchment, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val subtitle = listOf(monster.type, monster.size).filter { it.isNotBlank() }.joinToString(" • ")
                if (subtitle.isNotBlank()) Text(subtitle, color = Gold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (monster.challenge.isNotBlank()) InfoPill("ND ${monster.challenge}")
                    if (profile.usesArmorClass) InfoPill("CA ${monster.armorClass}")
                    InfoPill("PV ${monster.hp}")
                }
            }
        }
    }
}

@Composable
private fun MonsterEditor(
    modifier: Modifier,
    monster: MonsterSheet,
    prefs: SharedPreferences,
    onChange: (MonsterSheet) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val profile = remember(monster.systemId) { SystemProfiles.resolve(prefs, monster.systemId) }
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { EditorTopBar("Criatura • ${profile.name}", onBack, onDelete) }
        item {
            CollapsibleSection("IDENTIDADE", summary = listOf(monster.name, monster.type).filter { it.isNotBlank() }.joinToString(" • ")) {
                PortraitField(monster.imageUri, monster.name) { onChange(monster.copy(imageUri = it)) }
                TextInput("Nome", monster.name, { onChange(monster.copy(name = it)) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextInput("Tipo", monster.type, { onChange(monster.copy(type = it)) }, Modifier.weight(1f))
                    TextInput("Tamanho", monster.size, { onChange(monster.copy(size = it)) }, Modifier.weight(1f))
                }
                TextInput("ND / nível de ameaça", monster.challenge, { onChange(monster.copy(challenge = it)) })
            }
        }
        item {
            CollapsibleSection("COMBATE", summary = "PV ${monster.hp}" + if (profile.usesArmorClass) " • CA ${monster.armorClass}" else "") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (profile.usesArmorClass) {
                        NumberField("CA", monster.armorClass, { onChange(monster.copy(armorClass = it)) }, Modifier.weight(1f))
                    }
                    NumberField("PV", monster.hp, { onChange(monster.copy(hp = it.coerceAtLeast(0))) }, Modifier.weight(1f))
                }
                TextInput("Fórmula de PV (ex.: 2d8+3)", monster.hpFormula, { onChange(monster.copy(hpFormula = it)) })
                TextInput("Deslocamento", monster.speed, { onChange(monster.copy(speed = it)) })
            }
        }
        item {
            CollapsibleSection("ATRIBUTOS", summary = "${profile.attributes.size} atributos") {
                AttributeGrid(profile, monster.attributes) { key, value ->
                    onChange(monster.copy(attributes = monster.attributes + (key to value)))
                }
            }
        }
        item {
            CollapsibleSection("DEFESAS E SENTIDOS", summary = monster.senses, startExpanded = false) {
                TextInput("Resistências / imunidades / vulnerabilidades", monster.resistances, { onChange(monster.copy(resistances = it)) },
                    singleLine = false, minLines = 2)
                TextInput("Sentidos", monster.senses, { onChange(monster.copy(senses = it)) })
                TextInput("Idiomas", monster.languages, { onChange(monster.copy(languages = it)) })
            }
        }
        item { EntrySection("TRAÇOS", EntryKind.TRAIT, monster.entries, true) { onChange(monster.copy(entries = it)) } }
        item { EntrySection("AÇÕES", EntryKind.ACTION, monster.entries, true) { onChange(monster.copy(entries = it)) } }
        item { EntrySection("REAÇÕES", EntryKind.REACTION, monster.entries, false) { onChange(monster.copy(entries = it)) } }
        item { EntrySection("AÇÕES LENDÁRIAS", EntryKind.LEGENDARY, monster.entries, false) { onChange(monster.copy(entries = it)) } }
        item {
            CollapsibleSection("ANOTAÇÕES", summary = monster.notes.lineSequence().firstOrNull().orEmpty(), startExpanded = false) {
                TextInput("Táticas, ecologia, ganchos de aventura…", monster.notes, { onChange(monster.copy(notes = it)) },
                    singleLine = false, minLines = 4)
            }
        }
    }
}

/** Lista de entradas (traços, ações…) de um mesmo tipo dentro da lista única da criatura. */
@Composable
private fun EntrySection(
    title: String, kind: String, entries: List<NamedEntry>, startExpanded: Boolean,
    onChange: (List<NamedEntry>) -> Unit
) {
    val mine = entries.withIndex().filter { it.value.kind == kind }
    CollapsibleSection(title, summary = if (mine.isEmpty()) "Vazio" else "${mine.size} entradas", startExpanded = startExpanded) {
        if (mine.isEmpty()) EmptyHint("Nenhuma entrada.")
        mine.forEach { indexed ->
            val index = indexed.index
            val entry = indexed.value
            Surface(color = Panel2, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextInput("Nome", entry.name, { onChange(entries.replaced(index, entry.copy(name = it))) }, Modifier.weight(1f))
                        IconButton(onClick = { onChange(entries.without(index)) }) {
                            Icon(Icons.Default.Close, "Remover entrada", tint = Muted)
                        }
                    }
                    TextInput("Descrição", entry.text, { onChange(entries.replaced(index, entry.copy(text = it))) },
                        singleLine = false, minLines = 2)
                }
            }
        }
        AddRowButton("Adicionar") { onChange(entries + NamedEntry(kind)) }
    }
}
