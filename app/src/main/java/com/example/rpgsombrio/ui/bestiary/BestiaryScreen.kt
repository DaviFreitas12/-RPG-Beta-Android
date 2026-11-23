package com.example.rpgsombrio.ui.bestiary

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.rpgsombrio.data.CombatStore
import com.example.rpgsombrio.data.EntryKind
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.NamedEntry
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.data.shortResourceLabel
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.data.systemOptions
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.describeRoll
import com.example.rpgsombrio.engine.monsterCombatant
import com.example.rpgsombrio.engine.monsterHitPoints
import com.example.rpgsombrio.engine.monsterVariables
import com.example.rpgsombrio.engine.parseMonsterAction
import com.example.rpgsombrio.engine.primaryResource
import com.example.rpgsombrio.engine.rollAttributeCheck
import com.example.rpgsombrio.engine.rollInitiative
import com.example.rpgsombrio.engine.rollMonsterAction
import com.example.rpgsombrio.ui.characters.DefensesSection
import com.example.rpgsombrio.ui.components.AddRowButton
import com.example.rpgsombrio.ui.components.AttributeGrid
import com.example.rpgsombrio.ui.components.ChipFilterRow
import com.example.rpgsombrio.ui.components.CollapsibleSection
import com.example.rpgsombrio.ui.components.ConfirmDeleteDialog
import com.example.rpgsombrio.ui.components.EditorTopBar
import com.example.rpgsombrio.ui.components.EmptyHint
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.Portrait
import com.example.rpgsombrio.ui.components.PortraitField
import com.example.rpgsombrio.ui.components.RollLogBar
import com.example.rpgsombrio.ui.components.SystemPickerDialog
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.components.deletePortraitFile
import com.example.rpgsombrio.ui.components.listHeader
import com.example.rpgsombrio.ui.components.push
import com.example.rpgsombrio.ui.components.replaced
import com.example.rpgsombrio.ui.components.without
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel
import com.example.rpgsombrio.ui.theme.Panel2

private const val ALL_SYSTEMS = "Todos os sistemas"

