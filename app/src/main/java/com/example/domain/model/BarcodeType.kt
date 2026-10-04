package com.example.domain.model

enum class BarcodeType {
    URL,
    WIFI,
    CONTACT,
    EMAIL,
    PHONE,
    SMS,
    GEO,
    PRODUCT,
    TEXT;

    val displayName: String
        get() = when (this) {
            URL -> "Website Link"
            WIFI -> "Wi-Fi Network"
            CONTACT -> "Contact Card"
            EMAIL -> "Email Address"
            PHONE -> "Phone Number"
            SMS -> "SMS Message"
            GEO -> "Location Map"
            PRODUCT -> "Product Barcode"
            TEXT -> "Plain Text"
        }
}
