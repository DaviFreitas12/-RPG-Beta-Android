package com.example.rpgsombrio.engine

/**
 * Categorias do Bestiário (0.9.18): a categoria de uma criatura é o campo "Tipo" dela. Estas regras puras
 * (sem Android) juntam as categorias padrão, as que o mestre criou e as que já aparecem nas fichas,
 * sem diferenciar maiúsculas de minúsculas ("fera" e "Fera" são a mesma categoria).
 */

/** Categorias que sempre aparecem, mesmo vazias. O mestre pode criar outras. */
val DEFAULT_MONSTER_TYPES = listOf(
    "Aberração", "Celestial", "Construto", "Corruptor", "Dragão", "Elemental", "Fada", "Fera",
    "Gigante", "Humanoide", "Limo", "Monstruosidade", "Morto-vivo", "Planta"
)

/** Nome da pasta das criaturas sem tipo preenchido. */
const val NO_CATEGORY = "Sem categoria"

data class MonsterCategory(val name: String, val count: Int, val isDefault: Boolean, val isCustom: Boolean)

/** Chave de comparação: sem espaços nas pontas e sem diferença de maiúsculas. */
fun typeKey(text: String): String = text.trim().lowercase()

/**
 * Lista as categorias em ordem alfabética: as padrão, as criadas pelo mestre ([customs]) e as que só existem
 * porque alguma criatura usa esse tipo ([types] = o tipo de cada criatura). Criaturas sem tipo não entram aqui
 * (veja [NO_CATEGORY]).
 */
fun monsterCategories(customs: List<String>, types: List<String>): List<MonsterCategory> {
    val display = LinkedHashMap<String, String>()
    DEFAULT_MONSTER_TYPES.forEach { display.putIfAbsent(typeKey(it), it) }
    val defaultKeys = display.keys.toSet()
    customs.map { it.trim() }.filter { it.isNotEmpty() }.forEach { display.putIfAbsent(typeKey(it), it) }
    val customKeys = customs.map { typeKey(it) }.filter { it.isNotEmpty() }.toSet()
    types.map { it.trim() }.filter { it.isNotEmpty() }.forEach { display.putIfAbsent(typeKey(it), it) }
    val counts = types.map { typeKey(it) }.filter { it.isNotEmpty() }.groupingBy { it }.eachCount()
    return display.map { (key, name) ->
        MonsterCategory(name, counts[key] ?: 0, key in defaultKeys, key in customKeys && key !in defaultKeys)
    }.sortedBy { it.name.lowercase() }
}

/** Quantas criaturas não têm tipo. */
fun uncategorizedCount(types: List<String>): Int = types.count { it.isBlank() }

/** A criatura de tipo [type] pertence à categoria [category]? [NO_CATEGORY] vale para tipo vazio. */
fun inCategory(type: String, category: String): Boolean =
    if (category == NO_CATEGORY) type.isBlank() else typeKey(type) == typeKey(category)
