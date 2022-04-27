package ru.pema4.musicbx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.viewmodel.toModule

@Composable
fun ModuleGalleryView(
    appViewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    val modules by appViewModel.collectAvailableModulesAsState()

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        // horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(10.dp)
    ) {
        items(
            count = modules.size,
            key = { modules[it].uid },
        ) {
            val module = modules[it]
            ModuleView(
                state = module,
                enabled = false,
                modifier = Modifier.widthIn(max = 100.dp),
                onClick = { appViewModel.editorViewModel.addModule(module.toModule()) }
            )
        }
    }
}
