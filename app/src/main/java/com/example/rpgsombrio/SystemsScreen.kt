package com.example.rpgsombrio

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import com.example.rpgsombrio.data.ALL_DICE_SIDES
import com.example.rpgsombrio.data.ROLL_POOL
import com.example.rpgsombrio.data.ROLL_SUM
import com.example.rpgsombrio.data.RuleSystemCatalog
import com.example.rpgsombrio.data.RuleSystemKind
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.UserAttribute
import com.example.rpgsombrio.data.UserResource
import com.example.rpgsombrio.data.UserSkill
import com.example.rpgsombrio.data.UserSystem
import com.example.rpgsombrio.data.UserSystemStore

/** Aba "Sistemas": biblioteca de sistemas do usuário + modelos de fábrica para duplicar. */
@Composable
internal fun SystemsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val systems = remember { mutableStateListOf<UserSystem>().apply { addAll(UserSystemStore.load(prefs)) } }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<UserSystem?>(null) }
    var blockedUse by remember { mutableStateOf<Pair<UserSystem, Int>?>(null) }
    var activeId by remember { mutableStateOf(SheetStore.activeSystemId(prefs)) }
    val usage = remember {
        (SheetStore.loadCharacters(prefs).map { it.systemId } + SheetStore.loadMonsters(prefs).map { it.systemId })
            .groupingBy { it }.eachCount()
    }
    fun persist() = UserSystemStore.save(prefs, systems.toList())

    val current = systems.firstOrNull { it.id == openId }
    BackHandler(enabled = current != null) { openId = null }

    if (current != null) {
        SystemEditor(
            modifier, current,
            onChange = { updated ->
                val i = systems.indexOfFirst { it.id == updated.id }
                if (i >= 0) { systems[i] = updated; persist() }
            },
            onBack = { openId = null },
            onDelete = {
                val used = usage[current.id] ?: 0
                if (used > 0) blockedUse = current to used else deleteTarget = current
            }
        )
    } else {
        LazyColumn(
            modifier.fillMaxSize().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ScreenHeader("Ferramentas do mestre", "Sistemas", "Crie sistemas próprios ou parta de um modelo pronto. Cada ficha e cada campanha escolhe o seu.") }
            item {
                Button(
                    onClick = { val s = UserSystemStore.blank(); systems.add(0, s); persist(); openId = s.id },
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)
                ) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Novo sistema em branco") }
            }
            if (systems.isNotEmpty()) item { Text("MEUS SISTEMAS", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
            items(systems, key = { it.id }) { system ->
                SystemCard(
                    title = system.name, description = system.description,
                    structure = "${system.attributes.size} atributos • ${system.resources.size} recursos • ${system.skills.size} perícias",
                    isActive = system.id == activeId, usedBy = usage[system.id] ?: 0,
                    onOpen = { openId = system.id }
                ) {
                    if (system.id != activeId) TextButton(onClick = {
                        SheetStore.setActiveSystem(prefs, system.id); activeId = system.id
                    }) { Text("Usar na campanha") }
                    TextButton(onClick = {
                        val copy = UserSystemStore.duplicateOf(prefs, system.id)
                        systems.add(0, copy); persist()
                    }) { Text("Duplicar") }
                }
            }
            item { Text("MODELOS PRONTOS", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp) }
            items(
                RuleSystemCatalog.systems.filter { it.kind != RuleSystemKind.CUSTOM && it.kind != RuleSystemKind.GENERIC },
                key = { it.id }
            ) { def ->
                SystemCard(
                    title = def.name, description = def.description,
                    structure = "${def.attributes.size} atributos • ${def.resources.size} recursos • modelo de fábrica",
                    isActive = def.id == activeId, usedBy = usage[def.id] ?: 0, onOpen = null
                ) {
                    if (def.id != activeId) TextButton(onClick = {
                        SheetStore.setActiveSystem(prefs, def.id); activeId = def.id
                    }) { Text("Usar na campanha") }
                    TextButton(onClick = {
                        val copy = UserSystemStore.duplicateOf(prefs, def.id)
                        systems.add(0, copy); persist(); openId = copy.id
                    }) { Text("Duplicar e editar") }
                }
            }
        }
    }

    deleteTarget?.let { target ->
        ConfirmDeleteDialog(target.name, "este sistema", onConfirm = {
            systems.removeAll { it.id == target.id }
            persist()
            if (openId == target.id) openId = null
            deleteTarget = null
        }, onDismiss = { deleteTarget = null })
    }
    blockedUse?.let { (system, count) ->
        AlertDialog(
            onDismissRequest = { blockedUse = null },
            title = { Text("Sistema em uso") },
            text = { Text("${system.name} está vinculado a $count ficha(s) ou criatura(s). Troque o sistema delas antes de excluir.") },
            confirmButton = { TextButton(onClick = { blockedUse = null }) { Text("Entendi") } }
        )
    }
}

