package com.example.firebaseapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.firebaseapp.MainActivity
import com.example.firebaseapp.R
import com.example.firebaseapp.utils.Constants
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.RemoteMessage

/**
 * Helper para manejar notificaciones push en la aplicación.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Muestra una notificación push.
     */    fun showNotification(title: String, message: String, notificationId: Int = System.currentTimeMillis().toInt()) {
        Log.d("NotificationHelper", "Mostrando notificación: $title - $message (ID: $notificationId)")
        
        try {
            // Intent para abrir la app cuando se toca la notificación
            val intent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("notification_title", title)
                putExtra("notification_message", message)
            }
            
            // Asegurar el uso de FLAG_IMMUTABLE para seguridad
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            
            val pendingIntent = PendingIntent.getActivity(
                context, 
                notificationId, 
                intent,
                flags
            )
            
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            
            val notificationBuilder = NotificationCompat.Builder(context, Constants.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .setLights(0xFF0000FF.toInt(), 1000, 300)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Para mensajes largos
            
            // Verificar que el channel está creado antes de notificar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = notificationManager.getNotificationChannel(Constants.CHANNEL_ID)
                if (channel == null) {
                    Log.w("NotificationHelper", "Canal de notificación no encontrado. Creando...")
                    createNotificationChannel()
                }
            }
            
            Log.d("NotificationHelper", "Enviando notificación al sistema...")
            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d("NotificationHelper", "Notificación enviada correctamente")
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error al mostrar notificación", e)
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
                enableLights(true)
                lightColor = android.graphics.Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationHelper", "Canal de notificación creado: ${Constants.CHANNEL_ID}")
        }
    }
      /**
     * Procesa un mensaje remoto de FCM y muestra la notificación correspondiente.
     * 
     * Nota: Este método es mantenido por compatibilidad, pero preferiblemente la lógica
     * debería estar en el servicio FCM para mejor control.
     */
    fun handleRemoteMessage(remoteMessage: RemoteMessage) {
        Log.d("NotificationHelper", "Procesando mensaje FCM: ${remoteMessage.messageId}")
        
        // Generar ID único para la notificación
        val notificationId = System.currentTimeMillis().toInt()
        
        try {
            // Dar prioridad a los datos sobre la notificación para un mejor control
            if (remoteMessage.data.isNotEmpty()) {
                val title = remoteMessage.data["title"] ?: "Nueva notificación"
                val message = remoteMessage.data["message"] ?: "Has recibido una nueva notificación"
                
                Log.d("NotificationHelper", "Mostrando notificación desde datos: $title - $message")
                showNotification(title, message, notificationId)
            }
            // Solo si no hay datos, procesar la notificación directa
            else if (remoteMessage.notification != null) {
                val title = remoteMessage.notification?.title ?: "Nueva notificación"
                val body = remoteMessage.notification?.body ?: "Has recibido una nueva notificación"
                
                Log.d("NotificationHelper", "Mostrando notificación desde objeto notification: $title - $body")
                showNotification(title, body, notificationId)
            }
            else {
                Log.w("NotificationHelper", "Mensaje FCM sin datos ni notificación")
            }
        } catch (e: Exception) {
            Log.e("NotificationHelper", "Error al procesar mensaje FCM", e)
        }
    }
    
    /**
     * Suscribe al usuario actual a todos los temas de notificación necesarios.
     */
    fun subscribeToTopics() {
        // Tema general para todos los usuarios
        FirebaseMessaging.getInstance().subscribeToTopic("all")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("NotificationHelper", "Suscrito exitosamente al tema 'all'")
                } else {
                    Log.e("NotificationHelper", "Error al suscribirse al tema 'all'", task.exception)
                }
            }
    }
    
    /**
     * Cancela todas las notificaciones mostradas actualmente.
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}
