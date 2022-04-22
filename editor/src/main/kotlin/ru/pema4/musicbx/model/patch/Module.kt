package ru.pema4.musicbx.model.patch

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import ru.pema4.musicbx.util.GridOffset
import ru.pema4.musicbx.util.GridSize

@Immutable
@Serializable
data class Module(
    val id: Int,
    val name: String = "Module $id",
    val inputs: List<InputSocket> = emptyList(),
    val outputs: List<OutputSocket> = emptyList(),
    val offset: GridOffset = GridOffset(x = GridSize(0), y = GridSize(1)),
)
