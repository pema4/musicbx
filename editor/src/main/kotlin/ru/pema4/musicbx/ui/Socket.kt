package ru.pema4.musicbx.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.mouseClickable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import ru.pema4.musicbx.model.config.TestNodeDescription
import ru.pema4.musicbx.model.patch.CableFrom
import ru.pema4.musicbx.model.patch.CableTo
import ru.pema4.musicbx.model.patch.InputSocket
import ru.pema4.musicbx.model.patch.OutputSocket
import ru.pema4.musicbx.model.patch.Socket
import ru.pema4.musicbx.util.pointerHoverTip

@Stable
data class SocketState(
    val model: Socket,
) {
    var offsetInNode by mutableStateOf(DpOffset.Zero)
    var hoverInteractionSource = MutableInteractionSource()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocketView(
    state: SocketState,
    modifier: Modifier = Modifier,
) {
    val nodeId = AppContext.nodeViewModel.id
    val socketEnd = when (state.model) {
        is InputSocket -> CableTo(nodeId = nodeId, socketName = state.model.name)
        is OutputSocket -> CableFrom(nodeId = nodeId, socketName = state.model.name)
    }
    val color = when (state.model) {
        is InputSocket -> MaterialTheme.colors.primary
        is OutputSocket -> MaterialTheme.colors.secondary
    }

    val editor = AppContext.editorViewModel
    val isHovered by state.hoverInteractionSource.collectIsHoveredAsState()
    LaunchedEffect(state) {
        snapshotFlow { isHovered }
            .onEach {
                if (it) {
                    editor.startCablePreview(socketEnd)
                } else {
                    editor.endCablePreview(socketEnd)
                }
            }
            .collect()
    }

    val app = AppContext.appViewModel
    Canvas(
        modifier = modifier
            .size(24.dp)
            .mouseClickable {
                when {
                    buttons.isSecondaryPressed -> editor.editCable(socketEnd)
                    else -> editor.createCable(socketEnd)
                }
                app.markFileAsChanged()
            }
            .hoverable(state.hoverInteractionSource)
            .pointerHoverTip("${state.model.description}. Left Click to connect. Right Click to disconnect"),
    ) {
        drawCircle(color = color)
        drawCircle(
            color = lerp(color, Color.Black, 0.3f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Preview
@Composable
private fun SocketViewPreview() {
    val description = TestNodeDescription
    Row {
        SocketView(state = SocketState(description.inputs[0]))
        Spacer(Modifier.width(10.dp))
        SocketView(state = SocketState(description.outputs[0]))
    }
}
