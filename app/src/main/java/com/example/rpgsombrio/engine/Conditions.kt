package com.example.rpgsombrio.engine

import java.text.Normalizer

// ---------------------------------------------------------------------------------------------
// Efeitos reais das condições no combate (0.9.2).
//
// Lógica pura, sem Compose nem Android: o motor consulta estas regras para pular o turno de quem
// não pode agir, dar vantagem/desvantagem nos ataques e ajustar o dano. As regras seguem o estilo
// D&D 5e; condições sem efeito mecânico claro continuam sendo só marcadores.
// ---------------------------------------------------------------------------------------------

/** Como o d20 do ataque é rolado: normal, com vantagem (maior de dois) ou desvantagem (menor de dois). */
enum class RollMode { NORMAL, ADVANTAGE, DISADVANTAGE }

data class ConditionRule(
    val name: String,
    val emoji: String,
    /** Texto curto mostrado na lista de condições. */
    val summary: String,
    /** Não pode agir: o turno é pulado e ele não consegue atacar. */
    val skipsTurn: Boolean = false,
    /** Os ataques de quem tem a condição ganham vantagem / desvantagem. */
    val attackAdvantage: Boolean = false,
    val attackDisadvantage: Boolean = false,
    /** Os ataques contra quem tem a condição ganham vantagem / desvantagem. */
    val grantsAdvantage: Boolean = false,
    val grantsDisadvantage: Boolean = false,
    /** Acerto corpo a corpo contra o alvo é sempre crítico. */
    val meleeAutoCrit: Boolean = false,
    /** Caído: vantagem contra ele corpo a corpo, desvantagem à distância; o próprio tem desvantagem ao atacar. */
    val prone: Boolean = false,
    /** Resistência a todo dano (Petrificado). */
    val resistsAllDamage: Boolean = false,
    /** Falha automaticamente nas salvaguardas de Força e Destreza (Atordoado, Paralisado, Inconsciente, Petrificado). */
    val failsStrDexSaves: Boolean = false,
    /** Desvantagem nas salvaguardas de Destreza (Imobilizado). */
    val dexSaveDisadvantage: Boolean = false
)

val CONDITION_RULES: List<ConditionRule> = listOf(
    ConditionRule("Agarrado", "✊", "Só marcador: deslocamento 0, sem efeito nas rolagens."),
    ConditionRule("Amedrontado", "😨", "Desvantagem nos ataques de quem está com medo.", attackDisadvantage = true),
    ConditionRule("Atordoado", "💫", "Perde o turno e não age. Ataques contra ele têm vantagem. Falha em salvaguardas de Força e Destreza.",
        skipsTurn = true, grantsAdvantage = true, failsStrDexSaves = true),
    ConditionRule("Caído", "⬇️", "Desvantagem nos próprios ataques. Contra ele: vantagem corpo a corpo, desvantagem à distância.",
        prone = true),
    ConditionRule("Cego", "🙈", "Desvantagem nos próprios ataques. Ataques contra ele têm vantagem.",
        attackDisadvantage = true, grantsAdvantage = true),
    ConditionRule("Enfeitiçado", "💘", "Só marcador: não pode atacar quem o enfeitiçou (decida na mesa)."),
    ConditionRule("Envenenado", "🤢", "Desvantagem nos ataques.", attackDisadvantage = true),
    ConditionRule("Exausto", "🥱", "Só marcador: os níveis de exaustão variam entre os sistemas."),
    ConditionRule("Imobilizado", "⛓️", "Desvantagem nos próprios ataques e nas salvaguardas de Destreza. Ataques contra ele têm vantagem.",
        attackDisadvantage = true, grantsAdvantage = true, dexSaveDisadvantage = true),
    ConditionRule("Incapacitado", "🚫", "Perde o turno e não pode atacar.", skipsTurn = true),
    ConditionRule("Inconsciente", "💤", "Perde o turno. Ataques têm vantagem e acertos corpo a corpo são críticos. Falha em salvaguardas de Força e Destreza.",
        skipsTurn = true, grantsAdvantage = true, meleeAutoCrit = true, failsStrDexSaves = true),
    ConditionRule("Invisível", "👻", "Vantagem nos próprios ataques. Ataques contra ele têm desvantagem.",
        attackAdvantage = true, grantsDisadvantage = true),
    ConditionRule("Paralisado", "⚡", "Perde o turno. Ataques têm vantagem e acertos corpo a corpo são críticos. Falha em salvaguardas de Força e Destreza.",
        skipsTurn = true, grantsAdvantage = true, meleeAutoCrit = true, failsStrDexSaves = true),
    ConditionRule("Petrificado", "🗿", "Perde o turno, ataques contra ele têm vantagem e ele resiste a todo dano. Falha em salvaguardas de Força e Destreza.",
        skipsTurn = true, grantsAdvantage = true, resistsAllDamage = true, failsStrDexSaves = true),
    ConditionRule("Surdo", "🔇", "Só marcador: sem efeito nas rolagens.")
)

