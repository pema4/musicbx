package ru.pema4.musicbx.ui

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ApplicationScope
import androidx.compose.ui.window.Window
import org.jetbrains.compose.splitpane.ExperimentalSplitPaneApi
import org.jetbrains.compose.splitpane.HorizontalSplitPane
import ru.pema4.musicbx.model.config.NodeDescription
import ru.pema4.musicbx.model.config.NodeUid
import ru.pema4.musicbx.model.patch.TestPatch
import ru.pema4.musicbx.service.ConfigurationService
import ru.pema4.musicbx.service.PreferencesService
import ru.pema4.musicbx.util.FileDialog
import ru.pema4.musicbx.util.FileDialogMode
import ru.pema4.musicbx.util.InstallTooltipManager
import ru.pema4.musicbx.util.MutableTooltipManager
import ru.pema4.musicbx.viewmodel.rememberAppViewModel
import java.awt.Cursor
import java.nio.file.Path

@Composable
fun ApplicationScope.App(
    viewModel: AppViewModel = rememberAppViewModel(),
) {
    EditorTheme {
        Window(
            onCloseRequest = ::exitApplication,
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
        nodeGallery = {
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
        statusBar = {
            StatusBar(viewModel)
        },
        modifier = modifier,
    )
}

@OptIn(ExperimentalSplitPaneApi::class)
@Composable
fun AppWindowLayout(
    nodeGallery: @Composable () -> Unit,
    editor: @Composable ColumnScope.() -> Unit,
    statusBar: @Composable ColumnScope.() -> Unit,
    firstPaneMinSize: Dp = 300.dp,
    secondPanelMinSize: Dp = 100.dp,
    modifier: Modifier = Modifier,
) {
    HorizontalSplitPane(
        modifier = modifier,
    ) {
        first(firstPaneMinSize) {
            nodeGallery()
        }

        second(secondPanelMinSize) {
            Column {
                InstallTooltipManager(MutableTooltipManager()) {
                    editor()
                    statusBar()
                }
            }
        }

        splitter {
            visiblePart {
                Spacer(
                    modifier = Modifier
                        .background(MaterialTheme.colors.onBackground)
                        .width(1.dp)
                        .fillMaxHeight()
                )
            }

            handle {
                val interactionSource = remember { MutableInteractionSource() }
                val isHovered by interactionSource.collectIsHoveredAsState()
                val width by animateDpAsState(
                    targetValue = if (isHovered) 8.dp else 1.dp,
                )

                Spacer(
                    modifier = Modifier
                        .hoverable(interactionSource)
                        .markAsHandle()
                        .pointerHoverIcon(PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)))
                        .background(MaterialTheme.colors.onBackground.copy(alpha = 0.2f))
                        .width(width)
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

    @Composable
    fun collectAvailableNodesAsState(): State<Map<NodeUid, NodeDescription>>

    fun showOpenDialog() = Unit
    fun showSaveDialog() = Unit

    fun reset()
    fun save(path: Path?) = Unit
    fun open(path: Path?) = Unit
}

@Preview
@Composable
fun AppPreview() {
    EditorTheme {
        AppWindow(
            viewModel = rememberAppViewModel(TestPatch)
        )
    }
}
