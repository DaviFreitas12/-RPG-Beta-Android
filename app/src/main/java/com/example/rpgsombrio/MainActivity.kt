package com.example.rpgsombrio

import android.os.Bundle
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
                "Campanha" -> CampaignScreen(Modifier.padding(padding))
                "Personagens" -> CharacterScreen(Modifier.padding(padding))
                "Sistemas" -> SystemBuilderScreen(Modifier.padding(padding))
                "Bestiário" -> SimpleLibraryScreen(Modifier.padding(padding), "Bestiário", "Criaturas cadastradas", "Bláblá", "20 PV", "CA 9")
                "Grimório" -> SimpleLibraryScreen(Modifier.padding(padding), "Grimório", "Biblioteca de magias", "Nova magia", "Nível 1", "Evocação")
                else -> SimpleLibraryScreen(Modifier.padding(padding), "Itens", "Equipamentos e tesouros", "Novo item", "Arma", "Peso —")
            }
        }
    }
}

@Composable
private fun CampaignScreen(modifier: Modifier = Modifier) {
    var selectedSystem by remember { mutableStateOf("D&D 5e") }
    var systemMenuExpanded by remember { mutableStateOf(false) }
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
                Image(
                    painter = painterResource(id = R.drawable.rpg_crest),
                    contentDescription = "Brasão RPG",
                    modifier = Modifier.size(68.dp)
                )
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("CAMPANHA EM DESTAQUE", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("As Cinzas do Reino", color = Parchment, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    Text("O reino caiu. A escuridão despertou. O destino ainda pode ser reescrito.", color = Muted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoPill("1 personagem")
                        InfoPill("Sessão 01")
                    }
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Blood), modifier = Modifier.fillMaxWidth()) {
                        Text("Abrir campanha")
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
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = { selectedSystem = option; systemMenuExpanded = false }
                                )
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { /* acesso ao editor pela navegação inferior */ },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Tune, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Construtor de sistemas na aba Sistemas")
                    }
                    Text(
                        when (selectedSystem) {
                            "D&D 5e" -> "Atributos clássicos, níveis, classes e pontos de vida."
                            "D&D 2024" -> "Ficha preparada para a revisão de 2024."
                            "Vampiro" -> "Atributos e trilhas de recursos próprios do sistema."
                            "Sistema próprio" -> "Configure atributos, recursos, perícias e dados da sua mesa."
                            else -> "Use uma ficha neutra e personalize as regras."
                        }, color = Parchment, fontSize = 12.sp
                    )
                }
            }
        }
        item { SectionTitle("ACESSO RÁPIDO", "Tudo que sua mesa precisa") }
        item { QuickCard(Icons.Default.Person, "Personagens", "Fichas, atributos, vida e equipamentos") }
        item { QuickCard(Icons.Default.Pets, "Bestiário", "Monstros, estatísticas e habilidades") }
        item { QuickCard(Icons.Default.MenuBook, "Grimório", "Magias, níveis, alcance e efeitos") }
        item { QuickCard(Icons.Default.Inventory2, "Itens", "Armas, armaduras, tesouros e peso") }
        item {
            Text("PRÉVIA • DADOS DE DEMONSTRAÇÃO", color = Muted, fontSize = 9.sp, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun CharacterScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    var currentHp by remember { mutableIntStateOf(prefs.getInt("davi_hp", 20).coerceIn(0, 20)) }
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("PERSONAGEM", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Davi", color = Parchment, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text("Hemomante  •  Nível 2", color = Muted, fontSize = 14.sp)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("PONTOS DE VIDA", color = Gold, fontSize = 10.sp, letterSpacing = 1.5.sp)
                            Text("$currentHp / 20", color = Parchment, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        }
                        Surface(color = Panel2, shape = RoundedCornerShape(10.dp)) {
                            Text("CA 13", Modifier.padding(horizontal = 14.dp, vertical = 10.dp), color = Gold, fontWeight = FontWeight.Bold)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { currentHp / 20f },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = Blood,
                        trackColor = Panel2
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { currentHp = (currentHp - 1).coerceAtLeast(0); prefs.edit().putInt("davi_hp", currentHp).apply() }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Remove, null); Text(" 1 PV")
                        }
                        Button(onClick = { currentHp = (currentHp + 1).coerceAtMost(20); prefs.edit().putInt("davi_hp", currentHp).apply() }, modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Blood)) {
                            Icon(Icons.Default.Add, null); Text(" 1 PV")
                        }
                    }
                }
            }
        }
        item { SectionTitle("ATRIBUTOS", "Valores e modificadores") }
        item {
            val attrs = listOf("FOR" to "8", "DES" to "12", "CON" to "9", "INT" to "17", "SAB" to "10", "CAR" to "10")
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                attrs.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { (name, value) ->
                            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Panel2), shape = RoundedCornerShape(12.dp)) {
                                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(name, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(value, color = Parchment, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                                    val modifierValue = (value.toInt() - 10) / 2
                                    Text(if (modifierValue >= 0) "+$modifierValue" else "$modifierValue", color = Gold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
        item { SectionTitle("INFORMAÇÕES", "Resumo da ficha") }
        item { QuickCard(Icons.Default.Backpack, "Inventário", "Itens carregados e equipamentos") }
        item { QuickCard(Icons.Default.AutoAwesome, "Habilidades", "Características e poderes") }
        item { QuickCard(Icons.Default.Notes, "Anotações", "História, objetivos e observações") }
        item { Text("Os valores ausentes são apenas demonstração e poderão ser editados na ficha completa.", color = Muted, fontSize = 11.sp) }
    }
}

@Composable
private fun SimpleLibraryScreen(modifier: Modifier, title: String, subtitle: String, itemName: String, field1: String, field2: String) {
    LazyColumn(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text(title.uppercase(), color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text(subtitle, color = Parchment, fontSize = 27.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(itemName, color = Parchment, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { InfoPill(field1); InfoPill(field2) }
                    Text("Esta seção receberá cadastro, edição, busca e detalhes completos na próxima etapa.", color = Muted, fontSize = 12.sp)
                }
            }
        }
        item {
            OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar")
            }
        }
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
private fun QuickCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(12.dp)) {
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
