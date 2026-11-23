package com.example.rpgsombrio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CombatAction

@Composable
fun CombatActionCard(
    action: CombatAction,
    onQuickRoll: (CombatAction) -> Unit
) {
    val borderColor = when (action.damageType.lowercase()) {
        "cortante" -> Color(0xFFFF5722)
        "perfurante" -> Color(0xFF9E9E9E)
        "contundente" -> Color(0xFFCDDC39)
        "fogo" -> Color(0xFFE91E63)
        "raio" -> Color(0xFFFFEB3B)
        else -> Color(0xFF00BCD4)
    }

    Surface(
        modifier = Modifier
            .width(160.dp)
            .height(130.dp)
            .padding(4.dp)
            .clickable { onQuickRoll(action) },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, borderColor),
        color = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = action.name.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Custo: ${action.resourceCost}", color = Color.LightGray, fontSize = 11.sp)
                Text(text = action.damage, color = borderColor, fontWeight = FontWeight.Black, fontSize = 16.sp)
            }
            Surface(
                color = borderColor,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "ROLAR ${action.bonus}",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
