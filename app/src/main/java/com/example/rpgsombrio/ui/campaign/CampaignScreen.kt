package com.example.rpgsombrio.ui.campaign

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.Campaign
import com.example.rpgsombrio.data.CampaignRole
import com.example.rpgsombrio.data.CampaignStore
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.CreationMethod
import com.example.rpgsombrio.data.HpMethod
import com.example.rpgsombrio.data.SystemProfiles
import com.example.rpgsombrio.engine.ClientMsg
import com.example.rpgsombrio.engine.JoinResult
import com.example.rpgsombrio.engine.requestCampaign
import com.example.rpgsombrio.engine.splitAddress
import com.example.rpgsombrio.ui.combat.TableSession
import com.example.rpgsombrio.data.grantLevelUp
import com.example.rpgsombrio.data.levelRulesFor
import com.example.rpgsombrio.data.levelUpBlockReason
import com.example.rpgsombrio.data.pendingLevelUp
import com.example.rpgsombrio.data.removeLevel
import com.example.rpgsombrio.data.availableCreationMethods
import com.example.rpgsombrio.data.effectiveCreationMethod
import com.example.rpgsombrio.data.SheetStore
import com.example.rpgsombrio.data.campaignRoleOf
import com.example.rpgsombrio.data.canViewSheet
import com.example.rpgsombrio.data.asiGrantBlockReason
import com.example.rpgsombrio.data.pendingAsiGrants
import com.example.rpgsombrio.ui.characters.GrantAsiDialog
import com.example.rpgsombrio.data.canChangeCampaignSystem
import com.example.rpgsombrio.data.systemDisplayName
import com.example.rpgsombrio.data.systemOptions
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel

/** Aba Campanha (0.10.x): lista de campanhas. Quem cria a campanha é o mestre; o papel vale por campanha. */
@Composable
internal fun CampaignScreen(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("rpg_local", Context.MODE_PRIVATE) }
    val userId = remember { CampaignStore.localUserId(prefs) }
    val campaigns = remember { mutableStateListOf<Campaign>().apply { addAll(CampaignStore.load(prefs)) } }
    val sheets = remember { mutableStateListOf<CharacterSheet>().apply { addAll(SheetStore.loadCharacters(prefs)) } }
    var activeId by remember { mutableStateOf(CampaignStore.activeId(prefs)) }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by remember { mutableStateOf(false) }
    var joining by remember { mutableStateOf(false) }

    BackHandler(enabled = openId != null) { openId = null }

    val open = campaigns.firstOrNull { it.id == openId }
    if (open == null) {
        CampaignList(
            modifier, campaigns, sheets, userId, activeId, prefs,
            onOpen = { openId = it },
            onCreate = { creating = true },
            onJoin = { joining = true }
        )
    } else {
        CampaignDetail(
            modifier, open, sheets, userId, isActive = open.id == activeId, prefs = prefs,
            onBack = { openId = null },
            onUpdate = { updated ->
                val i = campaigns.indexOfFirst { it.id == updated.id }
                if (i >= 0) {
                    campaigns[i] = updated
                    CampaignStore.update(prefs, updated)
                    activeId = CampaignStore.activeId(prefs)
                }
            },
            onMakeActive = {
                CampaignStore.setActive(prefs, open.id)
                activeId = CampaignStore.activeId(prefs)
            },
            onDelete = {
                CampaignStore.delete(prefs, open.id)
                campaigns.removeAll { it.id == open.id }
                sheets.clear(); sheets.addAll(SheetStore.loadCharacters(prefs))
                activeId = CampaignStore.activeId(prefs)
                openId = null
            },
            onSheetsChange = { updated ->
                updated.forEach { u ->
                    val i = sheets.indexOfFirst { it.id == u.id }
                    if (i >= 0) sheets[i] = u
                }
                SheetStore.saveCharacters(prefs, sheets.toList())
            },
            onNavigate = onNavigate
        )
    }

    if (creating) {
        CreateCampaignDialog(
            prefs = prefs,
            defaultSystemId = SheetStore.activeSystemId(prefs),
            onCreate = { name, description, systemId ->
                creating = false
                val created = CampaignStore.create(prefs, name, description, systemId)
                campaigns.add(created)
                activeId = CampaignStore.activeId(prefs)
                openId = created.id
            },
            onDismiss = { creating = false }
        )
    }

    if (joining) {
        JoinCampaignDialog(
            prefs = prefs, userId = userId,
            onJoined = { joined ->
                joining = false
                val i = campaigns.indexOfFirst { it.id == joined.id }
                if (i >= 0) campaigns[i] = joined else campaigns.add(joined)
                activeId = CampaignStore.activeId(prefs)
                openId = joined.id
            },
            onDismiss = { joining = false }
        )
    }
}

