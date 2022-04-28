package ru.pema4.musicbx.model.preferences

@JvmInline
value class Zoom(val index: Int) {
    val scale: Float get() = zoomScales[index]

    operator fun inc(): Zoom {
        return Zoom(index.inc().coerceIn(zoomScales.indices))
    }

    operator fun dec(): Zoom {
        return Zoom(index.dec().coerceIn(zoomScales.indices))
    }

    companion object {
        private val zoomScales: List<Float> = listOf(
            0.25f,
            1f / 3f,
            0.50f,
            2f / 3f,
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
            5f,
        )

        val One: Zoom = Zoom(index = zoomScales.indexOf(1.0f))
    }
}
