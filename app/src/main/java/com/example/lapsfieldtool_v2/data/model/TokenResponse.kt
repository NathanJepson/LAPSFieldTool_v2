package com.example.lapsfieldtool_v2.data.model

data class TokenResponse(
    val tokenType: String,
    val expiresIn: Int,
    val extExpiresIn: Int,
    val accessToken: String
)
