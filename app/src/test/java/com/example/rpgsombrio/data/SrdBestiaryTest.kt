package com.example.rpgsombrio.data

import com.example.rpgsombrio.engine.DEFAULT_MONSTER_TYPES
import com.example.rpgsombrio.engine.monsterAttacks
import com.example.rpgsombrio.engine.typeKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SrdBestiaryTest {
    private val all = srdMonsters()

    @Test fun idsAreUniqueAndPrefixed() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertTrue(all.all { it.id.startsWith("srd-") })
    }

    @Test fun everyTypeIsADefaultCategory() {
        val keys = DEFAULT_MONSTER_TYPES.map { typeKey(it) }.toSet()
        assertTrue(all.filter { typeKey(it.type) !in keys }.map { it.name }.toString(), all.all { typeKey(it.type) in keys })
    }

    @Test fun importingTwiceNeverDuplicates() {
        val first = missingSrdMonsters(emptyList())
        assertEquals(all.size, first.size)
        assertTrue(missingSrdMonsters(first).isEmpty())
        assertEquals(all.size - 1, missingSrdMonsters(listOf(all.first())).size)
    }

    @Test fun attackActionsAreRollable() {
        val goblin = all.first { it.id == "srd-goblin" }
        assertEquals(2, monsterAttacks(goblin).size)
        assertEquals("1d6+2 cortante", monsterAttacks(goblin).first().damage)
        assertTrue(all.first { it.name == "Bugbear" }.entries.isNotEmpty())
    }
}
