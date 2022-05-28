package ru.pema4.musicbx.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.model.config.NodeUid
import ru.pema4.musicbx.model.patch.Patch
import ru.pema4.musicbx.service.AvailableNodesService
import ru.pema4.musicbx.service.ConfigurationService
import ru.pema4.musicbx.service.PreferencesService
import ru.pema4.musicbx.ui.AppViewModel
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Composable
fun rememberAppViewModel(
    initialPatch: Patch = Patch.Initial,
): AppViewModel {
    return remember {
        AppViewModelImpl(
            initialPatch = initialPatch,
            availableNodesService = AvailableNodesService,
        )
    }
}

@Stable
class AppViewModelImpl(
    initialPatch: Patch = Patch.Initial,
    private val availableNodesService: AvailableNodesService,
) : AppViewModel {
    private var _editorViewModel: EditorViewModelImpl by mutableStateOf(EditorViewModelImpl(initialPatch))
    override val editor by ::_editorViewModel
    override var showingOpenDialog: Boolean by mutableStateOf(false)
        private set
    override var showingSaveDialog: Boolean by mutableStateOf(false)
        private set
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    override val preferences = PreferencesService
    override val configuration = ConfigurationService

    @Composable
    override fun collectAvailableNodesAsState(): State<Map<NodeUid, NodeDescription>> {
        return availableNodesService
            .availableNodes
            .collectAsState()
    }

    override fun showOpenDialog() {
        showingOpenDialog = true
    }

    override fun showSaveDialog() {
        showingSaveDialog = true
    }

    override fun reset() {
        _editorViewModel = EditorViewModelImpl(Patch.Initial)
        runBlocking {
            editor.recreateGraphOnBackend()
        }
    }

    override fun save(path: Path?) {
        showingSaveDialog = false
        if (path != null) {
            val patch = editor.extractPatch()
            val fileText = json.encodeToString(value = patch)
            path.writeText(fileText)

            _editorViewModel = EditorViewModelImpl(patch)
        }
    }

    override fun open(path: Path?) {
        showingOpenDialog = false
        if (path?.exists() == true) {
            val fileText = path.readText()
            val patch = json.decodeFromString<Patch>(fileText)

            _editorViewModel = EditorViewModelImpl(patch)

            runBlocking {
                editor.recreateGraphOnBackend()
            }
        }
    }
}

