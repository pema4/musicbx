@file:OptIn(ExperimentalTime::class, FlowPreview::class)

package ru.pema4.musicbx.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@Composable
fun Flow<*>.wasEmittingIn(timeout: Duration): State<Boolean> {
    return produceState(initialValue = true) {
        onEach { value = true }
            .debounce(timeout)
            .onEach { value = false }
            .collect()
    }
}
