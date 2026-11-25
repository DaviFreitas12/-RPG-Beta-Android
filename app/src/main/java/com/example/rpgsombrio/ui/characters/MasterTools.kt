package com.example.rpgsombrio.ui.characters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.AsiGrant
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.CreationRule
import com.example.rpgsombrio.data.CreationState
import com.example.rpgsombrio.data.MAX_ASI_GRANT
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.asiGrantBlockReason
import com.example.rpgsombrio.data.asiGrantSummary
import com.example.rpgsombrio.data.completeAsiGrant
import com.example.rpgsombrio.data.freeSetLevel
import com.example.rpgsombrio.data.grantExtraAsi
import com.example.rpgsombrio.data.levelRulesFor
import com.example.rpgsombrio.data.pendingAsiGrants
import com.example.rpgsombrio.data.reopenCreation
import com.example.rpgsombrio.data.revokeAsiGrant
import com.example.rpgsombrio.ui.components.EditorCard
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.ToggleRow
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel

/**
 * Ferramentas do mestre na ficha (0.10.5, etapa 6): edição livre, nível livre, aumento de atributo extra e reabrir a
 * criação. Só aparece para o mestre da campanha. Toda a conta está em `data/MasterRules.kt`; aqui só há tela.
 */
@Composable
internal fun MasterSheetTools(
    sheet: CharacterSheet, profile: SystemProfile, creationRule: CreationRule,
    freeEdit: Boolean, onFreeEditChange: (Boolean) -> Unit, onChange: (CharacterSheet) -> Unit
) {
    val rules = levelRulesFor(sheet.systemId)
    var granting by remember { mutableStateOf(false) }
    var settingLevel by remember { mutableStateOf(false) }
    val labels = profile.attributes.associate { it.key to it.abbr }
    EditorCard("FERRAMENTAS DO MESTRE", "Só você, como mestre desta campanha, vê esta área") {
        ToggleRow("Edição livre (ignora as travas)", freeEdit, onFreeEditChange)
        Text(
            if (freeEdit) "Ligada: atributos e nível desta ficha ficam editáveis, para itens mágicos, bênçãos e regras da casa."
            else "Desligada: valem as travas de criação e de nível.",
            color = Muted, fontSize = 12.sp
        )
        if (freeEdit && rules != null) {
            TextButton(onClick = { settingLevel = true }) { Text("Definir nível livremente…") }
        }

        if (rules != null) {
            Text("ATRIBUTO EXTRA", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                "Concede pontos fora dos níveis 4, 8, 12, 16 e 19. Serve para exceções de classe (Guerreiro nos níveis 6 e 14, Ladino no 10).",
                color = Muted, fontSize = 12.sp
            )
            Button(
                onClick = { granting = true }, enabled = asiGrantBlockReason(sheet, rules) == null,
                colors = ButtonDefaults.buttonColors(containerColor = Brand), modifier = Modifier.fillMaxWidth()
            ) { Text("Conceder aumento de atributo") }
            asiGrantBlockReason(sheet, rules)?.let { Text("Não dá agora: $it.", color = Muted, fontSize = 12.sp) }
            sheet.asiGrants.forEach { grant ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(asiGrantSummary(grant, labels), color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { revokeAsiGrant(sheet, grant.id)?.let(onChange) }) { Text("Retirar") }
                }
            }
        }

        if (sheet.creation == CreationState.DONE && !creationRule.isFree) {
            Text("CRIAÇÃO", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("A criação desta ficha está finalizada. Reabrir devolve a calculadora de pontos, mantendo base e bônus.", color = Muted, fontSize = 12.sp)
            TextButton(onClick = { onChange(reopenCreation(sheet, creationRule, profile.attributes.map { it.key })) }) { Text("Reabrir criação") }
        }
    }
    if (granting) GrantAsiDialog(sheet, profile, onDismiss = { granting = false }, onGrant = { onChange(it); granting = false })
    if (settingLevel) FreeLevelDialog(sheet, profile, onDismiss = { settingLevel = false }, onApply = { onChange(it); settingLevel = false })
}

