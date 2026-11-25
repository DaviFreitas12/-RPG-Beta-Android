package com.example.rpgsombrio.ui.combat

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.CombatStore
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.engine.ClientMsg
import com.example.rpgsombrio.engine.Combatant
import com.example.rpgsombrio.engine.CombatantSource
import com.example.rpgsombrio.engine.Encounter
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.buildSnapshot
import com.example.rpgsombrio.engine.characterCombatant
import com.example.rpgsombrio.engine.conditionEmoji
import com.example.rpgsombrio.engine.encode
import com.example.rpgsombrio.engine.helloToCombatant
import com.example.rpgsombrio.engine.isDying
import com.example.rpgsombrio.engine.monsterCombatant
import com.example.rpgsombrio.engine.nextTurn
import com.example.rpgsombrio.engine.rollDeathSave
import com.example.rpgsombrio.engine.tickConditions
import com.example.rpgsombrio.engine.withCondition
import com.example.rpgsombrio.engine.withDamage
import com.example.rpgsombrio.engine.withGroupInitiative
import com.example.rpgsombrio.engine.withoutCondition
import com.example.rpgsombrio.ui.components.push

/**
 * O combate do aparelho (0.9.17). Antes o estado vivia dentro da tela Motor, então a mesa em rede parava de funcionar
 * quando o mestre abria o Bestiário ou uma ficha. Agora o estado e as regras ficam aqui, e cada mudança grava no
 * aparelho e já é transmitida aos jogadores, com qualquer aba aberta.
 *
 * Tudo roda na thread principal.
 */
object CombatController {
    private var prefs: SharedPreferences? = null
    private var lastSaved: List<Combatant> = emptyList()
    private val undoStack = ArrayDeque<List<Combatant>>()

    val engine = RpgEngine()
    val combatants = mutableStateListOf<Combatant>()
    val log = mutableStateListOf<RollLine>()

    var activeId by mutableStateOf("")
        private set
    var round by mutableStateOf(1)
        private set
    var canUndo by mutableStateOf(false)
        private set

    /** Ordem de iniciativa: maior primeiro; empates por nome. */
    val ordered: List<Combatant>
        get() = combatants.sortedWith(
            compareByDescending<Combatant> { it.initiative }.thenBy { it.name.lowercase() }.thenBy { it.id }
        )

    // --- ciclo de vida -------------------------------------------------------------------------

    fun init(p: SharedPreferences) {
        if (prefs != null) return
        prefs = p
        reload()
    }

    /** Lê o combate do aparelho e traz os PV atuais das fichas. */
    fun reload() {
        val p = prefs ?: return
        val stored = CombatStore.load(p)
        combatants.clear()
        combatants.addAll(CombatStore.refreshFromSheets(p, stored))
        activeId = CombatStore.activeId(p)
        round = CombatStore.round(p)
        lastSaved = stored
        clearUndo()
    }

    /** Outras telas (Bestiário, fichas) gravam direto no aparelho: se mudou, relê antes de aplicar qualquer coisa. */
    private fun syncFromStore() {
        val p = prefs ?: return
        if (CombatStore.load(p) != lastSaved) reload()
    }

    private fun persist() {
        val p = prefs ?: return
        CombatStore.save(p, combatants.toList(), activeId)
        CombatStore.saveRound(p, round)
        lastSaved = CombatStore.load(p)
        publish()
    }

    /** Transmite o retrato da luta aos jogadores (só se a mesa estiver aberta). */
    fun publish() {
        if (TableSession.mode != "host") return
        val order = ordered
        val activeIndex = order.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
        TableSession.broadcast(buildSnapshot(order, order.getOrNull(activeIndex)?.id.orEmpty(), round).encode())
    }

    // --- desfazer ------------------------------------------------------------------------------

    private fun pushUndo() {
        undoStack.addLast(combatants.toList())
        while (undoStack.size > 30) undoStack.removeFirst()
        canUndo = true
    }

    private fun clearUndo() {
        undoStack.clear()
        canUndo = false
    }

    /** Desfaz o último dano, cura ou remoção. */
    fun undo() {
        val p = prefs ?: return
        val previous = undoStack.removeLastOrNull() ?: return
        combatants.clear()
        combatants.addAll(previous)
        persist()
        previous.forEach { CombatStore.syncToSheet(p, it) }
        canUndo = undoStack.isNotEmpty()
        log.push(RollLine("Desfeito.", null))
    }

    // --- PV, condições, iniciativa --------------------------------------------------------------

