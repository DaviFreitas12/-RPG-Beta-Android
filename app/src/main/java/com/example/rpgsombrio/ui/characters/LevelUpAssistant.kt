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
import com.example.rpgsombrio.data.Campaign
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.HpMethod
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.averageHp
import com.example.rpgsombrio.data.completeLevelUp
import com.example.rpgsombrio.data.conModifier
import com.example.rpgsombrio.data.grantLevelUp
import com.example.rpgsombrio.data.levelEntrySummary
import com.example.rpgsombrio.data.levelRulesFor
import com.example.rpgsombrio.data.levelUpBlockReason
import com.example.rpgsombrio.data.pendingLevelUp
import com.example.rpgsombrio.data.removeLevel
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel

/**
 * Aviso no topo da ficha quando o mestre subiu o nível e o jogador ainda não concluiu (0.10.4).
 * Toda a conta está em `data/LevelRules.kt`; aqui só há tela.
 */
@Composable
internal fun LevelUpBanner(sheet: CharacterSheet, profile: SystemProfile, campaign: Campaign?, onChange: (CharacterSheet) -> Unit) {
    val entry = pendingLevelUp(sheet) ?: return
    var open by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Accent), modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("SUBIU DE NÍVEL", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Nível ${entry.level}!", color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                if (entry.asiGranted > 0) "Falta concluir: vida e ${entry.asiGranted} pontos de atributo."
                else "Falta concluir: vida.",
                color = Muted, fontSize = 13.sp
            )
            Button(onClick = { open = true }, colors = ButtonDefaults.buttonColors(containerColor = Brand), modifier = Modifier.fillMaxWidth()) {
                Text("Concluir level up")
            }
        }
    }
    if (open) LevelUpDialog(sheet, profile, campaign, onDismiss = { open = false }, onChange = { onChange(it); open = false })
}

@Composable
private fun LevelUpDialog(
    sheet: CharacterSheet, profile: SystemProfile, campaign: Campaign?,
    onDismiss: () -> Unit, onChange: (CharacterSheet) -> Unit
) {
    val rules = levelRulesFor(sheet.systemId) ?: return
    val entry = pendingLevelUp(sheet) ?: return
    val method = HpMethod.effective(campaign?.hpMethod.orEmpty())
    var die by remember { mutableStateOf(sheet.hitDie.takeIf { it in rules.hitDice } ?: 8) }
    var rolled by remember { mutableStateOf(0) }
    var note by remember { mutableStateOf("") }
    var asi by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    val outcome = completeLevelUp(sheet, rules, method, die, rolled.takeIf { it > 0 }, asi, note)
    val spent = asi.values.sum()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nível ${entry.level}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Bônus de proficiência: +${outcome.sheet?.proficiencyBonus ?: sheet.proficiencyBonus}", color = Muted, fontSize = 13.sp)

                Text("VIDA • ${HpMethod.label(method)}", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Dado de vida da classe", color = Muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    rules.hitDice.forEach { d -> FilterChip(selected = die == d, onClick = { die = d }, label = { Text("d$d") }) }
                }
                val con = conModifier(sheet, rules)
                if (method == HpMethod.ROLL) {
                    NumberField("Valor do dado (1 a $die)", rolled, { rolled = it.coerceIn(0, 99) }, Modifier.fillMaxWidth())
                } else {
                    Text("Média fixa: ${averageHp(die)} ${if (con >= 0) "+" else "−"} ${kotlin.math.abs(con)} (Constituição)", color = Muted, fontSize = 13.sp)
                }
                outcome.sheet?.levelHistory?.lastOrNull()?.let {
                    Text("Vida ganha: +${it.hpGained}", color = Ink, fontWeight = FontWeight.SemiBold)
                }

                if (entry.asiGranted > 0) {
                    Text("ATRIBUTOS • ${entry.asiGranted - spent} de ${entry.asiGranted} pontos livres", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                enabled = spent < entry.asiGranted && current + add < rules.attributeCap,
                                modifier = Modifier.size(34.dp)
                            ) { Icon(Icons.Default.Add, "Aumentar ${attr.abbr}", modifier = Modifier.size(18.dp)) }
                            if (add > 0) Text("(+$add)", color = Muted, fontSize = 12.sp)
                        }
                    }
                    Text("Teto de ${rules.attributeCap} em cada atributo.", color = Muted, fontSize = 12.sp)
                }

                Text("CARACTERÍSTICAS E MAGIAS NOVAS", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    note, { note = it }, modifier = Modifier.fillMaxWidth(), minLines = 2,
                    label = { Text("Anotação (opcional)") }
                )
                Text("Ficam só como anotação no histórico de nível: o app não aplica características nem magias.", color = Muted, fontSize = 12.sp)

                outcome.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(enabled = outcome.sheet != null, onClick = { outcome.sheet?.let(onChange) }) { Text("Concluir") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Depois") } }
    )
}

/** Botões do mestre na ficha: subir e remover nível (com confirmação do que será desfeito). */
@Composable
internal fun MasterLevelButtons(sheet: CharacterSheet, profile: SystemProfile, onChange: (CharacterSheet) -> Unit) {
    val rules = levelRulesFor(sheet.systemId) ?: return
    var confirmRemove by remember { mutableStateOf(false) }
    val reason = levelUpBlockReason(sheet, rules)
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(enabled = reason == null, onClick = { grantLevelUp(sheet, rules, profile.autoProficiency)?.let(onChange) }) { Text("Subir nível") }
        TextButton(enabled = sheet.level > 1, onClick = { confirmRemove = true }) { Text("Remover nível") }
    }
    if (reason != null && sheet.level < rules.maxLevel) Text("Não dá para subir agora: $reason.", color = Muted, fontSize = 12.sp)
    if (confirmRemove) {
        val last = sheet.levelHistory.lastOrNull()?.takeIf { it.level == sheet.level }
        val labels = profile.attributes.associate { it.key to it.abbr }
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remover o nível ${sheet.level}?") },
            text = {
                Text(
                    if (last == null) "Esta ficha não tem histórico deste nível: só o nível e a proficiência voltam. Vida e atributos ficam como estão."
                    else "Será desfeito exatamente o que o nível concedeu. ${levelEntrySummary(last, labels)}."
                )
            },
            confirmButton = {
                TextButton(onClick = { confirmRemove = false; removeLevel(sheet, rules, profile.autoProficiency)?.let(onChange) }) { Text("Remover") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancelar") } }
        )
    }
}
