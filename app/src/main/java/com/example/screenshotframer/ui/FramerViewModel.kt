package com.example.screenshotframer.ui

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.screenshotframer.core.DeviceFrame
import com.example.screenshotframer.core.DeviceMatcher
import com.example.screenshotframer.core.ExportFormat
import com.example.screenshotframer.core.FrameColor
import com.example.screenshotframer.core.FrameManifestParser
import com.example.screenshotframer.core.MatchConfidence
import com.example.screenshotframer.core.ScreenshotMetadata
import com.example.screenshotframer.export.ExportRenderConfig
import com.example.screenshotframer.export.FrameExportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class FramerViewModel(
    private val appContext: Context,
    initialUris: List<Uri>
) : ViewModel() {
    private val manifest = appContext.assets.open("frames/manifest.json").bufferedReader().use {
        FrameManifestParser.parse(it.readText())
    }
    private val matcher = DeviceMatcher(manifest)
    private val exportService = FrameExportService(appContext, manifest)
    private val _state = MutableStateFlow(FramerUiState())
    val state: StateFlow<FramerUiState> = _state

    init {
        importUris(initialUris)
    }

    fun importUris(uris: List<Uri>) {
        if (uris.isEmpty()) {
            _state.update { it.copy(isImporting = false, message = "没有选择图片。") }
            return
        }
        _state.update { it.copy(isImporting = true, screen = FramerScreen.Editing, message = "正在读取图片...") }
        viewModelScope.launch(Dispatchers.IO) {
            val imported = uris.map { uri -> runCatching { createItem(uri) } }
            val items = imported.mapNotNull { it.getOrNull() }
            val failed = imported.count { it.isFailure || it.getOrNull() == null }
            _state.update { current ->
                current.resetForNewImport().copy(
                    items = items,
                    screen = if (items.isNotEmpty()) FramerScreen.Editing else FramerScreen.Home,
                    isImporting = false,
                    message = when {
                        items.isNotEmpty() && failed == 0 -> "已生成默认套壳预览。"
                        items.isNotEmpty() -> "已导入 ${items.size} 张，$failed 张读取失败。"
                        else -> "读取失败：请选择系统相册中的 PNG/JPEG 截图。"
                    }
                )
            }
        }
    }

    fun clear() {
        _state.update {
            it.copy(
                items = emptyList(),
                screen = FramerScreen.Home,
                isImporting = false,
                latestExport = null,
                backgroundImageUri = null,
                message = "请选择一张截图。"
            )
        }
    }

    fun onRequestingPermission() {
        _state.update { it.copy(message = "正在请求相册权限...") }
    }

    fun onPermissionDenied() {
        _state.update { it.copy(isImporting = false, message = "相册权限被拒绝，请在系统设置里允许读取照片后再试。") }
    }

    fun selectDevice(uri: Uri, deviceId: String) {
        _state.update { current ->
            current.copy(items = current.items.map { item ->
                if (item.uri != uri) return@map item
                val device = manifest.devices.first { it.id == deviceId }
                item.copy(device = device, frameColor = device.colors.first(), matchConfidence = MatchConfidence.Medium)
            })
        }
    }

    fun selectShell(style: ShellStyle) {
        _state.update { it.copy(shellStyle = style, message = "已切换为 ${style.title}。") }
    }

    fun selectRatio(ratio: CanvasRatio) {
        _state.update { it.copy(canvasRatio = ratio) }
    }

    fun setVerticalMargin(value: Float) {
        _state.update { it.copy(verticalMarginFraction = FrameGeometry.normalizedVerticalMargin(value)) }
    }

    fun selectBackgroundMode(mode: BackgroundMode) {
        _state.update { it.copy(backgroundMode = mode) }
    }

    fun selectBackgroundColor(color: Int) {
        _state.update { it.copy(backgroundMode = BackgroundMode.Solid, backgroundColor = color) }
    }

    fun setCustomColor(red: Int, green: Int, blue: Int) {
        _state.update {
            it.copy(
                backgroundMode = BackgroundMode.Solid,
                backgroundColor = Color.rgb(red.coerceIn(0, 255), green.coerceIn(0, 255), blue.coerceIn(0, 255))
            )
        }
    }

    fun setBackgroundImage(uri: Uri?) {
        _state.update {
            it.copy(
                backgroundMode = if (uri == null) BackgroundMode.Solid else BackgroundMode.Image,
                backgroundImageUri = uri,
                message = if (uri == null) "已移除背景图。" else "已设置自定义背景图。"
            )
        }
    }

    fun continueSelecting() {
        clear()
    }

    fun exportAll(onFinished: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(isExporting = true, message = "正在导出...") }
            val current = state.value
            val result = runCatching {
                exportService.exportBatch(
                    items = current.items,
                    config = current.toRenderConfig(),
                    format = ExportFormat.Png
                )
            }
            _state.update {
                result.fold(
                    onSuccess = { export ->
                        it.copy(
                            screen = FramerScreen.Success,
                            isExporting = false,
                            latestExport = export.shareFile,
                            message = "导出成功，图片已自动保存到系统相册。"
                        )
                    },
                    onFailure = { throwable ->
                        it.copy(isExporting = false, message = "导出失败：${throwable.message ?: "未知错误"}")
                    }
                )
            }
            if (result.isSuccess) onFinished?.invoke()
        }
    }

    fun shareLatest() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = state.value
            val latest = current.latestExport ?: exportService.exportBatch(
                items = current.items,
                config = current.toRenderConfig(),
                format = ExportFormat.Png
            ).shareFile
            latest ?: return@launch
            val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", latest)
            val share = Intent(Intent.ACTION_SEND)
                .setType("image/png")
                .putExtra(Intent.EXTRA_STREAM, uri)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(Intent.createChooser(share, "分享加壳截图").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun createItem(uri: Uri): FramerItem? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        val screenshot = ScreenshotMetadata(options.outWidth, options.outHeight)
        val matches = matcher.match(screenshot)
        val selected = matches.first().device
        return FramerItem(
            uri = uri,
            displayName = uri.lastPathSegment ?: "Screenshot",
            screenshot = screenshot,
            device = selected,
            frameColor = selected.colors.first(),
            matchConfidence = matches.first().confidence,
            alternatives = matches.map { it.device.id to it.device.name }
        )
    }

    companion object {
        fun factory(appContext: Context, initialUris: List<Uri>) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = FramerViewModel(appContext, initialUris) as T
        }
    }
}

