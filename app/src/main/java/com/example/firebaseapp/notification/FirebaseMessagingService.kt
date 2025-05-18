package com.example.firebaseapp.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.firebaseapp.MainActivity
import com.example.firebaseapp.R
import com.example.firebaseapp.notification.NotificationHelper
import com.example.firebaseapp.utils.Constants
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FirebaseMessagingService : FirebaseMessagingService() {    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FCM", "####################")
        Log.d("FCM", "Mensaje recibido - ID: ${remoteMessage.messageId}")
        Log.d("FCM", "De: ${remoteMessage.from}")
        Log.d("FCM", "####################")
        
        // Registro detallado para depuración
        remoteMessage.notification?.let {
            Log.d("FCM_DEBUG", "NOTIFICACIÓN RECIBIDA:")
            Log.d("FCM_DEBUG", "- Título: ${it.title}")
            Log.d("FCM_DEBUG", "- Cuerpo: ${it.body}")
            Log.d("FCM_DEBUG", "- Canal Android: ${it.channelId}")
            Log.d("FCM_DEBUG", "- Icono: ${it.icon}")
        }
        
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM_DEBUG", "DATOS RECIBIDOS:")
            remoteMessage.data.forEach { (key, value) ->
                Log.d("FCM_DEBUG", "- $key: $value")
            }
        }
        
        // IMPORTANTE: Priorizar datos sobre la notificación para tener más control
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Nueva notificación"
            val message = remoteMessage.data["message"] ?: "Tienes una nueva notificación"
            val source = remoteMessage.data["source"] ?: "unknown"
            
            Log.d("FCM_DEBUG", "Mostrando notificación a partir de datos: $title - $message (fuente: $source)")
            val notificationHelper = NotificationHelper(this)
            notificationHelper.showNotification(title, message)
        }
        // Si no hay datos pero sí hay notificación, procesar la notificación directamente
        else if (remoteMessage.notification != null) {
            Log.d("FCM_DEBUG", "Mostrando notificación del objeto notification")
            val notificationHelper = NotificationHelper(this)
            notificationHelper.showNotification(
                remoteMessage.notification?.title ?: "Nueva notificación",
                remoteMessage.notification?.body ?: "Tienes una nueva notificación"
            )
        }
    }override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        Log.d("FCM_TOKEN_DEBUG", "Nuevo token generado: $token")
        
        // Suscribirse al tema 'all' con el nuevo token
        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnSuccessListener {
                Log.d("FCM_TOKEN_DEBUG", "Suscripción al tema 'all' actualizada con el nuevo token")
            }
            .addOnFailureListener { e ->
                Log.e("FCM_TOKEN_DEBUG", "Error al suscribir al tema 'all'", e)
            }
        
        // Si el usuario está autenticado, actualizar su token FCM en Firestore
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            val firestore = FirebaseFirestore.getInstance()
            
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d("FCM", "Token actualizado correctamente para el usuario: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("FCM", "Error al actualizar token para el usuario: $userId", e)
                }
        } else {
            // Guardar el token localmente para usarlo cuando el usuario inicie sesión
            val sharedPrefs = getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit().putString("fcm_token", token).apply()
            Log.d("FCM", "Token guardado localmente para uso futuro")
        }
    }    // El método sendNotification ha sido reemplazado por NotificationHelper
}
