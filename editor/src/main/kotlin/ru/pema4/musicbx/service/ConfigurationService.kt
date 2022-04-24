package ru.pema4.musicbx.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.pema4.musicbx.model.config.InputOutputSettings
import java.util.function.Consumer

object ConfigurationService {
    private external fun registerListener(listener: ConfigurationListener)
    // private external fun removeListener(listener: ConfigurationListener)

    private val _ioSettings: MutableStateFlow<InputOutputSettings?> = MutableStateFlow(null)
    val ioSettings: StateFlow<InputOutputSettings?> by ::_ioSettings

    init {
        val listener = ConfigurationListener {
            _ioSettings.value = it
        }
        registerListener(listener)
    }
}

private fun interface ConfigurationListener : Consumer<String> {
    fun acceptSettings(ioSettings: InputOutputSettings)

    override fun accept(t: String) {
        acceptSettings(Json.decodeFromString(t))
    }
}