    /** Troca o combatente de mesmo id pelo [updated], grava o combate e leva PV e condições à ficha. */
    fun updateFighter(updated: Combatant) {
        val p = prefs ?: return
        val at = combatants.indexOfFirst { it.id == updated.id }
        if (at < 0) return
        combatants[at] = updated
        persist()
        CombatStore.syncToSheet(p, updated)
    }

    /** Marca (com [rounds] de duração, 0 = até remover) ou desmarca a condição. */
    fun toggleCondition(fighterId: String, condition: String, rounds: Int) {
        val fighter = combatants.firstOrNull { it.id == fighterId } ?: return
        val active = fighter.conditions.any { it.equals(condition, ignoreCase = true) }
        updateFighter(if (active) fighter.withoutCondition(condition) else fighter.withCondition(condition, rounds))
    }

    /** [delta] negativo é dano, positivo é cura. Cada ajuste entra no histórico e pode ser desfeito. */
    fun setHp(fighter: Combatant, delta: Int) {
        val p = prefs ?: return
        val at = combatants.indexOfFirst { it.id == fighter.id }
        if (at < 0 || delta == 0) return
        pushUndo()
        val before = combatants[at]
        val updated = before.withDamage(-delta)
        combatants[at] = updated
        persist()
        CombatStore.syncToSheet(p, updated)
        val change = if (delta < 0) "−${-delta}" else "+$delta"
        log.push(RollLine("${updated.name}: $change PV (${before.currentHp} → ${updated.currentHp})", delta > 0))
    }

    /** Iniciativa digitada na linha. Criaturas do mesmo tipo ainda sem iniciativa acompanham a primeira. */
    fun setInitiative(id: String, value: Int) {
        val updated = withGroupInitiative(combatants.toList(), id, value)
        updated.forEachIndexed { i, c -> if (combatants[i] != c) combatants[i] = c }
        persist()
    }

    // --- turnos --------------------------------------------------------------------------------

