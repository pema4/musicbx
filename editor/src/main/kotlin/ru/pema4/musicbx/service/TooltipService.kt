package ru.pema4.musicbx.service

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf

class TooltipService {
    private val tooltips = mutableStateListOf<String?>("Test tooltip!")

    val activeTooltip: String? by derivedStateOf { tooltips.lastOrNull() }

    fun push(text: String?) {
        tooltips += text
    }

    fun pop() {
        tooltips.removeLastOrNull()
    }
}
