package ru.pema4.musicbx.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ModuleGalleryView(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 10.dp)
    ) {
        val modules = appViewModel.editorViewModel.modules
        items(
            count = modules.size,
            key = { modules[it].id },
        ) {
            val module = modules[it]
            ModuleView(
                state = module,
                modifier = Modifier
                    .combinedClickable(
                        enabled = true,
                        onDoubleClick = {
                            appViewModel.editorViewModel.addModule(module.toModule())
                        },
                        onClick = {},
                    )
                    .scale(1.0f),
            )
        }
    }
}
