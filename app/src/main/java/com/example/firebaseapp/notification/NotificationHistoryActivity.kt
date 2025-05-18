package com.example.firebaseapp.notification

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.Toast
import android.util.Log
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
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot

class NotificationHistoryActivity : AppCompatActivity() {    private lateinit var auth: FirebaseAuth
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
        
        val currentUserId = auth.currentUser?.uid
        
        // Debug info - mostrar ID de usuario para verificar
        if (currentUserId != null) {
            Log.d("NotificationHistory", "Usuario actual: $currentUserId")
        } else {
            Log.e("NotificationHistory", "Error: No hay usuario autenticado")
            Toast.makeText(this, "Error: No se detectó un usuario autenticado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Inicializar vistas
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        
        // Configurar el botón de refresh
        findViewById<ImageButton>(R.id.refreshButton).setOnClickListener {
            refreshNotifications()
        }        // Configurar RecyclerView
        notificationsAdapter = NotificationsAdapter(notificationsList)
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationsRecyclerView.adapter = notificationsAdapter
          // Configurar Firestore para deshabilitar la caché de forma segura
        try {
            // Nota: setPersistenceEnabled se usa aquí aunque está marcado como deprecated
            // porque todavía es la manera recomendada para deshabilitar el almacenamiento local
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(false) // Desactivar persistencia para evitar uso de caché
                .build()
            firestore.firestoreSettings = settings
            
            Log.d("NotificationHistory", "Persistencia de caché desactivada")
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Error al configurar Firestore settings: ${e.message}", e)
        }
        
        // Cargar notificaciones directamente del servidor
        loadNotifications()
    }
    private fun loadNotifications() {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("NotificationHistory", "No hay usuario autenticado al cargar notificaciones")
                Toast.makeText(this, "Error: No se pudo identificar al usuario", Toast.LENGTH_SHORT).show()
                return            }
            
            Log.d("NotificationHistory", "Cargando notificaciones para usuario: $userId")
            // Usar get() con Source.SERVER para forzar la recuperación desde el servidor
            firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("receiverUid", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get(Source.SERVER) // Forzar obtención desde servidor
                .addOnSuccessListener { snapshot ->
                    notificationsList.clear()
                    if (!snapshot.isEmpty) {
                        for (document in snapshot.documents) {
                            val notification = document.toObject(Notification::class.java)
                            if (notification != null) {
                                notificationsList.add(notification)
                            }
                        }
                        notificationsAdapter.notifyDataSetChanged()
                        if (notificationsList.isEmpty()) {
                            Toast.makeText(this, "No hay notificaciones para mostrar", Toast.LENGTH_SHORT).show()
                        }
                    }
                }                .addOnFailureListener { error ->
                    val errorMessage = error.message ?: "Error desconocido"
                    Log.e("NotificationHistory", "Error al cargar notificaciones: $errorMessage", error)
                    
                    if (errorMessage.contains("FAILED_PRECONDITION") && errorMessage.contains("index")) {
                        // Error específico por índice en creación
                        Toast.makeText(this, "El sistema está preparando los datos. Por favor, inténtalo de nuevo en unos minutos.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error al cargar notificaciones: $errorMessage", Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Excepción al cargar notificaciones: ${e.message}", e)
            Toast.makeText(this, "Error inesperado: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun refreshNotifications() {
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
        Toast.makeText(this, "Actualizando notificaciones desde servidor...", Toast.LENGTH_SHORT).show()
        Log.d("NotificationHistory", "Iniciando actualización forzada desde servidor para usuario: $userId")
        
        try {
            // Limpiar la lista actual
            notificationsList.clear()
            notificationsAdapter.notifyDataSetChanged()
            
            // Forzar la recarga desde el servidor
            loadNotificationsForRefresh { success ->
                // Detener la animación
                rotation.cancel()
                
                // Habilitar el botón nuevamente
                refreshButton.isEnabled = true
                refreshButton.rotation = 0f
                
                if (success) {
                    Log.d("NotificationHistory", "Actualización exitosa. Notificaciones cargadas: ${notificationsList.size}")
                    Toast.makeText(this, "Notificaciones actualizadas (${notificationsList.size})", Toast.LENGTH_SHORT).show()
                    
                    // Mostrar IDs para debugging
                    if (notificationsList.isNotEmpty()) {
                        notificationsList.take(3).forEach { notification ->
                            Log.d("NotificationHistory", "Notificación ID: ${notification.id}, de: ${notification.senderName}, timestamp: ${notification.timestamp}")
                        }
                    }
                } else {
                    Log.w("NotificationHistory", "No se encontraron notificaciones para el usuario: $userId")
                    Toast.makeText(this, "No se encontraron notificaciones", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // Detener la animación en caso de error
            rotation.cancel()
            refreshButton.isEnabled = true
            refreshButton.rotation = 0f
            
            Log.e("NotificationHistory", "Error en actualización: ${e.message}", e)            
            Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()        }
    }
    
    private fun loadNotificationsForRefresh(callback: (Boolean) -> Unit) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e("NotificationHistory", "No hay usuario autenticado al actualizar notificaciones")
                callback(false)
                return
            }
            
            Log.d("NotificationHistory", "Actualizando notificaciones para usuario: $userId")
            // Usar get() con Source.SERVER para forzar la actualización desde el servidor
            firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                .whereEqualTo("receiverUid", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100) // Limitar para optimizar la consulta
                .get(Source.SERVER) // Forzar obtención desde servidor
                .addOnSuccessListener { snapshot ->
                    notificationsList.clear()
                    
                    if (!snapshot.isEmpty) {
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
                }                .addOnFailureListener { error ->
                    val errorMessage = error.message ?: "Error desconocido"
                    Log.e("NotificationHistory", "Error al actualizar notificaciones: $errorMessage", error)
                    
                    if (errorMessage.contains("FAILED_PRECONDITION") && errorMessage.contains("index")) {
                        // Error específico por índice en creación
                        Toast.makeText(this, "El sistema está preparando los datos. Por favor, inténtalo de nuevo en unos minutos.", Toast.LENGTH_LONG).show()
                    }
                    callback(false)
                }
        } catch (e: Exception) {
            Log.e("NotificationHistory", "Excepción al actualizar notificaciones: ${e.message}", e)
            callback(false)
        }
    }
}

