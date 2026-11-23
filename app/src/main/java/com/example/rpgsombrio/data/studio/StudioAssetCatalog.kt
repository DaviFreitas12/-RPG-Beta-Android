package com.example.rpgsombrio.data.studio

import android.content.Context
import org.json.JSONObject

/** Carrega o catálogo oficial empacotado em app/src/main/assets/studio/catalog.json. */
object StudioAssetCatalog {
    private const val MANIFEST_PATH = "studio/catalog.json"

    fun load(context: Context): List<StudioAsset> = runCatching {
        val json = context.assets.open(MANIFEST_PATH).bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val entries = root.optJSONArray("assets") ?: return@runCatching emptyList()
        buildList {
            for (index in 0 until entries.length()) {
                val item = entries.optJSONObject(index) ?: continue
                val id = item.optString("id").trim()
                val name = item.optString("name").trim()
                val slot = enumValueOrNull<StudioSlot>(item.optString("slot"))
                val path = item.optString("resourcePath").trim()
                if (id.isEmpty() || name.isEmpty() || slot == null || path.isEmpty()) continue

                val bodyTypes = item.optStringSet("bodyTypes")
                    .mapNotNull { enumValueOrNull<StudioBodyType>(it) }
                    .toSet().ifEmpty { StudioBodyType.values().toSet() }
                val styles = item.optStringSet("styles")
                    .mapNotNull { enumValueOrNull<StudioStyle>(it) }
                    .toSet().ifEmpty { setOf(StudioStyle.DARK_FANTASY) }
                val rarity = enumValueOrNull<StudioRarity>(item.optString("rarity"))
                    ?: StudioRarity.COMMON
                val anchorJson = item.optJSONObject("anchor")
                val anchor = anchorJson?.let {
                    runCatching {
                        StudioAnchor(
                            name = it.getString("name"),
                            x = it.getDouble("x").toFloat(),
                            y = it.getDouble("y").toFloat(),
                            rotationDegrees = it.optDouble("rotationDegrees", 0.0).toFloat()
                        )
                    }.getOrNull()
                }
                add(
                    StudioAsset(
                        id = id,
                        name = name,
                        slot = slot,
                        resourcePath = path,
                        bodyTypes = bodyTypes,
                        styles = styles,
                        rarity = rarity,
                        anchor = anchor,
                        zIndex = item.optInt("zIndex", 0),
                        tags = item.optStringSet("tags"),
                        handsRequired = item.optInt("handsRequired", 0).coerceIn(0, 2),
                        isStarterAsset = item.optBoolean("isStarterAsset", false)
                    )
                )
            }
        }.distinctBy { it.id }
    }.getOrDefault(emptyList())

    private inline fun <reified T : Enum<T>> enumValueOrNull(value: String): T? =
        runCatching { enumValueOf<T>(value.trim().uppercase()) }.getOrNull()

    private fun JSONObject.optStringSet(key: String): Set<String> {
        val array = optJSONArray(key) ?: return emptySet()
        return buildSet {
            for (index in 0 until array.length()) {
                array.optString(index).trim().takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
    }
}
