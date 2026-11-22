package com.example.rpgsombrio.ui.avatar

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CharacterLook
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.ui.components.CollapsibleSection
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Muted
import kotlin.random.Random

private val RACE_NAMES = listOf("Humano", "Elfo", "Anão", "Orc", "Tiefling", "Halfling")
private val BODY_NAMES = listOf("Padrão", "Magro", "Forte")
private val FACE_NAMES = listOf("Normal", "Cheio", "Anguloso")
private val HAIR_NAMES = listOf("Careca", "Curto", "Longo", "Coque", "Moicano", "Rabo", "Trança", "Espetado")
private val EYE_STYLE_NAMES = listOf("Normal", "Estreito", "Grande")
private val MOUTH_NAMES = listOf("Neutra", "Sorriso", "Séria", "Presas")
private val BEARD_NAMES = listOf("Nenhuma", "Rala", "Cheia", "Bigode", "Longa", "Cavanhaque")
private val ACCESSORY_NAMES = listOf("Nenhum", "Cicatriz", "Tapa-olho", "Pintura", "Óculos", "Brinco")
private val OUTFIT_NAMES = listOf("Túnica", "Armadura", "Vestes", "Couro")
private val CAPE_NAMES = listOf("Sem capa", "Capa curta", "Capa longa")
private val HEADGEAR_NAMES = listOf("Nenhum", "Capuz", "Elmo", "Chapéu de mago", "Coroa")
private val WEAPON_NAMES = listOf("Nenhuma", "Espada", "Cajado", "Arco", "Machado", "Espada e escudo")

private fun <T> List<T>.at(i: Int): T = this[i.coerceIn(0, lastIndex)]

// --- avatar em pixel art -----------------------------------------------------------------------

/** Converte o visual em bitmap. Corpo inteiro, ou só a parte de cima (cabeça e tronco) para cartões pequenos. */
private fun spriteBitmap(look: CharacterLook, full: Boolean): Bitmap {
    val px = buildSprite(look)
    if (full) return Bitmap.createBitmap(px, SPRITE_W, SPRITE_H, Bitmap.Config.ARGB_8888)
    var first = 0
    while (first < SPRITE_H && (0 until SPRITE_W).none { (px[first * SPRITE_W + it] ushr 24) != 0 }) first++
    val startY = (first - 1).coerceIn(0, SPRITE_H - 1)
    val rows = minOf(SPRITE_W, SPRITE_H - startY)
    return Bitmap.createBitmap(px, startY * SPRITE_W, SPRITE_W, SPRITE_W, rows, Bitmap.Config.ARGB_8888)
}

/** Avatar do personagem. [full] mostra o corpo inteiro (editor); sem ele, um quadrado com cabeça e tronco. */
@Composable
internal fun AvatarView(look: CharacterLook, size: Dp = 84.dp, modifier: Modifier = Modifier, full: Boolean = false) {
    val key = look.toAvatarKey()
    val bitmap = remember(key, full) { spriteBitmap(look, full).asImageBitmap() }
    val box = if (full) modifier.size(width = size, height = size * (SPRITE_H / SPRITE_W.toFloat())) else modifier.size(size)
    Box(
        box.clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A3046), Color(0xFF151824))))
    ) {
        Image(
            bitmap = bitmap, contentDescription = "Avatar do personagem",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit, filterQuality = FilterQuality.None
        )
    }
}

// --- seção da ficha ----------------------------------------------------------------------------

@Composable
private fun SwatchRow(label: String, colors: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEachIndexed { i, argb ->
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(Color(argb))
                        .border(if (i == selected) 3.dp else 1.dp, if (i == selected) Accent else Hairline, CircleShape)
                        .clickable { onSelect(i) }
                )
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, names: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            names.forEachIndexed { i, name ->
                FilterChip(selected = i == selected, onClick = { onSelect(i) }, label = { Text(name) })
            }
        }
    }
}

/** Orcs e tieflings têm pele própria; ao sair deles, a pele volta para um tom comum. */
private fun skinForRace(race: Int, current: Int): Int = when (race) {
    3 -> 5
    4 -> 7
    else -> if (current in 5..9 && (race != 1 || current != 6)) 1 else current
}

/** Texto de aparência montado a partir das escolhas do avatar e dos campos de descrição. */
private fun lookSummary(look: CharacterLook): String {
    val colorNames = listOf("preto", "castanho", "ruivo", "loiro", "grisalho", "branco", "roxo", "verde", "vermelho", "azul")
    val eyeNames = listOf("castanhos", "azuis", "verdes", "cinzentos", "âmbar", "vermelhos", "violeta")
    val hairText = listOf("careca", "cabelo curto", "cabelo longo", "cabelo preso em coque", "moicano", "rabo de cavalo", "trança", "cabelo espetado")
    val beardText = listOf("", "barba rala", "barba cheia", "bigode", "barba longa", "cavanhaque")
    val parts = mutableListOf<String>()
    parts += RACE_NAMES.at(look.race)
    if (look.age.isNotBlank()) parts += if (look.age.all { it.isDigit() }) "${look.age} anos" else look.age.trim()
    if (look.height.isNotBlank()) parts += look.height.trim()
    if (look.build.isNotBlank()) parts += look.build.trim()
    val hair = hairText.at(look.hairStyle)
    parts += if (look.hairStyle == 0) hair else "$hair ${colorNames.at(look.hairColor)}"
    parts += "olhos ${eyeNames.at(look.eyeColor)}"
    beardText.at(look.beard).takeIf { it.isNotBlank() }?.let { parts += it }
    if (look.accessory > 0) parts += ACCESSORY_NAMES.at(look.accessory).lowercase()
    if (look.marks.isNotBlank()) parts += look.marks.trim()
    return parts.joinToString(", ") + "."
}

