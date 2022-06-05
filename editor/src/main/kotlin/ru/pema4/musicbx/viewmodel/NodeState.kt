package ru.pema4.musicbx.viewmodel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.model.patch.Node
import ru.pema4.musicbx.service.EditorService
import ru.pema4.musicbx.ui.NodeViewModel
import ru.pema4.musicbx.ui.ParameterState
import ru.pema4.musicbx.ui.ParameterValue
import ru.pema4.musicbx.ui.SocketState
import ru.pema4.musicbx.util.toDpOffset
import ru.pema4.musicbx.util.toGridOffset

@Stable
class NodeViewModelImpl(
    override val model: Node,
    override val description: NodeDescription,
    offset: DpOffset = DpOffset(x = 0.dp, y = 0.dp),
    expanded: Boolean = true,
    inputs: List<SocketState> = emptyList(),
    outputs: List<SocketState> = emptyList(),
    parameters: List<ParameterState> = emptyList(),
) : NodeViewModel {
    override var topStartOffset: DpOffset by mutableStateOf(offset)
    override var centerStartOffset: DpOffset by mutableStateOf(DpOffset.Unspecified)
    override var centerEndOffset: DpOffset by mutableStateOf(DpOffset.Unspecified)
    override var isExpanded by mutableStateOf(expanded)

    override val inputs = inputs.toMutableStateList()
    override val outputs = outputs.toMutableStateList()
    override val parameters = parameters.toMutableStateList()
}

fun NodeViewModelImpl(
    node: Node,
    description: NodeDescription,
): NodeViewModelImpl {
    val state = NodeViewModelImpl(
        model = node,
        description = description,
        offset = node.offset.toDpOffset(),
        expanded = !node.collapsed,
    )

    description.inputs.mapTo(state.inputs) { SocketState(it) }
    description.outputs.mapTo(state.outputs) { SocketState(it) }
    description.parameters.sortedBy { it.number }.mapTo(state.parameters) {
        val onChange = { normalizedValue: Float ->
            EditorService.Native.setParameter(
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

fun NodeViewModel.toNode(): Node {
    return model.copy(
        offset = topStartOffset.toGridOffset(),
        parameterValues = parameters.associateBy(
            keySelector = { it.parameter.name },
            valueTransform = { it.current.normalized.toString() },
        ),
        collapsed = !isExpanded,
    )
}
