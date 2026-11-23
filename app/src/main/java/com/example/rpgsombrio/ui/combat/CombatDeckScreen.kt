package com.example.rpgsombrio.ui.combat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CombatAction
import com.example.rpgsombrio.data.SheetRole
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Muted

/**
 * Deck tático (0.9.14): a "mão" de cartas do personagem ativo. Ao tocar numa carta, [onActionQuickRoll] dispara
 * na hora o ataque e o dano daquela ação no motor de regras, sem procurar valores na ficha.
 *
 * As cartas ficam em duas colunas; a tela não rola sozinha, porque vive dentro da lista da arena.
 */
@Composable
internal fun CombatDeckScreen(
    actions: List<CombatAction>,
    onActionQuickRoll: (CombatAction) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    hint: String = ""
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("SUA MÃO", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
        if (hint.isNotBlank()) Text(hint, color = Muted, fontSize = 13.sp)
        if (actions.isEmpty()) {
            Text("Este personagem ainda não tem ataques. Cadastre-os na ficha (aba Combate).", color = Muted, fontSize = 13.sp)
        }
        actions.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { action ->
                    CombatActionCard(action, Modifier.weight(1f), enabled = enabled) { onActionQuickRoll(action) }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Escolha do papel na mesa: define se a arena abre o painel do Mestre ou o deck do jogador. */
@Composable
internal fun RoleSelector(role: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val current = SheetRole.normalize(role)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = current == SheetRole.PLAYER, onClick = { onChange(SheetRole.PLAYER) }, label = { Text("Jogador") })
        FilterChip(selected = current == SheetRole.MASTER, onClick = { onChange(SheetRole.MASTER) }, label = { Text("Mestre") })
    }
}
