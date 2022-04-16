package ru.pema4.musicbx.model

import androidx.compose.runtime.Immutable

@Immutable
data class Patch(
    val modules: List<Module>,
    val cables: List<Cable>,
)

val DefaultPatch = Patch(
    modules = emptyList(),
    cables = emptyList(),
)
