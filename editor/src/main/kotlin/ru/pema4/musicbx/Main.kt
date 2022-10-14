package ru.pema4.musicbx

import androidx.compose.ui.window.application
import mu.KotlinLogging
import ru.pema4.musicbx.ui.App
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.outputStream
import kotlin.io.path.pathString

fun main() {
    loadNativeBackend()
    application {
        App()
    }
}

private fun loadNativeBackend() {
    val backendPath = distributableBackendPath ?: jarBackendPath
    checkNotNull(backendPath) {
        "Expected at least one available backend path"
    }

    System.load(backendPath.pathString)
    logger.info { "Loaded native backend from ${backendPath.pathString}" }
}

private val jarBackendPath: Path? by lazy {
    val resource = ClassLoader
        .getSystemClassLoader()
        .getResourceAsStream("libeditor_backend.dylib") ?: return@lazy null

    kotlin.io.path.createTempFile()
        .apply {
            outputStream().use { destination ->
                resource.use { origin ->
                    origin.copyTo(destination)
                }
            }
        }
}

private val distributableBackendPath: Path? by lazy {
    val resourcesDirProperty =
        System.getProperty("compose.application.resources.dir") ?: return@lazy null
    val resourcesDir = Path(resourcesDirProperty)
    resourcesDir / "libeditor_backend.dylib"
}

private val logger = KotlinLogging.logger {}
