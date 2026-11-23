package com.example.rpgsombrio.data

import android.content.SharedPreferences
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------------------------------------
// Modelos estruturados de ficha, criatura e magia. Tudo é guardado em SharedPreferences (JSON).
// ---------------------------------------------------------------------------------------------

data class ResourceValue(val current: Int = 0, val max: Int = 0)

data class SkillEntry(
    val name: String,
    val attributeKey: String = "",
    val proficient: Boolean = false,
    val extra: Int = 0,
    /** Sistema a que a perícia pertence. Vazio (fichas antigas) = aparece em qualquer sistema. */
    val systemId: String = ""
)

/** Vale para o sistema [sid]? Registros sem sistema valem para todos. */
fun SkillEntry.fitsSystem(sid: String): Boolean = systemId.isBlank() || systemId == sid
fun ItemEntry.fitsSystem(sid: String): Boolean = systemId.isBlank() || systemId == sid
fun SpellEntry.fitsSystem(sid: String): Boolean = systemId.isBlank() || systemId == sid
fun TraitEntry.fitsSystem(sid: String): Boolean = systemId.isBlank() || systemId == sid

data class AttackEntry(val name: String = "", val bonus: String = "", val damage: String = "")

object EntryKind {
    const val TRAIT = "trait"
    const val ACTION = "action"
    const val REACTION = "reaction"
    const val LEGENDARY = "legendary"
}

data class NamedEntry(val kind: String, val name: String = "", val text: String = "")

/** Item da biblioteca. O peso é numérico para que o inventário da ficha possa somá-lo. */
data class ItemEntry(
    val id: String,
    val name: String = "",
    val type: String = "",
    val rarity: String = "",
    val weight: Double = 0.0,
    val value: String = "",
    val bonus: String = "",
    val damage: String = "",
    val properties: String = "",
    val attunement: Boolean = false,
    val description: String = "",
    /**
     * Efeito na CA quando equipado. Em itens do tipo [ARMOR_TYPE] é o valor base da armadura (substitui a CA base
     * da ficha; vale a maior); nos demais tipos é um bônus somado (escudo +2, anel +1). 0 = sem efeito.
     */
    val armorClass: Int = 0,
    /** Sistema a que o item pertence (cada sistema usa o item de um jeito). Vazio = vale para todos. */
    val systemId: String = ""
)

/** Linha do inventário de um personagem: referência a um [ItemEntry] da biblioteca. */
data class InventoryEntry(val itemId: String, val quantity: Int = 1, val equipped: Boolean = false)

const val ARMOR_TYPE = "Armadura"

val ITEM_TYPES = listOf("Arma", "Armadura", "Escudo", "Consumível", "Ferramenta", "Mágico", "Tesouro", "Outro")

fun formatWeight(weight: Double): String {
    if (weight % 1.0 == 0.0) return weight.toInt().toString()
    return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", weight)
}

fun inventoryWeight(inventory: List<InventoryEntry>, items: List<ItemEntry>): Double =
    inventory.sumOf { entry -> (items.firstOrNull { it.id == entry.itemId }?.weight ?: 0.0) * entry.quantity }

object TraitKind {
    const val ADVANTAGE = "advantage"
    const val DISADVANTAGE = "disadvantage"
}

/**
 * Vantagem ou desvantagem da biblioteca. [cost] é sempre um valor positivo (a magnitude):
 * vantagens gastam pontos e desvantagens devolvem pontos. [systemId] vazio = vale para todos os sistemas.
 */
data class TraitEntry(
    val id: String,
    val name: String = "",
    val kind: String = TraitKind.ADVANTAGE,
    val cost: Int = 0,
    val category: String = "",
    val systemId: String = "",
    val description: String = ""
) {
    val isAdvantage: Boolean get() = kind != TraitKind.DISADVANTAGE
}

/** Pontos líquidos gastos: o que as vantagens custam menos o que as desvantagens devolvem. */
fun traitPointsSpent(traits: List<TraitEntry>): Int =
    traits.sumOf { if (it.isAdvantage) it.cost else -it.cost }

/** Texto do custo: vantagens custam pontos; desvantagens devolvem pontos. */
fun traitCostLabel(trait: TraitEntry): String = when {
    trait.cost == 0 -> "Sem custo"
    trait.isAdvantage -> "Custa ${trait.cost} pts"
    else -> "Devolve ${trait.cost} pts"
}

/** Tipos de defesa contra dano. */
object DefenseKind {
    const val RESISTANCE = "resist"
    const val VULNERABILITY = "vuln"
    const val IMMUNITY = "immune"
    val ALL = listOf(RESISTANCE, VULNERABILITY, IMMUNITY)

    fun label(kind: String): String = when (kind) {
        RESISTANCE -> "Resistências"
        VULNERABILITY -> "Vulnerabilidades"
        IMMUNITY -> "Imunidades a dano"
        else -> kind
    }
}

/** Sugestões de tipos de dano (o campo aceita qualquer texto, para sistemas com outros tipos). */
val DAMAGE_TYPE_SUGGESTIONS = listOf(
    "Ácido", "Contundente", "Cortante", "Elétrico", "Energético", "Frio", "Fogo",
    "Necrótico", "Perfurante", "Psíquico", "Radiante", "Trovejante", "Venenoso",
    "Não mágico", "Prata"
)

/** Condições que a ficha do personagem pode marcar como ativas. */
val CONDITION_SUGGESTIONS = listOf(
    "Agarrado", "Amedrontado", "Atordoado", "Caído", "Cego", "Enfeitiçado", "Envenenado", "Exausto",
    "Imobilizado", "Incapacitado", "Inconsciente", "Invisível", "Paralisado", "Petrificado", "Surdo"
)

