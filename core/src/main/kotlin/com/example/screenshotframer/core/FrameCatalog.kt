package com.example.screenshotframer.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.math.abs
import kotlin.math.roundToInt

@Serializable
data class FrameManifest(
    val version: Int,
    val devices: List<DeviceFrame>
)

@Serializable
data class DeviceFrame(
    val id: String,
    val brand: String,
    val name: String,
    val screen: Size,
    val frame: Size,
    val safeArea: RectSpec,
    val colors: List<FrameColor>
)

@Serializable
data class Size(
    val width: Int,
    val height: Int
) {
    val aspectRatio: Double get() = width.toDouble() / height.toDouble()
}

@Serializable
data class RectSpec(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)

@Serializable
data class FrameColor(
    val id: String,
    val name: String,
    val asset: String
)

data class ScreenshotMetadata(
    val width: Int,
    val height: Int
) {
    val aspectRatio: Double get() = width.toDouble() / height.toDouble()
}

object FrameManifestParser {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parse(rawJson: String): FrameManifest = json.decodeFromString(FrameManifest.serializer(), rawJson)
}

enum class MatchConfidence {
    Exact,
    High,
    Medium,
    Low
}

data class DeviceMatch(
    val device: DeviceFrame,
    val confidence: MatchConfidence,
    val score: Double
)

