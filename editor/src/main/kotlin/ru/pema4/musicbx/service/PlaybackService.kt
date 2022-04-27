package ru.pema4.musicbx.service

import ru.pema4.musicbx.model.patch.CableFrom
import ru.pema4.musicbx.model.patch.CableTo

object PlaybackService {
    fun start() = Unit

    fun stop() = Unit

    external fun addModule(uid: String, id: Int)

    external fun removeModule(moduleId: Int)

    private external fun connectModules(from: Int, fromOutput: Int, to: Int, toInput: Int)
    fun connectModules(from: CableFrom, to: CableTo) {
        connectModules(
            from = from.moduleId,
            fromOutput = from.socketNumber,
            to = to.moduleId,
            toInput = to.socketNumber,
        )
    }

    private external fun disconnectModules(from: Int, fromOutput: Int, to: Int, toInput: Int)
    fun disconnectModules(from: CableFrom, to: CableTo) {
        disconnectModules(
            from = from.moduleId,
            fromOutput = from.socketNumber,
            to = to.moduleId,
            toInput = to.socketNumber,
        )
    }
}
