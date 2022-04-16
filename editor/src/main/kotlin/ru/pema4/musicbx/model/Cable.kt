package ru.pema4.musicbx.model

import androidx.compose.runtime.Stable

@Stable
data class Cable(
    val from: CableFrom,
    val to: CableTo,
)

@Stable
sealed interface CableEnd {
    val moduleId: Int
    val socketNumber: Int
}

data class CableFrom(
    override val moduleId: Int,
    override val socketNumber: Int,
) : CableEnd

data class CableTo(
    override val moduleId: Int,
    override val socketNumber: Int,
) : CableEnd
