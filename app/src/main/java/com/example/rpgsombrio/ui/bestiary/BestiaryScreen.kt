package com.example.rpgsombrio.ui.bestiary

import com.example.rpgsombrio.ui.components.EmptyState
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import com.example.rpgsombrio.data.BestiaryCategoryStore
import com.example.rpgsombrio.engine.NO_CATEGORY
import com.example.rpgsombrio.engine.inCategory
import com.example.rpgsombrio.engine.monsterCategories
import com.example.rpgsombrio.engine.typeKey
import com.example.rpgsombrio.engine.uncategorizedCount
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
import com.example.rpgsombrio.data.missingSrdMonsters
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
private const val ALL_CATEGORY = "Todas as criaturas"

/**
 * Bestiário por categorias (0.9.18): ao abrir, o mestre vê as categorias (Aberração, Construto, Fera...) com a
 * quantidade de criaturas; tocar numa categoria abre a lista daquele tipo. A categoria é o campo "Tipo" da criatura.
 * O mestre pode criar categorias próprias, e a busca no topo procura em todas.
 */
@Composable
internal fun BestiaryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val monsters = remember { mutableStateListOf<MonsterSheet>().apply { addAll(SheetStore.loadMonsters(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var systemFilter by rememberSaveable { mutableStateOf(ALL_SYSTEMS) }
    var deleteTarget by remember { mutableStateOf<MonsterSheet?>(null) }
    var customCategories by remember { mutableStateOf(BestiaryCategoryStore.load(prefs)) }
    var newCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    fun persist() = SheetStore.saveMonsters(prefs, monsters.toList())

    val current = monsters.firstOrNull { it.id == openId }
    BackHandler(enabled = current != null) { openId = null }
    BackHandler(enabled = current == null && category != null) { category = null; query = "" }

    val types = monsters.map { it.type }
    val categories = monsterCategories(customCategories, types)

    /** Cria a criatura já na categoria aberta (nas pastas "Todas" e "Sem categoria" ela nasce sem tipo). */
    fun addMonster() {
        val picked = category?.takeIf { it != ALL_CATEGORY && it != NO_CATEGORY }.orEmpty()
        val monster = SheetStore.newMonster(prefs).copy(type = picked)
        monsters.add(0, monster); persist(); openId = monster.id
    }
    fun matches(m: MonsterSheet) = listOf(m.name, m.type, m.size, m.challenge).any { it.contains(query, ignoreCase = true) }

    if (current != null) {
        MonsterEditor(
            modifier, current, prefs,
            categoryNames = categories.map { it.name },
            onChange = { updated ->
                val i = monsters.indexOfFirst { it.id == updated.id }
                if (i >= 0) { monsters[i] = updated; persist() }
            },
            onBack = { openId = null },
            onDelete = { deleteTarget = current }
        )
    } else if (category == null) {
        // Entrada: as categorias. Com algo na busca, mostra direto as criaturas encontradas, de qualquer categoria.
        val searching = query.isNotBlank()
        val hits = if (searching) monsters.filter { matches(it) }.sortedBy { it.name.lowercase() } else emptyList()
        val noCategory = uncategorizedCount(types)
        LazyColumn(
            modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listHeader("Criaturas", "Bestiário", "Escolha uma categoria ou busque pelo nome",
                query, { query = it }, "Nova criatura") { addMonster() }
            if (searching) {
                if (hits.isEmpty()) item { EmptyHint("Nenhum resultado.") }
                items(hits, key = { it.id }) { monster -> MonsterCard(monster, prefs) { openId = monster.id } }
            } else {
                item { CategoryCard(ALL_CATEGORY, monsters.size, null) { category = ALL_CATEGORY } }
                items(categories, key = { it.name }) { c ->
                    CategoryCard(
                        c.name, c.count,
                        onDelete = if (c.isCustom && c.count == 0) {
                            {
                                customCategories = customCategories.filter { typeKey(it) != typeKey(c.name) }
                                BestiaryCategoryStore.save(prefs, customCategories)
                            }
                        } else null
                    ) { category = c.name }
                }
                if (noCategory > 0) item { CategoryCard(NO_CATEGORY, noCategory, null) { category = NO_CATEGORY } }
                val missingSrd = missingSrdMonsters(monsters.toList())
                if (missingSrd.isNotEmpty()) item {
                    OutlinedButton(
                        onClick = { monsters.addAll(missingSrd); persist() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Importar bestiário SRD 5.1 (${missingSrd.size} criaturas)") }
                }
                item { AddRowButton("Nova categoria") { newCategoryName = ""; newCategoryDialog = true } }
            }
        }
    } else {
        val chosen = category.orEmpty()
        val systemNames = monsters.map { systemDisplayName(prefs, it.systemId) }.distinct().sorted()
        val systemOptionsList = listOf(ALL_SYSTEMS) + systemNames
        val sysFilter = if (systemFilter in systemOptionsList) systemFilter else ALL_SYSTEMS
        val inFolder = monsters.filter { chosen == ALL_CATEGORY || inCategory(it.type, chosen) }
        val filtered = inFolder
            .filter { m -> sysFilter == ALL_SYSTEMS || systemDisplayName(prefs, m.systemId) == sysFilter }
            .filter { m -> matches(m) }
            .sortedBy { it.name.lowercase() }
        LazyColumn(
            modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { TextButton(onClick = { category = null; query = "" }) { Text("← Categorias", color = Accent) } }
            listHeader("Categoria", chosen, if (inFolder.size == 1) "1 criatura" else "${inFolder.size} criaturas",
                query, { query = it }, "Nova criatura") { addMonster() }
            if (systemOptionsList.size > 2) item { ChipFilterRow(systemOptionsList, sysFilter) { systemFilter = it } }
            if (filtered.isEmpty()) item {
                if (query.isBlank() && sysFilter == ALL_SYSTEMS) EmptyState("criatura", "Nenhuma criatura em $chosen", "Toque em Nova criatura para adicionar a esta categoria.") else EmptyHint("Nenhum resultado.")
            }
            items(filtered, key = { it.id }) { monster -> MonsterCard(monster, prefs) { openId = monster.id } }
        }
    }

    if (newCategoryDialog) {
        AlertDialog(
            onDismissRequest = { newCategoryDialog = false },
            title = { Text("Nova categoria") },
            text = { TextInput("Nome (ex.: Demônio)", newCategoryName, { newCategoryName = it }) },
            confirmButton = {
                TextButton(onClick = {
                    val name = newCategoryName.trim()
                    if (name.isNotEmpty() && categories.none { typeKey(it.name) == typeKey(name) }) {
                        customCategories = customCategories + name
                        BestiaryCategoryStore.save(prefs, customCategories)
                    }
                    newCategoryDialog = false
                }) { Text("Criar") }
            },
            dismissButton = { TextButton(onClick = { newCategoryDialog = false }) { Text("Cancelar") } }
        )
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
private fun CategoryCard(name: String, count: Int, onDelete: (() -> Unit)?, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Default.Pets, contentDescription = null, tint = Accent)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(if (count == 1) "1 criatura" else "$count criaturas", color = Muted, fontSize = 13.sp)
            }
            if (onDelete != null) IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Apagar categoria", tint = Muted)
            }
        }
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
    categoryNames: List<String>,
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
                    if (categoryNames.isNotEmpty()) {
                        Text("Categoria (toque para escolher)", color = Muted, fontSize = 13.sp)
                        ChipFilterRow(categoryNames, categoryNames.firstOrNull { typeKey(it) == typeKey(monster.type) } ?: "") {
                            onChange(monster.copy(type = it))
                        }
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
                            val added = List(quantity) { i -> monsterCombatant(engine, monster, profile, if (quantity > 1) i + 1 else null).copy(initiative = 0) }
                            CombatStore.add(prefs, *added.toTypedArray())
                            log.push(RollLine("${added.size} × ${monster.name.ifBlank { "Criatura" }} no combate. Veja na aba Mesa e digite a iniciativa.", true))
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