/** Uma defesa contra um tipo de dano: [kind] é um [DefenseKind]; [type] é o tipo de dano (ex.: "Fogo"). */
data class DamageDefense(val kind: String, val type: String)

/** Multiplicador de dano: imunidade zera; resistência e vulnerabilidade se anulam quando juntas (como no D&D 5e). */
private fun normalizeDamageType(raw: String): String =
    raw.trim().lowercase().trimEnd('.', ',', ';').removePrefix("de ").removePrefix("do ").removePrefix("da ").trim()

fun damageMultiplierFor(defenses: List<DamageDefense>, damageType: String): Double {
    val wanted = normalizeDamageType(damageType)
    if (wanted.isEmpty()) return 1.0
    val mine = defenses.filter { normalizeDamageType(it.type) == wanted }
    if (mine.any { it.kind == DefenseKind.IMMUNITY }) return 0.0
    val resist = mine.any { it.kind == DefenseKind.RESISTANCE }
    val vuln = mine.any { it.kind == DefenseKind.VULNERABILITY }
    return when {
        resist && !vuln -> 0.5
        vuln && !resist -> 2.0
        else -> 1.0
    }
}

/** Dano final depois das defesas do alvo (resistência arredonda para baixo). */
fun adjustedDamage(amount: Int, damageType: String, defenses: List<DamageDefense>): Int {
    val multiplier = damageMultiplierFor(defenses, damageType)
    return if (multiplier == 1.0) amount else (amount * multiplier).toInt().coerceAtLeast(0)
}

/** O texto de imunidades a condições (separado por vírgula, ponto e vírgula, quebra de linha ou " e ") cita a condição? */
fun immuneToCondition(immunities: String, condition: String): Boolean =
    immunities.split(Regex("[,;\\n]|\\s+e\\s+")).any { it.trim().equals(condition, ignoreCase = true) }

/** Texto curto das defesas, ex.: "Resiste: Fogo, Frio • Vulnerável: Radiante". */
fun defensesSummary(defenses: List<DamageDefense>): String {
    fun part(prefix: String, kind: String): String? =
        defenses.filter { it.kind == kind && it.type.isNotBlank() }.map { it.type }
            .takeIf { it.isNotEmpty() }?.joinToString(", ", prefix = "$prefix: ")
    return listOfNotNull(
        part("Resiste", DefenseKind.RESISTANCE),
        part("Vulnerável", DefenseKind.VULNERABILITY),
        part("Imune", DefenseKind.IMMUNITY)
    ).joinToString(" • ")
}

/** Aparência do personagem: avatar em pixel art (índices das opções) + descrição estruturada. */
data class CharacterLook(
    val configured: Boolean = false,
    val race: Int = 0,
    val skin: Int = 1,
    val face: Int = 0,
    val body: Int = 0,
    val hairStyle: Int = 1,
    val hairColor: Int = 1,
    val eyeColor: Int = 1,
    val eyeStyle: Int = 0,
    val mouth: Int = 0,
    val beard: Int = 0,
    val accessory: Int = 0,
    val outfit: Int = 0,
    val outfitColor: Int = 1,
    val cape: Int = 0,
    val headgear: Int = 0,
    val weapon: Int = 0,
    val age: String = "",
    val height: String = "",
    val build: String = "",
    val marks: String = ""
)

data class CharacterSheet(
    val id: String,
    val name: String = "",
    val systemId: String = "dnd5e",
    val role: String = "jogador"
    val concept: String = "",
    val origin: String = "",
    val level: Int = 1,
    val imageUri: String = "",
    val armorClass: Int = 10,
    val proficiencyBonus: Int = 2,
    val attributes: Map<String, Int> = emptyMap(),
    val resources: Map<String, ResourceValue> = emptyMap(),
    val skills: List<SkillEntry> = emptyList(),
    val attacks: List<AttackEntry> = emptyList(),
    val spellIds: List<String> = emptyList(),
    val inventory: List<InventoryEntry> = emptyList(),
    val traitIds: List<String> = emptyList(),
    val defenses: List<DamageDefense> = emptyList(),
    val conditionImmunities: String = "",
    /** Chaves dos atributos em que a ficha tem proficiência na salvaguarda. */
    val saveProficiencies: List<String> = emptyList(),
    /** Chaves dos atributos do sistema que esta ficha esconde (os valores continuam guardados e valendo nas fórmulas). */
    val hiddenAttributes: List<String> = emptyList(),
    val experience: Int = 0,
    val speed: String = "",
    val tempHp: Int = 0,
    val inspiration: Boolean = false,
    val deathSuccesses: Int = 0,
    val deathFailures: Int = 0,
    val conditions: List<String> = emptyList(),
    val languages: String = "",
    val proficiencies: String = "",
    val senses: String = "",
    val alignment: String = "",
    val background: String = "",
    val appearance: String = "",
    val look: CharacterLook = CharacterLook(),
    val personality: String = "",
    val ideals: String = "",
    val bonds: String = "",
    val flaws: String = "",
    val money: String = "",
    val equipment: String = "",
    val notes: String = ""
)

