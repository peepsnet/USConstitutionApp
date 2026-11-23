package com.example.usconstitution

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

// ---- Appearance: light/dark mode and accent color ------------------------------------------

enum class ThemeMode(val label: String) {
    System("System"),
    Light("Light"),
    Dark("Dark"),
}

enum class Accent(val label: String, val swatch: Color) {
    Dynamic("Match my wallpaper", Color(0xFF7F8AA3)),
    Blue("Patriot blue", Color(0xFF1D4E89)),
    Red("Liberty red", Color(0xFFB22234)),
    Green("Evergreen", Color(0xFF2E6B3F)),
    Gold("Parchment gold", Color(0xFF7A5900)),
    Slate("Slate", Color(0xFF4A5568)),
}

data class Appearance(val mode: ThemeMode = ThemeMode.System, val accent: Accent = Accent.Blue)

/** "Match my wallpaper" needs Android 12 or newer. */
val dynamicColorSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

private const val APPEARANCE_PREFS = "appearance"

fun loadAppearance(context: Context): Appearance {
    val prefs = context.getSharedPreferences(APPEARANCE_PREFS, Context.MODE_PRIVATE)
    val mode = runCatching { ThemeMode.valueOf(prefs.getString("mode", null) ?: "") }.getOrDefault(ThemeMode.System)
    val accent = runCatching { Accent.valueOf(prefs.getString("accent", null) ?: "") }.getOrDefault(Accent.Blue)
    return Appearance(mode, if (accent == Accent.Dynamic && !dynamicColorSupported) Accent.Blue else accent)
}

fun saveAppearance(context: Context, appearance: Appearance) {
    context.getSharedPreferences(APPEARANCE_PREFS, Context.MODE_PRIVATE).edit()
        .putString("mode", appearance.mode.name)
        .putString("accent", appearance.accent.name)
        .apply()
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

fun colorSchemeFor(context: Context, appearance: Appearance, dark: Boolean): ColorScheme {
    val accent = appearance.accent
    if (accent == Accent.Dynamic && dynamicColorSupported) {
        return if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    return palette(if (accent == Accent.Dynamic) Accent.Blue else accent, dark)
}

private fun c(hex: Long) = Color(0xFF000000 or hex)

private fun light(
    p: Long, pc: Long, opc: Long, s: Long, sc: Long, osc: Long, bg: Long, sv: Long,
) = lightColorScheme(
    primary = c(p), onPrimary = Color.White, primaryContainer = c(pc), onPrimaryContainer = c(opc),
    secondary = c(s), onSecondary = Color.White, secondaryContainer = c(sc), onSecondaryContainer = c(osc),
    background = c(bg), onBackground = c(0x1B1B1F), surface = c(bg), onSurface = c(0x1B1B1F),
    surfaceVariant = c(sv), onSurfaceVariant = c(0x44474E),
    outline = c(0x74777F), outlineVariant = c(0xC4C6D0),
)

private fun dark(
    p: Long, onP: Long, pc: Long, opc: Long, s: Long, onS: Long, sc: Long, osc: Long, bg: Long, sv: Long,
) = darkColorScheme(
    primary = c(p), onPrimary = c(onP), primaryContainer = c(pc), onPrimaryContainer = c(opc),
    secondary = c(s), onSecondary = c(onS), secondaryContainer = c(sc), onSecondaryContainer = c(osc),
    background = c(bg), onBackground = c(0xE3E2E6), surface = c(bg), onSurface = c(0xE3E2E6),
    surfaceVariant = c(sv), onSurfaceVariant = c(0xC4C6D0),
    outline = c(0x8E9099), outlineVariant = c(0x44474E),
)

private fun palette(accent: Accent, dark: Boolean): ColorScheme = when (accent) {
    Accent.Red ->
        if (dark) dark(0xFFB4AB, 0x690005, 0x93000F, 0xFFDAD6, 0xABC7FF, 0x002F65, 0x00458E, 0xD7E3FF, 0x1A1110, 0x3A2B2A)
        else light(0xB22234, 0xFFDAD6, 0x410006, 0x1D4E89, 0xD7E3FF, 0x001B3E, 0xFFF8F7, 0xF5DDDA)
    Accent.Green ->
        if (dark) dark(0x9CD5A4, 0x003915, 0x145228, 0xB8F0C0, 0xF4BF48, 0x402D00, 0x5C4300, 0xFFDEA3, 0x0F1511, 0x2B332D)
        else light(0x2E6B3F, 0xB8F0C0, 0x00210C, 0x7A5900, 0xFFDEA3, 0x261900, 0xF6FBF3, 0xDDE5DB)
    Accent.Gold ->
        if (dark) dark(0xF4BF48, 0x402D00, 0x5C4300, 0xFFDEA3, 0xABC7FF, 0x002F65, 0x00458E, 0xD7E3FF, 0x17130B, 0x363024)
        else light(0x7A5900, 0xFFDEA3, 0x261900, 0x1D4E89, 0xD7E3FF, 0x001B3E, 0xFFF8F0, 0xF0E4CF)
    Accent.Slate ->
        if (dark) dark(0xBDC7D8, 0x233040, 0x3A4757, 0xDDE3EC, 0xA1CAF1, 0x003351, 0x1E4B6B, 0xCCE5FF, 0x111418, 0x2D3136)
        else light(0x4A5568, 0xDDE3EC, 0x111C2B, 0x3E6A8A, 0xCCE5FF, 0x001E32, 0xF8F9FB, 0xE0E3E8)
    else /* Blue, and the fallback */ ->
        if (dark) dark(0xABC7FF, 0x002F65, 0x00458E, 0xD7E3FF, 0xFFB4AB, 0x690005, 0x93000F, 0xFFDAD6, 0x111318, 0x2D3139)
        else light(0x1D4E89, 0xD7E3FF, 0x001B3E, 0xB22234, 0xFFDAD6, 0x410006, 0xF8F9FF, 0xE1E6F1)
}

// ---- Icons for the groups in the list ------------------------------------------------------

fun groupIcon(name: String): ImageVector = when {
    name == BOOKMARKS_GROUP -> Icons.Filled.Bookmarks
    name == "The Constitution" -> Icons.Filled.AccountBalance
    name == "The Bill of Rights" -> Icons.Filled.Shield
    name == "Later Amendments" -> Icons.Filled.HistoryEdu
    name == "American DNA" -> Icons.Filled.Flag
    name == "The First Constitution" -> Icons.Filled.History
    name == "Founding Era" -> Icons.Filled.Stars
    name == "The Federalist Papers" -> Icons.AutoMirrored.Filled.MenuBook
    name == "Presidential Addresses" -> Icons.Filled.RecordVoiceOver
    name == "Rights and Courts" -> Icons.Filled.Balance
    name == "Older Roots" -> Icons.Filled.Public
    name == "State Ratifications" -> Icons.Filled.HowToVote
    name.startsWith("Supreme Court:") -> when {
        "Power" in name -> Icons.Filled.AccountBalance
        "Race" in name -> Icons.Filled.Groups
        "Speech" in name -> Icons.Filled.Campaign
        "Criminal" in name -> Icons.Filled.Gavel
        "Privacy" in name -> Icons.Filled.Lock
        "Second" in name -> Icons.Filled.Security
        else -> Icons.Filled.Gavel
    }
    else -> Icons.Filled.Description
}
