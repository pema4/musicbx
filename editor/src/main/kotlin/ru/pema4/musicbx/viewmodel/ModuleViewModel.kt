package ru.pema4.musicbx.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.DefaultPatch
import ru.pema4.musicbx.model.patch.Module
import ru.pema4.musicbx.service.PlaybackService
import ru.pema4.musicbx.ui.EditorViewModel
import ru.pema4.musicbx.ui.ModuleState
import ru.pema4.musicbx.ui.ParameterState
import ru.pema4.musicbx.ui.ParameterValue
import ru.pema4.musicbx.ui.SocketState
import ru.pema4.musicbx.ui.toInputSocket
import ru.pema4.musicbx.ui.toOutputSocket
import ru.pema4.musicbx.ui.toParameter
import ru.pema4.musicbx.util.toDpOffset
import ru.pema4.musicbx.util.toGridOffset

@Stable
class ModuleStateImpl(
    override val model: Module,
    offset: DpOffset = DpOffset(x = 0.dp, y = 0.dp),
    expanded: Boolean = true,
    inputs: List<SocketState> = emptyList(),
    outputs: List<SocketState> = emptyList(),
    parameters: List<ParameterState> = emptyList(),
    private val editorViewModel: EditorViewModel,
) : ModuleState {
    override var offset: DpOffset by mutableStateOf(offset)
    override var expanded by mutableStateOf(expanded)

    override val inputs = inputs.toMutableStateList()
    override val outputs = outputs.toMutableStateList()
    override val parameters = parameters.toMutableStateList()

    override fun createCable(end: CableEnd) {
        editorViewModel.createCable(end)
    }

    override fun editCable(end: CableEnd) {
        editorViewModel.editCable(end)
    }

    override fun startCablePreview(end: CableEnd) {
        editorViewModel.cables
            .filter { it.from.end == end || it.to.end == end }
            .takeLast(1)
            .map { it.isHovered = true }
    }

    override fun endCablePreview(end: CableEnd) {
        editorViewModel.cables
            .filter { it.from.end == end || it.to.end == end }
            .map { it.isHovered = false }
    }

    override fun removeModule(moduleId: Int) {
        editorViewModel.removeModule(moduleId)
    }
}

fun ModuleStateImpl(
    module: Module,
    expanded: Boolean = true,
    editorViewModel: EditorViewModel = EditorViewModelImpl(DefaultPatch),
): ModuleStateImpl {
    val state = ModuleStateImpl(
        model = module,
        offset = module.offset.toDpOffset(),
        expanded = expanded,
        editorViewModel = editorViewModel,
    )

    module.inputs.mapTo(state.inputs) { SocketState(it, state) }
    module.outputs.mapTo(state.outputs) { SocketState(it, state) }
    module.parameters.sortedBy { it.number }.mapTo(state.parameters) {
        val onChange = { normalizedValue: Float ->
            PlaybackService.setParameter(
                moduleId = module.id,
                parameterNum = it.number,
                normalizedValue
            )
        }

        ParameterState(
            model = it,
            current = ParameterValue(initial = it.current ?: it.default, kind = it.kind, onChange = onChange),
            default = ParameterValue(initial = it.default, kind = it.kind),
        )
    }

    return state
}

fun ModuleState.toModule(): Module {
    return model.copy(
        inputs = inputs.map { it.toInputSocket() },
        outputs = outputs.map { it.toOutputSocket() },
        offset = offset.toGridOffset(),
        parameters = parameters.map { it.toParameter() }
    )
}
