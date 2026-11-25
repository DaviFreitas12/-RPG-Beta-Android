package com.example.rpgsombrio.ui.combat

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.rpgsombrio.data.Campaign
import com.example.rpgsombrio.data.CampaignInfo
import com.example.rpgsombrio.data.CampaignStore
import com.example.rpgsombrio.data.infoOfCampaign
import com.example.rpgsombrio.engine.ClientMsg
import com.example.rpgsombrio.engine.encode
import com.example.rpgsombrio.engine.SyncSnapshot
import com.example.rpgsombrio.engine.TABLE_PORT
import com.example.rpgsombrio.engine.TableClient
import com.example.rpgsombrio.engine.TableHost
import com.example.rpgsombrio.engine.localIpv4Addresses
import com.example.rpgsombrio.engine.newJoinCode

/**
 * A mesa em rede do aparelho (0.9.16). É um objeto único para sobreviver à troca de aba do app: o servidor do mestre
 * continua de pé enquanto ele olha uma ficha. Desde a 0.9.17 as mensagens dos jogadores são aplicadas na hora pelo
 * [CombatController], com qualquer aba aberta. No jogador, o celular vibra quando chega a vez dele.
 *
 * Todos os campos observáveis só mudam na thread principal.
 */
object TableSession {
    private val main = Handler(Looper.getMainLooper())
    private val post: (() -> Unit) -> Unit = { action -> main.post { action() } }

    /** "off", "host" (mestre) ou "client" (jogador). */
    var mode by mutableStateOf("off")
        private set
    /** Cliente: "connecting", "connected", "reconnecting" ou "denied". */
    var status by mutableStateOf("")
        private set
    var code by mutableStateOf("")
        private set
    var addresses by mutableStateOf<List<String>>(emptyList())
        private set
    var clients by mutableStateOf(0)
        private set
    /** Mestre: nome da campanha ligada à mesa (o código da mesa é o código de convite dela). */
    var campaignName by mutableStateOf("")
        private set
    /** Sobe a cada jogador que entra na campanha; a aba Campanha usa para recarregar a lista de membros. */
    var membersTick by mutableStateOf(0)
        private set
    /** Último retrato da luta recebido do mestre (cliente). */
    var snapshot by mutableStateOf<SyncSnapshot?>(null)
        private set
    /** Id da ficha deste jogador (cliente); é também o id dele na lista do mestre. */
    var myId by mutableStateOf("")
        private set

    private var host: TableHost? = null
    private var client: TableClient? = null
    private var appContext: Context? = null

    /** Guarda o contexto do app (usado para vibrar quando chega a vez do jogador). */
    fun bind(context: Context) { appContext = context.applicationContext }

    private fun deliver(playerId: String, msg: ClientMsg) { CombatController.handleRemote(playerId, msg) }

    /** Vibra duas vezes curtas: é a sua vez. */
    @Suppress("DEPRECATION")
    private fun buzz() {
        try {
            val vibrator = appContext?.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 250, 120, 250), -1))
        } catch (_: Exception) { }
    }

    private fun onSnapshot(next: SyncSnapshot) {
        val wasMyTurn = snapshot?.activeId == myId
        snapshot = next
        if (!wasMyTurn && next.fighters.isNotEmpty() && next.activeId == myId) buzz()
    }

    /** Abre a mesa. Devolve null se deu certo ou o motivo da falha. */
    fun startHost(campaign: Campaign? = null): String? {
        stop()
        // Com campanha, o código da mesa é o de convite dela: um código só para entrar na campanha e na mesa.
        val joinCode = campaign?.inviteCode?.takeIf { it.length == 4 && it.all(Char::isDigit) } ?: newJoinCode()
        val line = campaign?.let { infoOfCampaign(it).encode() }.orEmpty()
        val campaignId = campaign?.id.orEmpty()
        val newHost = TableHost(
            code = joinCode, post = post, onMessage = { playerId, msg -> deliver(playerId, msg) }, onClientCount = { count -> clients = count },
            campaignLine = line, onJoinCampaign = { join -> onPlayerJoinedCampaign(campaignId, join) }
        )
        val error = newHost.start(TABLE_PORT)
        if (error != null) return error
        host = newHost
        code = joinCode
        campaignName = campaign?.name.orEmpty()
        addresses = localIpv4Addresses()
        clients = 0
        mode = "host"
        CombatController.publish()
        return null
    }

    fun stop() {
        host?.stop(); host = null
        client?.close(); client = null
        mode = "off"; status = ""; code = ""; addresses = emptyList(); clients = 0; snapshot = null; myId = ""; campaignName = ""
    }

    private fun localPrefs() = appContext?.getSharedPreferences("rpg_local", Context.MODE_PRIVATE)

    /** Mestre: um jogador entrou na campanha pelo código. Guarda o nome na lista de membros. */
    private fun onPlayerJoinedCampaign(campaignId: String, join: ClientMsg.JoinCampaign) {
        val prefs = localPrefs() ?: return
        if (campaignId.isBlank()) return
        CampaignStore.addPlayer(prefs, campaignId, join.userId, join.name)
        membersTick++
    }

    /** Jogador: o mestre mandou a campanha ao entrar na mesa. Guarda (ou atualiza) e deixa ativa. */
    private fun onCampaignReceived(info: CampaignInfo) {
        val prefs = localPrefs() ?: return
        CampaignStore.joinRemote(prefs, info)
    }

    /** Envia o retrato da luta aos jogadores (só no modo mestre). */
    fun broadcast(line: String) { host?.broadcast(line) }

    /** Jogador: conecta ao mestre. [address] pode ser "192.168.1.23" ou "192.168.1.23:8765". */
    fun join(address: String, hello: ClientMsg.Hello) {
        stop()
        val clean = address.trim()
        val host = clean.substringBefore(':')
        val port = clean.substringAfter(':', "").toIntOrNull() ?: TABLE_PORT
        mode = "client"; status = "connecting"; myId = hello.playerId
        val c = TableClient(post, { onSnapshot(it) }, { status = it }, { onCampaignReceived(it) })
        client = c
        c.connect(host, port, hello)
    }

    /** Jogador: pede ao mestre dano/cura ou troca de iniciativa. */
    fun send(msg: ClientMsg) { client?.send(msg) }
}
