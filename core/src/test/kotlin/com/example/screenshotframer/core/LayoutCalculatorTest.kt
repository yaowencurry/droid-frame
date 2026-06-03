package com.example.screenshotframer.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LayoutCalculatorTest {
    @Test
    fun transparentPresetWrapsFrameWithPadding() {
        val manifest = FrameManifestParser.parse(SAMPLE_MANIFEST)
        val device = manifest.devices.first()

        val layout = FrameLayoutCalculator.calculate(
            request = RenderRequest(
                screenshots = listOf(ScreenshotMetadata(1344, 2992)),
                selectedFrames = listOf(SelectedFrame(device, device.colors.first())),
                canvas = CanvasConfig.Transparent
            )
        )

        assertThat(layout.canvasWidth).isGreaterThan(device.frame.width)
        assertThat(layout.canvasHeight).isGreaterThan(device.frame.height)
        assertThat(layout.items.single().screenRect.width).isEqualTo(device.safeArea.width)
        assertThat(layout.background).isEqualTo(RenderBackground.Transparent)
    }

    @Test
    fun multiDeviceLayoutScalesItemsProportionally() {
        val manifest = FrameManifestParser.parse(SAMPLE_MANIFEST)

        val layout = FrameLayoutCalculator.calculate(
            request = RenderRequest(
                screenshots = manifest.devices.map { ScreenshotMetadata(it.screen.width, it.screen.height) },
                selectedFrames = manifest.devices.map { SelectedFrame(it, it.colors.first()) },
                canvas = CanvasConfig.Solid(color = 0xFF202124.toInt(), exportWidth = 2400)
            )
        )

        assertThat(layout.items).hasSize(2)
        assertThat(layout.canvasWidth).isEqualTo(2400)
        assertThat(layout.items[0].frameRect.height).isNotEqualTo(layout.items[1].frameRect.height)
    }
}
