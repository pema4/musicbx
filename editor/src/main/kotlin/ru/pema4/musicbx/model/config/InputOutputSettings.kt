package ru.pema4.musicbx.model.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InputOutputSettings(
    val output: OutputSettings,
)

@Serializable
data class OutputSettings(
    val current: String,
    val available: List<String>,
    @SerialName("sample_rate") val sampleRate: SampleRateSettings,
)

@Serializable
data class SampleRateSettings(
    val current: Double,
    val available: List<Double>,
)
