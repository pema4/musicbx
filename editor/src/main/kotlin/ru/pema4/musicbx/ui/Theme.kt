package ru.pema4.musicbx.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jthemedetecor.OsThemeDetector

private val themeDetector = OsThemeDetector.getDetector()

@Composable
fun EditorTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colors = if (isDarkTheme()) darkColors() else lightColors(),
    ) {
        content()
    }
}

@Composable
private fun isDarkTheme(): Boolean {
    var result by remember { mutableStateOf(themeDetector.isDark) }

    DisposableEffect(Unit) {
        val listener: (Boolean) -> Unit = { result = it }
        themeDetector.registerListener(listener)
        onDispose {
            themeDetector.removeListener(listener)
        }
    }

    return result
}
