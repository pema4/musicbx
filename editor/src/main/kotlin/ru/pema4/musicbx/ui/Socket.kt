package ru.pema4.musicbx.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.mouseClickable
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import ru.pema4.musicbx.WithKoin
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.InputSocket
import ru.pema4.musicbx.model.patch.OutputSocket
import ru.pema4.musicbx.util.explainedAs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SocketView(
    state: SocketState,
    modifier: Modifier = Modifier,
    actionHandler: SocketActionHandler = SocketActionHandler(),
) {
    val color = when (state.type) {
        SocketType.Input -> MaterialTheme.colors.primary
        SocketType.Output -> MaterialTheme.colors.secondary
    }

    val isHovered by state.hoverInteractionSource.collectIsHoveredAsState()
    LaunchedEffect(actionHandler) {
        snapshotFlow { isHovered }
            .onEach {
                if (it) {
                    actionHandler.startPreview()
                } else {
                    actionHandler.endPreview()
                }
            }
            .collect()
    }

    Canvas(
        modifier = modifier
            .padding(all = 5.dp)
            .size(20.dp)
            .mouseClickable {
                when {
                    keyboardModifiers.isMetaPressed -> actionHandler.editCable()
                    else -> actionHandler.createCable()
                }
            }
            .hoverable(state.hoverInteractionSource)
            .explainedAs("${state.type} socket #${state.number}"),
    ) {
        drawCircle(color = color)
        drawCircle(
            color = lerp(color, Color.Black, 0.3f),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

interface SocketActionHandler {
    fun createCable()
    fun editCable()
    fun startPreview()
    fun endPreview()
}

fun SocketActionHandler(
    createCable: () -> Unit = {},
    editCable: () -> Unit = {},
    startPreview: () -> Unit = {},
    endPreview: () -> Unit = {},
): SocketActionHandler {
    return object : SocketActionHandler {
        override fun createCable() = createCable()
        override fun editCable() = editCable()
        override fun startPreview() = startPreview()
        override fun endPreview() = endPreview()
    }
}

@Composable
fun rememberSocketActionHandler(
    moduleActionHandler: ModuleActionHandler,
    cableEnd: CableEnd,
): SocketActionHandler {
    return remember(moduleActionHandler, cableEnd) {
        SocketActionHandler(
            createCable = { moduleActionHandler.onCableCreated(cableEnd) },
            editCable = { moduleActionHandler.onCableEdit(cableEnd) },
            startPreview = { moduleActionHandler.onCablePreviewStart(cableEnd) },
            endPreview = { moduleActionHandler.onCablePreviewEnd(cableEnd) },
        )
    }
}

@Stable
data class SocketState(
    val type: SocketType,
    val number: Int,
    val name: String,
    val description: String = "description",
) {
    var offsetInModule by mutableStateOf(DpOffset.Unspecified)
    var hoverInteractionSource = MutableInteractionSource()
}

enum class SocketType {
    Input,
    Output,
}

fun InputSocket.toSocketState(): SocketState {
    return SocketState(
        type = SocketType.Input,
        number = number,
        name = name,
    )
}

fun OutputSocket.toSocketState(): SocketState {
    return SocketState(
        type = SocketType.Output,
        number = number,
        name = name,
        description = description,
    )
}

fun SocketState.toInputSocket(): InputSocket {
    require(type == SocketType.Input)
    return InputSocket(
        number = number,
        name = name,
        description = description,
    )
}

fun SocketState.toOutputSocket(): OutputSocket {
    require(type == SocketType.Output)
    return OutputSocket(
        number = number,
        name = name,
        description = description,
    )
}

@Preview
@Composable
private fun SocketViewPreview() {
    WithKoin {
        Row {
            SocketView(state = SocketState(SocketType.Input, 0, "socket"))
            Spacer(Modifier.width(10.dp))
            SocketView(state = SocketState(SocketType.Output, 0, "socket"))
        }
    }
}
