package ru.pema4.musicbx.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pema4.musicbx.model.config.NodeUid
import ru.pema4.musicbx.model.patch.CableFrom
import ru.pema4.musicbx.model.patch.CableTo
import ru.pema4.musicbx.model.patch.Node
import ru.pema4.musicbx.model.patch.Patch

object EditorService {
    private val _activePatch = MutableStateFlow(Patch.Default)
    val activePatch: StateFlow<Patch>
        get() = _activePatch.asStateFlow()

    external fun reset()

    private external fun addNodeOnBackend(uid: String, id: Int)
    fun addNode(uid: NodeUid, id: Int? = null): Int {
        val currentPatch = activePatch.value

        val newId = id ?: currentPatch.nextNodeId()

        val description = AvailableNodesService
            .availableNodes
            .value
            .getValue(uid)

        val node = Node(
            id = newId,
            uid = description.uid,
        )

        _activePatch.value = currentPatch
            .copy(nodes = currentPatch.nodes + node)

        addNodeOnBackend(description.uid.text, newId)

        return newId
    }

    private fun Patch.nextNodeId(): Int =
        nodes
            .maxOfOrNull { it.id + 1 }
            ?.coerceAtLeast(0)
            ?: 0

    external fun removeNode(nodeId: Int)

    private external fun connectNodes(from: Int, fromOutput: Int, to: Int, toInput: Int)
    fun connectNodes(from: CableFrom, to: CableTo) {
        connectNodes(
            from = from.nodeId,
            fromOutput = from.socketNumber,
            to = to.nodeId,
            toInput = to.socketNumber,
        )
    }

    private external fun disconnectNodes(from: Int, fromOutput: Int, to: Int, toInput: Int)
    fun disconnectNodes(from: CableFrom, to: CableTo) {
        disconnectNodes(
            from = from.nodeId,
            fromOutput = from.socketNumber,
            to = to.nodeId,
            toInput = to.socketNumber,
        )
    }

    external fun setParameter(nodeId: Int, parameterNum: Int, normalizedValue: Float)
}
