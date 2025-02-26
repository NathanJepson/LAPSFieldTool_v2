package com.example.lapsfieldtool_v2.data.api

import com.example.lapsfieldtool_v2.data.model.Device
import com.example.lapsfieldtool_v2.data.model.TokenResponse
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

class MicrosoftAuthService {
    private val executor = Executors.newSingleThreadExecutor()

    fun getToken(
        tenantId: String,
        clientId: String,
        clientSecret: String,
        onSuccess: (TokenResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                val tokenUrl = "https://login.microsoft.com/$tenantId/oauth2/v2.0/token"
                val url = URL(tokenUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.doOutput = true

                // Create the form data
                val formData = StringBuilder()
                formData.append("grant_type=").append("client_credentials")
                formData.append("&client_id=").append(URLEncoder.encode(clientId, "UTF-8"))
                formData.append("&client_secret=").append(URLEncoder.encode(clientSecret, "UTF-8"))
                formData.append("&scope=").append(URLEncoder.encode("https://graph.microsoft.com/.default", "UTF-8"))

                // Write the form data to the connection
                val outputStream = connection.outputStream
                val writer = OutputStreamWriter(outputStream, "UTF-8")
                writer.write(formData.toString())
                writer.flush()
                writer.close()

                // Check the response code
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Parse the response
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)

                    val tokenResponse = TokenResponse(
                        tokenType = jsonObject.getString("token_type"),
                        expiresIn = jsonObject.getInt("expires_in"),
                        extExpiresIn = jsonObject.optInt("ext_expires_in"),
                        accessToken = jsonObject.getString("access_token")
                    )

                    // Call the success callback on the main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onSuccess(tokenResponse)
                    }
                } else {
                    // Get the error message from the response
                    val errorStream = connection.errorStream
                    val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"

                    // Call the error callback on the main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onError("Authentication failed: $errorResponse")
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onError("Authentication failed: ${e.message}")
                }
            }
        }
    }
}

class MicrosoftGraphService {
    private val executor = Executors.newSingleThreadExecutor()

    fun getDevices(
        accessToken: String,
        trustType: String = "ServerAd",
        onSuccess: (List<Device>) -> Unit,
        onError: (String) -> Unit
    ) {
        executor.execute {
            try {
                val encodedTrustType = URLEncoder.encode("'$trustType'", "UTF-8")
                val deviceUrl = "https://graph.microsoft.com/v1.0/devices?$" +
                        "filter=trustType+eq+$encodedTrustType&$" +
                        "select=deviceId%2cdisplayName"

                val url = URL(deviceUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $accessToken")

                // Check the response code
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Parse the response
                    val inputStream = connection.inputStream
                    val response = inputStream.bufferedReader().use { it.readText() }
                    val jsonObject = JSONObject(response)
                    val valueArray = jsonObject.getJSONArray("value")

                    val devices = mutableListOf<Device>()
                    for (i in 0 until valueArray.length()) {
                        val deviceObj = valueArray.getJSONObject(i)
                        val device = Device(
                            deviceId = deviceObj.getString("deviceId"),
                            displayName = deviceObj.optString("displayName", "Unknown Device")
                        )
                        devices.add(device)
                    }

                    var nextLink: String? = jsonObject.optString("@odata.nextLink","").ifEmpty { null }

                    while (nextLink != null) {
                        val url2 = URL(nextLink)
                        val connection2 = url2.openConnection() as HttpURLConnection
                        connection2.requestMethod = "GET"
                        connection2.setRequestProperty("Authorization", "Bearer $accessToken")
                        val responseCode2 = connection2.responseCode
                        if (responseCode2 == HttpURLConnection.HTTP_OK) {
                            val inputStream2 = connection2.inputStream
                            val response2 = inputStream2.bufferedReader().use { it.readText() }
                            val jsonObject2 = JSONObject(response2)
                            val valueArray2 = jsonObject2.getJSONArray("value")

                            for (i in 0 until valueArray2.length()) {
                                val deviceObj = valueArray2.getJSONObject(i)
                                val device = Device(
                                    deviceId = deviceObj.getString("deviceId"),
                                    displayName = deviceObj.optString("displayName", "Unknown Device")
                                )
                                devices.add(device)
                            }

                            nextLink = jsonObject2.optString("@odata.nextLink","").ifEmpty { null }
                        }
                    }

                    // Call the success callback on the main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onSuccess(devices)
                    }
                } else {
                    // Get the error message from the response
                    val errorStream = connection.errorStream
                    val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"

                    // Call the error callback on the main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onError("Failed to fetch devices: $errorResponse")
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    onError("Failed to fetch devices: ${e.message}")
                }
            }
        }
    }
}