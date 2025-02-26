package com.example.lapsfieldtool_v2.ui.login

import android.app.Activity
import android.content.Context
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Toast
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.lapsfieldtool_v2.databinding.ActivityLoginBinding

import com.example.lapsfieldtool_v2.R

class LoginActivity : AppCompatActivity() {

    private lateinit var loginViewModel: LoginViewModel
    private lateinit var binding: ActivityLoginBinding

    private val PREFS_NAME = "LoginPrefs"
    //UserName is the ClientID or AppID
    private val KEY_USERNAME = "username"
    private val KEY_TENANT_ID = "tenant_id"
    private val KEY_TRUST_TYPE = "trust_type"

    // Mapping from dropdown options to trust type values
    private val trustTypeMap = mapOf(
        "ServerAd (Hybrid)" to "ServerAd",
        "AzureAd (Cloud only)" to "AzureAd",
        "Workplace" to "Workplace"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val username = binding.username
        val password = binding.password
        val tenantId = binding.test5!!
        val login = binding.login
        val loading = binding.loading

        val spinner: Spinner = findViewById(R.id.spinner)

        val adapter = ArrayAdapter.createFromResource(
            this,R.array.dropdown_options, android.R.layout.simple_spinner_dropdown_item
        )

        spinner.adapter = adapter

        // Load persisted values from SharedPreferences
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        tenantId.setText(prefs.getString(KEY_TENANT_ID, ""))
        username.setText(prefs.getString(KEY_USERNAME, ""))

        val savedTrustTypePosition = prefs.getInt(KEY_TRUST_TYPE, 0)
        if (savedTrustTypePosition < spinner.adapter.count) {
            spinner.setSelection(savedTrustTypePosition)
        }

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        //Handle selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent?.getItemAtPosition(position).toString()
                with(prefs.edit()) {
                    putInt(KEY_TRUST_TYPE, position)
                    apply()
                }

                // Get the trust type value from the map
                val trustType = trustTypeMap[selectedItem] ?: "ServerAd" // Default to ServerAd
                loginViewModel.setTrustType(trustType)            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.loginFormState.observe(this@LoginActivity, Observer {
            val loginState = it ?: return@Observer

            // disable login button unless both username / password is valid
            login.isEnabled = loginState.isDataValid

            if (loginState.usernameError != null) {
                username.error = getString(loginState.usernameError)
            }
            if (loginState.passwordError != null) {
                password.error = getString(loginState.passwordError)
            }
            if (loginState.tenantIdError != null) {
                tenantId.error = getString(loginState.tenantIdError)
            }
        })

        loginViewModel.loginResult.observe(this@LoginActivity, Observer {
            val loginResult = it ?: return@Observer

            loading.visibility = View.GONE
            if (loginResult.error != null) {
                showLoginFailed(loginResult.error)
            }
            if (loginResult.success != null) {
                // Save the entered tenant ID and client ID for future auto-fill
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putString(KEY_TENANT_ID, tenantId.text.toString())
                    putString(KEY_USERNAME, username.text.toString())
                    apply() // Asynchronously saves the data
                }

                updateUiWithUser(loginResult.success)
            }
            setResult(Activity.RESULT_OK)

            //Complete and destroy login activity once successful
            finish()
        })

        tenantId.afterTextChanged {
            loginViewModel.loginDataChanged(
                tenantId.text.toString(),
                username.text.toString(),
                password.text.toString()
            )
        }

        username.afterTextChanged {
            loginViewModel.loginDataChanged(
                tenantId.text.toString(),
                username.text.toString(),
                password.text.toString()
            )
        }

        password.apply {
            afterTextChanged {
                loginViewModel.loginDataChanged(
                    tenantId.text.toString(),
                    username.text.toString(),
                    password.text.toString()
                )
            }

            setOnEditorActionListener { _, actionId, _ ->
                when (actionId) {
                    EditorInfo.IME_ACTION_DONE ->
                        loginViewModel.login(
                            tenantId.text.toString(),
                            username.text.toString(),
                            password.text.toString()
                        )
                }
                false
            }

            login.setOnClickListener {
                loading.visibility = View.VISIBLE
                loginViewModel.login(tenantId.text.toString(),username.text.toString(), password.text.toString())
            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome)
        val displayName = model.displayName
        // TODO : initiate successful logged in experience
        Toast.makeText(
            applicationContext,
            "$welcome $displayName",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        Toast.makeText(applicationContext, errorString, Toast.LENGTH_SHORT).show()
    }
}

/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}