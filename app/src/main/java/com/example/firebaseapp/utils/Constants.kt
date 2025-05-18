package com.example.firebaseapp.utils

// Constantes para ser utilizadas en toda la aplicación
object Constants {
    // Contraseña maestra para creación de cuentas de administrador
    const val ADMIN_MASTER_PASSWORD = "admin123"
    
    // Colecciones de Firestore
    const val COLLECTION_USERS = "users"
    const val COLLECTION_NOTIFICATIONS = "notifications"
    
    // Roles de usuario
    const val ROLE_ADMIN = "admin"
    const val ROLE_USER = "user"
    
    // Claves para SharedPreferences
    const val PREF_USER_SESSION = "user_session"
    const val KEY_USER_ID = "user_id"
    const val KEY_USER_EMAIL = "user_email"
    const val KEY_USER_NAME = "user_name"
    const val KEY_USER_ROLE = "user_role"
    
    // Canales de notificación
    const val CHANNEL_ID = "firebaseapp_channel"
    const val CHANNEL_NAME = "Firebase App Notifications"
    const val CHANNEL_DESCRIPTION = "Receive notifications from Firebase App"
}
