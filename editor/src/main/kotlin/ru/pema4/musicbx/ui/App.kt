@file:OptIn(ExperimentalTime::class)

package ru.pema4.musicbx.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import kotlinx.coroutines.delay
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.model.config.NodeUid
import ru.pema4.musicbx.model.patch.TestPatch
import ru.pema4.musicbx.service.ConfigurationService
import ru.pema4.musicbx.service.PreferencesService
import ru.pema4.musicbx.util.FileDialog
import ru.pema4.musicbx.util.FileDialogMode
import ru.pema4.musicbx.util.HoverTipManagerProvider
import ru.pema4.musicbx.util.MutableHoverTipManager
import ru.pema4.musicbx.util.tipOnHover
import ru.pema4.musicbx.viewmodel.rememberAppViewModel
import java.awt.Cursor
import java.nio.file.Path
import kotlin.io.path.nameWithoutExtension
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

@Composable
fun ApplicationScope.App(
    viewModel: AppViewModel = rememberAppViewModel(),
) {
    val title by remember {
        derivedStateOf {
            val title = viewModel.openedFile?.nameWithoutExtension ?: "New Patch"
            val changedIcon = if (viewModel.editor.changed) {
                "\u25cf "
            } else {
                ""
            }

            "$changedIcon$title"
        }
    }

    EditorTheme {
        Window(
            onCloseRequest = ::exitApplication,
            title = title,
        ) {
            AppMenuBar(viewModel)
            AppDialogWindows(viewModel)
            AppWindow(
                viewModel = viewModel,
                modifier = Modifier.background(MaterialTheme.colors.background),
            )
        }
    }
}

@Composable
fun AppWindow(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    AppWindowLayout(
        sideBar = {
            NodeGalleryView(
                appViewModel = viewModel,
                modifier = Modifier
                    .fillMaxSize(),
            )
        },
        editor = {
            EditorView(
                viewModel = viewModel.editor,
                modifier = Modifier
                    .weight(1.0f)
            )
        },
        bottomBar = {
            EditorTipArea(
                appViewModel = viewModel,
                modifier = Modifier.tipOnHover(),
            )
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun AppWindowLayout(
    sideBar: @Composable () -> Unit,
    editor: @Composable ColumnScope.() -> Unit,
    bottomBar: @Composable ColumnScope.() -> Unit,
    firstPaneMinSize: Dp = 300.dp,
    secondPanelMinSize: Dp = 100.dp,
    modifier: Modifier = Modifier,
) {
    HorizontalSplitPane(
        modifier = modifier,
    ) {
        first(firstPaneMinSize) {
            sideBar()
        }

        second(secondPanelMinSize) {
            Column {
                HoverTipManagerProvider(MutableHoverTipManager()) {
                    editor()

                    Divider(Modifier.fillMaxWidth(), thickness = Dp.Hairline)

                    bottomBar()
                }
            }
        }

        splitter {
            visiblePart {
                Divider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                )
            }

            handle {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val transparency = remember { Animatable(0.0f) }

                LaunchedEffect(isHovered) {
                    if (isHovered) {
                        delay(0.3.seconds)
                        transparency.animateTo(1.0f)
                    } else {
                        transparency.animateTo(0.0f)
                    }
                }

                val dividerColor = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
                val bigDividerColor by remember {
                    derivedStateOf {
                        val alpha = dividerColor.alpha * transparency.value
                        dividerColor.copy(alpha = alpha)
                    }
                }

                Spacer(
                    modifier = Modifier
                        .hoverable(interactionSource)
                        .markAsHandle()
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                        .background(bigDividerColor)
                        .width(8.dp)
                        .fillMaxHeight()
                )

                Spacer(
                    modifier = Modifier
                        .background(dividerColor)
                        .width(0.dp)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun AppDialogWindows(
    viewModel: AppViewModel,
) {
    if (viewModel.showingOpenDialog) {
        FileDialog(
            title = "Choose a file",
            mode = FileDialogMode.Load,
        ) { path ->
            viewModel.open(path)
        }
    }

    if (viewModel.showingSaveDialog) {
        FileDialog(
            title = "Saving a file",
            mode = FileDialogMode.Save,
        ) { path ->
            viewModel.save(path)
        }
    }
}

@Stable
interface AppViewModel {
    val editor: EditorViewModel
    val showingOpenDialog: Boolean
    val showingSaveDialog: Boolean
    val preferences: PreferencesService
    val configuration: ConfigurationService
    val openedFile: Path?

    @Composable
    fun collectAvailableNodesAsState(): State<Map<NodeUid, NodeDescription>>

    fun showOpenDialog() = Unit
    fun showSaveDialog() = Unit

    fun reset()
    fun save(file: Path?) = Unit
    fun open(file: Path?) = Unit
}

@Preview
@Composable
private fun AppPreview() {
    EditorTheme {
        AppWindow(
            viewModel = rememberAppViewModel(TestPatch)
        )
    }
}
