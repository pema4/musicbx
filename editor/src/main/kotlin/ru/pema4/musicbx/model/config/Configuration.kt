package ru.pema4.musicbx.model.config

import kotlinx.serialization.Serializable

@Serializable
data class Configuration(
    val output: OutputConfiguration,
)
