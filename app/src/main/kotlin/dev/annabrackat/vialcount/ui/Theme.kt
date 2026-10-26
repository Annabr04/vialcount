package dev.annabrackat.vialcount.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Green = Color(0xFF1E4D3A)
private val Paper = Color(0xFFF3EFE6)
private val Ink = Color(0xFF1C1915)
private val Surface = Color(0xFFFFFDF8)

private val Scheme = lightColorScheme(
    primary = Green,
    onPrimary = Surface,
    background = Paper,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
)

@Composable
fun VialcountTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
