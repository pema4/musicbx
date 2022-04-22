package ru.pema4.musicbx.service

import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.pema4.musicbx.model.patch.Module
import kotlin.io.path.outputStream
import kotlin.io.path.pathString

object PlaybackService {
    init {
        val resource = ClassLoader
            .getSystemClassLoader()
            .getResourceAsStream("libaudio_test.dylib")
        check(resource != null)

        with(kotlin.io.path.createTempFile()) {
            resource.copyTo(outputStream())
            System.load(pathString)
        }
    }

    val sampleRate: Double? by derivedStateOf {
        ConfigurationService.currentConfiguration?.output?.sampleRate?.current
    }

    val supportedSampleRates: List<Int>
        @Composable get() = listOf(44100)

    val currentAudioInputDevice: String?
        @Composable get() = null

    val audioInputDevices: List<String>
        @Composable get() = emptyList()

    val currentAudioOutputDevice: String
        @Composable get() = "System Default"

    val audioOutputDevices: List<String>
        @Composable get() = listOf("System Default")

    val midiInputDevices: List<String>
        @Composable get() = emptyList()

    val midiOutputDevices: List<String>
        @Composable get() = emptyList()

    fun start() = Unit

    fun stop() = Unit

    fun addModule(module: Module) {
        val json = Json {
            encodeDefaults = true
        }
        val moduleJson = json.encodeToString(module)
        addModule(moduleJson)
    }

    private external fun addModule(moduleJson: String)
}
