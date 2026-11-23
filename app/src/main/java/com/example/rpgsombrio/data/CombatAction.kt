package com.example.rpgsombrio.data

data class CombatAction(
    val id: String,
    val name: String,
    val resourceCost: String,
    val damageType: String, // ex: cortante, fogo
    val bonus: String,
    val damage: String,
    val isAbility: Boolean = false
)