@Composable
private fun CampaignList(
    modifier: Modifier, campaigns: List<Campaign>, sheets: List<CharacterSheet>, userId: String,
    activeId: String?, prefs: SharedPreferences, onOpen: (String) -> Unit, onCreate: () -> Unit, onJoin: () -> Unit
) {
    val visible = campaigns.filter { !it.isArchived }
        .sortedWith(compareByDescending<Campaign> { it.id == activeId }.thenBy { it.name.lowercase() })
    val archived = campaigns.filter { it.isArchived }.sortedBy { it.name.lowercase() }
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("SUAS MESAS", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Campanhas", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Quem cria a campanha é o mestre dela. Os personagens são criados dentro da campanha.", color = Muted, fontSize = 13.sp)
        }
        item {
            Button(onClick = onCreate, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Brand)) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Criar campanha")
            }
        }
        item {
            OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth()) { Text("Entrar em campanha (com código do mestre)") }
        }
        if (visible.isEmpty()) item {
            Text("Nenhuma campanha ativa. Crie uma para começar.", color = Muted, fontSize = 13.sp)
        }
        items(visible, key = { it.id }) { c ->
            CampaignCard(c, sheets.count { it.campaignId == c.id && canViewSheet(it, c, userId) }, userId, c.id == activeId, prefs) { onOpen(c.id) }
        }
        if (archived.isNotEmpty()) {
            item { Text("ARQUIVADAS", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp) }
            items(archived, key = { it.id }) { c ->
                CampaignCard(c, sheets.count { it.campaignId == c.id && canViewSheet(it, c, userId) }, userId, false, prefs) { onOpen(c.id) }
            }
        }
    }
}

@Composable
private fun CampaignCard(
    campaign: Campaign, characterCount: Int, userId: String, isActive: Boolean, prefs: SharedPreferences, onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (isActive) Accent.copy(alpha = .7f) else Hairline)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(campaign.name, color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (isActive) Text("ATIVA", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            if (campaign.description.isNotBlank()) Text(campaign.description, color = Muted, fontSize = 13.sp, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                InfoPill(CampaignRole.label(campaignRoleOf(campaign, userId)))
                InfoPill(systemDisplayName(prefs, campaign.systemId))
                InfoPill(if (characterCount == 1) "1 personagem" else "$characterCount personagens")
            }
        }
    }
}

