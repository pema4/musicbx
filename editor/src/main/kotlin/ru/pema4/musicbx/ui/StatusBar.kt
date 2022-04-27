package ru.pema4.musicbx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.mapLatest
import ru.pema4.musicbx.util.LocalTooltipManager
import ru.pema4.musicbx.util.explainedAs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
@Composable
fun StatusBar(
    appViewModel: AppViewModel,
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

    val ioSettings by appViewModel.collectIoSettingsAsState()

    StatusBar(
        text = text,
        sampleRate = ioSettings?.output?.sampleRate?.current.toString(),
        modifier = modifier
    )
}

@Composable
fun StatusBar(
    text: String?,
    sampleRate: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .explainedAs("The status bar"),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text.orEmpty(),
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(4.dp),
        )
        Text(
            text = sampleRate,
            color = MaterialTheme.colors.onSurface,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.padding(end = 4.dp),
        )
    }
}
