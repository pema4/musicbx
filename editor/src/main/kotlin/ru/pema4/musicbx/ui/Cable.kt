package ru.pema4.musicbx.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

@Composable
fun CableView(
    state: CableState,
    style: CableStyle = LocalCableStyle.current,
) {
    val hoverInteractionSource = remember { MutableInteractionSource() }
    val isHovered by hoverInteractionSource.collectIsHoveredAsState()

    LaunchedEffect(Unit) {
        if (state is FullCableState) {
            snapshotFlow { isHovered }
                .collect { state.isHovered = it }
        }
    }

    val color = when {
        state is DraftCableState -> style.draftColor
        state is FullCableState && state.isHovered -> style.previewColor
        else -> style.primaryColor
    }

    Canvas(
        modifier = Modifier
            .hoverable(hoverInteractionSource)

    ) {
        val cursor = (state as? DraftCableState)?.cursorOffset?.let(::toOffset)
        val from = state.from?.let(::toOffset)
        val to = state.to?.let(::toOffset)

        drawLine(
            color = color,
            start = from ?: cursor!!,
            end = to ?: cursor!!,
            strokeWidth = 3.dp.toPx(),
        )

        for (center in listOfNotNull(from, to)) {
            drawCircle(
                color = color,
                center = center,
                radius = 5.dp.toPx(),
            )
        }
    }
}

private fun Density.toOffset(dpOffset: DpOffset): Offset =
    Offset(dpOffset.x.toPx(), dpOffset.y.toPx())

@Immutable
data class CableStyle(
    val primaryColor: Color,
    val previewColor: Color,
    val draftColor: Color,
)

fun defaultCableStyle(): CableStyle {
    return CableStyle(
        primaryColor = Color.Black,
        previewColor = lerp(Color.Black, Color.Red, 0.5f),
        draftColor = Color.Black.copy(alpha = 0.5f),
    )
}

val LocalCableStyle = staticCompositionLocalOf { defaultCableStyle() }

@Stable
sealed interface CableState {
    val from: DpOffset?
    val to: DpOffset?
}

@Stable
class FullCableState(
    val fromCalculation: () -> DpOffset,
    val toCalculation: () -> DpOffset,
) : CableState {
    override val from: DpOffset? by derivedStateOf(fromCalculation)
    override val to: DpOffset? by derivedStateOf(toCalculation)
    var isHovered by mutableStateOf(false)
}

@Stable
class DraftCableState(
    val cursorOffsetCalculation: () -> DpOffset,
    val fromCalculation: (() -> DpOffset)?,
    val toCalculation: (() -> DpOffset)?,
) : CableState {
    val cursorOffset: DpOffset by derivedStateOf(cursorOffsetCalculation)
    override val from: DpOffset? by derivedStateOf { fromCalculation?.invoke() }
    override val to: DpOffset? by derivedStateOf { toCalculation?.invoke() }
}

fun DraftCableState.toFullCableStateOrNull(): FullCableState? {

    return if (fromCalculation != null && toCalculation != null) {
        return FullCableState(
            fromCalculation = fromCalculation,
            toCalculation = toCalculation
        )
    } else {
        null
    }
}
