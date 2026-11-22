package com.example.rpgsombrio.data

import android.content.SharedPreferences
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

// ---------------------------------------------------------------------------------------------
// Sistemas criados pelo usuário. Cada sistema é uma definição completa (atributos, recursos,
// perícias, blocos da ficha e orçamento de vantagens/desvantagens), salva em JSON no aparelho.
// D&D e Vampiro continuam como modelos de fábrica que podem ser duplicados e editados.
// ---------------------------------------------------------------------------------------------

data class UserAttribute(
    val key: String,
    val label: String,
    val abbr: String,
    val default: Int = 10,
    /** null = sem limite. */
    val min: Int? = null,
    val max: Int? = null,
    val hasModifier: Boolean = false,
    /** Modificador = piso((valor − base) / divisor). D&D usa base 10 e divisor 2. */
    val modBase: Int = 10,
    val modDivisor: Int = 2
)

data class UserResource(val key: String, val label: String, val defaultMax: Int = 10)

data class UserSkill(val name: String, val attributeKey: String = "")

data class UserSystem(
    val id: String,
    val name: String = "Novo sistema",
    val description: String = "",
    val attributes: List<UserAttribute> = emptyList(),
    val resources: List<UserResource> = emptyList(),
    val skills: List<UserSkill> = emptyList(),
    val usesLevels: Boolean = false,
    val usesArmorClass: Boolean = false,
    val usesSkills: Boolean = true,
    val usesAttacks: Boolean = false,
    val usesSpells: Boolean = false,
    val usesInventory: Boolean = true,
    val usesTraits: Boolean = true,
    /** Pontos disponíveis para vantagens (desvantagens devolvem pontos). 0 = sem orçamento. */
    val traitBudget: Int = 0,
    val conceptLabel: String = "Conceito",
    val originLabel: String = "Origem",
    val testFormula: String = "",
    val initiativeFormula: String = "",
    val dice: List<Int> = listOf(4, 6, 8, 10, 12, 20, 100),
    /** [ROLL_SUM] ou [ROLL_POOL]. Em reservas, os testes contam sucessos em vez de somar. */
    val rollStyle: String = ROLL_SUM,
    val poolSides: Int = 10,
    val poolTarget: Int = 6
)

val ALL_DICE_SIDES = listOf(4, 6, 8, 10, 12, 20, 100)

/** Opção exibida nos seletores de sistema (campanha, nova ficha, troca de sistema). */
data class SystemOption(val id: String, val name: String, val builtIn: Boolean)

fun UserSystem.toProfile(): SystemProfile = SystemProfile(
    id = id, name = name,
    attributes = attributes.map {
        SheetAttr(it.key, it.label, it.abbr, it.default, it.hasModifier, it.min, it.max, it.modBase, it.modDivisor)
    },
    resources = resources.map { SheetResource(it.key, it.label, it.defaultMax) },
    skillTemplates = skills.map { SkillTemplate(it.name, it.attributeKey) },
    usesLevels = usesLevels, usesArmorClass = usesArmorClass, usesSkills = usesSkills,
    autoProficiency = false,
    conceptLabel = conceptLabel.ifBlank { "Conceito" }, originLabel = originLabel.ifBlank { "Origem" },
    usesAttacks = usesAttacks, usesSpells = usesSpells, usesInventory = usesInventory,
    usesTraits = usesTraits, traitBudget = traitBudget,
    testFormula = testFormula, initiativeFormula = initiativeFormula,
    rollStyle = rollStyle, poolSides = poolSides, poolTarget = poolTarget,
    dice = dice.ifEmpty { ALL_DICE_SIDES }
)

object UserSystemStore {
    private const val KEY = "user_systems"

    fun newId(): String = "sys-" + UUID.randomUUID().toString().take(8)
    fun newKey(): String = "k" + UUID.randomUUID().toString().take(6)

    fun blank(): UserSystem = UserSystem(
        id = newId(), name = "Novo sistema",
        attributes = listOf(UserAttribute(newKey(), "Atributo 1", "AT1", default = 1)),
        resources = listOf(UserResource(newKey(), "Vida", 10))
    )

