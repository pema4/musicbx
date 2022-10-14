package ru.pema4.musicbx.model.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InputOutputSettings(
    val output: DeviceSettings
)

@Serializable
data class DeviceSettings(
    val current: String?,
    val available: List<String>,
    @SerialName("sample_rate") val sampleRate: SampleRateSettings?
) {
    companion object {
        val Unspecified = DeviceSettings(
            current = null,
            available = emptyList(),
            sampleRate = null
        )
    }
}

@Serializable
data class SampleRateSettings(
    val current: Double,
    val available: List<Double>
)
