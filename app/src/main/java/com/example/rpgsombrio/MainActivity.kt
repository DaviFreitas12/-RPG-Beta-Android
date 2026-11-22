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
import androidx.compose.ui.draw.clip
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.data.systemOptions

// Paleta "Meia-noite": fundo azul-ardósia profundo, superfícies suaves e um único destaque índigo.
internal val Night = Color(0xFF0E1016)       // fundo
internal val Panel = Color(0xFF171A23)       // cartões
internal val Panel2 = Color(0xFF212633)      // superfícies elevadas, campos, pílulas
internal val Brand = Color(0xFF5B6CFF)       // botões principais
internal val Accent = Color(0xFFA5B4FF)      // títulos de seção, ícones e destaques
internal val Ink = Color(0xFFEEF0F8)         // texto principal
internal val Muted = Color(0xFF9199AE)       // texto secundário
internal val Danger = Color(0xFFFF6B7A)      // dano e alertas
internal val Hairline = Color(0x1AFFFFFF)    // contornos discretos dos cartões

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
internal fun importPortrait(context: Context, source: Uri): String? {
    val bitmap = decodeScaled(context, source, PORTRAIT_MAX_PX) ?: return null
    val dir = File(context.filesDir, "portraits").apply { mkdirs() }
    val file = File(dir, "${UUID.randomUUID()}.jpg")
    return runCatching {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        Uri.fromFile(file).toString()
    }.getOrNull()
}

/** Remove o arquivo do retrato somente se ele foi criado pelo app (nunca arquivos da galeria). */
internal fun deletePortraitFile(context: Context, uriString: String) {
    if (uriString.isBlank()) return
    runCatching {
        val path = Uri.parse(uriString).takeIf { it.scheme == "file" }?.path ?: return
        val file = File(path)
        if (file.parentFile == File(context.filesDir, "portraits")) file.delete()
    }
}

@Composable
internal fun Portrait(uriString: String, name: String, size: androidx.compose.ui.unit.Dp = 64.dp) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val bitmap by produceState<Bitmap?>(null, uriString) {
        value = withContext(Dispatchers.IO) { decodeScaled(context, Uri.parse(uriString), 256) }
    }
    bitmap?.let {
        Image(bitmap = it.asImageBitmap(), contentDescription = "Retrato de $name",
            modifier = Modifier.size(size).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Crop)
    }
}

private val LIBRARY_PAGES = listOf("Bestiário", "Grimório", "Itens", "Traços")

@Composable
private fun RpgApp() {
    var selected by rememberSaveable { mutableStateOf("Campanha") }
    BackHandler(enabled = selected in LIBRARY_PAGES) { selected = "Biblioteca" }
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, onPrimary = Night,
            secondary = Brand, onSecondary = Color.White,
            tertiary = Accent,
            background = Night, onBackground = Ink,
            surface = Panel, onSurface = Ink,
            surfaceVariant = Panel2, onSurfaceVariant = Muted,
            surfaceTint = Color.Transparent,
            surfaceContainerLowest = Night, surfaceContainerLow = Panel, surfaceContainer = Panel,
            surfaceContainerHigh = Panel2, surfaceContainerHighest = Panel2,
            primaryContainer = Panel2, onPrimaryContainer = Ink,
            secondaryContainer = Brand.copy(alpha = .35f), onSecondaryContainer = Ink,
            outline = Color(0xFF3B4256), outlineVariant = Hairline,
            error = Danger
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
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
                            selected = selected == label || (label == "Biblioteca" && selected in LIBRARY_PAGES),
                            onClick = { selected = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                indicatorColor = Brand.copy(alpha = .4f),
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
                "Personagens" -> CharacterSheetsScreen(Modifier.padding(padding))
                "Sistemas" -> SystemsScreen(Modifier.padding(padding))
                "Motor" -> RpgEngineScreen(Modifier.padding(padding))
                "Biblioteca" -> LibraryHubScreen(Modifier.padding(padding), onNavigate = { selected = it })
                "Bestiário" -> BestiaryScreen(Modifier.padding(padding))
                "Grimório" -> SpellbookScreen(Modifier.padding(padding))
                "Traços" -> TraitsScreen(Modifier.padding(padding))
                else -> ItemsScreen(Modifier.padding(padding))
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
            Text("ARQUIVOS DA MESA", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Text("Biblioteca", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Conteúdos reutilizáveis da campanha, organizados em um só lugar.", color = Muted, fontSize = 12.sp)
        }
        item { QuickCard(Icons.Default.Pets, "Bestiário", "Criaturas, estatísticas e ameaças") { onNavigate("Bestiário") } }
        item { QuickCard(Icons.Default.MenuBook, "Grimório", "Magias, poderes e efeitos") { onNavigate("Grimório") } }
        item { QuickCard(Icons.Default.Inventory2, "Itens", "Armas, armaduras, consumíveis e tesouros") { onNavigate("Itens") } }
        item { QuickCard(Icons.Default.Star, "Vantagens e desvantagens", "Traços com custo em pontos, por sistema") { onNavigate("Traços") } }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel2), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LIGADO AO MOTOR", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Text("Criaturas rolam ações e entram no combate com PV e iniciativa próprios; magias rolam o efeito e debitam o custo na ficha; itens equipados com dano viram ataques. Próximo passo: itens equipados alterarem atributos e CA.", color = Ink, fontSize = 12.sp)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("RPG", color = Accent, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
                    Text("CRÔNICAS NAS SOMBRAS", color = Muted, fontSize = 11.sp, letterSpacing = 2.sp)
                }
                Image(painter = painterResource(id = R.drawable.rpg_crest), contentDescription = "Brasão RPG", modifier = Modifier.size(68.dp))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)) {
                Column(
                    Modifier.background(Brush.linearGradient(listOf(Brand.copy(alpha = .38f), Panel))).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("CAMPANHA ATUAL", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
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
                    Text("SISTEMA DE REGRAS", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
                    Text("Novas fichas nascem neste sistema. Cada ficha pode ser vinculada a outro depois.", color = Muted, fontSize = 12.sp)
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
internal fun EditorCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text(title, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
            Text(subtitle, color = Muted, fontSize = 12.sp)
            content()
        }
    }
}

@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.7.sp)
        Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

@Composable
internal fun InfoPill(text: String) {
    Surface(color = Panel2, shape = RoundedCornerShape(50)) {
        Text(text, color = Ink, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

@Composable
private fun QuickCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, onClick: () -> Unit = {}) {
    Card(modifier = Modifier.clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Hairline)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = Ink, fontWeight = FontWeight.SemiBold)
                Text(description, color = Muted, fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Muted)
        }
    }
}
