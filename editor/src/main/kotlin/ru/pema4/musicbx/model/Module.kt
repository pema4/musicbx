package ru.pema4.musicbx.model

import androidx.compose.runtime.Immutable
import ru.pema4.musicbx.util.GridOffset
import ru.pema4.musicbx.util.GridSize

@Immutable
data class Module(
    val id: Int,
    val name: String = "Module $id",
    val inputs: List<InputSocket> = emptyList(),
    val outputs: List<OutputSocket> = emptyList(),
    val offset: GridOffset = GridOffset(x = GridSize(0), y = GridSize(1)),
)