@Composable
private fun SystemCard(
    title: String, description: String, structure: String,
    isActive: Boolean, usedBy: Int, onOpen: (() -> Unit)?,
    actions: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().let { if (onOpen != null) it.clickable(onClick = onOpen) else it },
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isActive) Accent.copy(alpha = .7f) else Hairline)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title.ifBlank { "Sem nome" }, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (description.isNotBlank()) Text(description, color = Muted, fontSize = 12.sp)
            Text(structure, color = Accent, fontSize = 12.sp)
            if (isActive || usedBy > 0) Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isActive) InfoPill("Sistema da campanha")
                if (usedBy > 0) InfoPill("$usedBy fichas")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { actions() }
        }
    }
}

@Composable
private fun AttributePicker(attributes: List<UserAttribute>, selectedKey: String, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val label = attributes.firstOrNull { it.key == selectedKey }?.abbr?.ifBlank { "?" } ?: "Sem atributo"
    Box {
        OutlinedButton(onClick = { open = true }) { Text(label, maxLines = 1) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Sem atributo") }, onClick = { onSelect(""); open = false })
            attributes.forEach { a ->
                DropdownMenuItem(text = { Text("${a.abbr} — ${a.label}") }, onClick = { onSelect(a.key); open = false })
            }
        }
    }
}

