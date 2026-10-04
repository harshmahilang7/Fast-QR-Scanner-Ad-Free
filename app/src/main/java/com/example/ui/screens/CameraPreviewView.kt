package com.example.ui.screens

import android.content.Context
import android.view.ScaleGestureDetector
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.model.ParsedBarcodeResult
import com.example.scanner.BarcodeAnalyzer
import java.util.concurrent.Executors

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    isTorchOn: Boolean,
    zoomRatio: Float,
    isFrontCamera: Boolean,
    isPaused: Boolean,
    onBarcodeDetected: (ParsedBarcodeResult) -> Unit,
    onZoomChange: (Float) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    // React to torch change
    LaunchedEffect(isTorchOn, camera) {
        camera?.let {
            if (it.cameraInfo.hasFlashUnit()) {
                it.cameraControl.enableTorch(isTorchOn)
            }
        }
    }

    // React to zoom change
    LaunchedEffect(zoomRatio, camera) {
        camera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Setup pinch-to-zoom detector on previewView
    DisposableEffect(previewView) {
        val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val currentZoom = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f
                val delta = detector.scaleFactor
                val newZoom = (currentZoom * delta).coerceIn(1.0f, 5.0f)
                onZoomChange(newZoom)
                return true
            }
        })

        previewView.setOnTouchListener { v, event ->
            scaleGestureDetector.onTouchEvent(event)
            v.performClick()
            true
        }

        onDispose {
            previewView.setOnTouchListener(null)
        }
    }

    LaunchedEffect(isFrontCamera) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = if (isFrontCamera) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                CameraSelector.DEFAULT_BACK_CAMERA
            }

            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            val analyzer = BarcodeAnalyzer(
                onBarcodeDetected = onBarcodeDetected,
                shouldProcess = { !isPaused }
            )

            imageAnalysis.setAnalyzer(cameraExecutor, analyzer)

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                // Reapply current zoom
                camera?.cameraControl?.setZoomRatio(zoomRatio)
                if (camera?.cameraInfo?.hasFlashUnit() == true) {
                    camera?.cameraControl?.enableTorch(isTorchOn)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    AndroidView(
        factory = { previewView },
        modifier = modifier.fillMaxSize()
    )
}
