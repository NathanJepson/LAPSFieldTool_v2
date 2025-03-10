package com.example.lapsfieldtool_v2.ui.login

/**
 * Data validation state of the login form.
 */
data class LoginFormState(
    val tenantIdError: Int? = null,
    val usernameError: Int? = null,
    val passwordError: Int? = null,
    val isDataValid: Boolean = false
)