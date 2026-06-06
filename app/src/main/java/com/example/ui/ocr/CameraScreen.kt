package com.example.ui.ocr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID

@Composable
fun CameraScreen(
    onImageCaptured: (android.net.Uri) -> Unit,
    onClose: () -> Unit,
    isProcessing: Boolean = false,
    processingMessage: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            onImageCaptured(uri)
        }
    }
    
    // Permission Check
    var hasCameraPermission by remember { 
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) 
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onClose()
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    if (!hasCameraPermission) {
        return
    }

    // Horizontal moving laser line animation within the scan frame
    val infiniteTransition = rememberInfiniteTransition(label = "laserScanner")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laserScanPosition"
    )

    // Capture flash screen visual cue animation
    var triggerFlash by remember { mutableStateOf(false) }
    val flashAlpha by animateFloatAsState(
        targetValue = if (triggerFlash) 0.8f else 0.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        finishedListener = { triggerFlash = false },
        label = "flashVisualCue"
    )

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // 1. Interactive Camera Preview View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PreviewView(ctx).apply {
                    previewView = this
                }
            },
            update = { view ->
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(view.surfaceProvider)
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    imageCapture = capture

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            capture
                        )
                        // Sync current torch state back in case of binding refresh
                        camera?.cameraControl?.enableTorch(isTorchEnabled)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )

        // 2. High-Tech Laser Scanning Bounding Box Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            // Define scan area bounds matching standard vertical bill aspect ratio
            val boxWidth = width * 0.82f
            val boxHeight = boxWidth * 1.4f
            val left = (width - boxWidth) / 2
            val top = (height - boxHeight) / 2.3f

            // Create circular window cut-out using transparent drawing mode
            drawRect(
                color = Color.Black.copy(alpha = 0.65f),
                size = size
            )
            
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                blendMode = BlendMode.Clear
            )

            // Draw glowing green border guidelines
            val neonGreen = Color(0xFF00FF66)
            drawRoundRect(
                color = neonGreen.copy(alpha = 0.4f),
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )

            // Corner Brackets for scanner framing emphasis
            val cornerLength = 24.dp.toPx()
            val thickness = 4.dp.toPx()
            val offsetCorrection = thickness / 2
            
            // Top Left Corner
            drawLine(neonGreen, Offset(left - offsetCorrection, top), Offset(left + cornerLength, top), thickness)
            drawLine(neonGreen, Offset(left, top - offsetCorrection), Offset(left, top + cornerLength), thickness)
            
            // Top Right Corner
            drawLine(neonGreen, Offset(left + boxWidth - cornerLength, top), Offset(left + boxWidth + offsetCorrection, top), thickness)
            drawLine(neonGreen, Offset(left + boxWidth, top - offsetCorrection), Offset(left + boxWidth, top + cornerLength), thickness)

            // Bottom Left Corner
            drawLine(neonGreen, Offset(left - offsetCorrection, top + boxHeight), Offset(left + cornerLength, top + boxHeight), thickness)
            drawLine(neonGreen, Offset(left, top + boxHeight - cornerLength), Offset(left, top + boxHeight + offsetCorrection), thickness)

            // Bottom Right Corner
            drawLine(neonGreen, Offset(left + boxWidth - cornerLength, top + boxHeight), Offset(left + boxWidth + offsetCorrection, top + boxHeight), thickness)
            drawLine(neonGreen, Offset(left + boxWidth, top + boxHeight - cornerLength), Offset(left + boxWidth, top + boxHeight + offsetCorrection), thickness)

            // Horizontal Moving Neon Laser Scan Line
            val laserY = top + (boxHeight * laserProgress)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        neonGreen.copy(alpha = 0.6f),
                        Color.Transparent
                    )
                ),
                topLeft = Offset(left + 4.dp.toPx(), laserY - 12.dp.toPx()),
                size = Size(boxWidth - 8.dp.toPx(), 24.dp.toPx())
            )
            drawLine(
                color = neonGreen,
                start = Offset(left + 2.dp.toPx(), laserY),
                end = Offset(left + boxWidth - 2.dp.toPx(), laserY),
                strokeWidth = 2.dp.toPx()
            )
        }

        // 3. User Assistance HUD Prompt
        Text(
            text = "Fit invoice completely inside scanner frame",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // 4. Capture & Torch Controls HUD panel
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
                .padding(bottom = 40.dp, top = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Image Picker Button
                IconButton(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Upload from Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Core Capture Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(6.dp)
                        .clickable(enabled = !isProcessing) {
                            triggerFlash = true
                            val photoFile = File(context.cacheDir, "scan_${UUID.randomUUID()}.jpg")
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                            imageCapture?.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        val uri = android.net.Uri.fromFile(photoFile)
                                        onImageCaptured(uri)
                                    }

                                    override fun onError(exc: ImageCaptureException) {
                                        exc.printStackTrace()
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }

                // Close Scanner View Trigger
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Scan",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 5. Simulated Capture Camera Shutter Flash visual layer
        if (flashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = flashAlpha)
                    .background(Color.White)
            )
        }

        // 6. Immersive Processing Loader Overlay
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    // Futuristic pulsing green progress indicator
                    val transition = rememberInfiniteTransition(label = "progressPulse")
                    val pulseScale by transition.animateFloat(
                        initialValue = 0.8f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scalePulse"
                    )

                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF00FF66),
                            strokeWidth = 3.dp
                        )
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Processing",
                            tint = Color(0xFF00FF66),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    Text(
                        text = "RetailDost AI Scanner",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = processingMessage ?: "Analyzing and extracting item invoice list...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
