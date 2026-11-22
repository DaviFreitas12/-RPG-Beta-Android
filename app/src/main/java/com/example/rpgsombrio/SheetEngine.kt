package com.example.rpgsombrio

import com.example.rpgsombrio.data.fitsSystem
import com.example.rpgsombrio.data.ARMOR_TYPE
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.EntryKind
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.MonsterSheet
import com.example.rpgsombrio.data.ResourceValue
import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.SheetResource
import com.example.rpgsombrio.data.SkillEntry
import com.example.rpgsombrio.data.SpellEntry
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.shortResourceLabel
import java.text.Normalizer
import java.util.UUID

// ---------------------------------------------------------------------------------------------
// Ligação entre fichas / criaturas / magias e o motor (Beta 0.9.1).
//
// Tudo aqui é lógica pura (sem Compose e sem Android), para que possa ser testada com
// um rolador de dados determinístico. As telas só chamam estas funções e exibem o resultado.
// ---------------------------------------------------------------------------------------------

/** Uma linha do histórico de rolagens. [good] = true (sucesso), false (falha) ou null (neutro). */
data class RollLine(val text: String, val good: Boolean? = null)

private fun sgn(n: Int): String = if (n >= 0) "+$n" else "$n"

// --- nomes de variáveis ---------------------------------------------------------------------

/**
 * Converte um rótulo qualquer ("Força", "Lidar com Animais", "d6") num nome que o motor aceita como
 * variável: maiúsculas, sem acento, só letras/dígitos/"_". Nomes que o parser confundiria com dado
 * ("D6") ou número ("1x") ganham um "_" na frente. Devolve "" quando não sobra nada.
 */
