package ru.pema4.musicbx.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.Window
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import ru.pema4.musicbx.WithKoin
import ru.pema4.musicbx.model.config.InputOutputSettings
import ru.pema4.musicbx.model.patch.TestPatch
import ru.pema4.musicbx.service.PlaybackService
import ru.pema4.musicbx.util.FileDialog
import ru.pema4.musicbx.util.FileDialogMode
import ru.pema4.musicbx.util.InstallTooltipManager
import ru.pema4.musicbx.util.MutableTooltipManager
import ru.pema4.musicbx.viewmodel.rememberAppViewModel
import java.awt.Cursor
import java.nio.file.Path
import kotlin.io.path.exists

@Composable
fun ApplicationScope.App(
    viewModel: AppViewModel = rememberAppViewModel(),
) {
    LaunchedEffect(viewModel) {
        PlaybackService.start()
        PlaybackService.stop()
    }

    Window(::exitApplication) {
        AppMenuBar(viewModel)
        AppDialogWindows(viewModel)
        AppWindowContent(viewModel)
    }
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun AppWindowContent(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    HorizontalSplitPane(
        modifier = modifier,
    ) {
        first(300.dp) {
            ModuleGalleryView(
                appViewModel = viewModel,
                modifier = Modifier
                    .background(SolidColor(Color.Blue), alpha = 0.05f)
                    .fillMaxSize(),
            )
        }

        second(100.dp) {
            Column {
                InstallTooltipManager(MutableTooltipManager()) {
                    EditorView(
                        viewModel = viewModel.editorViewModel,
                        modifier = Modifier
                            .weight(1.0f)
                    )
                    Spacer(
                        modifier = Modifier
                            .height(1.dp)
                            .background(Color.Black)
                    )
                    Tooltip(viewModel)
                }
            }
        }

        splitter {
            visiblePart {
                Spacer(
                    modifier = Modifier
                        .background(Color.Black)
                        .width(1.dp)
                        .fillMaxHeight()
                )
            }

            handle {
                Spacer(
                    modifier = Modifier
                        .markAsHandle()
                        .cursorForHorizontalResize()
                        .background(SolidColor(Color.Black), alpha = 0.05f)
                        .width(5.dp)
                        .fillMaxHeight()
                )
            }
        }
    }
}

private fun Modifier.cursorForHorizontalResize(): Modifier =
    pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FrameWindowScope.AppMenuBar(
    viewModel: AppViewModel,
) {
    MenuBar {
        Menu(text = "File") {
            Item(
                text = "Save As...",
                shortcut = KeyShortcut(Key.S, meta = true),
                onClick = viewModel::showSaveDialog,
            )
            Item(
                text = "Open...",
                onClick = viewModel::showOpenDialog,
            )
        }

        val ioSettings by viewModel.collectIoSettingsAsState()
        val availableOutputs = ioSettings?.output?.available ?: emptyList()
        Menu(text = "Settings") {
            Menu(
                text = "Select Output...",
                enabled = availableOutputs.isNotEmpty(),
            ) {
                for (output in availableOutputs) {
                    Item(
                        text = output,
                        onClick = { viewModel.changeOutput(output) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDialogWindows(
    viewModel: AppViewModel,
) {
    val uiState = viewModel.uiState

    if (uiState.showingOpenDialog) {
        FileDialog(
            title = "Choose a file",
            mode = FileDialogMode.Load,
        ) { path ->
            if (path?.exists() == true) {
                viewModel.open(path)
            }
        }
    }

    if (uiState.showingSaveDialog) {
        FileDialog(
            title = "Saving a file",
            mode = FileDialogMode.Save,
        ) { path ->
            if (path != null) {
                viewModel.save(path)
            }
        }
    }
}

@Composable
fun EditorMaterialTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme {
        content()
    }
}

@Stable
interface AppViewModel {
    val uiState: AppState
    val editorViewModel: EditorViewModel

    @Composable
    fun collectIoSettingsAsState(): State<InputOutputSettings?>

    @Composable
    fun collectAvailableModulesAsState(): State<List<ModuleState>>

    fun showOpenDialog() = Unit
    fun showSaveDialog() = Unit

    fun save(path: Path?) = Unit
    fun open(path: Path?) = Unit
    fun changeOutput(newOutput: String) = Unit
}

@Stable
interface AppState {
    val showingOpenDialog: Boolean
    val showingSaveDialog: Boolean
}

@Preview
@Composable
fun AppPreview() {
    EditorMaterialTheme {
        WithKoin {
            AppWindowContent(
                viewModel = rememberAppViewModel(TestPatch)
            )
        }
    }
}