data class MonsterSheet(
    val id: String,
    val name: String = "",
    val systemId: String = "dnd5e",
    val type: String = "",
    val size: String = "",
    val challenge: String = "",
    val imageUri: String = "",
    val armorClass: Int = 10,
    val hp: Int = 10,
    val hpFormula: String = "",
    val speed: String = "",
    val attributes: Map<String, Int> = emptyMap(),
    /** Texto livre das versões antigas (resistências/imunidades/vulnerabilidades juntas). Continua visível como "Outras defesas". */
    val resistances: String = "",
    val defenses: List<DamageDefense> = emptyList(),
    val conditionImmunities: String = "",
    val alignment: String = "",
    val savingThrows: String = "",
    val skillsText: String = "",
    val loot: String = "",
    val senses: String = "",
    val languages: String = "",
    val entries: List<NamedEntry> = emptyList(),
    val notes: String = ""
)

data class SpellEntry(
    val id: String,
    val name: String = "",
    val level: Int = 0,
    val school: String = "",
    val classes: String = "",
    val castingTime: String = "",
    val range: String = "",
    val components: String = "",
    val duration: String = "",
    val concentration: Boolean = false,
    val ritual: Boolean = false,
    val damage: String = "",
    val cost: String = "",
    val description: String = "",
    val higherLevels: String = "",
    /** Sistema a que a magia pertence. Vazio = vale para todos os sistemas. */
    val systemId: String = ""
)

fun spellLevelLabel(level: Int): String = if (level <= 0) "Truque" else "Nível $level"

// ---------------------------------------------------------------------------------------------
// Perfil do sistema: o que a ficha mostra depende do sistema da campanha.
// ---------------------------------------------------------------------------------------------

data class SheetAttr(
    val key: String,
    val label: String,
    val abbr: String,
    val default: Int,
    val hasModifier: Boolean,
    val min: Int? = null,
    val max: Int? = null,
    val modBase: Int = 10,
    val modDivisor: Int = 2
) {
    /** Modificador configurável por sistema: piso((valor − base) / divisor). */
    fun modifierOf(value: Int): Int = Math.floorDiv(value - modBase, modDivisor.coerceAtLeast(1))

    /** Limita o valor digitado. O mínimo só é aplicado quando é 0 ou 1, para não "pular" durante a digitação. */
    fun clamp(value: Int): Int {
        var v = value
        max?.let { v = v.coerceAtMost(it) }
        min?.let { if (it <= 1) v = v.coerceAtLeast(it) }
        return v
    }
}

const val ROLL_SUM = "sum"
const val ROLL_POOL = "pool"

data class SheetResource(val key: String, val label: String, val defaultMax: Int)

data class SkillTemplate(val name: String, val attributeKey: String)

data class SystemProfile(
    val id: String,
    val name: String,
    val attributes: List<SheetAttr>,
    val resources: List<SheetResource>,
    val skillTemplates: List<SkillTemplate>,
    val usesLevels: Boolean,
    val usesArmorClass: Boolean,
    val usesSkills: Boolean,
    val autoProficiency: Boolean,
    val conceptLabel: String,
    val originLabel: String,
    val usesAttacks: Boolean = true,
    val usesSpells: Boolean = true,
    val usesInventory: Boolean = true,
    val usesTraits: Boolean = true,
    val traitBudget: Int = 0,
    /** Fórmula padrão de teste do sistema (ex.: "1d20 + FOR + PROF"). Vazio = o Motor deduz uma. */
    val testFormula: String = "",
    /** Fórmula de iniciativa do sistema. Vazio = o Motor deduz uma a partir dos atributos. */
    val initiativeFormula: String = "",
    /** [ROLL_SUM] (soma de dados) ou [ROLL_POOL] (reserva de dados contando sucessos). */
    val rollStyle: String = ROLL_SUM,
    val poolSides: Int = 10,
    /** Em reservas de dados, cada dado com valor >= [poolTarget] é um sucesso. */
    val poolTarget: Int = 6,
    /** Dados oferecidos nos atalhos de rolagem do Motor. */
    val dice: List<Int> = ALL_DICE_SIDES
) {
    val isPool: Boolean get() = rollStyle == ROLL_POOL
    val usesProficiency: Boolean get() = attributes.any { it.hasModifier }
    fun attr(key: String): SheetAttr? = attributes.firstOrNull { it.key == key }
}

/** Ficha nova já preenchida com os padrões do sistema. Lógica pura (sem Android), para poder ser testada. */
fun buildNewCharacter(profile: SystemProfile, id: String, name: String): CharacterSheet = CharacterSheet(
    id = id, name = name, systemId = profile.id,
    attributes = profile.attributes.associate { it.key to it.default },
    resources = profile.resources.associate { it.key to ResourceValue(it.defaultMax, it.defaultMax) },
    skills = profile.skillTemplates.map { SkillEntry(it.name, it.attributeKey, systemId = profile.id) }
)

/** Atributos do sistema que a ficha mostra. Os escondidos pela ficha ficam de fora, mas seus valores continuam valendo nas fórmulas. */
fun SystemProfile.visibleAttributes(sheet: CharacterSheet): List<SheetAttr> =
    attributes.filter { it.key !in sheet.hiddenAttributes }

/** Aba em que a ficha abre: o Perfil para ficha recém-criada ou sem nome; senão a Ficha. */
fun initialSheetTab(startOnProfile: Boolean, name: String): String =
    if (startOnProfile || name.isBlank()) "Perfil" else "Ficha"

fun shortResourceLabel(resource: SheetResource): String = when (resource.key) {
    "hp" -> "PV"
    "health" -> "Vit."
    else -> resource.label.take(8)
}

