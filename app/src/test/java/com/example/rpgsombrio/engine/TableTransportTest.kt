package com.example.rpgsombrio.engine

import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Mesa em rede de ponta a ponta, com sockets reais em localhost (porta livre escolhida pelo sistema). */
class TableTransportTest {
    private val post: (() -> Unit) -> Unit = { it() }
    private val messages = LinkedBlockingQueue<Pair<String, ClientMsg>>()
    private val counts = LinkedBlockingQueue<Int>()
    private lateinit var host: TableHost
    private val clients = mutableListOf<TableClient>()

    private fun <T> next(q: LinkedBlockingQueue<T>, ms: Long = 5000): T =
        q.poll(ms, TimeUnit.MILLISECONDS) ?: throw AssertionError("tempo esgotado")

    private fun client(snaps: LinkedBlockingQueue<SyncSnapshot>, status: LinkedBlockingQueue<String>, hello: ClientMsg.Hello): TableClient {
        val c = TableClient(post, { snaps.add(it) }, { status.add(it) })
        clients += c
        c.connect("127.0.0.1", host.boundPort, hello)
        return c
    }

    @Before fun openTable() {
        host = TableHost("4821", post, { id, msg -> messages.add(id to msg) }, { counts.add(it) })
        assertEquals(null, host.start(0))
    }

    @After fun closeTable() { clients.forEach { it.close() }; host.stop() }

    @Test fun wrongCodeIsDenied() {
        val status = LinkedBlockingQueue<String>()
        client(LinkedBlockingQueue(), status, ClientMsg.Hello("0000", "p1", "Ana", 10, 5, 10, 12))
        assertEquals("connecting", next(status))
        assertEquals("denied", next(status))
        assertTrue(messages.isEmpty())
    }

    @Test fun playerJoinsReceivesSnapshotAndSendsRequests() {
        host.broadcast(buildSnapshot(listOf(Combatant("orc", "Orc", 9, 4, 30, 13)), "orc", 3).encode())
        val snaps = LinkedBlockingQueue<SyncSnapshot>()
        val status = LinkedBlockingQueue<String>()
        val ana = client(snaps, status, ClientMsg.Hello("4821", "p1", "Ana", 14, 8, 12, 15))
        assertEquals("connecting", next(status))
        assertEquals("connected", next(status))
        val first = next(snaps)
        assertEquals(3, first.round)
        assertEquals(0, first.fighters[0].hp) // o PV do monstro não chega ao jogador
        assertEquals(1, next(counts))
        assertEquals("p1", next(messages).first)
        ana.send(ClientMsg.Adjust(-5))
        assertEquals(ClientMsg.Adjust(-5), next(messages).second)
        host.broadcast(SyncSnapshot(4, "p1", emptyList()).encode())
        assertEquals(4, next(snaps).round)
    }

    @Test fun reconnectingPlayerDoesNotCountTwice() {
        val hello = ClientMsg.Hello("4821", "p1", "Ana", 14, 8, 12, 15)
        val first = client(LinkedBlockingQueue(), LinkedBlockingQueue(), hello)
        assertEquals(1, next(counts))
        first.close()
        client(LinkedBlockingQueue(), LinkedBlockingQueue(), hello)
        Thread.sleep(500)
        var last = -1
        while (counts.peek() != null) last = counts.poll()
        assertEquals(1, last)
    }
}
