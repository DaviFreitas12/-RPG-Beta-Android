package com.example.rpgsombrio.ui.characters

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.data.CharacterSheet
import com.example.rpgsombrio.data.ItemEntry
import com.example.rpgsombrio.data.SheetAttr
import com.example.rpgsombrio.data.SkillEntry
import com.example.rpgsombrio.data.SpellEntry
import com.example.rpgsombrio.data.SystemProfile
import com.example.rpgsombrio.data.fitsSystem
import com.example.rpgsombrio.data.spellLevelLabel
import com.example.rpgsombrio.data.visibleAttributes
import com.example.rpgsombrio.engine.AttackOption
import com.example.rpgsombrio.engine.armorClassBreakdown
import com.example.rpgsombrio.engine.attackRollBadge
import com.example.rpgsombrio.engine.attributeValue
import com.example.rpgsombrio.engine.characterAttacks
import com.example.rpgsombrio.engine.conditionEmoji
import com.example.rpgsombrio.engine.conditionNotes
import com.example.rpgsombrio.engine.passivePerception
import com.example.rpgsombrio.engine.playSkills
import com.example.rpgsombrio.engine.saveBonus
import com.example.rpgsombrio.engine.saveBreakdown
import com.example.rpgsombrio.engine.saveConditionNote
import com.example.rpgsombrio.engine.skillBonus
import com.example.rpgsombrio.engine.skillBreakdown
import com.example.rpgsombrio.engine.skillPoolSize
import com.example.rpgsombrio.ui.components.EmptyHint
import com.example.rpgsombrio.ui.components.InfoPill
import com.example.rpgsombrio.ui.components.replaced
import com.example.rpgsombrio.ui.components.signed
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel
import com.example.rpgsombrio.ui.theme.Panel2

/**
 * Aba "Jogar" (0.9.20): painel só de leitura com o que se consulta na mesa. Tudo já vem calculado
 * (modificadores, salvaguardas, perícias, ataques com bônus e dano) e cada item toca para rolar.
 * Para editar valores, a ficha tem a aba "Editar".
 */
internal fun LazyListScope.playTabItems(
    sheet: CharacterSheet,
    profile: SystemProfile,
    library: List<ItemEntry>,
    spells: List<SpellEntry>,
    onChange: (CharacterSheet) -> Unit,
    onRollAttr: (SheetAttr) -> Unit,
    onRollSave: (SheetAttr) -> Unit,
    onRollSkill: (SkillEntry) -> Unit,
    onRollAttack: (AttackOption) -> Unit,
    onCast: (SpellEntry) -> Unit
) {
    if (sheet.conditions.isNotEmpty()) item { ConditionsCard(sheet.conditions) }
    item { StatsCard(sheet, profile, onRollAttr) }
    if (profile.usesProficiency && !profile.isPool) item { SavesCard(sheet, profile, onRollSave) }
    if (profile.usesSkills) item { PlaySkillsCard(sheet, profile, onChange, onRollSkill) }
    if (profile.usesAttacks) item { AttacksCard(sheet, library, onRollAttack) }
    if (profile.usesSpells && spells.isNotEmpty()) item { SpellsCard(spells, onCast) }
    val equipment = equipmentLines(sheet, profile, library)
    if (equipment.isNotEmpty()) item { EquipmentCard(equipment) }
}

@Composable
private fun PlayCard(title: String, border: BorderStroke = BorderStroke(1.dp, Hairline), content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel),
        shape = RoundedCornerShape(20.dp),
        border = border
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            content()
        }
    }
}

/** O que as condições da ficha mudam nas rolagens, em frases curtas. */
@Composable
private fun ConditionsCard(conditions: List<String>) {
    val notes = conditionNotes(conditions)
    PlayCard("CONDIÇÕES", BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = .6f))) {
        Text(conditions.joinToString("   ") { "${conditionEmoji(it)} $it" }, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        notes.forEach { Text(it, color = Ink, fontSize = 13.sp) }
    }
}

