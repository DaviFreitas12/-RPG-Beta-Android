package com.example.rpgsombrio.ui.combat

import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.rpgsombrio.ui.components.CollapsibleSection
import com.example.rpgsombrio.ui.components.signed
import com.example.rpgsombrio.ui.theme.Night
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.rpgsombrio.engine.EncounterEntry
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.CombatStore
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.SheetRole
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.data.adjustedDamage
import com.example.rpgsombrio.data.damageMultiplierFor
import com.example.rpgsombrio.data.fitsSystem
import com.example.rpgsombrio.data.shortResourceLabel
import com.example.rpgsombrio.engine.ClientMsg
import com.example.rpgsombrio.engine.Combatant
import com.example.rpgsombrio.engine.CombatantSource
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.attributeVariable
import com.example.rpgsombrio.engine.buildSnapshot
import com.example.rpgsombrio.engine.encode
import com.example.rpgsombrio.engine.helloToCombatant
import com.example.rpgsombrio.engine.baseDie
import com.example.rpgsombrio.engine.characterCombatant
import com.example.rpgsombrio.engine.characterVariables
import com.example.rpgsombrio.engine.conditionEmoji
import com.example.rpgsombrio.engine.defaultTestFormula
import com.example.rpgsombrio.engine.isDying
import com.example.rpgsombrio.engine.monsterCombatant
import com.example.rpgsombrio.engine.monsterVariables
import com.example.rpgsombrio.engine.nextTurn
import com.example.rpgsombrio.engine.parseVariableList
import com.example.rpgsombrio.engine.rollAttributeCheck
import com.example.rpgsombrio.engine.rollDeathSave
import com.example.rpgsombrio.engine.rollFormula
import com.example.rpgsombrio.engine.rollPoolLine
import com.example.rpgsombrio.engine.rollSkillCheck
import com.example.rpgsombrio.engine.tickConditions
import com.example.rpgsombrio.engine.withCondition
import com.example.rpgsombrio.engine.withDamage
import com.example.rpgsombrio.engine.withoutCondition
import com.example.rpgsombrio.ui.characters.ConditionsDialog
import com.example.rpgsombrio.ui.components.EditorCard
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.RollLogBar
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.components.ToggleRow
import com.example.rpgsombrio.ui.components.push
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel2

private const val MANUAL_SOURCE = ""
private const val CHARACTER_PREFIX = "c:"
private const val MONSTER_PREFIX = "m:"

/** Botão com menu suspenso simples: mostra [label] e devolve o índice da opção escolhida. */
@Composable
internal fun PickButton(label: String, options: List<String>, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Default.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { index, text ->
                DropdownMenuItem(text = { Text(text) }, onClick = { open = false; onPick(index) })
            }
        }
    }
}

/**
 * Tela da Mesa (0.9.17), antes "Motor". O estado do combate vive no [CombatController] (assim a mesa em rede continua
 * funcionando em qualquer aba). Aqui ficam só a tela e as rolagens avulsas.
 *
 * Aba Combate (padrão): quem age agora no topo, próximos em ordem, encontros preparados, adicionar à batalha, rede.
 * O botão de passar o turno e o de desfazer ficam fixos no rodapé.
 */