data class FramerUiState(
    val items: List<FramerItem> = emptyList(),
    val screen: FramerScreen = FramerScreen.Home,
    val shellStyle: ShellStyle = ShellStyle.OppoSilverGreen,
    val canvasRatio: CanvasRatio = CanvasRatio.Ratio3x4,
    val backgroundMode: BackgroundMode = BackgroundMode.Solid,
    val backgroundColor: Int = 0xFFF3F5EA.toInt(),
    val backgroundImageUri: Uri? = null,
    val exportWidth: Int = 1800,
    val verticalMarginFraction: Float = FrameGeometry.DefaultVerticalMarginFraction,
    val isImporting: Boolean = false,
    val isExporting: Boolean = false,
    val message: String = "请选择一张截图。",
    val latestExport: File? = null
)

data class FramerItem(
    val uri: Uri,
    val displayName: String,
    val screenshot: ScreenshotMetadata,
    val device: DeviceFrame,
    val frameColor: FrameColor,
    val matchConfidence: MatchConfidence,
    val alternatives: List<Pair<String, String>>
) {
    val matchLabel: String
        get() = "${device.name} · ${matchConfidence.toChinese()} · ${screenshot.width}x${screenshot.height}"
}

data class GalleryImage(
    val uri: Uri,
    val name: String,
    val width: Int,
    val height: Int
)

enum class UiExportFormat {
    Png,
    Jpeg
}

enum class FramerScreen {
    Home,
    Editing,
    Success
}

enum class ShellStyle(val title: String, val subtitle: String) {
    OppoSilverGreen("H 安卓钛金属边", "右侧按键，圆形打孔，极窄黑边"),
    XiaomiLightSilver("H 安卓钛金属边", "右侧按键，圆形打孔，极窄黑边"),
    GlassMinimal("H 安卓钛金属边", "右侧按键，圆形打孔，极窄黑边")
}

val ShellStyle.metalGradientColors: IntArray
    get() = intArrayOf(
        0xFF171A19.toInt(),
        0xFF656D69.toInt(),
        0xFFEEF2ED.toInt(),
        0xFF929A95.toInt(),
        0xFF393F3C.toInt(),
        0xFF0B0D0D.toInt(),
        0xFF333A37.toInt(),
        0xFFE9EEE9.toInt(),
        0xFF5E6662.toInt(),
        0xFF151818.toInt()
    )

val ShellStyle.metalGradientPositions: FloatArray
    get() = floatArrayOf(0f, 0.045f, 0.08f, 0.12f, 0.20f, 0.50f, 0.80f, 0.895f, 0.945f, 1f)

enum class CanvasRatio(val title: String, val widthRatio: Int, val heightRatio: Int) {
    Ratio3x4("3:4", 3, 4),
    Ratio4x5("4:5", 4, 5),
    Ratio1x1("1:1", 1, 1),
    Ratio9x16("9:16", 9, 16)
}

enum class BackgroundMode {
    Solid,
    Image
}

data class BackgroundPreset(
    val name: String,
    val color: Int
)

val backgroundPresets = listOf(
    BackgroundPreset("米绿色", 0xFFF3F5EA.toInt()),
    BackgroundPreset("暖灰", 0xFFF1EFEB.toInt()),
    BackgroundPreset("雾蓝", 0xFFEAF0F4.toInt()),
    BackgroundPreset("浅粉", 0xFFF7ECEE.toInt()),
    BackgroundPreset("淡紫", 0xFFF1EDF8.toInt()),
    BackgroundPreset("纸白", 0xFFFAFAF4.toInt()),
    BackgroundPreset("浅灰", 0xFFF2F3F5.toInt()),
    BackgroundPreset("青瓷", 0xFFEAF4EF.toInt())
)

fun FramerUiState.resetForNewImport(): FramerUiState = copy(
    backgroundMode = BackgroundMode.Solid,
    backgroundImageUri = null,
    latestExport = null,
    isExporting = false
)

private fun FramerUiState.toRenderConfig(): ExportRenderConfig = ExportRenderConfig(
    style = shellStyle,
    ratio = canvasRatio,
    backgroundMode = backgroundMode,
    backgroundColor = backgroundColor,
    backgroundImageUri = backgroundImageUri,
    exportWidth = exportWidth,
    verticalMarginFraction = verticalMarginFraction
)

private fun MatchConfidence.toChinese(): String = when (this) {
    MatchConfidence.Exact -> "精确匹配"
    MatchConfidence.High -> "高匹配"
    MatchConfidence.Medium -> "可能匹配"
    MatchConfidence.Low -> "低匹配"
}
