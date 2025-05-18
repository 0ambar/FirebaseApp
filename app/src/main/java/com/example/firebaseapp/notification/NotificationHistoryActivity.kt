package com.example.firebaseapp.notification

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseapp.R
import com.example.firebaseapp.adapters.NotificationsAdapter
import com.example.firebaseapp.models.Notification
import com.example.firebaseapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class NotificationHistoryActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var notificationsRecyclerView: RecyclerView
    private val notificationsList = mutableListOf<Notification>()
    private lateinit var notificationsAdapter: NotificationsAdapter
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_history)
        
        title = "Historial de Notificaciones"
        
        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Inicializar vistas
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        
        // Configurar el botón de refresh
        findViewById<ImageButton>(R.id.refreshButton).setOnClickListener {
            refreshNotifications()
        }
        
        // Configurar RecyclerView
        notificationsAdapter = NotificationsAdapter(notificationsList)
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationsRecyclerView.adapter = notificationsAdapter
        
        // Cargar notificaciones
        loadNotifications()
    }
      private fun loadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo("receiverUid", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                notificationsList.clear()
                if (snapshot != null) {
                    for (document in snapshot.documents) {
                        val notification = document.toObject(Notification::class.java)
                        if (notification != null) {
                            notificationsList.add(notification)
                        }
                    }
                    notificationsAdapter.notifyDataSetChanged()
                }
            }
    }    private fun refreshNotifications() {
        val userId = auth.currentUser?.uid ?: return
        
        // Obtener referencia al botón de actualización
        val refreshButton = findViewById<ImageButton>(R.id.refreshButton)
        
        // Deshabilitar el botón durante la actualización
        refreshButton.isEnabled = false
        
        // Crear animación de rotación para el botón
        val rotation = ObjectAnimator.ofFloat(refreshButton, View.ROTATION, 0f, 360f).apply {
            duration = 1000
            repeatCount = ObjectAnimator.INFINITE
            interpolator = LinearInterpolator()
            start()
        }
        
        // Mostrar un mensaje indicando que se están actualizando las notificaciones
        Toast.makeText(this, "Actualizando notificaciones...", Toast.LENGTH_SHORT).show()
        
        try {
            // Solución alternativa: simular recreación de actividad para forzar la actualización
            // 1. Limpiar la lista actual
            notificationsList.clear()
            notificationsAdapter.notifyDataSetChanged()
            
            // 2. Configurar un nuevo listener temporal que se elimina después de la primera actualización
            loadNotificationsForRefresh { success ->
                // Detener la animación
                rotation.cancel()
                
                // Habilitar el botón nuevamente
                refreshButton.isEnabled = true
                refreshButton.rotation = 0f
                
                if (success) {
                    Toast.makeText(this, "Notificaciones actualizadas", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "No se encontraron nuevas notificaciones", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // Detener la animación en caso de error
            rotation.cancel()
            refreshButton.isEnabled = true
            refreshButton.rotation = 0f
            
            Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
      private fun loadNotificationsForRefresh(callback: (Boolean) -> Unit) {
        val userId = auth.currentUser?.uid ?: return callback(false)
        
        // Usar el mismo método que en loadNotifications pero con un callback
        // Creamos una referencia mutable para el listener
        var listenerReference: com.google.firebase.firestore.ListenerRegistration? = null
        
        listenerReference = firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo("receiverUid", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(100) // Limitar para optimizar la consulta
            .addSnapshotListener { snapshot, error ->
                // Eliminar el listener después de recibir la primera respuesta
                listenerReference?.remove()
                
                if (error != null) {
                    callback(false)
                    return@addSnapshotListener
                }
                
                notificationsList.clear()
                if (snapshot != null && !snapshot.isEmpty) {
                    for (document in snapshot.documents) {
                        val notification = document.toObject(Notification::class.java)
                        if (notification != null) {
                            notificationsList.add(notification)
                        }
                    }
                    notificationsAdapter.notifyDataSetChanged()
                    callback(true)
                } else {
                    callback(false)
                }
            }
    }
}

