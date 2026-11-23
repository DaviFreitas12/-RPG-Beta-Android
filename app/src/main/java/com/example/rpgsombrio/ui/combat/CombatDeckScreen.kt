package com.example.rpgsombrio.ui.combat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CombatAction
import com.example.rpgsombrio.ui.components.CombatActionCard

@Composable
fun CombatDeckScreen(
    actions: List<CombatAction>,
    onActionQuickRoll: (CombatAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "DECK DE COMBATE",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (actions.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(130.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Nenhuma ação de combate disponível.", color = Color.Gray)
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(actions, key = { it.id }) { action ->
                    CombatActionCard(action = action, onQuickRoll = onActionQuickRoll)
                }
            }
        }
    }
}