@Composable
internal fun AppearanceSection(sheet: CharacterSheet, onChange: (CharacterSheet) -> Unit) {
    val look = sheet.look
    fun update(next: CharacterLook) = onChange(sheet.copy(look = next.copy(configured = true)))
    CollapsibleSection(
        "APARÊNCIA",
        summary = if (look.configured) listOf(RACE_NAMES.at(look.race), look.age, look.height).filter { it.isNotBlank() }.joinToString(" • ") else "Crie o visual do personagem",
        startExpanded = false
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarView(look, 120.dp, full = true)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = {
                    val r = Random
                    val race = r.nextInt(RACE_NAMES.size)
                    update(look.copy(
                        race = race, skin = if (race == 3 || race == 4) skinForRace(race, 1) else r.nextInt(5),
                        face = r.nextInt(FACE_NAMES.size), body = r.nextInt(BODY_NAMES.size),
                        hairStyle = r.nextInt(HAIR_NAMES.size), hairColor = r.nextInt(HAIR_PALETTE.size),
                        eyeColor = r.nextInt(EYE_PALETTE.size), eyeStyle = r.nextInt(EYE_STYLE_NAMES.size),
                        mouth = r.nextInt(MOUTH_NAMES.size), beard = r.nextInt(BEARD_NAMES.size),
                        accessory = r.nextInt(ACCESSORY_NAMES.size), outfit = r.nextInt(OUTFIT_NAMES.size),
                        outfitColor = r.nextInt(CLOTH_PALETTE.size), cape = r.nextInt(CAPE_NAMES.size),
                        headgear = r.nextInt(HEADGEAR_NAMES.size), weapon = r.nextInt(WEAPON_NAMES.size)
                    ))
                }) { Text("Sortear") }
                Text("O retrato da galeria, se existir, aparece no lugar do avatar.", color = Muted, fontSize = 11.sp)
            }
        }
        ChoiceRow("Raça", RACE_NAMES, look.race) { update(look.copy(race = it, skin = skinForRace(it, look.skin))) }
        ChoiceRow("Corpo", BODY_NAMES, look.body) { update(look.copy(body = it)) }
        SwatchRow("Pele", SKIN_PALETTE, look.skin) { update(look.copy(skin = it)) }
        ChoiceRow("Rosto", FACE_NAMES, look.face) { update(look.copy(face = it)) }
        ChoiceRow("Cabelo", HAIR_NAMES, look.hairStyle) { update(look.copy(hairStyle = it)) }
        SwatchRow("Cor do cabelo", HAIR_PALETTE, look.hairColor) { update(look.copy(hairColor = it)) }
        ChoiceRow("Olhos", EYE_STYLE_NAMES, look.eyeStyle) { update(look.copy(eyeStyle = it)) }
        SwatchRow("Cor dos olhos", EYE_PALETTE, look.eyeColor) { update(look.copy(eyeColor = it)) }
        ChoiceRow("Boca", MOUTH_NAMES, look.mouth) { update(look.copy(mouth = it)) }
        ChoiceRow("Barba", BEARD_NAMES, look.beard) { update(look.copy(beard = it)) }
        ChoiceRow("Detalhe do rosto", ACCESSORY_NAMES, look.accessory) { update(look.copy(accessory = it)) }
        ChoiceRow("Roupa", OUTFIT_NAMES, look.outfit) { update(look.copy(outfit = it)) }
        SwatchRow("Cor da roupa", CLOTH_PALETTE, look.outfitColor) { update(look.copy(outfitColor = it)) }
        ChoiceRow("Capa", CAPE_NAMES, look.cape) { update(look.copy(cape = it)) }
        ChoiceRow("Na cabeça", HEADGEAR_NAMES, look.headgear) { update(look.copy(headgear = it)) }
        ChoiceRow("Arma", WEAPON_NAMES, look.weapon) { update(look.copy(weapon = it)) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput("Idade", look.age, { update(look.copy(age = it)) }, Modifier.weight(1f))
            TextInput("Altura", look.height, { update(look.copy(height = it)) }, Modifier.weight(1f))
        }
        TextInput("Porte (magro, forte…)", look.build, { update(look.copy(build = it)) })
        TextInput("Marcas e detalhes", look.marks, { update(look.copy(marks = it)) }, singleLine = false, minLines = 2)
        OutlinedButton(
            onClick = { onChange(sheet.copy(appearance = lookSummary(look), look = look.copy(configured = true))) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Gerar descrição em \"Aparência\" (História)") }
    }
}

// --- avatar na arena: o retrato chega como texto, então o visual viaja como "look:..." ----------

internal const val AVATAR_KEY_PREFIX = "look:"
private const val AVATAR_KEY_FIELDS = 16

internal fun CharacterLook.toAvatarKey(): String =
    AVATAR_KEY_PREFIX + listOf(race, skin, face, body, hairStyle, hairColor, eyeColor, eyeStyle, mouth, beard, accessory, outfit, outfitColor, cape, headgear, weapon)
        .joinToString(",")

internal fun parseAvatarKey(key: String): CharacterLook? {
    if (!key.startsWith(AVATAR_KEY_PREFIX)) return null
    val n = key.removePrefix(AVATAR_KEY_PREFIX).split(",").mapNotNull { it.toIntOrNull() }
    if (n.size < AVATAR_KEY_FIELDS) return null
    return CharacterLook(
        configured = true, race = n[0], skin = n[1], face = n[2], body = n[3], hairStyle = n[4], hairColor = n[5],
        eyeColor = n[6], eyeStyle = n[7], mouth = n[8], beard = n[9], accessory = n[10], outfit = n[11],
        outfitColor = n[12], cape = n[13], headgear = n[14], weapon = n[15]
    )
}
