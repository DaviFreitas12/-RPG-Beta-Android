package com.example.rpgsombrio.data

import kotlin.random.Random

// Regras puras de campanha (0.10.x). Sem Android, para poderem ser testadas na JVM.

/** Papéis aceitos em [CampaignMember.role]. */
object CampaignRole {
    const val MASTER = "MASTER"
    const val PLAYER = "PLAYER"
    fun label(role: String): String = if (role == MASTER) "Mestre" else "Jogador"
}

const val DEFAULT_CAMPAIGN_NAME = "Minha campanha"

/** Quem criou a campanha é o mestre; qualquer outra pessoa é jogador. */
fun campaignRoleOf(campaign: Campaign, userId: String): String =
    if (campaign.masterUserId == userId) CampaignRole.MASTER else CampaignRole.PLAYER

/** Código de 4 dígitos, no mesmo formato do código da mesa em rede. */
fun newInviteCode(random: Random = Random.Default): String = random.nextInt(1000, 10000).toString()

/**
 * Migração (etapa 1): a campanha única das versões 0.9.x vira a primeira da lista, e quem estava usando o app vira o mestre.
 * Nome e descrição vêm das preferências antigas (podem não existir).
 */
fun legacyCampaign(
    id: String, name: String?, description: String?, systemId: String, userId: String, inviteCode: String
): Campaign = Campaign(
    id = id,
    name = name.orEmpty().trim().ifBlank { DEFAULT_CAMPAIGN_NAME },
    description = description.orEmpty().trim(),
    masterUserId = userId,
    systemId = systemId,
    inviteCode = inviteCode
)

/** A campanha ativa: a salva, se ainda existe e não está arquivada; senão a primeira não arquivada; senão nenhuma. */
fun activeCampaignOf(campaigns: List<Campaign>, savedId: String?): Campaign? =
    campaigns.firstOrNull { it.id == savedId && !it.isArchived } ?: campaigns.firstOrNull { !it.isArchived }

/** Fichas de uma campanha, ou as soltas quando [campaign] é nulo. */
data class SheetGroup(val campaign: Campaign?, val sheets: List<CharacterSheet>)

/**
 * Agrupa as fichas por campanha para a lista de personagens (etapa 3). Ordem: campanhas não arquivadas (a ativa primeiro,
 * depois por nome), arquivadas, e por último "Sem campanha". Ficha apontando para campanha que não existe mais cai em
 * "Sem campanha". Com [includeEmpty] desligado, campanhas sem fichas não aparecem; arquivadas sem fichas nunca aparecem.
 */
fun groupSheetsByCampaign(
    sheets: List<CharacterSheet>, campaigns: List<Campaign>, activeId: String? = null, includeEmpty: Boolean = true
): List<SheetGroup> {
    val known = campaigns.map { it.id }.toSet()
    val ordered = campaigns.sortedWith(
        compareBy<Campaign> { it.isArchived }.thenByDescending { it.id == activeId }.thenBy { it.name.lowercase() }
    )
    val groups = ordered.mapNotNull { c ->
        val own = sheets.filter { it.campaignId == c.id }
        if (own.isEmpty() && (!includeEmpty || c.isArchived)) null else SheetGroup(c, own)
    }
    val loose = sheets.filter { it.campaignId.isBlank() || it.campaignId !in known }
    return if (loose.isEmpty()) groups else groups + SheetGroup(null, loose)
}

/** O sistema de uma campanha só muda enquanto ela não tem personagens. */
fun canChangeCampaignSystem(campaignId: String, sheets: List<CharacterSheet>): Boolean =
    sheets.none { it.campaignId == campaignId }

/** Ao excluir uma campanha, as fichas dela voltam para "Sem campanha" (nada é apagado). */
fun releaseSheets(campaignId: String, sheets: List<CharacterSheet>): List<CharacterSheet> =
    sheets.map { if (it.campaignId == campaignId) it.copy(campaignId = "") else it }

/** Mover a ficha para essa campanha troca o sistema dela? (Tirar da campanha nunca troca.) */
fun sheetMoveChangesSystem(sheet: CharacterSheet, campaign: Campaign?): Boolean =
    campaign != null && campaign.systemId != sheet.systemId
