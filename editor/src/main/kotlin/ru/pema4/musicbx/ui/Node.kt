package ru.pema4.musicbx.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.model.config.NodeUid
import ru.pema4.musicbx.model.config.TestNodeDescription
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.Node
import ru.pema4.musicbx.util.tipOnHover
import ru.pema4.musicbx.viewmodel.NodeStateImpl

@Composable
fun NodeView(
    state: NodeState,
    modifier: Modifier = Modifier,
) {
    var layoutCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }

    val elevation = remember { Animatable(1.0f) }
    LaunchedEffect(state, state.expanded) {
        elevation.animateTo(if (state.expanded) 8.0f else 1.0f)
        state.id
    }

    Card(
        modifier = modifier
            .onGloballyPositioned { layoutCoordinates = it }
            .tipOnHover(state.description.summary),
        shape = RoundedCornerShape(8.dp),
        elevation = elevation.value.dp,
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = state.description.name,
                    modifier = Modifier.weight(1.0f).padding(8.dp),
                    style = MaterialTheme.typography.h6,
                )

                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { state.removeNode(state.id) }
                            .size(20.dp)
                    )
                    Icon(
                        imageVector = if (state.expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (state.expanded) "Expand" else "Collapse",
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { state.expanded = !state.expanded }
                            .size(20.dp)
                    )
                }
            }

            NodeSettingsView(
                state = state,
                parentLayoutCoordinates = layoutCoordinates,
            )
        }
    }
}

@Composable
private fun NodeSettingsView(
    state: NodeState,
    parentLayoutCoordinates: LayoutCoordinates?,
) {
    AnimatedVisibility(
        visible = state.expanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Divider()
        Column(
            modifier = Modifier
                .padding(
                    top = if (state.parameters.isEmpty()) 8.dp else 0.dp,
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp
                )
                .fillMaxWidth()
        ) {
            for (parameter in state.parameters) {
                key(parameter.parameter.name) {
                    ParameterView(parameter)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                NodeSocketsView(state.inputs, parentLayoutCoordinates)
                NodeSocketsView(state.outputs, parentLayoutCoordinates)
            }
        }
    }
}

@Composable
private fun NodeSocketsView(
    sockets: List<SocketState>,
    parentLayoutCoordinates: LayoutCoordinates?,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (socket in sockets) {
            key(socket.name) {
                val density = LocalDensity.current
                SocketView(
                    state = socket,
                    modifier = Modifier
                        .onGloballyPositioned {
                            if (parentLayoutCoordinates != null) {
                                val offset = parentLayoutCoordinates
                                    .localPositionOf(it, it.size.center.toOffset())

                                socket.offsetInNode = with(density) {
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
interface NodeState {
    val node: Node
    val description: NodeDescription
    var topStartOffset: DpOffset
    var centerStartOffset: DpOffset
    var centerEndOffset: DpOffset
    var expanded: Boolean

    val uid: NodeUid get() = node.uid
    val id: Int get() = node.id

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
    fun removeNode(nodeId: Int)
}

@Preview
@Composable
private fun NodeViewPreview() {
    EditorTheme {
        Box(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .width(IntrinsicSize.Min)
        ) {
            val viewModel = NodeStateImpl(
                node = Node(
                    id = 0,
                    uid = TestNodeDescription.uid,
                ),
                description = TestNodeDescription,
            )
            NodeView(viewModel)
        }
    }
}
