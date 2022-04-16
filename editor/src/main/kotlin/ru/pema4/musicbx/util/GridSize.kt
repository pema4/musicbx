package ru.pema4.musicbx.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

@JvmInline
value class GridSize(val number: Int)

data class GridOffset(
    val x: GridSize,
    val y: GridSize,
)

fun GridSize.toDp(): Dp = (number * 10).dp

fun GridOffset.toDpOffset(): DpOffset =
    DpOffset(x = x.toDp(), y = y.toDp())
