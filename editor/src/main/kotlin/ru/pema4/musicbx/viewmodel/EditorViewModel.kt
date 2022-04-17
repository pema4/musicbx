package ru.pema4.musicbx.viewmodel

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.DpOffset
import ru.pema4.musicbx.model.Cable
import ru.pema4.musicbx.model.CableEnd
import ru.pema4.musicbx.model.CableFrom
import ru.pema4.musicbx.model.CableTo
import ru.pema4.musicbx.model.Module
import ru.pema4.musicbx.model.Patch
import ru.pema4.musicbx.view.CableFromState
import ru.pema4.musicbx.view.CableToState
import ru.pema4.musicbx.view.DraftCableState
import ru.pema4.musicbx.view.EditorState
import ru.pema4.musicbx.view.EditorViewModel
import ru.pema4.musicbx.view.FullCableState
import ru.pema4.musicbx.view.ModuleState
import ru.pema4.musicbx.view.SocketState
import ru.pema4.musicbx.view.SocketType
import ru.pema4.musicbx.view.toFullCableStateOrNull
import ru.pema4.musicbx.view.toModule
import ru.pema4.musicbx.view.toModuleState

@Stable
class EditorViewModelImpl(
    modules: Collection<ModuleState> = emptyList(),
    cables: Collection<FullCableState> = emptyList(),
) : EditorViewModel {
    override val uiState = EditorStateImpl()

    override val modules: MutableList<ModuleState> = modules.toMutableStateList()
    override val cables: MutableList<FullCableState> = cables.toMutableStateList()
    override var draftCable: DraftCableState? by mutableStateOf(null)

    override fun extractPatch(): Patch {
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

    override fun createCable(end: CableEnd) {
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
            cursorOffsetCalculation = uiState::cursorOffset,
        )

        val newCable = draftCable?.toFullCableStateOrNull()
        if (newCable != null) {
            draftCable = null
            cables += newCable
        }
    }

    override fun editCable(end: CableEnd) {
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
            cursorOffsetCalculation = uiState::cursorOffset,
        )
    }

    override fun resetDraftCable() {
        draftCable = null
    }

    override fun addModule() {
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
}

private fun EditorViewModelImpl.getSocketOffsetCalculation(cableEnd: CableEnd): () -> DpOffset {
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

fun EditorViewModelImpl(patch: Patch): EditorViewModelImpl {
    val viewModel = EditorViewModelImpl()

    val moduleStatesById = patch.modules
        .associateBy(Module::id) { it.toModuleState() }
    viewModel.modules += moduleStatesById.values

    viewModel.cables += patch.cables.map { (from, to) ->
        with(viewModel) {
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

    return viewModel
}

@Stable
class EditorStateImpl : EditorState {
    override val verticalScroll: ScrollState = ScrollState(initial = 0)
    override val horizontalScroll: ScrollState = ScrollState(initial = 0)
    override var cursorOffset: DpOffset by mutableStateOf(DpOffset.Unspecified)
}