@Composable
internal fun RpgEngineScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val controller = CombatController
    remember { controller.init(prefs); TableSession.bind(context); true }
    // Ao abrir a tela, traz o que outras telas gravaram (Bestiário, fichas) e os PV atuais das fichas.
    LaunchedEffect(Unit) { controller.reload() }

    val engine = controller.engine
    val log = controller.log
    val combatants = controller.combatants
    // Lista observável: trocar o papel (Jogador/Mestre) de uma ficha atualiza a tela na hora.
    val characters = remember { mutableStateListOf<CharacterSheet>().apply { addAll(SheetStore.loadCharacters(prefs)) } }
    val monsters = remember { SheetStore.loadMonsters(prefs) }
    val library = remember { SheetStore.loadItems(prefs) }

    var sourceKey by remember { mutableStateOf(prefs.getString("engine_source", null) ?: characters.firstOrNull()?.let { CHARACTER_PREFIX + it.id } ?: MANUAL_SOURCE) }
    var manualVars by remember { mutableStateOf(prefs.getString("engine_manual_vars", "FOR=3, DES=2, PROF=2") ?: "") }
    var formula by remember { mutableStateOf(prefs.getString("engine_formula", "1d20 + FOR + PROF") ?: "1d20 + FOR + PROF") }
    var difficulty by remember { mutableStateOf(15) }
    var poolDice by remember { mutableStateOf(3) }
    var poolDifficulty by remember { mutableStateOf(1) }
    var quantity by remember { mutableStateOf(1) }
    var actorName by remember { mutableStateOf("") }
    var actorHp by remember { mutableStateOf(20) }
    var actorDefense by remember { mutableStateOf(13) }

    val sourceCharacter = if (sourceKey.startsWith(CHARACTER_PREFIX)) characters.firstOrNull { it.id == sourceKey.removePrefix(CHARACTER_PREFIX) } else null
    val sourceMonster = if (sourceKey.startsWith(MONSTER_PREFIX)) monsters.firstOrNull { it.id == sourceKey.removePrefix(MONSTER_PREFIX) } else null
    val systemId = sourceCharacter?.systemId ?: sourceMonster?.systemId ?: SheetStore.activeSystemId(prefs)
    val profile = remember(systemId) { SystemProfiles.resolve(prefs, systemId) }
    val variables: Map<String, Int> = when {
        sourceCharacter != null -> characterVariables(sourceCharacter, profile, library)
        sourceMonster != null -> monsterVariables(sourceMonster, profile)
        else -> parseVariableList(manualVars)
    }
    val sourceAttributes: Map<String, Int> = sourceCharacter?.attributes ?: sourceMonster?.attributes ?: emptyMap()
    val sourceSkills = sourceCharacter?.skills?.filter { it.fitsSystem(sourceCharacter.systemId) } ?: emptyList()
    val defenseLabel = if (profile.usesArmorClass) "CA" else "Defesa"
    // Sem ficha de personagem escolhida (criatura ou valores manuais), quem opera o combate é o Mestre.
    val userRole = sourceCharacter?.role ?: SheetRole.MASTER

    fun setRole(character: CharacterSheet, role: String) {
        val at = characters.indexOfFirst { it.id == character.id }
        if (at < 0) return
        characters[at] = characters[at].copy(role = SheetRole.normalize(role))
        SheetStore.saveCharacters(prefs, characters.toList())
    }

    val ordered = controller.ordered
    val activeIndex = ordered.indexOfFirst { it.id == controller.activeId }.coerceAtLeast(0)
    val round = controller.round
    var editingConditionsId by remember { mutableStateOf("") }

    fun pickSource(key: String) {
        sourceKey = key
        prefs.edit().putString("engine_source", key).apply()
        val newSystem = when {
            key.startsWith(CHARACTER_PREFIX) -> characters.firstOrNull { it.id == key.removePrefix(CHARACTER_PREFIX) }?.systemId
            key.startsWith(MONSTER_PREFIX) -> monsters.firstOrNull { it.id == key.removePrefix(MONSTER_PREFIX) }?.systemId
            else -> null
        }
        if (newSystem != null) {
            val suggestion = defaultTestFormula(SystemProfiles.resolve(prefs, newSystem))
            if (suggestion.isNotBlank()) formula = suggestion
        }
    }

    val sourceOptions: List<Pair<String, String>> =
        listOf(MANUAL_SOURCE to "Sem ficha (valores manuais)") +
            characters.map { (CHARACTER_PREFIX + it.id) to "Personagem • ${it.name.ifBlank { "Sem nome" }}" } +
            monsters.map { (MONSTER_PREFIX + it.id) to "Criatura • ${it.name.ifBlank { "Sem nome" }}" }
    val sourceLabel = when {
        sourceCharacter != null -> "Personagem • ${sourceCharacter.name.ifBlank { "Sem nome" }}"
        sourceMonster != null -> "Criatura • ${sourceMonster.name.ifBlank { "Sem nome" }}"
        else -> "Sem ficha (valores manuais)"
    }

    // Jogador conectado: o personagem dele vem do mestre.
    val clientMode = TableSession.mode == "client"
    val remoteOwn: Combatant? = if (clientMode) {
        TableSession.snapshot?.fighters?.firstOrNull { it.id == TableSession.myId }?.let { f ->
            Combatant(
                id = f.id, name = f.name, initiative = f.initiative, currentHp = f.hp, maxHp = f.maxHp, armorClass = f.armorClass,
                conditions = f.conditions, sheetId = f.id, sheetKind = CombatantSource.CHARACTER, tempHp = f.tempHp
            )
        }
    } else null
    val tablePlayerSheet = if (SheetRole.normalize(userRole) == SheetRole.PLAYER) sourceCharacter else null
    val isMaster = !clientMode && tablePlayerSheet == null

    var tab by rememberSaveable { mutableStateOf("Combate") }

    Column(modifier.fillMaxSize()) {
        combatants.firstOrNull { it.id == editingConditionsId }?.let { f ->
            ConditionsDialog(f, { condition, rounds -> controller.toggleCondition(f.id, condition, rounds) }, { editingConditionsId = "" })
        }
        TabRow(selectedTabIndex = if (tab == "Rolar") 0 else 1, containerColor = Night, contentColor = Accent) {
            listOf("Rolar", "Combate").forEach { name ->
                val label = if (name == "Combate" && combatants.isNotEmpty()) "Combate (${combatants.size})" else name
                Tab(
                    selected = name == tab, onClick = { tab = name },
                    text = { Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) },
                    unselectedContentColor = Muted
                )
            }
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (tab == "Rolar") {
            item {
                EditorCard("QUEM ESTÁ ROLANDO", "As rolagens usam os valores da ficha ou criatura escolhida") {
                    PickButton(sourceLabel, sourceOptions.map { it.second }) { pickSource(sourceOptions[it].first) }
                    if (sourceCharacter != null || sourceMonster != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            InfoPill(profile.name)
                            if (profile.isPool) InfoPill("Reserva de d${profile.poolSides}")
                        }
                        val summary = profile.attributes.joinToString("   ") { attr ->
                            val bonus = variables[attributeVariable(attr)]
                            "${attr.abbr} ${if (bonus == null) "?" else if (attr.hasModifier) signed(bonus) else bonus.toString()}"
                        }
                        if (summary.isNotBlank()) Text(summary, color = Ink, fontSize = 12.sp)
                        Text("Na fórmula, a sigla do atributo vale o modificador. Também valem PROF, NIVEL e CA.",
                            color = Muted, fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val cd = if (profile.isPool) poolDifficulty else difficulty.takeIf { it > 0 }
                            PickButton("Atributo", profile.attributes.map { it.label }, Modifier.weight(1f)) { index ->
                                log.push(rollAttributeCheck(engine, profile, sourceAttributes, profile.attributes[index], cd))
                            }
                            if (sourceCharacter != null && profile.usesSkills && sourceSkills.isNotEmpty()) {
                                PickButton("Perícia", sourceSkills.map { it.name }, Modifier.weight(1f)) { index ->
                                    log.push(rollSkillCheck(engine, sourceCharacter, profile, sourceSkills[index], cd))
                                }
                            }
                        }
                    } else {
                        TextInput("Valores (ex.: FOR=3, DES=2)", manualVars, {
                            manualVars = it
                            prefs.edit().putString("engine_manual_vars", it).apply()
                        })
                        Text("Escolha uma ficha acima para usar os valores dela automaticamente.",
                            color = Muted, fontSize = 13.sp)
                    }
                }
            }

            item {
                EditorCard("ROLAGEM E TESTES",
                    if (profile.isPool) "Reserva de dados: cada d${profile.poolSides} com ${profile.poolTarget}+ é um sucesso"
                    else "Exemplos: 1d20 + FOR + PROF, 2d6 + 3, (1d8 + FOR) * 2") {
                    if (profile.isPool) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberField("Dados na reserva", poolDice, { poolDice = it.coerceIn(0, 100) }, Modifier.weight(1f))
                            NumberField("Dificuldade", poolDifficulty, { poolDifficulty = it.coerceAtLeast(1) }, Modifier.weight(1f))
                        }
                        Button(onClick = { log.push(rollPoolLine(engine, "Reserva", poolDice, profile, poolDifficulty)) },
                            modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                            Icon(Icons.Default.Casino, null); Spacer(Modifier.width(8.dp)); Text("Rolar reserva")
                        }
                    } else {
                        TextInput("Fórmula", formula, { formula = it })
                        NumberField("CD (0 = sem CD)", difficulty, { difficulty = it.coerceAtLeast(0) }, Modifier.fillMaxWidth())
                        Button(onClick = {
                            log.push(rollFormula(engine, "Teste", formula, variables, difficulty.takeIf { it > 0 }))
                            prefs.edit().putString("engine_formula", formula).apply()
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                            Icon(Icons.Default.Casino, null); Spacer(Modifier.width(8.dp)); Text("Rolar")
                        }
                    }
                    Text("Atalhos de rolagem", color = Muted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        profile.dice.forEach { die ->
                            OutlinedButton(onClick = { log.push(rollFormula(engine, "d$die", "d$die", emptyMap())) },
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)) { Text("d$die", fontSize = 12.sp) }
                        }
                    }
                }
            }

            } else {
            // Jogador: entrar na mesa do mestre vem primeiro (sem isso ele não vê nada).
            if (!isMaster) {
                item { TableLinkCard(playerSheet = tablePlayerSheet, library = library, prefs = prefs, engine = engine) }
            }
            item {
                TableModeScreen(
                    fighters = if (clientMode) listOfNotNull(remoteOwn) else ordered, activeIndex = activeIndex, round = round,
                    library = library, prefs = prefs, engine = engine, defenseLabel = defenseLabel,
                    playerSheet = tablePlayerSheet,
                    remote = clientMode,
                    monsters = monsters,
                    statusLine = if (TableSession.mode == "host") "${TableSession.clients} jogador(es) na rede" else "",
                    isMyTurn = clientMode && TableSession.snapshot?.activeId == TableSession.myId,
                    onEndTurn = { TableSession.send(ClientMsg.EndTurn) },
                    onAdjustHp = { fighter, delta -> if (clientMode) TableSession.send(ClientMsg.Adjust(delta)) else controller.setHp(fighter, delta) },
                    onUpdateFighter = { if (clientMode) TableSession.send(ClientMsg.SetInitiative(it.initiative)) else controller.updateFighter(it) },
                    onSetInitiative = { fighter, value -> controller.setInitiative(fighter.id, value) },
                    onNextTurn = { controller.advanceTurn() },
                    onEndCombat = { controller.endCombat() },
                    onOpenConditions = { editingConditionsId = it },
                    onRemove = { id -> controller.remove(id) },
                    onJoin = { sheet, initiative -> controller.joinCombat(sheet, initiative, library) },
                    onAddAllCharacters = { controller.addAllCharacters(characters, library) }
                )
            }
            if (isMaster) {
                item {
                    val currentGroups = combatants
                        .filter { it.sheetKind == CombatantSource.MONSTER && it.sheetId.isNotBlank() }
                        .groupingBy { it.sheetId }.eachCount()
                        .map { EncounterEntry(it.key, it.value) }
                    EncountersSection(
                        prefs = prefs, monsters = monsters, currentGroups = currentGroups,
                        startExpanded = ordered.isEmpty(),
                        onStart = { controller.startEncounter(it, monsters, characters, library) }
                    )
                }
                item {
                    CollapsibleSection("ADICIONAR À BATALHA", summary = "Personagem, criatura ou à mão", startExpanded = false) {
                        PickButton(sourceLabel, sourceOptions.map { it.second }) { pickSource(sourceOptions[it].first) }
                        if (sourceCharacter != null) {
                            Button(onClick = { controller.addCharacter(sourceCharacter, library) },
                                modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar ${sourceCharacter.name.ifBlank { "personagem" }}")
                            }
                        } else if (sourceMonster != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                NumberField("Quantidade", quantity, { quantity = it.coerceIn(1, 20) }, Modifier.weight(1f))
                                Button(onClick = { controller.addMonsters(sourceMonster, quantity) },
                                    modifier = Modifier.weight(1.4f), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar")
                                }
                            }
                        }
                        Text("Ou adicione à mão (a iniciativa você digita depois):", color = Muted, fontSize = 13.sp)
                        TextInput("Nome do combatente", actorName, { actorName = it })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberField("PV máx.", actorHp, { actorHp = it.coerceAtLeast(0) }, Modifier.weight(1f))
                            NumberField(defenseLabel, actorDefense, { actorDefense = it.coerceAtLeast(0) }, Modifier.weight(1f))
                        }
                        OutlinedButton(onClick = {
                            if (actorName.isNotBlank() && actorHp > 0) {
                                controller.addManual(actorName, actorHp, actorDefense)
                                actorName = ""
                            } else log.push(RollLine("Informe um nome e PV máximos maiores que zero.", null))
                        }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar combatente")
                        }
                    }
                }
                item { TableLinkCard(playerSheet = tablePlayerSheet, library = library, prefs = prefs, engine = engine) }
            }
            if (sourceCharacter != null) {
                item {
                    EditorCard("PAPEL NA MESA", "Jogador vê o próprio personagem e as ações; Mestre controla a cena inteira") {
                        RoleSelector(sourceCharacter.role, { setRole(sourceCharacter, it) })
                    }
                }
            }
            }
        }
        // Mestre: passar o turno e desfazer ficam sempre à mão, sem rolar a tela.
        if (tab == "Combate" && isMaster && ordered.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { controller.undo() }, enabled = controller.canUndo,
                    modifier = Modifier.height(56.dp), shape = RoundedCornerShape(16.dp)
                ) { Text("↶ Desfazer") }
                Button(
                    onClick = { controller.advanceTurn() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Próximo turno  ▶", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }
        RollLogBar(log) { log.clear() }
    }
}
