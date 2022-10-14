package ru.pema4.musicbx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import ru.pema4.musicbx.service.ConfigurationService
import ru.pema4.musicbx.service.PreferencesService

object AppContext {
    val appViewModel: AppViewModel
        @Composable
        @ReadOnlyComposable
        get() = LocalAppViewModel.current ?: error("AppViewModel is not in the composition")

    val nodeViewModel: NodeViewModel
        @Composable
        @ReadOnlyComposable
        get() = LocalNodeViewModel.current ?: error("NodeViewModel is not in the composition")
}

val AppContext.editorViewModel: EditorViewModel
    @Composable
    @ReadOnlyComposable
    get() = appViewModel.editor

val AppContext.menuBarViewModel: MenuBarViewModel
    @Composable
    @ReadOnlyComposable
    get() = appViewModel.menuBar

val AppContext.preferences: PreferencesService
    @Composable
    @ReadOnlyComposable
    get() = appViewModel.preferences

val AppContext.configuration: ConfigurationService
    @Composable
    @ReadOnlyComposable
    get() = appViewModel.configuration

@Composable
fun AppContext(
    appViewModel: AppViewModel? = LocalAppViewModel.current,
    nodeViewModel: NodeViewModel? = LocalNodeViewModel.current,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalAppViewModel provides appViewModel,
        LocalNodeViewModel provides nodeViewModel
    ) {
        content()
    }
}

private val LocalAppViewModel = compositionLocalOf<AppViewModel?> { null }

private val LocalNodeViewModel = compositionLocalOf<NodeViewModel?> { null }
