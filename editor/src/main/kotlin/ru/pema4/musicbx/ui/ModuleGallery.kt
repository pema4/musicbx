package ru.pema4.musicbx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Module Gallery",
            modifier = Modifier.padding(8.dp),
            color = MaterialTheme.colors.onBackground,
            style = MaterialTheme.typography.h5,
        )

        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            // horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(8.dp)
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
}
