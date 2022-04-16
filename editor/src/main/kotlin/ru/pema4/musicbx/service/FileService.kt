package ru.pema4.musicbx.service

import ru.pema4.musicbx.model.Cable
import ru.pema4.musicbx.model.CableEnd
import ru.pema4.musicbx.model.CableFrom
import ru.pema4.musicbx.model.CableTo
import ru.pema4.musicbx.model.InputSocket
import ru.pema4.musicbx.model.Module
import ru.pema4.musicbx.model.OutputSocket
import ru.pema4.musicbx.model.Patch
import ru.pema4.musicbx.util.GridOffset
import ru.pema4.musicbx.util.GridSize
import java.nio.file.Path
import kotlin.random.Random

class FileService {
    val activeFile: Patch get() = TODO()

    fun load(path: Path): Patch {
        return testPatch
    }

    fun save(patch: Patch, path: Path) {
        TODO("Not implemented yet")
    }

    private companion object {
        private val testPatch get() = Patch(
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
    }
}
