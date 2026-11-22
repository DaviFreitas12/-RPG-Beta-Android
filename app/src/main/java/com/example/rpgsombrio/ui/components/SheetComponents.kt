package com.example.rpgsombrio.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.SystemOption
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel
import com.example.rpgsombrio.ui.theme.Panel2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal fun signed(n: Int): String = if (n >= 0) "+$n" else "$n"

internal fun <T> List<T>.replaced(index: Int, value: T): List<T> =
    toMutableList().also { if (index in it.indices) it[index] = value }

internal fun <T> List<T>.without(index: Int): List<T> = filterIndexed { i, _ -> i != index }

@Composable
internal fun ScreenHeader(kicker: String, title: String, subtitle: String = "") {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(kicker.uppercase(), color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Text(title, color = Ink, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 12.sp)
    }
}

/** Cabeçalho + busca + botão de adicionar, comum às três listas. */
internal fun LazyListScope.listHeader(
    kicker: String, title: String, subtitle: String,
    query: String, onQuery: (String) -> Unit,
    addLabel: String, onAdd: () -> Unit
) {
    item { ScreenHeader(kicker, title, subtitle) }
    item {
        OutlinedTextField(
            value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true
        )
    }
    item {
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(addLabel)
        }
    }
}

@Composable
internal fun EditorTopBar(title: String, onBack: () -> Unit, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = Accent) }
        Text(title, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, "Excluir", tint = Muted) }
    }
}

/** Seção recolhível. [summary] aparece sob o título quando a seção está fechada. */
@Composable
internal fun CollapsibleSection(
    title: String,
    summary: String = "",
    startExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by rememberSaveable(title) { mutableStateOf(startExpanded) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Hairline)
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    if (!expanded && summary.isNotBlank()) Text(summary, color = Muted, fontSize = 12.sp, maxLines = 1)
                }
                Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Recolher" else "Expandir", tint = Muted)
            }
            if (expanded) {
                Column(
                    Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp, bottom = 15.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) { content() }
            }
        }
    }
}

@Composable
internal fun TextInput(
    label: String, value: String, onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true, minLines: Int = 1
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) },
        modifier = modifier, singleLine = singleLine, minLines = minLines
    )
}

/** Campo numérico: permite apagar tudo ao digitar (vale 0) sem "travar" o texto. */
@Composable
internal fun NumberField(label: String, value: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(value.toString()) }
    LaunchedEffect(value) { if ((text.toIntOrNull() ?: 0) != value) text = value.toString() }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val cleaned = raw.filterIndexed { i, c -> c.isDigit() || (i == 0 && c == '-') }.take(6)
            text = cleaned
            onChange(cleaned.toIntOrNull() ?: 0)
        },
        label = { Text(label, maxLines = 1) },
        modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Composable
internal fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Accent, checkedTrackColor = Brand))
    }
}

@Composable
internal fun EmptyHint(text: String) {
    Text(text, color = Muted, fontSize = 12.sp)
}

@Composable
internal fun ChipFilterRow(options: List<String>, selected: String, onSelect: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(options) { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(option) })
        }
    }
}

@Composable
internal fun AddRowButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text(label)
    }
}