@Composable
private fun SystemEditor(
    modifier: Modifier,
    system: UserSystem,
    onChange: (UserSystem) -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { EditorTopBar("Sistema • ${system.name.ifBlank { "sem nome" }}", onBack, onDelete) }

        item {
            CollapsibleSection("BÁSICO", summary = system.name) {
                TextInput("Nome do sistema", system.name, { onChange(system.copy(name = it)) })
                TextInput("Descrição", system.description, { onChange(system.copy(description = it)) }, singleLine = false, minLines = 2)
                TextInput("Rótulo do conceito (Classe, Clã, Arquétipo…)", system.conceptLabel, { onChange(system.copy(conceptLabel = it)) })
                TextInput("Rótulo da origem (Raça, Linhagem…)", system.originLabel, { onChange(system.copy(originLabel = it)) })
            }
        }

        item {
            CollapsibleSection("BLOCOS DA FICHA", summary = "Escolha o que as fichas deste sistema mostram", startExpanded = false) {
                ToggleRow("Nível e proficiência", system.usesLevels) { onChange(system.copy(usesLevels = it)) }
                ToggleRow("Classe de armadura (CA)", system.usesArmorClass) { onChange(system.copy(usesArmorClass = it)) }
                ToggleRow("Perícias", system.usesSkills) { onChange(system.copy(usesSkills = it)) }
                ToggleRow("Ataques", system.usesAttacks) { onChange(system.copy(usesAttacks = it)) }
                ToggleRow("Magias (Grimório)", system.usesSpells) { onChange(system.copy(usesSpells = it)) }
                ToggleRow("Inventário", system.usesInventory) { onChange(system.copy(usesInventory = it)) }
                ToggleRow("Vantagens e desvantagens", system.usesTraits) { onChange(system.copy(usesTraits = it)) }
                if (system.usesTraits) {
                    NumberField("Orçamento de pontos (0 = sem limite)", system.traitBudget,
                        { onChange(system.copy(traitBudget = it.coerceAtLeast(0))) })
                    Text("Vantagens gastam pontos; desvantagens devolvem. A ficha mostra quantos pontos ainda estão livres.",
                        color = Muted, fontSize = 12.sp)
                }
            }
        }

        item {
            CollapsibleSection("ATRIBUTOS", summary = "${system.attributes.size} atributos", startExpanded = false) {
                if (system.attributes.isEmpty()) EmptyHint("Nenhum atributo. Adicione abaixo.")
                system.attributes.forEachIndexed { index, a ->
                    fun update(next: UserAttribute) = onChange(system.copy(attributes = system.attributes.replaced(index, next)))
                    Surface(color = Panel2, shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextInput("Nome", a.label, { update(a.copy(label = it)) }, Modifier.weight(1f))
                                IconButton(onClick = { onChange(system.copy(attributes = system.attributes.without(index))) }) {
                                    Icon(Icons.Default.Close, "Remover atributo", tint = Muted)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextInput("Sigla", a.abbr, { update(a.copy(abbr = it.take(5))) }, Modifier.weight(1f))
                                NumberField("Padrão", a.default, { update(a.copy(default = it)) }, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NumberField("Mín. (0 = livre)", a.min ?: 0, { update(a.copy(min = if (it == 0) null else it)) }, Modifier.weight(1f))
                                NumberField("Máx. (0 = livre)", a.max ?: 0, { update(a.copy(max = if (it == 0) null else it)) }, Modifier.weight(1f))
                            }
                            ToggleRow("Calcula modificador", a.hasModifier) { update(a.copy(hasModifier = it)) }
                            if (a.hasModifier) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NumberField("Base", a.modBase, { update(a.copy(modBase = it)) }, Modifier.weight(1f))
                                    NumberField("Divisor", a.modDivisor, { update(a.copy(modDivisor = it.coerceAtLeast(1))) }, Modifier.weight(1f))
                                }
                                Text("Modificador = piso((valor − base) ÷ divisor). D&D: base 10 e divisor 2.", color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
                AddRowButton("Adicionar atributo") {
                    val n = system.attributes.size + 1
                    onChange(system.copy(attributes = system.attributes + UserAttribute(UserSystemStore.newKey(), "Atributo $n", "AT$n")))
                }
            }
        }

        item {
            CollapsibleSection("RECURSOS", summary = "${system.resources.size} recursos", startExpanded = false) {
                if (system.resources.isEmpty()) EmptyHint("Nenhum recurso (vida, mana, sanidade…).")
                system.resources.forEachIndexed { index, r ->
                    fun update(next: UserResource) = onChange(system.copy(resources = system.resources.replaced(index, next)))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextInput("Nome", r.label, { update(r.copy(label = it)) }, Modifier.weight(1f))
                        NumberField("Máx. padrão", r.defaultMax, { update(r.copy(defaultMax = it.coerceAtLeast(0))) }, Modifier.weight(0.7f))
                        IconButton(onClick = { onChange(system.copy(resources = system.resources.without(index))) }) {
                            Icon(Icons.Default.Close, "Remover recurso", tint = Muted)
                        }
                    }
                }
                AddRowButton("Adicionar recurso") {
                    onChange(system.copy(resources = system.resources + UserResource(UserSystemStore.newKey(), "Novo recurso", 10)))
                }
            }
        }

        item {
            CollapsibleSection("PERÍCIAS", summary = "${system.skills.size} perícias", startExpanded = false) {
                if (system.skills.isEmpty()) EmptyHint("Nenhuma perícia.")
                system.skills.forEachIndexed { index, skill ->
                    fun update(next: UserSkill) = onChange(system.copy(skills = system.skills.replaced(index, next)))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextInput("Nome", skill.name, { update(skill.copy(name = it)) }, Modifier.weight(1f))
                        AttributePicker(system.attributes, skill.attributeKey) { update(skill.copy(attributeKey = it)) }
                        IconButton(onClick = { onChange(system.copy(skills = system.skills.without(index))) }) {
                            Icon(Icons.Default.Close, "Remover perícia", tint = Muted)
                        }
                    }
                }
                AddRowButton("Adicionar perícia") { onChange(system.copy(skills = system.skills + UserSkill("Nova perícia"))) }
                Text("Fichas já criadas recebem as perícias novas pelo botão \"Adicionar perícias do sistema\".", color = Muted, fontSize = 12.sp)
            }
        }

        item {
            CollapsibleSection("DADOS E FÓRMULAS", summary = system.dice.joinToString(" ") { "d$it" }, startExpanded = false) {
                ALL_DICE_SIDES.chunked(4).forEach { rowDice ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowDice.forEach { sides ->
                            FilterChip(
                                selected = sides in system.dice,
                                onClick = { onChange(system.copy(dice = if (sides in system.dice) system.dice - sides else system.dice + sides)) },
                                label = { Text("d$sides") }
                            )
                        }
                    }
                }
                TextInput("Fórmula de teste", system.testFormula, { onChange(system.copy(testFormula = it)) })
                TextInput("Fórmula de iniciativa", system.initiativeFormula, { onChange(system.copy(initiativeFormula = it)) })
                Text("Exemplo: 1d20 + DES + PROF. A sigla de um atributo vale o seu modificador (ou o valor, se ele não calcula modificador). " +
                    "A fórmula de teste sugere o texto do Motor e define o dado-base das rolagens de atributo e perícia (vazio = 1d20); " +
                    "vazia, a iniciativa usa o dado-base + DES (quando há).", color = Muted, fontSize = 12.sp)
                ToggleRow("Reserva de dados (conta sucessos)", system.rollStyle == ROLL_POOL) {
                    onChange(system.copy(rollStyle = if (it) ROLL_POOL else ROLL_SUM))
                }
                if (system.rollStyle == ROLL_POOL) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Lados do dado", system.poolSides, { onChange(system.copy(poolSides = it.coerceIn(2, 1000))) }, Modifier.weight(1f))
                        NumberField("Sucesso a partir de", system.poolTarget, { onChange(system.copy(poolTarget = it.coerceIn(2, system.poolSides))) }, Modifier.weight(1f))
                    }
                    Text("Testes de atributo e perícia rolam uma reserva: atributo + pontos da perícia dados; cada dado com o valor de sucesso ou mais conta um sucesso.",
                        color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}
