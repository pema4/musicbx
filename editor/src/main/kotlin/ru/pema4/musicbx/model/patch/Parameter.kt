package ru.pema4.musicbx.model.patch

import kotlinx.serialization.Serializable
import kotlin.math.log2
import kotlin.math.pow

@Serializable
data class NodeParameter(
    val number: Int,
    val name: String,
    val description: String,
    val kind: NodeParameterKind,
    val default: String,
)

@Serializable
enum class NodeParameterKind(
    val min: Float,
    val max: Float,
) {
    Number(min = 0.0f, max = 1.0f),
    HzSlow(min = log2(0.001f), max = log2(200.0f)),
    HzFast(min = log2(20.0f), max = log2(22000.0f)),
    HzWide(min = log2(0.001f), max = log2(22000.0f)),
    Db(min = -120.0f, max = 12.0f);

    fun tryNormalize(displayValue: String): Float? {
        val floatValue = displayValue.toFloatOrNull() ?: return null
        val x = when (this) {
            Number -> floatValue
            HzSlow, HzFast, HzWide -> log2(floatValue)
            Db -> floatValue
        }

        return normalizeRaw(x)
    }

    fun normalize(displayValue: String): Float {
        return tryNormalize(displayValue)
            ?: error("Invalid display value $displayValue for parameter kind $this")
    }

    private fun normalizeRaw(raw: Float): Float {
        return (raw - min) / (max - min)
    }

    fun display(normalized: Float): String {
        val x = denormalizeRaw(normalized)

        return when (this) {
            Number -> "%.3f".format(x)
            HzSlow, HzWide -> "%.3f".format(2.0f.pow(x))
            HzFast -> "%.1f".format(2.0f.pow(x))
            Db -> "%.3f".format(x)
        }
    }

    private fun denormalizeRaw(raw: Float): Float {
        return raw * (max - min) + min
    }
}
