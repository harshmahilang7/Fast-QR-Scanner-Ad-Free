package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.BarcodeType
import com.example.domain.model.ParsedBarcodeResult

@Entity(tableName = "scan_history")
data class ScanItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val rawValue: String,
    val displayValue: String,
    val format: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val wifiSsid: String? = null,
    val wifiPassword: String? = null,
    val wifiEncryption: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactEmail: String? = null,
    val geoLatitude: Double? = null,
    val geoLongitude: Double? = null,
    val emailSubject: String? = null,
    val emailBody: String? = null,
    val smsMessage: String? = null
) {
    fun toDomain(): ParsedBarcodeResult {
        val barcodeType = try {
            BarcodeType.valueOf(type)
        } catch (e: Exception) {
            BarcodeType.TEXT
        }
        return ParsedBarcodeResult(
            id = id,
            rawValue = rawValue,
            displayValue = displayValue,
            format = format,
            type = barcodeType,
            title = title,
            subtitle = subtitle,
            timestamp = timestamp,
            isFavorite = isFavorite,
            wifiSsid = wifiSsid,
            wifiPassword = wifiPassword,
            wifiEncryption = wifiEncryption,
            contactName = contactName,
            contactPhone = contactPhone,
            contactEmail = contactEmail,
            geoLatitude = geoLatitude,
            geoLongitude = geoLongitude,
            emailSubject = emailSubject,
            emailBody = emailBody,
            smsMessage = smsMessage
        )
    }

    companion object {
        fun fromDomain(domain: ParsedBarcodeResult): ScanItemEntity {
            return ScanItemEntity(
                id = domain.id,
                rawValue = domain.rawValue,
                displayValue = domain.displayValue,
                format = domain.format,
                type = domain.type.name,
                title = domain.title,
                subtitle = domain.subtitle,
                timestamp = domain.timestamp,
                isFavorite = domain.isFavorite,
                wifiSsid = domain.wifiSsid,
                wifiPassword = domain.wifiPassword,
                wifiEncryption = domain.wifiEncryption,
                contactName = domain.contactName,
                contactPhone = domain.contactPhone,
                contactEmail = domain.contactEmail,
                geoLatitude = domain.geoLatitude,
                geoLongitude = domain.geoLongitude,
                emailSubject = domain.emailSubject,
                emailBody = domain.emailBody,
                smsMessage = domain.smsMessage
            )
        }
    }
}
