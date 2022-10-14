package ru.pema4.musicbx.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.pema4.musicbx.model.config.NodeUid
import ru.pema4.musicbx.model.patch.CableFrom
import ru.pema4.musicbx.model.patch.CableTo
import ru.pema4.musicbx.model.patch.Node
import ru.pema4.musicbx.model.patch.Patch

interface EditorService {
    val activePatch: StateFlow<Patch>

    fun reset()
    fun addNode(uid: NodeUid, nodeId: Int? = null): Int
    fun removeNode(nodeId: Int)
    fun connectNodes(from: CableFrom, to: CableTo)
    fun disconnectNodes(from: CableFrom, to: CableTo)
    fun setParameter(nodeId: Int, parameterNum: Int, normalizedValue: Float)

    companion object {
        val Native: EditorService = NativeEditorService()
        val Unspecified: EditorService = NoOpEditorService()
    }
}

private class NativeEditorService(
    private val availableNodesService: AvailableNodesService = AvailableNodesService.Native
) : EditorService {
    private val _activePatch = MutableStateFlow(Patch.Initial)
    override val activePatch: StateFlow<Patch>
        get() = _activePatch.asStateFlow()

    external override fun reset()

    private external fun addNodeOnBackend(uid: String, id: Int)
    override fun addNode(uid: NodeUid, nodeId: Int?): Int {
        val currentPatch = activePatch.value

        val newId = nodeId ?: currentPatch.nextNodeId()

        val description = availableNodesService
            .availableNodes
            .value
            .getValue(uid)

        val node = Node(
            id = newId,
            uid = description.uid
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

    external override fun removeNode(nodeId: Int)

    private external fun connectNodes(from: Int, fromOutput: String, to: Int, toInput: String)
    override fun connectNodes(from: CableFrom, to: CableTo) {
        connectNodes(
            from = from.nodeId,
            fromOutput = from.socketName,
            to = to.nodeId,
            toInput = to.socketName
        )
    }

    private external fun disconnectNodes(from: Int, fromOutput: String, to: Int, toInput: String)
    override fun disconnectNodes(from: CableFrom, to: CableTo) {
        disconnectNodes(
            from = from.nodeId,
            fromOutput = from.socketName,
            to = to.nodeId,
            toInput = to.socketName
        )
    }

    external override fun setParameter(nodeId: Int, parameterNum: Int, normalizedValue: Float)
}

private class NoOpEditorService(
    patch: Patch = Patch.Initial
) : EditorService {
    override val activePatch: StateFlow<Patch> =
        MutableStateFlow(patch)

    override fun reset() = Unit
    override fun addNode(uid: NodeUid, nodeId: Int?): Int = 0
    override fun removeNode(nodeId: Int) = Unit
    override fun connectNodes(from: CableFrom, to: CableTo) = Unit
    override fun disconnectNodes(from: CableFrom, to: CableTo) = Unit
    override fun setParameter(nodeId: Int, parameterNum: Int, normalizedValue: Float) = Unit
}