@Composable
private fun CampaignDetail(
    modifier: Modifier, campaign: Campaign, sheets: List<CharacterSheet>, userId: String, isActive: Boolean,
    prefs: SharedPreferences, onBack: () -> Unit, onUpdate: (Campaign) -> Unit, onMakeActive: () -> Unit,
    onDelete: () -> Unit, onSheetsChange: (List<CharacterSheet>) -> Unit, onNavigate: (String) -> Unit
) {
    val role = campaignRoleOf(campaign, userId)
    val levelRules = levelRulesFor(campaign.systemId)
    val autoProficiency = remember(campaign.systemId) { SystemProfiles.resolve(prefs, campaign.systemId).autoProficiency }
    var levelMessage by remember { mutableStateOf<String?>(null) }
    var confirmAllUp by remember { mutableStateOf(false) }
    val isMaster = role == CampaignRole.MASTER
    // 0.10.5: o mestre vê todas as fichas da campanha; o jogador, só as próprias. [everyone] é o total (usado ao excluir).
    val everyone = sheets.filter { it.campaignId == campaign.id }
    val own = everyone.filter { canViewSheet(it, campaign, userId) }
    var grantTarget by remember { mutableStateOf<CharacterSheet?>(null) }
    val canChangeSystem = isMaster && canChangeCampaignSystem(campaign.id, sheets)
    val options = remember { systemOptions(prefs) }
    var editing by remember { mutableStateOf(false) }
    var systemMenuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(6.dp))
                Text("Campanhas", color = Accent)
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Hairline)) {
                Column(
                    Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Brand.copy(alpha = .38f), Panel))).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(if (campaign.isArchived) "CAMPANHA ARQUIVADA" else if (isActive) "CAMPANHA ATIVA" else "CAMPANHA", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text(campaign.name, color = Ink, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                    if (campaign.description.isNotBlank()) Text(campaign.description, color = Muted, fontSize = 13.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoPill(if (isMaster) "Você é o mestre" else "Você é jogador")
                        InfoPill(systemDisplayName(prefs, campaign.systemId))
                    }
                    if (isMaster) Button(onClick = { editing = true }, colors = ButtonDefaults.buttonColors(containerColor = Brand), modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Editar campanha")
                    }
                    if (!isActive && !campaign.isArchived) OutlinedButton(onClick = onMakeActive, modifier = Modifier.fillMaxWidth()) {
                        Text("Tornar campanha ativa")
                    }
                }
            }
        }
        item {
            DetailCard("PERSONAGENS") {
                if (own.isEmpty()) {
                    Text("Nenhum personagem nesta campanha ainda.", color = Muted, fontSize = 13.sp)
                } else own.forEach { sheet ->
                    val subtitle = listOf(
                        sheet.concept, sheet.origin,
                        if (levelRules != null) "Nível ${sheet.level}" else "",
                        if (pendingLevelUp(sheet) != null) "level up pendente" else "",
                        if (pendingAsiGrants(sheet).isNotEmpty()) "atributo extra pendente" else ""
                    ).filter { it.isNotBlank() }.joinToString(" • ")
                    Column {
                        Text(sheet.name.ifBlank { "Sem nome" }, color = Ink, fontWeight = FontWeight.SemiBold)
                        if (subtitle.isNotBlank()) Text(subtitle, color = Muted, fontSize = 12.sp)
                        // 0.10.4: o nível só muda por ação do mestre. 0.10.5: o mestre também concede atributo extra.
                        if (isMaster && levelRules != null && !campaign.isArchived) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(
                                    enabled = levelUpBlockReason(sheet, levelRules) == null,
                                    onClick = { grantLevelUp(sheet, levelRules, autoProficiency)?.let { onSheetsChange(listOf(it)) } }
                                ) { Text("Subir") }
                                TextButton(
                                    enabled = sheet.level > 1,
                                    onClick = { removeLevel(sheet, levelRules, autoProficiency)?.let { onSheetsChange(listOf(it)) } }
                                ) { Text("Remover") }
                                TextButton(
                                    enabled = asiGrantBlockReason(sheet, levelRules) == null,
                                    onClick = { grantTarget = sheet }
                                ) { Text("Atributo extra") }
                            }
                        }
                    }
                }
                if (!isMaster && everyone.size > own.size) {
                    Text("Você vê só os seus personagens. O mestre vê todos.", color = Muted, fontSize = 12.sp)
                }
                if (isMaster && levelRules != null && !campaign.isArchived && own.isNotEmpty()) {
                    OutlinedButton(onClick = { confirmAllUp = true }, modifier = Modifier.fillMaxWidth()) { Text("Subir nível de todos") }
                    Text(
                        "O jogador conclui o level up (vida e atributos) na ficha. Remover nível desfaz exatamente o que aquele nível concedeu. Atributo extra concede pontos fora dos níveis 4, 8, 12, 16 e 19 (exceções de classe). A edição livre e a reabertura da criação ficam na ficha, em Perfil.",
                        color = Muted, fontSize = 12.sp
                    )
                }
                if (!campaign.isArchived) Button(
                    onClick = {
                        prefs.edit().putString("pending_new_character_campaign", campaign.id).apply()
                        onNavigate("Personagens")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Brand), modifier = Modifier.fillMaxWidth()
                ) { Text("Criar personagem nesta campanha") }
                Text("Para trazer uma ficha solta, abra a ficha e use Mover para campanha.", color = Muted, fontSize = 12.sp)
            }
        }
        item {
            DetailCard("SISTEMA DE REGRAS") {
                Text("Os personagens criados nesta campanha nascem neste sistema.", color = Muted, fontSize = 13.sp)
                if (canChangeSystem) {
                    Box {
                        OutlinedButton(onClick = { systemMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(systemDisplayName(prefs, campaign.systemId), modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ExpandMore, contentDescription = "Selecionar sistema")
                        }
                        DropdownMenu(expanded = systemMenuExpanded, onDismissRequest = { systemMenuExpanded = false }) {
                            options.forEach { option ->
                                DropdownMenuItem(text = { Text(option.name) }, onClick = {
                                    systemMenuExpanded = false
                                    if (option.id != campaign.systemId) onUpdate(campaign.copy(systemId = option.id))
                                })
                            }
                        }
                    }
                } else {
                    Text(systemDisplayName(prefs, campaign.systemId), color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        if (!isMaster) "Só o mestre muda o sistema da campanha."
                        else "O sistema fica fixo enquanto a campanha tem personagens. Mova ou remova os personagens para trocar.",
                        color = Muted, fontSize = 12.sp
                    )
                }
                OutlinedButton(onClick = { onNavigate("Sistemas") }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Criar ou editar sistemas")
                }
            }
        }
        item {
            val methods = availableCreationMethods(campaign.systemId)
            val current = effectiveCreationMethod(campaign.systemId, campaign.creationMethod)
            DetailCard("CRIAÇÃO DE PERSONAGEM") {
                Text("Como os jogadores definem os atributos ao criar um personagem nesta campanha.", color = Muted, fontSize = 13.sp)
                if (methods.size <= 1) {
                    Text(CreationMethod.label(current), color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${systemDisplayName(prefs, campaign.systemId)} ainda não tem regra de criação própria: os atributos ficam livres.",
                        color = Muted, fontSize = 12.sp
                    )
                } else if (isMaster) {
                    methods.forEach { method ->
                        Row(
                            Modifier.fillMaxWidth().clickable { if (method != current) onUpdate(campaign.copy(creationMethod = method)) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = method == current, onClick = { if (method != current) onUpdate(campaign.copy(creationMethod = method)) })
                            Column(Modifier.weight(1f)) {
                                Text(CreationMethod.label(method), color = Ink, fontWeight = FontWeight.SemiBold)
                                Text(CreationMethod.hint(method), color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                    Text(
                        "Vale para as fichas em criação. Fichas já finalizadas continuam como estão; no método Livre os atributos destravam.",
                        color = Muted, fontSize = 12.sp
                    )
                } else {
                    Text(CreationMethod.label(current), color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(CreationMethod.hint(current), color = Muted, fontSize = 12.sp)
                    Text("Só o mestre escolhe o método de criação.", color = Muted, fontSize = 12.sp)
                }
            }
        }
        if (levelRules != null) item {
            val hp = HpMethod.effective(campaign.hpMethod)
            DetailCard("VIDA NO LEVEL UP") {
                Text("Como os personagens ganham vida ao subir de nível nesta campanha.", color = Muted, fontSize = 13.sp)
                if (isMaster) {
                    listOf(HpMethod.AVERAGE, HpMethod.ROLL).forEach { method ->
                        Row(
                            Modifier.fillMaxWidth().clickable { if (method != hp) onUpdate(campaign.copy(hpMethod = if (method == HpMethod.ROLL) HpMethod.ROLL else "")) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = method == hp, onClick = { if (method != hp) onUpdate(campaign.copy(hpMethod = if (method == HpMethod.ROLL) HpMethod.ROLL else "")) })
                            Column(Modifier.weight(1f)) {
                                Text(HpMethod.label(method), color = Ink, fontWeight = FontWeight.SemiBold)
                                Text(HpMethod.hint(method), color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    Text(HpMethod.label(hp), color = Ink, fontWeight = FontWeight.SemiBold)
                    Text(HpMethod.hint(hp), color = Muted, fontSize = 12.sp)
                    Text("Só o mestre escolhe o método.", color = Muted, fontSize = 12.sp)
                }
            }
        }
        item {
            DetailCard("MEMBROS") {
                Text("Você • ${CampaignRole.label(role)}", color = Ink, fontWeight = FontWeight.SemiBold)
                if (isMaster && !campaign.inviteCode.isNullOrBlank()) {
                    Text("Código da campanha: ${campaign.inviteCode}", color = Accent, fontWeight = FontWeight.Bold)
                }
                if (isMaster) {
                    val tick = TableSession.membersTick
                    val players = remember(campaign.id, tick) { CampaignStore.players(prefs, campaign.id) }
                    players.forEach { Text("${it.name} • Jogador", color = Ink, fontWeight = FontWeight.SemiBold) }
                    Text(
                        (if (players.isEmpty()) "Ninguém entrou ainda. " else "") +
                            (if (isActive) "Para convidar: abra a mesa na aba Mesa e passe o endereço e este código; o jogador usa Campanhas > Entrar em campanha."
                            else "Para convidar: torne esta campanha ativa, abra a mesa na aba Mesa e passe o endereço e este código."),
                        color = Muted, fontSize = 12.sp
                    )
                } else {
                    Text("Você entrou nesta campanha pelo código do mestre. Os personagens que você criar ficam nela.", color = Muted, fontSize = 12.sp)
                }
            }
        }
        if (isMaster) item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onUpdate(campaign.copy(isArchived = !campaign.isArchived)) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (campaign.isArchived) "Restaurar campanha" else "Arquivar campanha")
                }
                TextButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Excluir campanha", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (confirmAllUp && levelRules != null) {
        AlertDialog(
            onDismissRequest = { confirmAllUp = false },
            title = { Text("Subir o nível de todos?") },
            text = { Text("Cada personagem desta campanha sobe um nível e fica com o level up pendente para o jogador concluir.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmAllUp = false
                    val raised = mutableListOf<CharacterSheet>()
                    val skipped = mutableListOf<String>()
                    own.forEach { sheet ->
                        val reason = levelUpBlockReason(sheet, levelRules)
                        val next = if (reason == null) grantLevelUp(sheet, levelRules, autoProficiency) else null
                        if (next != null) raised += next
                        else skipped += "${sheet.name.ifBlank { "Sem nome" }}: ${reason ?: "não foi possível"}"
                    }
                    if (raised.isNotEmpty()) onSheetsChange(raised)
                    levelMessage = "${raised.size} personagem(ns) subiram de nível." +
                        if (skipped.isEmpty()) "" else "\n\nNão subiram:\n" + skipped.joinToString("\n") { "• $it" }
                }) { Text("Subir") }
            },
            dismissButton = { TextButton(onClick = { confirmAllUp = false }) { Text("Cancelar") } }
        )
    }
    grantTarget?.let { target ->
        GrantAsiDialog(
            sheet = target, profile = remember(target.systemId) { SystemProfiles.resolve(prefs, target.systemId) },
            onDismiss = { grantTarget = null },
            onGrant = { onSheetsChange(listOf(it)); grantTarget = null }
        )
    }
    levelMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { levelMessage = null },
            title = { Text("Nível") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { levelMessage = null }) { Text("Ok") } }
        )
    }
    if (editing) {
        var name by remember { mutableStateOf(campaign.name) }
        var description by remember { mutableStateOf(campaign.description) }
        AlertDialog(
            onDismissRequest = { editing = false },
            title = { Text("Editar campanha") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, minLines = 2)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdate(campaign.copy(name = name.trim().ifBlank { campaign.name }, description = description.trim()))
                    editing = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { editing = false }) { Text("Cancelar") } }
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Excluir ${campaign.name.ifBlank { "campanha" }}?") },
            text = {
                Text(
                    if (everyone.isEmpty()) "A campanha será removida deste aparelho."
                    else "A campanha será removida. Os ${everyone.size} personagem(ns) dela não são apagados: voltam para Sem campanha."
                )
            },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Excluir") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DetailCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Hairline)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            content()
        }
    }
}

