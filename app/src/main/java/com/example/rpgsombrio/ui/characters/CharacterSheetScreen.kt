package com.example.rpgsombrio.ui.characters

import com.example.rpgsombrio.ui.components.EmptyState
import com.example.rpgsombrio.ui.components.EditorCard
import com.example.rpgsombrio.ui.combat.RoleSelector
import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.example.rpgsombrio.data.initialSheetTab
import com.example.rpgsombrio.data.visibleAttributes
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.example.rpgsombrio.data.AttackEntry
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.CombatStore
import com.example.rpgsombrio.data.InventoryEntry
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.ResourceValue
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SkillEntry
import com.example.rpgsombrio.data.SpellEntry
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.data.TraitEntry
import com.example.rpgsombrio.data.fitsSystem
import com.example.rpgsombrio.data.formatWeight
import com.example.rpgsombrio.data.inventoryWeight
import com.example.rpgsombrio.data.shortResourceLabel
import com.example.rpgsombrio.data.spellLevelLabel
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.data.systemOptions
import com.example.rpgsombrio.data.traitCostLabel
import com.example.rpgsombrio.data.traitPointsSpent
import com.example.rpgsombrio.engine.AttackOption
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.armorClassBreakdown
import com.example.rpgsombrio.engine.attributeVariable
import com.example.rpgsombrio.engine.castSpell
import com.example.rpgsombrio.engine.characterAttacks
import com.example.rpgsombrio.engine.characterCombatant
import com.example.rpgsombrio.engine.characterVariables
import com.example.rpgsombrio.engine.defaultTestFormula
import com.example.rpgsombrio.engine.describeRoll
import com.example.rpgsombrio.engine.effectiveArmorClass
import com.example.rpgsombrio.engine.rollAttributeCheck
import com.example.rpgsombrio.engine.rollFormula
import com.example.rpgsombrio.engine.rollInitiative
import com.example.rpgsombrio.engine.rollOption
import com.example.rpgsombrio.engine.rollPoolLine
import com.example.rpgsombrio.engine.rollSaveCheck
import com.example.rpgsombrio.engine.rollSkillCheck
import com.example.rpgsombrio.engine.skillBonus
import com.example.rpgsombrio.engine.skillPoolSize
import com.example.rpgsombrio.engine.variableName
import com.example.rpgsombrio.ui.avatar.AppearanceSection
import com.example.rpgsombrio.ui.avatar.AvatarView
import com.example.rpgsombrio.ui.components.AddRowButton
import com.example.rpgsombrio.ui.components.AttributeGrid
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
import com.example.rpgsombrio.ui.components.signed
import com.example.rpgsombrio.ui.components.without
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Night
import com.example.rpgsombrio.ui.theme.Panel
import com.example.rpgsombrio.ui.theme.Panel2