class DeviceMatcher(private val manifest: FrameManifest) {
    fun match(screenshot: ScreenshotMetadata, limit: Int = 3): List<DeviceMatch> {
        return manifest.devices
            .map { device -> score(device, screenshot) }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun score(device: DeviceFrame, screenshot: ScreenshotMetadata): DeviceMatch {
        val exact = device.screen.width == screenshot.width && device.screen.height == screenshot.height
        if (exact) {
            return DeviceMatch(device = device, confidence = MatchConfidence.Exact, score = 1.0)
        }

        val aspectDelta = abs(device.screen.aspectRatio - screenshot.aspectRatio)
        val widthDelta = abs(device.screen.width - screenshot.width).toDouble() / device.screen.width
        val heightDelta = abs(device.screen.height - screenshot.height).toDouble() / device.screen.height
        val score = (1.0 - (aspectDelta * 6.0) - (widthDelta * 0.2) - (heightDelta * 0.2)).coerceIn(0.0, 0.99)
        val confidence = when {
            score >= 0.94 -> MatchConfidence.High
            score >= 0.86 -> MatchConfidence.Medium
            else -> MatchConfidence.Low
        }

        return DeviceMatch(device = device, confidence = confidence, score = score)
    }
}

data class SelectedFrame(
    val device: DeviceFrame,
    val color: FrameColor
)

sealed interface CanvasConfig {
    data object Transparent : CanvasConfig
    data class Solid(val color: Int, val exportWidth: Int = 1800) : CanvasConfig
    data class Gradient(val startColor: Int, val endColor: Int, val exportWidth: Int = 1800) : CanvasConfig
}

sealed interface RenderBackground {
    data object Transparent : RenderBackground
    data class Solid(val color: Int) : RenderBackground
    data class Gradient(val startColor: Int, val endColor: Int) : RenderBackground
}

data class RenderRequest(
    val screenshots: List<ScreenshotMetadata>,
    val selectedFrames: List<SelectedFrame>,
    val canvas: CanvasConfig,
    val paddingRatio: Double = 0.08,
    val gapRatio: Double = 0.04
)

data class RenderLayout(
    val canvasWidth: Int,
    val canvasHeight: Int,
    val background: RenderBackground,
    val items: List<RenderItemLayout>
)

data class RenderItemLayout(
    val selectedFrame: SelectedFrame,
    val frameRect: RectSpec,
    val screenRect: RectSpec
)

object FrameLayoutCalculator {
    fun calculate(request: RenderRequest): RenderLayout {
        require(request.selectedFrames.isNotEmpty()) { "At least one frame is required." }
        require(request.screenshots.size == request.selectedFrames.size) { "Screenshots and selected frames must match." }

        val frames = request.selectedFrames.map { it.device.frame }
        val maxHeight = frames.maxOf { it.height }
        val normalizedWidths = frames.map { frame -> frame.width * (maxHeight.toDouble() / frame.height) }
        val baseGap = maxHeight * request.gapRatio
        val rawContentWidth = normalizedWidths.sum() + baseGap * (frames.size - 1).coerceAtLeast(0)
        val rawPadding = maxHeight * request.paddingRatio
        val targetWidth = when (val canvas = request.canvas) {
            CanvasConfig.Transparent -> (rawContentWidth + rawPadding * 2).roundToInt()
            is CanvasConfig.Solid -> canvas.exportWidth
            is CanvasConfig.Gradient -> canvas.exportWidth
        }
        val scale = if (request.canvas == CanvasConfig.Transparent) {
            1.0
        } else {
            ((targetWidth - rawPadding * 2) / rawContentWidth).coerceAtMost(1.0)
        }
        val contentHeight = maxHeight * scale
        val padding = (contentHeight * request.paddingRatio).roundToInt().coerceAtLeast(24)
        val gap = (contentHeight * request.gapRatio).roundToInt().coerceAtLeast(20)
        val itemHeights = frames.map { (it.height * (contentHeight / maxHeight)).roundToInt() }
        val itemWidths = frames.zip(itemHeights).map { (frame, height) ->
            (frame.width * (height.toDouble() / frame.height)).roundToInt()
        }
        val canvasWidth = when (request.canvas) {
            CanvasConfig.Transparent -> itemWidths.sum() + gap * (itemWidths.size - 1).coerceAtLeast(0) + padding * 2
            is CanvasConfig.Solid -> targetWidth
            is CanvasConfig.Gradient -> targetWidth
        }
        val canvasHeight = itemHeights.max() + padding * 2
        var cursorX = ((canvasWidth - (itemWidths.sum() + gap * (itemWidths.size - 1).coerceAtLeast(0))) / 2.0).roundToInt()
        val items = request.selectedFrames.mapIndexed { index, selected ->
            val width = itemWidths[index]
            val height = itemHeights[index]
            val y = ((canvasHeight - height) / 2.0).roundToInt()
            val frameRect = RectSpec(cursorX, y, width, height)
            val safe = selected.device.safeArea
            val safeScaleX = width.toDouble() / selected.device.frame.width
            val safeScaleY = height.toDouble() / selected.device.frame.height
            val screenRect = RectSpec(
                left = cursorX + (safe.left * safeScaleX).roundToInt(),
                top = y + (safe.top * safeScaleY).roundToInt(),
                width = (safe.width * safeScaleX).roundToInt(),
                height = (safe.height * safeScaleY).roundToInt()
            )
            cursorX += width + gap
            RenderItemLayout(selected, frameRect, screenRect)
        }

        return RenderLayout(
            canvasWidth = canvasWidth,
            canvasHeight = canvasHeight,
            background = when (val canvas = request.canvas) {
                CanvasConfig.Transparent -> RenderBackground.Transparent
                is CanvasConfig.Solid -> RenderBackground.Solid(canvas.color)
                is CanvasConfig.Gradient -> RenderBackground.Gradient(canvas.startColor, canvas.endColor)
            },
            items = items
        )
    }
}

enum class ExportFormat {
    Png,
    Jpeg
}

data class ExportConfig(
    val format: ExportFormat,
    val transparentBackground: Boolean = false,
    val quality: Int = 96
) {
    val supportsAlpha: Boolean get() = format == ExportFormat.Png
    val effectiveTransparentBackground: Boolean get() = transparentBackground && supportsAlpha
    val mimeType: String get() = when (format) {
        ExportFormat.Png -> "image/png"
        ExportFormat.Jpeg -> "image/jpeg"
    }
}
