package ru.pema4.musicbx.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.pema4.musicbx.model.config.DeviceSettings
import ru.pema4.musicbx.model.config.InputOutputSettings
import java.util.function.Consumer
import java.util.prefs.Preferences

object ConfigurationService {
    private val preferences = Preferences.userNodeForPackage(ConfigurationService::class.java)
    private val initialOutput = preferences.get(::output.name, null).toDeviceSettings()
    private val _output = MutableStateFlow(initialOutput)
    var output: StateFlow<DeviceSettings> = _output.asStateFlow()

    init {
        registerListener {
            _output.value = it.output
        }

        preferences.addPreferenceChangeListener { event ->
            val newValue = event.newValue
            when (event.key) {
                ::output.name -> _output.value = newValue.toDeviceSettings()
            }
        }
    }

    private external fun registerListener(listener: ConfigurationListener)
    external fun changeCurrentOutput(newOutput: String)
    external fun refresh()
}

private fun interface ConfigurationListener : Consumer<String> {
    fun acceptSettings(ioSettings: InputOutputSettings)
    override fun accept(t: String) {
        acceptSettings(Json.decodeFromString(t))
    }
}

private fun String?.toDeviceSettings(): DeviceSettings {
    return Json.decodeFromString(this ?: return DeviceSettings.Unspecified)
}
