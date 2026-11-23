package com.example.rpgsombrio.ui.combat

import android.content.SharedPreferences
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.SheetRole
import com.example.rpgsombrio.data.toAttackOption
import com.example.rpgsombrio.data.toCombatAction
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.data.damageMultiplierFor
import com.example.rpgsombrio.engine.AttackOption
import com.example.rpgsombrio.engine.AttackOutcome
import com.example.rpgsombrio.engine.Combatant
import com.example.rpgsombrio.engine.CombatantSource
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.engine.RollMode
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.actionBlocker
import com.example.rpgsombrio.engine.attackModifiers
import com.example.rpgsombrio.engine.characterAttacks
import com.example.rpgsombrio.engine.characterVariables
import com.example.rpgsombrio.engine.conditionDamageMultiplier
import com.example.rpgsombrio.engine.conditionEmoji
import com.example.rpgsombrio.engine.conditionLabel
import com.example.rpgsombrio.engine.conditionRule
import com.example.rpgsombrio.engine.deathStatus
import com.example.rpgsombrio.engine.isDying
import com.example.rpgsombrio.engine.monsterAttacks
import com.example.rpgsombrio.engine.monsterVariables
import com.example.rpgsombrio.engine.rollOption
import com.example.rpgsombrio.engine.scaleDamage
import com.example.rpgsombrio.ui.avatar.AvatarView
import com.example.rpgsombrio.ui.avatar.parseAvatarKey
import com.example.rpgsombrio.ui.avatar.toAvatarKey
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.components.Portrait
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.components.push
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Danger
import com.example.rpgsombrio.ui.theme.Amber
import com.example.rpgsombrio.ui.theme.Good
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Night
import com.example.rpgsombrio.ui.theme.Panel
import com.example.rpgsombrio.ui.theme.Panel2
import com.example.rpgsombrio.ui.theme.Warn
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------------------------
// Arena de batalha (0.9.2): ordem de turnos com retratos, palco com atacante e alvo, dado girando,
// golpe, tremor e número de dano flutuante. A lógica das regras vem de Conditions.kt e SheetEngine.kt;
// aqui só há apresentação.
// ---------------------------------------------------------------------------------------------


/** Um ataque já resolvido, pronto para ser animado. Os PV já foram aplicados; a animação só os revela aos poucos. */
internal data class BattleEvent(
    val id: Int,
    val attackerId: String,
    val targetId: String,
    val outcome: AttackOutcome,
    /** Dano que de fato entrou (depois de resistências e condições). */
    val dealt: Int,
    val hpBefore: Int,
    val hpAfter: Int,
    /** Ex.: "Goblin resiste a fogo: dano 7 → 3". Vazio quando nada mudou. */
    val note: String,
    /** Linhas do histórico, mostradas quando o golpe acerta (ou erra) — não antes do dado parar. */
    val logLines: List<RollLine>
)

private enum class StagePhase { IDLE, ROLLING, RESULT }

private fun portraitOf(fighter: Combatant, characters: List<CharacterSheet>, monsters: List<MonsterSheet>): String = when (fighter.sheetKind) {
    CombatantSource.CHARACTER -> characters.firstOrNull { it.id == fighter.sheetId }
        ?.let { c -> c.imageUri.ifBlank { if (c.look.configured) c.look.toAvatarKey() else "" } }
    CombatantSource.MONSTER -> monsters.firstOrNull { it.id == fighter.sheetId }?.imageUri
    else -> null
} ?: ""

/** Alvo sugerido: o primeiro em pé do outro lado (personagem contra criatura); senão qualquer um em pé. */
private fun defaultTarget(attacker: Combatant?, fighters: List<Combatant>): Combatant? {
    if (attacker == null) return null
    val others = fighters.filter { it.id != attacker.id }
    val standing = others.filter { it.currentHp > 0 }
    val opposing = standing.filter { attacker.sheetKind.isBlank() || it.sheetKind != attacker.sheetKind }
    return opposing.firstOrNull() ?: standing.firstOrNull() ?: others.firstOrNull()
}

