package ru.pema4.musicbx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import ru.pema4.musicbx.util.LocalHoverTipManager

@Composable
fun EditorTipArea(
    appViewModel: AppViewModel = AppContext.appViewModel,
    modifier: Modifier = Modifier
) {
    val outputSettings by appViewModel.configuration.output.collectAsState()
    val sampleRate = outputSettings.sampleRate?.current.toString()

    Row(
        modifier = modifier
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TipArea()

        BoxWithConstraints(
            modifier = Modifier.weight(1.0f)
        ) {
            if (maxWidth > 80.dp) {
                Text(
                    text = sampleRate,
                    color = MaterialTheme.colors.onSurface,
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TipArea(
    tip: String? = LocalHoverTipManager.current?.activeTooltip,
    modifier: Modifier = Modifier
) {
    Text(
        text = tip.orEmpty(),
        color = MaterialTheme.colors.onSurface,
        style = MaterialTheme.typography.body2,
        modifier = modifier.padding(4.dp)
    )
}
