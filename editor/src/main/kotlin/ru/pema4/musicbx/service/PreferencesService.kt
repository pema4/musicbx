package ru.pema4.musicbx.service

import androidx.compose.runtime.mutableStateOf
import ru.pema4.musicbx.model.preferences.PreferredTheme
import ru.pema4.musicbx.model.preferences.Zoom
import java.util.prefs.Preferences

object PreferencesService {
    private val preferences = Preferences.userNodeForPackage(PreferencesService::class.java)

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
    private var themeState = mutableStateOf(initialTheme)
    var theme: PreferredTheme
        get() = themeState.value
        set(value) {
            preferences.put(::theme.name, value.name)
        }

    private val initialZoomIndex = preferences.getInt(::zoom.name, Zoom.One.index)
    private var zoomState = mutableStateOf(Zoom(initialZoomIndex))
    var zoom: Zoom
        get() = zoomState.value
        set(value) {
            preferences.putInt(::zoom.name, value.index)
        }
}

private fun String.toPreferredTheme(): PreferredTheme {
    return enumValueOf(this)
}
