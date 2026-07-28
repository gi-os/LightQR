package com.gios.lightqr

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

private enum class Screen { SCAN, ENTRY, RESULT, HISTORY }

@Composable
private fun App() {
    val context = LocalContext.current
    val store = remember { ScanStore(context) }

    var screen by remember { mutableStateOf(Screen.SCAN) }
    var current by remember { mutableStateOf<ScannedItem?>(null) }
    var history by remember { mutableStateOf(store.load()) }

    var hasCamera by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val askCamera = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCamera = it }
    LaunchedEffect(Unit) { if (!hasCamera) askCamera.launch(Manifest.permission.CAMERA) }

    fun accept(text: String) {
        val t = text.trim()
        if (t.isEmpty()) return
        history = store.add(t)
        current = history.first()
        screen = Screen.RESULT
    }

    Box(Modifier.fillMaxSize().background(LightBg)) {
        when (screen) {
            Screen.SCAN -> ScanScreen(
                hasCamera = hasCamera,
                onRequest = { askCamera.launch(Manifest.permission.CAMERA) },
                onScanned = { if (screen == Screen.SCAN) accept(it) },
                onType = { screen = Screen.ENTRY },
                onHistory = { screen = Screen.HISTORY },
            )
            Screen.ENTRY -> EntryScreen(
                onSubmit = { accept(it) },
                onBack = { screen = Screen.SCAN },
            )
            Screen.RESULT -> ResultScreen(
                item = current,
                onBack = { screen = Screen.SCAN },
            )
            Screen.HISTORY -> HistoryScreen(
                items = history,
                onOpen = { current = it; screen = Screen.RESULT },
                onClear = { store.clear(); history = emptyList() },
                onBack = { screen = Screen.SCAN },
            )
        }
    }
}

@Composable
private fun ScanScreen(
    hasCamera: Boolean,
    onRequest: () -> Unit,
    onScanned: (String) -> Unit,
    onType: () -> Unit,
    onHistory: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        if (hasCamera) {
            CameraPreview(onScanned)
            // Minimal centered reticle
            Box(
                Modifier.align(Alignment.Center)
                    .size(gu(13f))
                    .border(1.dp, LightInk)
            )
        } else {
            Column(
                Modifier.fillMaxSize().padding(gu(2f)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                LText("Camera access needed", LightType.copy, color = LightMuted)
                Spacer(Modifier.height(gu(1f)))
                LText("ALLOW", LightType.barLabel, tap(onRequest))
            }
        }

        Column(Modifier.fillMaxSize()) {
            LightTopBar(
                title = "LightQR",
                right = "History" to onHistory,
            )
            Spacer(Modifier.weight(1f))
            if (hasCamera) {
                LText(
                    "Point at a QR code",
                    LightType.detail,
                    Modifier.align(Alignment.CenterHorizontally),
                    color = LightMuted,
                )
                Spacer(Modifier.height(gu(1f)))
            }
            LightBottomBar(actions = listOf("Type or paste" to onType))
        }
    }
}

@Composable
private fun CameraPreview(onScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var handled by remember { mutableStateOf(false) }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor, QrAnalyzer { text ->
                    if (!handled) { handled = true; previewView.post { onScanned(text) } }
                })
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                    )
                } catch (_: Exception) { }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun EntryScreen(onSubmit: (String) -> Unit, onBack: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    fun go() { keyboard?.hide(); onSubmit(text) }

    Column(Modifier.fillMaxSize()) {
        LightTopBar(title = "Type or Paste", left = "Back" to onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = gu(1.5f)),
            verticalArrangement = Arrangement.Center,
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = LightType.copy.copy(color = LightInk),
                cursorBrush = SolidColor(LightInk),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { go() }),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    Column {
                        if (text.isEmpty()) {
                            LText("A link, wifi, note…", LightType.copy, color = LightMuted)
                        }
                        inner()
                        Spacer(Modifier.height(gu(0.75f)))
                        Hairline()
                    }
                },
            )
        }
        LightBottomBar(actions = listOf("Go" to { go() }))
    }
}

@Composable
private fun ResultScreen(item: ScannedItem?, onBack: () -> Unit) {
    val context = LocalContext.current
    if (item == null) { onBack(); return }

    fun copy() {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("qr", item.text))
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
    }
    fun open() {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) }
        catch (_: Exception) { Toast.makeText(context, "Can't open", Toast.LENGTH_SHORT).show() }
    }

    Column(Modifier.fillMaxSize()) {
        LightTopBar(title = if (item.isLink) "Link" else "Text", left = "Back" to onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = gu(1.5f), vertical = gu(1.5f)),
        ) {
            LText(item.text, LightType.copy)
        }
        val actions = buildList {
            if (item.isLink) add("Open" to { open() })
            add("Copy" to { copy() })
        }
        LightBottomBar(actions = actions)
    }
}

@Composable
private fun HistoryScreen(
    items: List<ScannedItem>,
    onOpen: (ScannedItem) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    val fmt = remember { SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()) }
    Column(Modifier.fillMaxSize()) {
        LightTopBar(
            title = "History",
            left = "Back" to onBack,
            right = if (items.isNotEmpty()) "Clear" to onClear else null,
        )
        if (items.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                LText("Nothing scanned yet", LightType.copy, color = LightMuted)
            }
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = gu(1.5f))
            ) {
                items(items) { it ->
                    Column(tap { onOpen(it) }.fillMaxWidth().padding(vertical = gu(0.9f))) {
                        LText(it.text, LightType.row, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(gu(0.25f)))
                        LText(
                            (if (it.isLink) "link · " else "") + fmt.format(Date(it.time)),
                            LightType.detail, color = LightMuted,
                        )
                    }
                    Hairline()
                }
            }
        }
    }
}
