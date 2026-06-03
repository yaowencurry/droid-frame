package com.example.screenshotframer.ui

object FrameGeometry {
    const val PhoneAspectRatio = 9f / 19.5f
    const val DefaultVerticalMarginFraction = 0.10f
    const val MinVerticalMarginFraction = 0f
    const val MaxVerticalMarginFraction = 0.22f
    const val OuterCornerRadiusFraction = 0.144f
    const val ScreenCornerRadiusFraction = 0.118f
    const val ScreenInsetHorizontalFraction = 0.023f
    const val ScreenInsetTopFraction = 0.026f
    const val ScreenInsetBottomFraction = 0.020f

    fun normalizedVerticalMargin(value: Float): Float =
        value.coerceIn(MinVerticalMarginFraction, MaxVerticalMarginFraction)

    fun phoneHeightFraction(ratio: CanvasRatio, verticalMarginFraction: Float): Float {
        val available = 1f - normalizedVerticalMargin(verticalMarginFraction) * 2f
        val maxForRatio = when (ratio) {
            CanvasRatio.Ratio1x1 -> 0.82f
            CanvasRatio.Ratio9x16 -> 0.70f
            else -> 0.86f
        }
        return minOf(available, maxForRatio)
    }

    fun previewLayout(
        containerWidth: Float,
        maxHeight: Float,
        ratio: CanvasRatio,
        verticalMarginFraction: Float
    ): PreviewLayout {
        val canvasRatio = ratio.widthRatio.toFloat() / ratio.heightRatio
        val canvasHeightByWidth = containerWidth / canvasRatio
        val canvasHeight = minOf(canvasHeightByWidth, maxHeight)
        val canvasWidth = canvasHeight * canvasRatio
        val phoneHeightFraction = phoneHeightFraction(ratio, verticalMarginFraction)
        val phoneHeight = canvasHeight * phoneHeightFraction
        val phoneWidth = phoneHeight * PhoneAspectRatio
        return PreviewLayout(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            phoneWidthFraction = (phoneWidth / canvasWidth).coerceAtMost(0.92f)
        )
    }
}

data class PreviewLayout(
    val canvasWidth: Float,
    val canvasHeight: Float,
    val phoneWidthFraction: Float
)
