package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ScanHistoryRepository
import com.example.domain.model.ParsedBarcodeResult
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ScannerUiState(
    val isTorchOn: Boolean = false,
    val zoomRatio: Float = 1.0f,
    val isFrontCamera: Boolean = false,
    val detectedResult: ParsedBarcodeResult? = null,
    val isProcessingImage: Boolean = false,
    val errorMessage: String? = null,
    val isPaused: Boolean = false
)

class ScannerViewModel(
    private val repository: ScanHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScannerUiState())
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val quickRecentScans: StateFlow<List<ParsedBarcodeResult>> = repository.recentScans
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onBarcodeDetected(context: Context, result: ParsedBarcodeResult) {
        if (_uiState.value.detectedResult != null || _uiState.value.isPaused) return

        triggerHapticFeedback(context)

        viewModelScope.launch {
            val savedItem = repository.saveScan(result)
            _uiState.value = _uiState.value.copy(
                detectedResult = savedItem,
                isPaused = true
            )
        }
    }

    fun dismissDetectedResult() {
        _uiState.value = _uiState.value.copy(
            detectedResult = null,
            isPaused = false
        )
    }

    fun toggleTorch() {
        _uiState.value = _uiState.value.copy(isTorchOn = !_uiState.value.isTorchOn)
    }

    fun setZoom(ratio: Float) {
        _uiState.value = _uiState.value.copy(zoomRatio = ratio.coerceIn(1.0f, 5.0f))
    }

    fun toggleCameraFacing() {
        _uiState.value = _uiState.value.copy(
            isFrontCamera = !_uiState.value.isFrontCamera,
            isTorchOn = false
        )
    }

    fun toggleFavorite(item: ParsedBarcodeResult) {
        viewModelScope.launch {
            val newFav = !item.isFavorite
            repository.toggleFavorite(item.id, newFav)
            if (_uiState.value.detectedResult?.id == item.id) {
                _uiState.value = _uiState.value.copy(
                    detectedResult = _uiState.value.detectedResult?.copy(isFavorite = newFav)
                )
            }
        }
    }

    fun scanFromGalleryUri(context: Context, uri: Uri) {
        _uiState.value = _uiState.value.copy(isProcessingImage = true, errorMessage = null)
        try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build()
            val scanner = BarcodeScanning.getClient(options)

            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    val firstBarcode = barcodes.firstOrNull()
                    if (firstBarcode != null && !firstBarcode.rawValue.isNullOrBlank()) {
                        val parsed = ParsedBarcodeResult.fromMlKitBarcode(firstBarcode)
                        triggerHapticFeedback(context)
                        viewModelScope.launch {
                            val saved = repository.saveScan(parsed)
                            _uiState.value = _uiState.value.copy(
                                detectedResult = saved,
                                isProcessingImage = false,
                                isPaused = true
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isProcessingImage = false,
                            errorMessage = "No QR code or barcode found in selected image."
                        )
                    }
                }
                .addOnFailureListener { error ->
                    _uiState.value = _uiState.value.copy(
                        isProcessingImage = false,
                        errorMessage = "Failed to scan image: ${error.localizedMessage}"
                    )
                }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isProcessingImage = false,
                errorMessage = "Could not open selected image."
            )
        }
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun triggerHapticFeedback(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(45)
            }
        } catch (e: Exception) {
            // Non-fatal if device has no vibrator
        }
    }

    companion object {
        fun provideFactory(repository: ScanHistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ScannerViewModel(repository) as T
                }
            }
    }
}
