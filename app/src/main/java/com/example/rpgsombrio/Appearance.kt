package com.example.rpgsombrio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CharacterLook
import com.example.rpgsombrio.data.CharacterSheet
import kotlin.random.Random

// --- paletas e nomes das opções ----------------------------------------------------------------

private val SKIN_COLORS = listOf(
    Color(0xFFF6D9C4), Color(0xFFE8B896), Color(0xFFC68642), Color(0xFF8D5524),
    Color(0xFF5C3A21), Color(0xFFB7C7A0), Color(0xFFC9B8E0)
)
private val HAIR_COLORS = listOf(
    Color(0xFF1B1B1F), Color(0xFF4A2F1B), Color(0xFFB5651D), Color(0xFFE3C16F),
    Color(0xFFC9CCD6), Color(0xFFF4F4F4), Color(0xFF5B3A8C), Color(0xFF2E6F5E)
)
private val EYE_COLORS = listOf(
    Color(0xFF3B2A1A), Color(0xFF2E6FD0), Color(0xFF2E8B57), Color(0xFF8A8F99), Color(0xFFD4A017), Color(0xFFC0392B)
)
private val OUTFIT_COLORS = listOf(
    Color(0xFF7A1F2B), Color(0xFF2D4A7A), Color(0xFF2F5D3A), Color(0xFF3A3A44),
    Color(0xFF6B4E2E), Color(0xFF5B3A8C), Color(0xFFB8892B)
)
private val FACE_NAMES = listOf("Oval", "Redondo", "Alongado")
private val HAIR_NAMES = listOf("Careca", "Curto", "Longo", "Coque", "Moicano")
private val BEARD_NAMES = listOf("Nenhuma", "Rala", "Cheia", "Bigode")
private val ACCESSORY_NAMES = listOf("Nenhum", "Cicatriz", "Tapa-olho", "Pintura", "Tiara")
private val OUTFIT_NAMES = listOf("Túnica", "Armadura", "Capuz", "Vestes")

private fun <T> List<T>.at(i: Int): T = this[i.coerceIn(0, lastIndex)]

// --- avatar desenhado em camadas ---------------------------------------------------------------

