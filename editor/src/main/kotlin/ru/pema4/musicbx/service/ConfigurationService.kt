package ru.pema4.musicbx.service

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.pema4.musicbx.model.config.Configuration
import java.util.function.Consumer

object ConfigurationService {
    private external fun registerListener(listener: ConfigurationListener)
    // private external fun removeListener(listener: ConfigurationListener)

    var currentConfiguration: Configuration? by mutableStateOf(null)
        private set

    init {
        val listener = ConfigurationListener {
            currentConfiguration = it
        }
        registerListener(listener)
    }
}

private fun interface ConfigurationListener : Consumer<String> {
    fun acceptConfiguration(configuration: Configuration)

    override fun accept(t: String) {
        acceptConfiguration(Json.decodeFromString(t))
    }
}
