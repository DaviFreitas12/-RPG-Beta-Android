package com.example.rpgsombrio.ui.combat

import android.content.SharedPreferences
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.engine.ClientMsg
import com.example.rpgsombrio.engine.RpgEngine
import com.example.rpgsombrio.engine.SyncSnapshot
import com.example.rpgsombrio.engine.characterCombatant
import com.example.rpgsombrio.engine.conditionEmoji
import com.example.rpgsombrio.ui.components.EditorCard
import com.example.rpgsombrio.ui.components.NumberField
import com.example.rpgsombrio.ui.components.TextInput
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Amber
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Danger
import com.example.rpgsombrio.ui.theme.Good
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Night
import com.example.rpgsombrio.ui.theme.Panel2

/**
 * Cartão "Mesa em rede" (0.9.16): o mestre abre a mesa e mostra endereço e código; cada jogador digita os dois e entra.
 * Depois de conectado, o jogador vê a ordem de iniciativa, o aviso de "É a sua vez" e o estado (não o PV) das criaturas.
 */
@Composable
internal fun TableLinkCard(
    playerSheet: CharacterSheet?, library: List<ItemEntry>, prefs: SharedPreferences, engine: RpgEngine
) {
    val mode = TableSession.mode
    val view = LocalView.current
    // Com a mesa aberta ou conectada a tela não deve apagar, senão o Android derruba a conexão.
    DisposableEffect(mode) {
        view.keepScreenOn = mode != "off"
        onDispose { view.keepScreenOn = false }
    }

    var error by remember { mutableStateOf("") }
    var address by rememberSaveable { mutableStateOf(prefs.getString("net_host", "") ?: "") }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var initiative by remember { mutableStateOf(0) }

    when (mode) {
        "host" -> EditorCard("MESA EM REDE • ABERTA", "Jogadores na mesma rede Wi-Fi digitam o endereço e o código.") {
            val addresses = TableSession.addresses
            if (addresses.isEmpty()) {
                Text("Sem rede Wi-Fi encontrada. Conecte-se a uma rede ou ligue a zona Wi-Fi (hotspot) deste celular.", color = Amber, fontSize = 13.sp)
            } else {
                addresses.forEach { Text("Endereço: $it", color = Ink, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
                if (addresses.size > 1) Text("Mais de um endereço: use o da rede onde os jogadores estão.", color = Muted, fontSize = 12.sp)
            }
            Text("Código: ${TableSession.code}", color = Accent, fontWeight = FontWeight.Black, fontSize = 26.sp)
            Text("Jogadores conectados: ${TableSession.clients}", color = Muted, fontSize = 13.sp)
            Text("Mantenha este app aberto. A tela fica ligada enquanto a mesa estiver aberta.", color = Muted, fontSize = 12.sp)
            OutlinedButton(onClick = { TableSession.stop() }, modifier = Modifier.fillMaxWidth()) { Text("Fechar mesa") }
        }

        "client" -> {
            EditorCard("MESA EM REDE • JOGADOR", statusText(TableSession.status)) {
                OutlinedButton(onClick = { TableSession.stop() }, modifier = Modifier.fillMaxWidth()) { Text("Sair da mesa") }
            }
            TableSession.snapshot?.let { RemoteOrderView(it, TableSession.myId) }
        }

        else -> EditorCard(
            "MESA EM REDE",
            if (playerSheet == null) "Abra a mesa para os jogadores entrarem pelos celulares (mesma rede Wi-Fi)."
            else "Entre na mesa do mestre pela rede Wi-Fi para ver a ordem de iniciativa e a sua vez."
        ) {
            if (playerSheet == null) {
                Button(
                    onClick = { error = TableSession.startHost().orEmpty() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Brand)
                ) { Text("Abrir mesa") }
            } else {
                TextInput("Endereço do mestre (ex.: 192.168.1.23)", address, { address = it })
                TextInput("Código da mesa", joinCode, { joinCode = it.filter(Char::isDigit).take(4) })
                NumberField("Sua iniciativa (dado + bônus)", initiative, { initiative = it }, Modifier.fillMaxWidth())
                Button(
                    onClick = {
                        if (address.isBlank() || joinCode.length != 4) {
                            error = "Informe o endereço do mestre e o código de 4 dígitos."
                        } else {
                            error = ""
                            prefs.edit().putString("net_host", address.trim()).apply()
                            val profile = SystemProfiles.resolve(prefs, playerSheet.systemId)
                            val me = characterCombatant(engine, playerSheet, profile, library)
                            TableSession.join(
                                address,
                                ClientMsg.Hello(joinCode, playerSheet.id, me.name, initiative, me.currentHp, me.maxHp, me.armorClass)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)
                ) { Text("Entrar na mesa") }
            }
            if (error.isNotBlank()) Text(error, color = Danger, fontSize = 13.sp)
        }
    }
}

private fun statusText(status: String): String = when (status) {
    "connected" -> "Conectado ao mestre."
    "reconnecting" -> "Procurando o mestre… (tentando de novo a cada poucos segundos)"
    "denied" -> "Código errado. Saia da mesa e entre de novo com o código certo."
    else -> "Conectando…"
}

/** A ordem de iniciativa como o jogador a vê. Criaturas mostram só o estado; personagens mostram PV. */
@Composable
internal fun RemoteOrderView(snapshot: SyncSnapshot, myId: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (snapshot.fighters.isEmpty()) Text("Nenhuma luta em andamento.", color = Muted, fontSize = 13.sp)
        val myTurn = snapshot.activeId == myId
        if (snapshot.fighters.isNotEmpty()) Card(
            colors = CardDefaults.cardColors(containerColor = if (myTurn) Good.copy(alpha = .22f) else Panel2),
            border = BorderStroke(1.dp, if (myTurn) Good else Hairline), shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val active = snapshot.fighters.firstOrNull { it.id == snapshot.activeId }
            Text(
                if (myTurn) "É A SUA VEZ!" else "Rodada ${snapshot.round} • Vez de ${active?.name ?: "—"}",
                color = if (myTurn) Good else Ink, fontWeight = FontWeight.Black, fontSize = if (myTurn) 22.sp else 15.sp,
                modifier = Modifier.padding(14.dp)
            )
        }
        snapshot.fighters.forEach { f ->
            val isActive = f.id == snapshot.activeId
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isActive) Panel2 else Night),
                border = BorderStroke(1.dp, if (isActive) Accent.copy(alpha = .6f) else Hairline),
                shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            (if (isActive) "▶ " else "") + f.name + if (f.id == myId) " (você)" else "",
                            color = if (f.hp <= 0 && f.isCharacter) Muted else Ink, fontWeight = FontWeight.Bold,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (f.isCharacter) "PV ${f.hp}/${f.maxHp}" else f.state,
                            color = if (f.state == "Derrotado" || f.state == "Quase morto") Danger else Muted, fontSize = 13.sp
                        )
                        if (f.conditions.isNotEmpty()) {
                            Text(f.conditions.joinToString(" ") { "${conditionEmoji(it)} $it" }, color = Accent, fontSize = 12.sp)
                        }
                    }
                    Text("INI ${f.initiative}", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}
