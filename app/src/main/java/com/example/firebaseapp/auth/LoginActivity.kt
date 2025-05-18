package com.example.firebaseapp.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaseapp.MainActivity
import com.example.firebaseapp.R
import android.util.Log
import com.example.firebaseapp.admin.AdminDashboardActivity
import com.example.firebaseapp.models.User
import com.example.firebaseapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.tasks.OnCompleteListener
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

class LoginActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "LoginActivity"
    }
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerTextView: TextView
      override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        
        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Inicializar vistas
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        loginButton = findViewById(R.id.loginButton)
        registerTextView = findViewById(R.id.registerTextView)
          // DEPURACIÓN: Verificar estado de administrador del usuario actual (si hay sesión activa)
        if (auth.currentUser != null) {
            CheckAdminStatus.checkCurrentUserAdminStatus()
        }
        
        // Obtener y mostrar el token FCM para pruebas
        getFCMTokenForTesting()
        
        // Configurar botón de inicio de sesión
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            loginUser(email, password)
        }
        
        // Configurar enlace a registro
        registerTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }
    
    private fun loginUser(email: String, password: String) {
        // Mostrar progreso
        loginButton.isEnabled = false
        
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Inicio de sesión exitoso
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    
                    // Actualizar token FCM
                    updateFCMToken(userId)
                    
                    // Obtener información del usuario
                    firestore.collection(Constants.COLLECTION_USERS)
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val user = document.toObject(User::class.java)
                                
                                // Determinar a qué pantalla dirigir al usuario
                                if (user?.admin == true) {
                                    // Usuario administrador
                                    startActivity(Intent(this, AdminDashboardActivity::class.java))
                                } else {
                                    // Usuario normal
                                    startActivity(Intent(this, MainActivity::class.java))
                                }
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error al obtener datos: ${e.message}", Toast.LENGTH_SHORT).show()
                            loginButton.isEnabled = true
                        }
                } else {
                    // Error de inicio de sesión
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    loginButton.isEnabled = true
                }
            }
    }
      private fun updateFCMToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val userRef = firestore.collection(Constants.COLLECTION_USERS).document(userId)
            userRef.update("fcmToken", token)
        }
    }
    
    private fun getFCMTokenForTesting() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and show toast with token
            val msg = "FCM Token: $token"
            Log.d(TAG, msg)
            
            // Copiar al portapapeles para facilitar las pruebas
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("FCM Token", token)
            clipboard.setPrimaryClip(clip)
            
            Toast.makeText(baseContext, "Token copiado al portapapeles", Toast.LENGTH_SHORT).show()
        })
    }
    
    override fun onStart() {
        super.onStart()
        
        // Verificar si el usuario ya ha iniciado sesión
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid
            
            // Obtener información del usuario
            firestore.collection(Constants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val user = document.toObject(User::class.java)
                        
                        // Determinar a qué pantalla dirigir al usuario
                        if (user?.admin == true) {
                            // Usuario administrador
                            startActivity(Intent(this, AdminDashboardActivity::class.java))
                        } else {
                            // Usuario normal
                            startActivity(Intent(this, MainActivity::class.java))
                        }
                        finish()
                    }
                }
        }
    }
}
