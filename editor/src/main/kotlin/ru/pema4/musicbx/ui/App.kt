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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ru.pema4.musicbx.WithKoin
import ru.pema4.musicbx.model.DefaultPatch
import ru.pema4.musicbx.model.Patch

@Composable
fun App(
    state: AppState = rememberAppState(),
    modifier: Modifier = Modifier,
) {
    val patchState = rememberEditorState(
        basePatch = state.activePatch,
    )

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
                state = patchState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.0f)
            )
            Tooltip(modifier = Modifier.zIndex(1.0f))
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
) {
    var activePatch by mutableStateOf(activePatch)
}

@Composable
fun rememberAppState(patch: Patch = DefaultPatch): AppState {
    return remember {
        AppState(patch)
    }
}

@Preview
@Composable
fun AppViewPreview() {
    WithKoin {
        App()
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
