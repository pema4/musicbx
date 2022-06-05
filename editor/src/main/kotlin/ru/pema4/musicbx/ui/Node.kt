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
import ru.pema4.musicbx.model.config.TestNodeDescription
import ru.pema4.musicbx.model.patch.Node
import ru.pema4.musicbx.util.pointerHoverTip
import ru.pema4.musicbx.viewmodel.NodeViewModelImpl

@Stable
interface NodeViewModel {
    val model: Node
    val id: Int get() = model.id
    val description: NodeDescription

    var topStartOffset: DpOffset
    var centerStartOffset: DpOffset
    var centerEndOffset: DpOffset
    var isExpanded: Boolean

    @Stable
    val inputs: List<SocketState>

    @Stable
    val outputs: SnapshotStateList<SocketState>

    @Stable
    val parameters: SnapshotStateList<ParameterState>
}

@Composable
fun NodeView(
    viewModel: NodeViewModel,
    modifier: Modifier = Modifier,
) {
    var layoutCoordinates: LayoutCoordinates? by remember { mutableStateOf(null) }

    val elevation = remember { Animatable(1.0f) }
    LaunchedEffect(viewModel, viewModel.isExpanded) {
        elevation.animateTo(if (viewModel.isExpanded) 8.0f else 1.0f)
        viewModel.id
    }

    AppContext(nodeViewModel = viewModel) {
        Card(
            modifier = modifier
                .onGloballyPositioned { layoutCoordinates = it }
                .pointerHoverTip(viewModel.description.summary),
            shape = RoundedCornerShape(8.dp),
            elevation = elevation.value.dp,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.description.name,
                        modifier = Modifier.weight(1.0f).padding(8.dp),
                        style = MaterialTheme.typography.h6,
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(4.dp)
                    ) {
                        val app = AppContext.appViewModel
                        val editor = AppContext.editorViewModel
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    editor.removeNode(viewModel.id)
                                    app.markFileAsChanged()
                                }
                                .size(20.dp)
                        )
                        Icon(
                            imageVector = if (viewModel.isExpanded) {
                                Icons.Filled.KeyboardArrowUp
                            } else {
                                Icons.Filled.KeyboardArrowDown
                            },
                            contentDescription = if (viewModel.isExpanded) "Expand" else "Collapse",
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable { viewModel.isExpanded = !viewModel.isExpanded }
                                .size(20.dp)
                        )
                    }
                }

                NodeSettingsView(
                    viewModel = viewModel,
                    parentLayoutCoordinates = layoutCoordinates,
                )
            }
        }
    }
}

@Composable
private fun NodeSettingsView(
    viewModel: NodeViewModel = AppContext.nodeViewModel,
    parentLayoutCoordinates: LayoutCoordinates?,
) {
    AnimatedVisibility(
        visible = viewModel.isExpanded,
        enter = expandVertically(),
        exit = shrinkVertically(),
    ) {
        Divider()
        Column(
            modifier = Modifier
                .padding(
                    top = if (viewModel.parameters.isEmpty()) 8.dp else 0.dp,
                    start = 8.dp,
                    end = 8.dp,
                    bottom = 8.dp
                )
                .fillMaxWidth()
        ) {
            for (parameter in viewModel.parameters) {
                key(parameter.parameter.name) {
                    ParameterView(parameter)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                NodeSocketsView(viewModel.inputs, parentLayoutCoordinates)
                NodeSocketsView(viewModel.outputs, parentLayoutCoordinates)
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
            key(socket.model.name) {
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

@Preview
@Composable
private fun NodeViewPreview() {
    EditorTheme {
        Box(
            modifier = Modifier
                .height(IntrinsicSize.Min)
                .width(IntrinsicSize.Min)
        ) {
            val viewModel = NodeViewModelImpl(
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