@Composable
internal fun ConfirmDeleteDialog(name: String, what: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Excluir ${name.ifBlank { "registro" }}?") },
        text = { Text("Essa ação remove $what deste aparelho.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Excluir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * Seletor de sistema em diálogo. [onPick] recebe o id escolhido (ou "" para "Todos os sistemas",
 * quando [allowAll] está ligado). [campaignId] marca o sistema da campanha.
 */
@Composable
internal fun SystemPickerDialog(
    title: String,
    options: List<SystemOption>,
    currentId: String,
    campaignId: String? = null,
    allowAll: Boolean = false,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    @Composable
    fun PickRow(name: String, note: String, selected: Boolean, onClick: () -> Unit) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp)) {
            Text(name, color = if (selected) Accent else Ink, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            if (note.isNotBlank()) Text(note, color = Muted, fontSize = 12.sp)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                if (allowAll) item { PickRow("Todos os sistemas", "Aparece em qualquer ficha", currentId.isBlank()) { onPick("") } }
                items(options, key = { it.id }) { option ->
                    val note = listOfNotNull(
                        if (option.id == campaignId) "Sistema da campanha" else null,
                        if (option.builtIn) "Modelo de fábrica" else "Criado por você"
                    ).joinToString(" • ")
                    PickRow(option.name, note, option.id == currentId) { onPick(option.id) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Grade de atributos (3 por linha) guiada pelo perfil do sistema. */
@Composable
internal fun AttributeGrid(
    profile: SystemProfile, values: Map<String, Int>,
    onRoll: ((SheetAttr) -> Unit)? = null,
    onChange: (String, Int) -> Unit
) {
    if (profile.attributes.isEmpty()) {
        EmptyHint("Este sistema ainda não tem atributos. Defina-os na aba Sistemas.")
        return
    }
    profile.attributes.chunked(3).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { attr ->
                val value = values[attr.key] ?: attr.default
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    NumberField(attr.abbr, value, { onChange(attr.key, attr.clamp(it)) }, Modifier.fillMaxWidth())
                    if (attr.hasModifier) {
                        Text(signed(attr.modifierOf(value)), color = Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    if (onRoll != null) {
                        IconButton(onClick = { onRoll(attr) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Casino, "Rolar ${attr.abbr}", tint = Accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

/** Retrato com escolha da galeria; o arquivo antigo (se criado pelo app) é apagado ao trocar. */
@Composable
internal fun PortraitField(imageUri: String, name: String, onChange: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestUri by rememberUpdatedState(imageUri)
    val latestChange by rememberUpdatedState(onChange)
    var error by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val imported = withContext(Dispatchers.IO) { importPortrait(context, uri) }
            if (imported != null) {
                deletePortraitFile(context, latestUri)
                latestChange(imported)
                error = false
            } else error = true
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (imageUri.isNotBlank()) {
            Portrait(imageUri, name, 84.dp)
        } else {
            Box(Modifier.size(84.dp).background(Panel2, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = Muted)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) {
                Text(if (imageUri.isBlank()) "Escolher retrato" else "Trocar retrato")
            }
            if (imageUri.isNotBlank()) {
                TextButton(onClick = { deletePortraitFile(context, imageUri); onChange("") }) { Text("Remover") }
            }
            if (error) Text("Não foi possível ler essa imagem.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}

private fun plainNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()

/** Campo decimal (aceita vírgula ou ponto), usado para peso. */
@Composable
internal fun DecimalField(label: String, value: Double, onChange: (Double) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(plainNumber(value)) }
    LaunchedEffect(value) {
        if ((text.replace(',', '.').toDoubleOrNull() ?: 0.0) != value) text = plainNumber(value)
    }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            val cleaned = raw.filter { it.isDigit() || it == '.' || it == ',' }.take(8)
            if (cleaned.count { it == '.' || it == ',' } <= 1) {
                text = cleaned
                onChange(cleaned.replace(',', '.').toDoubleOrNull() ?: 0.0)
            }
        },
        label = { Text(label, maxLines = 1) },
        modifier = modifier, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

/** Guarda a rolagem mais recente no topo e limita o histórico a 20 linhas. */
internal fun MutableList<RollLine>.push(line: RollLine) {
    add(0, line)
    while (size > 20) removeAt(lastIndex)
}

/** Barra fixa no rodapé das telas com o resultado das últimas rolagens (a mais nova em destaque). */
@Composable
internal fun RollLogBar(log: List<RollLine>, onClear: () -> Unit) {
    if (log.isEmpty()) return
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = Panel2),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Accent.copy(alpha = .5f))
    ) {
        Row(Modifier.padding(start = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f).padding(top = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                log.take(4).forEachIndexed { index, line ->
                    val color = when {
                        index > 0 -> Muted
                        line.good == true -> Accent
                        line.good == false -> MaterialTheme.colorScheme.error
                        else -> Ink
                    }
                    Text(
                        line.text, color = color,
                        fontSize = if (index == 0) 13.sp else 11.sp,
                        fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
            IconButton(onClick = onClear) { Icon(Icons.Default.Close, "Limpar rolagens", tint = Muted) }
        }
    }
}

/** Valor do filtro de sistema que mostra tudo. */
internal const val ANY_SYSTEM = "*"

/** Registro sem sistema vale para todos; com filtro [ANY_SYSTEM] tudo aparece. */
internal fun fitsFilter(entrySystemId: String, filterId: String): Boolean =
    filterId == ANY_SYSTEM || entrySystemId.isBlank() || entrySystemId == filterId

/** Filtro de sistema das bibliotecas (Itens, Grimório, Traços). [selectedId] = id do sistema ou [ANY_SYSTEM]. */
@Composable
internal fun SystemFilterRow(options: List<SystemOption>, selectedId: String, onSelect: (String) -> Unit) {
    val labels = listOf("Todos os sistemas") + options.map { it.name }
    val selected = options.firstOrNull { it.id == selectedId }?.name ?: "Todos os sistemas"
    ChipFilterRow(labels, selected) { label -> onSelect(options.firstOrNull { it.name == label }?.id ?: ANY_SYSTEM) }
}

/** Mantém o filtro válido mesmo que o sistema escolhido tenha sido excluído. */
internal fun validSystemFilter(options: List<SystemOption>, filterId: String): String =
    if (filterId == ANY_SYSTEM || options.any { it.id == filterId }) filterId else ANY_SYSTEM

/** Linha "Sistema: X [Alterar]" de um registro da biblioteca, com seletor (inclui "Todos os sistemas"). */
@Composable
internal fun SystemBinding(
    systemName: String, hint: String, options: List<SystemOption>,
    currentId: String, campaignId: String?, onPick: (String) -> Unit
) {
    var picking by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Sistema", color = Muted, fontSize = 12.sp)
            Text(systemName, color = Ink, fontWeight = FontWeight.SemiBold)
        }
        TextButton(onClick = { picking = true }) { Text("Alterar") }
    }
    Text(hint, color = Muted, fontSize = 12.sp)
    if (picking) {
        SystemPickerDialog(
            title = "Sistema", options = options, currentId = currentId, campaignId = campaignId, allowAll = true,
            onPick = { picking = false; onPick(it) }, onDismiss = { picking = false }
        )
    }
}

/** Botão que copia o registro para outro sistema, onde seus valores podem ser ajustados à parte. */
@Composable
internal fun DuplicateToSystem(options: List<SystemOption>, currentId: String, campaignId: String?, onDuplicate: (String) -> Unit) {
    var picking by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) { Text("Duplicar para outro sistema") }
    Text("Cria uma cópia ligada ao sistema escolhido, para ajustar os valores nele.", color = Muted, fontSize = 12.sp)
    if (picking) {
        SystemPickerDialog(
            title = "Duplicar para…", options = options.filter { it.id != currentId }, currentId = "-", campaignId = campaignId,
            onPick = { picking = false; onDuplicate(it) }, onDismiss = { picking = false }
        )
    }
}