private fun damageNote(name: String, base: Double, finalMultiplier: Double, type: String, conditions: List<String>, raw: Int, dealt: Int): String {
    if (finalMultiplier == 1.0) return ""
    val typeText = type.trim().let { if (it.isBlank()) "" else " a $it" }
    val petrified = conditions.any { conditionRule(it)?.resistsAllDamage == true } && finalMultiplier != base
    val reason = when {
        finalMultiplier == 0.0 -> "é imune$typeText"
        petrified -> "resiste a todo dano (Petrificado)"
        finalMultiplier < 1.0 -> "resiste$typeText"
        else -> "é vulnerável$typeText"
    }
    return "$name $reason: dano $raw → $dealt"
}

// --- peças visuais -----------------------------------------------------------------------------

@Composable
private fun FighterAvatar(
    name: String, portraitUri: String, size: Dp, ring: Color, defeated: Boolean,
    modifier: Modifier = Modifier, overlay: @Composable BoxScope.() -> Unit = {}
) {
    val hue = (name.fold(0) { acc, c -> (acc * 31 + c.code) and 0xFFFF } % 360).toFloat()
    val shape = RoundedCornerShape(18.dp)
    Box(modifier.border(2.dp, ring, shape).padding(3.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(size).clip(RoundedCornerShape(14.dp))
                .background(Brush.verticalGradient(listOf(Color.hsv(hue, .45f, .62f), Color.hsv(hue, .55f, .34f)))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?", color = Color.White.copy(alpha = .9f),
                fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold
            )
        }
        val avatarLook = parseAvatarKey(portraitUri)
        if (avatarLook != null) AvatarView(avatarLook, size)
        else if (portraitUri.isNotBlank()) Portrait(portraitUri, name, size)
        if (defeated) {
            Box(Modifier.size(size).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = .62f)), contentAlignment = Alignment.Center) {
                Text("💀", fontSize = (size.value * 0.4f).sp)
            }
        }
        overlay()
    }
}

@Composable
private fun HpBar(current: Float, max: Int, modifier: Modifier = Modifier) {
    val ratio = if (max <= 0) 0f else (current / max).coerceIn(0f, 1f)
    val color = when {
        ratio > .6f -> Good
        ratio > .3f -> Warn
        else -> Danger
    }
    Box(modifier.height(8.dp).clip(RoundedCornerShape(50)).background(Hairline)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(ratio).clip(RoundedCornerShape(50)).background(color))
    }
}

/** Emojis das condições ativas (até 4, com "+N" se houver mais). */
private fun conditionBadges(conditions: List<String>): String {
    if (conditions.isEmpty()) return ""
    val shown = conditions.take(4).joinToString(" ") { conditionEmoji(it) }
    return if (conditions.size > 4) "$shown +${conditions.size - 4}" else shown
}

private fun DrawScope.drawDie(sides: Int, tint: Color) {
    val radius = size.minDimension / 2f * 0.92f
    val sidesCount = when (sides) { 4 -> 3; 6 -> 4; 8 -> 4; 12 -> 5; else -> 6 }
    val startAngle = if (sides == 6) -45f else -90f
    fun point(i: Int): Offset {
        val angle = (startAngle + 360f / sidesCount * i) * (PI / 180.0)
        return Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
    }
    val body = Path().apply {
        moveTo(point(0).x, point(0).y)
        for (i in 1 until sidesCount) lineTo(point(i).x, point(i).y)
        close()
    }
    drawPath(
        body,
        Brush.linearGradient(listOf(tint, tint.copy(alpha = .55f)), start = Offset.Zero, end = Offset(size.width, size.height))
    )
    if (sides == 20) {
        val inner = Path().apply {
            moveTo(point(0).x, point(0).y); lineTo(point(2).x, point(2).y); lineTo(point(4).x, point(4).y); close()
        }
        drawPath(inner, Color.White.copy(alpha = .22f), style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round))
    }
    drawPath(body, Color.White.copy(alpha = .65f), style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))
}

@Composable
private fun DieBadge(sides: Int, value: Int, rotation: Float, scale: Float, tint: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(84.dp).graphicsLayer { scaleX = scale; scaleY = scale }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) { drawDie(sides, tint) }
        Text(
            if (value > 0) value.toString() else "d$sides", color = Color.White,
            style = TextStyle(
                fontSize = if (value >= 100) 20.sp else 28.sp, fontWeight = FontWeight.Black,
                shadow = Shadow(Color.Black.copy(alpha = .5f), Offset(1f, 2f), 4f)
            )
        )
    }
}