object SystemProfiles {
    private val dndSkills = listOf(
        "Acrobacia" to "dexterity", "Arcanismo" to "intelligence", "Atletismo" to "strength",
        "Atuação" to "charisma", "Enganação" to "charisma", "Furtividade" to "dexterity",
        "História" to "intelligence", "Intimidação" to "charisma", "Intuição" to "wisdom",
        "Investigação" to "intelligence", "Lidar com Animais" to "wisdom", "Medicina" to "wisdom",
        "Natureza" to "intelligence", "Percepção" to "wisdom", "Persuasão" to "charisma",
        "Prestidigitação" to "dexterity", "Religião" to "intelligence", "Sobrevivência" to "wisdom"
    ).map { SkillTemplate(it.first, it.second) }

    private val vampireSkills = listOf(
        "Armas Brancas", "Armas de Fogo", "Atletismo", "Briga", "Condução", "Furtividade", "Ladroagem", "Ofícios",
        "Animais", "Etiqueta", "Intimidação", "Intuição", "Liderança", "Manha", "Performance", "Persuasão", "Sobrevivência",
        "Ciência", "Consciência", "Erudição", "Finanças", "Investigação", "Medicina", "Ocultismo", "Política", "Tecnologia"
    ).map { SkillTemplate(it, "") }

    fun resolve(prefs: SharedPreferences, systemId: String): SystemProfile {
        UserSystemStore.load(prefs).firstOrNull { it.id == systemId }?.let { return it.toProfile() }
        val def = RuleSystemCatalog.systems.firstOrNull { it.id == systemId } ?: RuleSystemCatalog.systems.first()
        return when (def.kind) {
            RuleSystemKind.CUSTOM, RuleSystemKind.GENERIC -> custom(prefs, def)
            RuleSystemKind.VAMPIRE -> SystemProfile(
                id = def.id, name = def.name,
                attributes = def.attributes.map { SheetAttr(it.key, it.label, it.abbreviation, 1, false) },
                resources = def.resources.map { SheetResource(it.key, it.label, it.defaultMaximum) },
                skillTemplates = vampireSkills,
                usesLevels = false, usesArmorClass = false, usesSkills = true, autoProficiency = false,
                conceptLabel = "Clã", originLabel = "Conceito",
                initiativeFormula = "1d10 + DES + RAC",
                rollStyle = ROLL_POOL, poolSides = 10, poolTarget = 6, dice = listOf(10)
            )
            else -> SystemProfile(
                id = def.id, name = def.name,
                attributes = def.attributes.map { SheetAttr(it.key, it.label, it.abbreviation, it.defaultValue, it.hasModifier) },
                resources = def.resources.map { SheetResource(it.key, it.label, it.defaultMaximum) },
                skillTemplates = dndSkills,
                usesLevels = true, usesArmorClass = true, usesSkills = true, autoProficiency = true,
                conceptLabel = "Classe", originLabel = "Raça / origem",
                testFormula = "1d20 + FOR + PROF", initiativeFormula = "1d20 + DES"
            )
        }
    }

    /** Sistema próprio / outro: usa o que foi montado no Construtor de sistemas. */
    private fun custom(prefs: SharedPreferences, def: RuleSystemDefinition): SystemProfile {
        val attrs = readStringList(prefs.getString("custom_attributes", null), listOf("FOR", "DES", "CON", "INT", "SAB", "CAR"))
        val resources = readStringList(prefs.getString("custom_resources", null), listOf("Pontos de vida", "Mana"))
        val skills = readStringList(prefs.getString("custom_skills", null), listOf("Atletismo", "Percepção"))
        return SystemProfile(
            id = def.id, name = def.name,
            attributes = attrs.map { SheetAttr(it, it, if (it.length <= 4) it else it.take(3).uppercase(), 10, false) },
            resources = resources.map { SheetResource(it, it, 10) },
            skillTemplates = skills.map { SkillTemplate(it, "") },
            usesLevels = true, usesArmorClass = true, usesSkills = true, autoProficiency = false,
            conceptLabel = "Classe / conceito", originLabel = "Origem"
        )
    }
}

private fun readStringList(raw: String?, fallback: List<String>): List<String> {
    if (raw.isNullOrBlank()) return fallback
    return try {
        val array = JSONArray(raw)
        List(array.length()) { array.getString(it) }
    } catch (_: Exception) { fallback }
}

// ---------------------------------------------------------------------------------------------
// JSON
// ---------------------------------------------------------------------------------------------

private fun JSONArray?.objects(): List<JSONObject> =
    if (this == null) emptyList() else List(length()) { getJSONObject(it) }

private fun JSONArray?.strings(): List<String> =
    if (this == null) emptyList() else List(length()) { getString(it) }

private fun intMapToJson(map: Map<String, Int>): JSONObject =
    JSONObject().also { o -> map.forEach { (k, v) -> o.put(k, v) } }

private fun jsonToIntMap(o: JSONObject?): Map<String, Int> {
    if (o == null) return emptyMap()
    val result = LinkedHashMap<String, Int>()
    o.keys().forEach { k -> result[k] = o.optInt(k) }
    return result
}

internal fun defensesToJson(list: List<DamageDefense>): JSONArray = JSONArray().also { a ->
    list.forEach { a.put(JSONObject().put("kind", it.kind).put("type", it.type)) }
}

internal fun jsonToDefenses(array: JSONArray?): List<DamageDefense> =
    array.objects().map { DamageDefense(it.optString("kind", DefenseKind.RESISTANCE), it.optString("type")) }

