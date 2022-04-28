package ru.pema4.musicbx.util

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

interface TooltipManager {
    val activeTooltip: String?
}

val LocalTooltipManager = compositionLocalOf<TooltipManager?> { null }

class MutableTooltipManager : TooltipManager {
    private val tooltips = mutableStateListOf<String?>()
    override val activeTooltip: String? by derivedStateOf { tooltips.lastOrNull() }

    fun push(text: String?) {
        tooltips += text
    }

    fun pop() {
        tooltips.removeLastOrNull()
    }
}

@Composable
fun InstallTooltipManager(
    tooltipManager: TooltipManager? = LocalTooltipManager.current,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalTooltipManager provides tooltipManager,
    ) {
        content()
    }
}

@Composable
fun Modifier.explainedAs(
    text: String?,
): Modifier = composed {
    val tooltipManager = LocalTooltipManager.current
    if (tooltipManager !is MutableTooltipManager) {
        return@composed this
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val tooltipText by rememberUpdatedState(text)

    if (isHovered) {
        DisposableEffect(tooltipText) {
            tooltipManager.push(tooltipText)
            onDispose { tooltipManager.pop() }
        }
    }

    hoverable(interactionSource)
}
