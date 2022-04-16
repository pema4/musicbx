package ru.pema4.musicbx

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.burnoo.cokoin.Koin
import dev.burnoo.cokoin.get
import org.koin.core.KoinApplication
import org.koin.dsl.module
import ru.pema4.musicbx.service.FileService
import ru.pema4.musicbx.service.PatchService
import ru.pema4.musicbx.service.PlaybackService
import ru.pema4.musicbx.service.TooltipService
import ru.pema4.musicbx.ui.EditorMaterialTheme
import ru.pema4.musicbx.ui.App
import ru.pema4.musicbx.ui.rememberAppState
import kotlin.io.path.Path

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
        ) {
            WithKoin {
                EditorMaterialTheme {
                    val appState = rememberAppState(
                        patch = get<FileService>().load(Path("")),
                    )
                    App(appState)
                }
            }
        }
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
    single { TooltipService() }
    single { FileService() }
    single { PatchService() }
    single { PlaybackService() }
}
