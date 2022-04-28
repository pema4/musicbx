package ru.pema4.musicbx.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.InputSocket
import ru.pema4.musicbx.model.patch.Module
import ru.pema4.musicbx.model.patch.OutputSocket
import ru.pema4.musicbx.util.explainedAs
import ru.pema4.musicbx.viewmodel.ModuleStateImpl

@Composable
fun ModuleView(
    state: ModuleState,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    var layoutCoordinates: LayoutCoordinates? by mutableStateOf(null)

    val elevation = remember { Animatable(1.0f) }
    LaunchedEffect(state, state.expanded) {
        elevation.animateTo(if (state.expanded) 8.0f else 1.0f)
        state.id
    }

    Card(
        modifier = modifier
            .onGloballyPositioned { layoutCoordinates = it }
            .explainedAs(state.model.description),
        shape = RoundedCornerShape(8.dp),
        elevation = elevation.value.dp,
    ) {
        Column(
            modifier = Modifier
                .clickable(enabled = onClick != null) { onClick?.invoke() }
        ) {
            Text(
                text = state.model.name,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.h6,
            )

            if (enabled) {
                Divider()
                EnabledModuleSettings(
                    state = state,
                    parentLayoutCoordinates = layoutCoordinates,
                )
            }
        }
    }
}

@Composable
private fun EnabledModuleSettings(
    state: ModuleState,
    parentLayoutCoordinates: LayoutCoordinates?,
) {
    Row(
        modifier = Modifier.padding(4.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(
            imageVector = Icons.Filled.Close,
            contentDescription = "Close",
            modifier = Modifier
                .clip(CircleShape)
                .clickable { state.removeModule(state.id) }
                .size(24.dp)
        )

        Icon(
            imageVector = if (state.expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            contentDescription = if (state.expanded) "Expand" else "Collapse",
            modifier = Modifier
                .clip(CircleShape)
                .clickable { state.expanded = !state.expanded }
                .size(24.dp)
        )
    }

    AnimatedVisibility(
        visible = state.expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Column(
            modifier = Modifier
                .padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                .fillMaxWidth()
        ) {
            for (parameter in state.parameters) {
                key(parameter.model.name) {
                    Parameter(parameter)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                ModuleSocketsView(state.inputs, parentLayoutCoordinates)
                ModuleSocketsView(state.outputs, parentLayoutCoordinates)
            }
        }
    }
}

@Composable
private fun ModuleSocketsView(
    sockets: List<SocketState>,
    parentLayoutCoordinates: LayoutCoordinates?,
) {
    val density = LocalDensity.current

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (socket in sockets) {
            key(socket.number) {
                SocketView(
                    state = socket,
                    modifier = Modifier
                        .onGloballyPositioned {
                            if (parentLayoutCoordinates != null) {
                                val offset = parentLayoutCoordinates
                                    .localPositionOf(it, it.size.center.toOffset())

                                socket.offsetInModule = with(density) {
                                    DpOffset(x = offset.x.toDp(), y = offset.y.toDp())
                                }
                            }
                        },
                )
            }
        }
    }
}

@Stable
interface ModuleState {
    val model: Module
    var offset: DpOffset
    var expanded: Boolean

    val uid: String get() = model.uid
    val id: Int get() = model.id

    @Stable
    val inputs: List<SocketState>

    @Stable
    val outputs: SnapshotStateList<SocketState>

    @Stable
    val parameters: SnapshotStateList<ParameterState>

    fun createCable(end: CableEnd)
    fun editCable(end: CableEnd)
    fun startCablePreview(end: CableEnd)
    fun endCablePreview(end: CableEnd)
    fun removeModule(moduleId: Int)
}

@Preview
@Composable
private fun ModuleViewPreview() {
    EditorTheme {
        val viewModel = ModuleStateImpl(
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
