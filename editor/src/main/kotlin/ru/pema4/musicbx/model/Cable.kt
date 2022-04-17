package ru.pema4.musicbx.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Cable(
    val from: CableFrom,
    val to: CableTo,
)

@Immutable
sealed interface CableEnd {
    val moduleId: Int
    val socketNumber: Int
}

@Immutable
@Serializable
data class CableFrom(
    override val moduleId: Int,
    override val socketNumber: Int,
) : CableEnd

@Immutable
@Serializable
data class CableTo(
    override val moduleId: Int,
    override val socketNumber: Int,
) : CableEnd