    /** Cópia editável de um sistema de fábrica ou de outro sistema do usuário. */
    fun duplicateOf(prefs: SharedPreferences, sourceId: String): UserSystem {
        val userSource = load(prefs).firstOrNull { it.id == sourceId }
        if (userSource != null) return userSource.copy(id = newId(), name = userSource.name + " (cópia)")
        val profile = SystemProfiles.resolve(prefs, sourceId)
        return UserSystem(
            id = newId(), name = profile.name + " (cópia)",
            attributes = profile.attributes.map {
                UserAttribute(it.key, it.label, it.abbr, it.default, it.min, it.max, it.hasModifier, it.modBase, it.modDivisor)
            },
            resources = profile.resources.map { UserResource(it.key, it.label, it.defaultMax) },
            skills = profile.skillTemplates.map { UserSkill(it.name, it.attributeKey) },
            usesLevels = profile.usesLevels, usesArmorClass = profile.usesArmorClass,
            usesSkills = profile.usesSkills, usesAttacks = profile.usesAttacks,
            usesSpells = profile.usesSpells, usesInventory = profile.usesInventory,
            usesTraits = profile.usesTraits, traitBudget = profile.traitBudget,
            conceptLabel = profile.conceptLabel, originLabel = profile.originLabel,
            testFormula = profile.testFormula, initiativeFormula = profile.initiativeFormula,
            rollStyle = profile.rollStyle, poolSides = profile.poolSides, poolTarget = profile.poolTarget,
            dice = profile.dice
        )
    }

