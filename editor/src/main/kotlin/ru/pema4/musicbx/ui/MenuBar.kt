@file:OptIn(ExperimentalComposeUiApi::class)

package ru.pema4.musicbx.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuScope
import ru.pema4.musicbx.model.preferences.PreferredTheme
import ru.pema4.musicbx.model.preferences.Zoom
import ru.pema4.musicbx.service.ConfigurationService
import ru.pema4.musicbx.service.PreferencesService
import kotlin.math.abs

@Composable
fun FrameWindowScope.AppMenuBar(
    vm: AppViewModel,
) {
    MenuBar {
        Menu(text = "File") {
            Open(vm)
            Save(vm)
            SaveAs(vm)
        }
        Menu(text = "View") {
            AppearanceSelection(vm.preferences)
            Separator()
            ActualSize(vm.preferences)
            ZoomIn(vm.preferences)
            ZoomOut(vm.preferences)
        }
        Menu(text = "Settings") {
            OutputSelection(vm.configuration)
        }
        // Обязательное меню для работы Spotlight на macOS
        Menu(text = "Help") {
            Item(text = "About", onClick = {})
        }
    }
}

@Composable
private fun MenuScope.Open(viewModel: AppViewModel) {
    Item(
        text = "Open...",
        shortcut = KeyShortcut(Key.O, meta = true),
        onClick = viewModel::showOpenDialog,
    )
}

@Composable
private fun MenuScope.Save(viewModel: AppViewModel) {
    Item(
        text = "Save",
        shortcut = KeyShortcut(Key.S, meta = true),
        onClick = viewModel::showSaveDialog,
    )
}

@Composable
private fun MenuScope.SaveAs(viewModel: AppViewModel) {
    Item(
        text = "Save As...",
        shortcut = KeyShortcut(Key.S, meta = true, shift = true),
        onClick = viewModel::showSaveDialog,
    )
}

@Composable
private fun MenuScope.AppearanceSelection(preferences: PreferencesService) {
    var theme by preferences::theme

    Menu(text = "Appearance") {
        RadioButtonItem(
            text = "System Theme",
            selected = theme == PreferredTheme.Auto,
            onClick = { theme = PreferredTheme.Auto },
        )
        RadioButtonItem(
            text = "Light Theme",
            selected = theme == PreferredTheme.Light,
            onClick = { theme = PreferredTheme.Light },
        )
        RadioButtonItem(
            text = "Dark Theme",
            selected = theme == PreferredTheme.Dark,
            onClick = { theme = PreferredTheme.Dark },
        )
    }
}

@Composable
private fun MenuScope.ZoomIn(preferences: PreferencesService) {
    var zoom by preferences::zoom

    Item(
        text = "Zoom In",
        shortcut = KeyShortcut(Key.Equals, meta = true),
        onClick = { zoom = zoom.increase() },
    )
}

@Composable
private fun MenuScope.ZoomOut(preferences: PreferencesService) {
    var zoom by preferences::zoom

    Item(
        text = "Zoom Out",
        shortcut = KeyShortcut(Key.Minus, meta = true),
        onClick = { zoom = zoom.decrease() },
    )
}

@Composable
private fun MenuScope.ActualSize(preferences: PreferencesService) {
    var zoom by preferences::zoom

    Item(
        text = "Actual Size",
        enabled = abs(zoom.scale - 1.0f) > 1e-5,
        shortcut = KeyShortcut(Key.Zero, meta = true),
        onClick = { zoom = Zoom.One },
    )
}

@Composable
private fun MenuScope.OutputSelection(configuration: ConfigurationService) {
    val outputs by configuration.output.collectAsState()

    Menu(
        text = "Select Output...",
        enabled = outputs.available.isNotEmpty(),
    ) {
        for (output in outputs.available) {
            RadioButtonItem(
                text = output,
                onClick = { configuration.changeCurrentOutput(output) },
                selected = outputs.current == output
            )
        }

        Separator()

        Item(
            text = "Refresh",
            onClick = { configuration.refresh() },
        )
    }
}
