package ru.pema4.musicbx.view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.DpOffset
import ru.pema4.musicbx.WithKoin
import ru.pema4.musicbx.model.CableEnd
import ru.pema4.musicbx.model.Patch
import ru.pema4.musicbx.model.TestPatch
import ru.pema4.musicbx.util.Scrollable
import ru.pema4.musicbx.util.diagonallyDraggable
import ru.pema4.musicbx.util.pointerMoveFilter
import ru.pema4.musicbx.viewmodel.EditorViewModelImpl

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorView(
    viewModel: EditorViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState

    // Used for determining cursor location inside the scrollable area
    var parent: LayoutCoordinates? by mutableStateOf(null)
    var child: LayoutCoordinates? by mutableStateOf(null)

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
        hideHorizontalScrollbarAutomatically = true,
    ) {
        Box(
            modifier = Modifier
                // .graphicsLayer {
                //     scaleX = 1.8f
                //     scaleY = 1.8f
                //     transformOrigin = TransformOrigin(0.0f, 0.0f)
                // }
                .onGloballyPositioned { child = it }
        ) {
            EditorContentView(viewModel)
        }
    }
}

@Composable
private fun EditorContentView(
    viewModel: EditorViewModel,
) {
    EditorModulesView(viewModel)
    EditorCablesView(viewModel)
    EditorDraftCableView(viewModel)
}

@Composable
private fun EditorModulesView(viewModel: EditorViewModel) {
    val actionHandler = rememberModuleActionHandler(viewModel)
    for (module in viewModel.modules) {
        key(module.id) {
            Box(
                modifier = Modifier
                    .composed {
                        diagonallyDraggable(
                            key1 = module,
                            offset = module.offset,
                            onChange = { module.offset = it }
                        )
                    }
            ) {
                ModuleView(
                    state = module,
                    modifier = Modifier,
                    actionHandler = actionHandler,
                )
            }
        }
    }
}

@Composable
private fun EditorCablesView(viewModel: EditorViewModel) {
    for (cable in viewModel.cables) {
        key(cable.from.end, cable.to.end) {
            CableView(cable)
        }
    }
}

@Composable
private fun EditorDraftCableView(viewModel: EditorViewModel) {
    val draftCable = viewModel.draftCable
    if (draftCable != null) {
        CableView(draftCable)
    }
}

interface EditorViewModel {
    val uiState: EditorState
    val modules: List<ModuleState>
    val cables: List<FullCableState>
    val draftCable: DraftCableState?

    fun extractPatch(): Patch

    fun createCable(end: CableEnd)
    fun editCable(end: CableEnd)
    fun resetDraftCable()
    fun addModule(module: ru.pema4.musicbx.model.Module)
    fun removeModule(moduleId: Int)
}

interface EditorState {
    val verticalScroll: ScrollState
    val horizontalScroll: ScrollState
    var cursorOffset: DpOffset
}

@Preview
@Composable
fun EditorViewPreview() {
    val viewModel = EditorViewModelImpl()
    viewModel.addModule(TestPatch.modules[0])
    viewModel.addModule(TestPatch.modules[1])
    WithKoin {
        EditorView(viewModel = viewModel)
    }
}
