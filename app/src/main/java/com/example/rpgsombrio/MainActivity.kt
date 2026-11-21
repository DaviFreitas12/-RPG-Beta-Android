package com.example.rpgsombrio

import android.os.Bundle
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID

private val Night = Color(0xFF17120F)       // fundo: madeira escura
private val Panel = Color(0xFF2B211A)       // painéis: couro
private val Panel2 = Color(0xFF3B2B20)      // painéis elevados
private val Blood = Color(0xFF7D2934)       // vinho medieval
private val Gold = Color(0xFFD2AE68)        // filetes e destaques
private val Parchment = Color(0xFFF0E2C4)   // texto de pergaminho
private val Muted = Color(0xFFC0AD91)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Night.toArgb()),
            navigationBarStyle = SystemBarStyle.dark(Night.toArgb())
        )
        setContent { RpgApp() }
    }
}

private const val PORTRAIT_MAX_PX = 512

/** Decodifica a imagem já reduzida (sem carregar o original inteiro na memória). */
private fun decodeScaled(context: Context, uri: Uri, maxPx: Int): Bitmap? = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= maxPx && bounds.outHeight / (sample * 2) >= maxPx) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
}.getOrNull()

/** Copia o retrato escolhido para o armazenamento do app, reduzido; o URI da galeria deixa de ser necessário. */
private fun importPortrait(context: Context, source: Uri): String? {
    val bitmap = decodeScaled(context, source, PORTRAIT_MAX_PX) ?: return null
    val dir = File(context.filesDir, "portraits").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    return runCatching {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        Uri.fromFile(file).toString()
    }.getOrNull()
}

/** Remove o arquivo do retrato somente se ele foi criado pelo app (nunca arquivos da galeria). */
private fun deletePortraitFile(context: Context, uriString: String) {
    if (uriString.isBlank()) return
    runCatching {
        val path = Uri.parse(uriString).takeIf { it.scheme == "file" }?.path ?: return
        val file = File(path)
        if (file.parentFile == File(context.filesDir, "portraits")) file.delete()
    }
}

@Composable
private fun Portrait(uriString: String, name: String) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by produceState<Bitmap?>(null, uriString) {
        value = withContext(Dispatchers.IO) { decodeScaled(context, Uri.parse(uriString), 256) }
    }
    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = "Retrato de $name",
            modifier = Modifier.size(64.dp), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun RpgApp() {
    var selected by rememberSaveable { mutableStateOf("Campanha") }
    BackHandler(enabled = selected in listOf("Bestiário", "Grimório", "Itens")) { selected = "Biblioteca" }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Gold, onPrimary = Night,
            secondary = Blood, onSecondary = Parchment,
            background = Night, onBackground = Parchment,
            surface = Panel, onSurface = Parchment,
            surfaceVariant = Panel2, onSurfaceVariant = Muted,
            primaryContainer = Panel2, onPrimaryContainer = Parchment,
            secondaryContainer = Blood.copy(alpha = .35f), onSecondaryContainer = Parchment,
            outline = Gold.copy(alpha = .55f), outlineVariant = Gold.copy(alpha = .25f)
        ),
        shapes = Shapes(
            small = RoundedCornerShape(10.dp),
            medium = RoundedCornerShape(14.dp),
            large = RoundedCornerShape(18.dp)
        )
    ) {
        Scaffold(
            containerColor = Night,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    // Cinco destinos principais evitam que os itens fiquem cortados em celulares estreitos.
                    val tabs = listOf(
                        "Campanha" to Icons.Default.Home,
                        "Personagens" to Icons.Default.Person,
                        "Motor" to Icons.Default.Casino,
                        "Biblioteca" to Icons.Default.MenuBook,
                        "Sistemas" to Icons.Default.Tune
                    )
                    tabs.forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = selected == label || (label == "Biblioteca" && selected in listOf("Bestiário", "Grimório", "Itens")),
                            onClick = { selected = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 10.sp, maxLines = 1) },
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
                "Motor" -> RpgEngineScreen(Modifier.padding(padding))
                "Biblioteca" -> LibraryHubScreen(Modifier.padding(padding), onNavigate = { selected = it })
                "Bestiário" -> SimpleLibraryScreen(Modifier.padding(padding), "bestiary", "Bestiário", "Cadastre e gerencie criaturas")
                "Grimório" -> SimpleLibraryScreen(Modifier.padding(padding), "spells", "Grimório", "Cadastre e gerencie magias")
                else -> SimpleLibraryScreen(Modifier.padding(padding), "items", "Itens", "Cadastre armas, armaduras e tesouros")
            }
        }
    }
}

