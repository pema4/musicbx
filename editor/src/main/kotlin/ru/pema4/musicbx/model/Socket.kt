package ru.pema4.musicbx.model

sealed interface Socket {
    val index: Int
    val name: String
    val description: String
}

data class InputSocket(
    override val index: Int,
    override val name: String = "In $index",
    override val description: String = "Input $index"
) : Socket

data class OutputSocket(
    override val index: Int,
    override val name: String = "Out $index",
    override val description: String = "Output $index"
) : Socket
