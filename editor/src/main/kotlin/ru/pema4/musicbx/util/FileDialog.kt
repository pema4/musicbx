package ru.pema4.musicbx.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.AwtWindow
import java.awt.FileDialog
import java.awt.Frame
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.div
import kotlin.io.path.pathString

@Composable
fun FileDialog(
    title: String,
    mode: FileDialogMode,
    directory: Path? = null,
    parent: Frame? = null,
    onCloseRequest: (result: Path?) -> Unit,
) = AwtWindow(
    create = {
        object : FileDialog(parent, title, mode.awtMode) {
            init {
                this.directory = directory?.pathString
            }

            override fun setVisible(value: Boolean) {
                super.setVisible(value)
                if (value) {
                    val path = if (file != null) {
                        Path(this.directory) / file
                    } else {
                        null
                    }
                    onCloseRequest(path)
                }
            }
        }
    },
    dispose = FileDialog::dispose
)

enum class FileDialogMode(
    val awtMode: Int,
) {
    Load(FileDialog.LOAD),
    Save(FileDialog.SAVE)
}
