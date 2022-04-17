package ru.pema4.musicbx.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.burnoo.cokoin.get
import ru.pema4.musicbx.model.DefaultPatch
import ru.pema4.musicbx.model.Patch
import ru.pema4.musicbx.service.FileService
import ru.pema4.musicbx.view.AppState
import ru.pema4.musicbx.view.AppViewModel
import ru.pema4.musicbx.view.EditorState
import ru.pema4.musicbx.view.toEditorState
import ru.pema4.musicbx.view.toPatch
import java.nio.file.Path

@Composable
fun rememberAppViewModel(
    initialPatch: Patch = DefaultPatch,
): AppViewModel {
    val fileService = get<FileService>()
    return remember {
        AppViewModelImpl(
            initialPatch = initialPatch,
            fileService = fileService,
        )
    }
}

@Stable
class AppViewModelImpl(
    initialPatch: Patch = DefaultPatch,
    private val fileService: FileService,
) : AppViewModel {
    private var _uiState: AppStateImpl by mutableStateOf(AppStateImpl(initialPatch))
    override val uiState: AppState by ::_uiState

    override fun showOpenDialog() {
        _uiState.showingOpenDialog = true
    }

    override fun showSaveDialog() {
        _uiState.showingSaveDialog = true
    }

    override fun save(path: Path?) {
        _uiState.showingSaveDialog = false
        if (path != null) {
            val patch = uiState.editorState.toPatch()
            fileService.save(
                patch = patch,
                path = path,
            )
            _uiState.editorState = patch.toEditorState()
        }
    }

    override fun open(path: Path?) {
        _uiState.showingOpenDialog = false
        if (path != null) {
            val patch = fileService.load(path)
            _uiState.editorState = patch.toEditorState()
        }
    }
}

@Stable
private class AppStateImpl(
    activePatch: Patch = DefaultPatch,
) : AppState {
    override var editorState: EditorState by mutableStateOf(activePatch.toEditorState())
    override var showingOpenDialog by mutableStateOf(false)
    override var showingSaveDialog by mutableStateOf(false)
}
