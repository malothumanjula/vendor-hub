package com.vendorapp.data

enum class AppLanguage(
    val storageKey: String,
    val nativeName: String,
    val englishName: String,
    val symbol: String
) {
    ENGLISH("english", "English", "English", "A"),
    TELUGU("telugu", "తెలుగు", "Telugu", "అ"),
    HINDI("hindi", "हिंदी", "Hindi", "अ");

    companion object {
        fun fromStorageKey(value: String?): AppLanguage? =
            entries.firstOrNull { it.storageKey == value }
    }
}
