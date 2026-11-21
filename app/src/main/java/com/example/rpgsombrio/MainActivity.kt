package com.example.rpgsombrio

import android.os.Bundle
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

private val Night = Color(0xFF101014)
private val Panel = Color(0xFF202027)
private val Panel2 = Color(0xFF29252A)
private val Blood = Color(0xFF922C38)
private val Gold = Color(0xFFC6A15B)
private val Parchment = Color(0xFFE9DDC5)
private val Muted = Color(0xFFAAA19A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RpgApp() }
    }
}

@Composable
private fun RpgApp() {
    var selected by remember { mutableStateOf("Campanha") }
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Gold, secondary = Blood, background = Night, surface = Panel,
        onBackground = Parchment, onSurface = Parchment
    )) {
        Scaffold(
            containerColor = Night,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    val tabs = listOf(
                        "Campanha" to Icons.Default.Home,
                        "Personagens" to Icons.Default.Person,
                        "Bestiário" to Icons.Default.Pets,
                        "Grimório" to Icons.Default.MenuBook,
                        "Itens" to Icons.Default.Inventory2,
                        "Sistemas" to Icons.Default.Tune
                    )
                    tabs.forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = selected == label,
                            onClick = { selected = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Gold, selectedTextColor = Gold,
                                indicatorColor = Blood.copy(alpha = .22f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (selected) {
                "Campanha" -> CampaignScreen(Modifier.padding(padding), onNavigate = { selected = it })
                "Personagens" -> CharacterScreen(Modifier.padding(padding))
                "Sistemas" -> SystemBuilderScreen(Modifier.padding(padding))
                "Bestiário" -> SimpleLibraryScreen(Modifier.padding(padding), "bestiary", "Bestiário", "Cadastre e gerencie criaturas")
                "Grimório" -> SimpleLibraryScreen(Modifier.padding(padding), "spells", "Grimório", "Cadastre e gerencie magias")
                else -> SimpleLibraryScreen(Modifier.padding(padding), "items", "Itens", "Cadastre armas, armaduras e tesouros")
            }
        }
    }
}

@Composable
private fun CampaignScreen(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    var campaignName by remember { mutableStateOf(prefs.getString("active_campaign_name", "As Cinzas do Reino") ?: "As Cinzas do Reino") }
    var campaignDescription by remember { mutableStateOf(prefs.getString("active_campaign_description", "O reino caiu. A escuridão despertou.") ?: "") }
    var selectedSystem by remember { mutableStateOf(prefs.getString("active_campaign_system", "D&D 5e") ?: "D&D 5e") }
    var systemMenuExpanded by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    val systemOptions = listOf("D&D 5e", "D&D 2024", "Vampiro", "Sistema próprio", "Outro sistema")
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("RPG", color = Gold, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                    Text("CRÔNICAS NAS SOMBRAS", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                }
                Image(painter = painterResource(id = R.drawable.rpg_crest), contentDescription = "Brasão RPG", modifier = Modifier.size(68.dp))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CAMPANHA ATUAL", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(campaignName, color = Parchment, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text(campaignDescription, color = Muted, fontSize = 13.sp)
                    InfoPill(selectedSystem)
                    Button(onClick = { editing = true }, colors = ButtonDefaults.buttonColors(containerColor = Blood), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Editar campanha")
                    }
                }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("SISTEMA DE REGRAS", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                    Text("Cada campanha pode usar um sistema diferente.", color = Muted, fontSize = 12.sp)
                    Box {
                        OutlinedButton(onClick = { systemMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(selectedSystem, modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ExpandMore, contentDescription = "Selecionar sistema")
                        }
                        DropdownMenu(expanded = systemMenuExpanded, onDismissRequest = { systemMenuExpanded = false }) {
                            systemOptions.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = {
                                    selectedSystem = option
                                    prefs.edit().putString("active_campaign_system", option).apply()
                                    systemMenuExpanded = false
                                })
                            }
                        }
                    }
                    OutlinedButton(onClick = { onNavigate("Sistemas") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir construtor de sistemas")
                    }
                }
            }
        }
        item { SectionTitle("ACESSO RÁPIDO", "Abra e gerencie os conteúdos da mesa") }
        item { QuickCard(Icons.Default.Person, "Personagens", "Fichas e pontos de vida") { onNavigate("Personagens") } }
        item { QuickCard(Icons.Default.Pets, "Bestiário", "Monstros e criaturas") { onNavigate("Bestiário") } }
        item { QuickCard(Icons.Default.MenuBook, "Grimório", "Magias e poderes") { onNavigate("Grimório") } }
        item { QuickCard(Icons.Default.Inventory2, "Itens", "Equipamentos e tesouros") { onNavigate("Itens") } }
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

