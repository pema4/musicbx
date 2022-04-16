package ru.pema4.musicbx.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.WithKoin
import ru.pema4.musicbx.model.CableEnd
import ru.pema4.musicbx.model.CableFrom
import ru.pema4.musicbx.model.CableTo
import ru.pema4.musicbx.model.DefaultPatch
import ru.pema4.musicbx.model.Module
import ru.pema4.musicbx.model.Patch
import ru.pema4.musicbx.util.Scrollable
import ru.pema4.musicbx.util.offsetByPadding

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EditorView(
    state: EditorState = rememberEditorState(DefaultPatch),
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current

    Scrollable(
        horizontalScrollState = state.horizontalScroll,
        verticalScrollState = state.verticalScroll,
        modifier = modifier
            .onPointerEvent(PointerEventType.Press) {
                if (it.buttons.isSecondaryPressed) {
                    state.draftCable = null
                }
            },
        // .background(Color.LightGray)
        // .padding(2000.dp),
        hideHorizontalScrollbarAutomatically = true,
    ) {
        Box(Modifier.fillMaxSize().background(Color.Green.copy(alpha = 0.1f)))
        Box(
            modifier = Modifier
                // .matchParentSize()
                // .scale(0.6f)
                .pointerMoveFilter(
                    onMove = { offset ->
                        state.cursorOffset = with(density) { DpOffset(x = offset.x.toDp(), y = offset.y.toDp()) }
                        false
                    }
                )
                .fillMaxSize()
        ) {
            EditorContentView(state)
        }
    }
}

@Composable
private fun EditorContentView(state: EditorState) {
    val modules = state.modules
    for (idx in modules.indices) {
        key(modules[idx].id) {
            val module by rememberUpdatedState(modules[idx])
            var rawModuleOffset by remember { mutableStateOf(module.offset) }
            Box(
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.1f))
                    // .offset(x = module.offset.x, y = module.offset.y)
                    // .padding(start = module.offset.x, top = module.offset.y)
                    .offsetByPadding(module.offset)
                    .diagonallyDraggable(module) { delta ->
                        rawModuleOffset += delta
                        module.offset = DpOffset(
                            x = rawModuleOffset.x.coerceAtLeast(0.0.dp),
                            y = rawModuleOffset.y.coerceAtLeast(0.0.dp)
                        )
                    }
            ) {
                ModuleView(
                    state = module,
                    modifier = Modifier,
                    onInputClick = { socketNumber ->
                        state.createCable(
                            to = CableTo(
                                moduleId = module.id,
                                socketNumber = socketNumber,
                            )
                        )
                    },
                    onOutputClick = { socketNumber ->
                        state.createCable(
                            from = CableFrom(
                                moduleId = module.id,
                                socketNumber = socketNumber,
                            )
                        )
                    },
                )
            }
        }
    }

    for (cable in state.cables) {
        key(cable) {
            CableView(cable)
        }
    }

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
    var cursorOffset by mutableStateOf(DpOffset.Unspecified)
    var draftCable: DraftCableState? by mutableStateOf(null)

    fun addModule() {
        val id = (modules.maxOfOrNull { it.id } ?: -1) + 1
        val newModuleState = ModuleState(
            id = id,
            name = "New module #$id",
            inputs = List(1) { number ->
                SocketState(type = SocketType.Input, number = number)
            },
            outputs = List(2) { number ->
                SocketState(type = SocketType.Output, number = number)
            }
        )
        modules += newModuleState
    }

    fun createCable(
        from: CableFrom? = null,
        to: CableTo? = null,
    ) {
        draftCable = DraftCableState(
            cursorOffsetCalculation = ::cursorOffset,
            fromCalculation = from?.let(this::getSocketOffsetCalculation) ?: draftCable?.fromCalculation,
            toCalculation = to?.let(this::getSocketOffsetCalculation) ?: draftCable?.toCalculation,
        )

        val newCable = draftCable?.toFullCableStateOrNull()
        if (newCable != null) {
            draftCable = null
            cables += newCable
        }
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
                fromCalculation = getSocketOffsetCalculation(from),
                toCalculation = getSocketOffsetCalculation(to),
            )
        }
    }

    return state
}

@Composable
fun rememberEditorState(
    basePatch: Patch = DefaultPatch,
): EditorState {
    return remember(basePatch) {
        basePatch.toEditorState()
    }
}

fun Modifier.diagonallyDraggable(
    key1: Any? = null,
    onChange: (DpOffset) -> Unit,
): Modifier =
    pointerInput(key1) {
        detectDragGestures { change, dragAmount ->
            change.consumeAllChanges()
            onChange(DpOffset(dragAmount.x.toDp(), dragAmount.y.toDp()))
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
