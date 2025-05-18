package com.example.firebaseapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.firebaseapp.notification.NotificationHelper
import com.example.firebaseapp.utils.Constants
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class FirebaseApplication : Application() {    override fun onCreate() {
        super.onCreate()
        
        // Inicializar Firebase
        FirebaseApp.initializeApp(this)
        
        // Crear canal de notificación para Android 8.0+
        createNotificationChannel()
        
        // Usar el helper para suscribirse a los temas necesarios
        val notificationHelper = NotificationHelper(this)
        notificationHelper.subscribeToTopics()
        
        // Registrar analíticas de mensajería y asegurar inicialización automática
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        
        // Obtener y verificar FCM token
        getAndVerifyFcmToken()
    }
    
    private fun getAndVerifyFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d("FCM_TOKEN_DEBUG", "FCM Token: $token")
                    
                    // Guardar en SharedPreferences para fácil acceso y debug
                    val prefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
                    prefs.edit().putString("fcm_token", token).apply()
                    
                    // Actualizar en Firestore si el usuario está autenticado
                    updateFirestoreToken(token)
                } else {
                    Log.e("FCM_TOKEN_DEBUG", "Error obteniendo FCM token", task.exception)
                }
            }
    }
    
    private fun updateFirestoreToken(token: String) {
        val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        
        if (currentUser != null) {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUser.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM_TOKEN_DEBUG", "Token actualizado en Firestore para usuario ${currentUser.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM_TOKEN_DEBUG", "Error al actualizar token en Firestore", e)
                }
        } else {
            Log.d("FCM_TOKEN_DEBUG", "Usuario no autenticado, no se puede actualizar token en Firestore")
        }
    }
      private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(
                Constants.CHANNEL_ID, 
                Constants.CHANNEL_NAME, 
                importance
            ).apply {
                description = Constants.CHANNEL_DESCRIPTION
                // Habilitar luces
                enableLights(true)
                lightColor = android.graphics.Color.RED
                
                // Habilitar vibración
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                  // Mostrar en la pantalla de bloqueo
                lockscreenVisibility = 1 // VISIBILITY_PUBLIC = 1
                
                // Permitir que la notificación tenga sonido
                setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION), null)
                
                // Mostrar insignia en el launcher
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
