package com.example.rpgsombrio.engine

import com.example.rpgsombrio.data.CampaignInfo
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * Mesa em rede (0.9.16): o celular do mestre é a fonte da verdade e os jogadores se conectam a ele pela rede local.
 * Este arquivo é Kotlin puro (sem Android): estado visível dos monstros, o "retrato" da luta que o mestre envia
 * e as mensagens do jogador, tudo em linhas de texto simples (uma mensagem por linha, campos separados por '|').
 *
 * Os jogadores veem o PV exato dos personagens, mas dos monstros só um estado ("Ferido", "Quase morto"...).
 */

/** Porta TCP da mesa. */
const val TABLE_PORT = 8765

/** Estado de saúde que o jogador vê de um combatente: nunca o número de PV. */
fun healthState(current: Int, max: Int): String = when {
    max <= 0 -> "—"
    current <= 0 -> "Derrotado"
    current >= max -> "Ileso"
    current * 100 > max * 50 -> "Ferido"
    current * 100 > max * 25 -> "Muito ferido"
    else -> "Quase morto"
}

/** Um combatente como o jogador o enxerga. Para criaturas, [hp], [maxHp], [tempHp] e [armorClass] vão zerados. */
data class SyncFighter(
    val id: String, val name: String, val initiative: Int, val isCharacter: Boolean,
    val hp: Int, val maxHp: Int, val tempHp: Int, val armorClass: Int,
    val state: String, val conditions: List<String>
)

/** A luta num instante: rodada, quem está na vez e a ordem de iniciativa. */
data class SyncSnapshot(val round: Int, val activeId: String, val fighters: List<SyncFighter>)

/** Monta o retrato a partir da lista já ordenada por iniciativa. Dados de criaturas são ocultados aqui. */
fun buildSnapshot(ordered: List<Combatant>, activeId: String, round: Int): SyncSnapshot =
    SyncSnapshot(
        round = round, activeId = activeId,
        fighters = ordered.map { f ->
            val character = f.sheetKind == CombatantSource.CHARACTER
            SyncFighter(
                id = f.id, name = f.name, initiative = f.initiative, isCharacter = character,
                hp = if (character) f.currentHp else 0, maxHp = if (character) f.maxHp else 0,
                tempHp = if (character) f.tempHp else 0, armorClass = if (character) f.armorClass else 0,
                state = healthState(f.currentHp, f.maxHp), conditions = f.conditions
            )
        }
    )

private fun enc(text: String): String = URLEncoder.encode(text, "UTF-8")
private fun dec(text: String): String = URLDecoder.decode(text, "UTF-8")

/** Uma linha: `SNAP|rodada|idDaVez|combatente;combatente;...`. */
fun SyncSnapshot.encode(): String {
    val body = fighters.joinToString(";") { f ->
        listOf(
            enc(f.id), enc(f.name), f.initiative.toString(), if (f.isCharacter) "P" else "M",
            f.hp.toString(), f.maxHp.toString(), f.tempHp.toString(), f.armorClass.toString(),
            enc(f.state), f.conditions.joinToString("~") { enc(it) }
        ).joinToString(",")
    }
    return "SNAP|$round|${enc(activeId)}|$body"
}

