package ru.pema4.musicbx.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.pema4.musicbx.model.config.InputOutputSettings
import ru.pema4.musicbx.model.patch.DefaultPatch
import ru.pema4.musicbx.model.patch.Patch
import ru.pema4.musicbx.model.preferences.Zoom
import ru.pema4.musicbx.service.AvailableModulesService
import ru.pema4.musicbx.service.ConfigurationService
import ru.pema4.musicbx.service.FileService
import ru.pema4.musicbx.service.PreferencesService
import ru.pema4.musicbx.ui.AppState
import ru.pema4.musicbx.ui.AppViewModel
import ru.pema4.musicbx.ui.ModuleState
import java.nio.file.Path
import kotlin.io.path.exists

@Composable
fun rememberAppViewModel(
    initialPatch: Patch = DefaultPatch,
): AppViewModel {
    val fileService = FileService()
    val availableModuleService = AvailableModulesService

    return remember {
        AppViewModelImpl(
            initialPatch = initialPatch,
            fileService = fileService,
            availableModuleService = availableModuleService,
            configurationService = ConfigurationService,
        )
    }
}

@Stable
class AppViewModelImpl(
    initialPatch: Patch = DefaultPatch,
    private val fileService: FileService,
    private val availableModuleService: AvailableModulesService,
    private val configurationService: ConfigurationService,
) : AppViewModel {
    private var _uiState: AppStateImpl by mutableStateOf(AppStateImpl())
    override val uiState: AppState by ::_uiState

    private var _editorViewModel: EditorViewModelImpl by mutableStateOf(EditorViewModelImpl(initialPatch))
    override val editor by ::_editorViewModel
    override val preferences: PreferencesService = PreferencesService

    @Composable
    override fun collectIoSettingsAsState(): State<InputOutputSettings?> {
        return configurationService.ioSettings.collectAsState()
    }

    @Composable
    override fun collectAvailableModulesAsState(): State<List<ModuleState>> {
        val modules by availableModuleService.availableModules.collectAsState()
        return derivedStateOf {
            modules.map { ModuleStateImpl(it, expanded = false) }
        }
    }

    override fun showOpenDialog() {
        _uiState.showingOpenDialog = true
    }

    override fun showSaveDialog() {
        _uiState.showingSaveDialog = true
    }

    override fun save(path: Path?) {
        _uiState.showingSaveDialog = false
        if (path != null) {
            val patch = editor.extractPatch()
            fileService.save(
                patch = patch,
                path = path,
            )

            _editorViewModel = EditorViewModelImpl(patch)
        }
    }

    override fun open(path: Path?) {
        _uiState.showingOpenDialog = false
        if (path?.exists() == true) {
            val patch = fileService.load(path)

            _editorViewModel = EditorViewModelImpl(patch)

            editor.recreateGraphOnBackend()
        }
    }

    override fun actualSize() {
        PreferencesService.zoom = Zoom.One
    }

    override fun zoomIn() {
        PreferencesService.zoom = PreferencesService.zoom.inc()
    }

    override fun zoomOut() {
        PreferencesService.zoom = PreferencesService.zoom.dec()
    }
}

@Stable
private class AppStateImpl : AppState {
    override var showingOpenDialog by mutableStateOf(false)
    override var showingSaveDialog by mutableStateOf(false)
}
