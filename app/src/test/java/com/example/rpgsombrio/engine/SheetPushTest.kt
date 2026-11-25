package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.CampaignInfo
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.mergeReceivedSheet
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Envio de fichas do jogador ao mestre (0.10.7): linha da rede, regra de recebimento e conversa em localhost. */
class SheetPushTest {
    private val info = CampaignInfo("c1", "Sombras", "", "dnd5e", "mestre-1", "", "")
    private val post: (() -> Unit) -> Unit = { it() }
    private fun sheet(id: String, owner: String = "", campaign: String = "", image: String = "") =
        CharacterSheet(id = id, name = id, ownerUserId = owner, campaignId = campaign, imageUri = image)

    @Test fun pushLineRoundTripsEvenWithBigAwkwardJson() {
        val json = "{\"name\":\"Zé | \\\"Ágil\\\"\",\"notes\":\"" + "linha|com barra e % de tudo; ".repeat(2000) + "\"}"
        val msg = ClientMsg.PushSheet("4821", "u1", "Ana", json)
        assertEquals(msg, parseClientMsg(msg.encode()))
        assertNull(parseClientMsg("SHEET|4821||Ana|%7B%7D"))
        assertNull(parseClientMsg("SHEET|4821|u1|Ana"))
    }

    @Test fun masterStoresReceivedSheetsInTheCampaignForThatPlayer() {
        val own = sheet("mestre-ficha", owner = "mestre-1", campaign = "c1")
        val first = mergeReceivedSheet(listOf(own), sheet("s1", campaign = "outra", image = "content://foto"), "c1", "u1")!!
        val added = first.first { it.id == "s1" }
        assertEquals("c1", added.campaignId)
        assertEquals("u1", added.ownerUserId)
        assertEquals("", added.imageUri)
        assertEquals(2, first.size)
        // mesmo jogador manda de novo: substitui, não duplica
        val second = mergeReceivedSheet(first, sheet("s1").copy(name = "Ana, a Ágil"), "c1", "u1")!!
        assertEquals(2, second.size)
        assertEquals("Ana, a Ágil", second.first { it.id == "s1" }.name)
    }

    @Test fun nobodyOverwritesAnotherOnesSheet() {
        val list = listOf(sheet("mestre-ficha", owner = "mestre-1", campaign = "c1"), sheet("s1", owner = "u1", campaign = "c1"))
        assertNull(mergeReceivedSheet(list, sheet("mestre-ficha"), "c1", "u2"))
        assertNull(mergeReceivedSheet(list, sheet("s1"), "c1", "u2"))
        assertNull(mergeReceivedSheet(list, sheet("novo"), "c1", ""))
    }

    @Test fun playerPushesSheetsOverTheNetwork() {
        val got = LinkedBlockingQueue<ClientMsg.PushSheet>()
        val host = TableHost("4821", post, { _, _ -> }, { }, info.encode(), onPushSheet = { got.add(it) })
        assertEquals(null, host.start(0))
        try {
            val results = LinkedBlockingQueue<PushResult>()
            pushSheets("127.0.0.1", host.boundPort, "4821", "u1", "Ana", listOf("{\"id\":\"a\"}", "{\"id\":\"b\"}"), post) { results.add(it) }
            assertEquals(PushResult.Sent(2), results.poll(5, TimeUnit.SECONDS))
            assertEquals("{\"id\":\"a\"}", got.poll(5, TimeUnit.SECONDS)?.json)
            assertEquals("{\"id\":\"b\"}", got.poll(5, TimeUnit.SECONDS)?.json)

            pushSheets("127.0.0.1", host.boundPort, "0000", "u1", "Ana", listOf("{\"id\":\"c\"}"), post) { results.add(it) }
            assertEquals(PushResult.Denied, results.poll(5, TimeUnit.SECONDS))
            assertTrue(got.isEmpty())
        } finally {
            host.stop()
        }
    }

    @Test fun tableWithoutCampaignRefusesSheets() {
        val host = TableHost("4821", post, { _, _ -> }, { })
        assertEquals(null, host.start(0))
        try {
            val results = LinkedBlockingQueue<PushResult>()
            pushSheets("127.0.0.1", host.boundPort, "4821", "u1", "Ana", listOf("{}"), post) { results.add(it) }
            assertEquals(PushResult.Denied, results.poll(5, TimeUnit.SECONDS))
        } finally {
            host.stop()
        }
    }
}
