package com.example.rpgsombrio.ui.combat

import android.content.SharedPreferences
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.CombatAction
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.engine.Combatant
import com.example.rpgsombrio.engine.RollLine
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.ui.components.push

@Composable
fun BattleArena(
    fighters: List<Combatant>,
    activeIndex: Int,
    characters: List<CharacterSheet>,
    monsters: List<MonsterSheet>,
    library: List<ItemEntry>,
    prefs: SharedPreferences,
    engine: RpgEngine,
    fallbackVariables: Map<String, Int>,
    fallbackProfile: SystemProfile,
    defenseLabel: String,
    log: MutableList<RollLine>,
    onDamage: (String, Int) -> Combatant?,
    round: Int,
    onUpdateFighter: (Combatant) -> Unit,
    onNextTurn: () -> Unit,
    onEndCombat: () -> Unit,
    onOpenConditions: (String) -> Unit,
    onAdjustHp: (Combatant, Int) -> Unit,
    onRemove: (String) -> Unit
) {
    val actions = remember {
        listOf(
            CombatAction("1", "Ataque Furioso", "1 Vigor", "Cortante", "+8", "1d12+4"),
            CombatAction("2", "Bola de Fogo", "3 Mana", "Fogo", "+6", "8d6", true),
            CombatAction("3", "Apunhalada", "1 Foco", "Perfurante", "+9", "2d6+4", true)
        )
    }

    var selectedTargetId by remember {
        mutableStateOf(fighters.firstOrNull()?.id.orEmpty())
    }

    val selectedTarget = fighters.firstOrNull {
        it.id == selectedTargetId
    }

    val activeFighter = fighters.getOrNull(activeIndex)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "ARENA DE COMBATE",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Rodada $round",
            color = Color.LightGray,
            fontSize = 14.sp
        )

        if (activeFighter != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF382A35)
                )
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        "VEZ DE ${activeFighter.name.uppercase()}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Iniciativa: ${activeFighter.initiative}",
                        color = Color.LightGray
                    )
                }
            }
        }

        Text(
            "COMBATENTES",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(fighters, key = { it.id }) { fighter ->
                val isTarget = fighter.id == selectedTargetId

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTarget) {
                            Color(0xFF49313A)
                        } else {
                            Color(0xFF242424)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    fighter.name,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${fighter.currentHp}/${fighter.maxHp} PV  •  $defenseLabel ${fighter.armorClass}",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                                if (fighter.tempHp > 0) {
                                    Text(
                                        "PV temporários: ${fighter.tempHp}",
                                        color = Color.Cyan,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            if (isTarget) {
                                Text(
                                    "ALVO",
                                    color = Color(0xFFFFB74D),
                                    fontWeight = FontWeight.Bold
                                )
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        selectedTargetId = fighter.id
                                    }
                                ) {
                                    Text("Selecionar")
                                }
                            }
                        }

                        if (fighter.conditions.isNotEmpty()) {
                            Text(
                                "Condições: ${fighter.conditions.joinToString()}",
                                color = Color(0xFFFFCC80),
                                fontSize = 12.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onAdjustHp(fighter, -1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("-1 PV")
                            }

                            OutlinedButton(
                                onClick = { onAdjustHp(fighter, 1) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("+1 PV")
                            }

                            OutlinedButton(
                                onClick = {
                                    onOpenConditions(fighter.id)
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Condições")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { onRemove(fighter.id) }
                            ) {
                                Text(
                                    "Remover",
                                    color = Color(0xFFFF8A80)
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            "DECK DE COMBATE",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            CombatDeckScreen(
                actions = actions,
                onActionQuickRoll = { action ->
                    val target = selectedTarget

                    if (target == null) {
                        log.push(
                            RollLine("Selecione um alvo antes de atacar.", false)
                        )
                    } else {
                        val bonus = action.bonus.trim().let {
                            if (it.startsWith("+") || it.startsWith("-")) it
                            else "+$it"
                        }

                        val attack = engine.roll(
                            "1d20 $bonus",
                            fallbackVariables
                        )

                        val critical = attack.firstD20 == 20
                        val hit = attack.total >= target.armorClass

                        log.push(
                            RollLine(
                                "${action.name}: ${attack.breakdown} " +
                                    "contra ${target.name} " +
                                    "(Defesa ${target.armorClass}). " +
                                    if (hit) "ACERTO!" else "ERRO!",
                                hit
                            )
                        )

                        if (hit) {
                            val damage = engine.roll(
                                action.damage,
                                fallbackVariables,
                                doubleDice = critical
                            )

                            val updated = onDamage(
                                target.id,
                                damage.total
                            )

                            if (updated != null) {
                                log.push(
                                    RollLine(
                                        "Dano ${if (critical) "CRÍTICO " else ""}" +
                                            "${damage.total} em ${target.name}. " +
                                            "PV restantes: ${updated.currentHp}/${updated.maxHp}.",
                                        true
                                    )
                                )
                            }
                        }
                    }
                }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onNextTurn,
                modifier = Modifier.weight(1f),
                enabled = fighters.isNotEmpty()
            ) {
                Text("Próximo turno")
            }

            OutlinedButton(
                onClick = onEndCombat,
                modifier = Modifier.weight(1f),
                enabled = fighters.isNotEmpty()
            ) {
                Text("Encerrar combate")
            }
        }
    }
}
