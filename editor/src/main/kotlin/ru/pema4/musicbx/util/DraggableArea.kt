package ru.pema4.musicbx.util

import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.dp

@Composable
fun DraggableArea(
    modifier: Modifier = Modifier,
    content: @Composable DraggableAreaScope.() -> Unit,
) {
    Layout(
        modifier = modifier,
        content = { DraggableAreaScope.content() },
    ) { measurables, constraints ->
        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        layout(constraints.maxWidth, constraints.maxHeight) {
            placeables.forEachIndexed { index, placeable ->
                val measurable = measurables[index]
                val position = measurable.parentData as IntOffset
                placeable.place(position)
            }
        }
    }
}

@LayoutScopeMarker
object DraggableAreaScope {
    fun Modifier.dragTo(offset: DpOffset): Modifier {
        return then(DragToModifier(offset))
    }
}

private data class DragToModifier(
    val offset: DpOffset,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): IntOffset {
        return IntOffset(x = offset.x.roundToPx(), y = offset.y.roundToPx())
    }
}

fun Modifier.offsetByPadding(offset: DpOffset): Modifier {
    return padding(
        start = offset.x.coerceAtLeast(0.0.dp),
        end = (-offset.x).coerceAtLeast(0.0.dp),
        top = offset.y.coerceAtLeast(0.0.dp),
        bottom = (-offset.y).coerceAtLeast(0.0.dp),
    )
}