private fun CharacterLook.toJson(): JSONObject = JSONObject()
    .put("configured", configured).put("race", race).put("skin", skin).put("face", face).put("body", body)
    .put("hairStyle", hairStyle).put("hairColor", hairColor).put("eyeColor", eyeColor)
    .put("eyeStyle", eyeStyle).put("mouth", mouth).put("beard", beard).put("accessory", accessory)
    .put("outfit", outfit).put("outfitColor", outfitColor).put("cape", cape)
    .put("headgear", headgear).put("weapon", weapon)
    .put("age", age).put("height", height).put("build", build).put("marks", marks)

private fun decodeLook(o: JSONObject?): CharacterLook {
    if (o == null) return CharacterLook()
    val d = CharacterLook()
    return CharacterLook(
        configured = o.optBoolean("configured", false),
        race = o.optInt("race", d.race), skin = o.optInt("skin", d.skin),
        face = o.optInt("face", d.face), body = o.optInt("body", d.body),
        hairStyle = o.optInt("hairStyle", d.hairStyle), hairColor = o.optInt("hairColor", d.hairColor),
        eyeColor = o.optInt("eyeColor", d.eyeColor), eyeStyle = o.optInt("eyeStyle", d.eyeStyle),
        mouth = o.optInt("mouth", d.mouth), beard = o.optInt("beard", d.beard),
        accessory = o.optInt("accessory", d.accessory), outfit = o.optInt("outfit", d.outfit),
        outfitColor = o.optInt("outfitColor", d.outfitColor), cape = o.optInt("cape", d.cape),
        headgear = o.optInt("headgear", d.headgear), weapon = o.optInt("weapon", d.weapon),
        age = o.optString("age"), height = o.optString("height"),
        build = o.optString("build"), marks = o.optString("marks")
    )
}

private fun CharacterSheet.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("systemId", systemId)
    .put("concept", concept).put("origin", origin).put("level", level).put("imageUri", imageUri)
    .put("armorClass", armorClass).put("proficiencyBonus", proficiencyBonus)
    .put("attributes", intMapToJson(attributes))
    .put("resources", JSONObject().also { o ->
        resources.forEach { (k, v) -> o.put(k, JSONObject().put("current", v.current).put("max", v.max)) }
    })
    .put("skills", JSONArray().also { a ->
        skills.forEach { a.put(JSONObject().put("name", it.name).put("attr", it.attributeKey).put("prof", it.proficient).put("extra", it.extra).put("system", it.systemId)) }
    })
    .put("attacks", JSONArray().also { a ->
        attacks.forEach { a.put(JSONObject().put("name", it.name).put("bonus", it.bonus).put("damage", it.damage)) }
    })
    .put("spellIds", JSONArray(spellIds))
    .put("inventory", JSONArray().also { a ->
        inventory.forEach { a.put(JSONObject().put("itemId", it.itemId).put("qty", it.quantity).put("equipped", it.equipped)) }
    })
    .put("traitIds", JSONArray(traitIds))
    .put("defenses", defensesToJson(defenses)).put("conditionImmunities", conditionImmunities)
    .put("saveProficiencies", JSONArray(saveProficiencies)).put("experience", experience).put("speed", speed)
    .put("tempHp", tempHp).put("inspiration", inspiration)
    .put("deathSuccesses", deathSuccesses).put("deathFailures", deathFailures)
    .put("conditions", JSONArray(conditions))
    .put("hiddenAttributes", JSONArray(hiddenAttributes))
    .put("languages", languages).put("proficiencies", proficiencies).put("senses", senses)
    .put("alignment", alignment).put("background", background).put("appearance", appearance)
    .put("look", look.toJson())
    .put("personality", personality).put("ideals", ideals).put("bonds", bonds).put("flaws", flaws)
    .put("money", money)
    .put("equipment", equipment).put("notes", notes)

private fun decodeCharacter(o: JSONObject): CharacterSheet = CharacterSheet(
    id = o.getString("id"),
    name = o.optString("name"), systemId = o.optString("systemId", "dnd5e"),
    concept = o.optString("concept"), origin = o.optString("origin"),
    level = o.optInt("level", 1), imageUri = o.optString("imageUri"),
    armorClass = o.optInt("armorClass", 10), proficiencyBonus = o.optInt("proficiencyBonus", 2),
    attributes = jsonToIntMap(o.optJSONObject("attributes")),
    resources = o.optJSONObject("resources").let { r ->
        if (r == null) emptyMap() else r.keys().asSequence().associateWith { k ->
            val v = r.getJSONObject(k)
            ResourceValue(v.optInt("current"), v.optInt("max"))
        }
    },
    skills = o.optJSONArray("skills").objects().map {
        SkillEntry(it.optString("name"), it.optString("attr"), it.optBoolean("prof"), it.optInt("extra"), it.optString("system"))
    },
    attacks = o.optJSONArray("attacks").objects().map {
        AttackEntry(it.optString("name"), it.optString("bonus"), it.optString("damage"))
    },
    spellIds = o.optJSONArray("spellIds").strings(),
    inventory = o.optJSONArray("inventory").objects().map {
        InventoryEntry(it.optString("itemId"), it.optInt("qty", 1), it.optBoolean("equipped"))
    },
    traitIds = o.optJSONArray("traitIds").strings(),
    defenses = jsonToDefenses(o.optJSONArray("defenses")),
    conditionImmunities = o.optString("conditionImmunities"),
    saveProficiencies = o.optJSONArray("saveProficiencies").strings(),
    experience = o.optInt("experience"), speed = o.optString("speed"),
    tempHp = o.optInt("tempHp"), inspiration = o.optBoolean("inspiration"),
    deathSuccesses = o.optInt("deathSuccesses"), deathFailures = o.optInt("deathFailures"),
    conditions = o.optJSONArray("conditions").strings(),
    hiddenAttributes = o.optJSONArray("hiddenAttributes").strings(),
    languages = o.optString("languages"), proficiencies = o.optString("proficiencies"), senses = o.optString("senses"),
    alignment = o.optString("alignment"), background = o.optString("background"), appearance = o.optString("appearance"),
    look = decodeLook(o.optJSONObject("look")),
    personality = o.optString("personality"), ideals = o.optString("ideals"), bonds = o.optString("bonds"),
    flaws = o.optString("flaws"), money = o.optString("money"),
    equipment = o.optString("equipment"), notes = o.optString("notes")
)

