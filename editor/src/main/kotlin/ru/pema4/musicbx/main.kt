@file:OptIn(ExperimentalComposeUiApi::class)

package ru.pema4.musicbx

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.burnoo.cokoin.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.module
import ru.pema4.musicbx.service.FileService
import ru.pema4.musicbx.service.PatchService
import ru.pema4.musicbx.service.PlaybackService
import ru.pema4.musicbx.service.TooltipService
import ru.pema4.musicbx.ui.App
import ru.pema4.musicbx.ui.EditorMaterialTheme

fun main() {
    application {
        WithKoin {
            EditorMaterialTheme {
                Window(
                    onCloseRequest = ::exitApplication,
                ) {
                    App()
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
