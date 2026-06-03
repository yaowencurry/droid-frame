package com.example.screenshotframer.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.screenshotframer.core.ExportFormat
import com.example.screenshotframer.core.FrameManifest
import com.example.screenshotframer.ui.BackgroundMode
import com.example.screenshotframer.ui.CanvasRatio
import com.example.screenshotframer.ui.FrameGeometry
import com.example.screenshotframer.ui.FramerItem
import com.example.screenshotframer.ui.ShellStyle
import com.example.screenshotframer.ui.metalGradientColors
import com.example.screenshotframer.ui.metalGradientPositions
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.roundToInt

class FrameExportService(
    private val context: Context,
    @Suppress("unused") private val manifest: FrameManifest
) {
    fun exportBatch(items: List<FramerItem>, config: ExportRenderConfig, format: ExportFormat): ExportResult {
        if (items.isEmpty()) return ExportResult(emptyList())
        val output = render(items, config)
        val file = createOutputFile("framed", format)
        FileOutputStream(file).use { stream ->
            output.compress(
                if (format == ExportFormat.Png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                if (format == ExportFormat.Png) 100 else 94,
                stream
            )
        }
        publishToMediaStore(file, format)
        output.recycle()
        val files = mutableListOf(file)
        if (items.size > 1) {
            files += createZipExport(items, config, format)
        }
        return ExportResult(files = files, shareFile = file)
    }

    private fun render(items: List<FramerItem>, config: ExportRenderConfig): Bitmap {
        val canvasWidth = config.exportWidth
        val canvasHeight = (canvasWidth * config.ratio.heightRatio.toFloat() / config.ratio.widthRatio).roundToInt()
        val output = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        drawBackground(canvas, canvasWidth, canvasHeight, config)

        if (items.size == 1) {
            val phoneHeight = (canvasHeight * FrameGeometry.phoneHeightFraction(config.ratio, config.verticalMarginFraction)).roundToInt()
            val phoneWidth = (phoneHeight * FrameGeometry.PhoneAspectRatio).roundToInt()
            val left = (canvasWidth - phoneWidth) / 2
            val top = (canvasHeight - phoneHeight) / 2
            drawPhone(
                canvas = canvas,
                bounds = RectF(left.toFloat(), top.toFloat(), (left + phoneWidth).toFloat(), (top + phoneHeight).toFloat()),
                item = items.first(),
                style = config.style
            )
        } else {
            val gap = (canvasWidth * 0.035f).roundToInt()
            val phoneHeight = (canvasHeight * minOf(0.70f, FrameGeometry.phoneHeightFraction(config.ratio, config.verticalMarginFraction))).roundToInt()
            val phoneWidth = (phoneHeight * FrameGeometry.PhoneAspectRatio).roundToInt()
            val totalWidth = phoneWidth * items.size + gap * (items.size - 1)
            var left = (canvasWidth - totalWidth) / 2
            val top = (canvasHeight - phoneHeight) / 2
            items.forEach { item ->
                drawPhone(
                    canvas = canvas,
                    bounds = RectF(left.toFloat(), top.toFloat(), (left + phoneWidth).toFloat(), (top + phoneHeight).toFloat()),
                    item = item,
                    style = config.style
                )
                left += phoneWidth + gap
            }
        }
        return output
    }

    private fun drawBackground(canvas: Canvas, width: Int, height: Int, config: ExportRenderConfig) {
        if (config.backgroundMode == BackgroundMode.Image && config.backgroundImageUri != null) {
            val background = decodeUri(config.backgroundImageUri, maxOf(width, height))
            if (background != null) {
                canvas.drawBitmap(
                    background,
                    coverSource(background.width, background.height, width, height),
                    Rect(0, 0, width, height),
                    bitmapPaint()
                )
                background.recycle()
                return
            }
        }
        canvas.drawColor(config.backgroundColor)
    }

    private fun drawPhone(canvas: Canvas, bounds: RectF, item: FramerItem, style: ShellStyle) {
        val scale = bounds.height() / 900f
        val widthScale = bounds.width() / 405f
        val outerRadius = bounds.width() * FrameGeometry.OuterCornerRadiusFraction
        val screenRadius = bounds.width() * FrameGeometry.ScreenCornerRadiusFraction
        val screenInsetX = bounds.width() * FrameGeometry.ScreenInsetHorizontalFraction
        val screenInsetTop = bounds.width() * FrameGeometry.ScreenInsetTopFraction
        val screenInsetBottom = bounds.width() * FrameGeometry.ScreenInsetBottomFraction
        val metalRect = bounds
        val innerRect = metalRect.insetCopy(4.2f * widthScale)
        val screenRect = RectF(
            bounds.left + screenInsetX,
            bounds.top + screenInsetTop,
            bounds.right - screenInsetX,
            bounds.bottom - screenInsetBottom
        )

        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(56, 0, 0, 0)
            maskFilter = BlurMaskFilter(23f * scale, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(bounds.offsetCopy(0f, 18f * scale), outerRadius, outerRadius, shadowPaint)
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(34, 0, 0, 0)
            maskFilter = BlurMaskFilter(46f * scale, BlurMaskFilter.Blur.NORMAL)
            canvas.drawOval(
                RectF(
                    bounds.left + 28f * scale,
                    bounds.bottom - 18f * scale,
                    bounds.right - 28f * scale,
                    bounds.bottom + 42f * scale
                ),
                this
            )
        }

        val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(121, 129, 125)
        }
        val buttonHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(62, 241, 244, 239)
        }
        val buttonWidth = 3.0f * widthScale
        val buttonRadius = 1.8f * widthScale
        val buttonLeft = bounds.right - buttonWidth * 0.15f
        val buttonRight = bounds.right + buttonWidth
        val volumeTop = bounds.top + 122f * scale
        val volumeBottom = bounds.top + 222f * scale
        val powerTop = bounds.top + 253f * scale
        val powerBottom = bounds.top + 322f * scale
        canvas.drawRoundRect(RectF(buttonLeft, volumeTop, buttonRight, volumeBottom), buttonRadius, buttonRadius, buttonPaint)
        canvas.drawRoundRect(RectF(buttonLeft, powerTop, buttonRight, powerBottom), buttonRadius, buttonRadius, buttonPaint)
        val highlightLeft = bounds.right + 0.45f * widthScale
        val highlightRight = bounds.right + 1.15f * widthScale
        canvas.drawRoundRect(RectF(highlightLeft, volumeTop + 10f * scale, highlightRight, volumeBottom - 10f * scale), buttonRadius, buttonRadius, buttonHighlight)
        canvas.drawRoundRect(RectF(highlightLeft, powerTop + 9f * scale, highlightRight, powerBottom - 9f * scale), buttonRadius, buttonRadius, buttonHighlight)

        val metalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = style.metalGradient(bounds.left, bounds.right)
        }
        canvas.drawRoundRect(metalRect, outerRadius, outerRadius, metalPaint)

        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 1.6f * widthScale
            color = Color.argb(56, 255, 255, 255)
            canvas.drawRoundRect(
                metalRect.insetCopy(1.6f * widthScale),
                outerRadius - 1.6f * widthScale,
                outerRadius - 1.6f * widthScale,
                this
            )
        }

        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                innerRect.left,
                0f,
                innerRect.right,
                0f,
                intArrayOf(
                    Color.rgb(5, 6, 6),
                    Color.rgb(10, 11, 11),
                    Color.rgb(1, 2, 2),
                    Color.rgb(10, 11, 11),
                    Color.rgb(5, 6, 6)
                ),
                floatArrayOf(0f, 0.06f, 0.50f, 0.94f, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(innerRect, outerRadius - 4.2f * widthScale, outerRadius - 4.2f * widthScale, this)
        }

        val screenshot = decodeScaled(item, screenRect.width().roundToInt(), screenRect.height().roundToInt())
        val clip = Path().apply { addRoundRect(screenRect, screenRadius, screenRadius, Path.Direction.CW) }
        val save = canvas.save()
        canvas.clipPath(clip)
        canvas.drawBitmap(
            screenshot,
            coverSource(screenshot.width, screenshot.height, screenRect.width().roundToInt(), screenRect.height().roundToInt()),
            screenRect.toRect(),
            bitmapPaint()
        )
        canvas.restoreToCount(save)
        screenshot.recycle()

        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.style = Paint.Style.STROKE
            strokeWidth = 2.0f * widthScale
            color = Color.argb(246, 2, 3, 3)
            canvas.drawRoundRect(screenRect, screenRadius, screenRadius, this)
        }

        val cameraRadius = 6.3f * widthScale
        val cameraY = bounds.top + 26f * widthScale
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(1, 2, 2)
            canvas.drawCircle(bounds.centerX(), cameraY, cameraRadius, this)
            shader = RadialGradient(
                bounds.centerX() - 1.6f * scale,
                cameraY - 1.6f * scale,
                cameraRadius,
                intArrayOf(Color.rgb(74, 99, 112), Color.rgb(5, 7, 7)),
                floatArrayOf(0.0f, 1.0f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(bounds.centerX(), cameraY, cameraRadius * 0.68f, this)
            shader = null
            color = Color.rgb(17, 25, 31)
            canvas.drawCircle(bounds.centerX() - 1.4f * scale, cameraY - 1.4f * scale, 1.6f * scale, this)
        }
    }

    private fun decodeScaled(item: FramerItem, targetWidth: Int, targetHeight: Int): Bitmap {
        val targetMax = maxOf(targetWidth, targetHeight)
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize(maxOf(item.screenshot.width, item.screenshot.height), targetMax)
        }
        return context.contentResolver.openInputStream(item.uri).use { input ->
            requireNotNull(BitmapFactory.decodeStream(input, null, options)) { "Unable to decode ${item.uri}" }
        }
    }

    private fun decodeUri(uri: Uri, targetMax: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inSampleSize = sampleSize(maxOf(bounds.outWidth, bounds.outHeight), targetMax)
        }
        return context.contentResolver.openInputStream(uri).use { input ->
            BitmapFactory.decodeStream(input, null, options)
        }
    }

    private fun sampleSize(sourceMax: Int, targetMax: Int): Int {
        var sample = 1
        while (sourceMax / sample > targetMax * 2) sample *= 2
        return sample
    }

    private fun createZipExport(items: List<FramerItem>, config: ExportRenderConfig, format: ExportFormat): File {
        val zipFile = createNamedFile("framed-batch", "zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            items.forEachIndexed { index, item ->
                val bitmap = render(listOf(item), config)
                val extension = if (format == ExportFormat.Png) "png" else "jpg"
                zip.putNextEntry(ZipEntry("framed-${index + 1}.$extension"))
                bitmap.compress(
                    if (format == ExportFormat.Png) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG,
                    if (format == ExportFormat.Png) 100 else 94,
                    zip
                )
                zip.closeEntry()
                bitmap.recycle()
            }
        }
        return zipFile
    }

    private fun createOutputFile(prefix: String, format: ExportFormat): File {
        val extension = if (format == ExportFormat.Png) "png" else "jpg"
        return createNamedFile(prefix, extension)
    }

    private fun createNamedFile(prefix: String, extension: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
        return File(dir, "$prefix-$timestamp.$extension")
    }

    private fun publishToMediaStore(file: File, format: ExportFormat) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, if (format == ExportFormat.Png) "image/png" else "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Screenshot Framer")
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        context.contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { input -> input.copyTo(output) }
        }
    }
}