private fun MonsterSheet.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("systemId", systemId)
    .put("type", type).put("size", size).put("challenge", challenge).put("imageUri", imageUri)
    .put("armorClass", armorClass).put("hp", hp).put("hpFormula", hpFormula).put("speed", speed)
    .put("attributes", intMapToJson(attributes))
    .put("resistances", resistances).put("senses", senses).put("languages", languages)
    .put("defenses", defensesToJson(defenses)).put("conditionImmunities", conditionImmunities)
    .put("alignment", alignment).put("savingThrows", savingThrows).put("skillsText", skillsText).put("loot", loot)
    .put("entries", JSONArray().also { a ->
        entries.forEach { a.put(JSONObject().put("kind", it.kind).put("name", it.name).put("text", it.text)) }
    })
    .put("notes", notes)

private fun decodeMonster(o: JSONObject): MonsterSheet = MonsterSheet(
    id = o.getString("id"), name = o.optString("name"), systemId = o.optString("systemId", "dnd5e"),
    type = o.optString("type"), size = o.optString("size"), challenge = o.optString("challenge"),
    imageUri = o.optString("imageUri"), armorClass = o.optInt("armorClass", 10), hp = o.optInt("hp", 10),
    hpFormula = o.optString("hpFormula"), speed = o.optString("speed"),
    attributes = jsonToIntMap(o.optJSONObject("attributes")),
    resistances = o.optString("resistances"), senses = o.optString("senses"), languages = o.optString("languages"),
    defenses = jsonToDefenses(o.optJSONArray("defenses")),
    conditionImmunities = o.optString("conditionImmunities"),
    alignment = o.optString("alignment"), savingThrows = o.optString("savingThrows"),
    skillsText = o.optString("skillsText"), loot = o.optString("loot"),
    entries = o.optJSONArray("entries").objects().map {
        NamedEntry(it.optString("kind", EntryKind.TRAIT), it.optString("name"), it.optString("text"))
    },
    notes = o.optString("notes")
)

private fun SpellEntry.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("level", level).put("school", school).put("classes", classes)
    .put("castingTime", castingTime).put("range", range).put("components", components).put("duration", duration)
    .put("concentration", concentration).put("ritual", ritual)
    .put("damage", damage).put("cost", cost).put("description", description).put("higherLevels", higherLevels)
    .put("systemId", systemId)

private fun decodeSpell(o: JSONObject): SpellEntry = SpellEntry(
    id = o.getString("id"), name = o.optString("name"), level = o.optInt("level"),
    school = o.optString("school"), classes = o.optString("classes"),
    castingTime = o.optString("castingTime"), range = o.optString("range"),
    components = o.optString("components"), duration = o.optString("duration"),
    concentration = o.optBoolean("concentration"), ritual = o.optBoolean("ritual"),
    damage = o.optString("damage"), cost = o.optString("cost"),
    description = o.optString("description"), higherLevels = o.optString("higherLevels"),
    systemId = o.optString("systemId")
)

private fun TraitEntry.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("kind", kind).put("cost", cost)
    .put("category", category).put("systemId", systemId).put("description", description)

private fun decodeTrait(o: JSONObject): TraitEntry = TraitEntry(
    id = o.getString("id"), name = o.optString("name"),
    kind = o.optString("kind", TraitKind.ADVANTAGE), cost = o.optInt("cost"),
    category = o.optString("category"), systemId = o.optString("systemId"),
    description = o.optString("description")
)

private fun ItemEntry.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("type", type).put("rarity", rarity).put("weight", weight)
    .put("value", value).put("bonus", bonus).put("damage", damage).put("properties", properties)
    .put("attunement", attunement).put("description", description).put("armorClass", armorClass)
    .put("systemId", systemId)

private fun decodeItem(o: JSONObject): ItemEntry = ItemEntry(
    id = o.getString("id"), name = o.optString("name"), type = o.optString("type"),
    rarity = o.optString("rarity"), weight = o.optDouble("weight", 0.0), value = o.optString("value"),
    bonus = o.optString("bonus"), damage = o.optString("damage"), properties = o.optString("properties"),
    attunement = o.optBoolean("attunement"), description = o.optString("description"),
    armorClass = o.optInt("armorClass", 0), systemId = o.optString("systemId")
)

// ---------------------------------------------------------------------------------------------
// Armazenamento
// ---------------------------------------------------------------------------------------------

object SheetStore {
    private const val CHARACTERS = "sheets_characters"
    private const val BESTIARY = "sheets_bestiary"
    private const val SPELLS = "sheets_spells"
    private const val ITEMS = "sheets_items"
    private const val TRAITS = "sheets_traits"

    fun newId(): String = UUID.randomUUID().toString()

