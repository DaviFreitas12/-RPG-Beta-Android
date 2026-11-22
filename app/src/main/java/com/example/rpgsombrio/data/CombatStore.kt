package com.example.rpgsombrio.data

import android.content.SharedPreferences
import com.example.rpgsombrio.engine.Combatant
import com.example.rpgsombrio.engine.CombatantSource
import com.example.rpgsombrio.engine.primaryResource
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lista de combate guardada no aparelho. É compartilhada pela tela Motor e pelas fichas
 * ("Enviar ao combate"), e mantém os PV dos personagens em sincronia com a ficha.
 */
object CombatStore {
    private const val LIST = "engine_combatants"
    private const val ACTIVE = "engine_active_id"
    private const val ROUND = "engine_round"

    fun load(prefs: SharedPreferences): List<Combatant> {
        val raw = prefs.getString(LIST, null)
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            List(array.length()) { i ->
                val o = array.getJSONObject(i)
                Combatant(
                    id = o.getString("id"), name = o.getString("name"), initiative = o.getInt("initiative"),
                    currentHp = o.getInt("currentHp"), maxHp = o.getInt("maxHp"), armorClass = o.optInt("armorClass", 10),
                    conditions = o.optJSONArray("conditions").let { c -> if (c == null) emptyList() else List(c.length()) { c.getString(it) } },
                    sheetId = o.optString("sheetId"), sheetKind = o.optString("sheetKind"),
                    defenses = jsonToDefenses(o.optJSONArray("defenses")),
                    conditionImmunities = o.optString("conditionImmunities"),
                    conditionTimers = timersFromJson(o.optJSONObject("conditionTimers")),
                    tempHp = o.optInt("tempHp", 0), deathSuccesses = o.optInt("deathSuccesses", 0),
                    deathFailures = o.optInt("deathFailures", 0)
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    fun activeId(prefs: SharedPreferences): String = prefs.getString(ACTIVE, "") ?: ""

    /** Número da rodada atual (começa em 1). */
    fun round(prefs: SharedPreferences): Int = prefs.getInt(ROUND, 1).coerceAtLeast(1)

    fun saveRound(prefs: SharedPreferences, round: Int) {
        prefs.edit().putInt(ROUND, round.coerceAtLeast(1)).apply()
    }

    private fun timersFromJson(o: JSONObject?): Map<String, Int> {
        if (o == null) return emptyMap()
        val result = LinkedHashMap<String, Int>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val rounds = o.optInt(key, 0)
            if (rounds > 0) result[key] = rounds
        }
        return result
    }

    private fun timersToJson(timers: Map<String, Int>): JSONObject {
        val o = JSONObject()
        timers.forEach { (name, rounds) -> if (rounds > 0) o.put(name, rounds) }
        return o
    }

    fun save(prefs: SharedPreferences, list: List<Combatant>, activeId: String) {
        val array = JSONArray()
        list.forEach { c ->
            array.put(
                JSONObject().put("id", c.id).put("name", c.name).put("initiative", c.initiative)
                    .put("currentHp", c.currentHp).put("maxHp", c.maxHp).put("armorClass", c.armorClass)
                    .put("conditions", JSONArray(c.conditions))
                    .put("sheetId", c.sheetId).put("sheetKind", c.sheetKind)
                    .put("defenses", defensesToJson(c.defenses)).put("conditionImmunities", c.conditionImmunities)
                    .put("conditionTimers", timersToJson(c.conditionTimers))
                    .put("tempHp", c.tempHp).put("deathSuccesses", c.deathSuccesses).put("deathFailures", c.deathFailures)
            )
        }
        prefs.edit().putString(LIST, array.toString()).putString(ACTIVE, activeId).apply()
    }

    /** Acrescenta combatentes à lista existente (usado pelas fichas, sem abrir o Motor). */
    fun add(prefs: SharedPreferences, vararg added: Combatant) {
        save(prefs, load(prefs) + added, activeId(prefs))
    }

    /**
     * Ao abrir o Motor, os PV dos personagens vêm da ficha (que pode ter sido editada depois de entrar no combate).
     * Criaturas do bestiário são cópias independentes e não são alteradas.
     */
    fun refreshFromSheets(prefs: SharedPreferences, list: List<Combatant>): List<Combatant> {
        if (list.none { it.sheetKind == CombatantSource.CHARACTER }) return list
        val sheets = SheetStore.loadCharacters(prefs).associateBy { it.id }
        return list.map { c ->
            val sheet = sheets[c.sheetId]
            if (c.sheetKind != CombatantSource.CHARACTER || sheet == null) return@map c
            // Condições e defesas também vêm da ficha, que é a fonte da verdade do personagem.
            // A duração (rodadas) só existe no combate: some junto com a condição se ela foi tirada na ficha,
            // e condições marcadas na ficha não têm duração.
            val synced = c.copy(
                conditions = sheet.conditions, defenses = sheet.defenses, conditionImmunities = sheet.conditionImmunities,
                conditionTimers = c.conditionTimers.filterKeys { it in sheet.conditions },
                tempHp = sheet.tempHp, deathSuccesses = sheet.deathSuccesses, deathFailures = sheet.deathFailures
            )
            val profile = SystemProfiles.resolve(prefs, sheet.systemId)
            val resource = primaryResource(profile) ?: return@map synced
            val value = sheet.resources[resource.key] ?: return@map synced
            if (value.max <= 0) synced else synced.copy(currentHp = value.current.coerceIn(0, value.max), maxHp = value.max)
        }
    }

    /** Grava na ficha os PV atuais do personagem em combate (recurso principal do sistema). */
    fun syncToSheet(prefs: SharedPreferences, combatant: Combatant) {
        if (combatant.sheetKind != CombatantSource.CHARACTER || combatant.sheetId.isBlank()) return
        val sheets = SheetStore.loadCharacters(prefs)
        val index = sheets.indexOfFirst { it.id == combatant.sheetId }
        if (index < 0) return
        val sheet: CharacterSheet = sheets[index]
        val resource = primaryResource(SystemProfiles.resolve(prefs, sheet.systemId)) ?: return
        val old = sheet.resources[resource.key] ?: ResourceValue(resource.defaultMax, resource.defaultMax)
        val updated = sheet.copy(
            resources = sheet.resources + (resource.key to old.copy(current = combatant.currentHp)),
            conditions = combatant.conditions,
            tempHp = combatant.tempHp, deathSuccesses = combatant.deathSuccesses, deathFailures = combatant.deathFailures
        )
        SheetStore.saveCharacters(prefs, sheets.toMutableList().also { it[index] = updated })
    }
}
