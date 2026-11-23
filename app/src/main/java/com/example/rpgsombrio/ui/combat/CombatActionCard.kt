package com.example.rpgsombrio.ui.combat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CombatAction
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Amber
import com.example.rpgsombrio.ui.theme.Danger
import com.example.rpgsombrio.ui.theme.Good
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel
import com.example.rpgsombrio.ui.theme.Panel2

/** Cor da faixa da carta conforme o tipo de dano; tipos desconhecidos usam o destaque padrão. */
private fun typeTint(type: String, fallback: Color): Color {
    val t = type.lowercase()
    return when {
        listOf("fogo", "fire", "ígneo", "igneo").any { it in t } -> Danger
        listOf("cort", "perfur", "contund", "slash", "pierc", "bludg").any { it in t } -> Amber
        listOf("veneno", "ácido", "acido", "poison", "acid", "necr").any { it in t } -> Good
        else -> fallback
    }
}

/**
 * Carta tática de uma ação (0.9.14): título, tipo de dano, modificador de ataque e fórmula de dano.
 * Um toque em qualquer parte da carta chama [onClick] (a rolagem rápida).
 */
@Composable
internal fun CombatActionCard(
    action: CombatAction,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = typeTint(action.type, Accent)
    Card(
        modifier = modifier.heightIn(min = 104.dp).alpha(if (enabled) 1f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        border = BorderStroke(1.5.dp, tint.copy(alpha = .75f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(action.name.ifBlank { "Ação" }, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (action.type.isNotBlank()) {
                Text(
                    action.type.uppercase(), color = tint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("ACERTO", color = Muted, fontSize = 10.sp, letterSpacing = .8.sp)
                    Text(
                        action.attackBonus.ifBlank { if (action.autoHit) "Auto" else "—" },
                        color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1
                    )
                }
                Column(Modifier.weight(1.3f)) {
                    Text("DANO", color = Muted, fontSize = 10.sp, letterSpacing = .8.sp)
                    Text(
                        action.damageFormula.ifBlank { "—" },
                        color = Ink, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
