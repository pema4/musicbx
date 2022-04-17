package ru.pema4.musicbx.view

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
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
import ru.pema4.musicbx.model.Cable
import ru.pema4.musicbx.model.CableEnd
import ru.pema4.musicbx.model.CableFrom
import ru.pema4.musicbx.model.CableTo
import ru.pema4.musicbx.model.DefaultPatch
import ru.pema4.musicbx.model.Module
import ru.pema4.musicbx.model.Patch
import ru.pema4.musicbx.util.Scrollable
import ru.pema4.musicbx.util.diagonallyDraggable
import ru.pema4.musicbx.util.pointerMoveFilter

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorView(
    state: EditorState = rememberEditorState(DefaultPatch),
    modifier: Modifier = Modifier,
) {
    // Used for determining cursor location inside the scrollable area
    var parent: LayoutCoordinates? by mutableStateOf(null)
    var child: LayoutCoordinates? by mutableStateOf(null)

    Scrollable(
        horizontalScrollState = state.horizontalScroll,
        verticalScrollState = state.verticalScroll,
        modifier = modifier
            .onGloballyPositioned { parent = it }
            .onPointerEvent(PointerEventType.Press) {
                if (it.buttons.isSecondaryPressed) {
                    state.draftCable = null
                }
            }
            .pointerMoveFilter(
                onMove = { offset ->
                    val localOffset = child!!.localPositionOf(parent!!, offset)
                    state.cursorOffset = DpOffset(x = localOffset.x.toDp(), y = localOffset.y.toDp())
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
            EditorContentView(state)
        }
    }
}

@Composable
private fun EditorContentView(
    state: EditorState,
) {
    EditorModulesView(state)
    EditorCablesView(state)
    EditorDraftCableView(state)
}

@Composable
private fun EditorModulesView(state: EditorState) {
    val actionHandler = rememberModuleActionHandler(state)
    for (module in state.modules) {
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
private fun EditorCablesView(state: EditorState) {
    for (cable in state.cables) {
        key(cable.from.end, cable.to.end) {
            CableView(cable)
        }
    }
}

@Composable
private fun EditorDraftCableView(state: EditorState) {
    val draftCable = state.draftCable
    if (draftCable != null) {
        CableView(draftCable)
    }
}

@Stable
class EditorState(
    modules: Collection<ModuleState> = emptyList(),
    cables: Collection<FullCableState> = emptyList(),
    val verticalScroll: ScrollState = ScrollState(initial = 0),
    val horizontalScroll: ScrollState = ScrollState(initial = 0),
) {
    val modules = modules.toMutableStateList()
    val cables = cables.toMutableStateList()
    var cursorOffset: DpOffset by mutableStateOf(DpOffset.Unspecified)
    var draftCable: DraftCableState? by mutableStateOf(null)

    fun addModule() {
        val id = (modules.maxOfOrNull { it.id } ?: -1) + 1
        val newModuleState = ModuleState(
            id = id,
            name = "New module #$id",
            inputs = List(1) { number ->
                SocketState(type = SocketType.Input, number = number, name = "name")
            },
            outputs = List(2) { number ->
                SocketState(type = SocketType.Output, number = number, name = "name")
            }
        )
        modules += newModuleState
    }

    fun createCable(
        end: CableEnd,
    ) {
        draftCable = DraftCableState(
            from = (end as? CableFrom)
                ?.let {
                    CableFromState(
                        end = it,
                        offsetCalculation = getSocketOffsetCalculation(it),
                    )
                }
                ?: draftCable?.from,
            to = (end as? CableTo)
                ?.let {
                    CableToState(
                        end = end,
                        offsetCalculation = getSocketOffsetCalculation(end),
                    )
                }
                ?: draftCable?.to,
            cursorOffsetCalculation = ::cursorOffset,
        )

        val newCable = draftCable?.toFullCableStateOrNull()
        if (newCable != null) {
            draftCable = null
            cables += newCable
        }
    }

    fun editCable(
        end: CableEnd,
    ) {
        val editedCable = cables.run {
            val idx = indexOfLast { it.from.end == end || it.to.end == end }
            if (idx != -1) {
                removeAt(idx)
            } else {
                return
            }
        }

        draftCable = DraftCableState(
            from = editedCable.from.takeIf { it.end != end },
            to = editedCable.to.takeIf { it.end != end },
            cursorOffsetCalculation = ::cursorOffset,
        )
    }
}

private fun EditorState.getSocketOffsetCalculation(cableEnd: CableEnd): () -> DpOffset {
    val module = modules.first { it.id == cableEnd.moduleId }
    val sockets = when (cableEnd) {
        is CableTo -> module.inputs
        is CableFrom -> module.outputs
    }
    val socket = sockets.first { it.number == cableEnd.socketNumber }

    return {
        module.offset + socket.offsetInModule
    }
}

fun Patch.toEditorState(): EditorState {
    val state = EditorState()

    val moduleStatesById = modules
        .associateBy(Module::id) { it.toModuleState() }
    state.modules += moduleStatesById.values

    state.cables += cables.map { (from, to) ->
        with(state) {
            FullCableState(
                from = CableFromState(
                    end = from,
                    offsetCalculation = getSocketOffsetCalculation(from),
                ),
                to = CableToState(
                    end = to,
                    offsetCalculation = getSocketOffsetCalculation(to),
                )
            )
        }
    }

    return state
}

fun EditorState.toPatch(): Patch {
    return Patch(
        modules = modules.map { it.toModule() },
        cables = cables.map { (from, to) ->
            Cable(
                from = from.end,
                to = to.end,
            )
        }
    )
}

@Composable
fun rememberEditorState(
    basePatch: Patch = DefaultPatch,
): EditorState {
    return remember(basePatch) {
        basePatch.toEditorState()
    }
}

@Preview
@Composable
fun EditorViewPreview() {
    val state = EditorState()
    state.addModule()
    state.addModule()
    WithKoin {
        EditorView(state = state)
    }
}