/** Cartão de um lado do palco: papel, retrato, nome, PV e condições. Tocar abre a lista para trocar o combatente. */
@Composable
private fun FighterCard(
    fighter: Combatant?, portraitUri: String, role: String, roleColor: Color,
    shownHp: Float, choices: List<Combatant>, onPick: (Combatant) -> Unit,
    modifier: Modifier = Modifier, overlay: @Composable BoxScope.() -> Unit = {}
) {
    var open by remember { mutableStateOf(false) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(role, color = roleColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        Box {
            if (fighter == null) {
                Box(Modifier.size(90.dp).clip(RoundedCornerShape(18.dp)).background(Panel2).clickable { open = true }, contentAlignment = Alignment.Center) {
                    Text("?", color = Muted, fontSize = 28.sp)
                }
            } else {
                FighterAvatar(
                    fighter.name, portraitUri, 84.dp, roleColor, defeated = shownHp <= 0f,
                    modifier = Modifier.clickable { open = true }, overlay = overlay
                )
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                choices.forEach { c ->
                    DropdownMenuItem(
                        text = { Text("${c.name}${if (c.currentHp <= 0) " (derrotado)" else ""}") },
                        onClick = { open = false; onPick(c) }
                    )
                }
            }
        }
        Text(fighter?.name ?: "Escolher", color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (fighter != null) {
            HpBar(shownHp, fighter.maxHp, Modifier.fillMaxWidth(0.9f))
            Text("PV ${shownHp.roundToInt()}/${fighter.maxHp}", color = Muted, fontSize = 13.sp)
            val badges = conditionBadges(fighter.conditions)
            Text(if (badges.isEmpty()) " " else badges, fontSize = 14.sp, maxLines = 1)
        }
    }
}

// --- palco -------------------------------------------------------------------------------------

@Composable
private fun BattleStage(
    attacker: Combatant?, target: Combatant?,
    attackerPortrait: String, targetPortrait: String,
    fighters: List<Combatant>,
    event: BattleEvent?, animate: Boolean, defenseLabel: String,
    onPickAttacker: (Combatant) -> Unit, onPickTarget: (Combatant) -> Unit,
    onReveal: (BattleEvent) -> Unit, onBusy: (Boolean) -> Unit
) {
    val density = LocalDensity.current.density
    var phase by remember { mutableStateOf(StagePhase.IDLE) }
    var shownDie by remember { mutableStateOf(0) }
    var dieSides by remember { mutableStateOf(20) }
    var result by remember { mutableStateOf<BattleEvent?>(null) }
    var settledId by remember { mutableStateOf(-1) }
    var animating by remember { mutableStateOf(false) }
    var floatText by remember { mutableStateOf("") }
    var floatTitle by remember { mutableStateOf("") }
    var floatColor by remember { mutableStateOf(Danger) }
    val dieRotation = remember { Animatable(0f) }
    val dieScale = remember { Animatable(1f) }
    val lunge = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val flash = remember { Animatable(0f) }
    val floatProgress = remember { Animatable(0f) }
    val hpShown = remember { Animatable(0f) }

    LaunchedEffect(event?.id) {
        val e = event ?: return@LaunchedEffect
        val o = e.outcome
        val sides = if (o.d20 != null) 20 else (o.damageSides ?: 20)
        val finalValue = o.d20 ?: o.damage ?: 0
        var revealed = false
        result = null
        dieSides = sides
        floatText = ""
        floatTitle = ""
        if (!animate) {
            shownDie = finalValue; result = e; phase = StagePhase.RESULT
            revealed = true; onReveal(e); settledId = e.id
            return@LaunchedEffect
        }
        hpShown.snapTo(e.hpBefore.toFloat())
        animating = true
        onBusy(true)
        try {
            phase = StagePhase.ROLLING
            // 1) o dado gira e os números passam rápido, desacelerando; o atacante recua para pegar impulso.
            coroutineScope {
                launch { dieRotation.snapTo(0f); dieRotation.animateTo(720f, tween(950, easing = FastOutSlowInEasing)) }
                launch { lunge.animateTo(-0.2f, tween(450)) }
                for (i in 0 until 12) {
                    shownDie = Random.nextInt(1, sides + 1)
                    delay((30 + i * i).toLong())
                }
            }
            // 2) o dado para no valor final com um "quique".
            shownDie = finalValue
            result = e
            phase = StagePhase.RESULT
            dieScale.snapTo(1.4f)
            dieScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            delay(200)
            // 3) o atacante avança até o alvo.
            lunge.animateTo(1f, tween(150, easing = FastOutLinearInEasing))
            // 4) impacto: histórico, tremor, clarão vermelho, PV caindo e número de dano subindo.
            revealed = true
            onReveal(e)
            val missed = o.hit == false
            val damaged = !missed && o.damage != null
            floatTitle = when {
                missed && o.fumble -> "FALHA CRÍTICA"
                o.critical && damaged -> "CRÍTICO!"
                else -> ""
            }
            floatText = when {
                missed -> "ERROU"
                !damaged -> ""
                e.dealt <= 0 -> "SEM DANO"
                else -> "-${e.dealt}"
            }
            floatColor = when {
                missed || e.dealt <= 0 -> Muted
                o.critical -> Amber
                else -> Danger
            }
            coroutineScope {
                launch { lunge.animateTo(0f, tween(320)) }
                launch { floatProgress.snapTo(0f); floatProgress.animateTo(1f, tween(1000)) }
                if (missed) {
                    launch { shake.animateTo(28f, tween(110)); shake.animateTo(0f, tween(240)) }
                } else if (damaged && e.dealt > 0) {
                    launch { flash.snapTo(1f); flash.animateTo(0f, tween(480)) }
                    launch {
                        val amplitude = if (o.critical) 20f else 12f
                        repeat(6) { shake.animateTo(if (it % 2 == 0) amplitude else -amplitude, tween(45)) }
                        shake.animateTo(0f, tween(60))
                    }
                    launch { hpShown.animateTo(e.hpAfter.toFloat(), tween(600)) }
                }
            }
        } finally {
            if (!revealed) onReveal(e)
            animating = false
            settledId = e.id
            onBusy(false)
        }
    }

    // Enquanto o golpe não terminou, o alvo mostra os PV de antes (os dados já foram atualizados por baixo).
    val pending = event != null && event.id != settledId && event.targetId == target?.id
    val targetHp: Float = when {
        target == null -> 0f
        animating && pending -> hpShown.value
        pending -> event!!.hpBefore.toFloat()
        else -> target.currentHp.toFloat()
    }

    val dieTint = when {
        phase == StagePhase.ROLLING || result == null -> Brand
        result?.outcome?.critical == true -> Amber
        result?.outcome?.fumble == true -> Danger
        result?.outcome?.hit == false -> Muted
        else -> Brand
    }
    val lungePx = 96f * density

    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.verticalGradient(listOf(Panel2, Panel)))
            .border(1.dp, Hairline, RoundedCornerShape(24.dp))
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                FighterCard(
                    attacker, attackerPortrait, "ATACANTE", Accent, attacker?.currentHp?.toFloat() ?: 0f,
                    fighters.filter { it.id != target?.id }, onPickAttacker,
                    Modifier.weight(1f).zIndex(1f).graphicsLayer { translationX = lunge.value * lungePx }
                )
                DieBadge(dieSides, shownDie, dieRotation.value, dieScale.value, dieTint)
                FighterCard(
                    target, targetPortrait, "ALVO", Danger, targetHp,
                    fighters.filter { it.id != attacker?.id }, onPickTarget,
                    Modifier.weight(1f).graphicsLayer { translationX = shake.value * density },
                    overlay = {
                        if (flash.value > 0f) {
                            Box(Modifier.size(84.dp).clip(RoundedCornerShape(14.dp)).background(Danger.copy(alpha = flash.value * 0.6f)))
                        }
                        val p = floatProgress.value
                        if (floatText.isNotEmpty() && p > 0f && p < 1f) {
                            Column(
                                Modifier.align(Alignment.Center).zIndex(3f).graphicsLayer {
                                    translationY = -p * 64f * density
                                    alpha = (2f * (1f - p)).coerceIn(0f, 1f)
                                    val pop = 1f + 0.35f * (1f - p)
                                    scaleX = pop; scaleY = pop
                                },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (floatTitle.isNotEmpty()) {
                                    Text(floatTitle, style = TextStyle(color = Amber, fontSize = 12.sp, fontWeight = FontWeight.Black,
                                        shadow = Shadow(Color.Black, Offset(1f, 2f), 4f)))
                                }
                                Text(floatText, style = TextStyle(color = floatColor, fontSize = 30.sp, fontWeight = FontWeight.Black,
                                    shadow = Shadow(Color.Black, Offset(2f, 3f), 6f)))
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            val shownResult = result
            when {
                phase == StagePhase.ROLLING -> Text("Rolando…", color = Accent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                shownResult == null -> Text("Escolha o ataque e toque em Atacar", color = Muted, fontSize = 13.sp)
                else -> ResultSummary(shownResult, defenseLabel)
            }
        }
    }
}

@Composable
private fun ResultSummary(e: BattleEvent, defenseLabel: String) {
    val o = e.outcome
    val (headline, color) = when {
        o.critical && o.hit != false -> "CRÍTICO!" to Amber
        o.fumble -> "FALHA CRÍTICA" to Danger
        o.hit == true -> "ACERTOU" to Good
        o.hit == false -> "ERROU" to Muted
        o.damage != null -> "DANO ${o.damage}" to Accent
        else -> "ROLAGEM" to Accent
    }
    val details = buildList {
        o.attackTotal?.let { add("$it${o.defense?.let { d -> " contra $defenseLabel $d" } ?: ""}") }
        if (o.mode != RollMode.NORMAL && o.d20 != null && o.otherD20 != null) {
            add("${if (o.mode == RollMode.ADVANTAGE) "vantagem" else "desvantagem"}: ${o.d20} (descartou ${o.otherD20})")
        }
        if (o.hit != false && o.damage != null) add("dano ${e.dealt}${o.damageType.trim().let { if (it.isBlank()) "" else " $it" }}")
        if (e.note.isNotBlank()) add(e.note)
    }.joinToString(" • ")
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(headline, color = color, fontWeight = FontWeight.Black, fontSize = 17.sp, letterSpacing = 1.sp)
        if (details.isNotBlank()) Text(details, color = Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

// --- ordem de turnos e lista -------------------------------------------------------------------

@Composable
private fun TurnStrip(
    fighters: List<Combatant>, activeIndex: Int, portraits: Map<String, String>,
    attackerId: String?, targetId: String?, onTap: (Combatant) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(activeIndex) {
        if (fighters.isNotEmpty()) listState.animateScrollToItem(activeIndex.coerceIn(0, fighters.lastIndex))
    }
    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)) {
        itemsIndexed(fighters, key = { _, f -> f.id }) { index, f ->
            val isActive = index == activeIndex
            val scale by animateFloatAsState(if (isActive) 1.1f else 1f, label = "turnScale")
            val ring = when {
                isActive -> Accent
                f.id == targetId -> Danger
                else -> Color.Transparent
            }
            Column(
                Modifier.width(64.dp).graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (f.currentHp <= 0) 0.55f else 1f }
                    .clickable { onTap(f) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                FighterAvatar(f.name, portraits[f.id].orEmpty(), 46.dp, ring, defeated = f.currentHp <= 0)
                Text(f.name, color = if (isActive) Ink else Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal)
                Text("INI ${f.initiative}" + if (f.id == attackerId && !isActive) " ⚔" else "", color = Muted, fontSize = 11.sp, maxLines = 1)
                val badges = conditionBadges(f.conditions)
                Text(if (badges.isEmpty()) " " else badges, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun RosterRow(
    fighter: Combatant, isActive: Boolean, portrait: String, defenseLabel: String,
    onConditions: () -> Unit, onAdjustHp: (Int) -> Unit, onRemove: () -> Unit
) {
    val hp by animateFloatAsState(fighter.currentHp.toFloat(), tween(450), label = "rosterHp")
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isActive) Panel2 else Night),
        border = if (isActive) BorderStroke(1.dp, Accent.copy(alpha = .6f)) else BorderStroke(1.dp, Hairline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            FighterAvatar(fighter.name, portrait, 42.dp, if (isActive) Accent else Color.Transparent, defeated = fighter.currentHp <= 0)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        fighter.name + if (fighter.isDying()) " (morrendo)" else if (fighter.currentHp <= 0) " (derrotado)" else "",
                        color = if (fighter.currentHp <= 0) Muted else Ink, fontWeight = FontWeight.Bold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(6.dp))
                    InfoPill("INI ${fighter.initiative}")
                }
                HpBar(hp, fighter.maxHp, Modifier.fillMaxWidth())
                Text("PV ${fighter.currentHp}/${fighter.maxHp}${if (fighter.tempHp > 0) " (+${fighter.tempHp} temp.)" else ""} • $defenseLabel ${fighter.armorClass}", color = Muted, fontSize = 13.sp)
                fighter.deathStatus().takeIf { it.isNotBlank() }?.let { Text(it, color = Danger, fontSize = 12.sp) }
                if (fighter.conditions.isNotEmpty()) {
                    Text(fighter.conditions.joinToString("  ") { "${conditionEmoji(it)} ${fighter.conditionLabel(it)}" }, color = Accent, fontSize = 12.sp)
                }
            }
            Column {
                Row {
                    IconButton(onClick = onConditions, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Info, "Condições de ${fighter.name}", tint = if (fighter.conditions.isNotEmpty()) Accent else Muted)
                    }
                    IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.DeleteOutline, "Remover", tint = Muted) }
                }
                Row {
                    IconButton(onClick = { onAdjustHp(-1) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Remove, "Aplicar 1 de dano", tint = Danger) }
                    IconButton(onClick = { onAdjustHp(+1) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Default.Add, "Curar 1 PV", tint = Accent) }
                }
            }
        }
    }
}

