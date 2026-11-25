package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BurstMode
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.AppLocale
import com.example.engine.AndroidSensorsManager
import com.example.ui.theme.LocalTibbColors
import com.example.ui.theme.TibbPrimaryPillButton
import com.example.ui.theme.TibbSecondaryPillButton
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraMultimodalModal(
    locale: AppLocale,
    accountId: String,
    qrOnlyMode: Boolean = false,
    onCapturedPhoto: (savedPath: String, kind: String) -> Unit,
    onQrScanned: (token: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val c = LocalTibbColors.current
    val scope = rememberCoroutineScope()
    val isUr = locale == AppLocale.UR

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var statusText by remember {
        mutableStateOf(
            if (qrOnlyMode) "Point camera at a Tibb offline referral QR slip"
            else "Point camera at skin rash, medicine strip, or lab slip"
        )
    }
    var busy by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember {
        BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            executor.shutdown()
            scanner.close()
        }
    }

    fun captureSingleFrame(kind: String, onSaved: (String) -> Unit) {
        val tmpFile = File(context.cacheDir, "tibb_cap_${System.currentTimeMillis()}.jpg")
        val opts = ImageCapture.OutputFileOptions.Builder(tmpFile).build()
        imageCapture.takePicture(
            opts,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    scope.launch {
                        val bmp = BitmapFactory.decodeFile(tmpFile.absolutePath)
                        tmpFile.delete()
                        if (bmp != null) {
                            val saved = AndroidSensorsManager.saveBitmapStrippedToVault(context, accountId, bmp)
                            bmp.recycle()
                            saved.onSuccess { onSaved(it) }
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    statusText = "Capture error: ${exception.message}"
                    busy = false
                }
            },
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding(),
    ) {
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        if (qrOnlyMode) {
                            var handled = false
                            analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                                @Suppress("UnsafeOptInUsageError")
                                val mediaImg = proxy.image
                                if (mediaImg != null && !handled) {
                                    val input = InputImage.fromMediaImage(mediaImg, proxy.imageInfo.rotationDegrees)
                                    scanner.process(input)
                                        .addOnSuccessListener { codes ->
                                            val raw = codes.firstOrNull()?.rawValue
                                            if (!raw.isNullOrBlank() && !handled) {
                                                handled = true
                                                onQrScanned(raw)
                                            }
                                        }
                                        .addOnCompleteListener { proxy.close() }
                                } else {
                                    proxy.close()
                                }
                            }
                        }

                        runCatching {
                            provider.unbindAll()
                            if (qrOnlyMode) {
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    analysis,
                                )
                            } else {
                                provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageCapture,
                                )
                            }
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isUr) "کیمرہ استعمال کرنے کی اجازت درکار ہے" else "Camera permission required",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    TibbPrimaryPillButton(
                        onClick = { permLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text("Grant Camera Permission", textAlign = TextAlign.Center)
                    }
                }
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .testTag("camera_modal_close"),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close Camera", tint = Color.White)
            }
        }

        // Bottom Capture Controls
        if (hasPermission && !qrOnlyMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(16.dp)
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TibbPrimaryPillButton(
                    enabled = !busy,
                    onClick = {
                        busy = true
                        statusText = "Saving EXIF-stripped photo..."
                        captureSingleFrame("CAMERA_FRAME") { path ->
                            busy = false
                            onCapturedPhoto(path, "CAMERA_FRAME")
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).testTag("camera_capture_photo_btn"),
                ) {
                    Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("Capture Photo", textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }

                TibbSecondaryPillButton(
                    enabled = !busy,
                    borderColor = c.accent,
                    contentColor = Color.White,
                    onClick = {
                        busy = true
                        scope.launch {
                            statusText = "Capturing 3-frame breathing burst..."
                            var lastPath: String? = null
                            repeat(3) { idx ->
                                captureSingleFrame("VIDEO_BURST") { path -> lastPath = path }
                                delay(850L)
                            }
                            delay(400L)
                            busy = false
                            lastPath?.let { onCapturedPhoto(it, "VIDEO_BURST") }
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f).fillMaxHeight().heightIn(min = 48.dp).testTag("camera_capture_burst_btn"),
                ) {
                    Icon(Icons.Rounded.BurstMode, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("3s Burst", textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        } else if (hasPermission && qrOnlyMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(220.dp)
                    .border(2.dp, c.accent, RoundedCornerShape(24.dp)),
            ) {
                Icon(
                    Icons.Rounded.QrCodeScanner,
                    contentDescription = null,
                    tint = c.accent.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Center).size(56.dp),
                )
            }
        }
    }
}