    fun load(prefs: SharedPreferences): List<UserSystem> {
        val raw = prefs.getString(KEY, null)
        if (raw == null) {
            val migrated = migrateLegacy(prefs)
            save(prefs, migrated)
            return migrated
        }
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { decode(array.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }

    fun save(prefs: SharedPreferences, list: List<UserSystem>) {
        val array = JSONArray()
        list.forEach { array.put(encode(it)) }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    /**
     * O construtor antigo guardava um único "Sistema próprio" em chaves soltas (custom_*), com os nomes
     * dos atributos como chaves. Ele vira um sistema do usuário com id "custom", mantendo as mesmas
     * chaves e as mesmas regras de exibição, para que as fichas existentes não mudem.
     */
    private fun migrateLegacy(prefs: SharedPreferences): List<UserSystem> {
        val hasLegacy = prefs.contains("custom_attributes") || prefs.contains("custom_system_name")
        if (!hasLegacy) return emptyList()
        fun list(key: String, fallback: List<String>): List<String> {
            val raw = prefs.getString(key, null)
            if (raw.isNullOrBlank()) return fallback
            return runCatching { JSONArray(raw).let { a -> List(a.length()) { a.getString(it) } } }.getOrDefault(fallback)
        }
        val attrs = list("custom_attributes", listOf("FOR", "DES", "CON", "INT", "SAB", "CAR"))
        val resources = list("custom_resources", listOf("Pontos de vida", "Mana"))
        val skills = list("custom_skills", listOf("Atletismo", "Percepção"))
        val dice = list("custom_dice", ALL_DICE_SIDES.map { "d$it" })
            .mapNotNull { it.removePrefix("d").toIntOrNull() }.ifEmpty { ALL_DICE_SIDES }
        return listOf(
            UserSystem(
                id = "custom",
                name = prefs.getString("custom_system_name", null)?.ifBlank { null } ?: "Sistema próprio",
                description = prefs.getString("custom_system_description", "") ?: "",
                attributes = attrs.map { UserAttribute(it, it, if (it.length <= 4) it else it.take(3).uppercase(), 10) },
                resources = resources.map { UserResource(it, it, 10) },
                skills = skills.map { UserSkill(it, "") },
                usesLevels = true, usesArmorClass = true, usesSkills = true,
                usesAttacks = true, usesSpells = true, usesInventory = true, usesTraits = true,
                conceptLabel = "Classe / conceito", originLabel = "Origem",
                testFormula = prefs.getString("custom_test_formula", "") ?: "",
                initiativeFormula = prefs.getString("custom_initiative_formula", "") ?: "",
                dice = dice
            )
        )
    }

    private fun encode(s: UserSystem): JSONObject = JSONObject()
        .put("id", s.id).put("name", s.name).put("description", s.description)
        .put("attributes", JSONArray().also { a ->
            s.attributes.forEach {
                a.put(JSONObject().put("key", it.key).put("label", it.label).put("abbr", it.abbr)
                    .put("default", it.default).put("hasModifier", it.hasModifier)
                    .put("modBase", it.modBase).put("modDivisor", it.modDivisor)
                    .also { o ->
                        it.min?.let { v -> o.put("min", v) }
                        it.max?.let { v -> o.put("max", v) }
                    })
            }
        })
        .put("resources", JSONArray().also { a ->
            s.resources.forEach { a.put(JSONObject().put("key", it.key).put("label", it.label).put("defaultMax", it.defaultMax)) }
        })
        .put("skills", JSONArray().also { a ->
            s.skills.forEach { a.put(JSONObject().put("name", it.name).put("attr", it.attributeKey)) }
        })
        .put("usesLevels", s.usesLevels).put("usesArmorClass", s.usesArmorClass).put("usesSkills", s.usesSkills)
        .put("usesAttacks", s.usesAttacks).put("usesSpells", s.usesSpells).put("usesInventory", s.usesInventory)
        .put("usesTraits", s.usesTraits).put("traitBudget", s.traitBudget)
        .put("conceptLabel", s.conceptLabel).put("originLabel", s.originLabel)
        .put("testFormula", s.testFormula).put("initiativeFormula", s.initiativeFormula)
        .put("dice", JSONArray(s.dice))
        .put("rollStyle", s.rollStyle).put("poolSides", s.poolSides).put("poolTarget", s.poolTarget)

    private fun decode(o: JSONObject): UserSystem {
        fun objs(name: String): List<JSONObject> = o.optJSONArray(name).let { a ->
            if (a == null) emptyList() else List(a.length()) { a.getJSONObject(it) }
        }
        return UserSystem(
            id = o.getString("id"), name = o.optString("name"), description = o.optString("description"),
            attributes = objs("attributes").map {
                UserAttribute(
                    key = it.getString("key"), label = it.optString("label"), abbr = it.optString("abbr"),
                    default = it.optInt("default", 10),
                    min = if (it.has("min")) it.optInt("min") else null,
                    max = if (it.has("max")) it.optInt("max") else null,
                    hasModifier = it.optBoolean("hasModifier"),
                    modBase = it.optInt("modBase", 10), modDivisor = it.optInt("modDivisor", 2)
                )
            },
            resources = objs("resources").map { UserResource(it.getString("key"), it.optString("label"), it.optInt("defaultMax", 10)) },
            skills = objs("skills").map { UserSkill(it.optString("name"), it.optString("attr")) },
            usesLevels = o.optBoolean("usesLevels"), usesArmorClass = o.optBoolean("usesArmorClass"),
            usesSkills = o.optBoolean("usesSkills", true), usesAttacks = o.optBoolean("usesAttacks"),
            usesSpells = o.optBoolean("usesSpells"), usesInventory = o.optBoolean("usesInventory", true),
            usesTraits = o.optBoolean("usesTraits", true), traitBudget = o.optInt("traitBudget"),
            conceptLabel = o.optString("conceptLabel", "Conceito"), originLabel = o.optString("originLabel", "Origem"),
            testFormula = o.optString("testFormula"), initiativeFormula = o.optString("initiativeFormula"),
            dice = o.optJSONArray("dice").let { a ->
                if (a == null) ALL_DICE_SIDES else List(a.length()) { a.getInt(it) }
            },
            rollStyle = o.optString("rollStyle", ROLL_SUM).ifBlank { ROLL_SUM },
            poolSides = o.optInt("poolSides", 10), poolTarget = o.optInt("poolTarget", 6)
        )
    }
}

/** Sistemas selecionáveis: modelos de fábrica (D&D, Vampiro) + sistemas do usuário. */
fun systemOptions(prefs: SharedPreferences): List<SystemOption> {
    val builtIn = RuleSystemCatalog.systems
        .filter { it.kind != RuleSystemKind.CUSTOM && it.kind != RuleSystemKind.GENERIC }
        .map { SystemOption(it.id, it.name, true) }
    val user = UserSystemStore.load(prefs).map { SystemOption(it.id, it.name, false) }
    return builtIn + user
}

fun systemDisplayName(prefs: SharedPreferences, systemId: String): String =
    systemOptions(prefs).firstOrNull { it.id == systemId }?.name
        ?: RuleSystemCatalog.systems.firstOrNull { it.id == systemId }?.name
        ?: systemId
