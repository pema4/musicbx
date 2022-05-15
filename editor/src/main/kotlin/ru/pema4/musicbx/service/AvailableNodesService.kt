package ru.pema4.musicbx.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.model.config.NodeUid
import java.util.function.Consumer

object AvailableNodesService {
    private external fun registerListener(listener: AvailableNodesListener)

    private val _availableNodes = MutableStateFlow(emptyMap<NodeUid, NodeDescription>())
    val availableNodes: StateFlow<Map<NodeUid, NodeDescription>>
        get() = _availableNodes.asStateFlow()

    init {
        val listener = AvailableNodesListener {
            _availableNodes.value = it
        }
        registerListener(listener)
    }
}

private fun interface AvailableNodesListener : Consumer<String> {
    fun acceptNodeDescriptions(nodes: Map<NodeUid, NodeDescription>)

    override fun accept(t: String) {
        val descriptions: List<NodeDescription> = Json.decodeFromString(t)
        acceptNodeDescriptions(descriptions.associateBy(NodeDescription::uid))
    }
}
