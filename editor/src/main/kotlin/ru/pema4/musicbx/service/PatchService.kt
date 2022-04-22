package ru.pema4.musicbx.service

import androidx.compose.runtime.Composable
import ru.pema4.musicbx.model.patch.DefaultPatch
import ru.pema4.musicbx.model.patch.Patch

class PatchService {
    private var _activePatch: Patch = DefaultPatch

    val activePatch: Patch
        @Composable get() {
            return _activePatch
        }
}
