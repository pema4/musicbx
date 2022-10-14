package ru.pema4.musicbx.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.collect
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.CableFrom
import ru.pema4.musicbx.model.patch.CableTo

@Composable
fun CableView(
    state: CableState
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
        state is DraftCableState -> MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
        state is FullCableState && state.isHovered -> {
            lerp(
                MaterialTheme.colors.onSurface,
                Color.Red,
                0.8f
            )
        }
        else -> MaterialTheme.colors.onSurface
    }

    val stateFrom = state.from
    if (stateFrom != null) {
        Canvas(
            modifier = Modifier
                .zIndex(stateFrom.zIndex)
                .hoverable(hoverInteractionSource)
        ) {
            val from = stateFrom.offset.let(::toOffset)
            val to = run {
                val otherEndOffset = state.to?.offset?.let(::toOffset)
                if (otherEndOffset != null) {
                    androidx.compose.ui.geometry.lerp(from, otherEndOffset, 0.5f)
                } else {
                    (state as? DraftCableState)?.cursorOffset?.let(::toOffset)!!
                }
            }

            drawLine(
                color = color,
                start = from,
                end = to,
                strokeWidth = 3.dp.toPx()
            )

            drawCircle(
                color = color,
                center = from,
                radius = 5.dp.toPx()
            )
        }
    }

    val stateTo = state.to
    if (stateTo != null) {
        Canvas(
            modifier = Modifier
                .zIndex(stateTo.zIndex)
                .hoverable(hoverInteractionSource)
        ) {
            val to = stateTo.offset.let(::toOffset)
            val from = run {
                val otherEndOffset = state.from?.offset?.let(::toOffset)
                if (otherEndOffset != null) {
                    androidx.compose.ui.geometry.lerp(to, otherEndOffset, 0.5f)
                } else {
                    (state as? DraftCableState)?.cursorOffset?.let(::toOffset)!!
                }
            }

            drawLine(
                color = color,
                start = to,
                end = from,
                strokeWidth = 3.dp.toPx()
            )

            drawCircle(
                color = color,
                center = to,
                radius = 5.dp.toPx()
            )
        }
    }
}

private fun Density.toOffset(dpOffset: DpOffset): Offset =
    Offset(dpOffset.x.toPx(), dpOffset.y.toPx())

@Stable
sealed interface CableState {
    val from: CableFromState?
    val to: CableToState?
}

@Stable
sealed interface CableEndState {
    val end: CableEnd
    val offset: DpOffset
    val zIndex: Float
}

class CableFromState(
    override val end: CableFrom,
    offsetCalculation: () -> DpOffset,
    override val zIndex: Float
) : CableEndState {
    override val offset: DpOffset by derivedStateOf(offsetCalculation)
}

class CableToState(
    override val end: CableTo,
    offsetCalculation: () -> DpOffset,
    override val zIndex: Float
) : CableEndState {
    override val offset: DpOffset by derivedStateOf(offsetCalculation)
}

@Stable
data class FullCableState(
    override val from: CableFromState,
    override val to: CableToState
) : CableState {
    var isHovered by mutableStateOf(false)
}

@Stable
class DraftCableState(
    override val from: CableFromState?,
    override val to: CableToState?,
    cursorOffsetCalculation: () -> DpOffset
) : CableState {
    val cursorOffset: DpOffset by derivedStateOf(cursorOffsetCalculation)
}

fun DraftCableState.toFullCableStateOrNull(): FullCableState? {
    return if (from != null && to != null) {
        return FullCableState(
            from = from,
            to = to
        )
    } else {
        null
    }
}
