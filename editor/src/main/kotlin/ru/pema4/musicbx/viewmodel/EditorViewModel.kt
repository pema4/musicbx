package ru.pema4.musicbx.viewmodel

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.unit.DpOffset
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.model.patch.Cable
import ru.pema4.musicbx.model.patch.CableEnd
import ru.pema4.musicbx.model.patch.CableFrom
import ru.pema4.musicbx.model.patch.CableTo
import ru.pema4.musicbx.model.patch.InputSocket
import ru.pema4.musicbx.model.patch.Node
import ru.pema4.musicbx.model.patch.OutputSocket
import ru.pema4.musicbx.model.patch.Patch
import ru.pema4.musicbx.service.AvailableNodesService
import ru.pema4.musicbx.service.EditorService
import ru.pema4.musicbx.ui.CableFromState
import ru.pema4.musicbx.ui.CableToState
import ru.pema4.musicbx.ui.DraftCableState
import ru.pema4.musicbx.ui.EditorState
import ru.pema4.musicbx.ui.EditorViewModel
import ru.pema4.musicbx.ui.FullCableState
import ru.pema4.musicbx.ui.NodeViewModel
import ru.pema4.musicbx.ui.toFullCableStateOrNull

@Stable
class EditorViewModelImpl(
    nodes: Collection<NodeViewModel> = emptyList(),
    cables: Collection<FullCableState> = emptyList(),
    private val editorService: EditorService = EditorService.Unspecified
) : EditorViewModel {
    override val uiState = EditorStateImpl()
    override val nodes: SnapshotStateMap<Int, NodeViewModel> = nodes
        .associateByTo(mutableStateMapOf(), NodeViewModel::id)
    override val cables: SnapshotStateList<FullCableState> = cables.toMutableStateList()
    override var draftCable: DraftCableState? by mutableStateOf(null)

    override fun recreateGraphOnBackend() {
        editorService.reset()

        for ((id, node) in nodes.entries.sortedBy { it.key }) {
            val newId = editorService.addNode(uid = node.model.uid, nodeId = id)
            for (parameter in node.parameters) {
                editorService.setParameter(newId, parameter.parameter.number, parameter.current.normalized)
            }
        }

        for (cable in cables) {
            editorService.connectNodes(cable.from.end, cable.to.end)
        }
    }

    override fun extractPatch(): Patch {
        return Patch(
            nodes = nodes.map { (_, n) -> n.toNode() },
            cables = cables.map { (from, to) ->
                Cable(
                    from = from.end,
                    to = to.end
                )
            }
        )
    }

    override fun createCable(end: CableEnd) {
        val zIndex = nodes.getValue(end.nodeId).id.toFloat() + 0.5f
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
            cursorOffsetCalculation = uiState::cursorOffset
        )

        val newCable = draftCable?.toFullCableStateOrNull() ?: return
        editorService.connectNodes(
            from = newCable.from.end,
            to = newCable.to.end
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

        editorService.disconnectNodes(
            from = editedCable.from.end,
            to = editedCable.to.end
        )

        draftCable = DraftCableState(
            from = editedCable.from.takeIf { it.end != end },
            to = editedCable.to.takeIf { it.end != end },
            cursorOffsetCalculation = uiState::cursorOffset
        )
    }

    override fun resetDraftCable() {
        draftCable = null
    }

    override fun addNode(description: NodeDescription) {
        val id = editorService.addNode(description.uid)

        for (parameter in description.parameters) {
            editorService.setParameter(
                nodeId = id,
                parameterNum = parameter.number,
                normalizedValue = parameter.kind.normalize(parameter.default)
            )
        }

        nodes[id] = NodeViewModelImpl(
            node = editorService.activePatch.value.nodes.single { it.id == id },
            description = description
        )
    }

    override fun removeNode(nodeId: Int) {
        editorService.removeNode(nodeId)
        nodes.remove(nodeId)
        cables.removeAll { it.to.end.nodeId == nodeId || it.from.end.nodeId == nodeId }
    }

    override fun startCablePreview(end: CableEnd) {
        cables
            .filter { it.from.end == end || it.to.end == end }
            .takeLast(1)
            .forEach { it.isHovered = true }
    }

    override fun endCablePreview(end: CableEnd) {
        cables
            .filter { it.from.end == end || it.to.end == end }
            .forEach { it.isHovered = false }
    }
}

private fun EditorViewModelImpl.getSocketOffsetCalculation(cableEnd: CableEnd): () -> DpOffset {
    val node = nodes.getValue(cableEnd.nodeId)
    val sockets = when (cableEnd) {
        is CableTo -> node.inputs
        is CableFrom -> node.outputs
    }
    val socket = sockets.first { it.model.name == cableEnd.socketName }

    return {
        when {
            node.isExpanded -> node.topStartOffset + socket.offsetInNode
            socket.model is InputSocket -> node.topStartOffset + node.centerStartOffset
            socket.model is OutputSocket -> node.topStartOffset + node.centerEndOffset
            else -> DpOffset.Unspecified
        }
    }
}

fun EditorViewModelImpl(
    patch: Patch,
    editorService: EditorService
): EditorViewModelImpl {
    val viewModel = EditorViewModelImpl(editorService = editorService)

    val nodeViewModelsById = patch.nodes
        .associateBy(Node::id) {
            NodeViewModelImpl(
                node = it,
                description = AvailableNodesService.Native.availableNodes.value.getValue(it.uid)
            )
        }

    viewModel.nodes += nodeViewModelsById

    viewModel.cables += patch.cables.map { (from, to) ->
        with(viewModel) {
            FullCableState(
                from = CableFromState(
                    end = from,
                    offsetCalculation = getSocketOffsetCalculation(from),
                    zIndex = nodes.getValue(from.nodeId).id.toFloat() + 0.5f
                ),
                to = CableToState(
                    end = to,
                    offsetCalculation = getSocketOffsetCalculation(to),
                    zIndex = nodes.getValue(to.nodeId).id.toFloat() + 0.5f
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