    /**
     * Sistema da campanha. Prefere o id salvo (`active_campaign_system_id`); versões anteriores guardavam só o
     * nome exibido, que ainda é aceito como alternativa.
     */
    fun activeSystemId(prefs: SharedPreferences): String {
        val options = systemOptions(prefs)
        prefs.getString("active_campaign_system_id", null)?.let { id ->
            if (options.any { it.id == id }) return id
        }
        val name = prefs.getString("active_campaign_system", "D&D 5e")
        options.firstOrNull { it.name == name }?.let { return it.id }
        return RuleSystemCatalog.systems.firstOrNull { it.name == name }?.id ?: "dnd5e"
    }

    fun setActiveSystem(prefs: SharedPreferences, systemId: String) {
        prefs.edit()
            .putString("active_campaign_system_id", systemId)
            .putString("active_campaign_system", systemDisplayName(prefs, systemId))
            .apply()
    }

    /**
     * Troca o sistema de uma ficha sem apagar nada: valores de atributos e recursos de chaves que o novo
     * sistema não conhece ficam guardados (voltam se o sistema for reativado), e o que faltar recebe o padrão.
     */
    fun rebindCharacter(prefs: SharedPreferences, sheet: CharacterSheet, systemId: String): CharacterSheet {
        val p = SystemProfiles.resolve(prefs, systemId)
        // As perícias de cada sistema ficam guardadas na ficha: as do sistema antigo somem da vista (e voltam
        // se a ficha for vinculada de novo a ele); as do novo sistema entram.
        val tagged = sheet.skills.map { if (it.systemId.isBlank()) it.copy(systemId = sheet.systemId) else it }
        val knownSkills = tagged.filter { it.systemId == p.id }.map { it.name }.toSet()
        return sheet.copy(
            systemId = p.id,
            attributes = p.attributes.associate { it.key to it.default } + sheet.attributes,
            resources = p.resources.associate { it.key to ResourceValue(it.defaultMax, it.defaultMax) } + sheet.resources,
            skills = tagged + p.skillTemplates.filter { it.name !in knownSkills }.map { SkillEntry(it.name, it.attributeKey, systemId = p.id) }
        )
    }

    // --- criação de registros novos, já preenchidos conforme o sistema ---

    fun newCharacter(prefs: SharedPreferences, systemId: String? = null, name: String = "Novo personagem"): CharacterSheet {
        val p = SystemProfiles.resolve(prefs, systemId ?: activeSystemId(prefs))
        return buildNewCharacter(p, newId(), name)
    }

    fun newMonster(prefs: SharedPreferences, systemId: String? = null): MonsterSheet {
        val p = SystemProfiles.resolve(prefs, systemId ?: activeSystemId(prefs))
        return MonsterSheet(id = newId(), name = "Nova criatura", systemId = p.id,
            attributes = p.attributes.associate { it.key to it.default })
    }

    /** Vincula a criatura a outro sistema sem apagar valores (mesma regra de [rebindCharacter]). */
    fun rebindMonster(prefs: SharedPreferences, monster: MonsterSheet, systemId: String): MonsterSheet {
        val p = SystemProfiles.resolve(prefs, systemId)
        return monster.copy(systemId = p.id, attributes = p.attributes.associate { it.key to it.default } + monster.attributes)
    }

    fun newSpell(systemId: String = ""): SpellEntry = SpellEntry(id = newId(), name = "Nova magia", level = 1, systemId = systemId)

    fun newItem(systemId: String = ""): ItemEntry = ItemEntry(id = newId(), name = "Novo item", type = "Outro", systemId = systemId)

    fun newTrait(kind: String = TraitKind.ADVANTAGE): TraitEntry = TraitEntry(
        id = newId(), kind = kind,
        name = if (kind == TraitKind.DISADVANTAGE) "Nova desvantagem" else "Nova vantagem"
    )

    // --- leitura / escrita ---

    fun loadCharacters(prefs: SharedPreferences): List<CharacterSheet> =
        load(prefs, CHARACTERS, "records_characters", ::decodeCharacter,
            migrate = { o ->
                CharacterSheet(
                    id = o.optString("id").ifBlank { newId() }, name = o.optString("name"),
                    systemId = activeSystemId(prefs), concept = o.optString("category"),
                    imageUri = o.optString("imageUri"),
                    notes = listOf(o.optString("detail"), o.optString("notes")).filter { it.isNotBlank() }.joinToString("\n")
                )
            },
            defaults = { demoCharacters(prefs) })

    fun saveCharacters(prefs: SharedPreferences, list: List<CharacterSheet>) =
        save(prefs, CHARACTERS, list.map { it.toJson() })

    fun loadMonsters(prefs: SharedPreferences): List<MonsterSheet> =
        load(prefs, BESTIARY, "records_bestiary", ::decodeMonster,
            migrate = { o ->
                MonsterSheet(
                    id = o.optString("id").ifBlank { newId() }, name = o.optString("name"),
                    systemId = activeSystemId(prefs), type = o.optString("category"),
                    imageUri = o.optString("imageUri"),
                    notes = listOf(o.optString("detail"), o.optString("notes")).filter { it.isNotBlank() }.joinToString("\n")
                )
            },
            defaults = { demoMonsters(prefs) })

    fun saveMonsters(prefs: SharedPreferences, list: List<MonsterSheet>) =
        save(prefs, BESTIARY, list.map { it.toJson() })

    fun loadSpells(prefs: SharedPreferences): List<SpellEntry> =
        load(prefs, SPELLS, "records_spells", ::decodeSpell,
            migrate = { o ->
                val category = o.optString("category")
                SpellEntry(
                    id = o.optString("id").ifBlank { newId() }, name = o.optString("name"),
                    level = Regex("\\d+").find(category)?.value?.toIntOrNull() ?: 0,
                    school = category.substringAfter("•", "").trim(),
                    range = o.optString("detail"), description = o.optString("notes")
                )
            },
            defaults = { demoSpells() })

