package com.example.rpgsombrio.ui.avatar

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.studio.StudioAsset
import com.example.rpgsombrio.data.studio.StudioCharacter
import com.example.rpgsombrio.data.studio.StudioSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Prévia composta a partir de PNGs transparentes empacotados em app/src/main/assets.
 * Contrato dos assets: todas as camadas de um mesmo conjunto corporal usam o mesmo
 * canvas e o mesmo enquadramento. O renderer nunca desloca, deforma ou recolore a arte.
 */
@Composable
internal fun StudioCharacterPreview(
    character: StudioCharacter,
    catalog: Collection<StudioAsset>,
    modifier: Modifier = Modifier,
    background: Color = Color(0xFF211F24)
) {
    val context = LocalContext.current
    val selectedLayers = character.loadout.selectedAssetIds.mapNotNull { (slot, assetId) ->
        catalog.firstOrNull { it.id == assetId && it.slot == slot }
    }.sortedWith(compareBy<StudioAsset> {
        if (it.zIndex == 0) defaultStudioLayerOrder(it.slot) else it.zIndex
    }.thenBy { it.slot.ordinal })

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
            selectedLayers.forEach { asset ->
                val bitmapState = produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                    initialValue = null,
                    key1 = asset.resourcePath
                ) {
                    value = withContext(Dispatchers.IO) {
                        runCatching {
                            context.assets.open(asset.resourcePath).use { stream ->
                                BitmapFactory.decodeStream(stream)?.asImageBitmap()
                            }
                        }.getOrNull()
                    }
                }
                bitmapState.value?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = asset.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }
            if (selectedLayers.isEmpty()) {
                Text(
                    text = "ARTES DO PERSONAGEM\nAGUARDANDO CATÁLOGO",
                    color = Color(0xFFB8AFA5),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
    }
}

/** Ordem de desenho padrão para assets que não precisarem de zIndex específico. */
internal fun defaultStudioLayerOrder(slot: StudioSlot): Int = when (slot) {
    StudioSlot.BODY -> 0
    StudioSlot.BACK -> 10
    StudioSlot.LEGS -> 20
    StudioSlot.TORSO -> 30
    StudioSlot.ARMS -> 40
    StudioSlot.HANDS -> 50
    StudioSlot.NECK -> 60
    StudioSlot.FACE -> 70
    StudioSlot.EARS -> 75
    StudioSlot.FACIAL_HAIR -> 90
    StudioSlot.HAIR -> 100
    StudioSlot.HEAD -> 110
    StudioSlot.WAIST -> 120
    StudioSlot.MAIN_HAND -> 130
    StudioSlot.OFF_HAND -> 140
    StudioSlot.FINGERS -> 150
    StudioSlot.AURA -> 160
    StudioSlot.EFFECT -> 170
}
