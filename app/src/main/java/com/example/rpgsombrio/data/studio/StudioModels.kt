package com.example.rpgsombrio.data.studio

/**
 * Modelos de domínio do Character Studio.
 * A arte é composta por PNGs ilustrados em camadas, com transparência e coordenadas normalizadas.
 * Este pacote não depende de UI/Android para permitir testes do catálogo e da compatibilidade.
 */

enum class StudioBodyType { MASCULINE, FEMININE, NEUTRAL }

enum class StudioSlot {
    BODY, FACE, HAIR, FACIAL_HAIR, HEAD, TORSO, ARMS, LEGS, HANDS,
    BACK, WAIST, MAIN_HAND, OFF_HAND, NECK, EARS, FINGERS, AURA, EFFECT
}

enum class StudioRarity {
    COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, ANCIENT, DIVINE, CURSED, ARTIFACT
}

enum class StudioStyle {
    MEDIEVAL, DARK_FANTASY, ORIENTAL, TRIBAL, ARCANE, NOBLE
}

/** Âncora expressa em coordenadas normalizadas (0..1) do quadro comum do personagem. */
data class StudioAnchor(
    val name: String,
    val x: Float,
    val y: Float,
    val rotationDegrees: Float = 0f
) {
    init {
        require(name.isNotBlank())
        require(x in 0f..1f && y in 0f..1f)
        require(rotationDegrees.isFinite())
    }
}

/** Metadados de uma peça ilustrada. resourcePath aponta para um PNG transparente empacotado no app. */
data class StudioAsset(
    val id: String,
    val name: String,
    val slot: StudioSlot,
    val resourcePath: String,
    val bodyTypes: Set<StudioBodyType> = StudioBodyType.values().toSet(),
    val styles: Set<StudioStyle> = setOf(StudioStyle.DARK_FANTASY),
    val rarity: StudioRarity = StudioRarity.COMMON,
    val anchor: StudioAnchor? = null,
    val zIndex: Int = 0,
    val tags: Set<String> = emptySet(),
    /** 0 = não ocupa mão; 1 = uma mão; 2 = duas mãos. */
    val handsRequired: Int = 0,
    val isStarterAsset: Boolean = false
) {
    init {
        require(id.isNotBlank() && name.isNotBlank() && resourcePath.isNotBlank())
        require(handsRequired in 0..2)
    }
}

/** Aparência editável. A seleção guarda IDs, nunca cópias destrutivas dos PNGs originais. */
data class StudioAppearance(
    val bodyType: StudioBodyType = StudioBodyType.NEUTRAL,
    val skinToneId: String = "skin_warm_01",
    val faceShapeId: String = "face_default",
    val eyeStyleId: String = "eyes_default",
    val eyeColorId: String = "eyes_brown",
    val expressionId: String = "expression_neutral",
    val hairStyleId: String = "hair_short_01",
    val hairColorId: String = "hair_black",
    val facialHairId: String? = null,
    val marks: List<String> = emptyList(),
    val proportions: Map<String, Float> = emptyMap()
) {
    init { require(proportions.values.all { it.isFinite() && it in 0f..1f }) }
}

data class StudioLoadout(
    val selectedAssetIds: Map<StudioSlot, String> = emptyMap(),
    val paletteId: String = "palette_default",
    val effectIds: Set<String> = emptySet()
)

data class StudioCharacter(
    val id: String,
    val name: String,
    val archetypeId: String? = null,
    val appearance: StudioAppearance = StudioAppearance(),
    val loadout: StudioLoadout = StudioLoadout(),
    val previewBackgroundId: String = "background_ruins",
    val createdAtEpochMillis: Long = 0L,
    val updatedAtEpochMillis: Long = 0L
) {
    init { require(id.isNotBlank() && name.isNotBlank()) }
}

/** Projeto editável é diferente de personagem finalizado e utilizável na ficha. */
data class StudioProject(
    val id: String,
    val name: String,
    val character: StudioCharacter,
    val schemaVersion: Int = 1,
    val isFinalized: Boolean = false,
    val updatedAtEpochMillis: Long = 0L
) {
    init { require(id.isNotBlank() && name.isNotBlank() && schemaVersion > 0) }
}

data class LoadoutIssue(val code: String, val message: String, val affectedSlots: Set<StudioSlot>)

/** Valida compatibilidade sem deformar, deslocar ou modificar a arte original. */
fun validateStudioLoadout(
    character: StudioCharacter,
    catalog: Collection<StudioAsset>
): List<LoadoutIssue> {
    val byId = catalog.associateBy { it.id }
    val chosen = character.loadout.selectedAssetIds.mapNotNull { (slot, id) ->
        byId[id]?.let { slot to it }
    }
    val issues = mutableListOf<LoadoutIssue>()

    character.loadout.selectedAssetIds.forEach { (slot, id) ->
        val asset = byId[id]
        if (asset == null) {
            issues += LoadoutIssue("missing_asset", "Uma peça selecionada não está disponível na biblioteca.", setOf(slot))
        } else {
            if (asset.slot != slot) {
                issues += LoadoutIssue("slot_mismatch", "${asset.name} não pertence à categoria ${slot.name.lowercase()}.", setOf(slot, asset.slot))
            }
            if (character.appearance.bodyType !in asset.bodyTypes) {
                issues += LoadoutIssue("body_incompatible", "${asset.name} não possui uma versão compatível com esta base corporal.", setOf(slot))
            }
        }
    }

    val handUse = chosen.sumOf { (_, asset) -> asset.handsRequired }
    if (handUse > 2) {
        issues += LoadoutIssue(
            "hands_conflict",
            "As peças selecionadas exigem mais mãos do que o personagem possui. Remova ou substitua uma arma.",
            setOf(StudioSlot.MAIN_HAND, StudioSlot.OFF_HAND)
        )
    }
    return issues.distinctBy { it.code to it.affectedSlots }
}
