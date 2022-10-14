package ru.pema4.musicbx.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import kotlinx.coroutines.launch

fun Modifier.diagonallyDraggable(
    key1: Any? = null,
    offset: DpOffset,
    onChange: (DpOffset) -> Unit
): Modifier = composed {
    val coroutineScope = rememberCoroutineScope()
    var rawNodeOffset by remember(key1) { mutableStateOf(offset) }
    var initialOffset by remember(key1) { mutableStateOf(offset) }
    val currentOffset by rememberUpdatedState(offset)
    val appliedOffsetFraction = remember(key1) { Animatable(1.0f) }

    val appliedOffset by remember(key1) {
        derivedStateOf {
            lerp(initialOffset, currentOffset, appliedOffsetFraction.value)
        }
    }

    absoluteOffset(x = (currentOffset.x - appliedOffset.x), y = (currentOffset.y - appliedOffset.y))
        .absolutePadding(left = appliedOffset.x, top = appliedOffset.y)
        .pointerInput(key1) {
            detectDragGestures(
                onDragStart = {
                    initialOffset = currentOffset
                    coroutineScope.launch {
                        appliedOffsetFraction.animateTo(0.0f, snap())
                    }
                },
                onDragEnd = {
                    coroutineScope.launch { appliedOffsetFraction.animateTo(1.0f) }
                },
                onDragCancel = {
                    coroutineScope.launch { appliedOffsetFraction.animateTo(1.0f) }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    rawNodeOffset += DpOffset(dragAmount.x.toDp(), dragAmount.y.toDp())
                    val targetOffset = DpOffset(
                        x = rawNodeOffset.x.coerceAtLeast(0.dp),
                        y = rawNodeOffset.y.coerceAtLeast(0.dp)
                    )
                    onChange(targetOffset)
                }
            )
        }
}

fun Modifier.pointerMoveFilter(
    onMove: Density.(Offset) -> Boolean
): Modifier {
    return pointerInput(onMove) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val consumed = when (event.type) {
                    PointerEventType.Move -> {
                        onMove(event.changes.first().position)
                    }
                    else -> false
                }
                if (consumed) {
                    event.changes.forEach { it.consumeAllChanges() }
                }
            }
        }
    }
}
