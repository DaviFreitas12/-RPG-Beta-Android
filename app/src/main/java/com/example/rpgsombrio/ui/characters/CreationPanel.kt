package com.example.rpgsombrio.ui.characters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.rpgsombrio.data.BONUS_MAX
import com.example.rpgsombrio.data.BONUS_MIN
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.CreationMethod
import com.example.rpgsombrio.data.CreationRule
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.canStepBase
import com.example.rpgsombrio.data.checkCreation
import com.example.rpgsombrio.data.finalizeCreation
import com.example.rpgsombrio.data.setAttributeBonus
import com.example.rpgsombrio.data.setBaseScore
import com.example.rpgsombrio.data.stepBaseScore
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.signed
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel2

/**
 * Calculadora de criação (0.10.3). Mostra, por atributo, o valor base (compra, array ou rolagem), o bônus de raça e
 * antecedente (aplicado depois) e o valor final. A regra vem de [com.example.rpgsombrio.data.creationRuleFor].
 * Toda a lógica está em `data/CreationRules.kt`; aqui só há tela.
 */
@Composable
internal fun CreationPanel(
    sheet: CharacterSheet, profile: SystemProfile, rule: CreationRule,
    onChange: (CharacterSheet) -> Unit
) {
    val keys = profile.attributes.map { it.key }
    val labels = profile.attributes.associate { it.key to it.abbr }
    val check = checkCreation(rule, sheet.baseAttributes, keys, labels)
    var confirming by remember { mutableStateOf(false) }
    val pointBuy = rule.method == CreationMethod.POINT_BUY

    Text("Criação • ${CreationMethod.label(rule.method)}", color = Ink, fontWeight = FontWeight.Bold)
    Text(CreationMethod.hint(rule.method), color = Muted, fontSize = 12.sp)

    if (pointBuy) {
        Row(
            Modifier.fillMaxWidth().background(Panel2, RoundedCornerShape(12.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pontos gastos", color = Muted, modifier = Modifier.weight(1f))
            Text(
                "${check.spent} / ${check.budget}",
                color = if (check.remaining < 0) MaterialTheme.colorScheme.error else Accent,
                fontWeight = FontWeight.Bold, fontSize = 18.sp
            )
        }
        Text(
            if (check.remaining >= 0) "Restam ${check.remaining} pontos. Valores de ${rule.minBase} a ${rule.maxBase} antes dos bônus."
            else "Você passou do orçamento em ${-check.remaining} pontos.",
            color = Muted, fontSize = 12.sp
        )
    }

    // Cabeçalho das colunas
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(40.dp))
        Text(if (pointBuy) "Base (custo)" else "Base", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1.15f))
        Text("Raça/antec.", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Text("Final", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.width(54.dp))
    }

    profile.attributes.forEach { attr ->
        val base = sheet.baseAttributes[attr.key] ?: rule.minBase
        val bonus = sheet.attributeBonuses[attr.key] ?: 0
        val total = sheet.attributes[attr.key] ?: base
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(attr.abbr, color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
            Column(Modifier.weight(1.15f), horizontalAlignment = Alignment.CenterHorizontally) {
                if (pointBuy) {
                    Stepper(
                        text = base.toString(),
                        minusEnabled = canStepBase(sheet, rule, keys, attr.key, -1),
                        plusEnabled = canStepBase(sheet, rule, keys, attr.key, +1),
                        onMinus = { onChange(stepBaseScore(sheet, rule, keys, attr.key, -1)) },
                        onPlus = { onChange(stepBaseScore(sheet, rule, keys, attr.key, +1)) }
                    )
                    Text("custo ${rule.costOf(base) ?: "–"}", color = Muted, fontSize = 10.sp)
                } else {
                    NumberField("", base, { onChange(setBaseScore(sheet, rule, keys, attr.key, it)) }, Modifier.width(76.dp))
                }
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Stepper(
                    text = signed(bonus),
                    minusEnabled = bonus > BONUS_MIN,
                    plusEnabled = bonus < BONUS_MAX,
                    onMinus = { onChange(setAttributeBonus(sheet, rule, keys, attr.key, bonus - 1)) },
                    onPlus = { onChange(setAttributeBonus(sheet, rule, keys, attr.key, bonus + 1)) }
                )
            }
            Column(Modifier.width(54.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(total.toString(), color = Ink, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                if (attr.hasModifier) Text(signed(attr.modifierOf(total)), color = Accent, fontSize = 12.sp)
            }
        }
    }

    rule.finalCap?.let { cap ->
        Text("Bônus de raça e antecedente entram depois da compra. O valor final para em $cap.", color = Muted, fontSize = 12.sp)
    }

    if (!check.valid) check.problems.forEach { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }

    Button(
        onClick = { confirming = true }, enabled = check.valid,
        colors = ButtonDefaults.buttonColors(containerColor = Brand), modifier = Modifier.fillMaxWidth()
    ) { Text("Finalizar criação") }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Finalizar criação?") },
            text = { Text("Os atributos ficam travados. Só o mestre da campanha pode reabrir a criação depois.") },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    finalizeCreation(sheet, rule, keys, labels)?.let(onChange)
                }) { Text("Finalizar") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancelar") } }
        )
    }
}

/** − valor +, usado na compra de pontos e nos bônus. */
@Composable
private fun Stepper(text: String, minusEnabled: Boolean, plusEnabled: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onMinus, enabled = minusEnabled, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Remove, "Diminuir", modifier = Modifier.size(18.dp))
        }
        Text(text, color = Ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.width(26.dp))
        IconButton(onClick = onPlus, enabled = plusEnabled, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Add, "Aumentar", modifier = Modifier.size(18.dp))
        }
    }
}
