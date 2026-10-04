package com.example.domain.model

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.mlkit.vision.barcode.common.Barcode

data class ParsedBarcodeResult(
    val id: Long = 0,
    val rawValue: String,
    val displayValue: String,
    val format: String,
    val type: BarcodeType,
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
    companion object {
        fun fromMlKitBarcode(barcode: Barcode): ParsedBarcodeResult {
            val raw = barcode.rawValue ?: barcode.displayValue ?: ""
            val formatStr = when (barcode.format) {
                Barcode.FORMAT_QR_CODE -> "QR Code"
                Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
                Barcode.FORMAT_AZTEC -> "Aztec"
                Barcode.FORMAT_PDF417 -> "PDF417"
                Barcode.FORMAT_EAN_13 -> "EAN-13"
                Barcode.FORMAT_EAN_8 -> "EAN-8"
                Barcode.FORMAT_UPC_A -> "UPC-A"
                Barcode.FORMAT_UPC_E -> "UPC-E"
                Barcode.FORMAT_CODE_128 -> "Code 128"
                Barcode.FORMAT_CODE_39 -> "Code 39"
                Barcode.FORMAT_CODE_93 -> "Code 93"
                Barcode.FORMAT_CODABAR -> "Codabar"
                Barcode.FORMAT_ITF -> "ITF"
                else -> "Barcode"
            }

            return when (barcode.valueType) {
                Barcode.TYPE_URL -> {
                    val url = barcode.url?.url ?: raw
                    ParsedBarcodeResult(
                        rawValue = raw,
                        displayValue = url,
                        format = formatStr,
                        type = BarcodeType.URL,
                        title = extractDomain(url) ?: "Web Link",
                        subtitle = url
                    )
                }
                Barcode.TYPE_WIFI -> {
                    val wifi = barcode.wifi
                    val ssid = wifi?.ssid ?: "Unknown Wi-Fi"
                    val password = wifi?.password
                    val encType = when (wifi?.encryptionType) {
                        Barcode.WiFi.TYPE_WPA -> "WPA/WPA2"
                        Barcode.WiFi.TYPE_WEP -> "WEP"
                        Barcode.WiFi.TYPE_OPEN -> "Open"
                        else -> "Secured"
                    }
                    ParsedBarcodeResult(
                        rawValue = raw,
                        displayValue = "SSID: $ssid ($encType)",
                        format = formatStr,
                        type = BarcodeType.WIFI,
                        title = ssid,
                        subtitle = "Wi-Fi Network ($encType)",
                        wifiSsid = ssid,
                        wifiPassword = password,
                        wifiEncryption = encType
                    )
                }
                Barcode.TYPE_CONTACT_INFO -> {
                    val contact = barcode.contactInfo
                    val name = contact?.name?.formattedName ?: "Contact"
                    val phone = contact?.phones?.firstOrNull()?.number
                    val email = contact?.emails?.firstOrNull()?.address
                    ParsedBarcodeResult(
                        rawValue = raw,
                        displayValue = name,
                        format = formatStr,
                        type = BarcodeType.CONTACT,
                        title = name,
                        subtitle = phone ?: email ?: "Contact Details",
                        contactName = name,
                        contactPhone = phone,
                        contactEmail = email
                    )
                }
                Barcode.TYPE_EMAIL -> {
                    val email = barcode.email
                    val address = email?.address ?: raw
                    val subject = email?.subject
                    val body = email?.body
                    ParsedBarcodeResult(
                        rawValue = raw,
                        displayValue = address,
                        format = formatStr,
                        type = BarcodeType.EMAIL,
                        title = address,
                        subtitle = subject ?: "Email",
                        contactEmail = address,
                        emailSubject = subject,
                        emailBody = body
                    )
                }
                Barcode.TYPE_PHONE -> {
                    val phone = barcode.phone?.number ?: raw
                    ParsedBarcodeResult(
                        rawValue = raw,
                        displayValue = phone,
                        format = formatStr,
                        type = BarcodeType.PHONE,
                        title = phone,
                        subtitle = "Phone Number",
                        contactPhone = phone
                    )
                }
                Barcode.TYPE_SMS -> {
                    val sms = barcode.sms
                    val phone = sms?.phoneNumber ?: ""
                    val message = sms?.message
                    ParsedBarcodeResult(
                        rawValue = raw,
                        displayValue = phone,
                        format = formatStr,
                        type = BarcodeType.SMS,
                        title = phone.ifEmpty { "SMS Message" },
                        subtitle = message ?: "SMS",
                        contactPhone = phone,
                        smsMessage = message
                    )
                }
                Barcode.TYPE_GEO -> {
                    val geo = barcode.geoPoint
                    val lat = geo?.lat ?: 0.0
                    val lng = geo?.lng ?: 0.0
                    ParsedBarcodeResult(
                        rawValue = raw,
                        displayValue = "$lat, $lng",
                        format = formatStr,
                        type = BarcodeType.GEO,
                        title = "Location Coordinates",
                        subtitle = "$lat, $lng",
                        geoLatitude = lat,
                        geoLongitude = lng
                    )
                }
                Barcode.TYPE_PRODUCT -> {
                    ParsedBarcodeResult(
                        rawValue = raw,
                        displayValue = raw,
                        format = formatStr,
                        type = BarcodeType.PRODUCT,
                        title = "$formatStr: $raw",
                        subtitle = "Product Code"
                    )
                }
                else -> {
                    // Check if rawValue looks like a URL or Wi-Fi string
                    if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
                        ParsedBarcodeResult(
                            rawValue = raw,
                            displayValue = raw,
                            format = formatStr,
                            type = BarcodeType.URL,
                            title = extractDomain(raw) ?: "Web Link",
                            subtitle = raw
                        )
                    } else if (raw.startsWith("WIFI:", ignoreCase = true)) {
                        val ssid = parseWifiSsid(raw)
                        val pass = parseWifiPass(raw)
                        ParsedBarcodeResult(
                            rawValue = raw,
                            displayValue = ssid,
                            format = formatStr,
                            type = BarcodeType.WIFI,
                            title = ssid,
                            subtitle = "Wi-Fi Network",
                            wifiSsid = ssid,
                            wifiPassword = pass
                        )
                    } else {
                        val firstLine = raw.lines().firstOrNull()?.take(50) ?: "Text Content"
                        ParsedBarcodeResult(
                            rawValue = raw,
                            displayValue = raw,
                            format = formatStr,
                            type = BarcodeType.TEXT,
                            title = firstLine,
                            subtitle = if (raw.length > 50) "${raw.take(50)}..." else raw
                        )
                    }
                }
            }
        }

        private fun extractDomain(url: String): String? {
            return try {
                val uri = Uri.parse(url)
                uri.host ?: url
            } catch (e: Exception) {
                null
            }
        }

        private fun parseWifiSsid(raw: String): String {
            val match = Regex("S:([^;]+)").find(raw)
            return match?.groupValues?.getOrNull(1) ?: "Wi-Fi Network"
        }

        private fun parseWifiPass(raw: String): String? {
            val match = Regex("P:([^;]+)").find(raw)
            return match?.groupValues?.getOrNull(1)
        }
    }

    fun executePrimaryAction(context: Context) {
        try {
            when (type) {
                BarcodeType.URL -> {
                    val urlToOpen = if (!rawValue.startsWith("http://") && !rawValue.startsWith("https://")) {
                        "https://$rawValue"
                    } else rawValue
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(urlToOpen))
                    context.startActivity(intent)
                }
                BarcodeType.PHONE -> {
                    val phoneUri = "tel:${contactPhone ?: rawValue}"
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse(phoneUri))
                    context.startActivity(intent)
                }
                BarcodeType.EMAIL -> {
                    val emailAddress = contactEmail ?: rawValue
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$emailAddress")
                        emailSubject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                        emailBody?.let { putExtra(Intent.EXTRA_TEXT, it) }
                    }
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                }
                BarcodeType.SMS -> {
                    val phone = contactPhone ?: ""
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
                        smsMessage?.let { putExtra("sms_body", it) }
                    }
                    context.startActivity(intent)
                }
                BarcodeType.GEO -> {
                    val geoUri = if (geoLatitude != null && geoLongitude != null) {
                        "geo:${geoLatitude},${geoLongitude}?q=${geoLatitude},${geoLongitude}(Location)"
                    } else {
                        "geo:0,0?q=$rawValue"
                    }
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(geoUri))
                    context.startActivity(intent)
                }
                BarcodeType.PRODUCT -> {
                    // Search product on web
                    val searchUrl = "https://www.google.com/search?q=$rawValue"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                    context.startActivity(intent)
                }
                BarcodeType.CONTACT -> {
                    val intent = Intent(Intent.ACTION_INSERT_OR_EDIT).apply {
                        type = "vnd.android.cursor.item/contact"
                        contactName?.let { putExtra(android.provider.ContactsContract.Intents.Insert.NAME, it) }
                        contactPhone?.let { putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, it) }
                        contactEmail?.let { putExtra(android.provider.ContactsContract.Intents.Insert.EMAIL, it) }
                    }
                    context.startActivity(intent)
                }
                BarcodeType.WIFI -> {
                    // Wi-Fi can't be auto-joined on modern Android without specific settings intent, so copy password or open wifi settings
                    val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
                    context.startActivity(intent)
                    Toast.makeText(context, "Opened Wi-Fi Settings. Password copied if available.", Toast.LENGTH_SHORT).show()
                }
                BarcodeType.TEXT -> {
                    // Web search or share
                    val searchUrl = "https://www.google.com/search?q=${Uri.encode(rawValue)}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))
                    context.startActivity(intent)
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to complete action: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
