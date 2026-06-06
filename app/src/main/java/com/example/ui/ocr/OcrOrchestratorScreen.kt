package com.example.ui.ocr

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun OcrOrchestratorScreen(
    viewModel: OcrViewModel = hiltViewModel(),
    isStockOut: Boolean = false,
    onComplete: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(isStockOut) {
        viewModel.setStockOutMode(isStockOut)
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraUri?.let { viewModel.processImage(context, it) }
        } else {
            viewModel.onScannerDismissed()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = OcrImageUtils.createImageUri(context)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            viewModel.onScannerDismissed()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            viewModel.processImage(context, uri)
        } else {
            viewModel.onScannerDismissed()
        }
    }

    when (val state = uiState) {
        is OcrUiState.Idle -> {
            LaunchedEffect(Unit) {
                viewModel.onScannerButtonClicked()
            }
        }

        is OcrUiState.ShowScanner -> {
            OcrScannerBottomSheet(
                onDismiss = {
                    viewModel.onScannerDismissed()
                    onNavigateBack()
                },
                onCameraCapture = {
                    val hasCamPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    if (hasCamPermission) {
                        val uri = OcrImageUtils.createImageUri(context)
                        cameraUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onGallerySelect = {
                    galleryLauncher.launch("image/*")
                }
            )
        }

        is OcrUiState.ProcessingImage -> OcrProcessingScreen()

        is OcrUiState.ReviewItems -> OcrReviewScreen(
            invoice = state.invoice,
            reviewableItems = state.reviewableItems,
            onEditItem = { id, updated -> viewModel.updateReviewItem(id, updated) },
            onRemoveItem = { id -> viewModel.removeReviewItem(id) },
            onConfirm = { viewModel.confirmAndSaveToStock() },
            onBack = { viewModel.retryFromScanner() }
        )

        is OcrUiState.SavingToStock -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Saving to stock...", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        is OcrUiState.Success -> OcrSuccessScreen(
            createdCount = state.createdCount,
            updatedCount = state.updatedCount,
            onDone = {
                viewModel.resetToIdle()
                onComplete()
            }
        )

        is OcrUiState.Error -> OcrErrorScreen(
            message = state.message,
            isNotInvoice = state.isNotInvoice,
            onRetry = { viewModel.retryFromScanner() },
            onDismiss = {
                viewModel.resetToIdle()
                onNavigateBack()
            }
        )
    }
}
