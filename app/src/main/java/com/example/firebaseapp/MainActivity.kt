package com.example.firebaseapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseapp.adapters.NotificationsAdapter
import com.example.firebaseapp.auth.LoginActivity
import com.example.firebaseapp.models.Notification
import com.example.firebaseapp.models.User
import com.example.firebaseapp.notification.NotificationHistoryActivity
import com.example.firebaseapp.user.UserProfileActivity
import com.example.firebaseapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MainActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var notificationsRecyclerView: RecyclerView
    private val notificationsList = mutableListOf<Notification>()
    private lateinit var notificationsAdapter: NotificationsAdapter
    
    private var currentUser: User? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        title = "Inicio"
        
        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Inicializar vistas
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        
        // Configurar RecyclerView
        notificationsAdapter = NotificationsAdapter(notificationsList)
        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
        notificationsRecyclerView.adapter = notificationsAdapter
        
        // Solicitar permisos de notificación
        requestNotificationPermissions()
        
        // Cargar datos del usuario actual
        loadCurrentUser()
        
        // Verificar si la app fue abierta desde una notificación
        handleNotificationIntent(intent)
        
        // Cargar notificaciones recientes
        loadRecentNotifications()
    }
    
    private fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    invalidateOptionsMenu() // Para actualizar el menú si es necesario
                }
            }
    }
    
    private fun loadRecentNotifications() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
            .whereEqualTo("receiverUid", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10) // Limitar a las 10 más recientes
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
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, UserProfileActivity::class.java))
                true
            }
            R.id.action_notifications -> {
                startActivity(Intent(this, NotificationHistoryActivity::class.java))
                true
            }
            R.id.action_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    override fun onStart() {
        super.onStart()
        
        // Verificar si hay un usuario autenticado
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        
        // Manejar el intent si la app ya está abierta cuando llega una notificación
        if (intent != null) {
            handleNotificationIntent(intent)
        }
    }
    
    private fun handleNotificationIntent(intent: Intent) {
        val notificationTitle = intent.getStringExtra("notification_title")
        val notificationMessage = intent.getStringExtra("notification_message")
        
        if (notificationTitle != null && notificationMessage != null) {
            // La app fue abierta desde una notificación
            Toast.makeText(
                this, 
                "Notificación: $notificationTitle", 
                Toast.LENGTH_SHORT
            ).show()
            
            // Aquí podríamos hacer cosas adicionales como navegar a una pantalla específica
            // o mostrar el detalle de la notificación
            
            // Refrescar la lista de notificaciones para mostrar la nueva
            loadRecentNotifications()
        }
    }
    
    // Registrar el launcher para el resultado del permiso
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permiso concedido
            Toast.makeText(this, "Notificaciones habilitadas", Toast.LENGTH_SHORT).show()
        } else {
            // Permiso denegado
            Toast.makeText(
                this,
                "Las notificaciones están desactivadas. No recibirás alertas de nuevos mensajes.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun requestNotificationPermissions() {
        // Para Android 13 (API 33) y superior, necesitamos solicitar POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tenemos el permiso
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explicar al usuario por qué necesitamos el permiso
                    Toast.makeText(
                        this,
                        "Necesitamos permiso para mostrar notificaciones y mantenerte informado",
                        Toast.LENGTH_LONG
                    ).show()
                    // Luego solicitar el permiso
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Solicitar el permiso directamente
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
}