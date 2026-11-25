package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.CampaignInfo
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Transporte da mesa em rede (0.9.16): TCP puro com uma mensagem por linha (ver TableSync.kt). Sem bibliotecas
 * novas e sem Android: [post] entrega os retornos para a thread principal (no app, um Handler); nos testes, executa na hora.
 * Toda escrita na rede sai por uma thread própria, porque o Android proíbe rede na thread principal.
 */

private class Conn(val id: String, val socket: Socket) {
    val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
    private val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
    @Volatile var authed = false
    @Volatile var playerId = ""

    /** Envia uma linha; false se a conexão caiu. */
    fun send(line: String): Boolean = synchronized(this) {
        try { writer.write(line); writer.write("\n"); writer.flush(); true } catch (_: IOException) { false }
    }

    fun close() { try { socket.close() } catch (_: IOException) { } }
}

private fun newIoThread(): ExecutorService =
    Executors.newSingleThreadExecutor { r -> Thread(r, "mesa-envio").apply { isDaemon = true } }

/** Lado do mestre: aceita jogadores com o código certo e transmite o retrato da luta. */
class TableHost(
    private val code: String,
    private val post: (() -> Unit) -> Unit,
    /** Recebe (idDaFichaDoJogador, mensagem), já na thread principal. */
    private val onMessage: (String, ClientMsg) -> Unit,
    private val onClientCount: (Int) -> Unit,
    /** Linha `CAMP|...` da campanha aberta (vazia se a mesa não está ligada a uma campanha). */
    private val campaignLine: String = "",
    /** Alguém entrou na campanha com o código, já na thread principal. */
    private val onJoinCampaign: (ClientMsg.JoinCampaign) -> Unit = {}
) {
    private val conns = ConcurrentHashMap<String, Conn>()
    private val io = newIoThread()
    @Volatile private var server: ServerSocket? = null
    @Volatile private var lastSnapshot: String = ""

    val boundPort: Int get() = server?.localPort ?: -1

    /** Abre a porta. Devolve null se deu certo, ou o motivo da falha. */
    fun start(port: Int = TABLE_PORT): String? = try {
        val s = ServerSocket()
        s.reuseAddress = true
        s.bind(InetSocketAddress(port))
        server = s
        Thread({ acceptLoop(s) }, "mesa-aceitar").apply { isDaemon = true }.start()
        null
    } catch (e: IOException) {
        e.message ?: "não foi possível abrir a porta $port"
    }

    private fun acceptLoop(s: ServerSocket) {
        while (!s.isClosed) {
            val socket = try { s.accept() } catch (_: IOException) { break }
            try { socket.tcpNoDelay = true } catch (_: IOException) { }
            val conn = Conn(UUID.randomUUID().toString(), socket)
            Thread({ serve(conn) }, "mesa-jogador").apply { isDaemon = true }.start()
        }
    }

    private fun serve(conn: Conn) {
        try {
            conn.socket.soTimeout = 10_000 // quem não se apresenta em 10 s é dispensado
            while (true) {
                val line = conn.reader.readLine() ?: break
                if (line.length > 4096) break
                val msg = parseClientMsg(line)
                if (!conn.authed) {
                    if (msg is ClientMsg.JoinCampaign) {
                        // Entrada na campanha: uma resposta e a conversa acaba (a conexão fecha no finally).
                        if (msg.code != code || campaignLine.isEmpty()) { conn.send("DENIED|codigo"); break }
                        conn.send(campaignLine)
                        post { onJoinCampaign(msg) }
                        break
                    }
                    if (msg !is ClientMsg.Hello) break
                    if (msg.code != code) { conn.send("DENIED|codigo"); break } // esta thread não é a principal, pode escrever direto
                    // Reconexão do mesmo jogador: a conexão antiga (talvez já morta) sai.
                    conns.values.filter { it.playerId == msg.playerId }.forEach { old -> conns.remove(old.id); old.close() }
                    conn.authed = true
                    conn.playerId = msg.playerId
                    conn.socket.soTimeout = 0
                    conns[conn.id] = conn
                    val snapshot = lastSnapshot
                    io.execute { conn.send("WELCOME"); if (campaignLine.isNotEmpty()) conn.send(campaignLine); if (snapshot.isNotEmpty()) conn.send(snapshot) }
                    val size = conns.size
                    post { onClientCount(size) }
                    post { onMessage(msg.playerId, msg) }
                } else if (msg != null) {
                    post { onMessage(conn.playerId, msg) }
                }
            }
        } catch (_: IOException) {
        } finally {
            conn.close()
            if (conn.authed && conns.remove(conn.id) != null) {
                val size = conns.size
                post { onClientCount(size) }
            }
        }
    }

    /** Envia o retrato da luta a todos os jogadores (e guarda para quem entrar depois). */
    fun broadcast(line: String) {
        lastSnapshot = line
        io.execute {
            conns.values.forEach { c ->
                if (!c.send(line)) {
                    c.close()
                    if (conns.remove(c.id) != null) { val size = conns.size; post { onClientCount(size) } }
                }
            }
        }
    }

    fun stop() {
        try { server?.close() } catch (_: IOException) { }
        server = null
        conns.values.forEach { it.close() }
        conns.clear()
        io.shutdownNow()
    }
}

