package ru.pema4.musicbx.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
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
import ru.pema4.musicbx.model.config.InputOutputSettings
import ru.pema4.musicbx.model.patch.TestPatch
import ru.pema4.musicbx.util.FileDialog
import ru.pema4.musicbx.util.FileDialogMode
import ru.pema4.musicbx.util.InstallTooltipManager
import ru.pema4.musicbx.util.MutableTooltipManager
import ru.pema4.musicbx.viewmodel.rememberAppViewModel
import java.awt.Cursor
import java.nio.file.Path
import kotlin.math.abs

@Composable
fun ApplicationScope.App(
    viewModel: AppViewModel = rememberAppViewModel(),
) {
    Window(::exitApplication) {
        AppMenuBar(viewModel)
        AppDialogWindows(viewModel)
        AppWindowContent(
            viewModel = viewModel,
            modifier = Modifier.background(MaterialTheme.colors.background),
        )
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
                            .background(MaterialTheme.colors.onSurface)
                            .height(1.dp)
                            .fillMaxWidth()
                    )
                    StatusBar(viewModel)
                }
            }
        }

        splitter {
            visiblePart {
                Spacer(
                    modifier = Modifier
                        .background(MaterialTheme.colors.onSurface)
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
                text = "Save",
                shortcut = KeyShortcut(Key.S, meta = true),
                onClick = viewModel::showSaveDialog,
            )
            Item(
                text = "Save As...",
                shortcut = KeyShortcut(Key.S, meta = true, shift = true),
                onClick = viewModel::showSaveDialog,
            )
            Item(
                text = "Open...",
                shortcut = KeyShortcut(Key.O, meta = true),
                onClick = viewModel::showOpenDialog,
            )
        }

        Menu(text = "View") {
            Item(
                text = "Actual Size",
                enabled = abs(viewModel.editorViewModel.scale - 1.0f) > 1e-5,
                shortcut = KeyShortcut(Key.Zero, meta = true),
                onClick = viewModel::actualSize,
            )
            Item(
                text = "Zoom In",
                shortcut = KeyShortcut(Key.Equals, meta = true),
                onClick = viewModel::zoomIn,
            )
            Item(
                text = "Zoom Out",
                shortcut = KeyShortcut(Key.Minus, meta = true),
                onClick = viewModel::zoomOut,
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
            viewModel.open(path)
        }
    }

    if (uiState.showingSaveDialog) {
        FileDialog(
            title = "Saving a file",
            mode = FileDialogMode.Save,
        ) { path ->
            viewModel.save(path)
        }
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
    fun actualSize() = Unit
    fun zoomIn() = Unit
    fun zoomOut() = Unit
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
    EditorTheme {
        AppWindowContent(
            viewModel = rememberAppViewModel(TestPatch)
        )
    }
}