@Composable
private fun CharacterScreen(modifier: Modifier = Modifier) {
    LocalRecordsScreen(modifier, "characters", "Personagens", "Fichas, atributos e pontos de vida",
        listOf("Nome", "Classe", "Nível / PV", "Anotações"),
        listOf("Novo personagem", "Ex.: Hemomante", "Nível 1 • 10/10 PV", ""),
        listOf(LocalRecord("demo-davi", "Davi", "Hemomante", "Nível 2 • 20/20 PV • CA 13",
            "Ficha inicial demonstrativa. Edite os dados para personalizar seu personagem.")))
}

private data class LocalRecord(val id: String, val name: String, val category: String, val detail: String, val notes: String)

private fun readRecords(prefs: android.content.SharedPreferences, key: String, defaults: List<LocalRecord>): List<LocalRecord> {
    val raw = prefs.getString("records_$key", null) ?: return defaults
    return try {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            LocalRecord(o.optString("id"), o.optString("name"), o.optString("category"), o.optString("detail"), o.optString("notes"))
        }
    } catch (_: Exception) { defaults }
}

private fun saveRecords(prefs: android.content.SharedPreferences, key: String, records: List<LocalRecord>) {
    val arr = JSONArray()
    records.forEach { r ->
        arr.put(JSONObject().put("id", r.id).put("name", r.name).put("category", r.category)
            .put("detail", r.detail).put("notes", r.notes))
    }
    prefs.edit().putString("records_$key", arr.toString()).apply()
}

@Composable
private fun SimpleLibraryScreen(modifier: Modifier, key: String, title: String, subtitle: String) {
    val defaults = when (key) {
        "bestiary" -> listOf(LocalRecord("sample-monster", "Lobo sombrio", "Monstro", "PV 12 • Defesa 12", "Criatura demonstrativa editável."))
        "spells" -> listOf(LocalRecord("sample-spell", "Chama sombria", "Nível 1 • Evocação", "Alcance: 18 m", "Magia demonstrativa editável."))
        else -> listOf(LocalRecord("sample-item", "Poção de cura", "Consumível", "Peso: 0,5 • Quantidade: 1", "Item demonstrativo editável."))
    }
    LocalRecordsScreen(modifier, key, title, subtitle,
        listOf("Nome", "Tipo / categoria", "Resumo / valores", "Descrição / notas"),
        when (key) {
            "bestiary" -> listOf("Nova criatura", "Monstro", "PV 10 • Defesa 10", "")
            "spells" -> listOf("Nova magia", "Nível 1 • Escola", "Alcance / dano", "")
            else -> listOf("Novo item", "Equipamento", "Peso / bônus / quantidade", "")
        }, defaults)
}

