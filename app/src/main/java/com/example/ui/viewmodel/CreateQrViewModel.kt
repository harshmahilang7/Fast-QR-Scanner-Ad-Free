package com.example.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ScanHistoryRepository
import com.example.domain.model.BarcodeType
import com.example.domain.model.ParsedBarcodeResult
import com.example.scanner.QrCodeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class QrCreateType(val label: String) {
    URL("Link"),
    TEXT("Text"),
    WIFI("Wi-Fi"),
    CONTACT("Contact"),
    PHONE("Phone"),
    EMAIL("Email")
}

data class CreateQrUiState(
    val selectedType: QrCreateType = QrCreateType.URL,
    val urlInput: String = "https://",
    val textInput: String = "",
    val wifiSsid: String = "",
    val wifiPassword: String = "",
    val wifiSecurity: String = "WPA",
    val contactName: String = "",
    val contactPhone: String = "",
    val contactEmail: String = "",
    val phoneInput: String = "",
    val emailTo: String = "",
    val emailSubject: String = "",
    val generatedBitmap: Bitmap? = null,
    val isSavedToHistory: Boolean = false,
    val infoMessage: String? = null
)

class CreateQrViewModel(
    private val repository: ScanHistoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateQrUiState())
    val uiState: StateFlow<CreateQrUiState> = _uiState.asStateFlow()

    init {
        generateQrBitmap()
    }

    fun setCreateType(type: QrCreateType) {
        _uiState.value = _uiState.value.copy(
            selectedType = type,
            isSavedToHistory = false
        )
        generateQrBitmap()
    }

    fun updateUrl(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url, isSavedToHistory = false)
        generateQrBitmap()
    }

    fun updateText(text: String) {
        _uiState.value = _uiState.value.copy(textInput = text, isSavedToHistory = false)
        generateQrBitmap()
    }

    fun updateWifi(ssid: String, pass: String, sec: String) {
        _uiState.value = _uiState.value.copy(
            wifiSsid = ssid,
            wifiPassword = pass,
            wifiSecurity = sec,
            isSavedToHistory = false
        )
        generateQrBitmap()
    }

    fun updateContact(name: String, phone: String, email: String) {
        _uiState.value = _uiState.value.copy(
            contactName = name,
            contactPhone = phone,
            contactEmail = email,
            isSavedToHistory = false
        )
        generateQrBitmap()
    }

    fun updatePhone(phone: String) {
        _uiState.value = _uiState.value.copy(phoneInput = phone, isSavedToHistory = false)
        generateQrBitmap()
    }

    fun updateEmail(to: String, subject: String) {
        _uiState.value = _uiState.value.copy(
            emailTo = to,
            emailSubject = subject,
            isSavedToHistory = false
        )
        generateQrBitmap()
    }

    fun getPayloadString(): String {
        val state = _uiState.value
        return when (state.selectedType) {
            QrCreateType.URL -> state.urlInput.trim()
            QrCreateType.TEXT -> state.textInput.trim()
            QrCreateType.WIFI -> {
                val t = if (state.wifiSecurity == "None") "nopass" else state.wifiSecurity
                "WIFI:T:$t;S:${state.wifiSsid};P:${state.wifiPassword};;"
            }
            QrCreateType.CONTACT -> {
                "BEGIN:VCARD\nVERSION:3.0\nN:${state.contactName}\nFN:${state.contactName}\nTEL:${state.contactPhone}\nEMAIL:${state.contactEmail}\nEND:VCARD"
            }
            QrCreateType.PHONE -> "tel:${state.phoneInput.trim()}"
            QrCreateType.EMAIL -> "mailto:${state.emailTo.trim()}?subject=${state.emailSubject.trim()}"
        }
    }

    private fun generateQrBitmap() {
        val payload = getPayloadString()
        if (payload.isBlank() || payload == "https://" || payload == "tel:") {
            _uiState.value = _uiState.value.copy(generatedBitmap = null)
            return
        }
        val bitmap = QrCodeGenerator.generateQrBitmap(
            content = payload,
            sizePx = 512,
            foregroundColor = Color(0xFF1E293B),
            backgroundColor = Color.White
        )
        _uiState.value = _uiState.value.copy(generatedBitmap = bitmap)
    }

    fun saveCreatedQrToHistory() {
        val state = _uiState.value
        val payload = getPayloadString()
        if (payload.isBlank()) return

        val barcodeType = when (state.selectedType) {
            QrCreateType.URL -> BarcodeType.URL
            QrCreateType.TEXT -> BarcodeType.TEXT
            QrCreateType.WIFI -> BarcodeType.WIFI
            QrCreateType.CONTACT -> BarcodeType.CONTACT
            QrCreateType.PHONE -> BarcodeType.PHONE
            QrCreateType.EMAIL -> BarcodeType.EMAIL
        }

        val title = when (state.selectedType) {
            QrCreateType.URL -> state.urlInput.replace("https://", "").replace("http://", "").ifEmpty { "Website" }
            QrCreateType.TEXT -> state.textInput.take(30).ifEmpty { "Text" }
            QrCreateType.WIFI -> state.wifiSsid.ifEmpty { "Wi-Fi" }
            QrCreateType.CONTACT -> state.contactName.ifEmpty { "Contact" }
            QrCreateType.PHONE -> state.phoneInput.ifEmpty { "Phone" }
            QrCreateType.EMAIL -> state.emailTo.ifEmpty { "Email" }
        }

        val result = ParsedBarcodeResult(
            rawValue = payload,
            displayValue = payload,
            format = "QR Code",
            type = barcodeType,
            title = title,
            subtitle = "Created QR Code",
            wifiSsid = state.wifiSsid.ifBlank { null },
            wifiPassword = state.wifiPassword.ifBlank { null },
            contactName = state.contactName.ifBlank { null },
            contactPhone = state.contactPhone.ifBlank { null },
            contactEmail = state.contactEmail.ifBlank { null }
        )

        viewModelScope.launch {
            repository.saveScan(result)
            _uiState.value = _uiState.value.copy(
                isSavedToHistory = true,
                infoMessage = "Saved to your scan history"
            )
        }
    }

    fun clearInfoMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }

    companion object {
        fun provideFactory(repository: ScanHistoryRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return CreateQrViewModel(repository) as T
                }
            }
    }
}
