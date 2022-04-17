package ru.pema4.musicbx.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
sealed interface Socket {
    val number: Int
    val name: String
    val description: String
}

@Immutable
@Serializable
data class InputSocket(
    override val number: Int,
    override val name: String = "In $number",
    override val description: String = "Input $number"
) : Socket

@Serializable
data class OutputSocket(
    override val number: Int,
    override val name: String = "Out $number",
    override val description: String = "Output $number"
) : Socket