/** Diálogo do mestre para conceder pontos extras de atributo. Também usado pela aba Campanha. */
@Composable
internal fun GrantAsiDialog(sheet: CharacterSheet, profile: SystemProfile, onDismiss: () -> Unit, onGrant: (CharacterSheet) -> Unit) {
    val rules = levelRulesFor(sheet.systemId)
    var points by remember { mutableStateOf(2) }
    var reason by remember { mutableStateOf("") }
    val suggestions = listOf("Guerreiro nível 6", "Guerreiro nível 14", "Ladino nível 10", "Bênção ou item mágico")
    val granted = grantExtraAsi(sheet, rules, points, reason, SheetStore.newId())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aumento de atributo extra") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Para ${sheet.name.ifBlank { "o personagem" }}. O jogador distribui os pontos depois (teto ${rules?.attributeCap ?: 20} em cada atributo).", color = Muted, fontSize = 13.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Pontos", color = Ink, modifier = Modifier.weight(1f))
                    IconButton(onClick = { points = (points - 1).coerceAtLeast(1) }, enabled = points > 1, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Remove, "Diminuir pontos", modifier = Modifier.size(18.dp))
                    }
                    Text("$points", color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
                    IconButton(onClick = { points = (points + 1).coerceAtMost(MAX_ASI_GRANT) }, enabled = points < MAX_ASI_GRANT, modifier = Modifier.size(34.dp)) {
                        Icon(Icons.Default.Add, "Aumentar pontos", modifier = Modifier.size(18.dp))
                    }
                }
                Text("MOTIVO (opcional)", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                suggestions.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        row.forEach { s -> FilterChip(selected = reason == s, onClick = { reason = if (reason == s) "" else s }, label = { Text(s, fontSize = 12.sp) }) }
                    }
                }
                OutlinedTextField(reason, { reason = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Motivo") })
                asiGrantBlockReason(sheet, rules)?.let { Text("Não dá agora: $it.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = { TextButton(enabled = granted != null, onClick = { granted?.let(onGrant) }) { Text("Conceder") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Nível livre (edição livre do mestre): digita o nível desejado e confirma o que será desfeito, se for para baixo. */
@Composable
private fun FreeLevelDialog(sheet: CharacterSheet, profile: SystemProfile, onDismiss: () -> Unit, onApply: (CharacterSheet) -> Unit) {
    val rules = levelRulesFor(sheet.systemId) ?: return
    var target by remember { mutableStateOf(sheet.level) }
    val clamped = target.coerceIn(1, rules.maxLevel)
    val result = freeSetLevel(sheet, rules, clamped, profile.autoProficiency)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Definir nível") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Nível (1 a ${rules.maxLevel})", target, { target = it.coerceIn(0, 99) }, Modifier.fillMaxWidth())
                Text(
                    when {
                        clamped < sheet.level -> "Vai remover ${sheet.level - clamped} nível(is): vida e pontos de atributo concedidos por eles são desfeitos exatamente. Níveis sem histórico só voltam o nível e a proficiência."
                        clamped > sheet.level -> "Só sobe o nível e a proficiência. Vida e atributos você concede por Subir nível ou por Atributo extra."
                        else -> "Esse já é o nível atual."
                    },
                    color = Muted, fontSize = 12.sp
                )
            }
        },
        confirmButton = { TextButton(enabled = clamped != sheet.level, onClick = { onApply(result) }) { Text("Aplicar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Aviso no topo da ficha quando o mestre concedeu aumento de atributo e o jogador ainda não distribuiu (0.10.5). */
@Composable
internal fun AsiGrantBanner(sheet: CharacterSheet, profile: SystemProfile, onChange: (CharacterSheet) -> Unit) {
    val grant = pendingAsiGrants(sheet).firstOrNull() ?: return
    var open by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Accent), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("O MESTRE CONCEDEU", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("+${grant.points} pontos de atributo", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            if (grant.reason.isNotBlank()) Text(grant.reason, color = Muted, fontSize = 13.sp)
            Button(onClick = { open = true }, colors = ButtonDefaults.buttonColors(containerColor = Brand), modifier = Modifier.fillMaxWidth()) {
                Text("Distribuir pontos")
            }
        }
    }
    if (open) AsiGrantDialog(sheet, profile, grant, onDismiss = { open = false }, onChange = { onChange(it); open = false })
}

@Composable
private fun AsiGrantDialog(sheet: CharacterSheet, profile: SystemProfile, grant: AsiGrant, onDismiss: () -> Unit, onChange: (CharacterSheet) -> Unit) {
    val rules = levelRulesFor(sheet.systemId) ?: return
    var asi by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val outcome = completeAsiGrant(sheet, rules, grant.id, asi)
    val spent = asi.values.sum()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aumento de atributo") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (grant.reason.isNotBlank()) Text(grant.reason, color = Muted, fontSize = 13.sp)
                Text("${grant.points - spent} de ${grant.points} pontos livres", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                profile.attributes.filter { it.key in rules.attributeKeys }.forEach { attr ->
                    val current = sheet.attributes[attr.key] ?: 10
                    val add = asi[attr.key] ?: 0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(attr.abbr, color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp))
                        IconButton(onClick = { asi = asi + (attr.key to add - 1) }, enabled = add > 0, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Remove, "Diminuir ${attr.abbr}", modifier = Modifier.size(18.dp))
                        }
                        Text("${current + add}", color = Ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.width(36.dp))
                        IconButton(
                            onClick = { asi = asi + (attr.key to add + 1) },
                            enabled = spent < grant.points && current + add < rules.attributeCap,
                            modifier = Modifier.size(34.dp)
                        ) { Icon(Icons.Default.Add, "Aumentar ${attr.abbr}", modifier = Modifier.size(18.dp)) }
                        if (add > 0) Text("(+$add)", color = Muted, fontSize = 12.sp)
                    }
                }
                Text("Teto de ${rules.attributeCap} em cada atributo.", color = Muted, fontSize = 12.sp)
                outcome.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = { TextButton(enabled = outcome.sheet != null, onClick = { outcome.sheet?.let(onChange) }) { Text("Concluir") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Depois") } }
    )
}
