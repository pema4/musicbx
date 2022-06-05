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

interface HoverTipManager {
    val activeTooltip: String?
}

val LocalHoverTipManager = compositionLocalOf<HoverTipManager?> { null }

class MutableHoverTipManager : HoverTipManager {
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
fun HoverTipManagerProvider(
    hoverTipManager: HoverTipManager? = LocalHoverTipManager.current,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalHoverTipManager provides hoverTipManager,
    ) {
        content()
    }
}

fun Modifier.pointerHoverTip(
    text: String? = null,
): Modifier = composed {
    val tooltipManager = LocalHoverTipManager.current
    if (tooltipManager !is MutableHoverTipManager) {
        return@composed this
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val activeText = LocalHoverTipManager.current?.activeTooltip
    val tooltipText by rememberUpdatedState(text ?: activeText)

    if (isHovered) {
        DisposableEffect(tooltipText) {
            tooltipManager.push(tooltipText)
            onDispose { tooltipManager.pop() }
        }
    }

    hoverable(interactionSource)
}
