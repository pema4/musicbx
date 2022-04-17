package ru.pema4.musicbx.view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import ru.pema4.musicbx.model.CableEnd
import ru.pema4.musicbx.model.CableFrom
import ru.pema4.musicbx.model.CableTo
import ru.pema4.musicbx.util.toDpOffset
import ru.pema4.musicbx.util.toGridOffset
import kotlin.random.Random

@Composable
fun ModuleView(
    state: ModuleState,
    modifier: Modifier = Modifier,
    actionHandler: ModuleActionHandler = ModuleActionHandler(),
) {
    val density = LocalDensity.current
    var moduleLayoutCoordinates: LayoutCoordinates? by mutableStateOf(null)

    Column(
        modifier = modifier
            .requiredWidth(100.dp)
            .defaultMinSize(minHeight = 100.dp)
            .graphicsLayer {
                shadowElevation = 8.dp.toPx()
                shape = RoundedCornerShape(size = 10.dp)
                clip = true
            }
            // .shadow(elevation = 8.dp, RoundedCornerShape(size = 10.dp))
            // .clip(RoundedCornerShape(size = 10.dp))
            .background(Color.LightGray.copy(red = Random.nextFloat()))
            .border(
                width = 2.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(size = 10.dp),
            )
            .onGloballyPositioned { moduleLayoutCoordinates = it }
            .explainedAs(state.name),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = state.name,
            modifier = Modifier
                .padding(5.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            LogCompositions("ModuleView Column Row ${state.name}")
            Column {
                for (input in state.inputs.reversed()) {
                    SocketView(
                        state = input,
                        modifier = Modifier
                            .onGloballyPositioned {
                                val offset = moduleLayoutCoordinates!!
                                    .localPositionOf(it, it.size.center.toOffset())
                                input.offsetInModule = with(density) {
                                    DpOffset(x = offset.x.toDp(), y = offset.y.toDp())
                                }
                            },
                        actionHandler = rememberSocketActionHandler(
                            moduleActionHandler = actionHandler,
                            cableEnd = CableTo(moduleId = state.id, socketNumber = input.number),
                        )
                    )
                }
            }

            Column {
                for (output in state.outputs.reversed()) {
                    SocketView(
                        state = output,
                        modifier = Modifier
                            .onGloballyPositioned {
                                val offset = moduleLayoutCoordinates!!
                                    .localPositionOf(it, it.size.center.toOffset())
                                output.offsetInModule = with(density) {
                                    DpOffset(x = offset.x.toDp(), y = offset.y.toDp())
                                }
                            },
                        actionHandler = rememberSocketActionHandler(
                            moduleActionHandler = actionHandler,
                            cableEnd = CableFrom(moduleId = state.id, socketNumber = output.number),
                        )
                    )
                }
            }
        }
    }
}

@Stable
interface ModuleActionHandler {
    fun createCable(end: CableEnd)
    fun editCable(end: CableEnd)
    fun startPreviewCable(end: CableEnd)
    fun endPreviewCable(end: CableEnd)
}

fun ModuleActionHandler(
    createCable: (CableEnd) -> Unit = {},
    editCable: (CableEnd) -> Unit = {},
    startPreviewCable: (CableEnd) -> Unit = {},
    endPreviewCable: (CableEnd) -> Unit = {},
): ModuleActionHandler {
    return object : ModuleActionHandler {
        override fun createCable(end: CableEnd) = createCable(end)
        override fun editCable(end: CableEnd): Unit = editCable(end)
        override fun startPreviewCable(end: CableEnd) = startPreviewCable(end)
        override fun endPreviewCable(end: CableEnd) = endPreviewCable(end)
    }
}

@Composable
fun rememberModuleActionHandler(state: EditorViewModel): ModuleActionHandler {
    return remember(state) {
        ModuleActionHandler(
            createCable = state::createCable,
            editCable = state::editCable,
            startPreviewCable = { previewedEnd ->
                state.cables
                    .filter { it.from.end == previewedEnd || it.to.end == previewedEnd }
                    .map { it.isHovered = true }
            },
            endPreviewCable = { previewedEnd ->
                state.cables
                    .filter { it.from.end == previewedEnd || it.to.end == previewedEnd }
                    .map { it.isHovered = false }
            },
        )
    }
}

@Stable
class ModuleState(
    val id: Int,
    val name: String,
    offset: DpOffset = DpOffset(x = 0.dp, y = 0.dp),
    inputs: List<SocketState> = emptyList(),
    outputs: List<SocketState> = emptyList(),
) {
    var offset: DpOffset by mutableStateOf(offset)
    val inputs = inputs.toMutableStateList()
    val outputs = outputs.toMutableStateList()
}

fun ru.pema4.musicbx.model.Module.toModuleState(): ModuleState {
    return ModuleState(
        id = id,
        name = name,
        offset = offset.toDpOffset(),
        inputs = inputs.map { it.toSocketState() },
        outputs = outputs.map { it.toSocketState() },
    )
}

fun ModuleState.toModule(): ru.pema4.musicbx.model.Module {
    return ru.pema4.musicbx.model.Module(
        id = id,
        name = name,
        inputs = inputs.map { it.toInputSocket() },
        outputs = outputs.map { it.toOutputSocket() },
        offset = offset.toGridOffset(),
    )
}

@Preview
@Composable
private fun ModuleViewPreview() {
    val moduleState = ModuleState(
        id = 0,
        name = "Module",
        inputs = listOf(
            SocketState(
                type = SocketType.Input,
                number = 0,
                name = "name",
            ),
        ),
        outputs = mutableStateListOf(
            SocketState(
                type = SocketType.Output,
                number = 0,
                name = "name",
            ),
            SocketState(
                type = SocketType.Output,
                number = 1,
                name = "name",
            ),
        ),
    )

    ModuleView(moduleState)
}
