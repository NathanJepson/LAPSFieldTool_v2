package com.example.lapsfieldtool_v2.ui

import android.util.Log
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lapsfieldtool_v2.R
import com.example.lapsfieldtool_v2.data.model.Device

class SearchAdapter : RecyclerView.Adapter<SearchViewHolder>() {

    private var deviceList: ArrayList<Device> = ArrayList()

     fun updateDevices(newDevices: ArrayList<Device>) {
         // Clear and add all in one atomic operation
         this.deviceList.clear()
         this.deviceList.addAll(newDevices)

         // Notify the adapter that all data has changed
         notifyDataSetChanged()
         Log.d("SearchAdapter", "Received device list with size: ${newDevices.size}")
     }

     override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
         val view = android.view.LayoutInflater.from(parent.context)
             .inflate(R.layout.item_device, parent, false)
         Log.d("SearchAdapter", "Creating view holder")
         return SearchViewHolder(view)
     }

     override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
         holder.bind(deviceList[position])
         Log.d("SearchAdapter", "Binding position $position: ${deviceList[position].displayName}")
     }

     override fun getItemCount(): Int = deviceList.size

 }