@Composable
internal fun BestiaryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val monsters = remember { mutableStateListOf<MonsterSheet>().apply { addAll(SheetStore.loadMonsters(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("Todos") }
    var systemFilter by rememberSaveable { mutableStateOf(ALL_SYSTEMS) }
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
        val systemNames = monsters.map { systemDisplayName(prefs, it.systemId) }.distinct().sorted()
        val systemOptionsList = listOf(ALL_SYSTEMS) + systemNames
        val sysFilter = if (systemFilter in systemOptionsList) systemFilter else ALL_SYSTEMS
        val filtered = monsters
            .filter { m -> sysFilter == ALL_SYSTEMS || systemDisplayName(prefs, m.systemId) == sysFilter }
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
            if (systemOptionsList.size > 2) item { ChipFilterRow(systemOptionsList, sysFilter) { systemFilter = it } }
            if (typeOptions.size > 2) item { ChipFilterRow(typeOptions, filter) { typeFilter = it } }
            if (filtered.isEmpty()) item {
                EmptyHint(if (query.isBlank() && filter == "Todos" && sysFilter == ALL_SYSTEMS) "Nenhuma criatura ainda. Toque em Nova criatura." else "Nenhum resultado.")
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
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (monster.imageUri.isNotBlank()) Portrait(monster.imageUri, monster.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(monster.name.ifBlank { "Sem nome" }, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val subtitle = listOf(monster.type, monster.size).filter { it.isNotBlank() }.joinToString(" • ")
                if (subtitle.isNotBlank()) Text(subtitle, color = Accent, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (monster.challenge.isNotBlank()) InfoPill("ND ${monster.challenge}")
                    InfoPill(profile.name)
                    if (profile.usesArmorClass) InfoPill("CA ${monster.armorClass}")
                    InfoPill("${primaryResource(profile)?.let { shortResourceLabel(it) } ?: "PV"} ${monster.hp}")
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
    val engine = remember { RpgEngine() }
    val log = remember { mutableStateListOf<RollLine>() }
    var targetDefense by remember { mutableStateOf(0) }
    var quantity by remember { mutableStateOf(1) }
    var switching by remember { mutableStateOf(false) }
    val variables = monsterVariables(monster, profile)
    val defense = targetDefense.takeIf { it > 0 }
    val hpLabel = primaryResource(profile)?.let { shortResourceLabel(it) } ?: "PV"

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { EditorTopBar("Criatura • ${profile.name}", onBack, onDelete) }
            item {
                CollapsibleSection("IDENTIDADE", summary = listOf(monster.name, monster.type).filter { it.isNotBlank() }.joinToString(" • ")) {
                    PortraitField(monster.imageUri, monster.name) { onChange(monster.copy(imageUri = it)) }
                    TextInput("Nome", monster.name, { onChange(monster.copy(name = it)) })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Sistema", color = Muted, fontSize = 13.sp)
                            Text(systemDisplayName(prefs, monster.systemId), color = Ink, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = { switching = true }) { Text("Trocar sistema") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextInput("Tipo", monster.type, { onChange(monster.copy(type = it)) }, Modifier.weight(1f))
                        TextInput("Tamanho", monster.size, { onChange(monster.copy(size = it)) }, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextInput("ND / ameaça", monster.challenge, { onChange(monster.copy(challenge = it)) }, Modifier.weight(1f))
                        TextInput("Alinhamento", monster.alignment, { onChange(monster.copy(alignment = it)) }, Modifier.weight(1f))
                    }
                }
            }
            item {
                CollapsibleSection("ROLAGENS", summary = "Iniciativa, PV e combate") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            val result = rollInitiative(engine, profile, variables)
                            log.push(RollLine("Iniciativa: ${describeRoll(result.roll)}" + if (result.note.isBlank()) "" else " • ${result.note}"))
                        }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Casino, null); Spacer(Modifier.width(6.dp)); Text("Iniciativa") }
                        OutlinedButton(onClick = {
                            if (monster.hpFormula.isBlank()) {
                                log.push(RollLine("Preencha a fórmula de $hpLabel na seção Combate (ex.: 2d8+3).", null))
                            } else {
                                val hp = monsterHitPoints(engine, monster)
                                onChange(monster.copy(hp = hp))
                                log.push(RollLine("$hpLabel rolados (${monster.hpFormula}): $hp", true))
                            }
                        }, modifier = Modifier.weight(1f)) { Text("Rolar $hpLabel") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        NumberField("Quantidade", quantity, { quantity = it.coerceIn(1, 20) }, Modifier.weight(1f))
                        Button(onClick = {
                            val added = List(quantity) { i -> monsterCombatant(engine, monster, profile, if (quantity > 1) i + 1 else null) }
                            CombatStore.add(prefs, *added.toTypedArray())
                            log.push(RollLine("${added.size} × ${monster.name.ifBlank { "Criatura" }} no combate. Veja na aba Motor.", true))
                        }, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Ao combate") }
                    }
                    NumberField(
                        if (profile.usesArmorClass) "CA do alvo nas ações (0 = sem alvo)" else "Defesa do alvo nas ações (0 = sem alvo)",
                        targetDefense, { targetDefense = it.coerceAtLeast(0) }, Modifier.fillMaxWidth()
                    )
                    Text("Cada ação com \"+N para acertar\" ou \"Dano: XdY\" no texto ganha um botão de rolar. Cada cópia no combate rola os próprios $hpLabel e iniciativa.",
                        color = Muted, fontSize = 13.sp)
                }
            }
            item {
                CollapsibleSection("COMBATE", summary = "$hpLabel ${monster.hp}" + if (profile.usesArmorClass) " • CA ${monster.armorClass}" else "") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (profile.usesArmorClass) {
                            NumberField("CA", monster.armorClass, { onChange(monster.copy(armorClass = it)) }, Modifier.weight(1f))
                        }
                        NumberField(hpLabel, monster.hp, { onChange(monster.copy(hp = it.coerceAtLeast(0))) }, Modifier.weight(1f))
                    }
                    TextInput("Fórmula de $hpLabel (ex.: 2d8+3)", monster.hpFormula, { onChange(monster.copy(hpFormula = it)) })
                    TextInput("Deslocamento", monster.speed, { onChange(monster.copy(speed = it)) })
                }
            }
            item {
                CollapsibleSection("ATRIBUTOS", summary = "${profile.attributes.size} atributos") {
                    AttributeGrid(
                        profile, monster.attributes,
                        onRoll = { attr -> log.push(rollAttributeCheck(engine, profile, monster.attributes, attr)) }
                    ) { key, value ->
                        onChange(monster.copy(attributes = monster.attributes + (key to value)))
                    }
                }
            }
            item {
                DefensesSection(
                    defenses = monster.defenses, conditionImmunities = monster.conditionImmunities,
                    onDefensesChange = { onChange(monster.copy(defenses = it)) },
                    onConditionsChange = { onChange(monster.copy(conditionImmunities = it)) },
                    legacyText = monster.resistances,
                    onLegacyChange = { onChange(monster.copy(resistances = it)) }
                )
            }
            item {
                CollapsibleSection("PERÍCIAS, SENTIDOS E IDIOMAS", summary = monster.senses, startExpanded = false) {
                    TextInput("Salvaguardas (ex.: DES +3, SAB +2)", monster.savingThrows, { onChange(monster.copy(savingThrows = it)) })
                    TextInput("Perícias (ex.: Furtividade +4)", monster.skillsText, { onChange(monster.copy(skillsText = it)) })
                    TextInput("Sentidos", monster.senses, { onChange(monster.copy(senses = it)) })
                    TextInput("Idiomas", monster.languages, { onChange(monster.copy(languages = it)) })
                }
            }
            item { EntrySection("TRAÇOS", EntryKind.TRAIT, monster.entries, true, null) { onChange(monster.copy(entries = it)) } }
            item {
                EntrySection("AÇÕES", EntryKind.ACTION, monster.entries, true,
                    { entry -> log.push(rollMonsterAction(engine, entry, variables, profile, defense).line) }
                ) { onChange(monster.copy(entries = it)) }
            }
            item { EntrySection("REAÇÕES", EntryKind.REACTION, monster.entries, false, null) { onChange(monster.copy(entries = it)) } }
            item {
                EntrySection("AÇÕES LENDÁRIAS", EntryKind.LEGENDARY, monster.entries, false,
                    { entry -> log.push(rollMonsterAction(engine, entry, variables, profile, defense).line) }
                ) { onChange(monster.copy(entries = it)) }
            }
            item {
                CollapsibleSection("ANOTAÇÕES", summary = monster.notes.lineSequence().firstOrNull().orEmpty(), startExpanded = false) {
                    TextInput("Tesouro / saque", monster.loot, { onChange(monster.copy(loot = it)) })
                    TextInput("Táticas, ecologia, ganchos de aventura…", monster.notes, { onChange(monster.copy(notes = it)) },
                        singleLine = false, minLines = 4)
                }
            }
        }
        RollLogBar(log) { log.clear() }
    }

    if (switching) {
        SystemPickerDialog(
            title = "Vincular a outro sistema",
            options = systemOptions(prefs), currentId = monster.systemId,
            campaignId = SheetStore.activeSystemId(prefs),
            onPick = { id ->
                switching = false
                if (id != monster.systemId) onChange(SheetStore.rebindMonster(prefs, monster, id))
            },
            onDismiss = { switching = false }
        )
    }
}

/** Lista de entradas (traços, ações…) de um mesmo tipo dentro da lista única da criatura. */
@Composable
private fun EntrySection(
    title: String, kind: String, entries: List<NamedEntry>, startExpanded: Boolean,
    onRoll: ((NamedEntry) -> Unit)?,
    onChange: (List<NamedEntry>) -> Unit
) {
    val mine = entries.withIndex().filter { it.value.kind == kind }
    CollapsibleSection(title, summary = if (mine.isEmpty()) "Vazio" else "${mine.size} entradas", startExpanded = startExpanded) {
        if (mine.isEmpty()) EmptyHint("Nenhuma entrada.")
        mine.forEach { indexed ->
            val index = indexed.index
            val entry = indexed.value
            Surface(color = Panel2, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextInput("Nome", entry.name, { onChange(entries.replaced(index, entry.copy(name = it))) }, Modifier.weight(1f))
                        if (onRoll != null && parseMonsterAction(entry.text) != null) {
                            IconButton(onClick = { onRoll(entry) }) { Icon(Icons.Default.Casino, "Rolar ${entry.name}", tint = Accent) }
                        }
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
