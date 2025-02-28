package com.example.lapsfieldtool_v2.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.security.SecureRandom

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "token_preferences")

class TokenManager (context: Context) {

    private val appContext: Context = context.applicationContext
    private val dataStore: DataStore<Preferences> = appContext.dataStore

    companion object {

        private const val STORE_NAME = "token_preferences"
        private val EXPIRATION_KEY = stringPreferencesKey("token_expiration")
        private val ENCRYPTED_TOKEN_KEY = stringPreferencesKey("encrypted_token")
        private val IV_KEY = stringPreferencesKey("token_iv")
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val TAG_LENGTH = 128

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Get or generate encryption key
    private fun getEncryptionKey(): SecretKey {
        val sharedPrefs = appContext.getSharedPreferences("key_prefs", Context.MODE_PRIVATE)
        val keyString = sharedPrefs.getString("encryption_key", null)

        return if (keyString != null) {
            val decodedKey = Base64.getDecoder().decode(keyString)
            SecretKeySpec(decodedKey, "AES")
        } else {
            // Generate a new key
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(KEY_SIZE)
            val secretKey = keyGenerator.generateKey()

            // Save the key
            val encodedKey = Base64.getEncoder().encodeToString(secretKey.encoded)
            sharedPrefs.edit().putString("encryption_key", encodedKey).apply()

            secretKey
        }
    }

    // Encrypt the token
    private fun encryptToken(token: String): Pair<String, String> {
        val key = getEncryptionKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(12) // 96 bits IV for GCM
        SecureRandom().nextBytes(iv)

        val parameterSpec = GCMParameterSpec(TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)

        val encryptedBytes = cipher.doFinal(token.toByteArray())
        val encryptedToken = Base64.getEncoder().encodeToString(encryptedBytes)
        val ivString = Base64.getEncoder().encodeToString(iv)

        return Pair(encryptedToken, ivString)
    }

    // Decrypt the token
    private fun decryptToken(encryptedToken: String, ivString: String): String? {
        return try {
            val key = getEncryptionKey()
            val cipher = Cipher.getInstance(ALGORITHM)

            val iv = Base64.getDecoder().decode(ivString)
            val parameterSpec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)

            val encryptedBytes = Base64.getDecoder().decode(encryptedToken)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            String(decryptedBytes)
        } catch (e: Exception) {
            null
        }
    }

    // Save the bearer token securely
    suspend fun saveToken(token: String, expiresIn: Int) {
        val (encryptedToken, iv) = encryptToken(token)

        val expirationTime = System.currentTimeMillis() + (expiresIn * 1000) // Convert seconds to milliseconds

        dataStore.edit { preferences ->
            preferences[ENCRYPTED_TOKEN_KEY] = encryptedToken
            preferences[IV_KEY] = iv
            preferences[EXPIRATION_KEY] = expirationTime.toString()
        }
    }

    // Retrieve the bearer token
    val token: Flow<String?> = dataStore.data
        .map { preferences ->
            val encryptedToken = preferences[ENCRYPTED_TOKEN_KEY] ?: return@map null
            val iv = preferences[IV_KEY] ?: return@map null
            val expirationTime = preferences[EXPIRATION_KEY]?.toLongOrNull() ?: return@map null

            if (System.currentTimeMillis() >= expirationTime) {
                null // Token expired, return null
            } else {
                decryptToken(encryptedToken, iv) // Token valid, return decrypted token
            }
        }

    // Clear the token when needed (e.g., for logout)
    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(ENCRYPTED_TOKEN_KEY)
            preferences.remove(IV_KEY)
        }
    }

    //Check token expired
    suspend fun checkTokenExpired(): Boolean {
        val preferences = dataStore.data.first() // Get the latest stored data synchronously
        val expirationTime = preferences[EXPIRATION_KEY]?.toLongOrNull()
        return (expirationTime == null || System.currentTimeMillis() >= expirationTime)
    }
}