package com.example.rpgsombrio.ui.campaign

import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.example.rpgsombrio.data.Campaign
import com.example.rpgsombrio.data.CampaignStore
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.toJsonString
import com.example.rpgsombrio.engine.PushResult
import com.example.rpgsombrio.engine.pushSheets
import com.example.rpgsombrio.engine.splitAddress

/**
 * Jogador: manda as fichas da campanha ao mestre (0.10.7). O endereço e o código ficam guardados quando ele entra na
 * campanha. Sem endereço guardado (entrou na 0.10.6), é só entrar de novo na campanha.
 */
internal object SheetSync {
    private val main = Handler(Looper.getMainLooper())
    private val post: (() -> Unit) -> Unit = { action -> main.post { action() } }

    private fun address(prefs: SharedPreferences, campaign: Campaign) = prefs.getString("join_addr_${campaign.id}", null).orEmpty()
    private fun code(prefs: SharedPreferences, campaign: Campaign) = prefs.getString("join_code_${campaign.id}", null).orEmpty()

    /** Há para onde enviar? */
    fun canPush(prefs: SharedPreferences, campaign: Campaign): Boolean =
        address(prefs, campaign).isNotBlank() && code(prefs, campaign).length == 4

    /** Manda ao mestre as fichas desta campanha que estão neste aparelho. */
    fun push(prefs: SharedPreferences, campaign: Campaign, sheets: List<CharacterSheet>, onResult: (PushResult) -> Unit = {}) {
        val userId = CampaignStore.localUserId(prefs)
        val mine = sheets.filter { it.campaignId == campaign.id }.map { it.copy(ownerUserId = userId).toJsonString() }
        if (mine.isEmpty() || !canPush(prefs, campaign)) { onResult(PushResult.Sent(0)); return }
        val target = splitAddress(address(prefs, campaign))
        val name = prefs.getString("player_display_name", null).orEmpty().ifBlank { "Jogador" }
        pushSheets(target.first, target.second, code(prefs, campaign), userId, name, mine, post, onResult)
    }

    /** Em silêncio, ao sair da lista de fichas: manda as fichas de todas as campanhas em que este aparelho é jogador. */
    fun pushAll(prefs: SharedPreferences, sheets: List<CharacterSheet>) {
        val userId = CampaignStore.localUserId(prefs)
        CampaignStore.load(prefs)
            .filter { it.masterUserId != userId && !it.isArchived && canPush(prefs, it) }
            .forEach { push(prefs, it, sheets) }
    }
}
