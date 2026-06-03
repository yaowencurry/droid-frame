package com.example.screenshotframer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.screenshotframer.ui.BackgroundMode
import com.example.screenshotframer.ui.CanvasRatio
import com.example.screenshotframer.ui.FrameGeometry
import com.example.screenshotframer.ui.FramerScreen
import com.example.screenshotframer.ui.FramerUiState
import com.example.screenshotframer.ui.FramerViewModel
import com.example.screenshotframer.ui.ShellStyle
import com.example.screenshotframer.ui.UiTheme
import com.example.screenshotframer.ui.backgroundPresets
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UiTheme {
                FramerApp(initialUris = extractIncomingUris(intent))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun extractIncomingUris(intent: Intent?): List<Uri> {
        if (intent == null) return emptyList()
        return when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(intent.streamUri())
            Intent.ACTION_SEND_MULTIPLE -> intent.streamUris()
            else -> emptyList()
        }
    }

    private fun Intent.streamUri(): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun Intent.streamUris(): List<Uri> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            @Suppress("DEPRECATION")
            getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FramerApp(initialUris: List<Uri>) {
    val context = LocalContext.current
    val viewModel: FramerViewModel = viewModel(factory = FramerViewModel.factory(context.applicationContext, initialUris))
    val state by viewModel.state.collectAsState()
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    lateinit var pickScreenshot: () -> Unit
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.importUris(listOf(uri))
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            viewModel.onPermissionDenied()
        }
    }
    pickScreenshot = {
        if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            viewModel.onRequestingPermission()
            permissionLauncher.launch(permission)
        }
    }

    val backgroundPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        viewModel.setBackgroundImage(uri)
    }

    when (state.screen) {
        FramerScreen.Home -> HomeScreen(
            message = state.message,
            isImporting = state.isImporting,
            onPick = pickScreenshot
        )
        FramerScreen.Editing -> Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("编辑套壳", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        TextButton(onClick = pickScreenshot) { Text("重新选择") }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.exportAll() }, enabled = state.items.isNotEmpty() && !state.isExporting) {
                            Text(if (state.isExporting) "导出中" else "导出")
                        }
                    }
                )
            }
        ) { padding ->
            EditorScreen(
                state = state,
                padding = padding,
                onShell = viewModel::selectShell,
                onRatio = viewModel::selectRatio,
                onVerticalMargin = viewModel::setVerticalMargin,
                onBackgroundColor = viewModel::selectBackgroundColor,
                onCustomColor = viewModel::setCustomColor,
                onPickBackground = {
                    backgroundPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                onRemoveBackground = { viewModel.setBackgroundImage(null) }
            )
        }
        FramerScreen.Success -> SuccessScreen(
            message = state.message,
            exportPath = state.latestExport?.absolutePath,
            onContinue = {
                viewModel.continueSelecting()
                pickScreenshot()
            },
            onShare = viewModel::shareLatest
        )
    }
}

