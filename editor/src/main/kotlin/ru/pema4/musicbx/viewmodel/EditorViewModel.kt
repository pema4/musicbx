package ru.pema4.musicbx.viewmodel

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
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

@Stable
class EditorViewModelImpl(
    modules: Collection<ModuleState> = emptyList(),
    cables: Collection<FullCableState> = emptyList(),
) : EditorViewModel {
    override val uiState = EditorStateImpl()

    override val modules: SnapshotStateList<ModuleState> = modules.toMutableStateList()
    override val cables: SnapshotStateList<FullCableState> = cables.toMutableStateList()
    override var draftCable: DraftCableState? by mutableStateOf(null)

    var scalingIndex by mutableStateOf(ScalingSteps.indexOf(1.0f))
    override val scale: Float by derivedStateOf { ScalingSteps[scalingIndex] }

    override fun recreateGraphOnBackend() {
        PlaybackService.reset()

        for (module in modules) {
            PlaybackService.addModule(module.uid, module.id)
            for (parameter in module.parameters) {
                PlaybackService.setParameter(module.id, parameter.model.number, parameter.current.normalized)
            }
        }

        for (cable in cables) {
            PlaybackService.connectModules(cable.from.end, cable.to.end)
        }
    }

    override fun extractPatch(): Patch {
        return Patch(
            modules = modules.map { it.toModule() },
            cables = cables.map { (from, to) ->
                Cable(
                    from = from.end,
                    to = to.end,
                )
            },
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

        val newCable = draftCable?.toFullCableStateOrNull() ?: return
        PlaybackService.connectModules(
            from = newCable.from.end,
            to = newCable.to.end,
        )

        draftCable = null
        cables += newCable
    }

    override fun editCable(end: CableEnd) {
        val editedCable = cables
            .filter { it.from.end == end || it.to.end == end }
            .asReversed()
            .maxByOrNull { it.isHovered }
            ?.also { cables.remove(it) }
            ?: return

        PlaybackService.disconnectModules(
            from = editedCable.from.end,
            to = editedCable.to.end,
        )

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
        val id = modules
            .maxOfOrNull { it.id + 1 }
            ?.coerceAtLeast(0)
            ?: 0

        PlaybackService.addModule(module.uid, id)
        for (parameter in module.parameters) {
            PlaybackService.setParameter(
                moduleId = id,
                parameterNum = parameter.number,
                normalizedValue = parameter.kind.normalize(parameter.default),
            )
        }

        modules += ModuleStateImpl(
            module = module.copy(id = id),
            editorViewModel = this,
        )
    }

    override fun removeModule(moduleId: Int) {
        PlaybackService.removeModule(moduleId)
        modules.removeAll { it.id == moduleId }
        cables.removeAll { it.to.end.moduleId == moduleId || it.from.end.moduleId == moduleId }
    }
}

val ScalingSteps: List<Float> = listOf(
    0.25f,
    1f / 3f,
    0.50f,
    2f / 3f,
    0.75f,
    0.8f,
    0.9f,
    1.0f,
    1.1f,
    1.25f,
    1.5f,
    1.75f,
    2.0f,
    2.5f,
    3f,
    4f,
    5f,
)

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
        .associateBy(Module::id) { ModuleStateImpl(it, expanded = true, viewModel) }
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
    override var cursorOffset: DpOffset by mutableStateOf(DpOffset.Zero)
}