/** Avatar do personagem desenhado em Canvas, sem imagens: cada camada vem das escolhas em [look]. */
@Composable
internal fun AvatarView(look: CharacterLook, size: Dp = 84.dp, modifier: Modifier = Modifier) {
    val skin = SKIN_COLORS.at(look.skin)
    val skinDark = lerp(skin, Color.Black, 0.22f)
    val hair = HAIR_COLORS.at(look.hairColor)
    val hairDark = lerp(hair, Color.Black, 0.3f)
    val eye = EYE_COLORS.at(look.eyeColor)
    val cloth = OUTFIT_COLORS.at(look.outfitColor)
    Canvas(
        modifier.size(size).clip(RoundedCornerShape(14.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A3046), Color(0xFF151824))))
    ) {
        val w = this.size.minDimension
        val c = w / 2f
        val fw = w * 0.40f * listOf(1.0f, 1.15f, 0.88f).at(look.face)
        val fh = w * 0.50f * listOf(1.0f, 0.92f, 1.1f).at(look.face)
        val faceCy = 0.46f * w
        val ftop = faceCy - fh / 2f
        val ink = 0.012f * w

        // capuz atrás da cabeça
        if (look.outfit == 2) drawOval(cloth, Offset(c - 0.27f * w, 0.14f * w), Size(0.54f * w, 0.62f * w))
        // cabelo longo atrás
        if (look.hairStyle == 2) {
            drawRoundRect(hair, Offset(c - fw / 2f - 0.05f * w, 0.20f * w), Size(fw + 0.10f * w, 0.58f * w), CornerRadius(0.12f * w))
        }
        // ombros e roupa
        drawOval(cloth, Offset(0.13f * w, 0.76f * w), Size(0.74f * w, 0.5f * w))
        drawRect(skinDark, Offset(c - 0.07f * w, 0.60f * w), Size(0.14f * w, 0.20f * w))
        when (look.outfit) {
            1 -> { // armadura
                drawOval(Color(0xFF8C93A6), Offset(0.13f * w, 0.76f * w), Size(0.74f * w, 0.5f * w))
                drawLine(Color(0xFF5A6072), Offset(c, 0.80f * w), Offset(c, w), ink)
                drawLine(Color(0xFF5A6072), Offset(0.30f * w, 0.84f * w), Offset(0.30f * w, w), ink)
                drawLine(Color(0xFF5A6072), Offset(0.70f * w, 0.84f * w), Offset(0.70f * w, w), ink)
                drawArc(Color(0xFFB4BAC9), 0f, 180f, false, Offset(c - 0.11f * w, 0.70f * w), Size(0.22f * w, 0.14f * w), style = Stroke(0.03f * w))
            }
            3 -> { // vestes com gola em V
                val v = Path().apply {
                    moveTo(c - 0.09f * w, 0.775f * w); lineTo(c + 0.09f * w, 0.775f * w); lineTo(c, 0.93f * w); close()
                }
                drawPath(v, skinDark)
                drawLine(Color(0xFFD9B35A), Offset(c - 0.09f * w, 0.775f * w), Offset(c, 0.93f * w), ink)
                drawLine(Color(0xFFD9B35A), Offset(c + 0.09f * w, 0.775f * w), Offset(c, 0.93f * w), ink)
            }
            else -> drawArc(lerp(cloth, Color.Black, 0.35f), 0f, 180f, false, Offset(c - 0.10f * w, 0.72f * w), Size(0.20f * w, 0.10f * w), style = Stroke(0.02f * w))
        }

        // rosto e orelhas
        drawCircle(skin, 0.035f * w, Offset(c - fw / 2f, faceCy + 0.02f * w))
        drawCircle(skin, 0.035f * w, Offset(c + fw / 2f, faceCy + 0.02f * w))
        drawOval(skin, Offset(c - fw / 2f, ftop), Size(fw, fh))

        // barba (antes da boca)
        when (look.beard) {
            1 -> drawArc(hair.copy(alpha = 0.35f), 0f, 180f, true, Offset(c - fw / 2f, ftop + 0.22f * fh), Size(fw, 0.8f * fh))
            2 -> drawArc(hair, 0f, 180f, true, Offset(c - fw / 2f, ftop + 0.22f * fh), Size(fw, 0.8f * fh))
        }

        // olhos, sobrancelhas, nariz, boca
        for (side in listOf(-1f, 1f)) {
            val ex = c + side * 0.085f * w
            drawOval(Color.White, Offset(ex - 0.035f * w, faceCy - 0.02f * w), Size(0.07f * w, 0.045f * w))
            drawCircle(eye, 0.02f * w, Offset(ex, faceCy + 0.0025f * w))
            drawCircle(Color.Black, 0.009f * w, Offset(ex, faceCy + 0.0025f * w))
            drawLine(hairDark, Offset(ex - 0.04f * w, faceCy - 0.055f * w), Offset(ex + 0.04f * w, faceCy - 0.06f * w), ink, StrokeCap.Round)
        }
        drawLine(skinDark, Offset(c, faceCy + 0.01f * w), Offset(c - 0.012f * w, faceCy + 0.08f * w), 0.008f * w, StrokeCap.Round)
        drawArc(Color(0xFF8B3A3A), 20f, 140f, false, Offset(c - 0.04f * w, faceCy + 0.085f * w), Size(0.08f * w, 0.05f * w), style = Stroke(0.01f * w, cap = StrokeCap.Round))
        if (look.beard == 3) {
            drawOval(hair, Offset(c - 0.065f * w, faceCy + 0.065f * w), Size(0.065f * w, 0.028f * w))
            drawOval(hair, Offset(c, faceCy + 0.065f * w), Size(0.065f * w, 0.028f * w))
        }

        // cabelo na frente
        val capTop = ftop - 0.04f * w
        when (look.hairStyle) {
            1, 2, 3 -> drawArc(hair, 180f, 180f, true, Offset(c - fw / 2f - 0.01f * w, capTop), Size(fw + 0.02f * w, 0.75f * fh))
            4 -> drawRoundRect(hair, Offset(c - 0.03f * w, ftop - 0.10f * w), Size(0.06f * w, 0.20f * w), CornerRadius(0.03f * w))
        }
        if (look.hairStyle == 3) drawCircle(hair, 0.07f * w, Offset(c, capTop - 0.01f * w))

        // acessórios
        when (look.accessory) {
            1 -> drawLine(Color(0xFF9B4B4B), Offset(c + 0.06f * w, faceCy - 0.08f * w), Offset(c + 0.11f * w, faceCy + 0.04f * w), 0.012f * w, StrokeCap.Round)
            2 -> {
                drawLine(Color(0xFF111111), Offset(c - fw / 2f, faceCy - 0.06f * w), Offset(c + fw / 2f, faceCy - 0.03f * w), 0.01f * w)
                drawCircle(Color(0xFF111111), 0.04f * w, Offset(c - 0.085f * w, faceCy))
            }
            3 -> {
                drawLine(Accent, Offset(c + 0.06f * w, faceCy + 0.03f * w), Offset(c + 0.12f * w, faceCy + 0.03f * w), 0.012f * w, StrokeCap.Round)
                drawLine(Accent, Offset(c + 0.065f * w, faceCy + 0.055f * w), Offset(c + 0.115f * w, faceCy + 0.055f * w), 0.012f * w, StrokeCap.Round)
            }
            4 -> {
                drawLine(Color(0xFFD9B35A), Offset(c - fw / 2f + 0.02f * w, ftop + 0.13f * fh), Offset(c + fw / 2f - 0.02f * w, ftop + 0.13f * fh), 0.016f * w, StrokeCap.Round)
                drawCircle(Color(0xFFC0392B), 0.015f * w, Offset(c, ftop + 0.13f * fh))
            }
        }
    }
}