@Composable
internal fun CharacterSheetsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val sheets = remember { mutableStateListOf<CharacterSheet>().apply { addAll(SheetStore.loadCharacters(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CharacterSheet?>(null) }
    var choosingSystem by remember { mutableStateOf(false) }
    var namingSystemId by rememberSaveable { mutableStateOf<String?>(null) }
    var newSheetId by rememberSaveable { mutableStateOf<String?>(null) }
    fun persist() = SheetStore.saveCharacters(prefs, sheets.toList())
    // Digitar regrava todas as fichas a cada letra; agrupa as gravações e garante a última ao sair da tela.
    val scope = rememberCoroutineScope()
    val pendingSave = remember { arrayOfNulls<Job>(1) }
    fun persistSoon() {
        pendingSave[0]?.cancel()
        pendingSave[0] = scope.launch { delay(300); persist() }
    }
    DisposableEffect(Unit) { onDispose { persist() } }

    val current = sheets.firstOrNull { it.id == openId }
    BackHandler(enabled = current != null) { openId = null }
    // A "ficha recém-criada" só vale até fechar o editor; ao reabrir, a ficha abre na aba normal.
    LaunchedEffect(openId) { if (openId == null) newSheetId = null }

    if (current != null) {
        CharacterEditor(
            modifier, current, prefs,
            startOnProfile = current.id == newSheetId,
            onChange = { updated ->
                val i = sheets.indexOfFirst { it.id == updated.id }
                if (i >= 0) { sheets[i] = updated; persistSoon() }
            },
            onBack = { openId = null },
            onDelete = { deleteTarget = current }
        )
    } else {
        val filtered = sheets.filter { s -> listOf(s.name, s.concept, s.origin).any { it.contains(query, ignoreCase = true) } }
        LazyColumn(
            modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listHeader("Fichas", "Personagens", "Cada ficha segue o sistema a que está vinculada",
                query, { query = it }, "Nova ficha") { choosingSystem = true }
            if (filtered.isEmpty()) item {
                if (query.isBlank()) EmptyState("ficha", "Nenhuma ficha ainda", "Toque em Nova ficha para criar seu primeiro personagem.") else EmptyHint("Nenhum resultado para essa busca.")
            }
            items(filtered, key = { it.id }) { sheet -> CharacterCard(sheet, prefs) { openId = sheet.id } }
        }
    }

    if (choosingSystem) {
        val campaignId = SheetStore.activeSystemId(prefs)
        SystemPickerDialog(
            title = "Sistema da nova ficha",
            options = systemOptions(prefs).sortedByDescending { it.id == campaignId },
            currentId = campaignId, campaignId = campaignId,
            onPick = { id ->
                choosingSystem = false
                namingSystemId = id
            },
            onDismiss = { choosingSystem = false }
        )
    }

    namingSystemId?.let { systemId ->
        NewCharacterDialog(
            profile = remember(systemId) { SystemProfiles.resolve(prefs, systemId) },
            onCreate = { name, concept, origin ->
                namingSystemId = null
                val sheet = SheetStore.newCharacter(prefs, systemId, name).copy(concept = concept, origin = origin)
                sheets.add(0, sheet); persist()
                newSheetId = sheet.id
                openId = sheet.id
            },
            onDismiss = { namingSystemId = null }
        )
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(target.name, "esta ficha", onConfirm = {
            deletePortraitFile(context, target.imageUri)
            sheets.removeAll { it.id == target.id }
            persist()
            if (openId == target.id) openId = null
            deleteTarget = null
        }, onDismiss = { deleteTarget = null })
    }
}

@Composable
private fun CharacterCard(sheet: CharacterSheet, prefs: SharedPreferences, onClick: () -> Unit) {
    val profile = remember(sheet.systemId) { SystemProfiles.resolve(prefs, sheet.systemId) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (sheet.imageUri.isNotBlank()) Portrait(sheet.imageUri, sheet.name)
            else if (sheet.look.configured) AvatarView(sheet.look, 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(sheet.name.ifBlank { "Sem nome" }, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val subtitle = listOf(sheet.concept, sheet.origin, if (profile.usesLevels) "Nível ${sheet.level}" else "")
                    .filter { it.isNotBlank() }.joinToString(" • ")
                if (subtitle.isNotBlank()) Text(subtitle, color = Accent, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoPill(profile.name)
                    profile.resources.firstOrNull()?.let { r ->
                        val v = sheet.resources[r.key] ?: ResourceValue(r.defaultMax, r.defaultMax)
                        InfoPill("${shortResourceLabel(r)} ${v.current}/${v.max}")
                    }
                    if (profile.usesArmorClass) InfoPill("CA ${remember(sheet.inventory, sheet.armorClass) { effectiveArmorClass(sheet, SheetStore.loadItems(prefs)) }}")
                }
            }
        }
    }
}

/** Primeiro passo da criação: nome (obrigatório) e, se quiser, classe/origem. Depois a ficha abre no Perfil, para o visual. */
@Composable
private fun NewCharacterDialog(
    profile: SystemProfile,
    onCreate: (name: String, concept: String, origin: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var concept by rememberSaveable { mutableStateOf("") }
    var origin by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo personagem") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sistema: ${profile.name}", color = Muted, fontSize = 13.sp)
                TextInput("Nome", name, { name = it }, Modifier.fillMaxWidth().focusRequester(focus))
                TextInput(profile.conceptLabel, concept, { concept = it })
                TextInput(profile.originLabel, origin, { origin = it })
                Text("Em seguida você escolhe a aparência e o retrato.", color = Muted, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name.trim(), concept.trim(), origin.trim()) }, enabled = name.isNotBlank()) { Text("Criar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterEditor(
    modifier: Modifier,
    sheet: CharacterSheet,
    prefs: SharedPreferences,
    startOnProfile: Boolean,
    onChange: (CharacterSheet) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val profile = remember(sheet.systemId) { SystemProfiles.resolve(prefs, sheet.systemId) }
    val engine = remember { RpgEngine() }
    val log = remember { mutableStateListOf<RollLine>() }
    val library = remember { SheetStore.loadItems(prefs) }
    var targetDefense by remember { mutableStateOf(0) }
    val variables = characterVariables(sheet, profile, library)
    val defense = targetDefense.takeIf { it > 0 }
    val armorClass = remember(sheet.inventory, sheet.armorClass) { effectiveArmorClass(sheet, library) }

    // Abas: o que se consulta jogando (Ficha, Combate, Itens) separado do que se monta uma vez (Perfil).
    val tabs = listOf("Ficha", "Combate") + (if (profile.usesInventory) listOf("Itens") else emptyList()) + listOf("Perfil")
    var savedTab by rememberSaveable(sheet.id) { mutableStateOf(initialSheetTab(startOnProfile, sheet.name)) }
    val tab = if (savedTab in tabs) savedTab else "Ficha"
    val listState = rememberLazyListState()
    // Ao trocar de aba, volta ao topo do conteúdo (mas não ao topo da página, para não reabrir o cabeçalho).
    LaunchedEffect(tab) { if (listState.firstVisibleItemIndex > 2) listState.scrollToItem(2) }
    // Quando o cabeçalho grande sai da tela, uma barra compacta com nome, PV e CA fica junto das abas.
    val showMini by remember { derivedStateOf { listState.firstVisibleItemIndex > 1 } }

    fun rollInitiativeToLog() {
        val result = rollInitiative(engine, profile, variables)
        log.push(RollLine("Iniciativa: ${describeRoll(result.roll)}" + if (result.note.isBlank()) "" else " • ${result.note}"))
    }

    Column(modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp),
            state = listState,
            contentPadding = PaddingValues(top = 0.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Column { Spacer(Modifier.height(8.dp)); EditorTopBar("Ficha • ${profile.name}", onBack, onDelete) } }
            item {
                SheetHeader(sheet, profile, armorClass, onOpenProfile = { savedTab = "Perfil" }, onChange = onChange, onInitiative = { rollInitiativeToLog() })
            }
            stickyHeader {
                Column(Modifier.fillMaxWidth().background(Night)) {
                    if (showMini) MiniHeader(sheet, profile, armorClass) { savedTab = "Perfil" }
                    TabRow(selectedTabIndex = tabs.indexOf(tab).coerceAtLeast(0), containerColor = Night, contentColor = Accent) {
                        tabs.forEach { name ->
                            Tab(
                                selected = name == tab, onClick = { savedTab = name },
                                text = { Text(name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                                unselectedContentColor = Muted
                            )
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                }
            }
            when (tab) {
                "Ficha" -> {
                    item { ResourcesSection(sheet, profile, onChange) }
                    item {
                        val shownAttrs = profile.visibleAttributes(sheet)
                        val attrSummary = if (shownAttrs.size == profile.attributes.size) "${profile.attributes.size} atributos"
                            else "${shownAttrs.size} de ${profile.attributes.size} atributos"
                        CollapsibleSection("ATRIBUTOS", summary = attrSummary) {
                            if (shownAttrs.isEmpty() && profile.attributes.isNotEmpty()) {
                                EmptyHint("Nenhum atributo visível nesta ficha. Use \"Escolher atributos\" para mostrar.")
                            } else {
                                AttributeGrid(
                                    profile.copy(attributes = shownAttrs), sheet.attributes,
                                    onRoll = { attr -> log.push(rollAttributeCheck(engine, profile, sheet.attributes, attr)) }
                                ) { key, value ->
                                    onChange(sheet.copy(attributes = sheet.attributes + (key to value)))
                                }
                            }
                            SheetAttributeChooser(sheet, profile, onChange)
                        }
                    }
                    if (profile.usesProficiency && !profile.isPool) item {
                        SavesSection(sheet, profile, onChange, onRoll = { attr -> log.push(rollSaveCheck(engine, sheet, profile, attr)) })
                    }
                    if (profile.usesSkills) item {
                        SkillsSection(sheet, profile, onChange, onRoll = { skill -> log.push(rollSkillCheck(engine, sheet, profile, skill)) })
                    }
                    if (profile.usesTraits) item { TraitsSection(sheet, profile, prefs, onChange) }
                }
                "Combate" -> {
                    item { RollPanel(sheet, profile, prefs, engine, variables, targetDefense, { targetDefense = it }, log) }
                    if (profile.usesAttacks) item {
                        AttacksSection(sheet, library, onChange, onRoll = { option ->
                            log.push(rollOption(engine, option, variables, profile, defense).line)
                        })
                    }
                    if (profile.usesSpells) item {
                        SpellsSection(sheet, prefs, onChange, onCast = { spell ->
                            val outcome = castSpell(engine, spell, sheet, profile)
                            log.push(outcome.line)
                            if (outcome.cast) onChange(outcome.sheet)
                        })
                    }
                    item {
                        DefensesSection(
                            defenses = sheet.defenses, conditionImmunities = sheet.conditionImmunities,
                            onDefensesChange = { onChange(sheet.copy(defenses = it)) },
                            onConditionsChange = { onChange(sheet.copy(conditionImmunities = it)) }
                        )
                    }
                }
                "Itens" -> {
                    item { InventorySection(sheet, prefs, onChange) }
                }
                else -> {
                    item { IdentitySection(sheet, profile, prefs, onChange) }
                    item {
                        EditorCard("PAPEL NA MESA", "Define a interface da arena: Jogador usa o deck de cartas, Mestre controla a cena") {
                            RoleSelector(sheet.role, { onChange(sheet.copy(role = it)) })
                        }
                    }
                    item { AppearanceSection(sheet, onChange) }
                    item { ProficienciesSection(sheet, onChange) }
                    item { BackstorySection(sheet, onChange) }
                    item {
                        CollapsibleSection("ANOTAÇÕES", summary = sheet.notes.lineSequence().firstOrNull().orEmpty(), startExpanded = false) {
                            TextInput("História, aliados, objetivos…", sheet.notes, { onChange(sheet.copy(notes = it)) }, singleLine = false, minLines = 5)
                        }
                    }
                }
            }
        }
        RollLogBar(log) { log.clear() }
    }
}

/** Avatar do personagem: retrato da galeria, se houver; senão o pixel art; senão um convite para criar o visual. */
@Composable
private fun HeaderAvatar(sheet: CharacterSheet, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        when {
            sheet.imageUri.isNotBlank() -> Portrait(sheet.imageUri, sheet.name, size)
            sheet.look.configured -> AvatarView(sheet.look, size)
            else -> Box(Modifier.fillMaxSize().background(Panel2), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Person, "Criar visual do personagem", tint = Muted, modifier = Modifier.size(size / 2))
            }
        }
    }
}

/** Cabeçalho da ficha: avatar grande, nome e o que mais se usa na mesa (PV, CA e iniciativa). */
@Composable
private fun SheetHeader(
    sheet: CharacterSheet,
    profile: SystemProfile,
    armorClass: Int,
    onOpenProfile: () -> Unit,
    onChange: (CharacterSheet) -> Unit,
    onInitiative: () -> Unit
) {
    val vital = profile.resources.firstOrNull()
    val value = vital?.let { sheet.resources[it.key] ?: ResourceValue(it.defaultMax, it.defaultMax) }
    fun setVital(next: ResourceValue) {
        if (vital != null) onChange(sheet.copy(resources = sheet.resources + (vital.key to next)))
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Column(
            Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Brand.copy(alpha = .16f), Panel))).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                HeaderAvatar(sheet, 88.dp, onOpenProfile)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(sheet.name.ifBlank { "Sem nome" }, color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                    val subtitle = listOf(sheet.concept, sheet.origin, if (profile.usesLevels) "Nível ${sheet.level}" else "")
                        .filter { it.isNotBlank() }.joinToString(" • ")
                    if (subtitle.isNotBlank()) Text(subtitle, color = Accent, fontSize = 13.sp)
                    if (sheet.conditions.isNotEmpty()) Text(sheet.conditions.joinToString(", "), color = Muted, fontSize = 13.sp, maxLines = 1)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (vital != null && value != null) {
                    Surface(color = Panel2, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(2.3f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                            IconButton(onClick = { setVital(value.copy(current = (value.current - 1).coerceAtLeast(0))) }, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Default.Remove, "Diminuir ${vital.label}", tint = Accent)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(shortResourceLabel(vital), color = Muted, fontSize = 11.sp)
                                Text("${value.current}/${value.max}", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                            IconButton(onClick = { setVital(value.copy(current = (value.current + 1).coerceAtMost(value.max))) }, modifier = Modifier.size(38.dp)) {
                                Icon(Icons.Default.Add, "Aumentar ${vital.label}", tint = Accent)
                            }
                        }
                    }
                }
                if (profile.usesArmorClass) {
                    Surface(color = Panel2, shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("CA", color = Muted, fontSize = 11.sp)
                            Text("$armorClass", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Surface(color = Brand.copy(alpha = .2f), shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1.2f).clickable(onClick = onInitiative)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Casino, "Rolar iniciativa", tint = Accent, modifier = Modifier.size(20.dp))
                        Text("Iniciativa", color = Ink, fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}

/** Versão compacta do cabeçalho, presa ao topo enquanto se rola a ficha. */
@Composable
private fun MiniHeader(sheet: CharacterSheet, profile: SystemProfile, armorClass: Int, onOpenProfile: () -> Unit) {
    val vital = profile.resources.firstOrNull()
    val value = vital?.let { sheet.resources[it.key] ?: ResourceValue(it.defaultMax, it.defaultMax) }
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        HeaderAvatar(sheet, 36.dp, onOpenProfile)
        Text(sheet.name.ifBlank { "Sem nome" }, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
        if (vital != null && value != null) InfoPill("${shortResourceLabel(vital)} ${value.current}/${value.max}")
        if (profile.usesArmorClass) InfoPill("CA $armorClass")
    }
}

/** Rolagens ligadas à ficha: iniciativa, teste por fórmula (ou reserva de dados) e envio ao combate. */
@Composable
private fun RollPanel(
    sheet: CharacterSheet,
    profile: SystemProfile,
    prefs: SharedPreferences,
    engine: RpgEngine,
    variables: Map<String, Int>,
    targetDefense: Int,
    onDefense: (Int) -> Unit,
    log: MutableList<RollLine>
) {
    var formula by remember(sheet.systemId) { mutableStateOf(defaultTestFormula(profile)) }
    var difficulty by remember(sheet.systemId) { mutableStateOf(if (profile.isPool) 1 else 15) }
    var poolDice by remember { mutableStateOf(3) }
    val names = listOfNotNull(
        if (profile.usesProficiency) "PROF" else null,
        if (profile.usesLevels) "NIVEL" else null,
        if (profile.usesArmorClass) "CA" else null
    ) + profile.attributes.map { attributeVariable(it) } + profile.resources.map { variableName(shortResourceLabel(it)) }

    CollapsibleSection("ROLAGENS", summary = "Iniciativa, testes e combate") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val result = rollInitiative(engine, profile, variables)
                log.push(RollLine("Iniciativa: ${describeRoll(result.roll)}" + if (result.note.isBlank()) "" else " • ${result.note}"))
            }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Casino, null); Spacer(Modifier.width(6.dp)); Text("Iniciativa") }
            OutlinedButton(onClick = {
                if (CombatStore.load(prefs).any { it.sheetId == sheet.id }) {
                    log.push(RollLine("${sheet.name.ifBlank { "Personagem" }} já está no combate.", null))
                } else {
                    val fighter = characterCombatant(engine, sheet, profile, SheetStore.loadItems(prefs))
                    CombatStore.add(prefs, fighter)
                    log.push(RollLine("${fighter.name} entrou no combate (iniciativa ${fighter.initiative}). Veja na aba Dados.", true))
                }
            }, modifier = Modifier.weight(1f)) { Text("Ao combate") }
        }
        if (profile.isPool) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField("Dados da reserva", poolDice, { poolDice = it.coerceIn(0, 100) }, Modifier.weight(1f))
                NumberField("Dificuldade", difficulty, { difficulty = it.coerceAtLeast(1) }, Modifier.weight(1f))
                Button(onClick = { log.push(rollPoolLine(engine, "Reserva", poolDice, profile, difficulty)) },
                    colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Rolar") }
            }
        } else {
            TextInput("Fórmula (ex.: 1d20 + FOR + PROF)", formula, { formula = it })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField("CD (0 = sem CD)", difficulty, { difficulty = it.coerceAtLeast(0) }, Modifier.weight(1f))
                Button(onClick = { log.push(rollFormula(engine, "Teste", formula, variables, difficulty.takeIf { it > 0 })) },
                    colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Rolar") }
            }
        }
        NumberField(
            if (profile.usesArmorClass) "CA do alvo nos ataques (0 = sem alvo)" else "Defesa do alvo nos ataques (0 = sem alvo)",
            targetDefense, { onDefense(it.coerceAtLeast(0)) }, Modifier.fillMaxWidth()
        )
        Text("Use na fórmula: ${names.joinToString(", ")} e o nome de cada perícia (ex.: ATLETISMO).", color = Muted, fontSize = 13.sp)
    }
}