@Composable
private fun LibraryHubScreen(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("ARQUIVOS DA MESA", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Biblioteca", color = Parchment, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Conteúdos reutilizáveis da campanha, organizados em um só lugar.", color = Muted, fontSize = 12.sp)
        }
        item { QuickCard(Icons.Default.Pets, "Bestiário", "Criaturas, estatísticas e ameaças") { onNavigate("Bestiário") } }
        item { QuickCard(Icons.Default.MenuBook, "Grimório", "Magias, poderes e efeitos") { onNavigate("Grimório") } }
        item { QuickCard(Icons.Default.Inventory2, "Itens", "Armas, armaduras, consumíveis e tesouros") { onNavigate("Itens") } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel2), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .24f))) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("PRÓXIMA INTEGRAÇÃO", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("Os registros da biblioteca serão conectados às fichas e ao combate: itens equipados alteram atributos, magias executam efeitos e criaturas entram diretamente nos encontros.", color = Parchment, fontSize = 12.sp)
                }
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
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .32f))) {
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
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .26f))) {
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
        item { QuickCard(Icons.Default.Casino, "Motor de RPG", "Dados, testes e combate") { onNavigate("Motor") } }
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
            "Ficha inicial demonstrativa. Edite os dados para personalizar seu personagem.")),
        allowPortrait = true)
}

private data class LocalRecord(val id: String, val name: String, val category: String, val detail: String, val notes: String, val imageUri: String = "")

private fun readRecords(prefs: android.content.SharedPreferences, key: String, defaults: List<LocalRecord>): List<LocalRecord> {
    val raw = prefs.getString("records_$key", null) ?: return defaults
    return try {
        val arr = JSONArray(raw)
        List(arr.length()) { i ->
            val o = arr.getJSONObject(i)
            LocalRecord(o.optString("id"), o.optString("name"), o.optString("category"), o.optString("detail"), o.optString("notes"), o.optString("imageUri"))
        }
    } catch (_: Exception) { defaults }
}

