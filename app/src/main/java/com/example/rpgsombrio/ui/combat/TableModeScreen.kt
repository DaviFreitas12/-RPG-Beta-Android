package com.example.rpgsombrio.ui.combat

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.engine.monsterCheatSheet
import com.example.rpgsombrio.ui.components.ToggleRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.engine.AttackOption
import com.example.rpgsombrio.engine.Combatant
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.actionBlocker
import com.example.rpgsombrio.engine.baseDie
import com.example.rpgsombrio.engine.characterAttacks
import com.example.rpgsombrio.engine.characterVariables
import com.example.rpgsombrio.engine.conditionEmoji
import com.example.rpgsombrio.engine.conditionLabel
import com.example.rpgsombrio.engine.deathStatus
import com.example.rpgsombrio.engine.isDying
import com.example.rpgsombrio.engine.tableAttack
import com.example.rpgsombrio.engine.tableAttackRoll
import com.example.rpgsombrio.engine.tableDamageTotal
import com.example.rpgsombrio.ui.components.EditorCard
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.signed
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Amber
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Danger
import com.example.rpgsombrio.ui.theme.Good
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Night
import com.example.rpgsombrio.ui.theme.Panel
import com.example.rpgsombrio.ui.theme.Panel2

/**
 * Modo mesa (0.9.15): combate para jogo presencial. Todo mundo rola o dado físico; o app só acompanha.
 *
 *  - Mestre: quem age agora fica no topo, com as ações da criatura prontas; depois vêm os próximos, em ordem de turno.
 *    A iniciativa é digitada direto na linha; dano e cura têm botões de um toque e um teclado grande.
 *    O botão de passar o turno (e o de desfazer) ficam fixos no rodapé da tela.
 *  - Jogador: o próprio personagem (PV, condições) e as cartas de ação, que somam o bônus ao dado que ele digitar.
 *
 * Sem animações e sem botões travados: cada toque tem efeito imediato.
 */
@Composable
internal fun TableModeScreen(
    fighters: List<Combatant>, activeIndex: Int, round: Int,
    library: List<ItemEntry>, prefs: SharedPreferences, engine: RpgEngine,
    defenseLabel: String,
    playerSheet: CharacterSheet?,
    onAdjustHp: (Combatant, Int) -> Unit,
    onUpdateFighter: (Combatant) -> Unit,
    onNextTurn: () -> Unit, onEndCombat: () -> Unit,
    onOpenConditions: (String) -> Unit, onRemove: (String) -> Unit,
    onJoin: (CharacterSheet, Int) -> Unit, onAddAllCharacters: () -> Unit,
    /** Mestre: a iniciativa digitada na linha (criaturas do mesmo tipo sem iniciativa acompanham a primeira). */
    onSetInitiative: (Combatant, Int) -> Unit = { _, _ -> },
    /** Mestre: fichas das criaturas, para mostrar as ações com os números prontos. */
    monsters: List<MonsterSheet> = emptyList(),
    /** Mestre: texto extra no topo (ex.: quantos jogadores estão conectados). */
    statusLine: String = "",
    /** Jogador conectado: é a vez dele e o botão "terminei meu turno". */
    isMyTurn: Boolean = false, onEndTurn: () -> Unit = {},
    /** Jogador conectado ao mestre pela rede: o personagem vem do mestre e as condições ficam com ele. */
    remote: Boolean = false
) {
    if (playerSheet != null) {
        val own = fighters.firstOrNull { it.sheetId == playerSheet.id && it.sheetKind == com.example.rpgsombrio.engine.CombatantSource.CHARACTER }
        PlayerTablePanel(
            sheet = playerSheet, fighter = own, library = library, prefs = prefs, engine = engine, defenseLabel = defenseLabel,
            onAdjustHp = onAdjustHp, onUpdateFighter = onUpdateFighter, onOpenConditions = onOpenConditions, onJoin = onJoin,
            remote = remote, isMyTurn = isMyTurn, onEndTurn = onEndTurn
        )
    } else {
        MasterTablePanel(
            fighters = fighters, activeIndex = activeIndex, round = round, defenseLabel = defenseLabel,
            onAdjustHp = onAdjustHp, onUpdateFighter = onUpdateFighter, onNextTurn = onNextTurn, onEndCombat = onEndCombat,
            onOpenConditions = onOpenConditions, onRemove = onRemove, onAddAllCharacters = onAddAllCharacters,
            onSetInitiative = onSetInitiative, monsters = monsters, statusLine = statusLine
        )
    }
}

