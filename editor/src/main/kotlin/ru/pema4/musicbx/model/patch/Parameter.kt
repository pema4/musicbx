package ru.pema4.musicbx.model.patch

import kotlinx.serialization.Serializable
import kotlin.math.log2
import kotlin.math.pow

@Serializable
data class ModuleParameter(
    val number: Int,
    val name: String,
    val description: String,
    val kind: ModuleParameterKind,
    val default: String,
    val current: String? = null,
)

@Serializable
enum class ModuleParameterKind(
    val min: Float,
    val max: Float,
) {
    Number(min = 0.0f, max = 1.0f),
    HzSlow(min = log2(0.001f), max = log2(200.0f)),
    HzFast(min = log2(20.0f), max = log2(22000.0f)),
    Db(min = 0.0f, max = 1.0f);

    fun normalize(displayValue: String): Float {
        val x = when (this) {
            Number -> displayValue.toFloat()
            HzSlow -> log2(displayValue.toFloat())
            HzFast -> log2(displayValue.toFloat())
            Db -> displayValue.toFloat()
        }

        return normalizeRaw(x)
    }

    private fun normalizeRaw(raw: Float): Float {
        return (raw - min) / (max - min)
    }

    fun display(normalized: Float): String {
        val x = denormalizeRaw(normalized)

        return when (this) {
            Number -> "%,.3f".format(x)
            HzSlow -> "%,.3f".format(2.0.pow(x.toDouble()))
            HzFast -> "%,.1f".format(2.0.pow(x.toDouble()))
            Db -> "%,.3f".format(x)
        }
    }

    private fun denormalizeRaw(raw: Float): Float {
        return raw * (max - min) + min
    }
}
