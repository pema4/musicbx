package ru.pema4.musicbx.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.burnoo.cokoin.get
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import ru.pema4.musicbx.WithKoin
import ru.pema4.musicbx.model.DefaultPatch
import ru.pema4.musicbx.model.Patch
import ru.pema4.musicbx.model.TestPatch
import ru.pema4.musicbx.service.FileService
import ru.pema4.musicbx.util.FileDialog
import ru.pema4.musicbx.util.FileDialogMode
import java.nio.file.Path
import kotlin.io.path.exists

@Composable
fun FrameWindowScope.App() {
    val initialPatch = TestPatch
    val state = rememberAppState(initialPatch)

    AppContent(state)

    AppMenuBar(state)

    if (state.isOpenDialogOpen) {
        FileDialog(
            title = "Choose a file",
            mode = FileDialogMode.Load,
        ) { path ->
            state.isOpenDialogOpen = false
            if (path?.exists() == true) {
                state.open(path)
            }
        }
    }

    if (state.isSaveDialogOpen) {
        FileDialog(
            title = "Saving a file",
            mode = FileDialogMode.Save,
        ) { path ->
            state.isSaveDialogOpen = false
            if (path != null) {
                state.save(path)
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FrameWindowScope.AppMenuBar(
    state: AppState,
) {
    MenuBar {
        Menu(text = "File") {
            Item(
                text = "Save As...",
                shortcut = KeyShortcut(Key.S, meta = true),
                onClick = { state.isSaveDialogOpen = true }
            )
            Item(
                text = "Open...",
                onClick = { state.isOpenDialogOpen = true }
            )
        }
    }
}

@Composable
fun AppContent(
    state: AppState = rememberAppState(),
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) {
        snapshotFlow { state.editorState }
            .onEach { println(state.editorState) }
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
                state = state.editorState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            )
            Tooltip()
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
class AppState(
    activePatch: Patch = DefaultPatch,
    private val fileService: FileService,
) {
    var editorState: EditorState by mutableStateOf(activePatch.toEditorState())
    var isOpenDialogOpen by mutableStateOf(false)
    var isSaveDialogOpen by mutableStateOf(false)

    fun save(path: Path) {
        val patch = editorState.toPatch()
        fileService.save(
            patch = patch,
            path = path,
        )
        editorState = patch.toEditorState()
    }

    fun open(path: Path) {
        val patch = fileService.load(path)
        editorState = patch.toEditorState()
    }
}

@Composable
fun rememberAppState(patch: Patch = DefaultPatch): AppState {
    val fileService = get<FileService>()
    return remember {
        AppState(patch, fileService)
    }
}

@Preview
@Composable
fun AppViewPreview() {
    WithKoin {
        AppContent()
    }
}

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
