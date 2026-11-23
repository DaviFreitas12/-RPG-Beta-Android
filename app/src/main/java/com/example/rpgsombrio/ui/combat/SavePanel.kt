package com.example.rpgsombrio.ui.combat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CONDITION_SUGGESTIONS
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.immuneToCondition
import com.example.rpgsombrio.engine.Combatant
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.engine.RollMode
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.applyFailedSaveEffect
import com.example.rpgsombrio.engine.conditionEmoji
import com.example.rpgsombrio.engine.deathStatus
import com.example.rpgsombrio.engine.fighterSaveBonus
import com.example.rpgsombrio.engine.isDying
import com.example.rpgsombrio.engine.resolveSave
import com.example.rpgsombrio.engine.rollDeathSave
import com.example.rpgsombrio.engine.saveAttributes
import com.example.rpgsombrio.engine.saveModifiers
import com.example.rpgsombrio.ui.components.EditorCard
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.push
import com.example.rpgsombrio.ui.components.signed
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Danger
import com.example.rpgsombrio.ui.theme.Good
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted

/**
 * Salvaguarda do alvo escolhido na arena (0.9.2): atributo, CD, vantagem/desvantagem manual e, se falhar,
 * uma condição por N rodadas. As regras ficam em Saves.kt; aqui só há a tela.
 */
@Composable
internal fun SavePanel(
    target: Combatant?, profile: SystemProfile, character: CharacterSheet?, monster: MonsterSheet?,
    engine: RpgEngine, log: MutableList<RollLine>, busy: Boolean,
    onUpdateFighter: (Combatant) -> Unit
) {
    var attrIndex by rememberSaveable { mutableStateOf(0) }
    var dc by rememberSaveable { mutableStateOf(13) }
    var extraBonus by rememberSaveable { mutableStateOf(0) }
    var manualMode by rememberSaveable { mutableStateOf(0) }      // 0 normal, 1 vantagem, 2 desvantagem
    var conditionIndex by rememberSaveable { mutableStateOf(0) }  // 0 = nenhuma; i = CONDITION_SUGGESTIONS[i - 1]
    var rounds by rememberSaveable { mutableStateOf(1) }
    var lastResult by remember { mutableStateOf<List<RollLine>>(emptyList()) }

    val attrs = saveAttributes(profile)

    EditorCard("SALVAGUARDA", "Quem rola é o alvo escolhido na arena. Se falhar, pode receber uma condição por algumas rodadas.") {
        if (target == null) {
            Text("Escolha um alvo na faixa de turnos.", color = Muted, fontSize = 13.sp)
            return@EditorCard
        }
        if (target.isDying()) {
            Text("${target.name} está a 0 PV: ${target.deathStatus()}. A salvaguarda contra a morte é rolada sozinha na vez dele; use o botão para rolar agora.",
                color = Danger, fontSize = 12.sp)
            Button(
                onClick = {
                    val death = rollDeathSave(engine, target)
                    onUpdateFighter(death.fighter)
                    log.push(death.line)
                    lastResult = listOf(death.line)
                },
                enabled = !busy, modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Brand)
            ) {
                Icon(Icons.Default.Casino, null)
                Spacer(Modifier.width(8.dp))
                Text("Salvaguarda contra a morte", maxLines = 1)
            }
        }
        if (attrs.isEmpty()) {
            Text("O sistema ${profile.name} não usa salvaguardas por modificador de atributo.", color = Muted, fontSize = 13.sp)
            return@EditorCard
        }

        val attr = attrs[attrIndex.coerceIn(0, attrs.lastIndex)]
        val baseBonus = fighterSaveBonus(character, monster, attr)
        val bonus = baseBonus + extraBonus
        val manual = when (manualMode) { 1 -> RollMode.ADVANTAGE; 2 -> RollMode.DISADVANTAGE; else -> RollMode.NORMAL }
        val mods = saveModifiers(target.conditions, attr, manual)
        val condition = if (conditionIndex in 1..CONDITION_SUGGESTIONS.size) CONDITION_SUGGESTIONS[conditionIndex - 1] else ""

        Text("${target.name} rola ${attr.label}: ${signed(bonus)}" +
            (if (character == null && monster == null) " (avulso: use o bônus extra)" else ""),
            color = Ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PickButton("Atributo: ${attr.label}", attrs.map { it.label }, Modifier.weight(1f)) { attrIndex = it }
            NumberField("CD", dc, { dc = it.coerceIn(0, 99) }, Modifier.weight(0.6f))
            NumberField("Bônus extra", extraBonus, { extraBonus = it.coerceIn(-99, 99) }, Modifier.weight(0.8f))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = manualMode == 0, onClick = { manualMode = 0 }, label = { Text("Normal") })
            FilterChip(selected = manualMode == 1, onClick = { manualMode = 1 }, label = { Text("Vantagem") })
            FilterChip(selected = manualMode == 2, onClick = { manualMode = 2 }, label = { Text("Desvantagem") })
        }
        mods.describe().forEach { line ->
            val tint = when {
                line.startsWith("Vantagem:") -> Good
                line.startsWith("Desvantagem:") || line.startsWith("Falha") -> Danger
                else -> Muted
            }
            Text(line, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        PickButton(
            "Se falhar: ${if (condition.isEmpty()) "nenhuma condição" else "${conditionEmoji(condition)} $condition"}",
            listOf("Nenhuma condição") + CONDITION_SUGGESTIONS, Modifier.fillMaxWidth()
        ) { conditionIndex = it }
        if (condition.isNotEmpty()) {
            NumberField("Duração (rodadas, 0 = até remover)", rounds, { rounds = it.coerceIn(0, 999) }, Modifier.fillMaxWidth())
            if (immuneToCondition(target.conditionImmunities, condition)) {
                Text("${target.name} é imune a $condition: a condição não será aplicada.", color = Muted, fontSize = 13.sp)
            }
        }

        Button(
            onClick = {
                val outcome = resolveSave(engine, target.name, attr.label, bonus, dc, profile, mods)
                val effect = applyFailedSaveEffect(target, outcome, condition, rounds)
                if (effect.target != target) onUpdateFighter(effect.target)
                // O veredito da salvaguarda fica por cima no histórico; o efeito aparece logo abaixo dele.
                val lines = listOfNotNull(outcome.line, effect.line)
                lines.asReversed().forEach { log.push(it) }
                lastResult = lines
            },
            enabled = !busy, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Brand)
        ) {
            Icon(Icons.Default.Casino, null)
            Spacer(Modifier.width(8.dp))
            Text(if (mods.autoFail) "Aplicar falha automática" else "Rolar salvaguarda de ${target.name}", maxLines = 1)
        }

        lastResult.forEach { line ->
            Text(
                line.text, fontSize = 12.sp,
                color = when (line.good) { true -> Accent; false -> Danger; null -> Muted }
            )
        }
    }
}
