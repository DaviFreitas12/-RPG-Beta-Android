package com.example.rpgsombrio.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Panel
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted

/** Tela de abertura: só a campanha atual e os dois caminhos mais usados. */
@Composable
internal fun HomeScreen(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val campaignName = remember { prefs.getString("active_campaign_name", "Minha campanha") ?: "Minha campanha" }
    val systemName = remember { systemDisplayName(prefs, SheetStore.activeSystemId(prefs)) }
    val hasCharacters = remember { SheetStore.loadCharacters(prefs).isNotEmpty() }
    Column(modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("RPG", color = Accent, fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = 3.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { onNavigate("Configurações") }) {
                Icon(Icons.Default.Settings, contentDescription = "Configurações", tint = Muted)
            }
        }
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(Brand.copy(alpha = .16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, tint = Brand, modifier = Modifier.size(50.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text("CAMPANHA ATUAL", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(6.dp))
            Text(campaignName, color = Ink, fontSize = 30.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            InfoPill(systemName)
            TextButton(onClick = { onNavigate("Campanha") }) { Text("Editar campanha", color = Accent) }
        }
        if (!hasCharacters) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Panel),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Hairline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("PRIMEIROS PASSOS", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    listOf(
                        "Escolha o sistema em Campanha",
                        "Crie seu personagem em Fichas",
                        "Role dados e controle o combate em Dados"
                    ).forEachIndexed { i, step ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(24.dp).clip(CircleShape).background(Brand.copy(alpha = .18f)), contentAlignment = Alignment.Center) {
                                Text("${i + 1}", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(step, color = Ink, fontSize = 14.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = { onNavigate("Personagens") },
            colors = ButtonDefaults.buttonColors(containerColor = Brand),
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) { Text(if (hasCharacters) "Abrir personagens" else "Criar primeira ficha", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = { onNavigate("Motor") }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
            Text("Rolar dados e combate")
        }
        Spacer(Modifier.height(12.dp))
    }
}
