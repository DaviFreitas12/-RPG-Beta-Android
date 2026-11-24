package com.example.rpgsombrio.data

import android.content.SharedPreferences

/** Categorias de criaturas que o mestre criou (as padrão não precisam ser gravadas). Uma por linha. */
object BestiaryCategoryStore {
    private const val KEY = "bestiary_categories"

    fun load(prefs: SharedPreferences): List<String> =
        (prefs.getString(KEY, "") ?: "").lines().map { it.trim() }.filter { it.isNotEmpty() }

    fun save(prefs: SharedPreferences, list: List<String>) {
        prefs.edit().putString(KEY, list.map { it.replace('\n', ' ').trim() }.filter { it.isNotEmpty() }.joinToString("\n")).apply()
    }
}
