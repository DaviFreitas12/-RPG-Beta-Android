package com.example.rpgsombrio.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CampaignStore
import com.example.rpgsombrio.data.masterToolsAvailable
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted

/**
 * Bestiário, encontros e combate são ferramentas do mestre (0.10.5). Devolve falso quando a campanha ativa é uma
 * em que este aparelho é jogador. Sem campanha ativa, nada é escondido. A regra está em `data/MasterRules.kt`.
 */
@Composable
internal fun rememberMasterTools(): Boolean {
    val context = LocalContext.current
    return remember {
        val prefs = context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE)
        masterToolsAvailable(CampaignStore.active(prefs), CampaignStore.localUserId(prefs))
    }
}

/** Mostra [content] só para o mestre da campanha ativa; para o jogador, um aviso no lugar da tela. */
@Composable
internal fun MasterOnlyGate(modifier: Modifier = Modifier, what: String, content: @Composable () -> Unit) {
    if (rememberMasterTools()) {
        content()
    } else {
        Column(
            modifier.fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("FERRAMENTA DO MESTRE", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(what, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Na campanha ativa você é jogador, então só o mestre usa esta área.", color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}