// --- seção da ficha ----------------------------------------------------------------------------

@Composable
private fun SwatchRow(label: String, colors: List<Color>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEachIndexed { i, color ->
                Box(
                    Modifier.size(34.dp).clip(CircleShape).background(color)
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

/** Texto de aparência montado a partir das escolhas do avatar e dos campos de descrição. */
private fun lookSummary(look: CharacterLook): String {
    val colorNames = listOf("preto", "castanho", "ruivo", "loiro", "grisalho", "branco", "roxo", "verde")
    val eyeNames = listOf("castanhos", "azuis", "verdes", "cinzentos", "âmbar", "vermelhos")
    val hairText = listOf("careca", "cabelo curto", "cabelo longo", "cabelo preso em coque", "moicano")
    val beardText = listOf("", "barba rala", "barba cheia", "bigode")
    val parts = mutableListOf<String>()
    if (look.age.isNotBlank()) parts += if (look.age.all { it.isDigit() }) "${look.age} anos" else look.age.trim()
    if (look.height.isNotBlank()) parts += look.height.trim()
    if (look.build.isNotBlank()) parts += look.build.trim()
    val hair = hairText.at(look.hairStyle)
    parts += if (look.hairStyle == 0) hair else "$hair ${colorNames.at(look.hairColor)}"
    parts += "olhos ${eyeNames.at(look.eyeColor)}"
    beardText.at(look.beard).takeIf { it.isNotBlank() }?.let { parts += it }
    if (look.accessory > 0) parts += ACCESSORY_NAMES.at(look.accessory).lowercase()
    if (look.marks.isNotBlank()) parts += look.marks.trim()
    return parts.joinToString(", ").replaceFirstChar { it.uppercase() } + "."
}

@Composable
internal fun AppearanceSection(sheet: CharacterSheet, onChange: (CharacterSheet) -> Unit) {
    val look = sheet.look
    fun update(next: CharacterLook) = onChange(sheet.copy(look = next.copy(configured = true)))
    CollapsibleSection(
        "APARÊNCIA",
        summary = if (look.configured) listOf(look.age, look.height, look.build).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Avatar criado" } else "Crie o visual do personagem",
        startExpanded = false
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AvatarView(look, 120.dp)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedButton(onClick = {
                    val r = Random
                    update(look.copy(
                        skin = r.nextInt(SKIN_COLORS.size), face = r.nextInt(FACE_NAMES.size),
                        hairStyle = r.nextInt(HAIR_NAMES.size), hairColor = r.nextInt(HAIR_COLORS.size),
                        eyeColor = r.nextInt(EYE_COLORS.size), beard = r.nextInt(BEARD_NAMES.size),
                        accessory = r.nextInt(ACCESSORY_NAMES.size), outfit = r.nextInt(OUTFIT_NAMES.size),
                        outfitColor = r.nextInt(OUTFIT_COLORS.size)
                    ))
                }) { Text("Sortear") }
                Text("O retrato da galeria, se existir, aparece no lugar do avatar.", color = Muted, fontSize = 11.sp)
            }
        }
        SwatchRow("Pele", SKIN_COLORS, look.skin) { update(look.copy(skin = it)) }
        ChoiceRow("Rosto", FACE_NAMES, look.face) { update(look.copy(face = it)) }
        ChoiceRow("Cabelo", HAIR_NAMES, look.hairStyle) { update(look.copy(hairStyle = it)) }
        SwatchRow("Cor do cabelo", HAIR_COLORS, look.hairColor) { update(look.copy(hairColor = it)) }
        SwatchRow("Olhos", EYE_COLORS, look.eyeColor) { update(look.copy(eyeColor = it)) }
        ChoiceRow("Barba", BEARD_NAMES, look.beard) { update(look.copy(beard = it)) }
        ChoiceRow("Detalhe", ACCESSORY_NAMES, look.accessory) { update(look.copy(accessory = it)) }
        ChoiceRow("Roupa", OUTFIT_NAMES, look.outfit) { update(look.copy(outfit = it)) }
        SwatchRow("Cor da roupa", OUTFIT_COLORS, look.outfitColor) { update(look.copy(outfitColor = it)) }
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

internal fun CharacterLook.toAvatarKey(): String =
    AVATAR_KEY_PREFIX + listOf(skin, face, hairStyle, hairColor, eyeColor, beard, accessory, outfit, outfitColor).joinToString(",")

internal fun parseAvatarKey(key: String): CharacterLook? {
    if (!key.startsWith(AVATAR_KEY_PREFIX)) return null
    val n = key.removePrefix(AVATAR_KEY_PREFIX).split(",").mapNotNull { it.toIntOrNull() }
    if (n.size < 9) return null
    return CharacterLook(
        configured = true, skin = n[0], face = n[1], hairStyle = n[2], hairColor = n[3], eyeColor = n[4],
        beard = n[5], accessory = n[6], outfit = n[7], outfitColor = n[8]
    )
}