    fun saveSpells(prefs: SharedPreferences, list: List<SpellEntry>) =
        save(prefs, SPELLS, list.map { it.toJson() })

    fun loadItems(prefs: SharedPreferences): List<ItemEntry> =
        load(prefs, ITEMS, "records_items", ::decodeItem,
            migrate = { o ->
                val detail = o.optString("detail")
                val weight = Regex("(?i)peso:\\s*([0-9]+(?:[.,][0-9]+)?)").find(detail)
                    ?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
                ItemEntry(
                    id = o.optString("id").ifBlank { newId() }, name = o.optString("name"),
                    type = o.optString("category"), weight = weight ?: 0.0,
                    description = listOf(if (weight == null) detail else "", o.optString("notes"))
                        .filter { it.isNotBlank() }.joinToString("\n")
                )
            },
            defaults = { demoItems() })

    fun saveItems(prefs: SharedPreferences, list: List<ItemEntry>) =
        save(prefs, ITEMS, list.map { it.toJson() })

    fun loadTraits(prefs: SharedPreferences): List<TraitEntry> =
        load(prefs, TRAITS, "records_traits", ::decodeTrait, migrate = ::decodeTrait, defaults = { emptyList() })

    fun saveTraits(prefs: SharedPreferences, list: List<TraitEntry>) =
        save(prefs, TRAITS, list.map { it.toJson() })

    private fun <T> load(
        prefs: SharedPreferences, key: String, oldKey: String,
        decode: (JSONObject) -> T, migrate: (JSONObject) -> T, defaults: () -> List<T>
    ): List<T> {
        prefs.getString(key, null)?.let { raw ->
            return runCatching { JSONArray(raw).objects().map(decode) }.getOrDefault(emptyList())
        }
        // Primeira abertura com o formato novo: aproveita os registros simples antigos (sem apagá-los).
        prefs.getString(oldKey, null)?.let { raw ->
            runCatching { JSONArray(raw).objects().map(migrate) }.getOrNull()?.let { return it }
        }
        return defaults()
    }

    private fun save(prefs: SharedPreferences, key: String, items: List<JSONObject>) {
        val array = JSONArray()
        items.forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    // --- exemplos demonstrativos (editáveis) ---

    private fun demoCharacters(prefs: SharedPreferences): List<CharacterSheet> {
        val p = SystemProfiles.resolve(prefs, "dnd5e")
        return listOf(
            CharacterSheet(
                id = "demo-davi", name = "Davi", systemId = "dnd5e", concept = "Hemomante", origin = "Humano",
                level = 2, armorClass = 13, proficiencyBonus = 2,
                attributes = mapOf("strength" to 10, "dexterity" to 14, "constitution" to 14,
                    "intelligence" to 12, "wisdom" to 10, "charisma" to 13),
                resources = mapOf("hp" to ResourceValue(20, 20), "hit_dice" to ResourceValue(2, 2)),
                skills = p.skillTemplates.map {
                    SkillEntry(it.name, it.attributeKey, proficient = it.name == "Arcanismo" || it.name == "Percepção", systemId = "dnd5e")
                },
                attacks = listOf(AttackEntry("Adaga", "+4", "1d4+2 perfurante")),
                inventory = listOf(InventoryEntry("demo-adaga", 1, true), InventoryEntry("demo-pocao", 2, false)),
                notes = "Ficha demonstrativa. Edite os campos para personalizar."
            )
        )
    }

    private fun demoMonsters(prefs: SharedPreferences): List<MonsterSheet> = listOf(
        MonsterSheet(
            id = "demo-lobo", name = "Lobo selvagem", systemId = "dnd5e", type = "Fera", size = "Médio",
            challenge = "1/4", armorClass = 12, hp = 12, hpFormula = "2d8+3", speed = "12 m",
            attributes = mapOf("strength" to 14, "dexterity" to 15, "constitution" to 12,
                "intelligence" to 3, "wisdom" to 12, "charisma" to 6),
            senses = "Percepção passiva 13",
            entries = listOf(
                NamedEntry(EntryKind.TRAIT, "Faro apurado", "Tem vantagem em testes de Percepção baseados no olfato."),
                NamedEntry(EntryKind.ACTION, "Mordida", "Ataque corpo a corpo: +4 para acertar. Dano: 2d4+2 perfurante.")
            ),
            notes = "Criatura demonstrativa editável."
        )
    )

    private fun demoItems(): List<ItemEntry> = listOf(
        ItemEntry(id = "demo-adaga", name = "Adaga", type = "Arma", weight = 0.5, value = "2 po",
            damage = "1d4 perfurante", properties = "Acuidade, leve, arremesso",
            description = "Lâmina curta e afiada. Item demonstrativo editável."),
        ItemEntry(id = "demo-pocao", name = "Poção de cura", type = "Consumível", rarity = "Comum", weight = 0.5,
            value = "50 po", damage = "2d4+2 de cura", description = "Restaura pontos de vida. Item demonstrativo editável.")
    )

    private fun demoSpells(): List<SpellEntry> = listOf(
        SpellEntry(
            id = "demo-chama", name = "Chama viva", level = 1, school = "Evocação",
            castingTime = "1 ação", range = "18 m", components = "V, S", duration = "Instantânea",
            damage = "2d8 necrótico",
            description = "Uma labareda escura atinge uma criatura à vista. Magia demonstrativa editável."
        )
    )
}
