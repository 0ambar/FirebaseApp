package com.example.firebaseapp.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Este receptor se inicia cuando el dispositivo se reinicia,
 * para asegurarse de que la suscripción a notificaciones se restablezca.
 */
class BootReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "Dispositivo reiniciado, restaurando suscripción a notificaciones")
            
            // Suscribir al tema 'all' para notificaciones globales
            FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d("BootReceiver", "Suscripción al tema 'all' restaurada")
                    } else {
                        Log.e("BootReceiver", "Error al suscribirse al tema 'all'", task.exception)
                    }
                }
            
            // Si el usuario ya había iniciado sesión, restaurar el token FCM
            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser != null) {
                val userId = currentUser.uid
                
                FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        Log.d("BootReceiver", "Token FCM obtenido: $token")
                        
                        // Aquí podríamos actualizar el token en Firestore si fuese necesario
                    } else {
                        Log.e("BootReceiver", "Error al obtener token FCM", task.exception)
                    }
                }
            }
        }
    }
}
