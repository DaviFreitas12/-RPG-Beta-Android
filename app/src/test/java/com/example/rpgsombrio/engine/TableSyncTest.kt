package com.example.rpgsombrio.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableSyncTest {
    private fun pc(name: String, hp: Int, max: Int, vararg conds: String) = Combatant(
        id = "id-$name", name = name, initiative = 12, currentHp = hp, maxHp = max, armorClass = 15,
        conditions = conds.toList(), sheetId = "s-$name", sheetKind = CombatantSource.CHARACTER
    )
    private fun orc(hp: Int, max: Int) = Combatant(id = "orc", name = "Orc, o Brutal; chefe", initiative = 9, currentHp = hp, maxHp = max, armorClass = 13)

    @Test fun healthStateBands() {
        assertEquals("Ileso", healthState(20, 20))
        assertEquals("Ferido", healthState(19, 20))
        assertEquals("Ferido", healthState(11, 20))
        assertEquals("Muito ferido", healthState(10, 20))
        assertEquals("Muito ferido", healthState(6, 20))
        assertEquals("Quase morto", healthState(5, 20))
        assertEquals("Quase morto", healthState(1, 20))
        assertEquals("Derrotado", healthState(0, 20))
    }

    @Test fun monsterNumbersNeverReachThePlayers() {
        val snap = buildSnapshot(listOf(pc("Ana", 7, 10), orc(4, 30)), "id-Ana", 2)
        val monster = snap.fighters.first { it.id == "orc" }
        assertFalse(monster.isCharacter)
        assertEquals(0, monster.hp); assertEquals(0, monster.maxHp); assertEquals(0, monster.armorClass)
        assertEquals("Quase morto", monster.state)
        val line = snap.encode()
        assertFalse("o PV do monstro vazou na linha: $line", line.contains(",4,30,"))
        assertEquals(7, snap.fighters.first { it.isCharacter }.hp)
    }

    @Test fun snapshotRoundTripsWithAwkwardNames() {
        val snap = buildSnapshot(listOf(pc("Zé | \"Ágil\", o 1º", 3, 9, "Caído", "Atordoado"), orc(30, 30)), "orc", 5)
        val back = decodeSnapshot(snap.encode())
        assertEquals(snap, back)
        assertEquals("Orc, o Brutal; chefe", back!!.fighters[1].name)
        assertEquals(listOf("Caído", "Atordoado"), back.fighters[0].conditions)
    }

    @Test fun emptyFightRoundTrips() {
        assertEquals(SyncSnapshot(1, "", emptyList()), decodeSnapshot(SyncSnapshot(1, "", emptyList()).encode()))
    }

    @Test fun malformedSnapshotsAreRejected() {
        assertNull(decodeSnapshot("lixo"))
        assertNull(decodeSnapshot("SNAP|x|a|"))
        assertNull(decodeSnapshot("SNAP|1|a|so,um,pedaco"))
        assertNull(decodeSnapshot("SNAP|1|%ZZ|"))
    }

    @Test fun clientMessagesRoundTrip() {
        val hello = ClientMsg.Hello("0427", "sheet-1", "Ana | a Rápida", 17, 8, 12, 14)
        assertEquals(hello, parseClientMsg(hello.encode()))
        assertEquals(ClientMsg.Adjust(-6), parseClientMsg(ClientMsg.Adjust(-6).encode()))
        assertEquals(ClientMsg.SetInitiative(15), parseClientMsg(ClientMsg.SetInitiative(15).encode()))
    }

    @Test fun clientMessagesAreSanitised() {
        assertNull(parseClientMsg("ADJ|abc"))
        assertNull(parseClientMsg("NADA|1"))
        assertNull(parseClientMsg("HELLO|1|2"))
        assertEquals(ClientMsg.Adjust(999), parseClientMsg("ADJ|99999"))
        val h = parseClientMsg("HELLO|0001|p|%20|5|50|10|14") as ClientMsg.Hello
        assertEquals("Jogador", h.name)
        assertEquals(10, h.hp); assertEquals(10, h.maxHp)
    }

    @Test fun helloBecomesACharacterCombatant() {
        val c = helloToCombatant(ClientMsg.Hello("1", "s-9", "Bia", 11, 6, 10, 13), "novo")
        assertEquals(CombatantSource.CHARACTER, c.sheetKind)
        assertEquals("s-9", c.sheetId)
        assertEquals(6, c.currentHp); assertEquals(13, c.armorClass); assertEquals(11, c.initiative)
    }

    @Test fun joinCodeHasFourDigits() {
        repeat(50) { assertTrue(Regex("\\d{4}").matches(newJoinCode())) }
        assertNotNull(newJoinCode())
    }
}