// --- mestre ------------------------------------------------------------------------------------

@Composable
private fun MasterTablePanel(
    fighters: List<Combatant>, activeIndex: Int, round: Int, defenseLabel: String,
    onAdjustHp: (Combatant, Int) -> Unit, onUpdateFighter: (Combatant) -> Unit,
    onNextTurn: () -> Unit, onEndCombat: () -> Unit,
    onOpenConditions: (String) -> Unit, onRemove: (String) -> Unit, onAddAllCharacters: () -> Unit,
    onSetInitiative: (Combatant, Int) -> Unit, monsters: List<MonsterSheet>, statusLine: String
) {
    var selectedId by rememberSaveable { mutableStateOf("") }
    val active = fighters.getOrNull(activeIndex)
    // Quando a vez muda, o cartão aberto à mão fecha: quem age agora é sempre o que aparece primeiro e aberto.
    LaunchedEffect(active?.id) { selectedId = "" }
    val unset = fighters.count { it.initiative == 0 }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column {
            Text(
                if (fighters.isEmpty()) "MESA" else "RODADA $round",
                color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp
            )
            if (fighters.isNotEmpty()) {
                Text(
                    "Vez de ${active?.name ?: "—"}" + if (statusLine.isBlank()) "" else " • $statusLine",
                    color = Muted, fontSize = 13.sp
                )
            } else if (statusLine.isNotBlank()) {
                Text(statusLine, color = Muted, fontSize = 13.sp)
            }
        }

        if (fighters.isEmpty()) {
            Text(
                "Ninguém na luta ainda. Inicie um encontro preparado, adicione criaturas abaixo ou traga todos os personagens de uma vez.",
                color = Muted, fontSize = 13.sp
            )
            Button(
                onClick = onAddAllCharacters, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Brand)
            ) { Text("Adicionar todos os personagens") }
        } else {
            if (unset > 0) {
                Text(
                    "$unset sem iniciativa (INI ?). Toque no campo, digite o resultado do dado e confirme. Criaturas do mesmo tipo acompanham a primeira.",
                    color = Amber, fontSize = 12.sp
                )
            }
            // Quem age agora no topo; depois os próximos, na ordem em que vão jogar.
            val upcoming = fighters.drop(activeIndex + 1) + fighters.take(activeIndex)
            val shown = listOfNotNull(active) + upcoming
            shown.forEachIndexed { position, f ->
                key(f.id) {
                    val isActive = position == 0
                    FighterTableRow(
                        fighter = f, isActive = isActive, expanded = isActive || f.id == selectedId, defenseLabel = defenseLabel,
                        monster = if (f.sheetKind == com.example.rpgsombrio.engine.CombatantSource.MONSTER) monsters.firstOrNull { it.id == f.sheetId } else null,
                        onToggle = { selectedId = if (f.id == selectedId) "none" else f.id },
                        onAdjustHp = { delta -> onAdjustHp(f, delta) },
                        onSetInitiative = { value -> onSetInitiative(f, value) },
                        onConditions = { onOpenConditions(f.id) },
                        onRemove = { onRemove(f.id) }
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onAddAllCharacters, modifier = Modifier.weight(1f)) { Text("+ Personagens") }
                OutlinedButton(onClick = onEndCombat, modifier = Modifier.weight(1f)) { Text("Encerrar combate") }
            }
        }
    }
}

