@file:OptIn(ExperimentalTime::class)
package ru.pema4.musicbx.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.NavigationRail
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowViaChannel
import ru.pema4.musicbx.model.InputSocket
import ru.pema4.musicbx.model.OutputSocket
import kotlin.random.Random
import kotlin.time.ExperimentalTime

@Composable
fun SocketView(
    state: SocketState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val color = when (state.type) {
        SocketType.Input -> MaterialTheme.colors.primary
        SocketType.Output -> MaterialTheme.colors.secondary
    }

    Canvas(
        modifier = modifier
            .padding(all = 5.dp)
            .size(20.dp)
            .clickableWithoutIndication { onClick() }
            .explainedAs("${state.type} socket #${state.number}"),
    ) {
        drawCircle(color = color)
        drawCircle(
            color = lerp(color, Color.Black, 0.3f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Stable
data class SocketState(
    val type: SocketType,
    val number: Int,
) {
    var offsetInModule by mutableStateOf(DpOffset.Unspecified)
}

enum class SocketType {
    Input,
    Output,
}

fun InputSocket.toSocketState(): SocketState {
    return SocketState(
        type = SocketType.Input,
        number = index,
    )
}

fun OutputSocket.toSocketState(): SocketState {
    return SocketState(
        type = SocketType.Output,
        number = index,
    )
}

fun Modifier.clickableWithoutIndication(
    onClick: () -> Unit
): Modifier = composed {
    clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

@Preview
@Composable
private fun SocketViewPreview() {
    Row {
        SocketView(state = SocketState(SocketType.Input, 0))
        Spacer(Modifier.width(10.dp))
        SocketView(state = SocketState(SocketType.Output, 0))
    }
}
