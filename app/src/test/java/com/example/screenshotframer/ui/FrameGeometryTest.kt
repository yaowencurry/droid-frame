package com.example.screenshotframer.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FrameGeometryTest {
    @Test
    fun phoneAspectRatioTracksMainstreamTallScreens() {
        assertThat(FrameGeometry.PhoneAspectRatio).isWithin(0.001f).of(9f / 19.5f)
    }

    @Test
    fun verticalMarginUsesSymmetricSliderValue() {
        assertThat(FrameGeometry.phoneHeightFraction(CanvasRatio.Ratio3x4, 0.10f)).isWithin(0.001f).of(0.80f)
        assertThat(FrameGeometry.phoneHeightFraction(CanvasRatio.Ratio3x4, 0.18f)).isWithin(0.001f).of(0.64f)
    }

    @Test
    fun verticalMarginIsClampedToUsableRange() {
        assertThat(FrameGeometry.normalizedVerticalMargin(-1f)).isEqualTo(FrameGeometry.MinVerticalMarginFraction)
        assertThat(FrameGeometry.normalizedVerticalMargin(1f)).isEqualTo(FrameGeometry.MaxVerticalMarginFraction)
    }

    @Test
    fun previewLayoutChangesCanvasSizeWhenRatioChanges() {
        val threeByFour = FrameGeometry.previewLayout(
            containerWidth = 360f,
            maxHeight = 320f,
            ratio = CanvasRatio.Ratio3x4,
            verticalMarginFraction = 0.10f
        )
        val square = FrameGeometry.previewLayout(
            containerWidth = 360f,
            maxHeight = 320f,
            ratio = CanvasRatio.Ratio1x1,
            verticalMarginFraction = 0.10f
        )

        assertThat(threeByFour.canvasWidth).isWithin(0.001f).of(240f)
        assertThat(threeByFour.canvasHeight).isWithin(0.001f).of(320f)
        assertThat(square.canvasWidth).isWithin(0.001f).of(320f)
        assertThat(square.canvasHeight).isWithin(0.001f).of(320f)
        assertThat(threeByFour.phoneWidthFraction).isNotEqualTo(square.phoneWidthFraction)
    }

    @Test
    fun phoneShellGeometryMatchesLargeCornerThinBezelTarget() {
        assertThat(FrameGeometry.OuterCornerRadiusFraction).isWithin(0.001f).of(0.144f)
        assertThat(FrameGeometry.ScreenCornerRadiusFraction).isWithin(0.001f).of(0.118f)
        assertThat(FrameGeometry.ScreenInsetHorizontalFraction).isWithin(0.001f).of(0.023f)
        assertThat(FrameGeometry.ScreenInsetTopFraction).isWithin(0.001f).of(0.026f)
        assertThat(FrameGeometry.ScreenInsetBottomFraction).isWithin(0.001f).of(0.020f)
    }
}
