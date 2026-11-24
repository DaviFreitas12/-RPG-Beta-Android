package com.example.rpgsombrio.ui.combat

import android.content.SharedPreferences
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.EncounterStore
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.engine.Encounter
import com.example.rpgsombrio.engine.EncounterEntry
import com.example.rpgsombrio.ui.components.CollapsibleSection
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted

/**
 * Encontros preparados (0.9.17): o mestre monta a lista de criaturas antes da sessão e, na hora da luta,
 * um toque em "Iniciar" coloca as criaturas e todos os personagens na batalha.
 */
@Composable
internal fun EncountersSection(
    prefs: SharedPreferences, monsters: List<MonsterSheet>, currentGroups: List<EncounterEntry>,
    startExpanded: Boolean, onStart: (Encounter) -> Unit
) {
    var encounters by remember { mutableStateOf(EncounterStore.load(prefs)) }
    var name by remember { mutableStateOf("") }
    var pickIndex by remember { mutableStateOf(0) }
    var quantity by remember { mutableStateOf(1) }
    var draft by remember { mutableStateOf<List<EncounterEntry>>(emptyList()) }
    var message by remember { mutableStateOf("") }

    fun monsterName(id: String): String = monsters.firstOrNull { it.id == id }?.name?.ifBlank { "Criatura" } ?: "Criatura removida"
    fun describe(entries: List<EncounterEntry>): String = entries.joinToString(", ") { "${it.quantity}× ${monsterName(it.monsterId)}" }
    fun save(entries: List<EncounterEntry>) {
        if (entries.isEmpty()) { message = "Adicione ao menos uma criatura."; return }
        val title = name.trim().ifBlank { "Encontro ${encounters.size + 1}" }
        encounters = encounters + Encounter(java.util.UUID.randomUUID().toString(), title, entries)
        EncounterStore.save(prefs, encounters)
        name = ""; draft = emptyList(); message = "Encontro \"$title\" salvo."
    }

    CollapsibleSection(
        "ENCONTROS PREPARADOS",
        summary = if (encounters.isEmpty()) "Monte antes da sessão, inicie com um toque" else "${encounters.size} salvo(s)",
        startExpanded = startExpanded
    ) {
        if (encounters.isEmpty()) Text("Nenhum encontro salvo ainda.", color = Muted, fontSize = 13.sp)
        encounters.forEach { encounter ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(encounter.name, color = Ink, fontWeight = FontWeight.Bold)
                    Text(describe(encounter.entries), color = Muted, fontSize = 12.sp)
                }
                Button(onClick = { onStart(encounter) }, colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Iniciar") }
                IconButton(onClick = {
                    encounters = encounters.filter { it.id != encounter.id }
                    EncounterStore.save(prefs, encounters)
                }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Apagar encontro", tint = Muted) }
            }
        }

        Text("Novo encontro", color = Muted, fontSize = 13.sp)
        if (monsters.isEmpty()) {
            Text("Cadastre criaturas no Bestiário para montar encontros.", color = Muted, fontSize = 13.sp)
        } else {
            TextInput("Nome do encontro", name, { name = it })
            val picked = monsters[pickIndex.coerceIn(0, monsters.lastIndex)]
            PickButton(picked.name.ifBlank { "Criatura" }, monsters.map { it.name.ifBlank { "Criatura" } }) { pickIndex = it }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                NumberField("Quantidade", quantity, { quantity = it.coerceIn(1, 20) }, Modifier.weight(1f))
                OutlinedButton(onClick = { draft = draft + EncounterEntry(picked.id, quantity) }, modifier = Modifier.weight(1f)) { Text("+ À lista") }
            }
            if (draft.isNotEmpty()) {
                Text(describe(draft), color = Ink, fontSize = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { save(draft) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Brand)) { Text("Salvar encontro") }
                    TextButton(onClick = { draft = emptyList() }) { Text("Limpar", color = Muted) }
                }
            }
        }
        if (currentGroups.isNotEmpty()) {
            OutlinedButton(onClick = { save(currentGroups) }, modifier = Modifier.fillMaxWidth()) { Text("Salvar a luta atual como encontro") }
        }
        if (message.isNotBlank()) Text(message, color = Muted, fontSize = 13.sp)
    }
}
