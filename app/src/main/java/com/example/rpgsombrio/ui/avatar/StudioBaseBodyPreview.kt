package com.example.rpgsombrio.ui.avatar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Base corporal vetorial provisória: proporções fixas para receber roupas e armaduras em camadas. */
@Composable
internal fun StudioBaseBodyPreview(
    body: String,
    skinTone: Int,
    modifier: Modifier = Modifier
) {
    val skinTones = listOf(
        Color(0xFFF0C6A5), Color(0xFFDDA982), Color(0xFFC58B67), Color(0xFFA96F52),
        Color(0xFF80503D), Color(0xFF58372F)
    )
    val skin = skinTones.getOrElse(skinTone.coerceIn(0, skinTones.lastIndex)) { skinTones[1] }
    val shadow = Color(0xFF9C624D).copy(alpha = .72f)
    val line = Color(0xFF4A3431)
    val cloth = Color(0xFF242329)
    val female = body == "FEMININE"

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.72f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF28262B))
    ) {
        val sx = size.width / 512f
        val sy = size.height / 712f
        scale(sx, sy, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            // Fundo discreto para destacar a silhueta sem competir com o personagem.
            drawCircle(Color(0xFF38343A), radius = 190f, center = androidx.compose.ui.geometry.Offset(256f, 330f))
            drawLine(Color(0xFF62534A).copy(alpha = .28f), androidx.compose.ui.geometry.Offset(256f, 34f), androidx.compose.ui.geometry.Offset(256f, 680f), 2f)

            fun shape(points: Path, color: Color, outline: Boolean = true) {
                drawPath(points, color)
                if (outline) drawPath(points, line, style = Stroke(width = 3.5f))
            }
            fun path(block: Path.() -> Unit) = Path().apply(block)

            // Pernas: a silhueta é contínua e simétrica para facilitar o encaixe de calças e botas.
            shape(path {
                moveTo(198f, 390f); cubicTo(185f, 440f, 183f, 500f, 178f, 566f)
                cubicTo(175f, 604f, 166f, 644f, 170f, 665f); cubicTo(180f, 678f, 211f, 679f, 220f, 665f)
                cubicTo(224f, 618f, 232f, 565f, 241f, 520f); cubicTo(247f, 482f, 250f, 440f, 250f, 397f); close()
            }, skin)
            shape(path {
                moveTo(262f, 397f); cubicTo(262f, 440f, 265f, 482f, 271f, 520f)
                cubicTo(280f, 565f, 288f, 618f, 292f, 665f); cubicTo(301f, 679f, 332f, 678f, 342f, 665f)
                cubicTo(346f, 644f, 337f, 604f, 334f, 566f); cubicTo(329f, 500f, 327f, 440f, 314f, 390f); close()
            }, skin)

            // Pés.
            shape(path { moveTo(171f, 641f); cubicTo(153f, 651f, 143f, 668f, 148f, 679f); cubicTo(159f, 690f, 199f, 690f, 218f, 680f); cubicTo(224f, 674f, 220f, 663f, 214f, 657f); close() }, skin)
            shape(path { moveTo(298f, 657f); cubicTo(292f, 663f, 288f, 674f, 294f, 680f); cubicTo(313f, 690f, 353f, 690f, 364f, 679f); cubicTo(369f, 668f, 359f, 651f, 341f, 641f); close() }, skin)

            // Braços.
            shape(path {
                moveTo(185f, 183f); cubicTo(159f, 187f, 145f, 212f, 139f, 245f)
                cubicTo(132f, 286f, 123f, 329f, 120f, 365f); cubicTo(118f, 384f, 126f, 397f, 139f, 398f)
                cubicTo(151f, 397f, 156f, 383f, 158f, 367f); cubicTo(171f, 327f, 185f, 284f, 201f, 246f); close()
            }, skin)
            shape(path {
                moveTo(327f, 183f); cubicTo(353f, 187f, 367f, 212f, 373f, 245f)
                cubicTo(380f, 286f, 389f, 329f, 392f, 365f); cubicTo(394f, 384f, 386f, 397f, 373f, 398f)
                cubicTo(361f, 397f, 356f, 383f, 354f, 367f); cubicTo(341f, 327f, 327f, 284f, 311f, 246f); close()
            }, skin)

            // Tronco com pequenas variações de proporção entre os dois modelos.
            shape(path {
                moveTo(207f, 133f); cubicTo(193f, 145f, 180f, 158f, 178f, 184f)
                cubicTo(175f, 219f, 185f, 252f, if (female) 193f else 187f, 286f)
                cubicTo(190f, 320f, 184f, 354f, 190f, 390f)
                cubicTo(225f, 407f, 287f, 407f, 322f, 390f)
                cubicTo(328f, 354f, 322f, 320f, 325f, 286f)
                cubicTo(if (female) 333f else 325f, 252f, 337f, 219f, 334f, 184f)
                cubicTo(332f, 158f, 319f, 145f, 305f, 133f); close()
            }, skin)

            // Pescoço e cabeça.
            shape(path { moveTo(226f, 111f); lineTo(286f, 111f); lineTo(292f, 153f); cubicTo(280f, 174f, 232f, 174f, 220f, 153f); close() }, skin)
            drawCircle(skin, 48f, androidx.compose.ui.geometry.Offset(256f, 78f))
            drawCircle(line, 48f, androidx.compose.ui.geometry.Offset(256f, 78f), style = Stroke(width = 3.5f))
            // Orelhas e traços faciais discretos.
            drawCircle(skin, 9f, androidx.compose.ui.geometry.Offset(208f, 81f))
            drawCircle(skin, 9f, androidx.compose.ui.geometry.Offset(304f, 81f))
            drawLine(shadow, androidx.compose.ui.geometry.Offset(235f, 91f), androidx.compose.ui.geometry.Offset(247f, 92f), 2.5f)
            drawLine(shadow, androidx.compose.ui.geometry.Offset(265f, 92f), androidx.compose.ui.geometry.Offset(277f, 91f), 2.5f)
            drawLine(line.copy(alpha = .55f), androidx.compose.ui.geometry.Offset(252f, 101f), androidx.compose.ui.geometry.Offset(256f, 108f), 2f)
            drawLine(line.copy(alpha = .55f), androidx.compose.ui.geometry.Offset(246f, 119f), androidx.compose.ui.geometry.Offset(266f, 119f), 2f)

            // Roupa-base neutra, necessária para que a composição não dependa de nudez.
            if (female) {
                shape(path { moveTo(199f, 169f); cubicTo(218f, 181f, 236f, 183f, 256f, 177f); cubicTo(276f, 183f, 294f, 181f, 313f, 169f); lineTo(322f, 246f); cubicTo(283f, 266f, 229f, 266f, 190f, 246f); close() }, cloth)
            }
            shape(path { moveTo(190f, 365f); cubicTo(224f, 378f, 288f, 378f, 322f, 365f); lineTo(330f, 421f); cubicTo(291f, 435f, 221f, 435f, 182f, 421f); close() }, cloth)
            drawLine(Color(0xFF9C8064), androidx.compose.ui.geometry.Offset(183f, 421f), androidx.compose.ui.geometry.Offset(329f, 421f), 3f)

            // Marcas anatômicas suaves, sem excesso de definição.
            drawLine(shadow, androidx.compose.ui.geometry.Offset(256f, 190f), androidx.compose.ui.geometry.Offset(256f, 350f), 2f)
            if (!female) {
                drawLine(shadow, androidx.compose.ui.geometry.Offset(205f, 214f), androidx.compose.ui.geometry.Offset(244f, 220f), 2f)
                drawLine(shadow, androidx.compose.ui.geometry.Offset(268f, 220f), androidx.compose.ui.geometry.Offset(307f, 214f), 2f)
            } else {
                drawLine(shadow, androidx.compose.ui.geometry.Offset(211f, 257f), androidx.compose.ui.geometry.Offset(245f, 262f), 2f)
                drawLine(shadow, androidx.compose.ui.geometry.Offset(267f, 262f), androidx.compose.ui.geometry.Offset(301f, 257f), 2f)
            }
            drawLine(shadow, androidx.compose.ui.geometry.Offset(216f, 290f), androidx.compose.ui.geometry.Offset(242f, 294f), 2f)
            drawLine(shadow, androidx.compose.ui.geometry.Offset(270f, 294f), androidx.compose.ui.geometry.Offset(296f, 290f), 2f)
        }
    }
}
