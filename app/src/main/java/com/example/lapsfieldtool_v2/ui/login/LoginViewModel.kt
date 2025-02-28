package com.example.lapsfieldtool_v2.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import com.example.lapsfieldtool_v2.R
import com.example.lapsfieldtool_v2.data.api.MicrosoftAuthService
import com.example.lapsfieldtool_v2.data.api.MicrosoftGraphService
import com.example.lapsfieldtool_v2.data.model.Device
import com.example.lapsfieldtool_v2.data.model.LoggedInUser
import com.example.lapsfieldtool_v2.data.model.TokenResponse
import com.example.lapsfieldtool_v2.data.TokenManager
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LoginViewModel(private val tokenManager: TokenManager) : ViewModel() {

    private val authService = MicrosoftAuthService()
    private val graphService = MicrosoftGraphService()

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _devices = MutableLiveData<List<Device>>()
    val devices: LiveData<List<Device>> = _devices

    private var tenantId: String = ""

    // Trust type will be set from the spinner
    private var trustType: String = "ServerAd" // Default value

    fun setTenantId(tenantId: String) {
        this.tenantId = tenantId
    }

    fun setTrustType(trustType: String) {
        this.trustType = trustType
    }

    fun login(tenantId: String,username: String, password: String) {

        authService.getToken(
            tenantId = tenantId,
            clientId = username,
            clientSecret = password,
            onSuccess = { tokenResponse ->
                // Store the token for later use
                storeToken(tokenResponse)

                // Now fetch the devices
                fetchDevices(tokenResponse.accessToken)
            },
            onError = { errorMessage ->
                _loginResult.value = LoginResult(error = R.string.login_failed)
            }
        )
    }

    private fun storeToken(tokenResponse: TokenResponse) {
        val expiresIn = tokenResponse.expiresIn
        val token = tokenResponse.accessToken

        viewModelScope.launch {
            tokenManager.saveToken(token, expiresIn)
        }
    }

    private fun fetchDevices(accessToken: String) {
        graphService.getDevices(
            accessToken = accessToken,
            onSuccess = { deviceList ->
                // Store the device list
                _devices.value = deviceList

                // Create a logged-in user
                val user = LoggedInUser(
                    userId = java.util.UUID.randomUUID().toString(),
                    displayName = "Admin"
                )

                _loginResult.value = LoginResult(success = LoggedInUserView(displayName = user.displayName))
            },
            onError = { errorMessage ->
                _loginResult.value = LoginResult(error = R.string.login_failed)
            }
        )
    }

    fun loginDataChanged(tenantId: String, username: String, password: String) {
        if (!isTenantIdValid(tenantId)) {
            _loginForm.value = LoginFormState(tenantIdError = R.string.invalid_tenantid)
        } else if (!isUserNameValid(username)) {
            _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
        } else if (!isPasswordValid(password)) {
            _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
        } else {
            _loginForm.value = LoginFormState(isDataValid = true)
        }
    }

    private fun isTenantIdValid(tenantId: String): Boolean {
        return tenantId.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
    }

    private fun isUserNameValid(username: String): Boolean {
        return username.matches(Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"))
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}