package ru.pema4.musicbx.model.patch

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import ru.pema4.musicbx.model.config.NodeUid
import ru.pema4.musicbx.util.GridOffset

@Immutable
@Serializable
data class Node(
    val id: Int,
    val uid: NodeUid,
    val offset: GridOffset = GridOffset.Zero,
    val parameterValues: Map<String, String> = emptyMap(),
)
