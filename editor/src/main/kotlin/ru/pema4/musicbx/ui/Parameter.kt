package ru.pema4.musicbx.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import ru.pema4.musicbx.model.patch.ModuleParameter
import ru.pema4.musicbx.model.patch.ModuleParameterKind
import ru.pema4.musicbx.util.explainedAs
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class, ExperimentalFoundationApi::class)
@Composable
fun Parameter(
    state: ParameterState,
    modifier: Modifier = Modifier,
) {
    val current = state.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // text будет равен значению
        var text by mutableStateOf(state.model.name)
        LaunchedEffect(current.text) {
            text = current.text
            delay(1.seconds)
            text = state.model.name
        }

        Text(
            text = text,
            style = MaterialTheme.typography.body1,
        )

        Spacer(modifier.width(8.dp))

        Slider(
            value = current.normalized,
            onValueChange = { current.normalized = it },
            modifier = modifier
                .explainedAs("${state.model.description}: ${current.text}")
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onDoubleClick = {},
                    onClick = {},
                ),
        )
    }
}

@Stable
interface ParameterState {
    val model: ModuleParameter
    val current: ParameterValue
    val default: ParameterValue
}

fun ParameterState(
    model: ModuleParameter,
    current: ParameterValue,
    default: ParameterValue,
): ParameterState {
    return object : ParameterState {
        override val model: ModuleParameter = model
        override val current = current
        override val default = default
    }
}

@Stable
interface ParameterValue {
    var normalized: Float
    val text: String
}

fun ParameterValue(
    initialNormalized: Float,
    toDisplay: (Float) -> String = Any::toString,
    onChange: (Float) -> Unit = {},
): ParameterValue {
    return object : ParameterValue {
        var _normalized: Float by mutableStateOf(initialNormalized)
        override var normalized: Float
            get() = _normalized
            set(value) {
                if (value != _normalized) {
                    _normalized = value
                    onChange(value)
                }
            }
        override val text: String by derivedStateOf { toDisplay(normalized) }
    }
}

fun ParameterValue(
    initial: String,
    toNormalized: (String) -> Float,
    toDisplay: (Float) -> String = Any::toString,
    onChange: (Float) -> Unit,
): ParameterValue {
    return ParameterValue(
        initialNormalized = toNormalized(initial),
        toDisplay = toDisplay,
        onChange = onChange,
    )
}

fun ParameterValue(
    initial: String,
    kind: ModuleParameterKind,
    onChange: (Float) -> Unit = {},
): ParameterValue {
    return ParameterValue(
        initial = initial,
        toDisplay = kind::display,
        toNormalized = kind::normalize,
        onChange = onChange,
    )
}

fun ParameterState.toParameter(): ModuleParameter {
    return model.copy(current = current.text)
}