@Composable
private fun IdentitySection(sheet: CharacterSheet, profile: SystemProfile, prefs: SharedPreferences, onChange: (CharacterSheet) -> Unit) {
    var switching by remember { mutableStateOf(false) }
    val armor = armorClassBreakdown(sheet, remember { SheetStore.loadItems(prefs) })
    CollapsibleSection("IDENTIDADE", summary = listOf(sheet.name, sheet.concept).filter { it.isNotBlank() }.joinToString(" • ")) {
        PortraitField(sheet.imageUri, sheet.name) { onChange(sheet.copy(imageUri = it)) }
        TextInput("Nome", sheet.name, { onChange(sheet.copy(name = it)) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sistema", color = Muted, fontSize = 13.sp)
                Text(systemDisplayName(prefs, sheet.systemId), color = Ink, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { switching = true }) { Text("Trocar sistema") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput(profile.conceptLabel, sheet.concept, { onChange(sheet.copy(concept = it)) }, Modifier.weight(1f))
            TextInput(profile.originLabel, sheet.origin, { onChange(sheet.copy(origin = it)) }, Modifier.weight(1f))
        }
        if (profile.usesLevels || profile.usesProficiency || profile.usesArmorClass) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (profile.usesLevels) {
                    NumberField("Nível", sheet.level, { level ->
                        val prof = if (profile.autoProficiency) 2 + (maxOf(level, 1) - 1) / 4 else sheet.proficiencyBonus
                        onChange(sheet.copy(level = level.coerceAtLeast(0), proficiencyBonus = prof))
                    }, Modifier.weight(1f))
                }
                if (profile.usesProficiency) {
                    NumberField("Prof.", sheet.proficiencyBonus, { onChange(sheet.copy(proficiencyBonus = it)) }, Modifier.weight(1f))
                }
                if (profile.usesArmorClass) {
                    NumberField(if (armor.modified) "CA base" else "CA", sheet.armorClass, { onChange(sheet.copy(armorClass = it)) }, Modifier.weight(1f))
                }
            }
            if (profile.usesArmorClass && armor.modified) {
                Text("CA efetiva ${armor.total} (${armor.text}) — vale nas rolagens e no combate.", color = Accent, fontSize = 12.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput("Desloc.", sheet.speed, { onChange(sheet.copy(speed = it)) }, Modifier.weight(1f))
            if (profile.usesLevels) {
                NumberField("XP", sheet.experience, { onChange(sheet.copy(experience = it.coerceAtLeast(0))) }, Modifier.weight(1f))
            }
        }
    }
    if (switching) {
        SystemPickerDialog(
            title = "Vincular a outro sistema",
            options = systemOptions(prefs), currentId = sheet.systemId,
            campaignId = SheetStore.activeSystemId(prefs),
            onPick = { id ->
                switching = false
                if (id != sheet.systemId) onChange(SheetStore.rebindCharacter(prefs, sheet, id))
            },
            onDismiss = { switching = false }
        )
    }
}

@Composable
private fun ResourcesSection(sheet: CharacterSheet, profile: SystemProfile, onChange: (CharacterSheet) -> Unit) {
    val summary = profile.resources.joinToString("  ") { r ->
        val v = sheet.resources[r.key] ?: ResourceValue(r.defaultMax, r.defaultMax)
        "${shortResourceLabel(r)} ${v.current}/${v.max}"
    }
    CollapsibleSection("RECURSOS", summary = listOf(summary, sheet.conditions.joinToString(", ")).filter { it.isNotBlank() }.joinToString(" • ")) {
        if (profile.resources.isEmpty()) EmptyHint("Nenhum recurso definido neste sistema.")
        profile.resources.forEach { r ->
            val v = sheet.resources[r.key] ?: ResourceValue(r.defaultMax, r.defaultMax)
            fun update(next: ResourceValue) = onChange(sheet.copy(resources = sheet.resources + (r.key to next)))
            Text(r.label, color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            LinearProgressIndicator(
                progress = { if (v.max > 0) (v.current.toFloat() / v.max).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(), color = Accent, trackColor = Panel2
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { update(v.copy(current = (v.current - 1).coerceAtLeast(0))) }) {
                    Icon(Icons.Default.Remove, "Diminuir ${r.label}", tint = Accent)
                }
                NumberField("Atual", v.current, { update(v.copy(current = it.coerceAtLeast(0))) }, Modifier.weight(1f))
                Text("/", color = Muted)
                NumberField("Máx.", v.max, { update(v.copy(max = it.coerceAtLeast(0))) }, Modifier.weight(1f))
                IconButton(onClick = { update(v.copy(current = (v.current + 1).coerceAtMost(v.max))) }) {
                    Icon(Icons.Default.Add, "Aumentar ${r.label}", tint = Accent)
                }
            }
        }
        VitalsExtras(sheet, profile, onChange)
    }
}

@Composable
private fun SkillsSection(
    sheet: CharacterSheet, profile: SystemProfile, onChange: (CharacterSheet) -> Unit,
    onRoll: (SkillEntry) -> Unit
) {
    // Cada sistema tem as suas perícias: as de outros sistemas ficam guardadas na ficha, mas fora da vista.
    val visible = sheet.skills.withIndex().filter { it.value.fitsSystem(sheet.systemId) }
    CollapsibleSection("PERÍCIAS", summary = "${visible.size} perícias de ${profile.name}", startExpanded = false) {
        if (visible.isEmpty()) EmptyHint("Nenhuma perícia neste sistema. Adicione abaixo ou use as do sistema.")
        visible.forEach { (index, skill) ->
            val attr = profile.attr(skill.attributeKey)
            val total = skillBonus(profile, sheet.attributes, sheet.proficiencyBonus, skill)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile.usesProficiency) {
                    Checkbox(
                        checked = skill.proficient,
                        onCheckedChange = { onChange(sheet.copy(skills = sheet.skills.replaced(index, skill.copy(proficient = it)))) },
                        colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Night)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(skill.name, color = Ink, fontSize = 14.sp)
                    if (attr != null || profile.isPool) {
                        val pool = if (profile.isPool) "reserva ${skillPoolSize(profile, sheet.attributes, skill)}" else ""
                        Text(listOfNotNull(attr?.abbr, pool.ifBlank { null }).joinToString(" • "), color = Muted, fontSize = 13.sp)
                    }
                }
                if (profile.usesProficiency) {
                    InfoPill(signed(total))
                } else {
                    NumberField("Valor", skill.extra, { onChange(sheet.copy(skills = sheet.skills.replaced(index, skill.copy(extra = it)))) },
                        Modifier.width(96.dp))
                }
                IconButton(onClick = { onRoll(skill) }) {
                    Icon(Icons.Default.Casino, "Rolar ${skill.name}", tint = Accent)
                }
                IconButton(onClick = { onChange(sheet.copy(skills = sheet.skills.without(index))) }) {
                    Icon(Icons.Default.DeleteOutline, "Excluir ${skill.name}", tint = Muted)
                }
            }
        }
        var newSkill by remember { mutableStateOf("") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextInput("Nova perícia", newSkill, { newSkill = it }, Modifier.weight(1f))
            IconButton(onClick = {
                val name = newSkill.trim()
                if (name.isNotEmpty()) { onChange(sheet.copy(skills = sheet.skills + SkillEntry(name, systemId = sheet.systemId))); newSkill = "" }
            }) { Icon(Icons.Default.Add, "Adicionar perícia", tint = Accent) }
        }
        val missing = profile.skillTemplates.filter { t -> visible.none { it.value.name == t.name } }
        if (missing.isNotEmpty()) {
            OutlinedButton(
                onClick = { onChange(sheet.copy(skills = sheet.skills + missing.map { SkillEntry(it.name, it.attributeKey, systemId = sheet.systemId) })) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Adicionar ${missing.size} perícias do sistema") }
        }
    }
}

@Composable
private fun TraitsSection(sheet: CharacterSheet, profile: SystemProfile, prefs: SharedPreferences, onChange: (CharacterSheet) -> Unit) {
    val library = remember { SheetStore.loadTraits(prefs) }
    var picking by remember { mutableStateOf(false) }
    val linked = sheet.traitIds.mapNotNull { id -> library.firstOrNull { it.id == id } }.filter { it.fitsSystem(sheet.systemId) }
    val visibleIds = sheet.traitIds.filter { id -> library.firstOrNull { it.id == id }?.fitsSystem(sheet.systemId) != false }
    val earned = linked.filter { !it.isAdvantage }.sumOf { it.cost }
    val spentOnAdvantages = linked.filter { it.isAdvantage }.sumOf { it.cost }
    val net = traitPointsSpent(linked)
    val budget = profile.traitBudget
    val summary = "${linked.size} traços • " +
        if (budget > 0) "${budget - net} de $budget pts livres" else "saldo ${-net} pts"

    CollapsibleSection("VANTAGENS E DESVANTAGENS", summary = summary, startExpanded = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            InfoPill("Gastos $spentOnAdvantages")
            InfoPill("Devolvidos $earned")
            if (budget > 0) InfoPill("Livres ${budget - net}")
        }
        if (budget > 0 && net > budget) {
            Text("Os pontos gastos passam do orçamento do sistema ($budget).", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        if (visibleIds.isEmpty()) EmptyHint("Nenhuma vantagem ou desvantagem deste sistema. Cadastre no Acervo e vincule aqui.")
        visibleIds.forEach { id ->
            val trait = library.firstOrNull { it.id == id }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(trait?.name?.ifBlank { "Sem nome" } ?: "Traço removido da biblioteca",
                        color = if (trait != null) Ink else Muted, fontSize = 14.sp)
                    if (trait != null) {
                        val kindLabel = if (trait.isAdvantage) "Vantagem" else "Desvantagem"
                        Text(listOf(kindLabel, traitCostLabel(trait), trait.category).filter { it.isNotBlank() }.joinToString(" • "),
                            color = if (trait.isAdvantage) Accent else Muted, fontSize = 12.sp)
                    }
                }
                IconButton(onClick = { onChange(sheet.copy(traitIds = sheet.traitIds - id)) }) {
                    Icon(Icons.Default.Close, "Remover traço", tint = Muted)
                }
            }
        }
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text("Vincular da biblioteca") }
    }

    if (picking) {
        val available = library.filter { it.systemId.isBlank() || it.systemId == sheet.systemId }
            .sortedWith(compareBy<TraitEntry>({ !it.isAdvantage }, { it.name.lowercase() }))
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Vantagens e desvantagens") },
            text = {
                if (available.isEmpty()) {
                    Text("Nenhum traço para este sistema. Cadastre em Acervo > Vantagens e desvantagens.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(available, key = { it.id }) { trait ->
                            val has = trait.id in sheet.traitIds
                            val toggle = {
                                onChange(sheet.copy(traitIds = if (has) sheet.traitIds - trait.id else sheet.traitIds + trait.id))
                            }
                            Row(Modifier.fillMaxWidth().clickable { toggle() }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = has, onCheckedChange = { toggle() },
                                    colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Night))
                                Column {
                                    Text(trait.name.ifBlank { "Sem nome" })
                                    Text(listOf(if (trait.isAdvantage) "Vantagem" else "Desvantagem", traitCostLabel(trait))
                                        .joinToString(" • "), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Concluir") } }
        )
    }
}

@Composable
private fun AttacksSection(
    sheet: CharacterSheet, library: List<ItemEntry>, onChange: (CharacterSheet) -> Unit,
    onRoll: (AttackOption) -> Unit
) {
    val fromItems = characterAttacks(sheet.copy(attacks = emptyList()), library)
    CollapsibleSection("ATAQUES", summary = "${sheet.attacks.size + fromItems.size} ataques", startExpanded = false) {
        if (sheet.attacks.isEmpty() && fromItems.isEmpty()) EmptyHint("Nenhum ataque cadastrado. Itens equipados com dano aparecem aqui sozinhos.")
        sheet.attacks.forEachIndexed { index, attack ->
            Surface(color = Panel2, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextInput("Nome", attack.name, { onChange(sheet.copy(attacks = sheet.attacks.replaced(index, attack.copy(name = it)))) },
                            Modifier.weight(1f))
                        IconButton(onClick = { onRoll(AttackOption(attack.name.ifBlank { "Ataque" }, attack.bonus, attack.damage)) }) {
                            Icon(Icons.Default.Casino, "Rolar ${attack.name}", tint = Accent)
                        }
                        IconButton(onClick = { onChange(sheet.copy(attacks = sheet.attacks.without(index))) }) {
                            Icon(Icons.Default.Close, "Remover ataque", tint = Muted)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextInput("Bônus", attack.bonus, { onChange(sheet.copy(attacks = sheet.attacks.replaced(index, attack.copy(bonus = it)))) },
                            Modifier.weight(1f))
                        TextInput("Dano", attack.damage, { onChange(sheet.copy(attacks = sheet.attacks.replaced(index, attack.copy(damage = it)))) },
                            Modifier.weight(1.4f))
                    }
                }
            }
        }
        fromItems.forEach { option ->
            Surface(color = Panel2, shape = RoundedCornerShape(14.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(option.label, color = Ink, fontSize = 14.sp)
                        Text(listOf(option.bonus, option.damage).filter { it.isNotBlank() }.joinToString(" • "), color = Muted, fontSize = 13.sp)
                    }
                    IconButton(onClick = { onRoll(option) }) { Icon(Icons.Default.Casino, "Rolar ${option.label}", tint = Accent) }
                }
            }
        }
        AddRowButton("Adicionar ataque") { onChange(sheet.copy(attacks = sheet.attacks + AttackEntry())) }
    }
}

@Composable
private fun SpellsSection(
    sheet: CharacterSheet, prefs: SharedPreferences, onChange: (CharacterSheet) -> Unit,
    onCast: (SpellEntry) -> Unit
) {
    val everySpell = remember { SheetStore.loadSpells(prefs) }
    // O seletor só oferece magias deste sistema (ou sem sistema); as já vinculadas continuam aparecendo.
    val allSpells = everySpell.filter { it.systemId.isBlank() || it.systemId == sheet.systemId }
    val linked = sheet.spellIds.mapNotNull { id -> everySpell.firstOrNull { it.id == id } }
        .filter { it.fitsSystem(sheet.systemId) }
        .sortedWith(compareBy<SpellEntry>({ it.level }, { it.name.lowercase() }))
    var picking by remember { mutableStateOf(false) }

    CollapsibleSection("MAGIAS", summary = "${linked.size} magias vinculadas", startExpanded = false) {
        if (linked.isEmpty()) EmptyHint("Nenhuma magia vinculada. Cadastre magias no Grimório e vincule aqui.")
        else EmptyHint("Conjurar rola o efeito com os valores da ficha e debita o custo quando ele aponta para um recurso (ex.: \"3 Mana\").")
        linked.forEach { spell ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(spell.name, color = Ink, fontSize = 14.sp)
                    Text(listOf(spellLevelLabel(spell.level), spell.school, spell.damage, spell.cost.takeIf { it.isNotBlank() }?.let { "custo $it" })
                        .filter { !it.isNullOrBlank() }.joinToString(" • "), color = Muted, fontSize = 13.sp)
                }
                TextButton(onClick = { onCast(spell) }) { Icon(Icons.Default.Casino, null); Spacer(Modifier.width(4.dp)); Text("Conjurar") }
                IconButton(onClick = { onChange(sheet.copy(spellIds = sheet.spellIds - spell.id)) }) {
                    Icon(Icons.Default.Close, "Desvincular ${spell.name}", tint = Muted)
                }
            }
        }
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text("Vincular magias do Grimório") }
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Magias da ficha") },
            text = {
                if (allSpells.isEmpty()) {
                    Text("O Grimório está vazio. Cadastre magias primeiro.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(allSpells.sortedWith(compareBy<SpellEntry>({ it.level }, { it.name.lowercase() })), key = { it.id }) { spell ->
                            val has = spell.id in sheet.spellIds
                            val toggle = {
                                onChange(sheet.copy(spellIds = if (has) sheet.spellIds - spell.id else sheet.spellIds + spell.id))
                            }
                            Row(Modifier.fillMaxWidth().clickable { toggle() }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = has, onCheckedChange = { toggle() },
                                    colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Night))
                                Column {
                                    Text(spell.name)
                                    Text(listOf(spellLevelLabel(spell.level), spell.school).filter { it.isNotBlank() }.joinToString(" • "), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Concluir") } }
        )
    }
}

@Composable
private fun InventorySection(sheet: CharacterSheet, prefs: SharedPreferences, onChange: (CharacterSheet) -> Unit) {
    val library = remember { SheetStore.loadItems(prefs) }
    var picking by remember { mutableStateOf(false) }
    // Itens de outros sistemas ficam guardados na ficha, mas fora da vista, do peso e dos efeitos.
    val visible = sheet.inventory.withIndex().filter { library.firstOrNull { item -> item.id == it.value.itemId }?.fitsSystem(sheet.systemId) != false }
    val totalWeight = inventoryWeight(visible.map { it.value }, library)
    val equippedCount = visible.count { it.value.equipped }
    val summary = "${visible.size} itens • ${formatWeight(totalWeight)} kg" +
        if (equippedCount > 0) " • $equippedCount equipados" else ""

    CollapsibleSection("INVENTÁRIO", summary = summary, startExpanded = false) {
        if (visible.isEmpty()) EmptyHint("Nenhum item deste sistema. Adicione itens da biblioteca.")
        visible.forEach { (index, entry) ->
            val item = library.firstOrNull { it.id == entry.itemId }
            fun update(next: InventoryEntry) = onChange(sheet.copy(inventory = sheet.inventory.replaced(index, next)))
            Surface(color = Panel2, shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = entry.equipped, onCheckedChange = { update(entry.copy(equipped = it)) },
                            enabled = item != null,
                            colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Night)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item?.name?.ifBlank { "Sem nome" } ?: "Item removido da biblioteca",
                                color = if (item != null) Ink else Muted, fontSize = 14.sp)
                            val detail = if (item == null) "" else listOf(item.type, item.bonus, item.damage)
                                .filter { it.isNotBlank() }.joinToString(" • ")
                            if (detail.isNotBlank()) Text(detail, color = Muted, fontSize = 13.sp)
                        }
                        IconButton(onClick = { onChange(sheet.copy(inventory = sheet.inventory.without(index))) }) {
                            Icon(Icons.Default.Close, "Remover do inventário", tint = Muted)
                        }
                    }
                    Row(Modifier.padding(start = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { update(entry.copy(quantity = (entry.quantity - 1).coerceAtLeast(1))) }) {
                            Icon(Icons.Default.Remove, "Diminuir quantidade", tint = Accent)
                        }
                        Text("× ${entry.quantity}", color = Ink, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { update(entry.copy(quantity = entry.quantity + 1)) }) {
                            Icon(Icons.Default.Add, "Aumentar quantidade", tint = Accent)
                        }
                        Spacer(Modifier.weight(1f))
                        if (item != null && item.weight > 0.0) {
                            Text("${formatWeight(item.weight * entry.quantity)} kg", color = Muted, fontSize = 13.sp,
                                modifier = Modifier.padding(end = 10.dp))
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text("Adicionar item da biblioteca") }
        TextInput("Dinheiro (ex.: 12 po, 5 pp)", sheet.money, { onChange(sheet.copy(money = it)) })
        TextInput("Itens avulsos e anotações", sheet.equipment, { onChange(sheet.copy(equipment = it)) },
            singleLine = false, minLines = 3)
    }

    if (picking) {
        AlertDialog(
            onDismissRequest = { picking = false },
            title = { Text("Itens da biblioteca") },
            text = {
                if (library.isEmpty()) {
                    Text("A biblioteca está vazia. Cadastre itens na aba Itens primeiro.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(library.filter { it.fitsSystem(sheet.systemId) }.sortedWith(compareBy<ItemEntry>({ it.type }, { it.name.lowercase() })), key = { it.id }) { item ->
                            val has = sheet.inventory.any { it.itemId == item.id }
                            val toggle = {
                                onChange(sheet.copy(inventory = if (has) sheet.inventory.filterNot { it.itemId == item.id }
                                    else sheet.inventory + InventoryEntry(item.id)))
                            }
                            Row(Modifier.fillMaxWidth().clickable { toggle() }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = has, onCheckedChange = { toggle() },
                                    colors = CheckboxDefaults.colors(checkedColor = Accent, checkmarkColor = Night))
                                Column {
                                    Text(item.name.ifBlank { "Sem nome" })
                                    Text(listOf(item.type, if (item.weight > 0.0) "${formatWeight(item.weight)} kg" else "")
                                        .filter { it.isNotBlank() }.joinToString(" • "), fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Concluir") } }
        )
    }
}
