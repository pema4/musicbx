@file:OptIn(ExperimentalTime::class, FlowPreview::class)

package ru.pema4.musicbx.util

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.FlowPreview
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Composable
fun Scrollable(
    horizontalScrollState: ScrollState? = null,
    verticalScrollState: ScrollState? = null,
    modifier: Modifier = Modifier,
    hideHorizontalScrollbarAutomatically: Boolean = false,
    hideVerticalScrollbarAutomatically: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    var scrollModifier: Modifier = Modifier

    Box(modifier) {
        if (horizontalScrollState != null) {
            scrollModifier = scrollModifier.horizontalScroll(horizontalScrollState)

            AnimatedScrollbarVisibility(
                enabled = hideHorizontalScrollbarAutomatically,
                scrollState = horizontalScrollState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(1.0f)
                    .padding(all = 3.dp)
                    .padding(end = 10.dp),
            ) {
                HorizontalScrollbar(rememberScrollbarAdapter(horizontalScrollState))
            }
        }

        if (verticalScrollState != null) {
            scrollModifier = scrollModifier.verticalScroll(verticalScrollState)

            AnimatedScrollbarVisibility(
                enabled = hideVerticalScrollbarAutomatically,
                scrollState = verticalScrollState,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .zIndex(1.0f)
                    .padding(all = 3.dp),
            ) {
                VerticalScrollbar(rememberScrollbarAdapter(verticalScrollState))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollModifier),
            content = content,
        )
    }
}

@Composable
private fun AnimatedScrollbarVisibility(
    enabled: Boolean,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (enabled) {
        val currentScrollState by rememberUpdatedState(scrollState)
        val scrollChanged by snapshotFlow { IntSize(currentScrollState.value, currentScrollState.maxValue) }
            .wasEmittingIn(1L.seconds)

        AnimatedVisibility(
            visible = scrollChanged,
            modifier = modifier,
            enter = fadeIn(snap()),
            exit = fadeOut(),
        ) {
            content()
        }
    } else {
        Box(
            modifier = modifier,
        ) {
            content()
        }
    }
}
