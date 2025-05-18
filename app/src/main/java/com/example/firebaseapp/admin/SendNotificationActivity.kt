package com.example.firebaseapp.admin

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseapp.R
import com.example.firebaseapp.adapters.UserSelectionAdapter
import com.example.firebaseapp.models.Notification
import com.example.firebaseapp.models.User
import com.example.firebaseapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions

class SendNotificationActivity : AppCompatActivity() {
    
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var functions: FirebaseFunctions
    
    private lateinit var titleEditText: EditText
    private lateinit var messageEditText: EditText
    private lateinit var sendTypeRadioGroup: RadioGroup
    private lateinit var specificUserRadio: RadioButton
    private lateinit var allUsersRadio: RadioButton
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var sendButton: Button
    
    private val usersList = mutableListOf<User>()
    private lateinit var userSelectionAdapter: UserSelectionAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_notification)
          title = "Enviar Notificaciones"        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        functions = FirebaseFunctions.getInstance("us-central1") // Especificar la región explícitamente
        
        // Inicializar vistas
        titleEditText = findViewById(R.id.titleEditText)
        messageEditText = findViewById(R.id.messageEditText)
        sendTypeRadioGroup = findViewById(R.id.sendTypeRadioGroup)
        specificUserRadio = findViewById(R.id.specificUserRadio)
        allUsersRadio = findViewById(R.id.allUsersRadio)
        usersRecyclerView = findViewById(R.id.usersRecyclerView)
        sendButton = findViewById(R.id.sendButton)
        
        // Configurar RecyclerView
        userSelectionAdapter = UserSelectionAdapter(usersList)
        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        usersRecyclerView.adapter = userSelectionAdapter
        
        // Cargar usuarios
        loadUsers()
        
        // Escuchar cambios en el tipo de envío
        sendTypeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == R.id.specificUserRadio) {
                usersRecyclerView.visibility = View.VISIBLE
            } else {
                usersRecyclerView.visibility = View.GONE
            }
        }
        
        // Configurar botón de envío
        sendButton.setOnClickListener {
            val title = titleEditText.text.toString().trim()
            val message = messageEditText.text.toString().trim()
            
            if (title.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Verificar qué opción está seleccionada
            if (specificUserRadio.isChecked) {
                // Enviar a usuarios específicos
                val selectedUsers = userSelectionAdapter.getSelectedUsers()
                if (selectedUsers.isEmpty()) {
                    Toast.makeText(this, "Por favor, selecciona al menos un usuario", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                sendNotificationToSpecificUsers(title, message, selectedUsers)
            } else {
                // Enviar a todos los usuarios
                sendNotificationToAllUsers(title, message)
            }
        }
    }
    
    private fun loadUsers() {
        val currentUserId = auth.currentUser?.uid ?: return
        
        firestore.collection(Constants.COLLECTION_USERS)
            .whereNotEqualTo("uid", currentUserId) // Excluir al usuario actual
            .get()
            .addOnSuccessListener { documents ->
                usersList.clear()
                for (document in documents) {
                    val user = document.toObject(User::class.java)                    // Verificar si el usuario tiene un token FCM válido
                    if (!user.fcmToken.isNullOrBlank()) {
                        Log.d("FCM_TOKEN_CHECK", "Usuario ${user.name} tiene token: ${user.fcmToken}")
                        usersList.add(user)
                    } else {
                        Log.d("FCM_TOKEN_CHECK", "Usuario ${user.name} NO tiene token válido")
                    }
                }
                userSelectionAdapter.notifyDataSetChanged()
                
                if (usersList.isEmpty()) {
                    Toast.makeText(this@SendNotificationActivity, 
                        "No hay usuarios disponibles con tokens válidos", 
                        Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("FIRESTORE", "Error al cargar usuarios", e)
                Toast.makeText(this@SendNotificationActivity, 
                    "Error al cargar usuarios: ${e.message}", 
                    Toast.LENGTH_SHORT).show()
            }
    }
      private fun sendNotificationToSpecificUsers(title: String, message: String, users: List<User>) {
        sendButton.isEnabled = false
        
        val currentUser = auth.currentUser ?: return
        val senderName = "Administrador" // Podrías obtener el nombre real del usuario aquí
        
        // Contador para saber cuando se han completado todas las solicitudes
        var pendingRequests = users.size
        var successCount = 0
        var failureCount = 0
        
        // Para cada usuario seleccionado
        for (user in users) {
            if (user.fcmToken.isNullOrBlank()) {
                Log.e("FCM_SEND", "Token inválido para usuario: ${user.name}")
                pendingRequests--
                failureCount++
                continue
            }
            
            // Crear objeto de notificación para Firestore
            val notification = Notification(
                id = firestore.collection(Constants.COLLECTION_NOTIFICATIONS).document().id,
                title = title,
                message = message,
                senderUid = currentUser.uid,
                senderName = senderName,
                receiverUid = user.uid
            )
            
            // Guardar notificación en Firestore
            firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                .document(notification.id)
                .set(notification)
                .addOnSuccessListener {
                    Log.d("FIRESTORE", "Notificación guardada en Firestore con ID: ${notification.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("FIRESTORE", "Error al guardar notificación en Firestore", e)
                }
                
            // Datos para la Cloud Function
            val data = hashMapOf(
                "title" to title,
                "message" to message,
                "token" to user.fcmToken
            )            // Log the token we're sending to
            Log.d("FCM_SEND", "Enviando notificación a token: ${user.fcmToken}")
            
            // Añadir más logs para diagnóstico
            Log.d("FCM_SEND", "Datos para la función: $data")
            Log.d("FCM_SEND", "Function instance: $functions")
            
            // Llamar a Cloud Function para enviar la notificación push
            try {
                Log.d("FCM_SEND", "Intentando llamar a la función sendNotification en la región us-central1")
                  // Correct use of Firebase Functions call
                val callable = functions.getHttpsCallable("sendNotification")
                // callable.setTimeout(60000) // Aumentar el timeout para debugging
                
                callable.call(data).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        pendingRequests--
                        successCount++
                        Log.d("FCM_SEND", "Notificación enviada con éxito a ${user.name}: ${task.result?.data}")
                        
                        // Cuando se completen todas las solicitudes, mostrar resumen y finalizar
                        if (pendingRequests <= 0) {
                            showCompletionMessage(successCount, failureCount)
                        }
                    } else {
                        pendingRequests--
                        failureCount++
                        Log.e("FCM_SEND", "Error al enviar notificación a ${user.name}: ${task.exception?.message}", task.exception)
                        
                        // Cuando se completen todas las solicitudes, mostrar resumen y finalizar
                        if (pendingRequests <= 0) {
                            showCompletionMessage(successCount, failureCount)
                        }
                    }
                }
            } catch (e: Exception) {
                pendingRequests--
                failureCount++
                Log.e("FCM_SEND", "Excepción crítica al llamar a la función en la nube: ${e.message}", e)
                
                if (pendingRequests <= 0) {
                    showCompletionMessage(successCount, failureCount)
                }
            }
        }
        
        // Si no hay usuarios seleccionados, habilitar el botón de nuevo
        if (users.isEmpty()) {
            sendButton.isEnabled = true
            Toast.makeText(this, "No hay usuarios seleccionados", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun showCompletionMessage(successCount: Int, failureCount: Int) {
        sendButton.isEnabled = true
        val message = when {
            failureCount == 0 -> "Notificaciones enviadas correctamente"
            successCount == 0 -> "Error al enviar las notificaciones"
            else -> "Enviadas: $successCount, Fallidas: $failureCount"
        }
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        if (successCount > 0) {
            finish()
        }    }
    
    private fun sendNotificationToAllUsers(title: String, message: String) {
        sendButton.isEnabled = false
        
        val currentUser = auth.currentUser ?: return
        val senderName = "Administrador" // Podrías obtener el nombre real del usuario
        
        // Datos para la Cloud Function
        val data = hashMapOf(
            "title" to title,
            "message" to message,
            "topic" to "all" // Enviar a todos los suscritos al tema 'all'
        )
        
        // Llamar a Cloud Function para enviar la notificación push
        try {
            // Añadir más logs para diagnóstico
            Log.d("FCM_SEND", "Datos para la función de tema: $data")
            Log.d("FCM_SEND", "Function instance para tema: $functions")
            
            Log.d("FCM_SEND", "Intentando llamar a la función sendNotificationToTopic en la región us-central1")
            
            // Correct use of Firebase Functions call for topic notification
            val callable = functions.getHttpsCallable("sendNotificationToTopic")
            // callable.setTimeout(60000) // Aumentar el timeout para debugging
            
            callable.call(data).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM_SEND", "Notificación a tema enviada con éxito: ${task.result?.data}")
                } else {
                    Log.e("FCM_SEND", "Error al enviar notificación a tema: ${task.exception?.message}", task.exception)
                }
            }
            
            // Despite the result, continue with local notifications
            Log.d("FCM_SEND", "Notificación a tema procesada")
            
            // Guardar notificación para cada usuario
            firestore.collection(Constants.COLLECTION_USERS)
                .get()
                .addOnSuccessListener { users -> 
                    for (user in users) {
                        val userId = user.id
                        if (userId != currentUser.uid) { // No enviar a sí mismo
                            val notification = Notification(
                                id = firestore.collection(Constants.COLLECTION_NOTIFICATIONS).document().id,
                                title = title,
                                message = message,
                                senderUid = currentUser.uid,
                                senderName = senderName,
                                receiverUid = userId
                            )
                            
                            firestore.collection(Constants.COLLECTION_NOTIFICATIONS)
                                .document(notification.id)
                                .set(notification)
                        }
                    }
                    
                    Toast.makeText(this, "Notificación enviada a todos los usuarios", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("FIRESTORE", "Error al obtener usuarios para notificaciones", e)
                    Toast.makeText(this, "Notificación enviada pero hubo un error al guardarla", Toast.LENGTH_SHORT).show()
                    sendButton.isEnabled = true
                }
                
        } catch (e: Exception) {
            Log.e("FCM_SEND", "Excepción al llamar a la función en la nube: ${e.message}", e)
            Toast.makeText(this, "Error crítico: ${e.message}", Toast.LENGTH_SHORT).show()
            sendButton.isEnabled = true
        }
    }
  
}
