package com.example.rpgsombrio.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.ui.components.ChipFilterRow
import com.example.rpgsombrio.ui.components.EditorCard
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.RpgPalette
import com.example.rpgsombrio.ui.theme.ThemeMode

@Composable
internal fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "-"
    }
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("AJUSTES DO APP", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Configurações", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        }
        item {
            EditorCard("APARÊNCIA", "Escolha entre o modo claro, escuro ou o do celular.") {
                ChipFilterRow(ThemeMode.values().map { it.label }, RpgPalette.mode.label) { picked ->
                    ThemeMode.values().firstOrNull { it.label == picked }?.let { RpgPalette.setMode(context, it) }
                }
            }
        }
        item {
            EditorCard("SOBRE", "Fichas, combate e regras para qualquer sistema de RPG.") {
                Text("Versão $version", color = Muted, fontSize = 13.sp)
            }
        }
    }
}
