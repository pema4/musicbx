package ru.pema4.musicbx.service

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pema4.musicbx.model.patch.Patch
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FileService {
    fun load(path: Path): Patch {
        val fileText = path.readText()
        return Json.decodeFromString(fileText)
    }

    fun save(patch: Patch, path: Path) {
        val fileText = Json.encodeToString(patch)
        path.writeText(fileText)
    }
}