@Composable
private fun CreateCampaignDialog(
    prefs: SharedPreferences, defaultSystemId: String,
    onCreate: (name: String, description: String, systemId: String) -> Unit, onDismiss: () -> Unit
) {
    val options = remember { systemOptions(prefs) }
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var systemId by rememberSaveable { mutableStateOf(defaultSystemId) }
    var menuExpanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova campanha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Você será o mestre desta campanha.", color = Muted, fontSize = 13.sp)
                OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true)
                OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, minLines = 2)
                Box {
                    OutlinedButton(onClick = { menuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(systemDisplayName(prefs, systemId), modifier = Modifier.weight(1f))
                        Icon(Icons.Default.ExpandMore, contentDescription = "Selecionar sistema")
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        options.forEach { option ->
                            DropdownMenuItem(text = { Text(option.name) }, onClick = { systemId = option.id; menuExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, description, systemId) }, enabled = name.isNotBlank()) { Text("Criar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/** Jogador: digita endereço e código do mestre e recebe a campanha (0.10.6). Mesma rede Wi-Fi, mesa aberta. */
@Composable
private fun JoinCampaignDialog(prefs: SharedPreferences, userId: String, onJoined: (Campaign) -> Unit, onDismiss: () -> Unit) {
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var address by remember { mutableStateOf(prefs.getString("net_host", "") ?: "") }
    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf(prefs.getString("player_display_name", "") ?: "") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Entrar em campanha") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Peça ao mestre o endereço e o código da campanha. Ele precisa ter aberto a mesa (aba Mesa) e vocês devem estar na mesma rede Wi-Fi.",
                    color = Muted, fontSize = 13.sp
                )
                OutlinedTextField(address, { address = it }, label = { Text("Endereço do mestre (ex.: 192.168.1.23)") }, singleLine = true)
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(4) }, label = { Text("Código da campanha") }, singleLine = true)
                OutlinedTextField(name, { name = it }, label = { Text("Seu nome") }, singleLine = true)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
            }
        },
        confirmButton = {
            TextButton(enabled = !busy, onClick = {
                if (address.isBlank() || code.length != 4) {
                    error = "Informe o endereço do mestre e o código de 4 dígitos."
                } else {
                    error = ""
                    busy = true
                    prefs.edit().putString("net_host", address.trim()).putString("player_display_name", name.trim()).apply()
                    val target = splitAddress(address)
                    requestCampaign(
                        host = target.first, port = target.second,
                        msg = ClientMsg.JoinCampaign(code, userId, name.trim().ifBlank { "Jogador" }),
                        post = { action -> mainHandler.post { action() } },
                        onResult = { result ->
                            busy = false
                            when (result) {
                                is JoinResult.Joined -> {
                                    val joined = CampaignStore.joinRemote(prefs, result.campaign)
                                    if (joined == null) error = "Esta campanha é sua: neste aparelho você já é o mestre dela." else onJoined(joined)
                                }
                                is JoinResult.Denied -> error = "Código errado."
                                is JoinResult.Failed -> error = result.reason
                            }
                            Unit
                        }
                    )
                }
            }) { Text(if (busy) "Entrando…" else "Entrar") }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = onDismiss) { Text("Cancelar") } }
    )
}
