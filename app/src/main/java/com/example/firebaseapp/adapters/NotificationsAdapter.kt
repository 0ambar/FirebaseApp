package com.example.firebaseapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseapp.R
import com.example.firebaseapp.models.Notification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotificationsAdapter(
    private val notifications: List<Notification>,
    private val onNotificationClick: ((Notification) -> Unit)? = null
) : RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.notificationTitleTextView)
        val messageTextView: TextView = itemView.findViewById(R.id.notificationMessageTextView)
        val senderTextView: TextView = itemView.findViewById(R.id.notificationSenderTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.notificationDateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        
        holder.titleTextView.text = notification.title
        holder.messageTextView.text = notification.message
        holder.senderTextView.text = "De: ${notification.senderName}"
        
        // Formatear la fecha
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val date = Date(notification.timestamp)
        holder.dateTextView.text = sdf.format(date)
        
        // Configurar click listener si se proporcionó
        if (onNotificationClick != null) {
            holder.itemView.setOnClickListener {
                onNotificationClick.invoke(notification)
            }
        }
    }

    override fun getItemCount(): Int = notifications.size
}
