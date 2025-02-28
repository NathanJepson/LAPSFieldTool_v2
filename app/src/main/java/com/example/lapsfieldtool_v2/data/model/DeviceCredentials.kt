package com.example.lapsfieldtool_v2.data.model

data class DeviceCredentials(
    val id: String,
    val deviceName: String,
    val lastBackupDateTime: String,
    val refreshDateTime: String,
    val credentials: List<Credential>
) {
    data class Credential(
        val accountName: String,
        val accountSid: String,
        val backupDateTime: String,
        val passwordBase64: String,
        val decodedPassword: String
    )
}