/** Lê uma linha `SNAP|...`; devolve null se estiver malformada. */
fun decodeSnapshot(line: String): SyncSnapshot? {
    return try {
        val parts = line.split('|')
        if (parts.size != 4 || parts[0] != "SNAP") return null
        val round = parts[1].toIntOrNull() ?: return null
        val fighters = if (parts[3].isEmpty()) emptyList() else parts[3].split(';').map { item ->
            val f = item.split(',')
            if (f.size != 10) return null
            SyncFighter(
                id = dec(f[0]), name = dec(f[1]), initiative = f[2].toIntOrNull() ?: return null,
                isCharacter = f[3] == "P",
                hp = f[4].toIntOrNull() ?: return null, maxHp = f[5].toIntOrNull() ?: return null,
                tempHp = f[6].toIntOrNull() ?: return null, armorClass = f[7].toIntOrNull() ?: return null,
                state = dec(f[8]),
                conditions = if (f[9].isEmpty()) emptyList() else f[9].split('~').map { dec(it) }
            )
        }
        SyncSnapshot(round, dec(parts[2]), fighters)
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** O que o jogador pode pedir ao mestre. */
sealed class ClientMsg {
    /** Entrada na mesa. [playerId] é o id da ficha do jogador; serve para reconhecê-lo se a conexão cair. */
    data class Hello(
        val code: String, val playerId: String, val name: String, val initiative: Int,
        val hp: Int, val maxHp: Int, val armorClass: Int
    ) : ClientMsg()

    /** Entrada na campanha (0.10.6): o jogador manda o código e quem ele é; o mestre responde com a campanha e a conversa acaba. */
    data class JoinCampaign(val code: String, val userId: String, val name: String) : ClientMsg()

    /** Dano (negativo) ou cura (positivo) no próprio personagem. */
    data class Adjust(val delta: Int) : ClientMsg()

    /** Troca a iniciativa do próprio personagem. */
    data class SetInitiative(val value: Int) : ClientMsg()

    /** "Terminei meu turno": o mestre passa a vez, mas só se for mesmo a vez de quem pediu. */
    object EndTurn : ClientMsg()
}

fun ClientMsg.encode(): String = when (this) {
    is ClientMsg.Hello -> listOf("HELLO", enc(code), enc(playerId), enc(name), initiative, hp, maxHp, armorClass).joinToString("|")
    is ClientMsg.JoinCampaign -> listOf("JOINC", enc(code), enc(userId), enc(name)).joinToString("|")
    is ClientMsg.Adjust -> "ADJ|$delta"
    is ClientMsg.SetInitiative -> "INI|$value"
    is ClientMsg.EndTurn -> "END"
}

/** Lê uma mensagem do jogador, limitando os valores a faixas razoáveis. Devolve null se for inválida. */
fun parseClientMsg(line: String): ClientMsg? {
    return try {
        val p = line.trim().split('|')
        when (p[0]) {
            "HELLO" -> {
                if (p.size != 8) return null
                val maxHp = (p[6].toIntOrNull() ?: return null).coerceIn(1, 9999)
                ClientMsg.Hello(
                    code = dec(p[1]), playerId = dec(p[2]).take(64), name = dec(p[3]).trim().take(40).ifBlank { "Jogador" },
                    initiative = (p[4].toIntOrNull() ?: return null).coerceIn(-99, 999),
                    hp = (p[5].toIntOrNull() ?: return null).coerceIn(0, maxHp), maxHp = maxHp,
                    armorClass = (p[7].toIntOrNull() ?: return null).coerceIn(0, 99)
                )
            }
            "JOINC" -> if (p.size != 4 || dec(p[2]).isBlank()) null else ClientMsg.JoinCampaign(
                code = dec(p[1]), userId = dec(p[2]).take(64), name = dec(p[3]).trim().take(40).ifBlank { "Jogador" }
            )
            "ADJ" -> if (p.size != 2) null else ClientMsg.Adjust((p[1].toIntOrNull() ?: return null).coerceIn(-999, 999))
            "INI" -> if (p.size != 2) null else ClientMsg.SetInitiative((p[1].toIntOrNull() ?: return null).coerceIn(-99, 999))
            "END" -> if (p.size != 1) null else ClientMsg.EndTurn
            else -> null
        }
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** Combatente que o mestre cria quando um jogador entra pela rede. */
fun helloToCombatant(hello: ClientMsg.Hello, id: String): Combatant = Combatant(
    id = id, name = hello.name, initiative = hello.initiative,
    currentHp = hello.hp, maxHp = hello.maxHp, armorClass = hello.armorClass,
    sheetId = hello.playerId, sheetKind = CombatantSource.CHARACTER
)

/** Código de 4 dígitos da sessão. */
fun newJoinCode(random: kotlin.random.Random = kotlin.random.Random.Default): String =
    random.nextInt(0, 10000).toString().padStart(4, '0')

/** Uma linha: `CAMP|id|nome|descrição|sistema|idDoMestre|criação|vida` (campos codificados). */
fun CampaignInfo.encode(): String =
    listOf("CAMP", enc(id), enc(name), enc(description), enc(systemId), enc(masterUserId), enc(creationMethod), enc(hpMethod))
        .joinToString("|")

/** Lê uma linha `CAMP|...`; devolve null se estiver malformada ou sem id. */
fun decodeCampaign(line: String): CampaignInfo? {
    return try {
        val p = line.trim().split('|')
        if (p.size != 8 || p[0] != "CAMP") return null
        val id = dec(p[1])
        if (id.isBlank()) return null
        CampaignInfo(
            id = id, name = dec(p[2]).trim().take(80).ifBlank { "Campanha" }, description = dec(p[3]).take(500),
            systemId = dec(p[4]).ifBlank { "dnd5e" }, masterUserId = dec(p[5]), creationMethod = dec(p[6]), hpMethod = dec(p[7])
        )
    } catch (_: IllegalArgumentException) {
        null
    }
}

/** "192.168.1.23" ou "192.168.1.23:8765" vira (endereço, porta). */
fun splitAddress(address: String): Pair<String, Int> {
    val clean = address.trim()
    return clean.substringBefore(':') to (clean.substringAfter(':', "").toIntOrNull() ?: TABLE_PORT)
}
