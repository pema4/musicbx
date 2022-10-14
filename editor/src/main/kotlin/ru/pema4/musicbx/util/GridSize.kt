package ru.pema4.musicbx.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
@JvmInline
value class GridSize(val number: Int)

@Serializable
data class GridOffset(
    val x: GridSize,
    val y: GridSize
) {
    companion object Constants {
        val Zero = GridOffset(x = GridSize(0), y = GridSize(0))
    }
}

fun GridSize.toDp(): Dp {
    return (number * 10).dp
}

fun GridOffset.toDpOffset(): DpOffset {
    return DpOffset(x = x.toDp(), y = y.toDp())
}

fun Dp.toGridSize(): GridSize {
    return GridSize((value / 10).roundToInt())
}

fun DpOffset.toGridOffset(): GridOffset {
    return GridOffset(x = x.toGridSize(), y = y.toGridSize())
}
