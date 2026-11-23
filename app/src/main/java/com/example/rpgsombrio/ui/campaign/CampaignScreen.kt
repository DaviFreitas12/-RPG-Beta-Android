package com.example.rpgsombrio.ui.campaign

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.data.systemOptions
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel

@Composable
internal fun CampaignScreen(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    var campaignName by remember { mutableStateOf(prefs.getString("active_campaign_name", "Minha campanha") ?: "Minha campanha") }
    var campaignDescription by remember { mutableStateOf(prefs.getString("active_campaign_description", "Descreva aqui a história da sua mesa.") ?: "") }
    var selectedSystemId by remember { mutableStateOf(SheetStore.activeSystemId(prefs)) }
    val selectedSystem = systemDisplayName(prefs, selectedSystemId)
    var systemMenuExpanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val options = remember { systemOptions(prefs) }
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("SUA MESA", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Campanha", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Nome, história e sistema de regras.", color = Muted, fontSize = 13.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)) {
                Column(
                    Modifier.background(Brush.linearGradient(listOf(Brand.copy(alpha = .38f), Panel))).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("CAMPANHA ATUAL", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text(campaignName, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(campaignDescription, color = Muted, fontSize = 13.sp)
                    InfoPill(selectedSystem)
                    Button(onClick = { editing = true }, colors = ButtonDefaults.buttonColors(containerColor = Brand), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Editar campanha")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SISTEMA DE REGRAS", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("Novas fichas nascem neste sistema. Cada ficha pode ser vinculada a outro depois.", color = Muted, fontSize = 13.sp)
                    Box {
                        OutlinedButton(onClick = { systemMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedSystem, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ExpandMore, contentDescription = "Selecionar sistema")
                        }
                        DropdownMenu(expanded = systemMenuExpanded, onDismissRequest = { systemMenuExpanded = false }) {
                            options.forEach { option ->
                                DropdownMenuItem(text = { Text(option.name) }, onClick = {
                                    selectedSystemId = option.id
                                    SheetStore.setActiveSystem(prefs, option.id)
                                    systemMenuExpanded = false
                                })
                            }
                        }
                    }
                    OutlinedButton(onClick = { onNavigate("Sistemas") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Criar ou editar sistemas")
                    }
                }
            }
        }
    }
    if (editing) {
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Editar campanha") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(campaignName, { campaignName = it }, label = { Text("Nome") }, singleLine = true)
                    OutlinedTextField(campaignDescription, { campaignDescription = it }, label = { Text("Descrição") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    prefs.edit().putString("active_campaign_name", campaignName.trim().ifBlank { "Minha campanha" })
                        .putString("active_campaign_description", campaignDescription.trim()).apply()
                    editing = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } }
        )
    }
}
