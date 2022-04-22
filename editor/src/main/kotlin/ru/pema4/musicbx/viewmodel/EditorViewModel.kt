package ru.pema4.musicbx.viewmodel

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.DpOffset
import ru.pema4.musicbx.model.patch.Cable
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.CableFrom
import ru.pema4.musicbx.model.patch.CableTo
import ru.pema4.musicbx.model.patch.Module
import ru.pema4.musicbx.model.patch.Patch
import ru.pema4.musicbx.service.PlaybackService
import ru.pema4.musicbx.ui.CableFromState
import ru.pema4.musicbx.ui.CableToState
import ru.pema4.musicbx.ui.DraftCableState
import ru.pema4.musicbx.ui.EditorState
import ru.pema4.musicbx.ui.EditorViewModel
import ru.pema4.musicbx.ui.FullCableState
import ru.pema4.musicbx.ui.ModuleState
import ru.pema4.musicbx.ui.toFullCableStateOrNull
import ru.pema4.musicbx.ui.toModule
import ru.pema4.musicbx.ui.toModuleState

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
        val zIndex = modules.first { it.id == end.moduleId }.id.toFloat() + 0.5f
        draftCable = DraftCableState(
            from = (end as? CableFrom)
                ?.let { it ->
                    CableFromState(
                        end = it,
                        offsetCalculation = getSocketOffsetCalculation(it),
                        zIndex = zIndex
                    )
                }
                ?: draftCable?.from,
            to = (end as? CableTo)
                ?.let {
                    CableToState(
                        end = end,
                        offsetCalculation = getSocketOffsetCalculation(end),
                        zIndex = zIndex
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

    override fun addModule(module: Module) {
        modules += module
            .copy(id = modules.maxOfOrNull { it.id + 1 } ?: 0)
            .toModuleState()
        PlaybackService.addModule(module)
    }

    override fun removeModule(moduleId: Int) {
        modules.removeAll { it.id == moduleId }
        cables.removeAll { it.to.end.moduleId == moduleId || it.from.end.moduleId == moduleId }
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
                    zIndex = modules.first { it.id == from.moduleId }.id.toFloat() + 0.5f
                ),
                to = CableToState(
                    end = to,
                    offsetCalculation = getSocketOffsetCalculation(to),
                    zIndex = modules.first { it.id == to.moduleId }.id.toFloat() + 0.5f
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