@Composable
private fun HomeScreen(message: String, isImporting: Boolean, onPick: () -> Unit) {
    Surface(
        color = Color(0xFFF6F7EF),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("截图加壳", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text("选择一张截图，自动生成精致手机边框图。", color = Color(0xFF657168))
            Spacer(Modifier.height(34.dp))
            Button(
                onClick = onPick,
                contentPadding = PaddingValues(horizontal = 34.dp, vertical = 16.dp),
                enabled = !isImporting
            ) {
                Text(if (isImporting) "读取中..." else "选择图片")
            }
            Spacer(Modifier.height(18.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF7A837C))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorScreen(
    state: FramerUiState,
    padding: PaddingValues,
    onShell: (ShellStyle) -> Unit,
    onRatio: (CanvasRatio) -> Unit,
    onVerticalMargin: (Float) -> Unit,
    onBackgroundColor: (Int) -> Unit,
    onCustomColor: (Int, Int, Int) -> Unit,
    onPickBackground: () -> Unit,
    onRemoveBackground: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF7F8F2))
            .padding(padding)
    ) {
        Box(Modifier.padding(16.dp)) {
            ExportPreview(state)
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Section("画布布局") {
                    Text("上下留白 ${(state.verticalMarginFraction * 100).roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = state.verticalMarginFraction,
                        onValueChange = onVerticalMargin,
                        valueRange = FrameGeometry.MinVerticalMarginFraction..FrameGeometry.MaxVerticalMarginFraction
                    )
                }
            }
            item {
                Section("手机壳样式") {
                    ShellStyle.entries.forEach { style ->
                        OptionRow(
                            title = style.title,
                            subtitle = style.subtitle,
                            selected = state.shellStyle == style,
                            onClick = { onShell(style) }
                        )
                    }
                }
            }
            item {
                Section("导出比例") {
                    FlowChips {
                        CanvasRatio.entries.forEach { ratio ->
                            FilterChip(
                                selected = state.canvasRatio == ratio,
                                onClick = { onRatio(ratio) },
                                label = { Text(ratio.title) }
                            )
                        }
                    }
                }
            }
            item {
                Section("背景颜色") {
                    FlowChips {
                        backgroundPresets.forEach { preset ->
                            ColorPresetChip(
                                name = preset.name,
                                color = Color(preset.color),
                                selected = state.backgroundMode == BackgroundMode.Solid && state.backgroundColor == preset.color,
                                onClick = { onBackgroundColor(preset.color) }
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    ColorMixer(
                        color = Color(state.backgroundColor),
                        onChange = onCustomColor
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(onClick = onPickBackground) { Text("从相册选背景") }
                        if (state.backgroundImageUri != null) {
                            OutlinedButton(onClick = onRemoveBackground) { Text("移除背景图") }
                        }
                    }
                }
            }
            item {
                Text(state.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF657168))
            }
        }
    }
}

@Composable
private fun ExportPreview(state: FramerUiState) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 360.dp),
        contentAlignment = Alignment.Center
    ) {
        val layout = FrameGeometry.previewLayout(
            containerWidth = maxWidth.value,
            maxHeight = 360f,
            ratio = state.canvasRatio,
            verticalMarginFraction = state.verticalMarginFraction
        )
        Box(
            modifier = Modifier
                .width(layout.canvasWidth.dp)
                .height(layout.canvasHeight.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(state.backgroundColor)),
            contentAlignment = Alignment.Center
        ) {
            if (state.backgroundMode == BackgroundMode.Image && state.backgroundImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(state.backgroundImageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            state.items.firstOrNull()?.let { item ->
                PhonePreview(
                    screenshotUri = item.uri,
                    style = state.shellStyle,
                    modifier = Modifier
                        .fillMaxWidth(layout.phoneWidthFraction)
                        .aspectRatio(FrameGeometry.PhoneAspectRatio)
                )
            }
        }
    }
}

@Composable
private fun PhonePreview(screenshotUri: Uri, style: ShellStyle, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier) {
        val outerRadius = maxWidth * FrameGeometry.OuterCornerRadiusFraction
        val screenRadius = maxWidth * FrameGeometry.ScreenCornerRadiusFraction
        val horizontalInset = maxWidth * FrameGeometry.ScreenInsetHorizontalFraction
        val topInset = maxWidth * FrameGeometry.ScreenInsetTopFraction
        val bottomInset = maxWidth * FrameGeometry.ScreenInsetBottomFraction
        val phoneHeight = maxWidth / FrameGeometry.PhoneAspectRatio
        val buttonWidth = maxWidth * 0.007f
        val volumeTop = phoneHeight * 0.135f
        val powerTop = phoneHeight * 0.295f
        val buttonColor = Color(0xFF79817D)
        val buttonHighlight = Color(0x55F1F4EF)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(18.dp, RoundedCornerShape(outerRadius))
                .clip(RoundedCornerShape(outerRadius))
                .background(style.previewBrush())
                .padding(start = horizontalInset, end = horizontalInset, top = topInset, bottom = bottomInset)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(screenRadius))
                    .background(Color(0xFF050706))
            ) {
                Image(
                    painter = rememberAsyncImagePainter(screenshotUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(screenRadius))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 5.dp)
                        .size(8.dp)
                        .background(Color(0xFF050606), CircleShape)
                )
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = buttonWidth * 0.85f)
                .padding(top = volumeTop)
                .width(buttonWidth)
                .height(phoneHeight * 0.11f)
                .clip(RoundedCornerShape(buttonWidth / 2f))
                .background(buttonColor)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(buttonWidth * 0.24f)
                    .fillMaxSize()
                    .background(buttonHighlight)
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = buttonWidth * 0.85f)
                .padding(top = powerTop)
                .width(buttonWidth)
                .height(phoneHeight * 0.077f)
                .clip(RoundedCornerShape(buttonWidth / 2f))
                .background(buttonColor)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(buttonWidth * 0.24f)
                    .fillMaxSize()
                    .background(buttonHighlight)
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

