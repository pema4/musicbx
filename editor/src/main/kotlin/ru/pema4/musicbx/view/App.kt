package ru.pema4.musicbx.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import ru.pema4.musicbx.util.FileDialog
import ru.pema4.musicbx.util.FileDialogMode
import ru.pema4.musicbx.viewmodel.rememberAppViewModel
import java.nio.file.Path
import kotlin.io.path.exists

@Composable
fun FrameWindowScope.App(
    viewModel: AppViewModel = rememberAppViewModel(),
) {
    AppMenuBar(viewModel)
    AppContent(viewModel)
    AppDialogWindows(viewModel)
}

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
    }
}

@Composable
fun AppContent(
    viewModel: AppViewModel,
    // state: AppState = rememberAppState(),
    modifier: Modifier = Modifier,
) {
    val uiState = viewModel.uiState

    LaunchedEffect(Unit) {
        snapshotFlow { uiState.editorState }
            .onEach { println(uiState.editorState) }
            .collect()
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .size(400.dp, 300.dp)
            .then(modifier),
    ) {
        Box(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
        ) {
            Spacer(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.Black)
            )
        }
        Column {
            EditorView(
                state = uiState.editorState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            )
            Tooltip()
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
    fun showOpenDialog()
    fun showSaveDialog()
    fun save(path: Path?)
    fun open(path: Path?)
}

@Stable
interface AppState {
    val editorState: EditorState
    val showingOpenDialog: Boolean
    val showingSaveDialog: Boolean
}

//
// @Composable
// fun rememberAppState(patch: Patch = DefaultPatch): AppState {
//     val fileService = get<FileService>()
//     return remember {
//         AppState(patch, fileService)
//     }
// }

// @Preview
// @Composable
// fun AppViewPreview() {
//     WithKoin {
//         AppContent()
//     }
// }

/*
fun App() {
    Row {
        ActivityBar()
        Spacer()
        Column {
            ControlPanel()
            Spacer()
            PatchEditor()
            StatusBar()
        }
    }
}

fun ActivityBar() {
    Column {
        Modules()
        Projects()
    }
}

fun ControlPanel() {
    Row {
        Play()
        Pause()
        SampleRateSelection()
        AudioDeviceChooser()
        MidiDeviceChooser()
        CpuMeter()
    }
}
 */
