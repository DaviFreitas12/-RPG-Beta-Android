package com.example.rpgsombrio.ui.theme

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.core.view.WindowCompat

/** Como o app escolhe entre as paletas clara e escura. */
enum class ThemeMode(val label: String) { SYSTEM("Sistema"), LIGHT("Claro"), DARK("Escuro") }

/**
 * Estado global da paleta. As cores abaixo (Night, Panel, Brand...) consultam este objeto,
 * então qualquer tela que as use se atualiza sozinha ao trocar o modo.
 */
object RpgPalette {
    private const val PREFS = "rpg_local"
    private const val KEY = "theme_mode"

    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set
    private var systemDark by mutableStateOf(false)

    val dark: Boolean
        get() = when (mode) {
            ThemeMode.SYSTEM -> systemDark
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }

    /** Lê a preferência salva e o modo atual do sistema. Chamado uma vez ao abrir o app. */
    fun init(context: Context) {
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        mode = ThemeMode.values().firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
        val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        systemDark = night == Configuration.UI_MODE_NIGHT_YES
    }

    fun setMode(context: Context, newMode: ThemeMode) {
        mode = newMode
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, newMode.name).apply()
    }
}

private fun pick(light: Long, dark: Long): Color = Color(if (RpgPalette.dark) dark else light)

// Base neutra: fundo quente no claro, grafite no escuro. O destaque é um verde-água vivo.
internal val Night: Color get() = pick(0xFFF6F5F2, 0xFF0F1113)        // fundo
internal val Panel: Color get() = pick(0xFFFFFFFF, 0xFF181B1F)        // cartões
internal val Panel2: Color get() = pick(0xFFECEAE5, 0xFF23272D)       // superfícies elevadas, campos, pílulas
internal val Brand: Color get() = pick(0xFF0C7F73, 0xFF2DD4BF)        // botões principais
internal val Accent: Color get() = pick(0xFF0B6E64, 0xFF5EEAD4)       // títulos de seção, ícones e destaques
internal val OnBrand: Color get() = pick(0xFFFFFFFF, 0xFF04211D)      // texto sobre Brand/Accent
internal val Ink: Color get() = pick(0xFF1C1E22, 0xFFECEEF1)          // texto principal
internal val Muted: Color get() = pick(0xFF5E6571, 0xFF9AA3AE)        // texto secundário
internal val Danger: Color get() = pick(0xFFD1324A, 0xFFFF6B7A)       // dano e alertas
internal val Hairline: Color get() = pick(0x1A000000, 0x1FFFFFFF)     // contornos discretos
internal val OutlineColor: Color get() = pick(0xFFC4C8CF, 0xFF3A3F47) // bordas de campos

// Cores de apoio. Estados: Good (ok), Warn (atenção), Danger (dano). Categorias do Acervo: Coral, Violet, Amber, Sky.
// O vermelho fica reservado para dano e perigo; nenhuma categoria usa tons próximos dele.
internal val Good: Color get() = pick(0xFF15803D, 0xFF4ADE80)
internal val Warn: Color get() = pick(0xFFB45309, 0xFFFACC15)
internal val Sky: Color get() = pick(0xFF2563EB, 0xFF7AB6FF)
internal val Violet: Color get() = pick(0xFF7C3AED, 0xFFB79CFF)
internal val Coral: Color get() = pick(0xFFD9622B, 0xFFFF9A6B)
internal val Amber: Color get() = pick(0xFFB86E00, 0xFFFFC857)

/**
 * O Material usa alturas de linha fixas (24sp no corpo), que ficam enormes quando o texto
 * tem fonte menor (12-13sp). Com altura proporcional (em) o espaçamento acompanha o tamanho.
 */
private fun TextStyle.tight(factor: Float = 1.4f) = copy(lineHeight = factor.em)

private val RpgTypography: Typography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.tight(1.2f), headlineMedium = t.headlineMedium.tight(1.2f), headlineSmall = t.headlineSmall.tight(1.25f),
        titleLarge = t.titleLarge.tight(1.25f), titleMedium = t.titleMedium.tight(1.3f), titleSmall = t.titleSmall.tight(1.3f),
        bodyLarge = t.bodyLarge.tight(), bodyMedium = t.bodyMedium.tight(), bodySmall = t.bodySmall.tight(),
        labelLarge = t.labelLarge.tight(1.3f), labelMedium = t.labelMedium.tight(1.3f), labelSmall = t.labelSmall.tight(1.3f)
    )
}

@Composable
internal fun RpgTheme(content: @Composable () -> Unit) {
    val dark = RpgPalette.dark
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val scheme = base.copy(
        primary = Accent, onPrimary = OnBrand,
        secondary = Brand, onSecondary = OnBrand,
        tertiary = Accent,
        background = Night, onBackground = Ink,
        surface = Panel, onSurface = Ink,
        surfaceVariant = Panel2, onSurfaceVariant = Muted,
        surfaceTint = Color.Transparent,
        surfaceContainerLowest = Night, surfaceContainerLow = Panel, surfaceContainer = Panel,
        surfaceContainerHigh = Panel2, surfaceContainerHighest = Panel2,
        primaryContainer = Panel2, onPrimaryContainer = Ink,
        secondaryContainer = Brand.copy(alpha = .22f), onSecondaryContainer = Ink,
        outline = OutlineColor, outlineVariant = Hairline,
        error = Danger
    )
    // Ícones da barra de status e de navegação seguem o modo escolhido.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !dark
                controller.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = RpgTypography,
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
