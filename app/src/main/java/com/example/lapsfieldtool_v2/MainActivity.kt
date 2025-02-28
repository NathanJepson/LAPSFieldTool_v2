package com.example.lapsfieldtool_v2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lapsfieldtool_v2.data.TokenManager
import com.example.lapsfieldtool_v2.data.model.Device
import com.example.lapsfieldtool_v2.databinding.ActivityMainBinding
import com.example.lapsfieldtool_v2.ui.SearchAdapter
import com.example.lapsfieldtool_v2.ui.login.LoginActivity
import com.example.lapsfieldtool_v2.ui.theme.LAPSFieldTool_V2Theme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private var tokenManager: TokenManager? = null
    private var searchBar: EditText? = null
    private lateinit var binding: ActivityMainBinding
    private var recyclerView: RecyclerView? = null
    private lateinit var searchAdapter: SearchAdapter
    private var deviceList = ArrayList<Device>()
    private val searchHandler = android.os.Handler(android.os.Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        searchBar = findViewById(R.id.searchBar)
        recyclerView = findViewById(R.id.recyclerView)
        searchAdapter = SearchAdapter()

        // Explicitly set layout manager and adapter
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = searchAdapter

        tokenManager = TokenManager.getInstance(applicationContext)

        val intentDeviceList: ArrayList<Device>? = intent.getParcelableArrayListExtra<Device>("device_list", Device::class.java)
        intentDeviceList?.sortBy { it.displayName }

        // Condition to check if array is empty or bearer token is expired
        var tokenExpired = false
        if (tokenManager != null) {
            lifecycleScope.launch {
                tokenExpired = tokenManager!!.checkTokenExpired()
                if (tokenExpired) {
                    tokenManager!!.clearToken()
                }
            }
        }

        // If no devices or bearer token expired, start LoginActivity
       if (tokenExpired) {
            if (!intentDeviceList.isNullOrEmpty()) {
                Toast.makeText(applicationContext, "Token has expired.", Toast.LENGTH_SHORT).show()
            }
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        } else if ( (intentDeviceList != null && intentDeviceList.isEmpty()) || (intentDeviceList == null)) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        deviceList = intentDeviceList

        searchAdapter.updateDevices(deviceList)
        Log.d("MainActivity", "Device list size: ${deviceList.size}")

        searchBar?.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel any pending searches to avoid rapid consecutive updates
                searchHandler.removeCallbacksAndMessages(null)
                // Add a small delay before filtering
                searchHandler.postDelayed({
                    val searchText = s.toString().lowercase()

                    // Create a safe copy of the current device list
                    val currentDevices = ArrayList<Device>(deviceList)

                    // Filter the list
                    val filteredList = ArrayList<Device>(currentDevices.filter {
                        it.displayName.lowercase().contains(searchText)
                    })

                    runOnUiThread {
                        searchAdapter.updateDevices(filteredList)
                    }
                }, 300)
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LAPSFieldTool_V2Theme {
        Greeting("Android")
    }
}