data class ExportRenderConfig(
    val style: ShellStyle,
    val ratio: CanvasRatio,
    val backgroundMode: BackgroundMode,
    val backgroundColor: Int,
    val backgroundImageUri: Uri?,
    val exportWidth: Int = 1800,
    val verticalMarginFraction: Float = FrameGeometry.DefaultVerticalMarginFraction
)

data class ExportResult(
    val files: List<File>,
    val shareFile: File? = files.firstOrNull()
)

private fun ShellStyle.metalGradient(left: Float, right: Float): LinearGradient {
    return LinearGradient(left, 0f, right, 0f, metalGradientColors, metalGradientPositions, Shader.TileMode.CLAMP)
}

private fun RectF.insetCopy(value: Float): RectF = RectF(left + value, top + value, right - value, bottom - value)

private fun RectF.offsetCopy(dx: Float, dy: Float): RectF = RectF(left + dx, top + dy, right + dx, bottom + dy)

private fun RectF.toRect(): Rect = Rect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())

private fun coverSource(sourceWidth: Int, sourceHeight: Int, targetWidth: Int, targetHeight: Int): Rect {
    val sourceRatio = sourceWidth.toFloat() / sourceHeight
    val targetRatio = targetWidth.toFloat() / targetHeight
    return if (sourceRatio > targetRatio) {
        val cropWidth = (sourceHeight * targetRatio).roundToInt()
        val left = ((sourceWidth - cropWidth) / 2f).roundToInt()
        Rect(left, 0, left + cropWidth, sourceHeight)
    } else {
        val cropHeight = (sourceWidth / targetRatio).roundToInt()
        val top = ((sourceHeight - cropHeight) / 2f).roundToInt()
        Rect(0, top, sourceWidth, top + cropHeight)
    }
}

private fun bitmapPaint(): Paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