private fun conditionKey(raw: String): String =
    Normalizer.normalize(raw.trim().lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

private val RULES_BY_KEY: Map<String, ConditionRule> = CONDITION_RULES.associateBy { conditionKey(it.name) }

/** Regra da condição (sem diferenciar maiúsculas e acentos), ou null para condições livres sem efeito. */
fun conditionRule(name: String): ConditionRule? = RULES_BY_KEY[conditionKey(name)]

fun conditionEmoji(name: String): String = conditionRule(name)?.emoji ?: "•"

fun conditionSummary(name: String): String =
    conditionRule(name)?.summary ?: "Marcador livre, sem efeito automático."

/** A primeira condição que impede de agir (Atordoado, Paralisado…), ou null se a pessoa pode agir. */
fun actionBlocker(conditions: List<String>): String? =
    conditions.firstOrNull { conditionRule(it)?.skipsTurn == true }

/** Em pé e sem condição que impeça de agir. */
fun Combatant.canAct(): Boolean = currentHp > 0 && actionBlocker(conditions) == null

// --- ataques ---------------------------------------------------------------------------------

data class AttackModifiers(
    val mode: RollMode,
    val advantageReasons: List<String> = emptyList(),
    val disadvantageReasons: List<String> = emptyList(),
    /** Se o ataque acertar, é crítico (alvo Paralisado/Inconsciente, corpo a corpo). */
    val forceCrit: Boolean = false,
    val critReason: String = ""
) {
    /** Linhas curtas para mostrar ao jogador antes de atacar. Vazio quando nada muda. */
    fun describe(): List<String> = buildList {
        if (advantageReasons.isNotEmpty() && disadvantageReasons.isNotEmpty()) {
            add("Vantagem (${advantageReasons.joinToString(", ")}) e desvantagem (${disadvantageReasons.joinToString(", ")}) se anulam.")
        } else if (advantageReasons.isNotEmpty()) {
            add("Vantagem: ${advantageReasons.joinToString(", ")}.")
        } else if (disadvantageReasons.isNotEmpty()) {
            add("Desvantagem: ${disadvantageReasons.joinToString(", ")}.")
        }
        if (forceCrit) add("Crítico garantido se acertar: $critReason.")
    }
}

/**
 * Calcula vantagem, desvantagem e crítico automático de um ataque a partir das condições dos dois lados.
 * Como no D&D 5e, qualquer vantagem junto com qualquer desvantagem se anulam. [ranged] = ataque à distância.
 */
fun attackModifiers(attackerConditions: List<String>, targetConditions: List<String>, ranged: Boolean = false): AttackModifiers {
    val advantage = mutableListOf<String>()
    val disadvantage = mutableListOf<String>()
    var critReason = ""
    for (c in attackerConditions) {
        val rule = conditionRule(c) ?: continue
        if (rule.attackAdvantage) advantage += "atacante ${rule.name}"
        if (rule.attackDisadvantage || rule.prone) disadvantage += "atacante ${rule.name}"
    }
    for (c in targetConditions) {
        val rule = conditionRule(c) ?: continue
        if (rule.grantsAdvantage) advantage += "alvo ${rule.name}"
        if (rule.grantsDisadvantage) disadvantage += "alvo ${rule.name}"
        if (rule.prone) {
            if (ranged) disadvantage += "alvo ${rule.name} (à distância)" else advantage += "alvo ${rule.name} (corpo a corpo)"
        }
        if (rule.meleeAutoCrit && !ranged && critReason.isEmpty()) critReason = "alvo ${rule.name}"
    }
    val adv = advantage.distinct()
    val dis = disadvantage.distinct()
    val mode = when {
        adv.isNotEmpty() && dis.isEmpty() -> RollMode.ADVANTAGE
        dis.isNotEmpty() && adv.isEmpty() -> RollMode.DISADVANTAGE
        else -> RollMode.NORMAL
    }
    return AttackModifiers(mode, adv, dis, critReason.isNotEmpty(), critReason)
}

/** Multiplicador final do dano: o das defesas do alvo, mais a resistência a tudo do Petrificado (resistência não acumula). */
fun conditionDamageMultiplier(baseMultiplier: Double, targetConditions: List<String>): Double {
    if (targetConditions.none { conditionRule(it)?.resistsAllDamage == true }) return baseMultiplier
    return when {
        baseMultiplier == 0.0 -> 0.0
        baseMultiplier > 1.0 -> 1.0   // vulnerabilidade e resistência se anulam
        else -> 0.5
    }
}

/** Aplica o multiplicador ao dano, arredondando para baixo (como a resistência já fazia). */
fun scaleDamage(amount: Int, multiplier: Double): Int =
    if (multiplier == 1.0) amount else (amount * multiplier).toInt().coerceAtLeast(0)

// --- duração das condições (rodadas) ---------------------------------------------------------

/** O mesmo nome sem diferenciar maiúsculas e acentos: "caido" e "Caído" são a mesma condição. */
private fun sameCondition(a: String, b: String): Boolean = conditionKey(a) == conditionKey(b)

/**
 * Marca a condição no combatente. Com [rounds] maior que zero ela expira sozinha depois desse número de
 * rodadas; com null ou zero dura até alguém removê-la. Se já estava ativa, só troca a duração.
 */
fun Combatant.withCondition(name: String, rounds: Int? = null): Combatant {
    val label = name.trim()
    if (label.isEmpty()) return this
    val existing = conditions.firstOrNull { sameCondition(it, label) }
    val key = existing ?: label
    val list = if (existing != null) conditions else conditions + label
    val timers = conditionTimers.filterKeys { !sameCondition(it, key) }
    return copy(conditions = list, conditionTimers = if (rounds != null && rounds > 0) timers + (key to rounds) else timers)
}

/** Tira a condição (e o contador dela). Se não estava ativa, devolve o combatente como está. */
fun Combatant.withoutCondition(name: String): Combatant =
    copy(
        conditions = conditions.filter { !sameCondition(it, name) },
        conditionTimers = conditionTimers.filterKeys { !sameCondition(it, name) }
    )

/** Rodadas que faltam para a condição acabar, ou null se ela não tem duração (ou não está ativa). */
fun Combatant.remainingRounds(name: String): Int? {
    if (conditions.none { sameCondition(it, name) }) return null
    return conditionTimers.entries.firstOrNull { sameCondition(it.key, name) }?.value
}

/** "Atordoado (2 rodadas)" para condições com duração; só o nome para as sem duração. */
fun Combatant.conditionLabel(name: String): String =
    remainingRounds(name)?.let { "$name ($it ${if (it == 1) "rodada" else "rodadas"})" } ?: name

/** "por 3 rodadas", "por 1 rodada" ou "até ser removida". */
fun durationText(rounds: Int?): String = when {
    rounds == null || rounds <= 0 -> "até ser removida"
    rounds == 1 -> "por 1 rodada"
    else -> "por $rounds rodadas"
}

/** [fighter] já sem as condições que acabaram e a lista do que expirou (na ordem em que estavam marcadas). */
data class ConditionTick(val fighter: Combatant, val expired: List<String>)

/**
 * Passa um turno de quem está afetado: cada condição com duração perde uma rodada, e as que chegam a zero
 * saem. Chamado ao fim do turno do combatente (inclusive quando o turno dele foi pulado), então "1 rodada"
 * significa que a condição vale até o fim do próximo turno de quem a recebeu.
 */
fun Combatant.tickConditions(): ConditionTick {
    if (conditionTimers.isEmpty()) return ConditionTick(this, emptyList())
    val expired = mutableListOf<String>()
    val timers = LinkedHashMap<String, Int>()
    for (c in conditions) {
        val left = conditionTimers[c] ?: continue
        if (left - 1 <= 0) expired += c else timers[c] = left - 1
    }
    return ConditionTick(copy(conditions = conditions.filter { it !in expired }, conditionTimers = timers), expired)
}

// --- turnos ----------------------------------------------------------------------------------

data class SkippedTurn(val name: String, val condition: String)

/**
 * Resultado de avançar o turno. [skipped] lista quem perdeu a vez por uma condição (quem está a 0 PV é
 * ignorado em silêncio). Se ninguém pode agir, [nobodyCanAct] fica true e a vez só passa para o seguinte.
 *
 * [passed] são os ids de quem teve o turno pulado no caminho (condição ou 0 PV), sem o de quem acabou de jogar:
 * as condições deles também perdem uma rodada. [newRound] é true quando a vez deu a volta na ordem.
 */
data class TurnStep(
    val nextIndex: Int,
    val skipped: List<SkippedTurn>,
    val nobodyCanAct: Boolean = false,
    val passed: List<String> = emptyList(),
    val newRound: Boolean = false
)

fun nextTurn(order: List<Combatant>, currentIndex: Int): TurnStep {
    if (order.isEmpty()) return TurnStep(0, emptyList())
    val skipped = mutableListOf<SkippedTurn>()
    val passed = mutableListOf<String>()
    for (step in 1..order.size) {
        val index = (currentIndex + step) % order.size
        val fighter = order[index]
        if (fighter.currentHp <= 0) { if (index != currentIndex) passed += fighter.id; continue }
        val blocker = actionBlocker(fighter.conditions)
        if (blocker != null) {
            skipped += SkippedTurn(fighter.name, blocker)
            if (index != currentIndex) passed += fighter.id
            continue
        }
        return TurnStep(index, skipped, passed = passed, newRound = currentIndex + step >= order.size)
    }
    return TurnStep((currentIndex + 1) % order.size, emptyList(), nobodyCanAct = true, passed = passed, newRound = currentIndex + 1 >= order.size)
}
