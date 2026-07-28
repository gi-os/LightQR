package com.gios.lightqr

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

private val Ink = Color.White
private val Paper = Color.Black
private val Muted = Color(0xFF9A9A9A)
private val Line = Color(0xFF2A2A2A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

private enum class Screen { SCAN, RESULT, HISTORY }

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
    val askCamera = androidx.activity.compose.rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCamera = it }

    LaunchedEffect(Unit) { if (!hasCamera) askCamera.launch(Manifest.permission.CAMERA) }

    fun onDecoded(text: String) {
        history = store.add(text)
        current = history.first()
        screen = Screen.RESULT
    }

    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        when (screen) {
            Screen.SCAN -> ScanScreen(
                hasCamera = hasCamera,
                onRequest = { askCamera.launch(Manifest.permission.CAMERA) },
                onDecoded = { if (screen == Screen.SCAN) onDecoded(it) },
                onHistory = { screen = Screen.HISTORY }
            )
            Screen.RESULT -> ResultScreen(
                item = current,
                onBack = { screen = Screen.SCAN }
            )
            Screen.HISTORY -> HistoryScreen(
                items = history,
                onOpen = { current = it; screen = Screen.RESULT },
                onClear = { store.clear(); history = emptyList() },
                onBack = { screen = Screen.SCAN }
            )
        }
    }
}

@Composable
private fun ScanScreen(
    hasCamera: Boolean,
    onRequest: () -> Unit,
    onDecoded: (String) -> Unit,
    onHistory: () -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        if (hasCamera) {
            CameraPreview(onDecoded = onDecoded)
        } else {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera access needed to scan", color = Ink, fontSize = 15.sp)
                Spacer(Modifier.height(16.dp))
                Btn("Allow camera", filled = true, onClick = onRequest)
            }
        }

        // Header row
        Row(
            Modifier.fillMaxWidth().padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("SCAN", color = Ink, fontSize = 16.sp)
            Text("History", color = Muted, fontSize = 14.sp,
                modifier = Modifier.clickable { onHistory() })
        }

        // Manual entry — paste or type a link/address with the normal keyboard,
        // instead of scanning. Treated exactly like a scanned result.
        ManualEntry(
            onSubmit = onDecoded,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun ManualEntry(onSubmit: (String) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current

    fun go() {
        val t = text.trim()
        if (t.isNotEmpty()) {
            keyboard?.hide()
            onSubmit(t)
            text = ""
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .background(Paper)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.weight(1f),
            singleLine = true,
            placeholder = { Text("or paste / type a link…", color = Muted, fontSize = 14.sp) },
            textStyle = LocalTextStyle.current.copy(color = Ink, fontSize = 15.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Ink,
                unfocusedBorderColor = Line,
                cursorColor = Ink
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { go() })
        )
        Btn2("Go", onClick = { go() })
    }
}

@Composable
private fun Btn2(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .background(Ink, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Paper, fontSize = 15.sp) }
}

@Composable
private fun CameraPreview(onDecoded: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    var handled by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val providerFuture = ProcessCameraProvider.getInstance(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor, QrAnalyzer { text ->
                    if (!handled) {
                        handled = true
                        previewView.post { onDecoded(text) }
                    }
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
private fun ResultScreen(item: ScannedItem?, onBack: () -> Unit) {
    val context = LocalContext.current
    if (item == null) { onBack(); return }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (item.isLink) "LINK" else "TEXT", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth()
                .background(Color(0xFF101010), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(item.text, color = Ink, fontSize = 16.sp)
        }
        Spacer(Modifier.height(24.dp))

        if (item.isLink) {
            Btn("Open link", filled = true) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
                } catch (_: Exception) {
                    Toast.makeText(context, "No app to open this", Toast.LENGTH_SHORT).show()
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Btn("Copy text", filled = false) {
            val cm = context.getSystemService(android.content.ClipboardManager::class.java)
            cm.setPrimaryClip(android.content.ClipData.newPlainText("qr", item.text))
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }
        Spacer(Modifier.height(10.dp))
        Btn("Scan again", filled = false, onClick = onBack)
    }
}

@Composable
private fun HistoryScreen(
    items: List<ScannedItem>,
    onOpen: (ScannedItem) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    val fmt = remember { SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()) }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Back", color = Muted, fontSize = 14.sp, modifier = Modifier.clickable { onBack() })
            Text("HISTORY", color = Ink, fontSize = 16.sp)
            Text("Clear", color = Muted, fontSize = 14.sp,
                modifier = Modifier.clickable { onClear() })
        }
        Spacer(Modifier.height(16.dp))

        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing scanned yet", color = Muted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(items) { it ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable { onOpen(it) }
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            it.text, color = Ink, fontSize = 15.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            (if (it.isLink) "link · " else "") + fmt.format(Date(it.time)),
                            color = Muted, fontSize = 11.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.fillMaxWidth().height(1.dp).background(Line))
                    }
                }
            }
        }
    }
}

@Composable
private fun Btn(label: String, filled: Boolean, onClick: () -> Unit) {
    val bg = if (filled) Ink else Color.Transparent
    val fg = if (filled) Paper else Ink
    Box(
        Modifier.fillMaxWidth()
            .background(bg, RoundedCornerShape(10.dp))
            .then(
                if (filled) Modifier
                else Modifier.background(Color.Transparent, RoundedCornerShape(10.dp))
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = fg, fontSize = 15.sp)
    }
}
