package com.example.rpgsombrio.ui.avatar

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.ui.theme.*
import java.util.UUID

private data class StudioDraft(val id: String, val name: String, val body: String, val skin: Int, val hair: Int, val armor: Int, val updated: Long)

private object StudioLocalStore {
    private const val PREFS = "rpg_character_studio"
    private const val KEY = "projects_v1"
    fun load(context: Context): List<StudioDraft> = runCatching {
        val array = JSONArray(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            StudioDraft(o.getString("id"), o.getString("name"), o.optString("body", "MASCULINE").let { if (it == "NEUTRAL") "MASCULINE" else it }, o.optInt("skin"), o.optInt("hair"), o.optInt("armor"), o.optLong("updated"))
        }
    }.getOrDefault(emptyList())
    fun save(context: Context, drafts: List<StudioDraft>) {
        val array = JSONArray()
        drafts.forEach { d -> array.put(JSONObject().put("id", d.id).put("name", d.name).put("body", d.body).put("skin", d.skin).put("hair", d.hair).put("armor", d.armor).put("updated", d.updated)) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }
}

@Composable
internal fun CharacterStudioScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var projects by remember { mutableStateOf(StudioLocalStore.load(context)) }
    var selectedId by remember { mutableStateOf(projects.firstOrNull()?.id) }
    var showCreate by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    val selected = projects.firstOrNull { it.id == selectedId }
    fun update(transform: (StudioDraft) -> StudioDraft) {
        val current = selected ?: return
        projects = projects.map { if (it.id == current.id) transform(it).copy(updated = System.currentTimeMillis()) else it }
        StudioLocalStore.save(context, projects)
    }
    Column(modifier.fillMaxSize().background(Night).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("CHARACTER STUDIO", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                Text("Forje sua lenda", color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            FilledTonalButton(onClick = { nameInput = ""; showCreate = true }) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Novo")
            }
        }
        Spacer(Modifier.height(14.dp))
        if (projects.isEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Hairline), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.AutoAwesome, null, tint = Accent, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Seu próximo herói começa aqui", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Crie um projeto para definir a base corporal e experimentar a aparência. O projeto fica salvo neste aparelho.", color = Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Button(onClick = { nameInput = ""; showCreate = true }) { Text("Criar personagem") }
                }
            }
        } else {
            Text("PROJETOS", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Spacer(Modifier.height(7.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(projects, key = { it.id }) { project ->
                    Card(onClick = { selectedId = project.id }, colors = CardDefaults.cardColors(containerColor = if (project.id == selectedId) Brand.copy(alpha = .16f) else Panel), border = BorderStroke(1.dp, if (project.id == selectedId) Accent.copy(alpha = .6f) else Hairline), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(Brush.linearGradient(listOf(Color(0xFF46352F), Color(0xFF17191E)))), contentAlignment = Alignment.Center) { Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFFD6B27A)) }
                            Spacer(Modifier.width(11.dp))
                            Column(Modifier.weight(1f)) { Text(project.name, color = Ink, fontWeight = FontWeight.SemiBold); Text("Base ${project.body.lowercase().replaceFirstChar { it.uppercase() }} · rascunho", color = Muted, fontSize = 11.sp) }
                            if (project.id == selectedId) Text("EDITANDO", color = Accent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            selected?.let { draft ->
                Spacer(Modifier.height(12.dp))
                Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Hairline), shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(draft.name, color = Ink, fontSize = 19.sp, fontWeight = FontWeight.Bold); Text("APARÊNCIA DO PERSONAGEM", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                            IconButton(onClick = { projects = projects.filterNot { it.id == draft.id }; StudioLocalStore.save(context, projects); selectedId = projects.firstOrNull()?.id }) { Icon(Icons.Default.DeleteOutline, "Excluir projeto", tint = Muted) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            StudioBaseBodyPreview(
                                body = draft.body,
                                skinTone = draft.skin,
                                modifier = Modifier.weight(1f)
                            )
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("BASE CORPORAL", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                listOf("MASCULINE" to "Masculina", "FEMININE" to "Feminina").forEach { (value, label) ->
                                    FilterChip(
                                        selected = draft.body == value,
                                        onClick = { update { it.copy(body = value) } },
                                        label = { Text(label, fontSize = 12.sp) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text("TOM DE PELE", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                StudioChoiceRow("", listOf("Claro", "Médio-claro", "Oliva", "Moreno", "Escuro", "Retinto"), draft.skin) { v -> update { it.copy(skin = v) } }
                            }
                        }
                        Text("As bases usam proporções fixas e o mesmo enquadramento. Cabelos, roupas e armaduras serão adicionados em camadas compatíveis.", color = Muted, fontSize = 11.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Salvamento automático ativo", color = Good, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Save, null, tint = Good, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
    if (showCreate) AlertDialog(
        onDismissRequest = { showCreate = false },
        title = { Text("Novo projeto") },
        text = { OutlinedTextField(value = nameInput, onValueChange = { nameInput = it.take(40) }, label = { Text("Nome do personagem") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { val n = nameInput.trim(); if (n.isNotEmpty()) { val now = System.currentTimeMillis(); val d = StudioDraft(UUID.randomUUID().toString(), n, "MASCULINE", 1, 0, 0, now); projects = listOf(d) + projects; selectedId = d.id; StudioLocalStore.save(context, projects); showCreate = false } }) { Text("Criar") } },
        dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun StudioChoiceRow(title: String, options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        if (title.isNotBlank()) Text(title.uppercase(), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            options.forEachIndexed { index, option ->
                FilterChip(selected = selected == index, onClick = { onSelect(index) }, label = { Text(option, fontSize = 10.sp, maxLines = 1) })
            }
        }
    }
}
