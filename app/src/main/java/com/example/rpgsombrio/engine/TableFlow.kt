package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.EntryKind
import com.example.rpgsombrio.data.MonsterSheet
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Fluxo da mesa (0.9.17): regras puras (sem Android) que deixam o combate presencial mais rápido.
 *  - iniciativa em grupo para criaturas do mesmo tipo;
 *  - "cola" de ações da criatura com os números já prontos para o mestre ler;
 *  - encontros preparados antes da sessão.
 */

/**
 * Troca a iniciativa de [id]. Se for a primeira vez (iniciativa 0, "INI ?") de uma criatura, todas as
 * outras do mesmo tipo que também estão sem iniciativa recebem o mesmo valor: o grupo age junto.
 * Depois disso, cada criatura pode ser ajustada sozinha.
 */
fun withGroupInitiative(list: List<Combatant>, id: String, value: Int): List<Combatant> {
    val target = list.firstOrNull { it.id == id } ?: return list
    val group = target.sheetKind == CombatantSource.MONSTER && target.sheetId.isNotBlank() && target.initiative == 0
    return list.map { c ->
        when {
            c.id == id -> c.copy(initiative = value)
            group && c.sheetKind == CombatantSource.MONSTER && c.sheetId == target.sheetId && c.initiative == 0 ->
                c.copy(initiative = value)
            else -> c
        }
    }
}

/** Uma linha por ação da criatura que tenha bônus de ataque ou dano reconhecível: "Garras: +4 acerto · 2d4+2 perfurante". */
fun monsterCheatSheet(monster: MonsterSheet): List<String> =
    monster.entries
        .filter { it.kind == EntryKind.ACTION || it.kind == EntryKind.LEGENDARY }
        .mapNotNull { entry ->
            val parsed = parseMonsterAction(entry.text) ?: return@mapNotNull null
            val parts = mutableListOf<String>()
            parsed.attackBonus?.let { parts += (if (it >= 0) "+$it" else "$it") + " acerto" }
            val damage = listOf(parsed.damage, parsed.damageType).filter { it.isNotBlank() }.joinToString(" ")
            if (damage.isNotBlank()) parts += damage
            "${entry.name.ifBlank { "Ação" }}: ${parts.joinToString(" · ")}"
        }

// --- encontros preparados --------------------------------------------------------------------

data class EncounterEntry(val monsterId: String, val quantity: Int)

data class Encounter(val id: String, val name: String, val entries: List<EncounterEntry>)

private fun enc(text: String): String = URLEncoder.encode(text, "UTF-8")
private fun dec(text: String): String = URLDecoder.decode(text, "UTF-8")

/** Uma linha por encontro: `id|nome|idCriatura:quantidade;idCriatura:quantidade` (campos codificados). */
fun encodeEncounters(list: List<Encounter>): String =
    list.joinToString("\n") { e ->
        listOf(enc(e.id), enc(e.name), e.entries.joinToString(";") { "${enc(it.monsterId)}:${it.quantity}" }).joinToString("|")
    }

/** Lê o texto gravado por [encodeEncounters]; linhas malformadas são ignoradas. */
fun decodeEncounters(raw: String): List<Encounter> =
    raw.lines().mapNotNull { line ->
        try {
            if (line.isBlank()) return@mapNotNull null
            val p = line.split('|')
            if (p.size != 3) return@mapNotNull null
            val entries = if (p[2].isEmpty()) emptyList() else p[2].split(';').map { item ->
                val parts = item.split(':')
                if (parts.size != 2) return@mapNotNull null
                EncounterEntry(dec(parts[0]), (parts[1].toIntOrNull() ?: return@mapNotNull null).coerceIn(1, 50))
            }
            Encounter(dec(p[0]), dec(p[1]), entries)
        } catch (_: IllegalArgumentException) {
            null
        }
    }
