package com.example.rpgsombrio.ui.combat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.rpgsombrio.data.CombatAction

@Composable
fun BattleArenaScreen(userRole: String = "jogador") {
    val sampleActions = remember {
        listOf(
            CombatAction("1", "Ataque Furioso", "1 Vigor", "Cortante", "+8", "1d12+4"),
            CombatAction("2", "Bola de Fogo", "3 Mana", "Fogo", "+6", "8d6"),
            CombatAction("3", "Apunhalada", "1 Foco", "Perfurante", "+9", "2d6+4", true)
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (userRole == "mestre") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "PAINEL DE GERENCIAMENTO DO MESTRE")
            }
        } else {
            CombatDeckScreen(
                actions = sampleActions,
                onActionQuickRoll = {}
