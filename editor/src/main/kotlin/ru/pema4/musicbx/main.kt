package ru.pema4.musicbx

import androidx.compose.ui.window.application
import ru.pema4.musicbx.ui.App
import ru.pema4.musicbx.viewmodel.rememberAppViewModel
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
    val resource = ClassLoader
        .getSystemClassLoader()
        .getResourceAsStream("libeditor_backend.dylib")
    with(kotlin.io.path.createTempFile()) {
        resource?.copyTo(outputStream())
        System.load(pathString)
    }
}

