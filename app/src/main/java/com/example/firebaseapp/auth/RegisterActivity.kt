package com.example.firebaseapp.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.firebaseapp.MainActivity
import com.example.firebaseapp.R
import com.example.firebaseapp.admin.AdminDashboardActivity
import com.example.firebaseapp.models.User
import com.example.firebaseapp.utils.Constants
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class RegisterActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var adminCheckBox: CheckBox
    private lateinit var adminPasswordLayout: TextInputLayout
    private lateinit var adminPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        
        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Inicializar vistas
        nameEditText = findViewById(R.id.nameEditText)
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        adminCheckBox = findViewById(R.id.adminCheckBox)
        adminPasswordLayout = findViewById(R.id.adminPasswordLayout)
        adminPasswordEditText = findViewById(R.id.adminPasswordEditText)
        registerButton = findViewById(R.id.registerButton)
        loginTextView = findViewById(R.id.loginTextView)
        
        // Configurar checkbox de administrador
        adminCheckBox.setOnCheckedChangeListener { _, isChecked ->
            adminPasswordLayout.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
        
        // Configurar botón de registro
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val isAdmin = adminCheckBox.isChecked
            val adminPassword = adminPasswordEditText.text.toString().trim()
            
            if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (password != confirmPassword) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (isAdmin && adminPassword != Constants.ADMIN_MASTER_PASSWORD) {
                Toast.makeText(this, "Contraseña de administrador incorrecta", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            registerUser(name, email, password, isAdmin)
        }
        
        // Configurar enlace a inicio de sesión
        loginTextView.setOnClickListener {
            finish()
        }
    }
    
    private fun registerUser(name: String, email: String, password: String, isAdmin: Boolean) {
        // Mostrar progreso
        registerButton.isEnabled = false
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    
                    // Obtener token FCM
                    FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                        // Crear objeto de usuario
                        val user = User(
                            uid = userId,
                            email = email,
                            name = name,
                            admin = isAdmin,
                            fcmToken = token
                        )
                        
                        // Guardar usuario en Firestore
                        firestore.collection(Constants.COLLECTION_USERS)
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                  // Redirigir a la pantalla principal correspondiente
                                val intent = if (isAdmin) {
                                    Intent(this, AdminDashboardActivity::class.java)
                                } else {
                                    Intent(this, MainActivity::class.java)
                                }
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                                registerButton.isEnabled = true
                            }
                    }
                } else {
                    // Error de registro
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    registerButton.isEnabled = true
                }
            }
    }
}
