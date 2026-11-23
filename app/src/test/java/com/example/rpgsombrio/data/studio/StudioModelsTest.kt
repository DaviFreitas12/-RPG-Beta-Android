package com.example.rpgsombrio.data.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioModelsTest {
    @Test fun compatibleLoadoutHasNoIssues() {
        val character = StudioCharacter(
            id = "char-1", name = "Viajante",
            appearance = StudioAppearance(bodyType = StudioBodyType.NEUTRAL),
            loadout = StudioLoadout(mapOf(StudioSlot.TORSO to "tunic-1"))
        )
        val catalog = listOf(StudioAsset(
            id = "tunic-1", name = "Túnica de couro", slot = StudioSlot.TORSO,
            resourcePath = "studio/torso/tunic_01.png"
        ))
        assertTrue(validateStudioLoadout(character, catalog).isEmpty())
    }

    @Test fun missingAssetIsReported() {
        val character = StudioCharacter(
            id = "char-1", name = "Viajante",
            loadout = StudioLoadout(mapOf(StudioSlot.MAIN_HAND to "missing"))
        )
        val issues = validateStudioLoadout(character, emptyList())
        assertEquals("missing_asset", issues.single().code)
    }

    @Test fun incompatibleBodyIsReportedWithoutMutatingAsset() {
        val asset = StudioAsset(
            id = "armor-1", name = "Armadura pesada", slot = StudioSlot.TORSO,
            resourcePath = "studio/armor/heavy_01.png",
            bodyTypes = setOf(StudioBodyType.MASCULINE)
        )
        val character = StudioCharacter(
            id = "char-1", name = "Viajante",
            appearance = StudioAppearance(bodyType = StudioBodyType.FEMININE),
            loadout = StudioLoadout(mapOf(StudioSlot.TORSO to asset.id))
        )
        assertEquals("body_incompatible", validateStudioLoadout(character, listOf(asset)).single().code)
        assertEquals("studio/armor/heavy_01.png", asset.resourcePath)
    }

    @Test fun twoHandWeaponsConflictWithOffhandEquipment() {
        val character = StudioCharacter(
            id = "char-1", name = "Viajante",
            loadout = StudioLoadout(mapOf(
                StudioSlot.MAIN_HAND to "greatsword",
                StudioSlot.OFF_HAND to "shield"
            ))
        )
        val catalog = listOf(
            StudioAsset("greatsword", "Espadão", StudioSlot.MAIN_HAND, "studio/weapons/greatsword.png", handsRequired = 2),
            StudioAsset("shield", "Escudo", StudioSlot.OFF_HAND, "studio/shields/shield.png", handsRequired = 1)
        )
        assertTrue(validateStudioLoadout(character, catalog).any { it.code == "hands_conflict" })
    }
}
