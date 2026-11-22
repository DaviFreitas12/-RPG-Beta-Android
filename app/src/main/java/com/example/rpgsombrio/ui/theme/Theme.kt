package com.example.rpgsombrio.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Paleta "Meia-noite": fundo azul-ardósia profundo, superfícies suaves e um único destaque índigo.
internal val Night = Color(0xFF0E1016)       // fundo
internal val Panel = Color(0xFF171A23)       // cartões
internal val Panel2 = Color(0xFF212633)      // superfícies elevadas, campos, pílulas
internal val Brand = Color(0xFF5B6CFF)       // botões principais
internal val Accent = Color(0xFFA5B4FF)      // títulos de seção, ícones e destaques
internal val Ink = Color(0xFFEEF0F8)         // texto principal
internal val Muted = Color(0xFF9199AE)       // texto secundário
internal val Danger = Color(0xFFFF6B7A)      // dano e alertas
internal val Hairline = Color(0x1AFFFFFF)    // contornos discretos dos cartões

@Composable
internal fun RpgTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Accent, onPrimary = Night,
            secondary = Brand, onSecondary = Color.White,
            tertiary = Accent,
            background = Night, onBackground = Ink,
            surface = Panel, onSurface = Ink,
            surfaceVariant = Panel2, onSurfaceVariant = Muted,
            surfaceTint = Color.Transparent,
            surfaceContainerLowest = Night, surfaceContainerLow = Panel, surfaceContainer = Panel,
            surfaceContainerHigh = Panel2, surfaceContainerHighest = Panel2,
            primaryContainer = Panel2, onPrimaryContainer = Ink,
            secondaryContainer = Brand.copy(alpha = .35f), onSecondaryContainer = Ink,
            outline = Color(0xFF3B4256), outlineVariant = Hairline,
            error = Danger
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(28.dp)
        )
    ) {
        content()
    }
}
