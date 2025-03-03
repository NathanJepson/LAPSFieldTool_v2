package com.example.lapsfieldtool_v2.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "token_preferences")

class TokenManager (context: Context) {

    private val appContext: Context = context.applicationContext
    private val dataStore: DataStore<Preferences> = appContext.dataStore

    companion object {

        private const val KEY_ALIAS = "token_encryption_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALGORITHM = "AES/GCM/NoPadding"
        private const val TAG_LENGTH = 128
        private const val KEY_SIZE = 256
        private val EXPIRATION_KEY = stringPreferencesKey("token_expiration")
        private val ENCRYPTED_TOKEN_KEY = stringPreferencesKey("encrypted_token")
        private val IV_KEY = stringPreferencesKey("token_iv")

        @Volatile
        private var INSTANCE: TokenManager? = null

        fun getInstance(context: Context): TokenManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TokenManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // Get or create the encryption key in Android Keystore
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        // Check if key already exists
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )

            val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE)
                .setRandomizedEncryptionRequired(true)
                .build()

            keyGenerator.init(keyGenParameterSpec)
            return keyGenerator.generateKey()
        }

        // Return the existing key
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    // Encrypt the token
    private fun encryptToken(token: String): Pair<String, ByteArray> {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val encryptedBytes = cipher.doFinal(token.toByteArray())
        val encryptedToken = Base64.getEncoder().encodeToString(encryptedBytes)
        val iv = cipher.iv

        return Pair(encryptedToken, iv)
    }

    // Decrypt the token
    private fun decryptToken(encryptedToken: String, iv: ByteArray): String? {
        return try {
            val key = getOrCreateKey()
            val cipher = Cipher.getInstance(ALGORITHM)
            val spec = GCMParameterSpec(TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val encryptedBytes = Base64.getDecoder().decode(encryptedToken)
            val decryptedBytes = cipher.doFinal(encryptedBytes)

            String(decryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Save the bearer token securely
    suspend fun saveToken(token: String, expiresIn: Int) {
        val (encryptedToken, iv) = encryptToken(token)
        val ivString = Base64.getEncoder().encodeToString(iv)
        val expirationTime = System.currentTimeMillis() + (expiresIn * 1000) // Convert seconds to milliseconds

        dataStore.edit { preferences ->
            preferences[ENCRYPTED_TOKEN_KEY] = encryptedToken
            preferences[IV_KEY] = ivString
            preferences[EXPIRATION_KEY] = expirationTime.toString()
        }
    }

    // Retrieve the bearer token
    val token: Flow<String?> = dataStore.data
        .map { preferences ->
            val encryptedToken = preferences[ENCRYPTED_TOKEN_KEY] ?: return@map null
            val ivString = preferences[IV_KEY] ?: return@map null
            val iv = Base64.getDecoder().decode(ivString)
            val expirationTime = preferences[EXPIRATION_KEY]?.toLongOrNull() ?: return@map null

            if (System.currentTimeMillis() >= expirationTime) {
                null // Token expired, return null
            } else {
                decryptToken(encryptedToken, iv) // Token valid, return decrypted token
            }
        }

    // Clear the token when needed
    suspend fun clearToken() {
        dataStore.edit { preferences ->
            preferences.remove(ENCRYPTED_TOKEN_KEY)
            preferences.remove(IV_KEY)
            preferences.remove(EXPIRATION_KEY)
        }
    }

    //Check token expired
    suspend fun checkTokenExpired(): Boolean {
        val preferences = dataStore.data.first() // Get the latest stored data synchronously
        val expirationTime = preferences[EXPIRATION_KEY]?.toLongOrNull()
        return (expirationTime == null || System.currentTimeMillis() >= expirationTime)
    }
}