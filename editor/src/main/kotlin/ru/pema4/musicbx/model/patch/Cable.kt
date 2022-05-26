package ru.pema4.musicbx.model.patch

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class Cable(
    val from: CableFrom,
    val to: CableTo,
)

@Immutable
sealed interface CableEnd {
    val nodeId: Int
    val socketName: String
}

@Immutable
@Serializable
data class CableFrom(
    @SerialName("node_id")
    override val nodeId: Int,
    @SerialName("socket_name")
    override val socketName: String,
) : CableEnd

@Immutable
@Serializable
data class CableTo(
    @SerialName("node_id")
    override val nodeId: Int,
    @SerialName("socket_name")
    override val socketName: String,
) : CableEnd
