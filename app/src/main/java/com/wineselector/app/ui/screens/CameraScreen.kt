@file:OptIn(ExperimentalMaterial3Api::class)

package com.wineselector.app.ui.screens

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.util.Log
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.wineselector.app.viewmodel.ScanMode
import java.io.File
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

private fun Context.findActivity(): ComponentActivity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/**
 * Full-screen CameraX viewfinder. Binds to the Activity lifecycle (not a
 * NavBackStackEntry, which can be destroyed) and saves captures to a file:
 * OnImageCapturedCallback's YUV output can't be decoded by BitmapFactory.
 */
@Composable
fun CameraScreen(
    initialMode: ScanMode,
    onCaptured: (File, ScanMode) -> Unit,
    onImport: (ScanMode) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(initialMode) }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (hasPermission) {
            CameraViewfinder(mode = mode, onCaptured = { onCaptured(it, mode) })
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Camera access is needed to scan", color = Color.White, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("You can also pick a photo you've already taken.", color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onImport(mode) }) { Text("Choose a photo") }
            }
        }

        // Top bar: back + mode switch
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = Color.Black.copy(alpha = 0.45f)) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White) }
            }
            Spacer(Modifier.weight(1f))
            SingleChoiceSegmentedButtonRow(Modifier.padding(end = 8.dp)) {
                ScanMode.entries.forEachIndexed { i, m ->
                    SegmentedButton(
                        selected = mode == m,
                        onClick = { mode = m },
                        shape = SegmentedButtonDefaults.itemShape(i, ScanMode.entries.size),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = Color.White,
                            inactiveContainerColor = Color.Black.copy(alpha = 0.45f),
                            inactiveContentColor = Color.White
                        ),
                        icon = {}
                    ) { Text(if (m == ScanMode.MENU) "Wine list" else "Bottle") }
                }
            }
        }

        if (hasPermission) {
            // Gallery import sits bottom-left; the shutter is drawn by the viewfinder.
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.BottomStart).navigationBarsPadding().padding(start = 32.dp, bottom = 44.dp)
            ) {
                IconButton(onClick = { onImport(mode) }, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.PhotoLibrary, "Choose from photos", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun CameraViewfinder(mode: ScanMode, onCaptured: (File) -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var torchOn by remember { mutableStateOf(false) }
    var capturing by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf(false) }
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var zoom by remember { mutableFloatStateOf(1f) }

    DisposableEffect(Unit) {
        onDispose { runCatching { provider?.unbindAll() } }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    val future = ProcessCameraProvider.getInstance(ctx)
                    future.addListener({
                        try {
                            val p = future.get()
                            provider = p
                            val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
                            // Quality over latency: small menu text needs every pixel.
                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()
                            imageCapture = capture
                            p.unbindAll()
                            if (activity != null) {
                                camera = p.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                            }
                        } catch (e: Exception) {
                            Log.e("CameraScreen", "Camera setup failed", e)
                            cameraError = true
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Gesture layer: tap to focus, pinch to zoom.
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(camera) {
                    detectTapGestures { offset ->
                        val cam = camera ?: return@detectTapGestures
                        val pv = previewView ?: return@detectTapGestures
                        val point = pv.meteringPointFactory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point)
                            .setAutoCancelDuration(4, TimeUnit.SECONDS)
                            .build()
                        cam.cameraControl.startFocusAndMetering(action)
                        focusPoint = offset
                    }
                }
                .pointerInput(camera) {
                    detectTransformGestures { _, _, gestureZoom, _ ->
                        val cam = camera ?: return@detectTransformGestures
                        val state = cam.cameraInfo.zoomState.value
                        val min = state?.minZoomRatio ?: 1f
                        val max = state?.maxZoomRatio ?: 4f
                        zoom = (zoom * gestureZoom).coerceIn(min, max)
                        cam.cameraControl.setZoomRatio(zoom)
                    }
                }
        )

        FramingGuide(mode)

        focusPoint?.let { p ->
            LaunchedEffect(p) { delay(900); focusPoint = null }
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color.White.copy(alpha = 0.9f), radius = 36.dp.toPx(), center = p, style = Stroke(2.dp.toPx()))
            }
        }

        if (cameraError) {
            Text(
                "No camera available on this device. Choose a photo instead.",
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(32.dp)
            )
        }

        // Torch
        val cam = camera
        if (cam != null && cam.cameraInfo.hasFlashUnit()) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(end = 32.dp, bottom = 44.dp)
            ) {
                IconButton(
                    onClick = { torchOn = !torchOn; cam.cameraControl.enableTorch(torchOn) },
                    modifier = Modifier.size(52.dp)
                ) {
                    Icon(if (torchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff, "Torch", tint = Color.White)
                }
            }
        }

        // Shutter
        Box(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            if (capturing) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(76.dp))
            } else if (!cameraError) {
                Surface(
                    onClick = {
                        val capture = imageCapture ?: return@Surface
                        capturing = true
                        val file = File(context.cacheDir, "capture_${System.currentTimeMillis()}.jpg")
                        capture.takePicture(
                            ImageCapture.OutputFileOptions.Builder(file).build(),
                            ContextCompat.getMainExecutor(context),
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    runCatching { if (torchOn) camera?.cameraControl?.enableTorch(false) }
                                    runCatching { provider?.unbindAll() }
                                    onCaptured(file)
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    Log.e("CameraScreen", "Capture failed", exception)
                                    capturing = false
                                }
                            }
                        )
                    },
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(76.dp).border(4.dp, Color.White.copy(alpha = 0.5f), CircleShape).padding(6.dp)
                ) {
                    Box(Modifier.fillMaxSize().border(2.dp, Color.Black.copy(alpha = 0.15f), CircleShape))
                }
            }
        }
    }
}

@Composable
private fun FramingGuide(mode: ScanMode) {
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            val (wFrac, hFrac) = if (mode == ScanMode.MENU) 0.88f to 0.62f else 0.56f to 0.5f
            val w = size.width * wFrac
            val h = size.height * hFrac
            val left = (size.width - w) / 2
            val top = (size.height - h) / 2 - size.height * 0.02f
            drawRoundRect(
                Color.White.copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size = Size(w, h),
                cornerRadius = CornerRadius(28.dp.toPx()),
                style = Stroke(2.dp.toPx())
            )
        }
        Column(
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(color = Color.Black.copy(alpha = 0.5f), shape = CircleShape) {
                Text(
                    if (mode == ScanMode.MENU) "Fit the wine list in the frame · hold steady"
                    else "Centre the front label · avoid glare",
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
            Text("Tap to focus · pinch to zoom", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall)
        }
    }
}
