package ru.pema4.musicbx.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pema4.musicbx.model.patch.Patch
import ru.pema4.musicbx.service.AvailableNodesService
import ru.pema4.musicbx.service.ConfigurationService
import ru.pema4.musicbx.service.EditorService
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
            preferences = PreferencesService.Default,
            configuration = ConfigurationService.Native,
            availableNodesService = AvailableNodesService.Native,
            editorService = EditorService.Native,
        )
    }
}

@Stable
private class AppViewModelImpl(
    initialPatch: Patch = Patch.Initial,
    override val preferences: PreferencesService = PreferencesService.Unspecified,
    override val configuration: ConfigurationService = ConfigurationService.Unspecified,
    override val availableNodesService: AvailableNodesService = AvailableNodesService.Unspecified,
    private val editorService: EditorService = EditorService.Unspecified,
) : AppViewModel {
    override var editor: EditorViewModelImpl by run {
        val editorViewModel = EditorViewModelImpl(
            patch = initialPatch,
            editorService = editorService,
        )
        mutableStateOf(editorViewModel)
    }
    override val menuBar: MenuBarViewModelImpl = MenuBarViewModelImpl()

    override var fileChanged: Boolean by mutableStateOf(false)
        private set
    override var openedFile: Path? by mutableStateOf(null)
        private set

    override fun reset() {
        editor = EditorViewModelImpl(Patch.Initial, editorService)
        runBlocking {
            editor.recreateGraphOnBackend()
        }
        openedFile = null
    }

    override fun save(file: Path?) {
        if (file != null) {
            val patch = editor.extractPatch()
            val fileText = json.encodeToString(value = patch)
            file.writeText(fileText)

            editor = EditorViewModelImpl(patch, editorService)
            openedFile = file
        }

        menuBar.uiState.showingSaveDialog = false
    }

    override fun open(file: Path?) {
        if (file?.exists() == true) {
            val fileText = file.readText()
            val patch = json.decodeFromString<Patch>(fileText)

            editor = EditorViewModelImpl(patch, editorService)
            editor.recreateGraphOnBackend()
            openedFile = file
        }

        menuBar.uiState.showingSaveDialog = false
    }

    override fun markFileAsChanged() {
        fileChanged = true
    }
}

private val json = Json {
    prettyPrint = true
    encodeDefaults = true
}
