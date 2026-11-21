package com.example.rpgsombrio.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ---------------------------------------------------------------------------------------------
// Modelos estruturados de ficha, criatura e magia. Tudo é guardado em SharedPreferences (JSON).
// ---------------------------------------------------------------------------------------------

data class ResourceValue(val current: Int = 0, val max: Int = 0)

data class SkillEntry(
    val name: String,
    val attributeKey: String = "",
    val proficient: Boolean = false,
    val extra: Int = 0
)

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
    val description: String = ""
)

/** Linha do inventário de um personagem: referência a um [ItemEntry] da biblioteca. */
data class InventoryEntry(val itemId: String, val quantity: Int = 1, val equipped: Boolean = false)

val ITEM_TYPES = listOf("Arma", "Armadura", "Escudo", "Consumível", "Ferramenta", "Mágico", "Tesouro", "Outro")

fun formatWeight(weight: Double): String {
    if (weight % 1.0 == 0.0) return weight.toInt().toString()
    return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", weight)
}

fun inventoryWeight(inventory: List<InventoryEntry>, items: List<ItemEntry>): Double =
    inventory.sumOf { entry -> (items.firstOrNull { it.id == entry.itemId }?.weight ?: 0.0) * entry.quantity }

data class CharacterSheet(
    val id: String,
    val name: String = "",
    val systemId: String = "dnd5e",
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
    val resistances: String = "",
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
    val higherLevels: String = ""
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
    val hasModifier: Boolean
)

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
    val originLabel: String
) {
    val usesProficiency: Boolean get() = attributes.any { it.hasModifier }
    fun attr(key: String): SheetAttr? = attributes.firstOrNull { it.key == key }
}

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

    fun resolve(prefs: SharedPreferences, systemId: String): SystemProfile {
        val def = RuleSystemCatalog.systems.firstOrNull { it.id == systemId } ?: RuleSystemCatalog.systems.first()
        return when (def.kind) {
            RuleSystemKind.CUSTOM, RuleSystemKind.GENERIC -> custom(prefs, def)
            RuleSystemKind.VAMPIRE -> SystemProfile(
                id = def.id, name = def.name,
                attributes = def.attributes.map { SheetAttr(it.key, it.label, it.abbreviation, 1, false) },
                resources = def.resources.map { SheetResource(it.key, it.label, it.defaultMaximum) },
                skillTemplates = emptyList(),
                usesLevels = false, usesArmorClass = false, usesSkills = true, autoProficiency = false,
                conceptLabel = "Clã", originLabel = "Conceito"
            )
            else -> SystemProfile(
                id = def.id, name = def.name,
                attributes = def.attributes.map { SheetAttr(it.key, it.label, it.abbreviation, it.defaultValue, it.hasModifier) },
                resources = def.resources.map { SheetResource(it.key, it.label, it.defaultMaximum) },
                skillTemplates = dndSkills,
                usesLevels = true, usesArmorClass = true, usesSkills = true, autoProficiency = true,
                conceptLabel = "Classe", originLabel = "Raça / origem"
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

private fun CharacterSheet.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("systemId", systemId)
    .put("concept", concept).put("origin", origin).put("level", level).put("imageUri", imageUri)
    .put("armorClass", armorClass).put("proficiencyBonus", proficiencyBonus)
    .put("attributes", intMapToJson(attributes))
    .put("resources", JSONObject().also { o ->
        resources.forEach { (k, v) -> o.put(k, JSONObject().put("current", v.current).put("max", v.max)) }
    })
    .put("skills", JSONArray().also { a ->
        skills.forEach { a.put(JSONObject().put("name", it.name).put("attr", it.attributeKey).put("prof", it.proficient).put("extra", it.extra)) }
    })
    .put("attacks", JSONArray().also { a ->
        attacks.forEach { a.put(JSONObject().put("name", it.name).put("bonus", it.bonus).put("damage", it.damage)) }
    })
    .put("spellIds", JSONArray(spellIds))
    .put("inventory", JSONArray().also { a ->
        inventory.forEach { a.put(JSONObject().put("itemId", it.itemId).put("qty", it.quantity).put("equipped", it.equipped)) }
    })
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
        SkillEntry(it.optString("name"), it.optString("attr"), it.optBoolean("prof"), it.optInt("extra"))
    },
    attacks = o.optJSONArray("attacks").objects().map {
        AttackEntry(it.optString("name"), it.optString("bonus"), it.optString("damage"))
    },
    spellIds = o.optJSONArray("spellIds").strings(),
    inventory = o.optJSONArray("inventory").objects().map {
        InventoryEntry(it.optString("itemId"), it.optInt("qty", 1), it.optBoolean("equipped"))
    },
    equipment = o.optString("equipment"), notes = o.optString("notes")
)

private fun MonsterSheet.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("systemId", systemId)
    .put("type", type).put("size", size).put("challenge", challenge).put("imageUri", imageUri)
    .put("armorClass", armorClass).put("hp", hp).put("hpFormula", hpFormula).put("speed", speed)
    .put("attributes", intMapToJson(attributes))
    .put("resistances", resistances).put("senses", senses).put("languages", languages)
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

private fun decodeSpell(o: JSONObject): SpellEntry = SpellEntry(
    id = o.getString("id"), name = o.optString("name"), level = o.optInt("level"),
    school = o.optString("school"), classes = o.optString("classes"),
    castingTime = o.optString("castingTime"), range = o.optString("range"),
    components = o.optString("components"), duration = o.optString("duration"),
    concentration = o.optBoolean("concentration"), ritual = o.optBoolean("ritual"),
    damage = o.optString("damage"), cost = o.optString("cost"),
    description = o.optString("description"), higherLevels = o.optString("higherLevels")
)

private fun ItemEntry.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("type", type).put("rarity", rarity).put("weight", weight)
    .put("value", value).put("bonus", bonus).put("damage", damage).put("properties", properties)
    .put("attunement", attunement).put("description", description)

private fun decodeItem(o: JSONObject): ItemEntry = ItemEntry(
    id = o.getString("id"), name = o.optString("name"), type = o.optString("type"),
    rarity = o.optString("rarity"), weight = o.optDouble("weight", 0.0), value = o.optString("value"),
    bonus = o.optString("bonus"), damage = o.optString("damage"), properties = o.optString("properties"),
    attunement = o.optBoolean("attunement"), description = o.optString("description")
)

// ---------------------------------------------------------------------------------------------
// Armazenamento
// ---------------------------------------------------------------------------------------------

object SheetStore {
    private const val CHARACTERS = "sheets_characters"
    private const val BESTIARY = "sheets_bestiary"
    private const val SPELLS = "sheets_spells"
    private const val ITEMS = "sheets_items"

    fun newId(): String = UUID.randomUUID().toString()

    /** A campanha guarda o nome exibido do sistema; aqui ele vira o id do catálogo. */
    fun activeSystemId(prefs: SharedPreferences): String {
        val name = prefs.getString("active_campaign_system", "D&D 5e")
        return RuleSystemCatalog.systems.firstOrNull { it.name == name }?.id ?: "dnd5e"
    }

    // --- criação de registros novos, já preenchidos conforme o sistema ---

    fun newCharacter(prefs: SharedPreferences): CharacterSheet {
        val p = SystemProfiles.resolve(prefs, activeSystemId(prefs))
        return CharacterSheet(
            id = newId(), name = "Novo personagem", systemId = p.id,
            attributes = p.attributes.associate { it.key to it.default },
            resources = p.resources.associate { it.key to ResourceValue(it.defaultMax, it.defaultMax) },
            skills = p.skillTemplates.map { SkillEntry(it.name, it.attributeKey) }
        )
    }

    fun newMonster(prefs: SharedPreferences): MonsterSheet {
        val p = SystemProfiles.resolve(prefs, activeSystemId(prefs))
        return MonsterSheet(id = newId(), name = "Nova criatura", systemId = p.id,
            attributes = p.attributes.associate { it.key to it.default })
    }

    fun newSpell(): SpellEntry = SpellEntry(id = newId(), name = "Nova magia", level = 1)

    fun newItem(): ItemEntry = ItemEntry(id = newId(), name = "Novo item", type = "Outro")

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
                    SkillEntry(it.name, it.attributeKey, proficient = it.name == "Arcanismo" || it.name == "Percepção")
                },
                attacks = listOf(AttackEntry("Adaga", "+4", "1d4+2 perfurante")),
                inventory = listOf(InventoryEntry("demo-adaga", 1, true), InventoryEntry("demo-pocao", 2, false)),
                notes = "Ficha demonstrativa. Edite os campos para personalizar."
            )
        )
    }

    private fun demoMonsters(prefs: SharedPreferences): List<MonsterSheet> = listOf(
        MonsterSheet(
            id = "demo-lobo", name = "Lobo sombrio", systemId = "dnd5e", type = "Fera", size = "Médio",
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
            id = "demo-chama", name = "Chama sombria", level = 1, school = "Evocação",
            castingTime = "1 ação", range = "18 m", components = "V, S", duration = "Instantânea",
            damage = "2d8 necrótico",
            description = "Uma labareda escura atinge uma criatura à vista. Magia demonstrativa editável."
        )
    )
}
