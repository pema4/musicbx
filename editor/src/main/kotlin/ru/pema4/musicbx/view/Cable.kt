package ru.pema4.musicbx.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import ru.pema4.musicbx.model.CableEnd
import ru.pema4.musicbx.model.CableFrom
import ru.pema4.musicbx.model.CableTo

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
        val from = state.fromOffset?.let(::toOffset)
        val to = state.toOffset?.let(::toOffset)

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
    val from: CableFromState?
    val to: CableToState?
    val fromOffset: DpOffset?
        get() = from?.offset
    val toOffset: DpOffset?
        get() = to?.offset
}

@Stable
sealed interface CableEndState {
    val end: CableEnd
    val offset: DpOffset
}

data class CableFromState(
    override val end: CableFrom,
    val offsetCalculation: () -> DpOffset,
) : CableEndState {
    override val offset: DpOffset by derivedStateOf(offsetCalculation)
}

data class CableToState(
    override val end: CableTo,
    val offsetCalculation: () -> DpOffset,
) : CableEndState {
    override val offset: DpOffset by derivedStateOf(offsetCalculation)
}

@Stable
data class FullCableState(
    override val from: CableFromState,
    override val to: CableToState,
) : CableState {
    override val fromOffset: DpOffset? by from::offset
    override val toOffset: DpOffset? by to::offset
    var isHovered by mutableStateOf(false)
}

@Stable
data class DraftCableState(
    override val from: CableFromState?,
    override val to: CableToState?,
    val cursorOffsetCalculation: () -> DpOffset,
) : CableState {
    val cursorOffset: DpOffset by derivedStateOf(cursorOffsetCalculation)
    override val fromOffset: DpOffset? by derivedStateOf { from?.offset }
    override val toOffset: DpOffset? by derivedStateOf { to?.offset }
}

fun DraftCableState.toFullCableStateOrNull(): FullCableState? {
    return if (from != null && to != null) {
        return FullCableState(
            from = from,
            to = to,
        )
    } else {
        null
    }
}
