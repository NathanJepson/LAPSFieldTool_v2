package com.example.lapsfieldtool_v2.ui.computer

import androidx.appcompat.app.AppCompatActivity
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lapsfieldtool_v2.data.TokenManager
import com.example.lapsfieldtool_v2.data.api.DeviceCredentialsService
import com.example.lapsfieldtool_v2.databinding.ActivityComputerBinding
import com.example.lapsfieldtool_v2.R
import com.example.lapsfieldtool_v2.ui.login.LoginActivity
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ComputerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityComputerBinding
    private lateinit var fullscreenContent: TextView
    private lateinit var fullscreenContentControls: LinearLayout
    private val hideHandler = Handler(Looper.myLooper()!!)
    private lateinit var credentialsAdapter: CredentialsAdapter
    private lateinit var credentialsRecyclerView: RecyclerView
    private lateinit var tokenManager: TokenManager
    private val deviceCredentialsService = DeviceCredentialsService()

    @SuppressLint("InlinedApi")
    private val hidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LOW_PROFILE or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        }
    }

    private val showPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        fullscreenContentControls.visibility = View.VISIBLE
    }

    private var isFullscreen: Boolean = false

    private val hideRunnable = Runnable { hide() }

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private val delayHideTouchListener = View.OnTouchListener { view, motionEvent ->
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS)
            }
            MotionEvent.ACTION_UP -> view.performClick()
            else -> {
            }
        }
        false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityComputerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        isFullscreen = true

        // Set up the UI elements
        fullscreenContent = binding.fullscreenContent
        fullscreenContent.setOnClickListener { toggle() }
        fullscreenContentControls = binding.fullscreenContentControls

        // Set up the recycler view
        credentialsRecyclerView = binding.credentialsRecyclerView
        credentialsAdapter = CredentialsAdapter()
        credentialsRecyclerView.layoutManager = LinearLayoutManager(this)
        credentialsRecyclerView.adapter = credentialsAdapter

        // Set up the back button
        binding.dummyButton.setOnClickListener {
            finish() // Return to previous activity
        }

        // Get the token manager
        tokenManager = TokenManager.getInstance(applicationContext)

        // Get device ID from intent
        val deviceId = intent.getStringExtra("device_id")
        val deviceName = intent.getStringExtra("device_name") ?: "Unknown Device"

        fullscreenContent.text = "Loading credentials for $deviceName..."

        if (deviceId != null) {
            // Fetch the credentials
            fetchDeviceCredentials(deviceId)
        } else {
            Toast.makeText(this, "No device ID provided", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        binding.dummyButton.setOnTouchListener(delayHideTouchListener)
    }

    private fun fetchDeviceCredentials(deviceId: String) {
        lifecycleScope.launch {
            val token = tokenManager.token.firstOrNull()
            if (token != null) {
                deviceCredentialsService.getDeviceCredentials(
                    deviceId = deviceId,
                    accessToken = token,
                    onSuccess = { deviceCredentials ->
                        // Update UI with credentials
                        fullscreenContent.text = "Device: ${deviceCredentials.deviceName}"

                        // Format date for display
                        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", Locale.US)
                        val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

                        val lastBackupDate = try {
                            val date = inputFormat.parse(deviceCredentials.lastBackupDateTime)
                            "Last Backup: ${outputFormat.format(date!!)}"
                        } catch (e: Exception) {
                            "Last Backup: ${deviceCredentials.lastBackupDateTime}"
                        }

                        val refreshDate = try {
                            val date = inputFormat.parse(deviceCredentials.refreshDateTime)
                            "Next Refresh: ${outputFormat.format(date!!)}"
                        } catch (e: Exception) {
                            "Next Refresh: ${deviceCredentials.refreshDateTime}"
                        }

                        fullscreenContent.text = "${deviceCredentials.deviceName}\n\n$lastBackupDate\n$refreshDate"

                        // Add this to confirm the adapter is being updated and to check the data
                        val credentialsCount = deviceCredentials.credentials.size
                        if (credentialsCount > 0) {
                            // Debug log or Toast to check if credentials are available
                            Toast.makeText(this@ComputerActivity, "Found $credentialsCount credentials", Toast.LENGTH_SHORT).show()
                        } else {
                            // If no credentials available
                            Toast.makeText(this@ComputerActivity, "No credentials available for this device", Toast.LENGTH_SHORT).show()
                        }

                        // Update the adapter with the credentials
                        credentialsAdapter.updateCredentials(deviceCredentials.credentials)
                    },
                    onError = { errorMessage ->
                        Toast.makeText(this@ComputerActivity, errorMessage, Toast.LENGTH_LONG).show()
                        fullscreenContent.text = "Error loading credentials"
                    }
                )
            } else {
                Toast.makeText(this@ComputerActivity, "Authentication token expired.", Toast.LENGTH_SHORT).show()
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                val intent = Intent(applicationContext, LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (isFullscreen) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreenContentControls.visibility = View.GONE
        isFullscreen = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        hideHandler.removeCallbacks(showPart2Runnable)
        hideHandler.postDelayed(hidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        if (Build.VERSION.SDK_INT >= 30) {
            fullscreenContent.windowInsetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
        } else {
            fullscreenContent.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        isFullscreen = true

        // Schedule a runnable to display UI elements after a delay
        hideHandler.removeCallbacks(hidePart2Runnable)
        hideHandler.postDelayed(showPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun delayedHide(delayMillis: Int) {
        hideHandler.removeCallbacks(hideRunnable)
        hideHandler.postDelayed(hideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private const val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private const val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private const val UI_ANIMATION_DELAY = 300
    }
}