package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.EntryKind
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.NamedEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TableFlowTest {
    private fun goblin(n: Int, initiative: Int = 0) = Combatant(
        id = "g$n", name = "Goblin $n", initiative = initiative, currentHp = 7, maxHp = 7,
        sheetId = "goblin", sheetKind = CombatantSource.MONSTER
    )
    private fun orc(initiative: Int = 0) = Combatant(
        id = "orc", name = "Orc", initiative = initiative, currentHp = 15, maxHp = 15,
        sheetId = "orc", sheetKind = CombatantSource.MONSTER
    )
    private fun hero(initiative: Int = 0) = Combatant(
        id = "ana", name = "Ana", initiative = initiative, currentHp = 10, maxHp = 10,
        sheetId = "ana", sheetKind = CombatantSource.CHARACTER
    )

    @Test fun firstInitiativeOfAMonsterGroupIsSharedByTheOthersOfTheSameKind() {
        val result = withGroupInitiative(listOf(goblin(1), goblin(2), goblin(3), orc(), hero()), "g2", 14)
        assertEquals(listOf(14, 14, 14), result.filter { it.sheetId == "goblin" }.map { it.initiative })
        assertEquals(0, result.first { it.id == "orc" }.initiative)
        assertEquals(0, result.first { it.id == "ana" }.initiative)
    }

    @Test fun laterEditsChangeOnlyThatCreature() {
        val grouped = withGroupInitiative(listOf(goblin(1), goblin(2)), "g1", 14)
        val result = withGroupInitiative(grouped, "g2", 9)
        assertEquals(14, result.first { it.id == "g1" }.initiative)
        assertEquals(9, result.first { it.id == "g2" }.initiative)
    }

    @Test fun charactersNeverShareInitiative() {
        val other = hero().copy(id = "bia", name = "Bia", sheetId = "bia")
        val result = withGroupInitiative(listOf(hero(), other), "ana", 18)
        assertEquals(18, result.first { it.id == "ana" }.initiative)
        assertEquals(0, result.first { it.id == "bia" }.initiative)
    }

    @Test fun unknownIdChangesNothing() {
        val list = listOf(goblin(1), hero())
        assertEquals(list, withGroupInitiative(list, "nada", 5))
    }

    @Test fun cheatSheetShowsAttackBonusAndDamageReadyToRead() {
        val monster = MonsterSheet(
            id = "goblin", name = "Goblin",
            entries = listOf(
                NamedEntry(EntryKind.TRAIT, "Furtivo", "Pode se esconder como ação bônus."),
                NamedEntry(EntryKind.ACTION, "Cimitarra", "Ataque corpo a corpo: +4 para acertar. Dano: 1d6+2 cortante.")
            )
        )
        val lines = monsterCheatSheet(monster)
        assertEquals(1, lines.size)
        assertTrue(lines[0], lines[0].startsWith("Cimitarra:"))
        assertTrue(lines[0], lines[0].contains("+4 acerto"))
        assertTrue(lines[0], lines[0].contains("1d6+2"))
    }

    @Test fun cheatSheetIsEmptyWhenNoActionCanBeRead() {
        val monster = MonsterSheet(id = "m", entries = listOf(NamedEntry(EntryKind.ACTION, "Rugido", "Todos ficam assustados.")))
        assertTrue(monsterCheatSheet(monster).isEmpty())
    }

    @Test fun encountersRoundTripWithAwkwardNames() {
        val list = listOf(
            Encounter("e1", "Emboscada | na \"ponte\"; 1º", listOf(EncounterEntry("goblin", 4), EncounterEntry("id:com|sinais", 1))),
            Encounter("e2", "Chefe", listOf(EncounterEntry("orc", 1)))
        )
        assertEquals(list, decodeEncounters(encodeEncounters(list)))
    }

    @Test fun emptyOrBrokenEncounterTextDecodesToWhatIsValid() {
        assertTrue(decodeEncounters("").isEmpty())
        val good = Encounter("e1", "Ok", listOf(EncounterEntry("orc", 2)))
        val raw = "lixo sem barras\n" + encodeEncounters(listOf(good)) + "\na|b|orc:xx"
        assertEquals(listOf(good), decodeEncounters(raw))
    }

    @Test fun endTurnMessageRoundTrips() {
        assertEquals("END", ClientMsg.EndTurn.encode())
        assertEquals(ClientMsg.EndTurn, parseClientMsg("END"))
        assertNull(parseClientMsg("END|5"))
    }
}
