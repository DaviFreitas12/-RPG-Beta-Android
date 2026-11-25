package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.CampaignInfo
import com.example.rpgsombrio.data.CampaignPlayer
import com.example.rpgsombrio.data.CampaignRole
import com.example.rpgsombrio.data.campaignFromMaster
import com.example.rpgsombrio.data.campaignRoleOf
import com.example.rpgsombrio.data.upsertPlayer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Entrada na campanha por código (0.10.6): linhas da rede, regras puras e a conversa de ponta a ponta em localhost. */
class CampaignJoinTest {
    private val info = CampaignInfo("c1", "A Maldição, de \"Strahd\" | 1º ato", "Terra de névoa; tudo", "dnd5e", "mestre-1", "POINT_BUY", "")
    private val post: (() -> Unit) -> Unit = { it() }

    @Test fun campaignLineRoundTrips() {
        assertEquals(info, decodeCampaign(info.encode()))
    }

    @Test fun malformedCampaignLinesAreRejected() {
        assertNull(decodeCampaign("CAMP|so|tres"))
        assertNull(decodeCampaign("SNAP|1|a|"))
        assertNull(decodeCampaign("CAMP||n|d|s|m|c|h")) // sem id
        assertNull(decodeCampaign("CAMP|%ZZ|n|d|s|m|c|h")) // codificação inválida
    }

    @Test fun joinMessageRoundTripsAndRejectsBlankUser() {
        val msg = ClientMsg.JoinCampaign("4821", "u|1", "Zé | Ágil")
        assertEquals(msg, parseClientMsg(msg.encode()))
        assertNull(parseClientMsg("JOINC|4821||Ana"))
        assertNull(parseClientMsg("JOINC|4821"))
    }

    @Test fun splitAddressDefaultsThePort() {
        assertEquals("192.168.1.23" to TABLE_PORT, splitAddress(" 192.168.1.23 "))
        assertEquals("10.0.0.5" to 9000, splitAddress("10.0.0.5:9000"))
    }

    @Test fun upsertPlayerDoesNotDuplicate() {
        val one = upsertPlayer(emptyList(), "u1", "Ana")
        val again = upsertPlayer(one, "u1", "Ana Maria")
        assertEquals(listOf(CampaignPlayer("u1", "Ana Maria")), again)
        assertEquals(2, upsertPlayer(again, "u2", " ").size)
        assertEquals("Jogador", upsertPlayer(emptyList(), "u3", "  ").first().name)
        assertEquals(again, upsertPlayer(again, "", "Fantasma"))
    }

    @Test fun joinedCampaignIsAPlayerCampaignWithoutInvite() {
        val c = campaignFromMaster(info)
        assertEquals(info.id, c.id)
        assertEquals(CampaignRole.PLAYER, campaignRoleOf(c, "jogador-9"))
        assertEquals(CampaignRole.MASTER, campaignRoleOf(c, "mestre-1"))
        assertNull(c.inviteCode)
        assertFalse(c.isArchived)
    }

    @Test fun playerJoinsCampaignOverTheNetwork() {
        val joined = LinkedBlockingQueue<ClientMsg.JoinCampaign>()
        val host = TableHost("4821", post, { _, _ -> }, { }, info.encode()) { joined.add(it) }
        assertEquals(null, host.start(0))
        try {
            val results = LinkedBlockingQueue<JoinResult>()
            requestCampaign("127.0.0.1", host.boundPort, ClientMsg.JoinCampaign("4821", "u1", "Ana"), post) { results.add(it) }
            assertEquals(JoinResult.Joined(info), results.poll(5, TimeUnit.SECONDS))
            assertEquals("Ana", joined.poll(5, TimeUnit.SECONDS)?.name)

            requestCampaign("127.0.0.1", host.boundPort, ClientMsg.JoinCampaign("0000", "u2", "Bia"), post) { results.add(it) }
            assertEquals(JoinResult.Denied, results.poll(5, TimeUnit.SECONDS))
            assertTrue(joined.isEmpty())
        } finally {
            host.stop()
        }
    }

    @Test fun tableWithoutCampaignDeniesJoin() {
        val host = TableHost("4821", post, { _, _ -> }, { })
        assertEquals(null, host.start(0))
        try {
            val results = LinkedBlockingQueue<JoinResult>()
            requestCampaign("127.0.0.1", host.boundPort, ClientMsg.JoinCampaign("4821", "u1", "Ana"), post) { results.add(it) }
            assertEquals(JoinResult.Denied, results.poll(5, TimeUnit.SECONDS))
        } finally {
            host.stop()
        }
    }

    @Test fun nobodyAnswersGivesAFriendlyFailure() {
        val results = LinkedBlockingQueue<JoinResult>()
        requestCampaign("127.0.0.1", 1, ClientMsg.JoinCampaign("4821", "u1", "Ana"), post) { results.add(it) }
        assertTrue(results.poll(8, TimeUnit.SECONDS) is JoinResult.Failed)
    }

    @Test fun playerWhoJoinsTheTableAlsoReceivesTheCampaign() {
        val host = TableHost("4821", post, { _, _ -> }, { }, info.encode())
        assertEquals(null, host.start(0))
        val got = LinkedBlockingQueue<CampaignInfo>()
        val client = TableClient(post, { }, { }, { got.add(it) })
        try {
            client.connect("127.0.0.1", host.boundPort, ClientMsg.Hello("4821", "p1", "Ana", 12, 8, 10, 14))
            assertEquals(info, got.poll(5, TimeUnit.SECONDS))
        } finally {
            client.close(); host.stop()
        }
    }
}
