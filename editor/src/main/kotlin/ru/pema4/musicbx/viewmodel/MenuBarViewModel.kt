package ru.pema4.musicbx.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.pema4.musicbx.ui.MenuBarState
import ru.pema4.musicbx.ui.MenuBarViewModel

class MenuBarViewModelImpl : MenuBarViewModel {
    override var uiState: MenuBarStateImpl by mutableStateOf(MenuBarStateImpl())
        private set

    override fun showOpenDialog() {
        uiState.showingOpenDialog = true
    }

    override fun showSaveDialog() {
        uiState.showingSaveDialog = true
    }

    override fun showSaveAsDialog() {
        uiState.showingSaveAsDialog = true
    }
}

class MenuBarStateImpl : MenuBarState {
    override var showingOpenDialog: Boolean by mutableStateOf(false)
    override var showingSaveDialog: Boolean by mutableStateOf(false)
    override var showingSaveAsDialog: Boolean by mutableStateOf(false)
}