@Composable
private fun LocalRecordsScreen(
    modifier: Modifier,
    key: String,
    title: String,
    subtitle: String,
    fieldLabels: List<String>,
    placeholders: List<String>,
    defaults: List<LocalRecord> = emptyList()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val records = remember(key) { mutableStateListOf<LocalRecord>().apply { addAll(readRecords(prefs, key, defaults)) } }
    var query by remember { mutableStateOf("") }
    var dialogOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<LocalRecord?>(null) }

    fun persist() = saveRecords(prefs, key, records.toList())
    fun openEditor(record: LocalRecord?) {
        editingId = record?.id
        name = record?.name ?: placeholders[0]
        category = record?.category ?: placeholders[1]
        detail = record?.detail ?: placeholders[2]
        notes = record?.notes ?: placeholders[3]
        dialogOpen = true
    }

    LazyColumn(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp)) {
        item {
            Text(title.uppercase(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(subtitle, color = Parchment, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
        item {
            OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true)
        }
        item {
            Button(onClick = { openEditor(null) }, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Blood)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar")
            }
        }
        val filtered = records.filter {
            listOf(it.name, it.category, it.detail, it.notes).any { value -> value.contains(query, ignoreCase = true) }
        }
        if (filtered.isEmpty()) {
            item { Text(if (query.isBlank()) "Nenhum registro ainda. Toque em Adicionar para começar." else "Nenhum resultado para essa busca.", color = Muted) }
        }
        items(filtered.size) { index ->
            val record = filtered[index]
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(record.name, color = Parchment, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        if (record.category.isNotBlank()) InfoPill(record.category)
                        if (record.detail.isNotBlank()) InfoPill(record.detail)
                    }
                    if (record.notes.isNotBlank()) Text(record.notes, color = Muted, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { openEditor(record) }) { Icon(Icons.Default.Edit, null); Text("Editar") }
                        TextButton(onClick = { deleteTarget = record }) { Icon(Icons.Default.DeleteOutline, null); Text("Excluir") }
                    }
                }
            }
        }
    }
    if (dialogOpen) {
        AlertDialog(
            onDismissRequest = { dialogOpen = false },
            title = { Text(if (editingId == null) "Novo registro" else "Editar registro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(fieldLabels[0]) }, singleLine = true)
                    OutlinedTextField(category, { category = it }, label = { Text(fieldLabels[1]) }, singleLine = true)
                    OutlinedTextField(detail, { detail = it }, label = { Text(fieldLabels[2]) }, singleLine = true)
                    OutlinedTextField(notes, { notes = it }, label = { Text(fieldLabels[3]) }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        val record = LocalRecord(editingId ?: UUID.randomUUID().toString(), name.trim(),
                            category.trim(), detail.trim(), notes.trim())
                        val index = records.indexOfFirst { it.id == record.id }
                        if (index >= 0) records[index] = record else records.add(0, record)
                        persist()
                        dialogOpen = false
                    }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { dialogOpen = false }) { Text("Cancelar") } }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir ${target.name}?") },
            text = { Text("Essa ação remove o registro deste aparelho.") },
            confirmButton = {
                TextButton(onClick = {
                    records.removeAll { it.id == target.id }
                    persist()
                    deleteTarget = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SystemBuilderScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    var saveMessage by remember { mutableStateOf("") }
    var systemName by remember { mutableStateOf(prefs.getString("custom_system_name", "Meu sistema") ?: "Meu sistema") }
    var description by remember { mutableStateOf(prefs.getString("custom_system_description", "Regras personalizadas da campanha") ?: "Regras personalizadas da campanha") }
    var newAttribute by remember { mutableStateOf("") }
    var newResource by remember { mutableStateOf("") }
    var newSkill by remember { mutableStateOf("") }
    var testFormula by remember { mutableStateOf(prefs.getString("custom_test_formula", "1d20 + atributo + bônus") ?: "1d20 + atributo + bônus") }
    var initiativeFormula by remember { mutableStateOf(prefs.getString("custom_initiative_formula", "1d20 + DES") ?: "1d20 + DES") }
    val attributes = remember { mutableStateListOf<String>().apply { addAll(loadStringList(prefs.getString("custom_attributes", null), listOf("FOR", "DES", "CON", "INT", "SAB", "CAR"))) } }
    val resources = remember { mutableStateListOf<String>().apply { addAll(loadStringList(prefs.getString("custom_resources", null), listOf("Pontos de vida", "Mana"))) } }
    val skills = remember { mutableStateListOf<String>().apply { addAll(loadStringList(prefs.getString("custom_skills", null), listOf("Atletismo", "Percepção"))) } }
    val dice = listOf("d4", "d6", "d8", "d10", "d12", "d20", "d100")
    val selectedDice = remember { mutableStateListOf<String>().apply { addAll(loadStringList(prefs.getString("custom_dice", null), dice)) } }

    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("FERRAMENTAS DO MESTRE", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Construtor de sistemas", color = Parchment, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text("Crie regras e fichas sem ficar preso a um RPG específico.", color = Muted, fontSize = 12.sp)
        }
        item {
            EditorCard("IDENTIDADE DO SISTEMA", "Nome e descrição") {
                OutlinedTextField(value = systemName, onValueChange = { systemName = it }, label = { Text("Nome do sistema") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            }
        }
        item {
            EditorCard("ATRIBUTOS", "Características usadas nas fichas") {
                attributes.forEach { value -> EditableEntry(value) { attributes.remove(value) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newAttribute, onValueChange = { newAttribute = it }, label = { Text("Novo atributo") }, modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = { val v = newAttribute.trim(); if (v.isNotEmpty() && !attributes.contains(v)) attributes.add(v); newAttribute = "" }) { Icon(Icons.Default.Add, "Adicionar atributo", tint = Gold) }
                }
            }
        }
        item {
            EditorCard("RECURSOS", "Valores que podem aumentar ou diminuir") {
                resources.forEach { value -> EditableEntry(value) { resources.remove(value) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newResource, onValueChange = { newResource = it }, label = { Text("Novo recurso") }, modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = { val v = newResource.trim(); if (v.isNotEmpty() && !resources.contains(v)) resources.add(v); newResource = "" }) { Icon(Icons.Default.Add, "Adicionar recurso", tint = Gold) }
                }
            }
        }
        item {
            EditorCard("PERÍCIAS", "Especializações ligadas aos atributos") {
                skills.forEach { value -> EditableEntry(value) { skills.remove(value) } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newSkill, onValueChange = { newSkill = it }, label = { Text("Nova perícia") }, modifier = Modifier.weight(1f), singleLine = true)
                    IconButton(onClick = { val v = newSkill.trim(); if (v.isNotEmpty() && !skills.contains(v)) skills.add(v); newSkill = "" }) { Icon(Icons.Default.Add, "Adicionar perícia", tint = Gold) }
                }
            }
        }
        item {
            EditorCard("DADOS DISPONÍVEIS", "Selecione os dados usados pelo sistema") {
                dice.chunked(4).forEach { rowDice ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        rowDice.forEach { die ->
                            FilterChip(
                                selected = selectedDice.contains(die),
                                onClick = { if (selectedDice.contains(die)) selectedDice.remove(die) else selectedDice.add(die) },
                                label = { Text(die) }
                            )
                        }
                    }
                }
            }
        }
        item {
            EditorCard("FÓRMULAS", "Defina como os testes são calculados") {
                OutlinedTextField(value = testFormula, onValueChange = { testFormula = it }, label = { Text("Fórmula de teste") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = initiativeFormula, onValueChange = { initiativeFormula = it }, label = { Text("Fórmula de iniciativa") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Exemplo: 1d20 + FOR + proficiência. A validação e execução das fórmulas será implementada no motor de regras.", color = Muted, fontSize = 11.sp)
            }
        }
        item {
            Button(
                onClick = {
                    val json = JSONObject().apply {
                        put("name", systemName)
                        put("description", description)
                        put("attributes", JSONArray(attributes))
                        put("resources", JSONArray(resources))
                        put("skills", JSONArray(skills))
                        put("dice", JSONArray(selectedDice))
                        put("testFormula", testFormula)
                        put("initiativeFormula", initiativeFormula)
                    }
                    prefs.edit()
                        .putString("custom_system_json", json.toString())
                        .putString("custom_system_name", systemName)
                        .putString("custom_system_description", description)
                        .putString("custom_test_formula", testFormula)
                        .putString("custom_initiative_formula", initiativeFormula)
                        .putString("custom_attributes", JSONArray(attributes).toString())
                        .putString("custom_resources", JSONArray(resources).toString())
                        .putString("custom_skills", JSONArray(skills).toString())
                        .putString("custom_dice", JSONArray(selectedDice).toString())
                        .apply()
                    saveMessage = "Sistema salvo neste aparelho."
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Blood)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Salvar sistema neste aparelho")
            }
            if (saveMessage.isNotBlank()) Text(saveMessage, color = Gold, fontSize = 12.sp)
        }
        item {
            EditorCard("RESUMO DA FICHA", "Estrutura que será usada neste sistema") {
                Text("${attributes.size} atributos • ${resources.size} recursos • ${skills.size} perícias • ${selectedDice.size} tipos de dado", color = Parchment, fontWeight = FontWeight.SemiBold)
                Text("Campos personalizados e regras de combate poderão ser adicionados sem alterar o núcleo do aplicativo.", color = Muted, fontSize = 11.sp)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel2), shape = RoundedCornerShape(12.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Gold)
                    Spacer(Modifier.width(10.dp))
                    Text("Os sistemas agora podem ser salvos localmente neste aparelho. Sincronização entre contas, publicação comunitária, versionamento e execução segura de fórmulas serão etapas futuras.", color = Parchment, fontSize = 11.sp)
                }
            }
        }
    }
}

private fun loadStringList(raw: String?, fallback: List<String>): List<String> {
    if (raw.isNullOrBlank()) return fallback
    return try {
        val array = JSONArray(raw)
        List(array.length()) { index -> array.getString(index) }
    } catch (_: Exception) { fallback }
}

@Composable
private fun EditorCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(subtitle, color = Muted, fontSize = 11.sp)
            content()
        }
    }
}

@Composable
private fun EditableEntry(value: String, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(value, color = Parchment, modifier = Modifier.weight(1f), fontSize = 13.sp)
        IconButton(onClick = onRemove) { Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir $value", tint = Muted) }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun InfoPill(text: String) {
    Surface(color = Panel2, shape = RoundedCornerShape(50)) {
        Text(text, color = Parchment, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun QuickCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, onClick: () -> Unit = {}) {
    Card(modifier = Modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Parchment, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, fontSize = 11.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}
