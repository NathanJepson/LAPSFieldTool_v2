package com.example.lapsfieldtool_v2.ui.computer

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.lapsfieldtool_v2.R
import com.example.lapsfieldtool_v2.data.model.DeviceCredentials
import java.text.SimpleDateFormat
import java.util.Locale

class CredentialsAdapter : RecyclerView.Adapter<CredentialsAdapter.CredentialViewHolder>() {

    private var credentials: List<DeviceCredentials.Credential> = emptyList()

    fun updateCredentials(newCredentials: List<DeviceCredentials.Credential>) {
        credentials = newCredentials
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CredentialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_credential, parent, false)
        return CredentialViewHolder(view)
    }

    override fun onBindViewHolder(holder: CredentialViewHolder, position: Int) {
        Log.d("CredentialsAdapter", "Successfully binding credentials to recycler view.")
        holder.bind(credentials[position])
    }

    override fun getItemCount(): Int = credentials.size

    class CredentialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val accountNameTextView: TextView = itemView.findViewById(R.id.accountNameTextView)
        private val accountSidTextView: TextView = itemView.findViewById(R.id.accountSidTextView)
        private val backupDateTextView: TextView = itemView.findViewById(R.id.backupDateTextView)
        private val base64PasswordTextView: TextView = itemView.findViewById(R.id.base64PasswordTextView)
        private val decodedPasswordTextView: TextView = itemView.findViewById(R.id.decodedPasswordTextView)

        fun bind(credential: DeviceCredentials.Credential) {
            accountNameTextView.text = "Account: ${credential.accountName}"
            accountSidTextView.text = "SID: ${credential.accountSid}"

            // Format date for better readability
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS'Z'", Locale.US)
            val outputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val formattedDate = try {
                val date = inputFormat.parse(credential.backupDateTime)
                outputFormat.format(date!!)
            } catch (e: Exception) {
                credential.backupDateTime
            }

            backupDateTextView.text = "Backup Date: $formattedDate"
            base64PasswordTextView.text = "Base64 Password: ${credential.passwordBase64}"
            decodedPasswordTextView.text = "Password: ${credential.decodedPassword}"
        }
    }
}