package com.example.rpgsombrio.ui.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rpgsombrio.ui.components.QuickCard
import com.example.rpgsombrio.ui.components.rememberMasterTools
import com.example.rpgsombrio.ui.theme.Accent
import com.example.rpgsombrio.ui.theme.Amber
import com.example.rpgsombrio.ui.theme.Coral
import com.example.rpgsombrio.ui.theme.Hairline
import com.example.rpgsombrio.ui.theme.Ink
import com.example.rpgsombrio.ui.theme.Muted
import com.example.rpgsombrio.ui.theme.Panel2
import com.example.rpgsombrio.ui.theme.Sky
import com.example.rpgsombrio.ui.theme.Violet

@Composable
internal fun LibraryHubScreen(modifier: Modifier = Modifier, onNavigate: (String) -> Unit) {
    // 0.10.5: o Bestiário é ferramenta do mestre; quem é jogador na campanha ativa não vê o atalho.
    val masterTools = rememberMasterTools()
    LazyColumn(
        modifier.fillMaxSize().padding(horizontal = 18.dp),
        contentPadding = PaddingValues(top = 22.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("CONTEÚDO DA MESA", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text("Acervo", color = Ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
            Text("Criaturas, magias, itens e traços da sua mesa.", color = Muted, fontSize = 13.sp)
        }
        if (masterTools) item { QuickCard(Icons.Default.Pets, "Bestiário", "Criaturas por categoria: construtos, feras, aberrações...", Coral) { onNavigate("Bestiário") } }
        item { QuickCard(Icons.Default.MenuBook, "Grimório", "Magias, poderes e efeitos", Violet) { onNavigate("Grimório") } }
        item { QuickCard(Icons.Default.Inventory2, "Itens", "Armas, armaduras, consumíveis e tesouros", Amber) { onNavigate("Itens") } }
        item { QuickCard(Icons.Default.Star, "Vantagens e desvantagens", "Traços com custo em pontos, por sistema", Sky) { onNavigate("Traços") } }
    }
}
