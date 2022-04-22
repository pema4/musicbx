package ru.pema4.musicbx.model.config

import kotlinx.serialization.Serializable

@Serializable
data class SampleRateConfiguration(
    val current: Double,
    val available: List<Double>,
)