private fun saveRecords(prefs: android.content.SharedPreferences, key: String, records: List<LocalRecord>) {
    val arr = JSONArray()
    records.forEach { r ->
        arr.put(JSONObject().put("id", r.id).put("name", r.name).put("category", r.category)
            .put("detail", r.detail).put("notes", r.notes).put("imageUri", r.imageUri))
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
    defaults: List<LocalRecord> = emptyList(),
    allowPortrait: Boolean = false
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
    var portraitUri by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    var portraitError by remember { mutableStateOf(false) }
    // Retratos escolhidos nesta edição que ainda não foram salvos; são apagados se o diálogo for cancelado.
    val pendingPortraits = remember { mutableListOf<String>() }
    val portraitPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val imported = withContext(Dispatchers.IO) { importPortrait(context, uri) }
            if (imported != null) {
                pendingPortraits.add(imported)
                portraitUri = imported
                portraitError = false
            } else portraitError = true
        }
    }
    fun discardPendingPortraits(keep: String = "") {
        pendingPortraits.filter { it != keep }.forEach { deletePortraitFile(context, it) }
        pendingPortraits.clear()
    }
    var deleteTarget by remember { mutableStateOf<LocalRecord?>(null) }

    fun persist() = saveRecords(prefs, key, records.toList())
    fun openEditor(record: LocalRecord?) {
        editingId = record?.id
        name = record?.name ?: placeholders[0]
        category = record?.category ?: placeholders[1]
        detail = record?.detail ?: placeholders[2]
        notes = record?.notes ?: placeholders[3]
        portraitUri = record?.imageUri ?: ""
        portraitError = false
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
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .22f))) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (allowPortrait && record.imageUri.isNotBlank()) Portrait(record.imageUri, record.name)
                        Column(Modifier.weight(1f)) {
                            Text(record.name, color = Parchment, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            if (record.category.isNotBlank()) Text(record.category, color = Gold, fontSize = 12.sp)
                        }
                    }
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
            onDismissRequest = { discardPendingPortraits(); dialogOpen = false },
            title = { Text(if (editingId == null) "Novo registro" else "Editar registro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(fieldLabels[0]) }, singleLine = true)
                    OutlinedTextField(category, { category = it }, label = { Text(fieldLabels[1]) }, singleLine = true)
                    OutlinedTextField(detail, { detail = it }, label = { Text(fieldLabels[2]) }, singleLine = true)
                    OutlinedTextField(notes, { notes = it }, label = { Text(fieldLabels[3]) }, minLines = 2)
                    if (allowPortrait) {
                        OutlinedButton(onClick = { portraitPicker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (portraitUri.isBlank()) "Escolher retrato da galeria" else "Trocar retrato")
                        }
                        if (portraitUri.isNotBlank()) Text("Imagem selecionada para esta ficha.", color = Gold, fontSize = 11.sp)
                        if (portraitError) Text("Não foi possível ler essa imagem.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        val record = LocalRecord(editingId ?: UUID.randomUUID().toString(), name.trim(),
                            category.trim(), detail.trim(), notes.trim(), portraitUri)
                        val index = records.indexOfFirst { it.id == record.id }
                        val previousImage = if (index >= 0) records[index].imageUri else ""
                        if (index >= 0) records[index] = record else records.add(0, record)
                        persist()
                        if (previousImage != portraitUri) deletePortraitFile(context, previousImage)
                        discardPendingPortraits(keep = portraitUri)
                        dialogOpen = false
                    }
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { discardPendingPortraits(); dialogOpen = false }) { Text("Cancelar") } }
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
                    deletePortraitFile(context, target.imageUri)
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

