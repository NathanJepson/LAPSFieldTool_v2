package com.example.lapsfieldtool_v2.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lapsfieldtool_v2.R
import com.example.lapsfieldtool_v2.data.model.Device
import com.example.lapsfieldtool_v2.ui.computer.ComputerActivity

class SearchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), View.OnClickListener {

    private var deviceName: TextView = itemView.findViewById(R.id.deviceName)
    private var deviceId: String = ""

    init {
        itemView.setOnClickListener(this)
    }

    fun bind(thisDevice: Device) {
        deviceName.text = thisDevice.displayName
        deviceId = thisDevice.deviceId
    }

    override fun onClick(v: View?) {
        //extras.putString("device_name", deviceName.text.toString())
        //extras.putString("device_id",deviceId)

        val intent = Intent(itemView.context, ComputerActivity::class.java).apply {
            putExtra("device_name", deviceName.text.toString())
            putExtra("device_id",deviceId)
        }

        v?.context?.startActivity(intent)
    }
}