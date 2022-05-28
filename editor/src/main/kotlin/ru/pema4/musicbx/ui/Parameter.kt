package ru.pema4.musicbx.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.DropdownMenu
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import ru.pema4.musicbx.model.patch.NodeParameter
import ru.pema4.musicbx.model.patch.NodeParameterKind
import ru.pema4.musicbx.util.explainedAs

@Composable
fun Parameter(
    state: ParameterState,
    modifier: Modifier = Modifier,
) {
    val current = state.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        var showDropdown by remember { mutableStateOf(false) }

        Text(
            text = state.parameter.name,
            style = MaterialTheme.typography.body1,
            modifier = Modifier.clickable { showDropdown = true },
        )

        ParameterValueDropdown(
            state = state,
            enabled = showDropdown,
            onDismissRequest = { lastValidValue: String? ->
                if (lastValidValue != null) {
                    current.text = lastValidValue
                }
                showDropdown = false
            },
        )

        Spacer(modifier.width(8.dp))

        Slider(
            value = current.normalized,
            onValueChange = { current.normalized = it },
            modifier = modifier
                .explainedAs("${state.parameter.description}: ${current.text}")
        )
    }
}

@Composable
@OptIn(ExperimentalComposeUiApi::class)
private fun ParameterValueDropdown(
    state: ParameterState,
    enabled: Boolean,
    onDismissRequest: (lastValidValue: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val initialText = state.current.text
    var textFieldValue by remember(initialText) {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(0, Int.MAX_VALUE),
            )
        )
    }

    val isError by derivedStateOf {
        state.parameter.kind.tryNormalize(textFieldValue.text) == null
    }

    DropdownMenu(
        expanded = enabled,
        onDismissRequest = { onDismissRequest(null) },
        modifier = modifier
            .onPreviewKeyEvent {
                if (it.key == Key.Enter) {
                    onDismissRequest(textFieldValue.text.takeIf { !isError })
                    true
                } else {
                    false
                }
            }
    ) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        TextField(
            value = textFieldValue,
            onValueChange = { textFieldValue = it },
            isError = isError,
            modifier = Modifier.focusRequester(focusRequester)
        )
    }
}

@Stable
interface ParameterState {
    val parameter: NodeParameter
    val current: ParameterValue
    val default: ParameterValue
}

fun ParameterState(
    model: NodeParameter,
    current: ParameterValue,
    default: ParameterValue,
): ParameterState {
    return object : ParameterState {
        override val parameter: NodeParameter = model
        override val current = current
        override val default = default
    }
}

@Stable
interface ParameterValue {
    var normalized: Float
    var text: String
}

fun ParameterValue(
    initialNormalized: Float,
    toDisplay: (Float) -> String = Any::toString,
    toNormalized: (String) -> Float = String::toFloat,
    onChange: (Float) -> Unit = {},
): ParameterValue {
    return object : ParameterValue {
        var _normalized: Float by mutableStateOf(initialNormalized)
        val _text by derivedStateOf { toDisplay(normalized) }
        override var normalized: Float
            get() = _normalized
            set(value) {
                if (value != _normalized) {
                    _normalized = value
                    onChange(value)
                }
            }
        override var text: String
            get() = _text
            set(value) {
                normalized = toNormalized(value)
            }
    }
}

fun ParameterValue(
    initial: String,
    kind: NodeParameterKind,
    onChange: (Float) -> Unit = {},
): ParameterValue {
    return ParameterValue(
        initialNormalized = kind.normalize(initial),
        toDisplay = kind::display,
        toNormalized = kind::normalize,
        onChange = onChange,
    )
}