@Composable
private fun RpgEngineScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val engine = remember { RpgEngine() }
    var formula by remember { mutableStateOf(prefs.getString("engine_formula", "1d20 + FOR + PROF") ?: "1d20 + FOR + PROF") }
    var difficulty by remember { mutableStateOf("15") }
    var strength by remember { mutableStateOf("3") }
    var dexterity by remember { mutableStateOf("2") }
    var proficiency by remember { mutableStateOf("2") }
    var lastResult by remember { mutableStateOf("") }
    var resultIsSuccess by remember { mutableStateOf<Boolean?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var actorName by remember { mutableStateOf("") }
    var actorHp by remember { mutableStateOf("20") }
    var actorDefense by remember { mutableStateOf("13") }
    var actorInitiativeBonus by remember { mutableStateOf("0") }
    val combatants = remember {
        mutableStateListOf<Combatant>().apply {
            val raw = prefs.getString("engine_combatants", null)
            if (!raw.isNullOrBlank()) try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(Combatant(o.getString("id"), o.getString("name"), o.getInt("initiative"),
                        o.getInt("currentHp"), o.getInt("maxHp"), o.optInt("armorClass", 10),
                        loadStringList(o.optString("conditions"), emptyList())))
                }
            } catch (_: Exception) { clear() }
        }
    }
    // O turno ativo é guardado pelo id do combatente, não por posição: adicionar ou remover alguém não troca a vez.
    var activeId by remember { mutableStateOf(prefs.getString("engine_active_id", "") ?: "") }
    val ordered = combatants.sortedWith(
        compareByDescending<Combatant> { it.initiative }.thenBy { it.name.lowercase() }.thenBy { it.id }
    )
    val activeIndex = ordered.indexOfFirst { it.id == activeId }.coerceAtLeast(0)
    fun persistCombat() {
        val array = JSONArray()
        combatants.forEach { c -> array.put(JSONObject().put("id", c.id).put("name", c.name)
            .put("initiative", c.initiative).put("currentHp", c.currentHp).put("maxHp", c.maxHp)
            .put("armorClass", c.armorClass).put("conditions", JSONArray(c.conditions))) }
        prefs.edit().putString("engine_combatants", array.toString()).putString("engine_active_id", activeId).apply()
    }
    fun variables() = mapOf(
        "FOR" to (strength.toIntOrNull() ?: 0), "DES" to (dexterity.toIntOrNull() ?: 0),
        "PROF" to (proficiency.toIntOrNull() ?: 0), "CON" to 0, "INT" to 0, "SAB" to 0, "CAR" to 0
    )

    LazyColumn(modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("SISTEMA DE REGRAS", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Motor de RPG", color = Parchment, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Role dados, execute testes e controle a iniciativa e os pontos de vida da batalha.", color = Muted, fontSize = 12.sp)
        }
        item {
            EditorCard("ROLAGEM E TESTES", "Fórmulas executáveis: 1d20 + FOR + PROF, 2d6 + 3, (1d8 + FOR) * 2") {
                OutlinedTextField(value = formula, onValueChange = { formula = it }, label = { Text("Fórmula") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = strength, onValueChange = { strength = it }, label = { Text("FOR") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = dexterity, onValueChange = { dexterity = it }, label = { Text("DES") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = proficiency, onValueChange = { proficiency = it }, label = { Text("PROF") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = difficulty, onValueChange = { difficulty = it.filter(Char::isDigit) }, label = { Text("Classe de dificuldade (CD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(onClick = {
                    try {
                        val check = engine.check(formula, difficulty.toIntOrNull() ?: 0, variables())
                        lastResult = "${check.roll.total} • ${if (check.success) "SUCESSO" else "FALHA"} • margem ${if (check.margin >= 0) "+" else ""}${check.margin}\n${check.roll.naturalRolls.takeIf { it.isNotEmpty() }?.joinToString(" + ") ?: "Sem dados"}"
                        resultIsSuccess = check.success
                        errorMessage = ""
                        prefs.edit().putString("engine_formula", formula).apply()
                    } catch (e: Exception) { errorMessage = e.message ?: "Não foi possível executar a fórmula."; lastResult = ""; resultIsSuccess = null }
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blood)) {
                    Icon(Icons.Default.Casino, null); Spacer(Modifier.width(8.dp)); Text("Executar teste")
                }
                if (lastResult.isNotBlank()) Text(lastResult, color = if (resultIsSuccess == true) Gold else Parchment, fontWeight = FontWeight.Bold)
                if (errorMessage.isNotBlank()) Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                Text("Atalhos de rolagem", color = Muted, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("d4", "d6", "d8", "d10", "d12", "d20", "d100").forEach { die ->
                        OutlinedButton(onClick = {
                            try { val r = engine.roll(die); lastResult = "$die → ${r.total}"; resultIsSuccess = null; errorMessage = "" }
                            catch (e: Exception) { errorMessage = e.message ?: "Erro na rolagem" }
                        }, contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp)) { Text(die, fontSize = 11.sp) }
                    }
                }
            }
        }
        item {
            EditorCard("ORDEM DE INICIATIVA", "Adicione participantes e organize os turnos automaticamente") {
                OutlinedTextField(value = actorName, onValueChange = { actorName = it }, label = { Text("Nome do combatente") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = actorInitiativeBonus, onValueChange = { actorInitiativeBonus = it }, label = { Text("Bônus iniciativa") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = actorHp, onValueChange = { actorHp = it.filter(Char::isDigit) }, label = { Text("PV máximos") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = actorDefense, onValueChange = { actorDefense = it.filter(Char::isDigit) }, label = { Text("Defesa/CA") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Button(onClick = {
                    val hp = actorHp.toIntOrNull() ?: 0
                    if (actorName.isNotBlank() && hp > 0) {
                        val init = engine.roll("1d20 + ${actorInitiativeBonus.toIntOrNull() ?: 0}").total
                        combatants.add(Combatant(UUID.randomUUID().toString(), actorName.trim(), init, hp, hp, actorDefense.toIntOrNull() ?: 10))
                        persistCombat(); actorName = ""
                    } else errorMessage = "Informe um nome e PV máximos maiores que zero."
                }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blood)) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar e rolar iniciativa")
                }
                if (ordered.isNotEmpty()) {
                    Text("TURNO ${activeIndex + 1} DE ${ordered.size} • ${ordered[activeIndex].name}", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    ordered.forEachIndexed { index, fighter ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = if (index == activeIndex) Panel2 else Night),
                            border = if (index == activeIndex) androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .6f)) else null
                        ) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${index + 1}. ${fighter.name}${if (fighter.currentHp == 0) " (caído)" else ""}", color = if (fighter.currentHp == 0) Muted else Parchment, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                    InfoPill("INI ${fighter.initiative}")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("PV ${fighter.currentHp}/${fighter.maxHp} • CA ${fighter.armorClass}", color = Muted, modifier = Modifier.weight(1f), fontSize = 12.sp)
                                    IconButton(onClick = {
                                        val at = combatants.indexOfFirst { it.id == fighter.id }
                                        if (at >= 0) combatants[at] = combatants[at].copy(currentHp = (fighter.currentHp - 1).coerceAtLeast(0))
                                        persistCombat()
                                    }) { Icon(Icons.Default.Remove, "Aplicar 1 de dano", tint = Blood) }
                                    IconButton(onClick = {
                                        val at = combatants.indexOfFirst { it.id == fighter.id }
                                        if (at >= 0) combatants[at] = combatants[at].copy(currentHp = (fighter.currentHp + 1).coerceAtMost(fighter.maxHp))
                                        persistCombat()
                                    }) { Icon(Icons.Default.Add, "Curar 1 PV", tint = Gold) }
                                    IconButton(onClick = { combatants.removeAll { it.id == fighter.id }; persistCombat() }) { Icon(Icons.Default.DeleteOutline, "Remover", tint = Muted) }
                                }
                            }
                        }
                    }
                    EditorCard("RESOLVER ATAQUE", "Escolha atacante e alvo. O motor rola o acerto e aplica o dano automaticamente.") {
                        var attackerMenu by remember { mutableStateOf(false) }
                        var targetMenu by remember { mutableStateOf(false) }
                        var attackExpression by remember { mutableStateOf("1d20 + FOR + PROF") }
                        var damageExpression by remember { mutableStateOf("1d8 + FOR") }
                        var attackMessage by remember { mutableStateOf("") }
                        var attackError by remember { mutableStateOf("") }
                        var selectedAttackerId by remember { mutableStateOf("") }
                        var selectedTargetId by remember { mutableStateOf("") }
                        val currentAttacker = ordered.firstOrNull { it.id == selectedAttackerId } ?: ordered.firstOrNull()
                        val currentTarget = ordered.firstOrNull { it.id == selectedTargetId && it.id != currentAttacker?.id }
                            ?: ordered.firstOrNull { it.id != currentAttacker?.id }
                        Box {
                            OutlinedButton(onClick = { attackerMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Atacante: ${currentAttacker?.name ?: "Selecione"}", modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }
                            DropdownMenu(expanded = attackerMenu, onDismissRequest = { attackerMenu = false }) {
                                ordered.forEach { fighter -> DropdownMenuItem(text = { Text(fighter.name) }, onClick = { selectedAttackerId = fighter.id; attackerMenu = false }) }
                            }
                        }
                        Box {
                            OutlinedButton(onClick = { targetMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Alvo: ${currentTarget?.name ?: "Selecione"}", modifier = Modifier.weight(1f))
                                Icon(Icons.Default.ExpandMore, contentDescription = null)
                            }
                            DropdownMenu(expanded = targetMenu, onDismissRequest = { targetMenu = false }) {
                                ordered.filter { it.id != currentAttacker?.id }.forEach { fighter -> DropdownMenuItem(text = { Text(fighter.name) }, onClick = { selectedTargetId = fighter.id; targetMenu = false }) }
                            }
                        }
                        OutlinedTextField(value = attackExpression, onValueChange = { attackExpression = it }, label = { Text("Fórmula de ataque") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        OutlinedTextField(value = damageExpression, onValueChange = { damageExpression = it }, label = { Text("Fórmula de dano") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = {
                            val attacker = currentAttacker
                            val target = currentTarget
                            if (attacker == null || target == null) {
                                attackError = "Adicione pelo menos dois combatentes para resolver um ataque."
                                attackMessage = ""
                            } else try {
                                val result = engine.attack(attackExpression, damageExpression, target.armorClass, variables())
                                val targetIndex = combatants.indexOfFirst { it.id == target.id }
                                val damageRoll = result.damageRoll
                                if (damageRoll != null && targetIndex >= 0) {
                                    val updated = target.copy(currentHp = (target.currentHp - damageRoll.total).coerceAtLeast(0))
                                    combatants[targetIndex] = updated
                                    persistCombat()
                                    val kind = if (result.critical) "CRÍTICO! ${attacker.name} acertou" else "${attacker.name} acertou"
                                    attackMessage = "$kind ${target.name}! Ataque ${result.attackRoll.total} contra CA ${target.armorClass}; dano ${damageRoll.total}. PV restantes: ${updated.currentHp}/${updated.maxHp}."
                                } else {
                                    val kind = if (result.criticalMiss) "Falha crítica! ${attacker.name} errou" else "${attacker.name} errou"
                                    attackMessage = "$kind ${target.name}. Ataque ${result.attackRoll.total} contra CA ${target.armorClass}."
                                }
                                attackError = ""
                            } catch (e: Exception) {
                                attackError = e.message ?: "Não foi possível resolver o ataque."
                                attackMessage = ""
                            }
                        }, enabled = ordered.size >= 2, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Blood)) {
                            Icon(Icons.Default.Gavel, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Rolar ataque e aplicar dano")
                        }
                        if (attackMessage.isNotBlank()) Text(attackMessage, color = Gold, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        if (attackError.isNotBlank()) Text(attackError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            if (ordered.isNotEmpty()) {
                                // Avança para o próximo combatente em pé; se todos caíram, apenas passa a vez.
                                val steps = (1..ordered.size).map { (activeIndex + it) % ordered.size }
                                val next = steps.firstOrNull { ordered[it].currentHp > 0 } ?: steps.first()
                                activeId = ordered[next].id
                                persistCombat()
                            }
                        }, modifier = Modifier.weight(1f)) { Text("Próximo turno") }
                        OutlinedButton(onClick = { combatants.clear(); activeId = ""; persistCombat() }, modifier = Modifier.weight(1f)) { Text("Encerrar combate") }
                    }
                } else Text("Nenhum combatente na iniciativa.", color = Muted, fontSize = 12.sp)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel2)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Default.Info, null, tint = Gold)
                    Spacer(Modifier.width(10.dp))
                    Text("Núcleo executável da Beta 0.6.0: rolagens, fórmulas, testes contra CD, iniciativa, turnos, controle de PV e resolução de ataques com aplicação automática de dano. Os valores de atributos desta tela ainda são informados manualmente; a próxima etapa é conectá-los às fichas e aos adaptadores de cada sistema.", color = Parchment, fontSize = 11.sp)
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
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .28f))) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
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
    Card(modifier = Modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Gold.copy(alpha = .24f))) {
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