@Composable
private fun FighterTableRow(
    fighter: Combatant, isActive: Boolean, expanded: Boolean, defenseLabel: String, monster: MonsterSheet?,
    onToggle: () -> Unit, onAdjustHp: (Int) -> Unit, onSetInitiative: (Int) -> Unit,
    onConditions: () -> Unit, onRemove: () -> Unit
) {
    val blocker = if (isActive) actionBlocker(fighter.conditions) else null
    val cheatSheet = remember(monster) { if (monster == null) emptyList() else monsterCheatSheet(monster) }
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isActive) Panel2 else Night),
        border = BorderStroke(1.dp, if (isActive) Accent.copy(alpha = .6f) else Hairline),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    (if (isActive) "▶ " else "") + fighter.name +
                        if (fighter.isDying()) " (morrendo)" else if (fighter.currentHp <= 0) " (derrotado)" else "",
                    color = if (fighter.currentHp <= 0) Muted else Ink, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                )
                InlineInitiative(fighter.initiative, onSetInitiative)
            }
            TableHpBar(fighter.currentHp, fighter.maxHp, Modifier.fillMaxWidth())
            Text(
                "PV ${fighter.currentHp}/${fighter.maxHp}${if (fighter.tempHp > 0) " (+${fighter.tempHp} temp.)" else ""} • $defenseLabel ${fighter.armorClass}",
                color = Muted, fontSize = 13.sp
            )
            fighter.deathStatus().takeIf { it.isNotBlank() }?.let { Text(it, color = Danger, fontSize = 12.sp) }
            if (fighter.conditions.isNotEmpty()) {
                Text(fighter.conditions.joinToString("  ") { "${conditionEmoji(it)} ${fighter.conditionLabel(it)}" }, color = Accent, fontSize = 12.sp)
            }
            if (blocker != null) Text("Está $blocker e perde o turno.", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (expanded) {
                if (cheatSheet.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("AÇÕES (role o dado e some o bônus)", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        cheatSheet.forEach { Text(it, color = Ink, fontSize = 13.sp) }
                    }
                }
                QuickHpControls(fighter.id, fighter.name, onAdjustHp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onConditions, modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)) { Text("Condições", maxLines = 1) }
                    TextButton(onClick = onRemove) { Text("Remover", color = Muted, maxLines = 1) }
                }
            }
        }
    }
}

/**
 * Campo de iniciativa direto na linha: sem diálogo. O valor vale ao confirmar no teclado ou ao sair do campo
 * (assim a lista não pula de lugar enquanto o mestre ainda digita). Vazio e em âmbar = "INI ?".
 */
@Composable
private fun InlineInitiative(value: Int, onCommit: (Int) -> Unit) {
    var text by remember(value) { mutableStateOf(if (value == 0) "" else value.toString()) }
    var focused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    fun commit() {
        val parsed = text.toIntOrNull()
        if (parsed != null && parsed != value) onCommit(parsed) else text = if (value == 0) "" else value.toString()
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("INI", color = Muted, fontSize = 12.sp)
        BasicTextField(
            value = text,
            onValueChange = { raw -> text = raw.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(4) },
            singleLine = true,
            textStyle = TextStyle(color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit(); focusManager.clearFocus() }),
            modifier = Modifier
                .width(58.dp).height(40.dp)
                .background(Panel2, RoundedCornerShape(10.dp))
                .border(1.dp, if (value == 0) Amber else Hairline, RoundedCornerShape(10.dp))
                .onFocusChanged { state ->
                    if (focused && !state.isFocused) commit()
                    focused = state.isFocused
                },
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().fillMaxHeight(), contentAlignment = Alignment.Center) {
                    if (text.isEmpty()) Text("?", color = Muted, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    inner()
                }
            }
        )
    }
}

// --- jogador -----------------------------------------------------------------------------------

