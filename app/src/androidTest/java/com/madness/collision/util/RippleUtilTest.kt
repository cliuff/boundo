package com.madness.collision.util

import com.google.common.truth.Truth

internal class RippleUtilTest {

    @org.junit.jupiter.api.Test
    fun lightenOrDarken() {
        val color = ColorUtil.lightenOrDarken(0xFFbc9bff.toInt(), 0.9f)
        print(color.toString(16))
        Truth.assertThat(color).isNull()
    }
}