/** Lado do jogador: conecta ao mestre, se apresenta e reconecta sozinho se a rede cair. */
class TableClient(
    private val post: (() -> Unit) -> Unit,
    private val onSnapshot: (SyncSnapshot) -> Unit,
    /** "connecting", "connected", "reconnecting" ou "denied" (código errado: não tenta de novo). */
    private val onStatus: (String) -> Unit,
    /** A campanha do mestre, recebida logo depois de entrar na mesa (0.10.6). */
    private val onCampaign: (CampaignInfo) -> Unit = {}
) {
    @Volatile private var closed = false
    @Volatile private var conn: Conn? = null
    private val io = newIoThread()

    fun connect(host: String, port: Int, hello: ClientMsg.Hello) {
        post { onStatus("connecting") }
        Thread({ run(host, port, hello) }, "mesa-conexao").apply { isDaemon = true }.start()
    }

    private fun run(host: String, port: Int, hello: ClientMsg.Hello) {
        while (!closed) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), 4000)
                socket.tcpNoDelay = true
                val c = Conn("mestre", socket)
                conn = c
                io.execute { c.send(hello.encode()) }
                var denied = false
                while (!closed && !denied) {
                    val line = c.reader.readLine() ?: break
                    if (line == "WELCOME") post { onStatus("connected") }
                    else if (line.startsWith("DENIED")) { denied = true; post { onStatus("denied") } }
                    else if (line.startsWith("SNAP|")) decodeSnapshot(line)?.let { s -> post { onSnapshot(s) } }
                    else if (line.startsWith("CAMP|")) decodeCampaign(line)?.let { info -> post { onCampaign(info) } }
                }
                c.close()
                if (denied) { closed = true; return }
            } catch (_: Exception) {
                // rede fora do ar, mestre ainda não abriu a mesa, endereço inválido...: tenta de novo abaixo
            }
            if (closed) return
            post { onStatus("reconnecting") }
            try { Thread.sleep(3000) } catch (_: InterruptedException) { return }
        }
    }

    fun send(msg: ClientMsg) {
        val c = conn ?: return
        io.execute { c.send(msg.encode()) }
    }

    fun close() {
        closed = true
        conn?.close()
        io.shutdownNow()
    }
}

/** Endereços IPv4 deste aparelho na rede local (Wi-Fi ou zona Wi-Fi do próprio celular). */
fun localIpv4Addresses(): List<String> = try {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filter { it is Inet4Address && it.isSiteLocalAddress }
        .mapNotNull { it.hostAddress }
} catch (_: Exception) {
    emptyList()
}

/** Resultado do pedido de entrada na campanha. */
sealed class JoinResult {
    data class Joined(val campaign: CampaignInfo) : JoinResult()
    object Denied : JoinResult()
    data class Failed(val reason: String) : JoinResult()
}

/**
 * Jogador: pede ao mestre para entrar na campanha com o código (0.10.6). É uma conversa curta: manda uma linha, lê a
 * resposta e fecha. Roda em thread própria; [onResult] volta pela thread principal ([post]).
 */
fun requestCampaign(
    host: String, port: Int, msg: ClientMsg.JoinCampaign,
    post: (() -> Unit) -> Unit, onResult: (JoinResult) -> Unit
) {
    Thread({
        val result: JoinResult = try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(host, port), 4000)
                socket.soTimeout = 5000
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), Charsets.UTF_8))
                writer.write(msg.encode()); writer.write("\n"); writer.flush()
                val line = reader.readLine()
                when {
                    line == null -> JoinResult.Failed("O mestre fechou a conexão. Confira se a mesa está aberta e se ele também está na versão 0.10.6.")
                    line.startsWith("DENIED") -> JoinResult.Denied
                    else -> decodeCampaign(line)?.let { JoinResult.Joined(it) } ?: JoinResult.Failed("Resposta inesperada do mestre.")
                }
            }
        } catch (_: java.net.SocketTimeoutException) {
            JoinResult.Failed("O mestre não respondeu a tempo.")
        } catch (_: Exception) {
            JoinResult.Failed("Não consegui falar com o mestre. Confira o endereço, se a mesa está aberta e se os dois estão na mesma rede Wi-Fi.")
        }
        post { onResult(result) }
    }, "mesa-campanha").apply { isDaemon = true }.start()
}
