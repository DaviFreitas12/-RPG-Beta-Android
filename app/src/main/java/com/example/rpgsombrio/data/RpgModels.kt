package com.example.rpgsombrio.data

// Modelos iniciais compatíveis com a estrutura JSON enviada.
// Os campos numéricos são Int para facilitar cálculos no aplicativo.

data class Attribute(
    val name: String,
    val value: Int = 10,
    val proficiency: Int = 0
)

data class PlayerCharacter(
    val id: String,
    val name: String = "",
    val characterClass: String = "",
    val image: String = "",
    val level: Int = 1,
    val hp: Int = 10,
    val maxHp: Int = 10,
    val armorClass: Int = 10,
    val proficiencyBonus: Int = 2,
    val attributes: List<Attribute> = emptyList(),
    val advantages: List<String> = emptyList(),
    val inventoryIds: List<String> = emptyList(),
    val notes: String = "",
    val ownerUserId: String = "",
    val campaignId: String = "",
    val systemId: String = "dnd5e",
    val skills: Map<String, Int> = emptyMap(),
    val conditions: List<String> = emptyList(),
    val customFields: Map<String, String> = emptyMap()
)

data class Monster(
    val id: String,
    val name: String = "",
    val image: String = "",
    val hp: Int = 10,
    val maxHp: Int = 10,
    val armorClass: Int = 10,
    val type: String = "",
    val attributes: List<Attribute> = emptyList(),
    val abilities: List<String> = emptyList(),
    val advantages: List<String> = emptyList(),
    val notes: String = "",
    val campaignId: String = "",
    val systemId: String = "dnd5e",
    val customFields: Map<String, String> = emptyMap()
)

data class Spell(
    val id: String,
    val name: String = "",
    val level: Int = 0,
    val school: String = "",
    val damage: String = "",
    val bonus: String = "",
    val range: String = "",
    val description: String = ""
)

data class Item(
    val id: String,
    val name: String = "",
    val type: String = "",
    val bonus: String = "",
    val weight: String = "",
    val description: String = ""
)

data class Campaign(
    val id: String,
    val name: String,
    val description: String = "",
    val masterUserId: String,
    val systemId: String = "dnd5e",
    val inviteCode: String? = null,
    val isArchived: Boolean = false,
    /** Método de criação de atributos escolhido pelo mestre (0.10.3). "" = padrão do sistema. Ver [CreationMethod]. */
    val creationMethod: String = "",
    /** Como a vida é ganha no level up (0.10.4). "" = média fixa. Ver [HpMethod]. */
    val hpMethod: String = ""
)

data class CampaignMember(
    val userId: String,
    val role: String, // "MASTER" ou "PLAYER"
    val characterIds: List<String> = emptyList()
)