    /** Passa a vez ao próximo que pode agir; quem está com Atordoado, Paralisado etc. perde o turno e isso vai para o histórico. */
    fun advanceTurn() {
        val order = ordered
        if (order.isEmpty()) return
        val activeIndex = order.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
        val step = nextTurn(order, activeIndex)
        val nextId = order[step.nextIndex].id
        // O turno de quem jogou e o de quem foi pulado terminam aqui: as condições com duração perdem uma rodada.
        val expiredLines = mutableListOf<RollLine>()
        for (id in listOf(order[activeIndex].id) + step.passed) {
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
        if (step.newRound) round += 1
        clearUndo()
        persist()
        step.skipped.forEach { log.push(RollLine("${conditionEmoji(it.condition)} ${it.name} está ${it.condition} e perde o turno.", null)) }
        expiredLines.forEach { log.push(it) }
        if (step.newRound) log.push(RollLine("— Rodada $round —", null))
        if (step.nobodyCanAct) log.push(RollLine("Ninguém consegue agir agora (todos derrotados ou incapacitados).", null))
        else log.push(RollLine("Vez de ${order[step.nextIndex].name}.", null))
    }

    fun endCombat() {
        combatants.clear()
        activeId = ""
        round = 1
        clearUndo()
        persist()
    }

    fun remove(id: String) {
        pushUndo()
        combatants.removeAll { it.id == id }
        persist()
    }

    // --- entrada na luta (iniciativa sempre 0 = "INI ?": o dado é físico e o mestre digita o resultado) ----------

    private fun appendCharacters(characters: List<CharacterSheet>, library: List<ItemEntry>): Int {
        val p = prefs ?: return 0
        var added = 0
        for (sheet in characters) {
            if (combatants.any { it.sheetId == sheet.id }) continue
            val profile = SystemProfiles.resolve(p, sheet.systemId)
            combatants.add(characterCombatant(engine, sheet, profile, library).copy(initiative = 0))
            added++
        }
        return added
    }

    private fun appendMonsters(monster: MonsterSheet, quantity: Int): Int {
        val p = prefs ?: return 0
        val profile = SystemProfiles.resolve(p, monster.systemId)
        val existing = combatants.count { it.sheetKind == CombatantSource.MONSTER && it.sheetId == monster.id }
        val total = quantity.coerceIn(1, 50)
        repeat(total) { i ->
            val number = existing + i + 1
            combatants.add(monsterCombatant(engine, monster, profile, if (existing + total > 1) number else null).copy(initiative = 0))
        }
        return total
    }

    /** Põe todas as fichas de personagem na luta com iniciativa 0 ("INI ?"). */
    fun addAllCharacters(characters: List<CharacterSheet>, library: List<ItemEntry>) {
        val added = appendCharacters(characters, library)
        persist()
        log.push(RollLine(if (added == 0) "Todos os personagens já estão na luta." else "$added personagem(ns) entraram. Digite a iniciativa de cada um.", null))
    }

    fun addCharacter(sheet: CharacterSheet, library: List<ItemEntry>) {
        if (combatants.any { it.sheetId == sheet.id }) {
            log.push(RollLine("${sheet.name.ifBlank { "Personagem" }} já está no combate.", null))
            return
        }
        appendCharacters(listOf(sheet), library)
        persist()
        log.push(RollLine("${sheet.name.ifBlank { "Personagem" }} entrou. Digite a iniciativa.", true))
    }

    fun addMonsters(monster: MonsterSheet, quantity: Int) {
        val added = appendMonsters(monster, quantity)
        persist()
        log.push(RollLine("$added × ${monster.name.ifBlank { "Criatura" }} entraram. Digite a iniciativa de um: o grupo acompanha.", true))
    }

    fun addManual(name: String, hp: Int, defense: Int) {
        combatants.add(Combatant(java.util.UUID.randomUUID().toString(), name.trim(), 0, hp, hp, defense))
        persist()
    }

    /** O jogador entra na luta com a iniciativa que tirou no dado físico. */
    fun joinCombat(sheet: CharacterSheet, initiative: Int, library: List<ItemEntry>) {
        val p = prefs ?: return
        if (combatants.any { it.sheetId == sheet.id }) return
        val profile = SystemProfiles.resolve(p, sheet.systemId)
        combatants.add(characterCombatant(engine, sheet, profile, library).copy(initiative = initiative))
        persist()
    }

    /** Um toque: as criaturas do encontro e todos os personagens entram, sem iniciativa. */
    fun startEncounter(encounter: Encounter, monsters: List<MonsterSheet>, characters: List<CharacterSheet>, library: List<ItemEntry>) {
        var creatures = 0
        var missing = 0
        encounter.entries.forEach { entry ->
            val monster = monsters.firstOrNull { it.id == entry.monsterId }
            if (monster == null) missing++ else creatures += appendMonsters(monster, entry.quantity)
        }
        val heroes = appendCharacters(characters, library)
        persist()
        log.push(RollLine("${encounter.name}: $creatures criatura(s) e $heroes personagem(ns) na luta. Digite as iniciativas.", true))
        if (missing > 0) log.push(RollLine("$missing tipo(s) de criatura do encontro não existem mais no bestiário.", false))
    }

    // --- mesa em rede ---------------------------------------------------------------------------

    /** Aplica o que um jogador pediu pela rede. Só o mestre (que abriu a mesa) recebe essas mensagens. */
    fun handleRemote(playerId: String, msg: ClientMsg) {
        if (prefs == null) return
        syncFromStore()
        when (msg) {
            is ClientMsg.Hello -> {
                val sameId = combatants.firstOrNull { it.id == playerId }
                val bySheet = combatants.firstOrNull { it.sheetKind == CombatantSource.CHARACTER && it.sheetId == playerId }
                when {
                    sameId != null -> Unit
                    bySheet != null -> {
                        // O mestre já tinha colocado esta ficha na luta: o jogador assume essa linha, sem duplicar.
                        val at = combatants.indexOfFirst { it.id == bySheet.id }
                        combatants[at] = bySheet.copy(id = playerId, initiative = if (bySheet.initiative == 0) msg.initiative else bySheet.initiative)
                        if (activeId == bySheet.id) activeId = playerId
                        persist()
                        log.push(RollLine("${msg.name} conectou pela rede.", true))
                    }
                    else -> {
                        combatants.add(helloToCombatant(msg, playerId))
                        persist()
                        log.push(RollLine("${msg.name} entrou pela rede • iniciativa ${msg.initiative}", true))
                    }
                }
            }
            is ClientMsg.JoinCampaign -> Unit // tratada na rede (TableHost); não chega aqui
            is ClientMsg.Adjust -> combatants.firstOrNull { it.id == playerId }?.let { setHp(it, msg.delta) }
            is ClientMsg.SetInitiative -> combatants.firstOrNull { it.id == playerId }?.let { setInitiative(it.id, msg.value) }
            is ClientMsg.EndTurn -> {
                val order = ordered
                val activeIndex = order.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
                if (order.getOrNull(activeIndex)?.id == playerId) advanceTurn()
            }
        }
    }
}
