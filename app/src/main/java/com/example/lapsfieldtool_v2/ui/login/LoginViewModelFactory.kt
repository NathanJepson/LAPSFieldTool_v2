package com.example.lapsfieldtool_v2.ui.login

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lapsfieldtool_v2.data.LoginDataSource
import com.example.lapsfieldtool_v2.data.LoginRepository
import com.example.lapsfieldtool_v2.data.TokenManager

/**
 * ViewModel provider factory to instantiate LoginViewModel.
 * Required given LoginViewModel has a non-empty constructor
 */
class LoginViewModelFactory(private val application: Application) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            return LoginViewModel(
                loginRepository = LoginRepository(
                    dataSource = LoginDataSource()
                ), tokenManager = TokenManager(application)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}