// --- tela --------------------------------------------------------------------------------------

/**
 * A batalha: ordem de turnos, palco animado, controles de ataque e lista de participantes.
 * O atacante padrão é quem está na vez; o alvo padrão é o primeiro inimigo em pé.
 *
 * 0.9.14 — a interface depende do papel ([userRole], vindo da ficha ativa):
 *  - "mestre": painel completo (cena, ordem de turnos, PV, condições, participantes e encerrar combate);
 *  - "jogador": o [CombatDeckScreen] com as cartas do personagem ([playerSheetId]) e rolagem rápida por toque.
 * Jogador sem ataques cadastrados cai nos controles manuais, para nunca ficar sem ação.
 */
@Composable
internal fun BattleArenaScreen(
    fighters: List<Combatant>, activeIndex: Int,
    characters: List<CharacterSheet>, monsters: List<MonsterSheet>, library: List<ItemEntry>,
    prefs: SharedPreferences, engine: RpgEngine,
    fallbackVariables: Map<String, Int>, fallbackProfile: SystemProfile,
    defenseLabel: String, log: MutableList<RollLine>,
    round: Int,
    userRole: String = SheetRole.MASTER, playerSheetId: String = "",
    onDamage: (String, Int) -> Combatant?,
    onUpdateFighter: (Combatant) -> Unit,
    onNextTurn: () -> Unit, onEndCombat: () -> Unit,
    onOpenConditions: (String) -> Unit, onAdjustHp: (Combatant, Int) -> Unit, onRemove: (String) -> Unit
) {
    val active = fighters.getOrNull(activeIndex)
    var attackerOverride by rememberSaveable(active?.id) { mutableStateOf("") }
    var targetPick by rememberSaveable { mutableStateOf("") }
    var ranged by rememberSaveable { mutableStateOf(false) }
    var manualAttack by rememberSaveable { mutableStateOf("1d20 + FOR + PROF") }
    var manualDamage by rememberSaveable { mutableStateOf("1d8 + FOR") }
    var animationsOn by remember { mutableStateOf(prefs.getBoolean("engine_anim", true)) }
    var event by remember { mutableStateOf<BattleEvent?>(null) }
    var counter by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(false) }

    val isPlayer = SheetRole.normalize(userRole) == SheetRole.PLAYER
    val playerFighter = if (isPlayer && playerSheetId.isNotBlank()) {
        fighters.firstOrNull { it.sheetKind == CombatantSource.CHARACTER && it.sheetId == playerSheetId }
    } else null
    val attacker = fighters.firstOrNull { it.id == attackerOverride } ?: playerFighter ?: active
    val target = fighters.firstOrNull { it.id == targetPick && it.id != attacker?.id } ?: defaultTarget(attacker, fighters)
    val portraits = fighters.associate { it.id to portraitOf(it, characters, monsters) }

    val attackerCharacter = characters.firstOrNull { attacker != null && attacker.sheetKind == CombatantSource.CHARACTER && it.id == attacker.sheetId }
    val attackerMonster = monsters.firstOrNull { attacker != null && attacker.sheetKind == CombatantSource.MONSTER && it.id == attacker.sheetId }
    val systemId = attackerCharacter?.systemId ?: attackerMonster?.systemId
    val profile = remember(systemId, fallbackProfile.id) { if (systemId != null) SystemProfiles.resolve(prefs, systemId) else fallbackProfile }
    val variables = when {
        attackerCharacter != null -> characterVariables(attackerCharacter, profile, library)
        attackerMonster != null -> monsterVariables(attackerMonster, profile)
        else -> fallbackVariables
    }
    val options = when {
        attackerCharacter != null -> characterAttacks(attackerCharacter, library)
        attackerMonster != null -> monsterAttacks(attackerMonster)
        else -> emptyList()
    }
    var optionIndex by rememberSaveable(attacker?.id) { mutableStateOf(0) }
    val chosen = options.getOrNull(optionIndex)
    // O deck mostra as cartas do personagem do jogador; se ele ainda não entrou na batalha, não mostra as de outro combatente.
    val showDeck = isPlayer && options.isNotEmpty() && (playerSheetId.isBlank() || playerFighter != null)
    val deckActions = remember(options, variables.keys) { options.map { it.toCombatAction(variables.keys) } }

    // Quem rola a salvaguarda é o alvo: o perfil do sistema dele define os atributos e o dado-base.
    val targetCharacter = characters.firstOrNull { target != null && target.sheetKind == CombatantSource.CHARACTER && it.id == target.sheetId }
    val targetMonster = monsters.firstOrNull { target != null && target.sheetKind == CombatantSource.MONSTER && it.id == target.sheetId }
    val targetSystemId = targetCharacter?.systemId ?: targetMonster?.systemId
    val targetProfile = remember(targetSystemId, fallbackProfile.id) { if (targetSystemId != null) SystemProfiles.resolve(prefs, targetSystemId) else fallbackProfile }

    val mods = if (attacker != null && target != null) attackModifiers(attacker.conditions, target.conditions, ranged) else null
    val attackerBlocker = attacker?.let { actionBlocker(it.conditions) }
    val activeBlocker = active?.let { actionBlocker(it.conditions) }
    val canAttack = attacker != null && target != null && attacker.currentHp > 0 && attackerBlocker == null && !busy
    val attackLabel = when {
        fighters.size < 2 -> "Adicione pelo menos dois combatentes"
        attacker == null || target == null -> "Escolha atacante e alvo"
        else -> when {
            attacker.currentHp <= 0 -> "${attacker.name} está derrotado"
            attackerBlocker != null -> "${attacker.name} está $attackerBlocker e não pode agir"
            busy -> "Rolando…"
            else -> "Atacar ${target.name}"
        }
    }

    fun attack(picked: AttackOption? = null) {
        if (attacker == null || target == null || !canAttack) return
        val currentMods = attackModifiers(attacker.conditions, target.conditions, ranged)
        val option = picked ?: chosen ?: AttackOption(attacker.name, manualAttack, manualDamage)
        val outcome = rollOption(engine, option, variables, profile, target.armorClass, currentMods.mode, currentMods.forceCrit)
        val label = if (picked != null || chosen != null) "${attacker.name} • ${option.label}" else attacker.name
        val lines = mutableListOf(outcome.line.copy(text = outcome.line.text.replaceFirst(option.label, label)))
        var dealt = 0
        var note = ""
        var after = target.currentHp
        val damage = outcome.damage
        if (damage != null && outcome.hit != false) {
            val base = damageMultiplierFor(target.defenses, outcome.damageType)
            val multiplier = conditionDamageMultiplier(base, target.conditions)
            dealt = scaleDamage(damage, multiplier)
            note = damageNote(target.name, base, multiplier, outcome.damageType, target.conditions, damage, dealt)
            if (note.isNotBlank()) lines += RollLine(note, true)
            val updated = onDamage(target.id, dealt)
            if (updated != null) {
                after = updated.currentHp
                lines += RollLine("${updated.name}: PV ${updated.currentHp}/${updated.maxHp} ($defenseLabel ${target.armorClass})", null)
                if (updated.currentHp == 0 && target.currentHp > 0) lines += RollLine("${updated.name} foi derrotado!", true)
            }
        }
        counter += 1
        event = BattleEvent(counter, attacker.id, target.id, outcome, dealt, target.currentHp, after, note, lines)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column {
            Text("BATALHA", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Rodada $round • Turno ${activeIndex + 1} de ${fighters.size}${active?.let { " • ${it.name}" } ?: ""}", color = Muted, fontSize = 13.sp)
        }

        TurnStrip(fighters, activeIndex, portraits, attacker?.id, target?.id) { f ->
            if (f.id != attacker?.id) targetPick = f.id
        }

        if (isPlayer && playerSheetId.isNotBlank() && playerFighter == null) {
            Text("Seu personagem ainda não está na batalha. Peça ao Mestre para adicioná-lo.", color = Muted, fontSize = 13.sp)
        }

        if (active != null && (activeBlocker != null || active.currentHp <= 0)) {
            Card(colors = CardDefaults.cardColors(containerColor = Panel2), border = BorderStroke(1.dp, Warn.copy(alpha = .6f)), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (activeBlocker != null) "${conditionEmoji(activeBlocker)} ${active.name} está $activeBlocker e perde o turno."
                        else "💀 ${active.name} está derrotado.",
                        color = Ink, fontSize = 13.sp, modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onNextTurn, enabled = !busy) { Text("Pular turno") }
                }
            }
        }

        BattleStage(
            attacker = attacker, target = target,
            attackerPortrait = attacker?.let { portraits[it.id] }.orEmpty(), targetPortrait = target?.let { portraits[it.id] }.orEmpty(),
            fighters = fighters, event = event, animate = animationsOn, defenseLabel = defenseLabel,
            onPickAttacker = { if (!isPlayer) attackerOverride = it.id }, onPickTarget = { targetPick = it.id },
            onReveal = { e -> e.logLines.forEach { log.push(it) } },
            onBusy = { busy = it }
        )

        if (showDeck) {
            val own = attacker?.sheetId == playerSheetId && attacker?.sheetKind == CombatantSource.CHARACTER
            val turnHint = when {
                attacker == null || target == null -> ""
                own && active?.id != attacker.id -> "Não é a sua vez (turno de ${active?.name ?: "—"}). Alvo: ${target.name}."
                else -> "Toque numa carta para atacar ${target.name}."
            }
            CombatDeckScreen(
                actions = deckActions, enabled = canAttack, hint = turnHint,
                onActionQuickRoll = { action -> attack(action.toAttackOption()) }
            )
            if (!canAttack && attacker != null) Text(attackLabel, color = Muted, fontSize = 12.sp)
        } else {
            if (options.isNotEmpty()) {
                PickButton("Ataque: ${chosen?.label ?: "Fórmulas manuais"}", options.map { it.label } + "Fórmulas manuais") { optionIndex = it }
                if (chosen != null) {
                    Text(listOf(chosen.bonus.ifBlank { if (chosen.autoHit) "sem teste de acerto" else "" }, chosen.damage).filter { it.isNotBlank() }.joinToString(" • "),
                        color = Muted, fontSize = 13.sp)
                }
            }
            if (chosen == null) {
                TextInput("Fórmula de ataque", manualAttack, { manualAttack = it })
                TextInput("Fórmula de dano", manualDamage, { manualDamage = it })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = !ranged, onClick = { ranged = false }, label = { Text("Corpo a corpo") })
            FilterChip(selected = ranged, onClick = { ranged = true }, label = { Text("À distância") })
        }
        mods?.describe()?.forEach { line ->
            val tint = when {
                line.startsWith("Vantagem:") -> Good
                line.startsWith("Desvantagem:") -> Danger
                line.startsWith("Crítico") -> Amber
                else -> Muted
            }
            Text(line, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        if (!showDeck) {
            Button(
                onClick = { attack() }, enabled = canAttack, modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Brand), shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Gavel, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    attackLabel,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }

        SavePanel(
            target = target, profile = targetProfile, character = targetCharacter, monster = targetMonster,
            engine = engine, log = log, busy = busy, onUpdateFighter = onUpdateFighter
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { onNextTurn() }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Próximo turno") }
            if (!isPlayer) OutlinedButton(onClick = onEndCombat, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Encerrar combate") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Animações da batalha", color = Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Switch(checked = animationsOn, onCheckedChange = {
                animationsOn = it
                prefs.edit().putBoolean("engine_anim", it).apply()
            })
        }

        if (!isPlayer) {
            Text("PARTICIPANTES", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            fighters.forEachIndexed { index, f ->
                RosterRow(
                    fighter = f, isActive = index == activeIndex, portrait = portraits[f.id].orEmpty(), defenseLabel = defenseLabel,
                    onConditions = { onOpenConditions(f.id) }, onAdjustHp = { onAdjustHp(f, it) }, onRemove = { onRemove(f.id) }
                )
            }
        }
    }
}
