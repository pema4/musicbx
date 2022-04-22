package ru.pema4.musicbx.model.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OutputConfiguration(
    val current: String,
    val available: List<String>,
    @SerialName("sample_rate") val sampleRate: SampleRateConfiguration,
)
