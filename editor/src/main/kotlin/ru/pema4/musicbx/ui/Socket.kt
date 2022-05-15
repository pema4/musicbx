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
import ru.pema4.musicbx.model.patch.Node
import ru.pema4.musicbx.model.patch.OutputSocket
import ru.pema4.musicbx.model.patch.Socket
import ru.pema4.musicbx.util.explainedAs
import ru.pema4.musicbx.viewmodel.NodeStateImpl

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocketView(
    state: SocketState,
    modifier: Modifier = Modifier,
) {
    val color = when (state.type) {
        SocketType.Input -> MaterialTheme.colors.primary
        SocketType.Output -> MaterialTheme.colors.secondary
    }

    val isHovered by state.hoverInteractionSource.collectIsHoveredAsState()
    LaunchedEffect(state) {
        snapshotFlow { isHovered }
            .onEach {
                if (it) {
                    state.startPreview()
                } else {
                    state.endPreview()
                }
            }
            .collect()
    }

    Canvas(
        modifier = modifier
            .size(24.dp)
            .mouseClickable {
                when {
                    buttons.isSecondaryPressed -> state.edit()
                    else -> state.create()
                }
            }
            .hoverable(state.hoverInteractionSource)
            .explainedAs("${state.description}. Right Click to reassign"),
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
    val model: Socket,
    private val nodeState: NodeState,
) {
    var offsetInNode by mutableStateOf(DpOffset.Zero)
    var hoverInteractionSource = MutableInteractionSource()
    private val end = when (type) {
        SocketType.Input -> CableTo(nodeId = nodeState.id, socketNumber = number)
        SocketType.Output -> CableFrom(nodeId = nodeState.id, socketNumber = number)
    }

    val number: Int get() = model.number
    val name: String get() = model.name
    val description: String get() = model.description

    fun create() = nodeState.createCable(end)
    fun edit() = nodeState.editCable(end)
    fun startPreview() = nodeState.startCablePreview(end)
    fun endPreview() = nodeState.endCablePreview(end)
}

enum class SocketType {
    Input,
    Output,
}

fun SocketState(model: Socket, nodeState: NodeState): SocketState {
    return SocketState(
        type = when (model) {
            is InputSocket -> SocketType.Input
            is OutputSocket -> SocketType.Output
        },
        model = model,
        nodeState = nodeState
    )
}

@Preview
@Composable
private fun SocketViewPreview() {
    val node = Node(
        id = 0,
        uid = TestNodeDescription.uid,
    )
    val description = TestNodeDescription
    val nodeState = NodeStateImpl(node, description)
    Row {
        SocketView(state = SocketState(description.inputs[0], nodeState))
        Spacer(Modifier.width(10.dp))
        SocketView(state = SocketState(description.outputs[0], nodeState))
    }
}
