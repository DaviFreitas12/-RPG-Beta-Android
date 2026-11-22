package com.example.rpgsombrio.ui.characters

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.DAMAGE_TYPE_SUGGESTIONS
import com.example.rpgsombrio.data.DamageDefense
import com.example.rpgsombrio.data.DefenseKind
import com.example.rpgsombrio.data.defensesSummary
import com.example.rpgsombrio.ui.components.CollapsibleSection
import com.example.rpgsombrio.ui.components.EmptyHint
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.components.without
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Danger
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted

private val kindShortLabels = mapOf(
    DefenseKind.RESISTANCE to "Resistência",
    DefenseKind.VULNERABILITY to "Vulnerabilidade",
    DefenseKind.IMMUNITY to "Imunidade"
)

private fun kindColor(kind: String): Color = when (kind) {
    DefenseKind.VULNERABILITY -> Danger
    DefenseKind.IMMUNITY -> Ink
    else -> Accent
}

/**
 * Resistências, vulnerabilidades e imunidades a dano (lista de tipos de dano) e imunidades a condições.
 * Serve para personagens e criaturas. Para criaturas antigas, [legacyText] mantém o texto livre que existia antes.
 */
@Composable
internal fun DefensesSection(
    defenses: List<DamageDefense>,
    conditionImmunities: String,
    onDefensesChange: (List<DamageDefense>) -> Unit,
    onConditionsChange: (String) -> Unit,
    legacyText: String? = null,
    onLegacyChange: ((String) -> Unit)? = null
) {
    var kind by remember { mutableStateOf(DefenseKind.RESISTANCE) }
    var custom by remember { mutableStateOf("") }

    val summary = listOf(
        defensesSummary(defenses),
        if (conditionImmunities.isBlank()) "" else "Imune a: $conditionImmunities"
    ).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Nenhuma" }

    fun add(type: String) {
        val t = type.trim()
        if (t.isEmpty()) return
        if (defenses.any { it.kind == kind && it.type.equals(t, ignoreCase = true) }) return
        onDefensesChange(defenses + DamageDefense(kind, t))
    }

    CollapsibleSection("RESISTÊNCIAS E VULNERABILIDADES", summary = summary, startExpanded = false) {
        DefenseKind.ALL.forEach { k ->
            val mine = defenses.withIndex().filter { it.value.kind == k }
            if (mine.isNotEmpty()) {
                Text(DefenseKind.label(k), color = kindColor(k), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(mine) { indexed ->
                        FilterChip(
                            selected = true,
                            onClick = { onDefensesChange(defenses.without(indexed.index)) },
                            label = { Text(indexed.value.type) },
                            trailingIcon = { Icon(Icons.Default.Close, "Remover ${indexed.value.type}", Modifier.size(16.dp)) }
                        )
                    }
                }
            }
        }
        if (defenses.isEmpty()) EmptyHint("Nenhuma defesa. Escolha o tipo e toque no tipo de dano.")

        Text("Adicionar como", color = Muted, fontSize = 12.sp)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(DefenseKind.ALL) { k ->
                FilterChip(selected = k == kind, onClick = { kind = k }, label = { Text(kindShortLabels.getValue(k)) })
            }
        }
        val used = defenses.filter { it.kind == kind }.map { it.type.trim().lowercase() }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(DAMAGE_TYPE_SUGGESTIONS.filter { it.lowercase() !in used }) { type ->
                FilterChip(selected = false, onClick = { add(type) }, label = { Text(type) })
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput("Outro tipo de dano", custom, { custom = it }, Modifier.weight(1f))
            IconButton(onClick = { add(custom); custom = "" }) {
                Icon(Icons.Default.Add, "Adicionar defesa", tint = Accent)
            }
        }

        TextInput("Imunidades a condições (ex.: Enfeitiçado, Amedrontado)", conditionImmunities, onConditionsChange)
        if (legacyText != null && onLegacyChange != null) {
            TextInput("Outras defesas (texto livre)", legacyText, onLegacyChange, singleLine = false, minLines = 2)
        }
    }
}
