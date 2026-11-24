package com.example.rpgsombrio.data

import android.content.SharedPreferences
import com.example.rpgsombrio.engine.Encounter
import com.example.rpgsombrio.engine.decodeEncounters
import com.example.rpgsombrio.engine.encodeEncounters

/** Encontros preparados pelo mestre, guardados no aparelho. */
object EncounterStore {
    private const val KEY = "encounters"

    fun load(prefs: SharedPreferences): List<Encounter> = decodeEncounters(prefs.getString(KEY, "") ?: "")

    fun save(prefs: SharedPreferences, list: List<Encounter>) {
        prefs.edit().putString(KEY, encodeEncounters(list)).apply()
    }
}