@Composable
private fun PlayerTablePanel(
    sheet: CharacterSheet, fighter: Combatant?, library: List<ItemEntry>,
    prefs: SharedPreferences, engine: RpgEngine, defenseLabel: String,
    onAdjustHp: (Combatant, Int) -> Unit, onUpdateFighter: (Combatant) -> Unit,
    onOpenConditions: (String) -> Unit, onJoin: (CharacterSheet, Int) -> Unit, remote: Boolean,
    isMyTurn: Boolean, onEndTurn: () -> Unit
) {
    val profile = remember(sheet.systemId) { SystemProfiles.resolve(prefs, sheet.systemId) }
    val variables = characterVariables(sheet, profile, library)
    val options = characterAttacks(sheet, library)
    val d20Based = baseDie(profile) == "1d20"
    var joinInitiative by remember { mutableStateOf(0) }
    var editingInitiative by rememberSaveable { mutableStateOf(false) }
    // Modo simples: o jogador só acompanha PV e turno; o dado e a conta ficam na mesa, falados em voz alta.
    var simple by rememberSaveable { mutableStateOf(prefs.getBoolean("table_simple", false)) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text("MODO MESA", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("${sheet.name.ifBlank { "Seu personagem" }} • role o dado físico e digite o resultado aqui.", color = Muted, fontSize = 13.sp)
        }

        if (fighter == null && remote) {
            Text("Conectado. Aguardando o mestre colocar você na luta…", color = Muted, fontSize = 13.sp)
        } else if (fighter == null) {
            EditorCard("ENTRAR NA LUTA", "Digite a iniciativa que você tirou no dado (já com o bônus).") {
                NumberField("Iniciativa", joinInitiative, { joinInitiative = it }, Modifier.fillMaxWidth())
                Button(
                    onClick = { onJoin(sheet, joinInitiative) }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand)
                ) { Text("Entrar na luta") }
            }
        } else {
            val blocker = actionBlocker(fighter.conditions)
            Card(
                colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Hairline), modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            fighter.name + if (fighter.isDying()) " (morrendo)" else if (fighter.currentHp <= 0) " (derrotado)" else "",
                            color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        InfoPill(if (fighter.initiative == 0) "INI ?" else "INI ${fighter.initiative}")
                    }
                    TableHpBar(fighter.currentHp, fighter.maxHp, Modifier.fillMaxWidth())
                    Text(
                        "PV ${fighter.currentHp}/${fighter.maxHp}${if (fighter.tempHp > 0) " (+${fighter.tempHp} temp.)" else ""} • $defenseLabel ${fighter.armorClass}",
                        color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold
                    )
                    fighter.deathStatus().takeIf { it.isNotBlank() }?.let { Text(it, color = Danger, fontSize = 12.sp) }
                    if (fighter.conditions.isNotEmpty()) {
                        Text(fighter.conditions.joinToString("  ") { "${conditionEmoji(it)} ${fighter.conditionLabel(it)}" }, color = Accent, fontSize = 13.sp)
                    }
                    if (blocker != null) Text("Você está $blocker e não pode agir.", color = Amber, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    QuickHpControls(fighter.id, fighter.name) { delta -> onAdjustHp(fighter, delta) }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!remote) OutlinedButton(onClick = { onOpenConditions(fighter.id) }, modifier = Modifier.weight(1f)) { Text("Condições", maxLines = 1) }
                        OutlinedButton(onClick = { editingInitiative = true }, modifier = Modifier.weight(1f)) { Text("Iniciativa", maxLines = 1) }
                    }
                }
            }
            if (editingInitiative) {
                InitiativeDialog(fighter, onSave = { onUpdateFighter(fighter.copy(initiative = it)); editingInitiative = false }, onDismiss = { editingInitiative = false })
            }
            if (remote && isMyTurn) {
                Button(
                    onClick = onEndTurn, modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand), shape = RoundedCornerShape(16.dp)
                ) { Text("Terminei meu turno  ▶", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
            }
        }

        ToggleRow("Modo simples (só PV e turno, sem calculadora)", simple) {
            simple = it
            prefs.edit().putBoolean("table_simple", it).apply()
        }
        if (!simple) {
            Text("SUAS AÇÕES", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            if (options.isEmpty()) {
                Text("Este personagem ainda não tem ataques. Cadastre-os na ficha (aba Combate).", color = Muted, fontSize = 13.sp)
            }
            options.forEachIndexed { index, option ->
                key(index, option.label) { PlayerActionCard(option, variables, engine, d20Based) }
            }
        }
    }
}

