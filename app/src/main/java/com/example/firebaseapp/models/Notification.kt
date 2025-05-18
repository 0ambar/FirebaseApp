package com.example.firebaseapp.models

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val receiverUid: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val read: Boolean = false
)
