package com.example.rpgsombrio.ui.combat
import com.example.rpgsombrio.ui.components.signed

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.data.adjustedDamage
import com.example.rpgsombrio.data.damageMultiplierFor
import com.example.rpgsombrio.data.fitsSystem
import com.example.rpgsombrio.data.shortResourceLabel
import com.example.rpgsombrio.engine.Combatant
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.attributeVariable
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
 * Tela do Motor (0.9.1): as rolagens usam os valores de uma ficha ou criatura escolhida (ou de uma lista manual),
 * o sistema da ficha define fórmulas, dados e reservas, e o combate lê e grava os PV dos personagens.
 */
@Composable
internal fun RpgEngineScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val engine = remember { RpgEngine() }
    val characters = remember { SheetStore.loadCharacters(prefs) }
    val monsters = remember { SheetStore.loadMonsters(prefs) }
    val library = remember { SheetStore.loadItems(prefs) }
    val log = remember { mutableStateListOf<RollLine>() }

    var sourceKey by remember { mutableStateOf(prefs.getString("engine_source", MANUAL_SOURCE) ?: MANUAL_SOURCE) }
    var manualVars by remember { mutableStateOf(prefs.getString("engine_manual_vars", "FOR=3, DES=2, PROF=2") ?: "") }
    var formula by remember { mutableStateOf(prefs.getString("engine_formula", "1d20 + FOR + PROF") ?: "1d20 + FOR + PROF") }
    var difficulty by remember { mutableStateOf(15) }
    var poolDice by remember { mutableStateOf(3) }
    var poolDifficulty by remember { mutableStateOf(1) }
    var quantity by remember { mutableStateOf(1) }
    var actorName by remember { mutableStateOf("") }
    var actorHp by remember { mutableStateOf(20) }
    var actorDefense by remember { mutableStateOf(13) }
    var actorInitiativeBonus by remember { mutableStateOf(0) }

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

    val combatants = remember {
        mutableStateListOf<Combatant>().apply { addAll(CombatStore.refreshFromSheets(prefs, CombatStore.load(prefs))) }
    }
    // O turno ativo é guardado pelo id do combatente, não por posição: adicionar ou remover alguém não troca a vez.
    var activeId by remember { mutableStateOf(CombatStore.activeId(prefs)) }
    val ordered = combatants.sortedWith(
        compareByDescending<Combatant> { it.initiative }.thenBy { it.name.lowercase() }.thenBy { it.id }
    )
    val activeIndex = ordered.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
    fun persistCombat() = CombatStore.save(prefs, combatants.toList(), activeId)
    var editingConditionsId by remember { mutableStateOf("") }
    var round by remember { mutableStateOf(CombatStore.round(prefs)) }

    /** Troca o combatente de mesmo id pelo [updated], grava o combate e leva PV e condições à ficha. */
    fun updateFighter(updated: Combatant) {
        val at = combatants.indexOfFirst { it.id == updated.id }
        if (at < 0) return
        combatants[at] = updated
        persistCombat()
        CombatStore.syncToSheet(prefs, updated)
    }

    /** Marca (com [rounds] de duração, 0 = até remover) ou desmarca a condição. */
    fun toggleCondition(fighterId: String, condition: String, rounds: Int) {
        val fighter = combatants.firstOrNull { it.id == fighterId } ?: return
        val active = fighter.conditions.any { it.equals(condition, ignoreCase = true) }
        updateFighter(if (active) fighter.withoutCondition(condition) else fighter.withCondition(condition, rounds))
    }

    fun setHp(fighter: Combatant, delta: Int) {
        val at = combatants.indexOfFirst { it.id == fighter.id }
        if (at < 0) return
        val updated = combatants[at].withDamage(-delta)
        combatants[at] = updated
        persistCombat()
        CombatStore.syncToSheet(prefs, updated)
    }

    /** Passa a vez ao próximo que pode agir; quem está com Atordoado, Paralisado etc. perde o turno e isso vai para o histórico. */
    fun advanceTurn() {
        if (ordered.isEmpty()) return
        val step = nextTurn(ordered, activeIndex)
        val nextId = ordered[step.nextIndex].id
        // O turno de quem jogou e o de quem foi pulado terminam aqui: as condições com duração perdem uma rodada.
        val expiredLines = mutableListOf<RollLine>()
        for (id in listOf(ordered[activeIndex].id) + step.passed) {
            val fighter = combatants.firstOrNull { it.id == id } ?: continue
            val tick = fighter.tickConditions()
            tick.expired.forEach { expiredLines += RollLine("⏳ ${conditionEmoji(it)} $it de ${fighter.name} acabou.", true) }
            var after = tick.fighter
            // Personagem a 0 PV: a vez dele passou, então rola a salvaguarda contra a morte.
            if (id in step.passed && after.isDying()) {
                val death = rollDeathSave(engine, after)
                after = death.fighter
                expiredLines += death.line
            }
            if (after != fighter) updateFighter(after)
        }
        activeId = nextId
        persistCombat()
        if (step.newRound) { round += 1; CombatStore.saveRound(prefs, round) }
        step.skipped.forEach { log.push(RollLine("${conditionEmoji(it.condition)} ${it.name} está ${it.condition} e perde o turno.", null)) }
        expiredLines.forEach { log.push(it) }
        if (step.newRound) log.push(RollLine("— Rodada $round —", null))
        if (step.nobodyCanAct) log.push(RollLine("Ninguém consegue agir agora (todos derrotados ou incapacitados).", null))
        else log.push(RollLine("Vez de ${ordered[step.nextIndex].name}.", null))
    }

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
        listOf(MANUAL_SOURCE to "Manual (sem ficha)") +
            characters.map { (CHARACTER_PREFIX + it.id) to "Personagem • ${it.name.ifBlank { "Sem nome" }}" } +
            monsters.map { (MONSTER_PREFIX + it.id) to "Criatura • ${it.name.ifBlank { "Sem nome" }}" }
    val sourceLabel = when {
        sourceCharacter != null -> "Personagem • ${sourceCharacter.name.ifBlank { "Sem nome" }}"
        sourceMonster != null -> "Criatura • ${sourceMonster.name.ifBlank { "Sem nome" }}"
        else -> "Manual (sem ficha)"
    }

    Column(modifier.fillMaxSize()) {
        combatants.firstOrNull { it.id == editingConditionsId }?.let { f ->
            ConditionsDialog(f, { condition, rounds -> toggleCondition(f.id, condition, rounds) }, { editingConditionsId = "" })
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 18.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column {
                    Text("SISTEMA DE REGRAS", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Motor de RPG", color = Ink, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Role dados, execute testes e controle a iniciativa e os pontos de vida da batalha — ligado às suas fichas.", color = Muted, fontSize = 12.sp)
                }
            }

            item {
                EditorCard("FICHA ATIVA", "As rolagens abaixo usam os valores desta ficha ou criatura") {
                    PickButton("Fonte: $sourceLabel", sourceOptions.map { it.second }) { pickSource(sourceOptions[it].first) }
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
                        Text("Na fórmula, a sigla vale o modificador (ou o valor, se o atributo não tem modificador). Também existem PROF, NIVEL, CA, os recursos e o nome de cada perícia.",
                            color = Muted, fontSize = 12.sp)
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
                        TextInput("Valores manuais (NOME=valor, …)", manualVars, {
                            manualVars = it
                            prefs.edit().putString("engine_manual_vars", it).apply()
                        })
                        Text("Sem ficha, informe você mesmo os valores que a fórmula usa. Escolha uma ficha acima para ligá-los automaticamente.",
                            color = Muted, fontSize = 12.sp)
                    }
                }
            }

            item {
                EditorCard("ROLAGEM E TESTES",
                    if (profile.isPool) "Reserva de dados: cada d${profile.poolSides} com ${profile.poolTarget}+ é um sucesso"
                    else "Fórmulas executáveis: 1d20 + FOR + PROF, 2d6 + 3, (1d8 + FOR) * 2") {
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
                        NumberField("Classe de dificuldade (0 = sem CD)", difficulty, { difficulty = it.coerceAtLeast(0) }, Modifier.fillMaxWidth())
                        Button(onClick = {
                            log.push(rollFormula(engine, "Teste", formula, variables, difficulty.takeIf { it > 0 }))
                            prefs.edit().putString("engine_formula", formula).apply()
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                            Icon(Icons.Default.Casino, null); Spacer(Modifier.width(8.dp)); Text("Executar teste")
                        }
                    }
                    Text("Atalhos de rolagem", color = Muted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        profile.dice.forEach { die ->
                            OutlinedButton(onClick = { log.push(rollFormula(engine, "d$die", "d$die", emptyMap())) },
                                contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)) { Text("d$die", fontSize = 12.sp) }
                        }
                    }
                }
            }

            item {
                EditorCard("ORDEM DE INICIATIVA", "Adicione participantes (da ficha ativa ou à mão) e organize os turnos automaticamente") {
                    if (sourceCharacter != null) {
                        Button(onClick = {
                            if (combatants.any { it.sheetId == sourceCharacter.id }) {
                                log.push(RollLine("${sourceCharacter.name.ifBlank { "Personagem" }} já está no combate.", null))
                            } else {
                                val fighter = characterCombatant(engine, sourceCharacter, profile, library)
                                combatants.add(fighter); persistCombat()
                                log.push(RollLine("${fighter.name} entrou • iniciativa ${fighter.initiative} • PV ${fighter.currentHp}/${fighter.maxHp}", true))
                            }
                        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar ${sourceCharacter.name.ifBlank { "personagem" }}")
                        }
                    } else if (sourceMonster != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            NumberField("Quantidade", quantity, { quantity = it.coerceIn(1, 20) }, Modifier.weight(1f))
                            Button(onClick = {
                                val added = List(quantity) { i -> monsterCombatant(engine, sourceMonster, profile, if (quantity > 1) i + 1 else null) }
                                combatants.addAll(added); persistCombat()
                                log.push(RollLine("${added.size} × ${sourceMonster.name.ifBlank { "Criatura" }} entraram, cada uma com PV e iniciativa próprios.", true))
                            }, modifier = Modifier.weight(1.4f), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar")
                            }
                        }
                    }
                    Text("Ou adicione à mão:", color = Muted, fontSize = 12.sp)
                    TextInput("Nome do combatente", actorName, { actorName = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("Bônus iniciativa", actorInitiativeBonus, { actorInitiativeBonus = it }, Modifier.weight(1f))
                        NumberField("PV máx.", actorHp, { actorHp = it.coerceAtLeast(0) }, Modifier.weight(1f))
                        NumberField(defenseLabel, actorDefense, { actorDefense = it.coerceAtLeast(0) }, Modifier.weight(1f))
                    }
                    OutlinedButton(onClick = {
                        if (actorName.isNotBlank() && actorHp > 0) {
                            val base = if (profile.isPool) "1d${profile.poolSides}" else baseDie(profile)
                            val bonus = actorInitiativeBonus
                            val init = engine.roll(if (bonus >= 0) "$base + $bonus" else "$base - ${-bonus}").total
                            combatants.add(Combatant(java.util.UUID.randomUUID().toString(), actorName.trim(), init, actorHp, actorHp, actorDefense))
                            persistCombat(); actorName = ""
                        } else log.push(RollLine("Informe um nome e PV máximos maiores que zero.", null))
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar e rolar iniciativa")
                    }

                    if (ordered.isEmpty()) Text("Nenhum combatente na iniciativa.", color = Muted, fontSize = 12.sp)
                    else Text("${ordered.size} participante(s) na batalha — veja a arena logo abaixo.", color = Muted, fontSize = 12.sp)
                }
            }

            if (ordered.isNotEmpty()) {
                item {
                    BattleArena(
                        fighters = ordered, activeIndex = activeIndex,
                        characters = characters, monsters = monsters, library = library,
                        prefs = prefs, engine = engine, fallbackVariables = variables, fallbackProfile = profile,
                        defenseLabel = if (profile.usesArmorClass) "CA" else "defesa", log = log,
                        onDamage = { targetId, damage ->
                            val at = combatants.indexOfFirst { it.id == targetId }
                            if (at < 0) null else {
                                val updated = combatants[at].withDamage(damage)
                                combatants[at] = updated
                                persistCombat()
                                CombatStore.syncToSheet(prefs, updated)
                                updated
                            }
                        },
                        round = round,
                        onUpdateFighter = { updateFighter(it) },
                        onNextTurn = { advanceTurn() },
                        onEndCombat = { combatants.clear(); activeId = ""; round = 1; CombatStore.saveRound(prefs, 1); persistCombat() },
                        onOpenConditions = { editingConditionsId = it },
                        onAdjustHp = { fighter, delta -> setHp(fighter, delta) },
                        onRemove = { id -> combatants.removeAll { it.id == id }; persistCombat() }
                    )
                }
            }

            item {
                Card(colors = CardDefaults.cardColors(containerColor = Panel2)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null, tint = Accent)
                        Spacer(Modifier.width(10.dp))
                        Text("Beta 0.9.2: fichas, criaturas e magias alimentam o motor. Atributos viram variáveis (a sigla vale o modificador), " +
                            "as fórmulas e os dados vêm do sistema da ficha, sistemas de reserva de dados contam sucessos, e os PV dos personagens " +
                            "ficam sincronizados entre o combate e a ficha. As condições têm efeito: Atordoado, Paralisado e afins pulam o turno, e vantagem/desvantagem entram sozinhas nos ataques. Condições podem ter duração em rodadas e expiram sozinhas, e a salvaguarda do alvo pode aplicar uma condição se falhar. Este é um motor genérico, não uma implementação das regras oficiais de um jogo.",
                            color = Ink, fontSize = 12.sp)
                    }
                }
            }
        }
        RollLogBar(log) { log.clear() }
    }
}
