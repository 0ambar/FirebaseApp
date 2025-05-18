package com.example.firebaseapp.user

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaseapp.R
import com.example.firebaseapp.models.User
import com.example.firebaseapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UserProfileActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var nameEditText: EditText
    private lateinit var emailTextView: TextView
    private lateinit var roleTextView: TextView
    private lateinit var saveButton: Button
    
    private var currentUser: User? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)
        
        title = "Mi Perfil"
        
        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Inicializar vistas
        nameEditText = findViewById(R.id.nameEditText)
        emailTextView = findViewById(R.id.emailTextView)
        roleTextView = findViewById(R.id.roleTextView)
        saveButton = findViewById(R.id.saveButton)
        
        // Cargar datos del usuario
        loadUserProfile()
        
        // Configurar botón de guardar
        saveButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            
            if (name.isEmpty()) {
                Toast.makeText(this, "Por favor, ingresa tu nombre", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            updateUserProfile(name)
        }
    }
    
    private fun loadUserProfile() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    currentUser = document.toObject(User::class.java)
                    currentUser?.let { user ->
                        nameEditText.setText(user.name)
                        emailTextView.text = user.email
                        roleTextView.text = if (user.admin) "Administrador" else "Usuario Normal"
                    }
                }
            }
    }
    
    private fun updateUserProfile(name: String) {
        val userId = auth.currentUser?.uid ?: return
        
        // Mostrar progreso
        saveButton.isEnabled = false
        
        // Actualizar solo el nombre del usuario
        firestore.collection(Constants.COLLECTION_USERS)
            .document(userId)
            .update("name", name)
            .addOnSuccessListener {
                Toast.makeText(this, "Perfil actualizado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
                saveButton.isEnabled = true
            }
    }
}
