package ru.pema4.musicbx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.model.config.NodeDescription

@Composable
fun NodeGalleryView(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val descriptionsMap by appViewModel.collectAvailableNodesAsState()
    val descriptions by derivedStateOf {
        descriptionsMap
            .entries
            .sortedBy { it.key.text }
            .map { it.value }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Nodes Gallery",
            modifier = Modifier.padding(8.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h5,
        )

        Divider(
            modifier = Modifier.fillMaxWidth(),
        )

        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
        ) {
            items(
                count = descriptions.size,
                key = { descriptions[it].uid },
            ) {
                val description = descriptions[it]
                NodeCard(
                    description = description,
                    modifier = Modifier
                        .widthIn(max = 100.dp)
                        .clickable { appViewModel.editor.addNode(description) },
                )
            }
        }
    }
}

@Composable
private fun NodeCard(
    description: NodeDescription,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = description.name,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.h6,
        )
    }
}