fun variableName(raw: String): String {
    val plain = Normalizer.normalize(raw.trim().uppercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    var name = plain.replace(Regex("[^A-Z0-9]+"), "_").trim('_')
    if (name.isEmpty()) return ""
    if (name[0].isDigit() || Regex("^D\\d").containsMatchIn(name)) name = "_$name"
    return name
}

private class VariableBuilder {
    val map = LinkedHashMap<String, Int>()

    /** O primeiro nome registrado vence: PROF/NIVEL/CA e atributos têm prioridade sobre perícias. */
    fun put(rawName: String, value: Int) {
        val name = variableName(rawName)
        if (name.isNotEmpty()) map.putIfAbsent(name, value)
    }
}

/** Nome da variável de um atributo: a sigla, ou o nome por extenso se a sigla estiver vazia. */
fun attributeVariable(attr: SheetAttr): String = variableName(attr.abbr).ifEmpty { variableName(attr.label) }

fun primaryResource(profile: SystemProfile): SheetResource? = profile.resources.firstOrNull()

fun attributeValue(attributes: Map<String, Int>, attr: SheetAttr): Int = attributes[attr.key] ?: attr.default

/** O que entra nas fórmulas: o modificador quando o atributo calcula um, senão o próprio valor. */
fun attributeBonus(attr: SheetAttr, value: Int): Int = if (attr.hasModifier) attr.modifierOf(value) else value

/** Bônus total de uma perícia, como mostrado na ficha. */
fun skillBonus(profile: SystemProfile, attributes: Map<String, Int>, proficiencyBonus: Int, skill: SkillEntry): Int {
    if (!profile.usesProficiency) return skill.extra
    val attr = profile.attr(skill.attributeKey)
    val mod = if (attr != null && attr.hasModifier) attr.modifierOf(attributes[attr.key] ?: attr.default) else 0
    return mod + (if (skill.proficient) proficiencyBonus else 0) + skill.extra
}

/** Tamanho da reserva de dados de uma perícia: valor do atributo ligado + pontos na perícia. */
fun skillPoolSize(profile: SystemProfile, attributes: Map<String, Int>, skill: SkillEntry): Int {
    val attr = profile.attr(skill.attributeKey)
    val base = if (attr != null) attributeValue(attributes, attr) else 0
    return (base + skill.extra).coerceAtLeast(0)
}

private fun VariableBuilder.addAttributes(profile: SystemProfile, attributes: Map<String, Int>) {
    profile.attributes.forEach { attr ->
        val value = attributeValue(attributes, attr)
        val bonus = attributeBonus(attr, value)
        put(attr.abbr, bonus)
        put(attr.label, bonus)
        put(attr.abbr + "_VAL", value)
    }
}

/**
 * Variáveis que a ficha entrega ao motor. Para cada atributo existem `SIGLA` (modificador, ou o valor
 * quando o sistema não usa modificador), `NOME` e `SIGLA_VAL` (valor bruto). Também entram `PROF`,
 * `NIVEL`, `CA`, cada recurso (`PV`, `PV_MAX`...) e cada perícia pelo nome (`ATLETISMO`).
 */
fun characterVariables(sheet: CharacterSheet, profile: SystemProfile, items: List<ItemEntry> = emptyList()): Map<String, Int> {
    val b = VariableBuilder()
    if (profile.usesProficiency) b.put("PROF", sheet.proficiencyBonus)
    if (profile.usesLevels) b.put("NIVEL", sheet.level)
    if (profile.usesArmorClass) b.put("CA", effectiveArmorClass(sheet, items))
    b.addAttributes(profile, sheet.attributes)
    profile.resources.forEach { r ->
        val v = sheet.resources[r.key] ?: ResourceValue(r.defaultMax, r.defaultMax)
        val short = shortResourceLabel(r)
        b.put(short, v.current); b.put(short + "_MAX", v.max)
        b.put(r.label, v.current); b.put(r.label + "_MAX", v.max)
    }
    sheet.skills.filter { it.fitsSystem(sheet.systemId) }.forEach { skill ->
        b.put(skill.name, if (profile.isPool) skillPoolSize(profile, sheet.attributes, skill)
        else skillBonus(profile, sheet.attributes, sheet.proficiencyBonus, skill))
    }
    return b.map
}

/** CA final da ficha e como ela foi composta. */
data class ArmorBreakdown(val total: Int, val text: String, val modified: Boolean)

/**
 * CA com os itens equipados: a maior armadura equipada (tipo Armadura) substitui a CA base da ficha, e os
 * demais itens equipados com valor de CA (escudo, anel…) somam como bônus. Itens com CA 0 são ignorados.
 */
fun armorClassBreakdown(sheet: CharacterSheet, items: List<ItemEntry>): ArmorBreakdown {
    val equipped = sheet.inventory.filter { it.equipped }
        .mapNotNull { entry -> items.firstOrNull { it.id == entry.itemId && it.fitsSystem(sheet.systemId) } }
        .filter { it.armorClass != 0 }
    val armor = equipped.filter { it.type == ARMOR_TYPE }.maxByOrNull { it.armorClass }
    val bonuses = equipped.filter { it.type != ARMOR_TYPE }
    val base = armor?.armorClass ?: sheet.armorClass
    val parts = mutableListOf(if (armor != null) "${armor.name.ifBlank { "Armadura" }} $base" else "base $base")
    bonuses.forEach { parts.add("${it.name.ifBlank { "Item" }} ${sgn(it.armorClass)}") }
    return ArmorBreakdown(base + bonuses.sumOf { it.armorClass }, parts.joinToString(" • "), armor != null || bonuses.isNotEmpty())
}

fun effectiveArmorClass(sheet: CharacterSheet, items: List<ItemEntry>): Int = armorClassBreakdown(sheet, items).total

/** Variáveis de uma criatura: atributos do sistema, `CA`, `PV` e `PV_MAX`. */
fun monsterVariables(monster: MonsterSheet, profile: SystemProfile): Map<String, Int> {
    val b = VariableBuilder()
    if (profile.usesArmorClass) b.put("CA", monster.armorClass)
    b.put("PV", monster.hp); b.put("PV_MAX", monster.hp)
    b.addAttributes(profile, monster.attributes)
    return b.map
}

// --- fórmulas padrão do sistema -------------------------------------------------------------

private val DICE_TERM = Regex("\\d*[dD]\\d+")

/** Dado-base dos testes do sistema ("1d20", "2d6"...), deduzido da fórmula de teste. Padrão: 1d20. */
fun baseDie(profile: SystemProfile?): String {
    val term = DICE_TERM.find(profile?.testFormula.orEmpty())?.value ?: return "1d20"
    return if (term.startsWith("d", ignoreCase = true)) "1$term" else term
}

fun defaultTestFormula(profile: SystemProfile): String {
    if (profile.testFormula.isNotBlank()) return profile.testFormula
    if (profile.isPool) return ""
    val first = profile.attributes.firstOrNull()?.let { attributeVariable(it) }
    return listOfNotNull(baseDie(profile), first, if (profile.usesProficiency) "PROF" else null).joinToString(" + ")
}

fun defaultInitiativeFormula(profile: SystemProfile): String {
    if (profile.initiativeFormula.isNotBlank()) return profile.initiativeFormula
    val base = if (profile.isPool) "1d${profile.poolSides}" else baseDie(profile)
    val dex = profile.attributes.firstOrNull { attributeVariable(it) == "DES" || attributeVariable(it) == "DEX" }
    return if (dex != null) "$base + ${attributeVariable(dex)}" else base
}

// --- formatação e rolagens ------------------------------------------------------------------

fun describeRoll(roll: EngineRoll): String =
    roll.breakdown + if (roll.naturalRolls.isEmpty()) "" else "  [${roll.naturalRolls.joinToString(", ")}]"

internal fun naturalTag(roll: EngineRoll): String = when (roll.firstD20) {
    20 -> " • 20 natural!"
    1 -> " • 1 natural"
    else -> ""
}

/** Rola uma fórmula com as variáveis dadas. Nunca lança: erros viram uma linha de texto. */
fun rollFormula(
    engine: RpgEngine, label: String, expression: String,
    variables: Map<String, Int>, difficulty: Int? = null
): RollLine = try {
    val roll = engine.roll(expression, variables)
    val verdict = difficulty?.let { if (roll.total >= it) " • sucesso (CD $it)" else " • falha (CD $it)" } ?: ""
    RollLine("$label: ${describeRoll(roll)}${naturalTag(roll)}$verdict", difficulty?.let { roll.total >= it })
} catch (e: RuntimeException) {
    RollLine("$label: erro — ${e.message}", null)
}

fun rollPoolLine(engine: RpgEngine, label: String, dice: Int, profile: SystemProfile, difficulty: Int = 1): RollLine = try {
    val pool = engine.rollPool(dice.coerceAtLeast(0), profile.poolSides, profile.poolTarget)
    val ok = pool.successes >= difficulty
    val tens = if (pool.maxFaces > 0) " • ${pool.maxFaces}× ${pool.sides}" else ""
    RollLine(
        "$label: ${pool.successes} sucesso(s) em ${pool.rolls.size} dado(s) [${pool.rolls.joinToString(", ")}]$tens" +
            " • ${if (ok) "passou" else "falhou"} (dif. $difficulty)", ok
    )
} catch (e: RuntimeException) {
    RollLine("$label: erro — ${e.message}", null)
}

internal fun biasedExpression(base: String, bonus: Int): String =
    if (bonus >= 0) "$base + $bonus" else "$base - ${-bonus}"

/** Teste de atributo: reserva de dados nos sistemas que contam sucessos, dado-base + bônus nos demais. */
fun rollAttributeCheck(
    engine: RpgEngine, profile: SystemProfile, attributes: Map<String, Int>,
    attr: SheetAttr, difficulty: Int? = null
): RollLine {
    val value = attributeValue(attributes, attr)
    if (profile.isPool) return rollPoolLine(engine, attr.label, value, profile, difficulty ?: 1)
    return rollFormula(engine, attr.label, biasedExpression(baseDie(profile), attributeBonus(attr, value)), emptyMap(), difficulty)
}

fun rollSkillCheck(engine: RpgEngine, sheet: CharacterSheet, profile: SystemProfile, skill: SkillEntry, difficulty: Int? = null): RollLine {
    if (profile.isPool) return rollPoolLine(engine, skill.name, skillPoolSize(profile, sheet.attributes, skill), profile, difficulty ?: 1)
    val bonus = skillBonus(profile, sheet.attributes, sheet.proficiencyBonus, skill)
    return rollFormula(engine, skill.name, biasedExpression(baseDie(profile), bonus), emptyMap(), difficulty)
}

/** Bônus de salvaguarda: modificador do atributo + proficiência, se a ficha for proficiente nela. */
fun saveBonus(sheet: CharacterSheet, attr: SheetAttr): Int =
    attributeBonus(attr, attributeValue(sheet.attributes, attr)) +
        (if (attr.hasModifier && attr.key in sheet.saveProficiencies) sheet.proficiencyBonus else 0)

fun rollSaveCheck(engine: RpgEngine, sheet: CharacterSheet, profile: SystemProfile, attr: SheetAttr, difficulty: Int? = null): RollLine =
    rollFormula(engine, "Salvaguarda de ${attr.label}", biasedExpression(baseDie(profile), saveBonus(sheet, attr)), emptyMap(), difficulty)

data class InitiativeResult(val roll: EngineRoll, val note: String = "")

/** Rola a iniciativa do sistema. Se a fórmula do sistema falhar, usa só o dado-base e avisa em [InitiativeResult.note]. */
fun rollInitiative(engine: RpgEngine, profile: SystemProfile, variables: Map<String, Int>): InitiativeResult {
    val formula = defaultInitiativeFormula(profile)
    return try {
        InitiativeResult(engine.roll(formula, variables))
    } catch (e: RuntimeException) {
        val fallback = if (profile.isPool) "1d${profile.poolSides}" else baseDie(profile)
        InitiativeResult(engine.roll(fallback), "Fórmula de iniciativa inválida (${e.message}); usei $fallback.")
    }
}

// --- separar "fórmula" de "texto" -----------------------------------------------------------

private val DICE_AT = Regex("\\d*[dD]\\d+")
private val NUMBER_AT = Regex("\\d+")
private val WORD_AT = Regex("[\\p{L}_][\\p{L}\\p{N}_]*")

/**
 * Separa a parte calculável do começo de um texto como "1d4+2 perfurante" ou "2d8 + INT necrótico".
 * Devolve (fórmula, resto). Palavras só entram na fórmula se forem variáveis conhecidas.
 */
fun splitFormula(text: String, known: Set<String>): Pair<String, String> {
    val s = text.trim()
    var pos = 0
    var end = 0
    var needOperand = true
    var depth = 0
    while (pos < s.length) {
        while (pos < s.length && s[pos].isWhitespace()) pos++
        if (pos >= s.length) break
        val c = s[pos]
        if (needOperand) {
            if (c == '(') { depth++; pos++; continue }
            if (c == '+' || c == '-' || c == '−') { pos++; continue }
            val match = DICE_AT.matchAt(s, pos) ?: NUMBER_AT.matchAt(s, pos)
                ?: WORD_AT.matchAt(s, pos)?.takeIf { variableName(it.value) in known }
                ?: break
            pos = match.range.last + 1
            end = pos
            needOperand = false
        } else {
            if (c == '+' || c == '-' || c == '−' || c == '*' || c == '/' || c == '×' || c == '÷') {
                pos++; needOperand = true
            } else if (c == ')' && depth > 0) {
                depth--; pos++; end = pos
            } else break
        }
    }
    if (end == 0) return "" to s
    var expression = s.substring(0, end).trim()
    val open = expression.count { it == '(' } - expression.count { it == ')' }
    if (open > 0) expression += ")".repeat(open)
    val rest = s.substring(end).trim().trim(',', ';', '.', ' ', '+', '-', '−')
    return expression to rest
}

// --- ataques --------------------------------------------------------------------------------

/** Um ataque rolável: [bonus] e [damage] são os textos como o usuário os escreveu. */
data class AttackOption(
    val label: String, val bonus: String, val damage: String,
    /** Ação sem rolagem de acerto (sopros, áreas): só o dano é rolado. */
    val autoHit: Boolean = false
)

/** "+4" vira "1d20 + +4"; vazio vira só o dado-base; um texto que já traz dados é usado como está. */
fun attackFormula(bonus: String, profile: SystemProfile?): String {
    val b = bonus.trim()
    val base = baseDie(profile)
    if (b.isEmpty()) return base
    if (DICE_TERM.containsMatchIn(b)) return b
    return "$base + $b"
}

fun characterAttacks(sheet: CharacterSheet, items: List<ItemEntry>): List<AttackOption> {
    val own = sheet.attacks
        .filter { it.name.isNotBlank() || it.damage.isNotBlank() }
        .map { AttackOption(it.name.ifBlank { "Ataque" }, it.bonus, it.damage) }
    val equipped = sheet.inventory.filter { it.equipped }.mapNotNull { entry ->
        val item = items.firstOrNull { it.id == entry.itemId && it.fitsSystem(sheet.systemId) }
        if (item != null && item.damage.isNotBlank()) AttackOption("${item.name.ifBlank { "Item" }} (equipado)", item.bonus, item.damage) else null
    }
    return own + equipped
}

data class ParsedAction(val attackBonus: Int?, val damage: String, val damageType: String)

private val ATTACK_BONUS = Regex("([+\\-−]\\s?\\d+)\\s*(?:para\\s+(?:acertar|atingir)|to\\s+hit|de\\s+ataque)", RegexOption.IGNORE_CASE)
private val ATTACK_BONUS_LOOSE = Regex("Ataque[^:.]*:\\s*([+\\-−]\\s?\\d+)", RegexOption.IGNORE_CASE)
private val DAMAGE_TEXT = Regex("Dano:?\\s*(?:\\d+\\s*\\(\\s*)?(\\d*[dD]\\d+(?:\\s*[+\\-−]\\s*\\d+)?)\\s*\\)?\\s*(\\p{L}*)", RegexOption.IGNORE_CASE)

/**
 * Lê o texto de uma ação de criatura, no estilo "Ataque corpo a corpo: +4 para acertar. Dano: 2d4+2 perfurante."
 * Devolve null quando não há nem bônus de ataque nem dano reconhecível.
 */
fun parseMonsterAction(text: String): ParsedAction? {
    val bonusText = (ATTACK_BONUS.find(text) ?: ATTACK_BONUS_LOOSE.find(text))?.groupValues?.get(1)
    val bonus = bonusText?.replace("−", "-")?.replace(" ", "")?.toIntOrNull()
    val dmg = DAMAGE_TEXT.find(text)
    val expression = dmg?.groupValues?.get(1)?.trim().orEmpty()
    if (bonus == null && expression.isEmpty()) return null
    return ParsedAction(bonus, expression, dmg?.groupValues?.get(2).orEmpty())
}

fun monsterAttacks(monster: MonsterSheet): List<AttackOption> =
    monster.entries
        .filter { it.kind == EntryKind.ACTION || it.kind == EntryKind.LEGENDARY }
        .mapNotNull { entry ->
            val parsed = parseMonsterAction(entry.text) ?: return@mapNotNull null
            AttackOption(
                entry.name.ifBlank { "Ação" },
                parsed.attackBonus?.let { sgn(it) } ?: "",
                listOf(parsed.damage, parsed.damageType).filter { it.isNotBlank() }.joinToString(" "),
                autoHit = parsed.attackBonus == null
            )
        }

/**
 * Resultado de um ataque. Além do texto do histórico, traz os números que a tela de batalha anima:
 * o d20 que valeu ([d20]) e o descartado ([otherD20], só com vantagem/desvantagem), os dados de dano, etc.
 */
data class AttackOutcome(
    val line: RollLine, val damage: Int?, val hit: Boolean?, val damageType: String = "",
    val attackTotal: Int? = null,
    val d20: Int? = null,
    val otherD20: Int? = null,
    val mode: RollMode = RollMode.NORMAL,
    val critical: Boolean = false,
    val fumble: Boolean = false,
    val damageDice: List<Int> = emptyList(),
    /** Lados do primeiro dado do dano (para a animação), ou null se o dano não tem dados. */
    val damageSides: Int? = null,
    val defense: Int? = null
)

private val FIRST_DIE = Regex("\\d*[dD](\\d+)")

private fun firstDieSides(expression: String): Int? = FIRST_DIE.find(expression)?.groupValues?.get(1)?.toIntOrNull()

/**
 * Rola um ataque com as variáveis do atacante. Com [defense], decide acerto (20 natural acerta e dobra os dados
 * do dano; 1 natural erra) e só rola dano se acertou. Sem defesa, rola ataque e dano e deixa o veredito ao mestre.
 *
 * [mode] aplica vantagem/desvantagem (rola a fórmula duas vezes e fica com o melhor/pior total; só vale quando a
 * fórmula tem um d20). [forceCrit] torna crítico todo acerto (alvo paralisado ou inconsciente, corpo a corpo).
 */
fun resolveAttack(
    engine: RpgEngine, name: String, bonus: String, damage: String,
    variables: Map<String, Int>, profile: SystemProfile?, defense: Int?,
    mode: RollMode = RollMode.NORMAL, forceCrit: Boolean = false
): AttackOutcome = try {
    val formula = attackFormula(bonus, profile)
    val first = engine.roll(formula, variables)
    var attack = first
    var discarded: EngineRoll? = null
    if (mode != RollMode.NORMAL && first.firstD20 != null) {
        val second = engine.roll(formula, variables)
        val takeSecond = if (mode == RollMode.ADVANTAGE) second.total > first.total else second.total < first.total
        attack = if (takeSecond) second else first
        discarded = if (takeSecond) first else second
    }
    val naturalTwenty = attack.firstD20 == 20
    val fumble = attack.firstD20 == 1
    val hit = defense?.let { naturalTwenty || (!fumble && attack.total >= it) }
    val critical = naturalTwenty || (forceCrit && hit == true)
    val (expression, type) = splitFormula(damage, variables.keys)
    val damageRoll = if (expression.isNotBlank() && hit != false) engine.roll(expression, variables, doubleDice = critical) else null
    val verdict = when {
        naturalTwenty -> " • CRÍTICO"
        critical -> " • CRÍTICO (alvo indefeso)"
        fumble -> " • falha crítica"
        hit == true -> " • acerta (defesa $defense)"
        hit == false -> " • erra (defesa $defense)"
        else -> ""
    }
    val modeText = if (discarded == null) "" else
        " • ${if (mode == RollMode.ADVANTAGE) "vantagem" else "desvantagem"} (d20: ${attack.firstD20} e ${discarded.firstD20})"
    val damageText = damageRoll?.let { " • dano ${it.total}${if (type.isBlank()) "" else " $type"} [${it.naturalRolls.joinToString(", ")}]" } ?: ""
    val dice = if (attack.naturalRolls.isEmpty()) "" else " [${attack.naturalRolls.joinToString(", ")}]"
    AttackOutcome(
        line = RollLine("$name: ataque ${attack.total}$dice$modeText$verdict$damageText", hit ?: if (critical) true else null),
        damage = damageRoll?.total, hit = hit, damageType = type,
        attackTotal = attack.total, d20 = attack.firstD20, otherD20 = discarded?.firstD20,
        mode = if (discarded == null) RollMode.NORMAL else mode,
        critical = critical, fumble = fumble,
        damageDice = damageRoll?.naturalRolls ?: emptyList(), damageSides = firstDieSides(expression),
        defense = defense
    )
} catch (e: RuntimeException) {
    AttackOutcome(RollLine("$name: erro — ${e.message}", null), null, null)
}

// --- magias ---------------------------------------------------------------------------------

/**
 * Interpreta o custo de uma magia ("3 Mana", "2 pontos de sangue") como um gasto de recurso da ficha.
 * Devolve null quando o texto não aponta para um recurso do sistema (o custo fica só informativo).
 */
fun parseResourceCost(cost: String, profile: SystemProfile): Pair<SheetResource, Int>? {
    val match = Regex("(\\d+)\\s*(?:x\\s*)?(.*)", RegexOption.IGNORE_CASE).find(cost.trim()) ?: return null
    val amount = match.groupValues[1].toIntOrNull() ?: return null
    val wanted = variableName(match.groupValues[2])
    if (wanted.length < 2 || amount <= 0) return null
    val resource = profile.resources.firstOrNull { r ->
        val names = listOf(variableName(r.label), variableName(shortResourceLabel(r))).filter { it.length >= 2 }
        names.any { it == wanted || wanted.contains(it) || it.contains(wanted) }
    } ?: return null
    return resource to amount
}

private fun effectOf(engine: RpgEngine, spell: SpellEntry, variables: Map<String, Int>): Pair<RollLine, Boolean> {
    val (expression, type) = splitFormula(spell.damage, variables.keys)
    if (expression.isBlank()) {
        return RollLine("${spell.name}: o campo Dano/efeito não tem fórmula rolável (ex.: 2d8 + INT).", null) to false
    }
    return try {
        val roll = engine.roll(expression, variables)
        RollLine("${spell.name}: ${describeRoll(roll)}${if (type.isBlank()) "" else " $type"}", true) to true
    } catch (e: RuntimeException) {
        RollLine("${spell.name}: erro — ${e.message}", null) to false
    }
}

/** Rola só o efeito da magia (usado no Grimório), sem gastar recursos. */
fun rollSpellEffect(engine: RpgEngine, spell: SpellEntry, variables: Map<String, Int>): RollLine =
    effectOf(engine, spell, variables).first

data class CastOutcome(val sheet: CharacterSheet, val line: RollLine, val cast: Boolean)

/**
 * Conjura a magia pela ficha: confere e debita o custo (quando aponta para um recurso), rola o efeito com as
 * variáveis da ficha e devolve a ficha atualizada. Se a fórmula for inválida ou faltar recurso, nada é gasto.
 */
fun castSpell(engine: RpgEngine, spell: SpellEntry, sheet: CharacterSheet, profile: SystemProfile): CastOutcome {
    val cost = parseResourceCost(spell.cost, profile)
    if (cost != null) {
        val have = sheet.resources[cost.first.key]?.current ?: cost.first.defaultMax
        if (have < cost.second) {
            return CastOutcome(sheet, RollLine("${spell.name}: ${cost.first.label} insuficiente (custa ${cost.second}, restam $have).", false), false)
        }
    }
    val variables = characterVariables(sheet, profile)
    val effect: Pair<RollLine, Boolean> =
        if (spell.damage.isBlank()) RollLine("${spell.name}: conjurada.", true) to true
        else effectOf(engine, spell, variables)
    if (!effect.second) return CastOutcome(sheet, effect.first, false)

    var updated = sheet
    var note = ""
    if (cost != null) {
        val (resource, amount) = cost
        val current = sheet.resources[resource.key] ?: ResourceValue(resource.defaultMax, resource.defaultMax)
        updated = sheet.copy(resources = sheet.resources + (resource.key to current.copy(current = current.current - amount)))
        note = " • gasta $amount ${resource.label}"
    } else if (spell.cost.isNotBlank()) {
        note = " • custo \"${spell.cost}\" não debitado (só informativo)"
    }
    return CastOutcome(updated, RollLine(effect.first.text + note, true), true)
}

// --- combate --------------------------------------------------------------------------------

/**
 * Aplica dano (valor positivo) ou cura (negativo), sem passar de 0 nem do máximo.
 * - Dano gasta primeiro os PV temporários.
 * - Personagem já a 0 PV que leva dano ganha uma falha na salvaguarda contra a morte (3 falham = morto).
 * - Voltar a ter PV zera as salvaguardas contra a morte.
 */
fun Combatant.withDamage(amount: Int): Combatant {
    if (amount <= 0) {
        val hp = (currentHp - amount).coerceIn(0, maxHp)
        return if (hp > 0) copy(currentHp = hp, deathSuccesses = 0, deathFailures = 0) else copy(currentHp = hp)
    }
    val absorbed = minOf(tempHp, amount)
    val rest = amount - absorbed
    val shielded = copy(tempHp = tempHp - absorbed)
    if (rest == 0) return shielded
    if (currentHp <= 0) {
        if (sheetKind != CombatantSource.CHARACTER) return shielded
        return shielded.copy(deathFailures = (deathFailures + 1).coerceAtMost(3))
    }
    return shielded.copy(currentHp = (currentHp - rest).coerceAtLeast(0))
}

fun monsterHitPoints(engine: RpgEngine, monster: MonsterSheet): Int {
    if (monster.hpFormula.isBlank()) return monster.hp.coerceAtLeast(1)
    return try { engine.roll(monster.hpFormula).total.coerceAtLeast(1) } catch (_: RuntimeException) { monster.hp.coerceAtLeast(1) }
}

fun monsterCombatant(engine: RpgEngine, monster: MonsterSheet, profile: SystemProfile, number: Int? = null): Combatant {
    val hp = monsterHitPoints(engine, monster)
    val initiative = rollInitiative(engine, profile, monsterVariables(monster, profile)).roll.total
    return Combatant(
        id = UUID.randomUUID().toString(),
        name = monster.name.ifBlank { "Criatura" } + (if (number != null) " $number" else ""),
        initiative = initiative, currentHp = hp, maxHp = hp,
        armorClass = monster.armorClass, sheetId = monster.id, sheetKind = CombatantSource.MONSTER,
        defenses = monster.defenses, conditionImmunities = monster.conditionImmunities
    )
}

fun characterCombatant(engine: RpgEngine, sheet: CharacterSheet, profile: SystemProfile, items: List<ItemEntry> = emptyList()): Combatant {
    val resource = primaryResource(profile)
    val value = resource?.let { sheet.resources[it.key] ?: ResourceValue(it.defaultMax, it.defaultMax) }
    val max = (value?.max ?: 10).coerceAtLeast(1)
    val current = (value?.current ?: max).coerceIn(0, max)
    val initiative = rollInitiative(engine, profile, characterVariables(sheet, profile, items)).roll.total
    return Combatant(
        id = UUID.randomUUID().toString(), name = sheet.name.ifBlank { "Personagem" },
        initiative = initiative, currentHp = current, maxHp = max,
        armorClass = if (profile.usesArmorClass) effectiveArmorClass(sheet, items) else 10,
        sheetId = sheet.id, sheetKind = CombatantSource.CHARACTER,
        conditions = sheet.conditions, defenses = sheet.defenses, conditionImmunities = sheet.conditionImmunities,
        tempHp = sheet.tempHp, deathSuccesses = sheet.deathSuccesses, deathFailures = sheet.deathFailures
    )
}

/** Rola só o dano de uma ação (sem teste de acerto), com as variáveis dadas. */
fun rollDamageOnly(engine: RpgEngine, name: String, damage: String, variables: Map<String, Int>): AttackOutcome {
    val (expression, type) = splitFormula(damage, variables.keys)
    if (expression.isBlank()) return AttackOutcome(RollLine("$name: sem fórmula de dano rolável.", null), null, null)
    return try {
        val roll = engine.roll(expression, variables)
        AttackOutcome(
            RollLine("$name: dano ${describeRoll(roll)}${if (type.isBlank()) "" else " $type"}"), roll.total, null, type,
            damageDice = roll.naturalRolls, damageSides = firstDieSides(expression)
        )
    } catch (e: RuntimeException) {
        AttackOutcome(RollLine("$name: erro — ${e.message}", null), null, null)
    }
}

/** Rola uma opção de ataque: acerto + dano, ou só o dano quando a ação não tem teste de acerto. */
fun rollOption(
    engine: RpgEngine, option: AttackOption, variables: Map<String, Int>, profile: SystemProfile?, defense: Int?,
    mode: RollMode = RollMode.NORMAL, forceCrit: Boolean = false
): AttackOutcome =
    if (option.autoHit) rollDamageOnly(engine, option.label, option.damage, variables)
    else resolveAttack(engine, option.label, option.bonus, option.damage, variables, profile, defense, mode, forceCrit)

/**
 * Rola uma ação de criatura a partir do texto dela ("+4 para acertar. Dano: 2d4+2 perfurante").
 * Sem bônus de ataque no texto, rola apenas o dano.
 */
fun rollMonsterAction(
    engine: RpgEngine, entry: com.example.rpgsombrio.data.NamedEntry,
    variables: Map<String, Int>, profile: SystemProfile?, defense: Int?
): AttackOutcome {
    val name = entry.name.ifBlank { "Ação" }
    val parsed = parseMonsterAction(entry.text)
        ?: return AttackOutcome(RollLine("$name: não achei bônus de ataque nem dano no texto (ex.: \"+4 para acertar. Dano: 2d4+2 perfurante\").", null), null, null)
    val option = AttackOption(
        name, parsed.attackBonus?.let { sgn(it) } ?: "",
        listOf(parsed.damage, parsed.damageType).filter { it.isNotBlank() }.joinToString(" "),
        autoHit = parsed.attackBonus == null
    )
    return rollOption(engine, option, variables, profile, defense)
}

/**
 * Lê uma lista manual de variáveis no formato "FOR=3, DES=2; PROF=2" (separadores: vírgula, ponto e vírgula
 * ou quebra de linha). Entradas inválidas são ignoradas.
 */
fun parseVariableList(text: String): Map<String, Int> {
    val result = LinkedHashMap<String, Int>()
    text.split(',', ';', '\n').forEach { part ->
        val name = variableName(part.substringBefore('='))
        val value = part.substringAfter('=', "").trim().replace("−", "-").toIntOrNull()
        if (part.contains('=') && name.isNotEmpty() && value != null) result[name] = value
    }
    return result
}
