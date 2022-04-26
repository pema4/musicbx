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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.CableFrom
import ru.pema4.musicbx.model.patch.CableTo
import ru.pema4.musicbx.model.patch.InputSocket
import ru.pema4.musicbx.model.patch.Module
import ru.pema4.musicbx.model.patch.OutputSocket
import ru.pema4.musicbx.util.explainedAs
import ru.pema4.musicbx.viewmodel.ModuleViewModelImpl
import kotlin.random.Random

@Composable
fun ModuleView(
    viewModel: ModuleViewModel,
    modifier: Modifier = Modifier,
) {
    var moduleLayoutCoordinates: LayoutCoordinates? by mutableStateOf(null)
    val isHovered by viewModel.uiState.hoverInteractionSource.collectIsHoveredAsState()

    val currentShadowElevation = Animatable(0.0f)

    LaunchedEffect(viewModel, isHovered) {
        if (isHovered) {
            currentShadowElevation.animateTo(8.0f)
        } else {
            currentShadowElevation.animateTo(0.0f)
        }
    }

    Column(
        modifier = modifier
            .requiredWidth(100.dp)
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
            .explainedAs(viewModel.model.description)
            .hoverable(viewModel.uiState.hoverInteractionSource),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Text(
                text = viewModel.model.name,
                modifier = Modifier
                    .padding(5.dp)
                    .align(Alignment.Center)
            )

            if (isHovered) {
                Text(
                    text = "\u00D7",
                    modifier = Modifier
                        .clickable { viewModel.removeModule(viewModel.id) }
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
                for (input in viewModel.inputs.reversed()) {
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
                            moduleViewModel = viewModel,
                            cableEnd = CableTo(moduleId = viewModel.id, socketNumber = input.number),
                        )
                    )
                }
            }

            Column {
                for (output in viewModel.outputs.reversed()) {
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
                            moduleViewModel = viewModel,
                            cableEnd = CableFrom(moduleId = viewModel.id, socketNumber = output.number),
                        )
                    )
                }
            }
        }
    }
}

@Stable
interface ModuleViewModel {
    val model: Module
    val uiState: ModuleState

    val uid: String get() = model.uid
    val id: Int get() = model.id

    val inputs: SnapshotStateList<SocketState>
    val outputs: SnapshotStateList<SocketState>

    fun createCable(end: CableEnd)
    fun editCable(end: CableEnd)
    fun startCablePreview(end: CableEnd)
    fun endCablePreview(end: CableEnd)
    fun removeModule(moduleId: Int)
}

@Stable
interface ModuleState {
    var offset: DpOffset
    val hoverInteractionSource: MutableInteractionSource
}

@Preview
@Composable
private fun ModuleViewPreview() {
    EditorTheme {
        val viewModel = ModuleViewModelImpl(
            module = Module(
                id = 0,
                uid = "Oscillator",
                name = "Sine Oscillator",
                inputs = listOf(
                    InputSocket(
                        number = 0,
                        name = "name",
                    )
                ),
                outputs = listOf(
                    OutputSocket(
                        number = 0,
                        name = "name 2",
                    ),
                    OutputSocket(
                        number = 1,
                        name = "name 3",
                    )
                )
            ),
        )
        ModuleView(viewModel)
    }
}

@Preview
@Composable
fun F(content: @Composable () -> Unit) {
    Box(Modifier.size(100.dp, 200.dp).background(Color.Black)) {
        Column(Modifier.size(200.dp, 150.dp).background(Color.Green)) {

        }
    }
}
