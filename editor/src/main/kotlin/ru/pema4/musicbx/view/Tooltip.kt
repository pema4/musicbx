package ru.pema4.musicbx.view

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.burnoo.cokoin.get
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.mapLatest
import ru.pema4.musicbx.service.TooltipService
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
@Composable
fun Tooltip(
    tooltipService: TooltipService = get(),
    modifier: Modifier = Modifier,
) {
    val text by snapshotFlow { tooltipService.activeTooltip }
        .mapLatest {
            if (it == null) {
                delay(200.milliseconds)
            }
            it
        }
        .collectAsState(null)

    Tooltip(text, modifier)
}

@Composable
fun Tooltip(
    text: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(20.dp)
            .fillMaxWidth()
            .background(color = Color.LightGray)
            .explainedAs("tooltip")
        // contentAlignment = Alignment.CenterStart,
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.Black)
        )
        Text(
            text = text.orEmpty(),
        )
    }
}

@Composable
fun Modifier.explainedAs(
    text: String?,
): Modifier = composed {
    val tooltipService: TooltipService = get()
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    LaunchedEffect(Unit) {
        snapshotFlow { isHovered }
            .collect { isHovered ->
                if (isHovered) {
                    tooltipService.push(text)
                } else {
                    tooltipService.pop()
                }
            }
    }

    hoverable(interactionSource)
}