@Composable
private fun OptionRow(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(if (selected) Color(0xFFEAF4EF) else Color(0xFFF7F8F5))
            .border(1.dp, if (selected) Color(0xFF4D8B67) else Color.Transparent, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(Modifier.size(34.dp)) {
            drawRoundRect(
                brush = Brush.linearGradient(listOf(Color(0xFFEEF7EE), Color(0xFF7D9187), Color.White)),
                cornerRadius = CornerRadius(12f, 12f),
                size = Size(size.width * 0.62f, size.height),
                topLeft = Offset(size.width * 0.18f, 0f)
            )
            drawCircle(Color(0xFF070908), radius = 3.2f, center = Offset(size.width / 2f, size.height * 0.14f))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF68736B))
        }
        if (selected) Text("已选", color = Color(0xFF2D7A50), style = MaterialTheme.typography.labelMedium)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(content: @Composable FlowRowScope.() -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        content()
    }
}

@Composable
private fun ColorPresetChip(name: String, color: Color, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(999.dp),
        color = if (selected) Color(0xFFE5F2EA) else Color(0xFFF6F7F3),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) Color(0xFF4D8B67) else Color(0xFFE3E6E0))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Box(Modifier.size(18.dp).background(color, CircleShape).border(1.dp, Color(0x22000000), CircleShape))
            Text(name, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ColorMixer(color: Color, onChange: (Int, Int, Int) -> Unit) {
    val red = (color.red * 255).roundToInt()
    val green = (color.green * 255).roundToInt()
    val blue = (color.blue * 255).roundToInt()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("自定义颜色", style = MaterialTheme.typography.labelLarge)
        ColorSlider("R", red) { onChange(it, green, blue) }
        ColorSlider("G", green) { onChange(red, it, blue) }
        ColorSlider("B", blue) { onChange(red, green, it) }
    }
}

@Composable
private fun ColorSlider(label: String, value: Int, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, modifier = Modifier.width(16.dp), fontWeight = FontWeight.SemiBold)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f)
        )
        Text(value.toString(), modifier = Modifier.width(34.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SuccessScreen(message: String, exportPath: String?, onContinue: () -> Unit, onShare: () -> Unit) {
    Surface(
        color = Color(0xFFF6F7EF),
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("导出成功", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFF657168))
            if (exportPath != null) {
                Spacer(Modifier.height(8.dp))
                Text(exportPath, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8A928C))
            }
            Spacer(Modifier.height(34.dp))
            Button(onClick = onContinue, contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)) {
                Text("继续选择图片")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = onShare) { Text("分享图片") }
        }
    }
}

private fun ShellStyle.previewBrush(): Brush = Brush.horizontalGradient(
    listOf(
        Color(0xFF171A19),
        Color(0xFF656D69),
        Color(0xFFEEF2ED),
        Color(0xFF929A95),
        Color(0xFF393F3C),
        Color(0xFF0B0D0D),
        Color(0xFF333A37),
        Color(0xFFE9EEE9),
        Color(0xFF5E6662),
        Color(0xFF151818)
    )
)
