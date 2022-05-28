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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.util.LocalTooltipManager
import ru.pema4.musicbx.util.explainedAs

@Composable
fun StatusBar(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val tooltipManager = LocalTooltipManager.current
    val text = tooltipManager?.activeTooltip
    val outputSettings by appViewModel.configuration.output.collectAsState()

    StatusBar(
        text = text,
        sampleRate = outputSettings.sampleRate?.current.toString(),
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
