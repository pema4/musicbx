package ru.pema4.musicbx.model.config

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import ru.pema4.musicbx.model.patch.InputSocket
import ru.pema4.musicbx.model.patch.NodeParameter
import ru.pema4.musicbx.model.patch.OutputSocket

@JvmInline
@Serializable
value class NodeUid(val text: String) : Comparable<NodeUid> {
    override fun compareTo(other: NodeUid): Int {
        return text.compareTo(other.text)
    }
}

@Immutable
@Serializable
data class NodeDescription(
    val uid: NodeUid,
    val name: String,
    val summary: String,
    val inputs: List<InputSocket> = emptyList(),
    val outputs: List<OutputSocket> = emptyList(),
    val parameters: List<NodeParameter> = emptyList(),
)

val TestNodeDescription = NodeDescription(
    uid = NodeUid("std.osc.sin"),
    name = "Osc",
    summary = "Oscillator",
    inputs = listOf(
        InputSocket(0),
        InputSocket(1),
    ),
    outputs = listOf(
        OutputSocket(0),
    ),
)
