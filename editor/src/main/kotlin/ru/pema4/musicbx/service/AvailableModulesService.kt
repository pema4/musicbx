package ru.pema4.musicbx.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ru.pema4.musicbx.model.patch.Module
import java.util.function.Consumer

object AvailableModulesService {
    private external fun registerListener(listener: AvailableModulesListener)
    private val _availableModules = MutableStateFlow(emptyList<Module>())
    val availableModules: StateFlow<List<Module>> by ::_availableModules

    init {
        val listener = AvailableModulesListener {
            _availableModules.value = it
        }
        registerListener(listener)
    }
}

private fun interface AvailableModulesListener : Consumer<String> {
    fun acceptModules(modules: List<Module>)
    override fun accept(t: String) {
        acceptModules(Json.decodeFromString(t))
    }
}
