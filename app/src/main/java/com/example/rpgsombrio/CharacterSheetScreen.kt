package com.example.rpgsombrio

import com.example.rpgsombrio.pixelstudio.PixelStudioScreen

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
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
import com.example.rpgsombrio.data.InventoryEntry
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.ResourceValue
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SkillEntry
import com.example.rpgsombrio.data.SpellEntry
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.data.TraitEntry
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.data.systemOptions
import com.example.rpgsombrio.data.traitCostLabel
import com.example.rpgsombrio.data.traitPointsSpent
import com.example.rpgsombrio.data.formatWeight
import com.example.rpgsombrio.data.inventoryWeight
import com.example.rpgsombrio.data.shortResourceLabel
import com.example.rpgsombrio.data.spellLevelLabel

@Composable
internal fun CharacterSheetsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val sheets = remember { mutableStateListOf<CharacterSheet>().apply { addAll(SheetStore.loadCharacters(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<CharacterSheet?>(null) }
    var choosingSystem by remember { mutableStateOf(false) }
    var showPixelStudio by rememberSaveable { mutableStateOf(false) }
    fun persist() = SheetStore.saveCharacters(prefs, sheets.toList())

    val current = sheets.firstOrNull { it.id == openId }
    BackHandler(enabled = showPixelStudio) { showPixelStudio = false }
    BackHandler(enabled = current != null && !showPixelStudio) { openId = null }

    if (showPixelStudio) {
        PixelStudioScreen(modifier = modifier, onBack = { showPixelStudio = false })
    } else if (current != null) {
        CharacterEditor(
            modifier, current, prefs,
            onChange = { updated ->
                val i = sheets.indexOfFirst { it.id == updated.id }
                if (i >= 0) { sheets[i] = updated; persist() }
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
            item {
                androidx.compose.material3.Card(
                    modifier = Modifier.fillMaxWidth().clickable { showPixelStudio = true },
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Panel2),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Gold.copy(alpha = .55f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🎨", fontSize = 28.sp)
                        Column(Modifier.weight(1f)) {
                            Text("Pixel Art Studio", color = Gold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("Crie e edite personagens em pixel art 64×64", color = Parchment, fontSize = 12.sp)
                        }
                        Text("ABRIR  ›", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            listHeader("Fichas", "Personagens", "Cada ficha segue o sistema a que está vinculada",
                query, { query = it }, "Nova ficha") { choosingSystem = true }
            if (filtered.isEmpty()) item {
                EmptyHint(if (query.isBlank()) "Nenhuma ficha ainda. Toque em Nova ficha." else "Nenhum resultado para essa busca.")
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
                val sheet = SheetStore.newCharacter(prefs, id)
                sheets.add(0, sheet); persist(); openId = sheet.id
            },
            onDismiss = { choosingSystem = false }
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
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Gold.copy(alpha = .22f))
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (sheet.imageUri.isNotBlank()) Portrait(sheet.imageUri, sheet.name)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(sheet.name.ifBlank { "Sem nome" }, color = Parchment, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                val subtitle = listOf(sheet.concept, sheet.origin, if (profile.usesLevels) "Nível ${sheet.level}" else "")
                    .filter { it.isNotBlank() }.joinToString(" • ")
                if (subtitle.isNotBlank()) Text(subtitle, color = Gold, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoPill(profile.name)
                    profile.resources.firstOrNull()?.let { r ->
                        val v = sheet.resources[r.key] ?: ResourceValue(r.defaultMax, r.defaultMax)
                        InfoPill("${shortResourceLabel(r)} ${v.current}/${v.max}")
                    }
                    if (profile.usesArmorClass) InfoPill("CA ${sheet.armorClass}")
                }
            }
        }
    }
}

@Composable
private fun CharacterEditor(
    modifier: Modifier,
    sheet: CharacterSheet,
    prefs: SharedPreferences,
    onChange: (CharacterSheet) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val profile = remember(sheet.systemId) { SystemProfiles.resolve(prefs, sheet.systemId) }
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { EditorTopBar("Ficha • ${profile.name}", onBack, onDelete) }
        item { IdentitySection(sheet, profile, prefs, onChange) }
        item { ResourcesSection(sheet, profile, onChange) }
        item {
            CollapsibleSection("ATRIBUTOS", summary = "${profile.attributes.size} atributos") {
                AttributeGrid(profile, sheet.attributes) { key, value ->
                    onChange(sheet.copy(attributes = sheet.attributes + (key to value)))
                }
            }
        }
        if (profile.usesSkills) item { SkillsSection(sheet, profile, onChange) }
        if (profile.usesTraits) item { TraitsSection(sheet, profile, prefs, onChange) }
        if (profile.usesAttacks) item { AttacksSection(sheet, onChange) }
        if (profile.usesSpells) item { SpellsSection(sheet, prefs, onChange) }
        if (profile.usesInventory) item { InventorySection(sheet, prefs, onChange) }
        item {
            CollapsibleSection("ANOTAÇÕES", summary = sheet.notes.lineSequence().firstOrNull().orEmpty(), startExpanded = false) {
                TextInput("História, aliados, objetivos…", sheet.notes, { onChange(sheet.copy(notes = it)) }, singleLine = false, minLines = 5)
            }
        }
    }
}

@Composable
private fun IdentitySection(sheet: CharacterSheet, profile: SystemProfile, prefs: SharedPreferences, onChange: (CharacterSheet) -> Unit) {
    var switching by remember { mutableStateOf(false) }
    CollapsibleSection("IDENTIDADE", summary = listOf(sheet.name, sheet.concept).filter { it.isNotBlank() }.joinToString(" • ")) {
        PortraitField(sheet.imageUri, sheet.name) { onChange(sheet.copy(imageUri = it)) }
        TextInput("Nome", sheet.name, { onChange(sheet.copy(name = it)) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Sistema", color = Muted, fontSize = 11.sp)
                Text(systemDisplayName(prefs, sheet.systemId), color = Parchment, fontWeight = FontWeight.SemiBold)
            }
            TextButton(onClick = { switching = true }) { Text("Trocar sistema") }
        }
        TextInput(profile.conceptLabel, sheet.concept, { onChange(sheet.copy(concept = it)) })
        TextInput(profile.originLabel, sheet.origin, { onChange(sheet.copy(origin = it)) })
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
                    NumberField("CA", sheet.armorClass, { onChange(sheet.copy(armorClass = it)) }, Modifier.weight(1f))
                }
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
    CollapsibleSection("RECURSOS", summary = summary) {
        if (profile.resources.isEmpty()) EmptyHint("Nenhum recurso definido neste sistema.")
        profile.resources.forEach { r ->
            val v = sheet.resources[r.key] ?: ResourceValue(r.defaultMax, r.defaultMax)
            fun update(next: ResourceValue) = onChange(sheet.copy(resources = sheet.resources + (r.key to next)))
            Text(r.label, color = Parchment, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            LinearProgressIndicator(
                progress = { if (v.max > 0) (v.current.toFloat() / v.max).coerceIn(0f, 1f) else 0f },
                modifier = Modifier.fillMaxWidth(), color = Gold, trackColor = Panel2
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { update(v.copy(current = (v.current - 1).coerceAtLeast(0))) }) {
                    Icon(Icons.Default.Remove, "Diminuir ${r.label}", tint = Gold)
                }
                NumberField("Atual", v.current, { update(v.copy(current = it.coerceAtLeast(0))) }, Modifier.weight(1f))
                Text("/", color = Muted)
                NumberField("Máx.", v.max, { update(v.copy(max = it.coerceAtLeast(0))) }, Modifier.weight(1f))
                IconButton(onClick = { update(v.copy(current = (v.current + 1).coerceAtMost(v.max))) }) {
                    Icon(Icons.Default.Add, "Aumentar ${r.label}", tint = Gold)
                }
            }
        }
    }
}

@Composable
private fun SkillsSection(sheet: CharacterSheet, profile: SystemProfile, onChange: (CharacterSheet) -> Unit) {
    CollapsibleSection("PERÍCIAS", summary = "${sheet.skills.size} perícias", startExpanded = false) {
        if (sheet.skills.isEmpty()) EmptyHint("Nenhuma perícia. Adicione abaixo.")
        sheet.skills.forEachIndexed { index, skill ->
            val attr = profile.attr(skill.attributeKey)
            val total = if (profile.usesProficiency) {
                val mod = if (attr != null && attr.hasModifier) attr.modifierOf(sheet.attributes[attr.key] ?: attr.default) else 0
                mod + (if (skill.proficient) sheet.proficiencyBonus else 0) + skill.extra
            } else skill.extra
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (profile.usesProficiency) {
                    Checkbox(
                        checked = skill.proficient,
                        onCheckedChange = { onChange(sheet.copy(skills = sheet.skills.replaced(index, skill.copy(proficient = it)))) },
                        colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Night)
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(skill.name, color = Parchment, fontSize = 14.sp)
                    if (attr != null) Text(attr.abbr, color = Muted, fontSize = 11.sp)
                }
                if (profile.usesProficiency) {
                    InfoPill(signed(total))
                } else {
                    NumberField("Valor", skill.extra, { onChange(sheet.copy(skills = sheet.skills.replaced(index, skill.copy(extra = it)))) },
                        Modifier.width(96.dp))
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
                if (name.isNotEmpty()) { onChange(sheet.copy(skills = sheet.skills + SkillEntry(name))); newSkill = "" }
            }) { Icon(Icons.Default.Add, "Adicionar perícia", tint = Gold) }
        }
        val missing = profile.skillTemplates.filter { t -> sheet.skills.none { it.name == t.name } }
        if (missing.isNotEmpty()) {
            OutlinedButton(
                onClick = { onChange(sheet.copy(skills = sheet.skills + missing.map { SkillEntry(it.name, it.attributeKey) })) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Adicionar ${missing.size} perícias do sistema") }
        }
    }
}

@Composable
private fun TraitsSection(sheet: CharacterSheet, profile: SystemProfile, prefs: SharedPreferences, onChange: (CharacterSheet) -> Unit) {
    val library = remember { SheetStore.loadTraits(prefs) }
    var picking by remember { mutableStateOf(false) }
    val linked = sheet.traitIds.mapNotNull { id -> library.firstOrNull { it.id == id } }
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
            Text("Os pontos gastos passam do orçamento do sistema ($budget).", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
        }
        if (sheet.traitIds.isEmpty()) EmptyHint("Nenhuma vantagem ou desvantagem. Cadastre na Biblioteca e vincule aqui.")
        sheet.traitIds.forEach { id ->
            val trait = library.firstOrNull { it.id == id }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(trait?.name?.ifBlank { "Sem nome" } ?: "Traço removido da biblioteca",
                        color = if (trait != null) Parchment else Muted, fontSize = 14.sp)
                    if (trait != null) {
                        val kindLabel = if (trait.isAdvantage) "Vantagem" else "Desvantagem"
                        Text(listOf(kindLabel, traitCostLabel(trait), trait.category).filter { it.isNotBlank() }.joinToString(" • "),
                            color = if (trait.isAdvantage) Gold else Muted, fontSize = 11.sp)
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
                    Text("Nenhum traço para este sistema. Cadastre em Biblioteca > Vantagens e desvantagens.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(available, key = { it.id }) { trait ->
                            val has = trait.id in sheet.traitIds
                            val toggle = {
                                onChange(sheet.copy(traitIds = if (has) sheet.traitIds - trait.id else sheet.traitIds + trait.id))
                            }
                            Row(Modifier.fillMaxWidth().clickable { toggle() }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = has, onCheckedChange = { toggle() },
                                    colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Night))
                                Column {
                                    Text(trait.name.ifBlank { "Sem nome" })
                                    Text(listOf(if (trait.isAdvantage) "Vantagem" else "Desvantagem", traitCostLabel(trait))
                                        .joinToString(" • "), fontSize = 11.sp)
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
private fun AttacksSection(sheet: CharacterSheet, onChange: (CharacterSheet) -> Unit) {
    CollapsibleSection("ATAQUES", summary = "${sheet.attacks.size} ataques", startExpanded = false) {
        if (sheet.attacks.isEmpty()) EmptyHint("Nenhum ataque cadastrado.")
        sheet.attacks.forEachIndexed { index, attack ->
            Surface(color = Panel2, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextInput("Nome", attack.name, { onChange(sheet.copy(attacks = sheet.attacks.replaced(index, attack.copy(name = it)))) },
                            Modifier.weight(1f))
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
        AddRowButton("Adicionar ataque") { onChange(sheet.copy(attacks = sheet.attacks + AttackEntry())) }
    }
}

@Composable
private fun SpellsSection(sheet: CharacterSheet, prefs: SharedPreferences, onChange: (CharacterSheet) -> Unit) {
    val allSpells = remember { SheetStore.loadSpells(prefs) }
    val linked = sheet.spellIds.mapNotNull { id -> allSpells.firstOrNull { it.id == id } }
        .sortedWith(compareBy<SpellEntry>({ it.level }, { it.name.lowercase() }))
    var picking by remember { mutableStateOf(false) }

    CollapsibleSection("MAGIAS", summary = "${linked.size} magias vinculadas", startExpanded = false) {
        if (linked.isEmpty()) EmptyHint("Nenhuma magia vinculada. Cadastre magias no Grimório e vincule aqui.")
        linked.forEach { spell ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(spell.name, color = Parchment, fontSize = 14.sp)
                    Text(listOf(spellLevelLabel(spell.level), spell.school).filter { it.isNotBlank() }.joinToString(" • "),
                        color = Muted, fontSize = 11.sp)
                }
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
                                    colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Night))
                                Column {
                                    Text(spell.name)
                                    Text(listOf(spellLevelLabel(spell.level), spell.school).filter { it.isNotBlank() }.joinToString(" • "), fontSize = 11.sp)
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
    val totalWeight = inventoryWeight(sheet.inventory, library)
    val equippedCount = sheet.inventory.count { it.equipped }
    val summary = "${sheet.inventory.size} itens • ${formatWeight(totalWeight)} kg" +
        if (equippedCount > 0) " • $equippedCount equipados" else ""

    CollapsibleSection("INVENTÁRIO", summary = summary, startExpanded = false) {
        if (sheet.inventory.isEmpty()) EmptyHint("Nenhum item. Adicione itens da biblioteca.")
        sheet.inventory.forEachIndexed { index, entry ->
            val item = library.firstOrNull { it.id == entry.itemId }
            fun update(next: InventoryEntry) = onChange(sheet.copy(inventory = sheet.inventory.replaced(index, next)))
            Surface(color = Panel2, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = entry.equipped, onCheckedChange = { update(entry.copy(equipped = it)) },
                            enabled = item != null,
                            colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Night)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(item?.name?.ifBlank { "Sem nome" } ?: "Item removido da biblioteca",
                                color = if (item != null) Parchment else Muted, fontSize = 14.sp)
                            val detail = if (item == null) "" else listOf(item.type, item.bonus, item.damage)
                                .filter { it.isNotBlank() }.joinToString(" • ")
                            if (detail.isNotBlank()) Text(detail, color = Muted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { onChange(sheet.copy(inventory = sheet.inventory.without(index))) }) {
                            Icon(Icons.Default.Close, "Remover do inventário", tint = Muted)
                        }
                    }
                    Row(Modifier.padding(start = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { update(entry.copy(quantity = (entry.quantity - 1).coerceAtLeast(1))) }) {
                            Icon(Icons.Default.Remove, "Diminuir quantidade", tint = Gold)
                        }
                        Text("× ${entry.quantity}", color = Parchment, fontWeight = FontWeight.SemiBold)
                        IconButton(onClick = { update(entry.copy(quantity = entry.quantity + 1)) }) {
                            Icon(Icons.Default.Add, "Aumentar quantidade", tint = Gold)
                        }
                        Spacer(Modifier.weight(1f))
                        if (item != null && item.weight > 0.0) {
                            Text("${formatWeight(item.weight * entry.quantity)} kg", color = Muted, fontSize = 11.sp,
                                modifier = Modifier.padding(end = 10.dp))
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text("Adicionar item da biblioteca") }
        TextInput("Itens avulsos, moedas e anotações", sheet.equipment, { onChange(sheet.copy(equipment = it)) },
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
                        items(library.sortedWith(compareBy<ItemEntry>({ it.type }, { it.name.lowercase() })), key = { it.id }) { item ->
                            val has = sheet.inventory.any { it.itemId == item.id }
                            val toggle = {
                                onChange(sheet.copy(inventory = if (has) sheet.inventory.filterNot { it.itemId == item.id }
                                    else sheet.inventory + InventoryEntry(item.id)))
                            }
                            Row(Modifier.fillMaxWidth().clickable { toggle() }, verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = has, onCheckedChange = { toggle() },
                                    colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = Night))
                                Column {
                                    Text(item.name.ifBlank { "Sem nome" })
                                    Text(listOf(item.type, if (item.weight > 0.0) "${formatWeight(item.weight)} kg" else "")
                                        .filter { it.isNotBlank() }.joinToString(" • "), fontSize = 11.sp)
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
