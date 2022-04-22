package ru.pema4.musicbx.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ru.pema4.musicbx.WithKoin
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.CableFrom
import ru.pema4.musicbx.model.patch.CableTo
import ru.pema4.musicbx.model.patch.Module
import ru.pema4.musicbx.util.explainedAs
import ru.pema4.musicbx.util.toDpOffset
import ru.pema4.musicbx.util.toGridOffset
import kotlin.random.Random

@Composable
fun ModuleView(
    state: ModuleState,
    modifier: Modifier = Modifier,
    actionHandler: ModuleActionHandler = ModuleActionHandler(),
) {
    var moduleLayoutCoordinates: LayoutCoordinates? by mutableStateOf(null)
    val isHovered by state.hoverInteractionSource.collectIsHoveredAsState()

    val currentShadowElevation = Animatable(0.0f)

    LaunchedEffect(state, isHovered) {
        if (isHovered) {
            currentShadowElevation.animateTo(8.0f)
        } else {
            currentShadowElevation.animateTo(0.0f)
        }
    }

    Column(
        modifier = modifier
            .requiredWidth(100.dp)
            .defaultMinSize(minHeight = 100.dp)
            .graphicsLayer {
                shadowElevation = currentShadowElevation.value.dp.toPx()
                shape = RoundedCornerShape(size = 10.dp)
                clip = true
            }
            .background(Color.LightGray.copy(red = Random.nextFloat()))
            .border(
                width = 2.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(size = 10.dp),
            )
            .onGloballyPositioned { moduleLayoutCoordinates = it }
            .explainedAs(state.name)
            .hoverable(state.hoverInteractionSource),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = state.name,
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.Center)
            )

            if (isHovered) {
                Text(
                    text = "\u00D7",
                    modifier = Modifier
                        .clickable { actionHandler.onModuleRemoved(state.id) }
                        .padding(5.dp)
                        .align(Alignment.TopEnd)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            val density = LocalDensity.current

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
    fun onCableCreated(end: CableEnd)
    fun onCableEdit(end: CableEnd)
    fun onCablePreviewStart(end: CableEnd)
    fun onCablePreviewEnd(end: CableEnd)
    fun onModuleRemoved(moduleId: Int)
}

fun ModuleActionHandler(
    onCableCreated: (CableEnd) -> Unit = {},
    onCableEdit: (CableEnd) -> Unit = {},
    onCablePreviewStart: (CableEnd) -> Unit = {},
    onCablePreviewEnd: (CableEnd) -> Unit = {},
    onRemoval: (moduleId: Int) -> Unit = {},
): ModuleActionHandler {
    return object : ModuleActionHandler {
        override fun onCableCreated(end: CableEnd) = onCableCreated(end)
        override fun onCableEdit(end: CableEnd): Unit = onCableEdit(end)
        override fun onCablePreviewStart(end: CableEnd) = onCablePreviewStart(end)
        override fun onCablePreviewEnd(end: CableEnd) = onCablePreviewEnd(end)
        override fun onModuleRemoved(moduleId: Int) = onRemoval(moduleId)
    }
}

@Composable
fun rememberModuleActionHandler(state: EditorViewModel): ModuleActionHandler {
    return remember(state) {
        ModuleActionHandler(
            onCableCreated = state::createCable,
            onCableEdit = state::editCable,
            onCablePreviewStart = { previewedEnd ->
                state.cables
                    .filter { it.from.end == previewedEnd || it.to.end == previewedEnd }
                    .map { it.isHovered = true }
            },
            onCablePreviewEnd = { previewedEnd ->
                state.cables
                    .filter { it.from.end == previewedEnd || it.to.end == previewedEnd }
                    .map { it.isHovered = false }
            },
            onRemoval = state::removeModule,
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
    val inputs = inputs.toMutableStateList()
    val outputs = outputs.toMutableStateList()
    var offset: DpOffset by mutableStateOf(offset)
    var hoverInteractionSource = MutableInteractionSource()
}

fun Module.toModuleState(): ModuleState {
    return ModuleState(
        id = id,
        name = name,
        offset = offset.toDpOffset(),
        inputs = inputs.map { it.toSocketState() },
        outputs = outputs.map { it.toSocketState() },
    )
}

fun ModuleState.toModule(): Module {
    return Module(
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
    EditorMaterialTheme {
        WithKoin {
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
    }
}
