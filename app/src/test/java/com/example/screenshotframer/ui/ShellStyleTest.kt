package com.example.screenshotframer.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ShellStyleTest {
    @Test
    fun metalGradientSpecHasMatchingColorsAndPositions() {
        ShellStyle.entries.forEach { style ->
            assertThat(style.metalGradientColors).hasLength(style.metalGradientPositions.size)
        }
    }

    @Test
    fun metalGradientPositionsCoverFullWidthInOrder() {
        ShellStyle.entries.forEach { style ->
            assertThat(style.metalGradientPositions.first()).isEqualTo(0f)
            assertThat(style.metalGradientPositions.last()).isEqualTo(1f)
            assertThat(style.metalGradientPositions.asList()).isInStrictOrder()
        }
    }

    @Test
    fun defaultMetalGradientMatchesExportReferenceColors() {
        val style = ShellStyle.OppoSilverGreen

        assertThat(style.metalGradientColors.first()).isEqualTo(0xFF171A19.toInt())
        assertThat(style.metalGradientColors[2]).isEqualTo(0xFFEEF2ED.toInt())
        assertThat(style.metalGradientColors[7]).isEqualTo(0xFFE9EEE9.toInt())
        assertThat(style.metalGradientColors.last()).isEqualTo(0xFF151818.toInt())
        assertThat(style.metalGradientPositions[1]).isEqualTo(0.045f)
        assertThat(style.metalGradientPositions[7]).isEqualTo(0.895f)
    }
}
