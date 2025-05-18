package com.example.firebaseapp.models

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val admin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val fcmToken: String = ""
)
