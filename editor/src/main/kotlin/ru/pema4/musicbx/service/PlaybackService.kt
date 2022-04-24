package ru.pema4.musicbx.service

import androidx.compose.runtime.Composable

object PlaybackService {
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

    external fun start()

    external fun stop()

    external fun addModule(uid: String, id: Int)

    external fun removeModule(moduleId: Int)
}
