package ru.pema4.musicbx

import androidx.compose.ui.window.application
import mu.KotlinLogging
import ru.pema4.musicbx.ui.App
import ru.pema4.musicbx.viewmodel.rememberAppViewModel
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.outputStream
import kotlin.io.path.pathString

fun main() {
    loadNativeBackend()
    application {
        val appViewModel = rememberAppViewModel()
        App(appViewModel)
    }
}

private fun loadNativeBackend() {
    val possiblePaths = listOfNotNull(
        getBackendFromDistributable(),
        getBackendFromJar(),
    )

    check(possiblePaths.isNotEmpty()) {
        "Expected at least one available backend path"
    }

    for (path in possiblePaths) {
        System.load(path.pathString)
        logger.info { "Loaded native backend from ${path.pathString}" }
        return
    }
}

private fun getBackendFromJar(): Path? {
    val resource = ClassLoader
        .getSystemClassLoader()
        .getResourceAsStream("libeditor_backend.dylib") ?: return null

    val tempFile = kotlin.io.path.createTempFile()
    tempFile.outputStream().use { destination ->
        resource.use { origin ->
            origin.copyTo(destination)
        }
    }
    return tempFile
}

private fun getBackendFromDistributable(): Path? {
    val resourcesDirProperty =
        System.getProperty("compose.application.resources.dir") ?: return null
    val resourcesDir = Path(resourcesDirProperty)
    return resourcesDir / "libeditor_backend.dylib"
}

private val logger = KotlinLogging.logger {}
