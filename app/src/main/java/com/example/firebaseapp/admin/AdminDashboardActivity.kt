package com.example.firebaseapp.admin

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseapp.R
import com.example.firebaseapp.adapters.UsersAdapter
import com.example.firebaseapp.auth.LoginActivity
import com.example.firebaseapp.models.User
import com.example.firebaseapp.notification.NotificationHistoryActivity
import com.example.firebaseapp.user.UserProfileActivity
import com.example.firebaseapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var sendNotificationButton: Button
    
    private val usersList = mutableListOf<User>()
    private lateinit var usersAdapter: UsersAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)
        
        title = "Panel de Administrador"
        
        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Inicializar vistas
        usersRecyclerView = findViewById(R.id.usersRecyclerView)
        sendNotificationButton = findViewById(R.id.sendNotificationButton)
        
        // Configurar RecyclerView
        usersAdapter = UsersAdapter(usersList)
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        usersRecyclerView.adapter = usersAdapter
        
        // Cargar lista de usuarios
        loadUsers()
        
        // Configurar botón para enviar notificaciones
        sendNotificationButton.setOnClickListener {
            startActivity(Intent(this, SendNotificationActivity::class.java))
        }
    }
    
    private fun loadUsers() {
        firestore.collection(Constants.COLLECTION_USERS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                
                usersList.clear()
                if (snapshot != null) {
                    for (document in snapshot.documents) {
                        val user = document.toObject(User::class.java)
                        if (user != null) {
                            usersList.add(user)
                        }
                    }
                    usersAdapter.notifyDataSetChanged()
                }
            }
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.admin_menu, menu)
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
            R.id.action_send_notification -> {
                startActivity(Intent(this, SendNotificationActivity::class.java))
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
}
