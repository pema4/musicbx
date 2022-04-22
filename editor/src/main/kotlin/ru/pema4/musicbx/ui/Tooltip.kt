package ru.pema4.musicbx.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapLatest
import ru.pema4.musicbx.service.PlaybackService
import ru.pema4.musicbx.util.LocalTooltipManager
import ru.pema4.musicbx.util.explainedAs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
@Composable
fun Tooltip(
    modifier: Modifier = Modifier,
) {
    val tooltipManager = LocalTooltipManager.current
    val text by snapshotFlow { tooltipManager?.activeTooltip }
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
    Row(
        modifier = modifier
            .height(20.dp)
            .fillMaxWidth()
            .background(color = Color.LightGray)
            .explainedAs("tooltip"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text.orEmpty(),
        )
        Text(
            text = PlaybackService.sampleRate.toString(),
            modifier = Modifier.padding(end = 20.dp),
        )
    }
}
