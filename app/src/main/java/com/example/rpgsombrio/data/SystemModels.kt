package com.example.rpgsombrio.data

/**
 * Sistema de regras selecionado por campanha. Os dados são declarativos:
 * a ficha não presume que todo RPG use FOR/DES/CON ou pontos de vida.
 */
enum class RuleSystemKind { DND_5E, DND_2024, VAMPIRE, CUSTOM, GENERIC }

data class AttributeDefinition(
    val key: String,
    val label: String,
    val abbreviation: String,
    val defaultValue: Int = 10,
    val minValue: Int? = null,
    val maxValue: Int? = null,
    val hasModifier: Boolean = false,
    val modifierFormula: String? = null
)

data class ResourceDefinition(
    val key: String,
    val label: String,
    val defaultMaximum: Int = 0,
    val currentValueLabel: String = "Atual",
    val maximumValueLabel: String = "Máximo"
)

data class DiceDefinition(val sides: Int, val label: String = "d$sides")

data class RuleSystemDefinition(
    val id: String,
    val name: String,
    val description: String,
    val kind: RuleSystemKind,
    val attributes: List<AttributeDefinition>,
    val resources: List<ResourceDefinition>,
    val dice: List<DiceDefinition>,
    val usesLevels: Boolean = true,
    val usesArmorClass: Boolean = false,
    val usesSkills: Boolean = true,
    val isBuiltIn: Boolean = false
)

data class CampaignSystemSettings(
    val campaignId: String,
    val systemId: String,
    val customRulesJson: String? = null,
    val houseRules: List<String> = emptyList()
)

/** Catálogo inicial. Regras completas e conteúdo oficial devem ser adicionados por módulos próprios. */
object RuleSystemCatalog {
    private val dndAttributes = listOf(
        AttributeDefinition("strength", "Força", "FOR", hasModifier = true, modifierFormula = "(value - 10) / 2"),
        AttributeDefinition("dexterity", "Destreza", "DES", hasModifier = true, modifierFormula = "(value - 10) / 2"),
        AttributeDefinition("constitution", "Constituição", "CON", hasModifier = true, modifierFormula = "(value - 10) / 2"),
        AttributeDefinition("intelligence", "Inteligência", "INT", hasModifier = true, modifierFormula = "(value - 10) / 2"),
        AttributeDefinition("wisdom", "Sabedoria", "SAB", hasModifier = true, modifierFormula = "(value - 10) / 2"),
        AttributeDefinition("charisma", "Carisma", "CAR", hasModifier = true, modifierFormula = "(value - 10) / 2")
    )
    private val standardDice = listOf(4, 6, 8, 10, 12, 20, 100).map { DiceDefinition(it) }

    val systems: List<RuleSystemDefinition> = listOf(
        RuleSystemDefinition(
            id = "dnd5e", name = "D&D 5e", description = "Atributos clássicos, níveis, classes e pontos de vida.",
            kind = RuleSystemKind.DND_5E, attributes = dndAttributes,
            resources = listOf(ResourceDefinition("hp", "Pontos de vida", 10), ResourceDefinition("hit_dice", "Dados de vida")),
            dice = standardDice, usesArmorClass = true, isBuiltIn = true
        ),
        RuleSystemDefinition(
            id = "dnd2024", name = "D&D 2024", description = "Estrutura compatível com a revisão de 2024.",
            kind = RuleSystemKind.DND_2024, attributes = dndAttributes,
            resources = listOf(ResourceDefinition("hp", "Pontos de vida", 10), ResourceDefinition("hit_dice", "Dados de vida")),
            dice = standardDice, usesArmorClass = true, isBuiltIn = true
        ),
        RuleSystemDefinition(
            id = "vampire", name = "Vampiro", description = "Atributos, habilidades e trilhas de recursos configuráveis.",
            kind = RuleSystemKind.VAMPIRE,
            attributes = listOf(
                AttributeDefinition("strength", "Força", "FOR"),
                AttributeDefinition("dexterity", "Destreza", "DES"),
                AttributeDefinition("stamina", "Vigor", "VIG"),
                AttributeDefinition("charisma", "Carisma", "CAR"),
                AttributeDefinition("manipulation", "Manipulação", "MAN"),
                AttributeDefinition("composure", "Autocontrole", "AUT"),
                AttributeDefinition("intelligence", "Inteligência", "INT"),
                AttributeDefinition("wits", "Raciocínio", "RAC"),
                AttributeDefinition("resolve", "Determinação", "DET")
            ),
            resources = listOf(ResourceDefinition("health", "Vitalidade", 5), ResourceDefinition("willpower", "Força de vontade", 5)),
            dice = listOf(DiceDefinition(10)), usesLevels = false, usesArmorClass = false, isBuiltIn = true
        ),
        RuleSystemDefinition(
            id = "custom", name = "Sistema próprio", description = "Crie atributos, recursos, dados e regras da sua mesa.",
            kind = RuleSystemKind.CUSTOM, attributes = emptyList(), resources = emptyList(),
            dice = standardDice, isBuiltIn = true
        ),
        RuleSystemDefinition(
            id = "generic", name = "Outro sistema", description = "Comece com uma ficha neutra e personalize depois.",
            kind = RuleSystemKind.GENERIC, attributes = emptyList(), resources = emptyList(),
            dice = standardDice, isBuiltIn = true
        )
    )

}
