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
import com.example.rpgsombrio.ui.avatar.CharacterStudioScreen
import com.example.rpgsombrio.ui.bestiary.BestiaryScreen
import com.example.rpgsombrio.ui.campaign.CampaignScreen
import com.example.rpgsombrio.ui.characters.CharacterSheetsScreen
import com.example.rpgsombrio.ui.components.MasterOnlyGate
import com.example.rpgsombrio.ui.combat.RpgEngineScreen
import com.example.rpgsombrio.ui.home.HomeScreen
import com.example.rpgsombrio.ui.items.ItemsScreen
import com.example.rpgsombrio.ui.library.LibraryHubScreen
import com.example.rpgsombrio.ui.settings.SettingsScreen
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
private val CAMPAIGN_PAGES = listOf("Sistemas")
private val HOME_PAGES = listOf("Configurações", "Studio")

@Composable
internal fun RpgApp() {
    var selected by rememberSaveable { mutableStateOf("Início") }
    BackHandler(enabled = selected in LIBRARY_PAGES) { selected = "Biblioteca" }
    BackHandler(enabled = selected in HOME_PAGES) { selected = "Início" }
    BackHandler(enabled = selected in CAMPAIGN_PAGES) { selected = "Campanha" }
    RpgTheme {
        Scaffold(
            containerColor = Night,
            bottomBar = {
                NavigationBar(containerColor = Panel) {
                    // Cinco destinos principais. Rótulos curtos (até 8 letras) para não cortar em telas estreitas;
                    // a rota (primeiro item) continua sendo o nome interno de cada tela.
                    val tabs = listOf(
                        Triple("Início", "Início", Icons.Default.Home),
                        Triple("Personagens", "Fichas", Icons.Default.Person),
                        Triple("Motor", "Mesa", Icons.Default.Casino),
                        Triple("Biblioteca", "Acervo", Icons.Default.MenuBook),
                        Triple("Campanha", "Campanha", Icons.Default.Map)
                    )
                    tabs.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = selected == route || (route == "Biblioteca" && selected in LIBRARY_PAGES) || (route == "Início" && selected in HOME_PAGES) || (route == "Campanha" && selected in CAMPAIGN_PAGES),
                            onClick = { selected = route },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label, fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Accent, selectedTextColor = Accent,
                                indicatorColor = Brand.copy(alpha = .2f),
                                unselectedIconColor = Muted,
                                unselectedTextColor = Muted
                            )
                        )
                    }
                }
            }
        ) { padding ->
            when (selected) {
                "Início" -> HomeScreen(Modifier.padding(padding), onNavigate = { selected = it })
                "Campanha" -> CampaignScreen(Modifier.padding(padding), onNavigate = { selected = it })
                "Configurações" -> SettingsScreen(Modifier.padding(padding))
                "Studio" -> CharacterStudioScreen(Modifier.padding(padding))
                "Personagens" -> CharacterSheetsScreen(Modifier.padding(padding))
                "Sistemas" -> SystemsScreen(Modifier.padding(padding))
                "Motor" -> RpgEngineScreen(Modifier.padding(padding))
                "Biblioteca" -> LibraryHubScreen(Modifier.padding(padding), onNavigate = { selected = it })
                "Bestiário" -> MasterOnlyGate(Modifier.padding(padding), "Bestiário") { BestiaryScreen(Modifier.padding(padding)) }
                "Grimório" -> SpellbookScreen(Modifier.padding(padding))
                "Traços" -> TraitsScreen(Modifier.padding(padding))
                else -> ItemsScreen(Modifier.padding(padding))
            }
        }
    }
}
