package ru.pema4.musicbx.model.patch

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import ru.pema4.musicbx.util.GridOffset
import ru.pema4.musicbx.util.GridSize
import kotlin.random.Random

@Immutable
@Serializable
data class Patch(
    val modules: List<Module>,
    val cables: List<Cable>,
)

val DefaultPatch = Patch(
    modules = emptyList(),
    cables = emptyList(),
)

val TestPatch = Patch(
    modules = listOf(
        Module(
            id = 0,
            inputs = listOf(
                InputSocket(0),
                InputSocket(1),
            ),
            outputs = listOf(
                OutputSocket(0),
            ),
        ),
        Module(
            id = 1,
            inputs = listOf(
                InputSocket(0),
            ),
            outputs = listOf(
                OutputSocket(0),
                OutputSocket(1),
            ),
            offset = GridOffset(x = GridSize(10), y = GridSize(10))
        ),
        Module(
            id = Random.nextInt(10, 10000),
            inputs = listOf(
                InputSocket(0),
            ),
            outputs = listOf(
                OutputSocket(0),
                OutputSocket(1),
            ),
            offset = GridOffset(x = GridSize(30), y = GridSize(30))
        )
    ),
    cables = listOf(
        Cable(
            from = CableFrom(moduleId = 1, socketNumber = 1),
            to = CableTo(moduleId = 0, socketNumber = 0),
        )
    ),
)
