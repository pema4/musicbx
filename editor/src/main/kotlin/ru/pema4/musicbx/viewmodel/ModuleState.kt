package ru.pema4.musicbx.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.Node
import ru.pema4.musicbx.model.patch.Patch
import ru.pema4.musicbx.service.EditorService
import ru.pema4.musicbx.ui.EditorViewModel
import ru.pema4.musicbx.ui.NodeState
import ru.pema4.musicbx.ui.ParameterState
import ru.pema4.musicbx.ui.ParameterValue
import ru.pema4.musicbx.ui.SocketState
import ru.pema4.musicbx.util.toDpOffset
import ru.pema4.musicbx.util.toGridOffset

@Stable
class NodeStateImpl(
    override val node: Node,
    override val description: NodeDescription,
    offset: DpOffset = DpOffset(x = 0.dp, y = 0.dp),
    expanded: Boolean = true,
    inputs: List<SocketState> = emptyList(),
    outputs: List<SocketState> = emptyList(),
    parameters: List<ParameterState> = emptyList(),
    private val editorViewModel: EditorViewModel,
) : NodeState {
    override var topStartOffset: DpOffset by mutableStateOf(offset)
    override var centerStartOffset: DpOffset by mutableStateOf(DpOffset.Unspecified)
    override var centerEndOffset: DpOffset by mutableStateOf(DpOffset.Unspecified)
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

    override fun removeNode(nodeId: Int) {
        editorViewModel.removeNode(nodeId)
    }
}

fun NodeStateImpl(
    node: Node,
    description: NodeDescription,
    editorViewModel: EditorViewModel = EditorViewModelImpl(Patch.Initial),
): NodeStateImpl {
    val state = NodeStateImpl(
        node = node,
        description = description,
        offset = node.offset.toDpOffset(),
        expanded = !node.collapsed,
        editorViewModel = editorViewModel,
    )

    description.inputs.mapTo(state.inputs) { SocketState(it, state) }
    description.outputs.mapTo(state.outputs) { SocketState(it, state) }
    description.parameters.sortedBy { it.number }.mapTo(state.parameters) {
        val onChange = { normalizedValue: Float ->
            EditorService.setParameter(
                nodeId = node.id,
                parameterNum = it.number,
                normalizedValue
            )
        }

        val initialValue = node.parameterValues[it.name]
            ?.toFloat()
            ?.let(it.kind::display) // normalized
            ?: it.default // not normalized
        ParameterState(
            model = it,
            current = ParameterValue(initial = initialValue, kind = it.kind, onChange = onChange),
            default = ParameterValue(initial = it.default, kind = it.kind),
        )
    }

    return state
}

fun NodeState.toNode(): Node {
    return node.copy(
        offset = topStartOffset.toGridOffset(),
        parameterValues = parameters.associateBy(
            keySelector = { it.parameter.name },
            valueTransform = { it.current.normalized.toString() },
        ),
        collapsed = !expanded,
    )
}
