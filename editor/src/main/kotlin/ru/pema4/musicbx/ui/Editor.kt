package ru.pema4.musicbx.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.model.config.TestNodeDescription
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.Patch
import ru.pema4.musicbx.util.Scrollable
import ru.pema4.musicbx.util.diagonallyDraggable
import ru.pema4.musicbx.util.pointerMoveFilter
import ru.pema4.musicbx.viewmodel.EditorViewModelImpl
import kotlin.math.roundToInt

interface EditorViewModel {
    val uiState: EditorState
    val nodes: Map<Int, NodeViewModel>
    val cables: List<FullCableState>
    val draftCable: DraftCableState?

    fun recreateGraphOnBackend()
    fun extractPatch(): Patch

    fun createCable(end: CableEnd)
    fun editCable(end: CableEnd)
    fun startCablePreview(end: CableEnd)
    fun endCablePreview(end: CableEnd)
    fun resetDraftCable()
    fun addNode(description: NodeDescription)
    fun removeNode(nodeId: Int)
}

interface EditorState {
    val verticalScroll: ScrollState
    val horizontalScroll: ScrollState
    var cursorOffset: DpOffset
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorView(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState

    // Used for determining cursor location inside the scrollable area
    var parent: LayoutCoordinates? by remember { mutableStateOf(null) }
    var child: LayoutCoordinates? by remember { mutableStateOf(null) }

    Scrollable(
        horizontalScrollState = uiState.horizontalScroll,
        verticalScrollState = uiState.verticalScroll,
        modifier = modifier
            .onGloballyPositioned { parent = it }
            .onPointerEvent(PointerEventType.Press) {
                if (it.buttons.isSecondaryPressed) {
                    viewModel.resetDraftCable()
                }
            }
            .pointerMoveFilter(
                onMove = { offset ->
                    val localOffset = child!!.localPositionOf(parent!!, offset)
                    uiState.cursorOffset = DpOffset(x = localOffset.x.toDp(), y = localOffset.y.toDp())
                    false
                }
            ),
        hideHorizontalScrollbarAutomatically = true
    ) {
        val scale = AppContext.preferences.zoom.scale

        ScaledLayout(scale = scale) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(0.0f, 0.0f)
                    }
                    .onGloballyPositioned { child = it }
            ) {
                EditorContentView()
            }
        }
    }
}

@Composable
private fun ScaledLayout(
    scale: Float = AppContext.preferences.zoom.scale,
    content: @Composable () -> Unit
) {
    val measurePolicy = remember(scale) {
        MeasurePolicy { measurables, constraints ->
            val placeables = measurables.map { it.measure(constraints) }
            val width = (placeables.maxOf { it.measuredWidth } * scale).roundToInt()
            val height = (placeables.maxOf { it.measuredHeight } * scale).roundToInt()
            layout(width = width, height = height) {
                for (placeable in placeables) {
                    placeable.place(IntOffset.Zero)
                }
            }
        }
    }
    Layout(content = content, measurePolicy = measurePolicy)
}

@Composable
private fun EditorContentView() {
    EditorNodesView()
    EditorCablesView()
    EditorDraftCableView()
}

@Composable
private fun EditorNodesView(
    viewModel: EditorViewModel = AppContext.editorViewModel
) {
    for ((id, nodeViewModel) in viewModel.nodes) {
        key(id) {
            var zIndex = nodeViewModel.id.toFloat()
            if (!nodeViewModel.isExpanded) {
                zIndex -= 1_000_000
            }

            Box(
                modifier = Modifier
                    .zIndex(zIndex)
                    .composed {
                        diagonallyDraggable(
                            key1 = nodeViewModel,
                            offset = nodeViewModel.topStartOffset,
                            onChange = { nodeViewModel.topStartOffset = it }
                        )
                    }
            ) {
                val density = LocalDensity.current
                NodeView(
                    viewModel = nodeViewModel,
                    modifier = Modifier
                        .widthIn(max = 150.dp)
                        .onGloballyPositioned {
                            val size = with(density) { it.size.toSize().toDpSize() }
                            nodeViewModel.centerStartOffset = size.center.copy(x = 0.dp)
                            nodeViewModel.centerEndOffset = size.center.copy(x = size.width)
                        }
                )
            }
        }
    }
}

@Composable
private fun EditorCablesView(
    viewModel: EditorViewModel = AppContext.editorViewModel
) {
    for (cable in viewModel.cables) {
        key(cable.from.end, cable.to.end) {
            CableView(cable)
        }
    }
}

@Composable
private fun EditorDraftCableView(
    viewModel: EditorViewModel = AppContext.editorViewModel
) {
    val draftCable = viewModel.draftCable
    if (draftCable != null) {
        CableView(draftCable)
    }
}

@Preview
@Composable
fun EditorViewPreview() {
    val viewModel = EditorViewModelImpl()
    viewModel.addNode(TestNodeDescription)
    viewModel.addNode(TestNodeDescription)
    EditorView(viewModel = viewModel)
}
