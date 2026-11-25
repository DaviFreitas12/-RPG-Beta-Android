package com.example.rpgsombrio.data

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Armazenamento das campanhas (0.10.x): lista em JSON nas preferências, como as demais coleções do app.
 * O aparelho tem um id de usuário local; quem criou a campanha ([Campaign.masterUserId]) é o mestre.
 */
object CampaignStore {
    private const val KEY = "campaigns_v1"
    private const val ACTIVE = "active_campaign_id"
    private const val USER = "local_user_id"

    /** Id deste aparelho/usuário, gerado na primeira vez. */
    fun localUserId(prefs: SharedPreferences): String {
        prefs.getString(USER, null)?.takeIf { it.isNotBlank() }?.let { return it }
        val id = SheetStore.newId()
        prefs.edit().putString(USER, id).apply()
        return id
    }

    /**
     * Lê as campanhas. Na primeira vez (antes de existir a lista), migra a campanha única das versões 0.9.x:
     * ela vira a primeira da lista, com este aparelho como mestre, e as fichas existentes continuam sem campanha.
     */
    fun load(prefs: SharedPreferences): List<Campaign> {
        prefs.getString(KEY, null)?.let { raw ->
            return runCatching {
                val array = JSONArray(raw)
                (0 until array.length()).map { decode(array.getJSONObject(it)) }
            }.getOrDefault(emptyList())
        }
        val migrated = legacyCampaign(
            id = SheetStore.newId(),
            name = prefs.getString("active_campaign_name", null),
            description = prefs.getString("active_campaign_description", null),
            systemId = SheetStore.legacyActiveSystemId(prefs),
            userId = localUserId(prefs),
            inviteCode = newInviteCode()
        )
        save(prefs, listOf(migrated))
        setActive(prefs, migrated.id)
        return listOf(migrated)
    }

    fun save(prefs: SharedPreferences, list: List<Campaign>) {
        val array = JSONArray()
        list.forEach { array.put(encode(it)) }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun activeId(prefs: SharedPreferences): String? = active(prefs)?.id

    fun active(prefs: SharedPreferences): Campaign? =
        activeCampaignOf(load(prefs), prefs.getString(ACTIVE, null))

    fun setActive(prefs: SharedPreferences, id: String?) {
        prefs.edit().putString(ACTIVE, id).apply()
    }

    /** Cria a campanha, com este aparelho como mestre, e já a deixa ativa. */
    fun create(prefs: SharedPreferences, name: String, description: String, systemId: String): Campaign {
        val campaign = Campaign(
            id = SheetStore.newId(),
            name = name.trim().ifBlank { DEFAULT_CAMPAIGN_NAME },
            description = description.trim(),
            masterUserId = localUserId(prefs),
            systemId = systemId,
            inviteCode = newInviteCode()
        )
        save(prefs, load(prefs) + campaign)
        setActive(prefs, campaign.id)
        return campaign
    }

    private fun playersKey(campaignId: String) = "campaign_players_v1_$campaignId"

    /** Jogadores que entraram nesta campanha pelo código (lista do mestre). */
    fun players(prefs: SharedPreferences, campaignId: String): List<CampaignPlayer> = runCatching {
        val array = JSONArray(prefs.getString(playersKey(campaignId), null) ?: "[]")
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            CampaignPlayer(o.optString("id"), o.optString("name"))
        }
    }.getOrDefault(emptyList())

    fun addPlayer(prefs: SharedPreferences, campaignId: String, userId: String, name: String) {
        val next = upsertPlayer(players(prefs, campaignId), userId, name)
        val array = JSONArray()
        next.forEach { array.put(JSONObject().put("id", it.userId).put("name", it.name)) }
        prefs.edit().putString(playersKey(campaignId), array.toString()).apply()
    }

    /**
     * Jogador: guarda a campanha recebida do mestre (cria ou atualiza) e a deixa ativa, para os personagens novos
     * nascerem nela. Devolve null se a campanha é deste próprio aparelho (quem criou já é o mestre).
     */
    fun joinRemote(prefs: SharedPreferences, info: CampaignInfo): Campaign? {
        if (info.masterUserId == localUserId(prefs)) return null
        val list = load(prefs)
        val joined = campaignFromMaster(info)
        save(prefs, if (list.any { it.id == info.id }) list.map { if (it.id == info.id) joined else it } else list + joined)
        setActive(prefs, joined.id)
        return joined
    }

    fun update(prefs: SharedPreferences, campaign: Campaign) {
        save(prefs, load(prefs).map { if (it.id == campaign.id) campaign else it })
    }

    /** Exclui a campanha. As fichas dela voltam para "Sem campanha" (não são apagadas). */
    fun delete(prefs: SharedPreferences, id: String) {
        save(prefs, load(prefs).filterNot { it.id == id })
        val sheets = SheetStore.loadCharacters(prefs)
        if (sheets.any { it.campaignId == id }) SheetStore.saveCharacters(prefs, releaseSheets(id, sheets))
        if (prefs.getString(ACTIVE, null) == id) setActive(prefs, null)
        prefs.edit().remove(playersKey(id)).apply()
    }

    private fun encode(c: Campaign): JSONObject = JSONObject()
        .put("id", c.id).put("name", c.name).put("description", c.description)
        .put("master", c.masterUserId).put("system", c.systemId)
        .put("invite", c.inviteCode.orEmpty()).put("archived", c.isArchived)
        .put("creation", c.creationMethod).put("hpMethod", c.hpMethod)

    private fun decode(o: JSONObject): Campaign = Campaign(
        id = o.getString("id"),
        name = o.optString("name"),
        description = o.optString("description"),
        masterUserId = o.optString("master"),
        systemId = o.optString("system", "dnd5e"),
        inviteCode = o.optString("invite").ifBlank { null },
        isArchived = o.optBoolean("archived", false),
        creationMethod = CreationMethod.normalize(o.optString("creation")),
        hpMethod = HpMethod.normalize(o.optString("hpMethod"))
    )
}
