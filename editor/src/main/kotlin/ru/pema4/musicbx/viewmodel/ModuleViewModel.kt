package ru.pema4.musicbx.viewmodel

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.DefaultPatch
import ru.pema4.musicbx.model.patch.Module
import ru.pema4.musicbx.ui.EditorViewModel
import ru.pema4.musicbx.ui.ModuleState
import ru.pema4.musicbx.ui.ModuleViewModel
import ru.pema4.musicbx.ui.SocketState
import ru.pema4.musicbx.ui.toInputSocket
import ru.pema4.musicbx.ui.toOutputSocket
import ru.pema4.musicbx.ui.toSocketState
import ru.pema4.musicbx.util.toDpOffset
import ru.pema4.musicbx.util.toGridOffset

@Stable
class ModuleViewModelImpl(
    override val model: Module,
    offset: DpOffset = DpOffset(x = 0.dp, y = 0.dp),
    inputs: List<SocketState> = emptyList(),
    outputs: List<SocketState> = emptyList(),
    private val editorViewModel: EditorViewModel,
) : ModuleViewModel {
    override val uiState: ModuleState = ModuleStateImpl(offset)
    override val inputs: SnapshotStateList<SocketState> = inputs.toMutableStateList()
    override val outputs: SnapshotStateList<SocketState> = outputs.toMutableStateList()

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

fun ModuleViewModelImpl(
    module: Module,
    editorViewModel: EditorViewModel = EditorViewModelImpl(DefaultPatch),
): ModuleViewModelImpl {
    return ModuleViewModelImpl(
        model = module,
        offset = module.offset.toDpOffset(),
        inputs = module.inputs.map { it.toSocketState() },
        outputs = module.outputs.map { it.toSocketState() },
        editorViewModel = editorViewModel,
    )
}

fun ModuleViewModel.toModule(): Module {
    return model.copy(
        inputs = inputs.map { it.toInputSocket() },
        outputs = outputs.map { it.toOutputSocket() },
        offset = uiState.offset.toGridOffset(),
    )
}

class ModuleStateImpl(offset: DpOffset = DpOffset(x = 0.dp, y = 0.dp)) : ModuleState {
    override var offset: DpOffset by mutableStateOf(offset)
    override val hoverInteractionSource = MutableInteractionSource()
}
