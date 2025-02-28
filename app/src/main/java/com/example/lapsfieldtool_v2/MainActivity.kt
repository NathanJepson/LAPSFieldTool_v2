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

//        recyclerView?.apply {
//            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this@MainActivity)
//            adapter = searchAdapter
//        }
        /*
        tokenManager = TokenManager(application)
        */

        tokenManager = TokenManager.getInstance(applicationContext)

        // Example condition to check if array is empty or bearer token is expired

        val intentDeviceList: ArrayList<Device>? = intent.getParcelableArrayListExtra<Device>("device_list", Device::class.java)

        var tokenExpired = false

        if (tokenManager != null) {
            lifecycleScope.launch {
                tokenExpired = tokenManager!!.checkTokenExpired()
                if (tokenExpired) {
                    tokenManager!!.clearToken()
                }
            }
        }

//        if (true) {
//            deviceList.add(Device("123","Test1"))
//            deviceList.add(Device("4","YY"))
//            deviceList.add(Device("5","UU"))
//            deviceList.add(Device("6","Test2"))
//            deviceList.add(Device("7","Test3"))
//            deviceList.add(Device("8","Test4"))
//            deviceList.add(Device("9","Bet"))
//            deviceList.add(Device("10","Yuuuup"))
//            deviceList.add(Device("11","Noooooo"))
//            deviceList.add(Device("12","Algore"))
//            deviceList.add(Device("13","Gump"))
//            deviceList.add(Device("14","Forest"))
//            deviceList.add(Device("15","Jim"))
//            deviceList.add(Device("16","Nate"))
//            deviceList.add(Device("17","Device30895"))
//            deviceList.add(Device("18","RRRRRR"))
//            deviceList.add(Device("19","SSSSSS"))
//            deviceList.add(Device("20","TTTTTT"))
//            deviceList.add(Device("21","HHHHHHH"))
//            deviceList.add(Device("22","aaaaaaaaaaaaaa"))
//            deviceList.add(Device("23","bbbbbbbbbbbbb"))
//            deviceList.add(Device("24","cccccccccccc"))
//            deviceList.add(Device("25","ddddddddddddd"))
//            deviceList.add(Device("26","eeeeeeeeeeeee"))
//            deviceList.add(Device("27","ffffffffffff"))
//            deviceList.add(Device("28","gggggggggggg"))
//            deviceList.add(Device("29","hhhhhhhhhhhh"))
//            deviceList.add(Device("30","iiiiiiiiiiiii"))
//            deviceList.add(Device("31","e6"))
//            deviceList.add(Device("32","e7"))
//            deviceList.add(Device("33","e8"))
//            deviceList.add(Device("34","e9"))
//            deviceList.add(Device("35","e10"))
//            deviceList.add(Device("36","e11"))
//            deviceList.add(Device("37","e12"))
//            deviceList.add(Device("38","e13"))
//            deviceList.add(Device("39","e14"))
//            deviceList.add(Device("40","e15"))
//            deviceList.add(Device("41","e16"))
//            deviceList.add(Device("42","e17"))
//            deviceList.add(Device("43","e18"))
//            deviceList.add(Device("44","e19"))
//            deviceList.add(Device("45","e20"))
//            deviceList.add(Device("46","e21"))
//            deviceList.add(Device("47","e22"))
//            deviceList.add(Device("48","e23"))
//
//            searchAdapter.updateDevices(deviceList)
//            Log.d("MainActivity", "Device list size: ${deviceList.size}")
//        }

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