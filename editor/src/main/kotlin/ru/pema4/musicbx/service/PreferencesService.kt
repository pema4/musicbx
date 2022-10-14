package ru.pema4.musicbx.service

import androidx.compose.runtime.mutableStateOf
import ru.pema4.musicbx.model.preferences.PreferredTheme
import ru.pema4.musicbx.model.preferences.Zoom
import java.util.prefs.Preferences

interface PreferencesService {
    var theme: PreferredTheme
    var zoom: Zoom

    companion object {
        val Default: PreferencesService = DefaultPreferencesService()
        val Unspecified: PreferencesService = NoOpPreferencesService()
    }
}

private class DefaultPreferencesService : PreferencesService {
    private val preferences = Preferences.userNodeForPackage(DefaultPreferencesService::class.java)

    init {
        preferences.addPreferenceChangeListener { event ->
            val newValue = event.newValue
            when (event.key) {
                ::theme.name -> themeState.value = newValue.toPreferredTheme()
                ::zoom.name -> zoomState.value = Zoom(newValue.toInt())
            }
        }
    }

    private val initialTheme = preferences.get(::theme.name, PreferredTheme.Auto.name).toPreferredTheme()
    private val themeState = mutableStateOf(initialTheme)
    override var theme: PreferredTheme
        get() = themeState.value
        set(value) {
            preferences.put(::theme.name, value.name)
        }

    private val initialZoomIndex = preferences.getInt(::zoom.name, Zoom.Default.step)
    private val zoomState = mutableStateOf(Zoom(initialZoomIndex))
    override var zoom: Zoom
        get() = zoomState.value
        set(value) {
            preferences.putInt(::zoom.name, value.step)
        }
}

private fun String.toPreferredTheme(): PreferredTheme {
    return enumValueOf(this)
}

private class NoOpPreferencesService(
    override var theme: PreferredTheme = PreferredTheme.Auto,
    override var zoom: Zoom = Zoom.Default
) : PreferencesService
