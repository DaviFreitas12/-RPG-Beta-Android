package com.example.rpgsombrio.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.ui.bestiary.BestiaryScreen
import com.example.rpgsombrio.ui.campaign.CampaignScreen
import com.example.rpgsombrio.ui.characters.CharacterSheetsScreen
import com.example.rpgsombrio.ui.combat.RpgEngineScreen
import com.example.rpgsombrio.ui.items.ItemsScreen
import com.example.rpgsombrio.ui.library.LibraryHubScreen
import com.example.rpgsombrio.ui.spellbook.SpellbookScreen
import com.example.rpgsombrio.ui.systems.SystemsScreen
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Brand
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Night
import com.example.rpgsombrio.ui.theme.Panel
import com.example.rpgsombrio.ui.theme.RpgTheme
import com.example.rpgsombrio.ui.traits.TraitsScreen

private val LIBRARY_PAGES = listOf("Bestiário", "Grimório", "Itens", "Traços")

@Composable
internal fun RpgApp() {
    var selected by rememberSaveable { mutableStateOf("Campanha") }
    BackHandler(enabled = selected in LIBRARY_PAGES) { selected = "Biblioteca" }
    RpgTheme {
        Scaffold(
            containerColor = Night,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    // Cinco destinos principais evitam que os itens fiquem cortados em celulares estreitos.
                    val tabs = listOf(
                        "Campanha" to Icons.Default.Home,
                        "Personagens" to Icons.Default.Person,
                        "Motor" to Icons.Default.Casino,
                        "Biblioteca" to Icons.Default.MenuBook,
                        "Sistemas" to Icons.Default.Tune
                    )
                    tabs.forEach { (label, icon) ->
                        NavigationBarItem(
                            selected = selected == label || (label == "Biblioteca" && selected in LIBRARY_PAGES),
                            onClick = { selected = label },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                indicatorColor = Brand.copy(alpha = .4f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (selected) {
                "Campanha" -> CampaignScreen(Modifier.padding(padding), onNavigate = { selected = it })
                "Personagens" -> CharacterSheetsScreen(Modifier.padding(padding))
                "Sistemas" -> SystemsScreen(Modifier.padding(padding))
                "Motor" -> RpgEngineScreen(Modifier.padding(padding))
                "Biblioteca" -> LibraryHubScreen(Modifier.padding(padding), onNavigate = { selected = it })
                "Bestiário" -> BestiaryScreen(Modifier.padding(padding))
                "Grimório" -> SpellbookScreen(Modifier.padding(padding))
                "Traços" -> TraitsScreen(Modifier.padding(padding))
                else -> ItemsScreen(Modifier.padding(padding))
            }
        }
    }
}
