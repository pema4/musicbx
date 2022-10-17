package ru.pema4.musicbx.model.preferences

@JvmInline
value class Zoom(val step: Int) {
    val scale: Float get() = zoomSteps[step]

    fun increase(): Zoom {
        return Zoom(step.inc().coerceIn(zoomSteps.indices))
    }

    fun decrease(): Zoom {
        return Zoom(step.dec().coerceIn(zoomSteps.indices))
    }

    companion object {
        private val zoomSteps: List<Float> = listOf(
            0.25f,
            0.33f,
            0.50f,
            0.67f,
            0.75f,
            0.8f,
            0.9f,
            1.0f,
            1.1f,
            1.25f,
            1.5f,
            1.75f,
            2.0f,
            2.5f,
            3f,
            4f,
            5f
        )

        val Default: Zoom = Zoom(step = zoomSteps.indexOf(1.0f))
    }
}
