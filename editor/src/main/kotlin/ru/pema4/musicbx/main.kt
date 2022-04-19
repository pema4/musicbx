@file:OptIn(ExperimentalComposeUiApi::class)

package ru.pema4.musicbx

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.application
import dev.burnoo.cokoin.Koin
import org.koin.core.KoinApplication
import org.koin.dsl.module
import ru.pema4.musicbx.model.TestPatch
import ru.pema4.musicbx.service.FileService
import ru.pema4.musicbx.service.PatchService
import ru.pema4.musicbx.service.PlaybackService
import ru.pema4.musicbx.view.App
import ru.pema4.musicbx.view.AppViewModel
import ru.pema4.musicbx.view.EditorMaterialTheme
import ru.pema4.musicbx.viewmodel.AppViewModelImpl
import ru.pema4.musicbx.viewmodel.rememberAppViewModel

fun main() {
    application {
        WithKoin {
            EditorMaterialTheme {
                val appViewModel = rememberAppViewModel(initialPatch = TestPatch)
                App(appViewModel)
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
    single<AppViewModel> { AppViewModelImpl(TestPatch, get()) }
    single { FileService() }
    single { PatchService() }
    single { PlaybackService() }
}