/** Atributos com o modificador em destaque (o valor bruto fica pequeno) e as pílulas de uso frequente. */
@Composable
private fun StatsCard(sheet: CharacterSheet, profile: SystemProfile, onRoll: (SheetAttr) -> Unit) {
    val attrs = profile.visibleAttributes(sheet)
    val pills = buildList {
        if (profile.usesProficiency) add("Prof ${signed(sheet.proficiencyBonus)}")
        if (sheet.speed.isNotBlank()) add("Desloc. ${sheet.speed}")
        passivePerception(profile, sheet)?.let { add("Percepção passiva $it") }
        if (sheet.tempHp > 0) add("PV temp. ${sheet.tempHp}")
        if (sheet.inspiration && profile.autoProficiency) add("Inspiração")
    }
    PlayCard("ATRIBUTOS") {
        if (attrs.isEmpty()) EmptyHint("Nenhum atributo visível. Use \"Escolher atributos\" na aba Editar.")
        attrs.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { attr ->
                    val value = attributeValue(sheet.attributes, attr)
                    Surface(
                        color = Panel2, shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).clickable { onRoll(attr) }
                    ) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(attr.abbr, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(if (attr.hasModifier) signed(attr.modifierOf(value)) else "$value",
                                color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            if (attr.hasModifier) Text("$value", color = Muted, fontSize = 12.sp)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (pills.isNotEmpty()) {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                pills.forEach { InfoPill(it) }
            }
        }
    }
}

@Composable
private fun SavesCard(sheet: CharacterSheet, profile: SystemProfile, onRoll: (SheetAttr) -> Unit) {
    val attrs = profile.visibleAttributes(sheet).filter { it.hasModifier }
    if (attrs.isEmpty()) return
    PlayCard("SALVAGUARDAS") {
        attrs.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { attr ->
                    val proficient = attr.key in sheet.saveProficiencies
                    val note = saveConditionNote(sheet.conditions, attr)
                    Surface(
                        color = if (proficient) Brand.copy(alpha = .25f) else Panel2,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f).clickable { onRoll(attr) }
                    ) {
                        Column(Modifier.fillMaxWidth().padding(vertical = 7.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(attr.abbr, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(signed(saveBonus(sheet, attr)), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(saveBreakdown(sheet, attr), color = Muted, fontSize = 11.sp, maxLines = 1)
                            if (note != null) Text(note, color = MaterialTheme.colorScheme.error, fontSize = 11.sp, maxLines = 2)
                        }
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Perícias favoritas e proficientes; "Ver todas" mostra o resto, e a estrela fixa/solta uma perícia. */
@Composable
private fun PlaySkillsCard(
    sheet: CharacterSheet, profile: SystemProfile,
    onChange: (CharacterSheet) -> Unit, onRoll: (SkillEntry) -> Unit
) {
    var showAll by rememberSaveable(sheet.id) { mutableStateOf(false) }
    val rows = playSkills(sheet, showAll)
    PlayCard("PERÍCIAS") {
        if (rows.isEmpty()) {
            EmptyHint("Nenhuma perícia favorita ou proficiente. Toque em \"Ver todas\" e marque a estrela das que você usa.")
        }
        rows.forEach { (index, skill) ->
            val label = if (profile.isPool) "${skillPoolSize(profile, sheet.attributes, skill)} dados"
                else signed(skillBonus(profile, sheet.attributes, sheet.proficiencyBonus, skill))
            val detail = skillBreakdown(profile, sheet.attributes, sheet.proficiencyBonus, skill)
            Row(Modifier.fillMaxWidth().clickable { onRoll(skill) }, verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onChange(sheet.copy(skills = sheet.skills.replaced(index, skill.copy(favorite = !skill.favorite)))) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        if (skill.favorite) Icons.Default.Star else Icons.Default.StarBorder,
                        if (skill.favorite) "Tirar ${skill.name} das favoritas" else "Favoritar ${skill.name}",
                        tint = if (skill.favorite) Accent else Muted
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(skill.name, color = Ink, fontSize = 15.sp)
                    if (detail.isNotBlank()) Text(detail, color = Muted, fontSize = 12.sp)
                }
                Surface(color = if (skill.proficient) Brand.copy(alpha = .25f) else Panel2, shape = RoundedCornerShape(50)) {
                    Text(label, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
                Icon(Icons.Default.Casino, "Rolar ${skill.name}", tint = Accent, modifier = Modifier.padding(start = 10.dp, end = 4.dp))
            }
        }
        TextButton(onClick = { showAll = !showAll }) { Text(if (showAll) "Mostrar só as principais" else "Ver todas as perícias") }
    }
}

/** Ataques com bônus e dano à vista; as condições da ficha aparecem como vantagem/desvantagem. */
@Composable
private fun AttacksCard(sheet: CharacterSheet, library: List<ItemEntry>, onRoll: (AttackOption) -> Unit) {
    val attacks = characterAttacks(sheet, library)
    val badge = attackRollBadge(sheet.conditions)
    PlayCard("ATAQUES") {
        if (attacks.isEmpty()) EmptyHint("Nenhum ataque. Cadastre na aba Combate ou equipe uma arma no inventário.")
        attacks.forEach { option ->
            Surface(color = Panel2, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().clickable { onRoll(option) }) {
                Row(Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(option.label, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        val detail = listOf(option.bonus, option.damage).filter { it.isNotBlank() }.joinToString(" • ")
                        if (detail.isNotBlank()) Text(detail, color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    if (badge != null && !option.autoHit) {
                        Text(badge, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (badge == "Desvantagem") MaterialTheme.colorScheme.error else Accent,
                            modifier = Modifier.padding(end = 8.dp))
                    }
                    Icon(Icons.Default.Casino, "Rolar ${option.label}", tint = Accent, modifier = Modifier.padding(end = 4.dp))
                }
            }
        }
    }
}

@Composable
private fun SpellsCard(spells: List<SpellEntry>, onCast: (SpellEntry) -> Unit) {
    PlayCard("MAGIAS") {
        spells.forEach { spell ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(spell.name, color = Ink, fontSize = 15.sp)
                    Text(
                        listOf(spellLevelLabel(spell.level), spell.damage, spell.cost.takeIf { it.isNotBlank() }?.let { "custo $it" })
                            .filter { !it.isNullOrBlank() }.joinToString(" • "),
                        color = Muted, fontSize = 13.sp
                    )
                }
                TextButton(onClick = { onCast(spell) }) { Icon(Icons.Default.Casino, null); Spacer(Modifier.width(4.dp)); Text("Conjurar") }
            }
        }
    }
}

/** CA com a conta (armadura + escudo...) e os bônus dos itens equipados. */
private fun equipmentLines(sheet: CharacterSheet, profile: SystemProfile, library: List<ItemEntry>): List<String> = buildList {
    val ac = armorClassBreakdown(sheet, library)
    if (profile.usesArmorClass && ac.modified) add("CA ${ac.total} = ${ac.text}")
    sheet.inventory.filter { it.equipped }.forEach { entry ->
        val item = library.firstOrNull { it.id == entry.itemId && it.fitsSystem(sheet.systemId) } ?: return@forEach
        if (item.bonus.isNotBlank() && item.damage.isBlank()) add("${item.name.ifBlank { "Item" }}: ${item.bonus}")
    }
}

@Composable
private fun EquipmentCard(lines: List<String>) {
    PlayCard("EQUIPADO") { lines.forEach { Text(it, color = Ink, fontSize = 14.sp) } }
}
