package com.example.rpgsombrio.pixelstudio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.Gold
import com.example.rpgsombrio.Muted
import com.example.rpgsombrio.Night
import com.example.rpgsombrio.Panel
import com.example.rpgsombrio.Panel2
import com.example.rpgsombrio.Parchment
import java.io.File

private enum class StudioTool { PENCIL, ERASER, FILL, PICKER }

@Composable
internal fun PixelStudioScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    var section by rememberSaveable { mutableStateOf("Editor") }
    var document by remember { mutableStateOf(PixelTemplates.humanoid()) }
    val history = remember { PixelHistory(initial = document) }
    var selectedColor by remember { mutableIntStateOf(PixelPalettes.default[5]) }
    var tool by remember { mutableStateOf(StudioTool.PENCIL) }
    var showGrid by rememberSaveable { mutableStateOf(true) }
    var status by remember { mutableStateOf("Base humanoide pronta para editar") }
    val context = LocalContext.current
    val repository = remember { PixelProjectRepository(File(context.filesDir, "pixelstudio/projects")) }
    var projectFiles by remember { mutableStateOf(repository.list()) }

    fun commit(next: PixelDocument) {
        if (history.commit(next)) document = history.current
    }
    fun saveProject() {
        runCatching {
            repository.save(document)
            projectFiles = repository.list()
            status = "Projeto salvo com segurança"
        }.onFailure { status = "Falha ao salvar: ${it.message}" }
    }

    Column(modifier.fillMaxSize().background(Night)) {
        Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal = 8.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Voltar", tint = Parchment) }
            Column(Modifier.weight(1f)) {
                Text("PIXEL ART STUDIO", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(document.name, color = Parchment, fontSize = 16.sp)
            }
            IconButton(onClick = { if (history.canUndo) document = history.undo() ?: document }) { Icon(Icons.Default.Undo, "Desfazer", tint = Gold) }
            IconButton(onClick = { if (history.canRedo) document = history.redo() ?: document }) { Icon(Icons.Default.Redo, "Refazer", tint = Gold) }
            IconButton(onClick = { saveProject() }) { Icon(Icons.Default.Save, "Salvar", tint = Gold) }
        }
        Row(Modifier.fillMaxWidth().background(Panel2).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Personagens", "Biblioteca", "Editor", "Projetos").forEach { label ->
                TextButton(onClick = { section = label }) { Text(label, color = if (section == label) Gold else Muted) }
            }
        }
        when (section) {
            "Editor" -> {
                Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    StudioToolButton("Lápis", tool == StudioTool.PENCIL, { tool = StudioTool.PENCIL }, Icons.Default.Brush, Modifier.weight(1f))
                    StudioToolButton("Borracha", tool == StudioTool.ERASER, { tool = StudioTool.ERASER }, Icons.Default.Delete, Modifier.weight(1f))
                    StudioToolButton("Balde", tool == StudioTool.FILL, { tool = StudioTool.FILL }, Icons.Default.Palette, Modifier.weight(1f))
                    StudioToolButton("Conta-gotas", tool == StudioTool.PICKER, { tool = StudioTool.PICKER }, Icons.Default.Colorize, Modifier.weight(1f))
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
                    PixelCanvas(document, showGrid, tool, selectedColor, { selectedColor = it }, ::commit)
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("64 × 64 px", color = Muted, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Grade", color = Muted, fontSize = 12.sp)
                        Switch(checked = showGrid, onCheckedChange = { showGrid = it })
                    }
                    TextButton(onClick = {
                        val mirrored = PixelDrawing.mirrorHorizontal(document.layers.last())
                        commit(document.copy(layers = document.layers.dropLast(1) + mirrored))
                    }) { Text("Espelhar", color = Gold) }
                }
                Column(Modifier.fillMaxWidth().background(Panel).padding(12.dp)) {
                    Text("PALETA", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        document.palette.forEach { color ->
                            Box(Modifier.size(32.dp).background(Color(color), RoundedCornerShape(7.dp))
                                .border(if (selectedColor == color) 3.dp else 1.dp, if (selectedColor == color) Gold else Muted.copy(alpha = .4f), RoundedCornerShape(7.dp))
                                .clickable { selectedColor = color })
                        }
                    }
                    Text(status, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
            "Personagens" -> Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("MONTAGEM DE PERSONAGENS", color = Gold, fontWeight = FontWeight.Bold)
                Text("Comece com uma base humanoide 64×64. A montagem com modelos prontos será ampliada nas próximas etapas.", color = Parchment)
                Button(onClick = { document = PixelTemplates.humanoid(); history.commit(document); section = "Editor" }, colors = ButtonDefaults.buttonColors(containerColor = Panel2)) { Text("Criar personagem base") }
            }
            "Biblioteca" -> Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("BIBLIOTECA DE MODELOS", color = Gold, fontWeight = FontWeight.Bold)
                Text("Categorias preparadas para receber peças individuais e conjuntos completos editáveis. Os modelos ainda serão produzidos nas próximas etapas.", color = Parchment)
                listOf("Cabelos", "Rostos", "Roupas", "Armaduras", "Capacetes", "Armas", "Escudos", "Botas e luvas", "Capas e acessórios").forEach { category ->
                    Surface(color = Panel, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) { Text(category, color = Parchment, modifier = Modifier.padding(14.dp)) }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { Row(verticalAlignment = Alignment.CenterVertically) { Text("PROJETOS SALVOS", color = Gold, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onClick = ::saveProject) { Text("Salvar atual", color = Gold) } } }
                if (projectFiles.isEmpty()) item { Text("Nenhum projeto salvo ainda.", color = Muted) }
                items(projectFiles, key = { it.name }) { file ->
                    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(file.nameWithoutExtension, color = Parchment, fontWeight = FontWeight.SemiBold); Text("Projeto editável • 64×64", color = Muted, fontSize = 11.sp) }
                            TextButton(onClick = { runCatching { document = repository.load(file); history.commit(document); section = "Editor"; status = "Projeto carregado" }.onFailure { status = it.message ?: "Erro ao abrir projeto" } }) { Text("Abrir", color = Gold) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioToolButton(label: String, selected: Boolean, onClick: () -> Unit, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Surface(modifier.clickable(onClick = onClick), color = if (selected) Panel2 else Panel, shape = RoundedCornerShape(9.dp), border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Gold else Muted.copy(alpha = .25f))) {
        Column(Modifier.padding(vertical = 7.dp, horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, label, tint = Parchment, modifier = Modifier.size(18.dp))
            Text(label, color = Parchment, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun PixelCanvas(document: PixelDocument, showGrid: Boolean, tool: StudioTool, color: Int, onPick: (Int) -> Unit, onChange: (PixelDocument) -> Unit) {
    val latestDocument by rememberUpdatedState(document)
    Canvas(Modifier.fillMaxWidth().aspectRatio(1f).background(Color(0xFF292929), RoundedCornerShape(8.dp)).border(1.dp, Gold.copy(alpha = .7f), RoundedCornerShape(8.dp))
        .pointerInput(tool, color) {
            detectTapGestures { p ->
                val point = PixelViewport.screenToPixel(p.x, p.y, 0f, 0f, size.width) ?: return@detectTapGestures
                val current = latestDocument
                val layer = current.layers.last()
                when (tool) {
                    StudioTool.PICKER -> onPick(PixelDrawing.pick(layer, point.x, point.y))
                    StudioTool.FILL -> onChange(current.copy(layers = current.layers.dropLast(1) + PixelDrawing.fill(layer, point.x, point.y, color)))
                    StudioTool.PENCIL, StudioTool.ERASER -> onChange(current.copy(layers = current.layers.dropLast(1) + PixelDrawing.setPixel(layer, point.x, point.y, if (tool == StudioTool.ERASER) 0 else color)))
                }
            }
        }
        .pointerInput(tool, color) {
            detectDragGestures { change, _ ->
                if (tool == StudioTool.PENCIL || tool == StudioTool.ERASER) {
                    val point = PixelViewport.screenToPixel(change.position.x, change.position.y, 0f, 0f, size.width) ?: return@detectDragGestures
                    val current = latestDocument
                    val layer = current.layers.last()
                    onChange(current.copy(layers = current.layers.dropLast(1) + PixelDrawing.setPixel(layer, point.x, point.y, if (tool == StudioTool.ERASER) 0 else color)))
                }
            }
        }) {
        val composite = document.composite()
        val cell = size.width / 64f
        for (y in 0 until 64) for (x in 0 until 64) {
            val i = y * 64 + x
            val checker = if ((x + y) % 2 == 0) 0xFFB8B8B8.toInt() else 0xFFE0E0E0.toInt()
            drawRect(Color(checker), Offset(x * cell, y * cell), Size(cell + .2f, cell + .2f))
            val px = composite[i]
            if ((px ushr 24) != 0) drawRect(Color(px), Offset(x * cell, y * cell), Size(cell + .2f, cell + .2f))
            if (showGrid && cell >= 5f) {
                drawRect(Color.Black.copy(alpha = .12f), Offset(x * cell, y * cell), Size(cell, .5f))
                drawRect(Color.Black.copy(alpha = .12f), Offset(x * cell, y * cell), Size(.5f, cell))
            }
        }
    }
}
