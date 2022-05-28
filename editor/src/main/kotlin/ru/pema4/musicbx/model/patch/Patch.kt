package ru.pema4.musicbx.model.patch

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import ru.pema4.musicbx.model.config.TestNodeDescription
import ru.pema4.musicbx.util.GridOffset
import ru.pema4.musicbx.util.GridSize
import kotlin.random.Random

@Immutable
@Serializable
data class Patch(
    val nodes: List<Node>,
    val cables: List<Cable>,
) {
    companion object Constants {
        val Initial = Patch(
            nodes = emptyList(),
            cables = emptyList(),
        )
    }
}

val TestPatch = Patch(
    nodes = listOf(
        Node(
            id = 0,
            uid = TestNodeDescription.uid,
        ),
        Node(
            id = 1,
            uid = TestNodeDescription.uid,
            offset = GridOffset(x = GridSize(10), y = GridSize(10))
        ),
        Node(
            id = Random.nextInt(10, 10000),
            uid = TestNodeDescription.uid,
            offset = GridOffset(x = GridSize(30), y = GridSize(30))
        )
    ),
    cables = listOf(
        Cable(
            from = CableFrom(nodeId = 1, socketName = "out"),
            to = CableTo(nodeId = 0, socketName = "in"),
        )
    ),
)
