package ru.pema4.musicbx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalMinimumTouchTargetEnforcement
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.util.HoverTipManagerProvider
import ru.pema4.musicbx.util.MutableHoverTipManager
import ru.pema4.musicbx.util.pointerHoverTip

@Composable
fun NodeGalleryView(
    appViewModel: AppViewModel = AppContext.appViewModel,
    modifier: Modifier = Modifier
) {
    val descriptionsMap by appViewModel.availableNodesService.availableNodes.collectAsState()
    val descriptions by remember {
        derivedStateOf {
            descriptionsMap
                .entries
                .sortedBy { it.key.text }
                .map { it.value }
        }
    }

    NodeGalleryLayout(
        modifier = modifier,
        header = {
            Text(
                text = "Nodes Gallery",
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colors.onBackground,
                style = MaterialTheme.typography.h5
            )
        },
        nodeListContent = {
            items(
                count = descriptions.size,
                key = { descriptions[it].uid }
            ) {
                val description = descriptions[it]
                val editor = AppContext.editorViewModel
                NodeCard(
                    description = description,
                    onClick = {
                        editor.addNode(description)
                        appViewModel.markFileAsChanged()
                    },
                    modifier = Modifier
                        .pointerHoverTip(description.summary)
                        .widthIn(max = 100.dp)
                )
            }
        },
        bottomBar = {
            TipArea(modifier = Modifier.pointerHoverTip())
        }
    )
}

@Composable
private fun NodeGalleryLayout(
    header: @Composable () -> Unit,
    nodeListContent: LazyListScope.() -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    HoverTipManagerProvider(MutableHoverTipManager()) {
        Column(modifier = modifier.fillMaxWidth()) {
            header()
            Divider(Modifier.fillMaxWidth(), thickness = Dp.Hairline)

            LazyColumn(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(8.dp),
                content = nodeListContent
            )

            Divider(Modifier.fillMaxWidth(), thickness = Dp.Hairline)
            bottomBar()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun NodeCard(
    description: NodeDescription,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(
        LocalMinimumTouchTargetEnforcement provides false
    ) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = description.name,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.h6
            )
        }
    }
}
