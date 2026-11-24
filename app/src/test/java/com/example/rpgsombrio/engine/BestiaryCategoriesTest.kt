package com.example.rpgsombrio.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BestiaryCategoriesTest {
    @Test fun defaultsAlwaysAppearEvenWhenEmpty() {
        val names = monsterCategories(emptyList(), emptyList())
        assertEquals(DEFAULT_MONSTER_TYPES.size, names.size)
        assertTrue(names.all { it.count == 0 && it.isDefault && !it.isCustom })
        assertTrue(names.any { it.name == "Construto" } && names.any { it.name == "Aberração" } && names.any { it.name == "Fera" })
    }

    @Test fun caseAndSpacesDoNotCreateDuplicateCategories() {
        val result = monsterCategories(emptyList(), listOf("fera", " Fera ", "FERA", "Construto"))
        assertEquals(DEFAULT_MONSTER_TYPES.size, result.size)
        assertEquals(3, result.first { it.name == "Fera" }.count)
        assertEquals(1, result.first { it.name == "Construto" }.count)
    }

    @Test fun customAndUnknownTypesBecomeCategories() {
        val result = monsterCategories(listOf("Demônio"), listOf("Demônio", "Aparição"))
        val demon = result.first { it.name == "Demônio" }
        assertTrue(demon.isCustom); assertFalse(demon.isDefault); assertEquals(1, demon.count)
        val ghost = result.first { it.name == "Aparição" }
        assertFalse(ghost.isCustom); assertFalse(ghost.isDefault); assertEquals(1, ghost.count)
    }

    @Test fun customWithDefaultNameStaysDefault() {
        val result = monsterCategories(listOf("fera"), emptyList())
        assertEquals(DEFAULT_MONSTER_TYPES.size, result.size)
        assertFalse(result.first { it.name == "Fera" }.isCustom)
    }

    @Test fun uncategorizedAndMembership() {
        assertEquals(2, uncategorizedCount(listOf("", "  ", "Fera")))
        assertTrue(inCategory("fera ", "Fera"))
        assertFalse(inCategory("Construto", "Fera"))
        assertTrue(inCategory("", NO_CATEGORY))
        assertFalse(inCategory("Fera", NO_CATEGORY))
    }
}