@Composable
private fun PlayerActionCard(option: AttackOption, variables: Map<String, Int>, engine: RpgEngine, d20Based: Boolean) {
    val info = remember(option, variables) { tableAttack(engine, option, variables) }
    var open by rememberSaveable(option.label) { mutableStateOf(false) }
    var natural by remember { mutableStateOf(0) }
    var diceSum by remember { mutableStateOf(0) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel2), shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Hairline), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.fillMaxWidth().clickable { open = !open }) {
                Text(info.name, color = Ink, fontWeight = FontWeight.Bold)
                Text(info.summary, color = Muted, fontSize = 13.sp)
            }
            if (open) {
                val toHit = info.toHit
                if (!info.autoHit && toHit != null) {
                    NumberField(if (d20Based) "Resultado do d20" else "Resultado do dado de ataque", natural, { natural = it.coerceAtLeast(0) }, Modifier.fillMaxWidth())
                    if (natural > 0) {
                        val roll = tableAttackRoll(natural, toHit)
                        Text("Acerto: ${roll.natural} ${signed(toHit)} = ${roll.total}", color = Ink, fontWeight = FontWeight.Bold)
                        if (d20Based && roll.critical) Text("20 natural: CRÍTICO! Dobre os dados do dano.", color = Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (d20Based && roll.fumble) Text("1 natural: falha crítica.", color = Danger, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
                val flat = info.damageFlat
                if (info.damageFormula.isNotBlank()) {
                    NumberField("Soma dos dados de dano", diceSum, { diceSum = it.coerceAtLeast(0) }, Modifier.fillMaxWidth())
                    if (diceSum > 0) {
                        if (flat != null) {
                            Text(
                                "Dano: $diceSum ${signed(flat)} = ${tableDamageTotal(diceSum, flat)}${if (info.damageType.isBlank()) "" else " ${info.damageType}"}",
                                color = Danger, fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text("Fórmula: ${info.damageFormula}. Aplique o que sobrar depois dos dados.", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- peças compartilhadas ----------------------------------------------------------------------

/**
 * Dano e cura de um toque: botões de valor fixo e, para outro valor, um teclado numérico grande.
 * [onAdjustHp] recebe negativo para dano.
 */
@Composable
private fun QuickHpControls(ownerId: String, ownerName: String, onAdjustHp: (Int) -> Unit) {
    var padOpen by remember(ownerId) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(-10, -5, -1, 1, 5, 10).forEach { step ->
                OutlinedButton(
                    onClick = { onAdjustHp(step) }, modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 8.dp)
                ) { Text(if (step > 0) "+$step" else "$step", color = if (step < 0) Danger else Good, fontSize = 14.sp, maxLines = 1) }
            }
        }
        OutlinedButton(onClick = { padOpen = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Outro valor…") }
    }
    if (padOpen) DamagePadDialog(ownerName, onApply = { onAdjustHp(it); padOpen = false }, onDismiss = { padOpen = false })
}

/** Teclado grande para dano e cura: digita o número e toca em Dano ou Cura. */
@Composable
private fun DamagePadDialog(name: String, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    var digits by remember { mutableStateOf("") }
    val amount = digits.toIntOrNull() ?: 0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (digits.isEmpty()) "0" else digits, color = Ink, fontSize = 36.sp, fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
                listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("C", "0", "⌫")).forEach { keys ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        keys.forEach { k ->
                            OutlinedButton(
                                onClick = {
                                    digits = when (k) {
                                        "C" -> ""
                                        "⌫" -> digits.dropLast(1)
                                        else -> if (digits.length >= 4) digits else (digits + k).trimStart('0')
                                    }
                                },
                                modifier = Modifier.weight(1f).height(54.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) { Text(k, fontSize = 20.sp, fontWeight = FontWeight.SemiBold) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { if (amount > 0) onApply(-amount) }, enabled = amount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Danger)
                ) { Text("Dano") }
                Button(
                    onClick = { if (amount > 0) onApply(amount) }, enabled = amount > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = Brand)
                ) { Text("Cura") }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun InitiativeDialog(fighter: Combatant, onSave: (Int) -> Unit, onDismiss: () -> Unit) {
    var value by remember(fighter.id) { mutableStateOf(fighter.initiative) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Iniciativa de ${fighter.name}") },
        text = { NumberField("Resultado do dado + bônus", value, { value = it }, Modifier.fillMaxWidth()) },
        confirmButton = { TextButton(onClick = { onSave(value) }) { Text("OK") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun TableHpBar(current: Int, max: Int, modifier: Modifier = Modifier) {
    val fraction = if (max <= 0) 0f else (current.toFloat() / max).coerceIn(0f, 1f)
    val color = when {
        fraction > .5f -> Good
        fraction > .25f -> Amber
        else -> Danger
    }
    Box(modifier.height(8.dp).clip(RoundedCornerShape(50)).background(Hairline)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color))
    }
}
