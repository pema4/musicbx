package ru.pema4.musicbx

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.application
import dev.burnoo.cokoin.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.module
import ru.pema4.musicbx.service.AvailableModulesService
import ru.pema4.musicbx.service.FileService
import ru.pema4.musicbx.ui.App
import ru.pema4.musicbx.ui.EditorMaterialTheme
import ru.pema4.musicbx.viewmodel.rememberAppViewModel
import kotlin.io.path.outputStream
import kotlin.io.path.pathString

fun main() {
    loadNativeBackend()

    application {
        WithKoin {
            EditorMaterialTheme {
                val appViewModel = rememberAppViewModel()
                App(appViewModel)
            }
        }
    }
}

private fun loadNativeBackend() {
    val resource = ClassLoader
        .getSystemClassLoader()
        .getResourceAsStream("libmusicbx-jni.dylib")
    require(resource != null)

    with(kotlin.io.path.createTempFile()) {
        resource.copyTo(outputStream())
        System.load(pathString)
    }
}

@Composable
fun WithKoin(content: @Composable () -> Unit) {
    val appDeclaration: KoinApplication.() -> Unit = {
        modules(koinModule)
    }

    Koin(appDeclaration) {
        content()
    }
}

private val koinModule = module {
    single { FileService() }
    single { AvailableModulesService }
    // single { PlaybackService() }
}
