package ru.pema4.musicbx.service

import androidx.compose.runtime.Composable

class PlaybackService {
    val sampleRate: Int
        @Composable get() = 44100

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
}
