package com.example.firebaseapp.auth

import android.util.Log
import com.example.firebaseapp.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Utilidad para verificar y corregir el estado de administrador de un usuario.
 * Esta clase se puede usar para depurar problemas con los permisos de administrador.
 */
class CheckAdminStatus {
    companion object {
        private const val TAG = "CheckAdminStatus"
        
        /**
         * Verifica si el usuario actual tiene privilegios de administrador y lo registra.
         * Esta función es útil para depuración.
         */
        fun checkCurrentUserAdminStatus() {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
            
            val currentUserId = auth.currentUser?.uid ?: return
            
            firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .get()
                .addOnSuccessListener { document ->                    if (document != null && document.exists()) {
                        val isAdmin = document.getBoolean("admin") ?: false
                        Log.d(TAG, "Usuario actual es administrador: $isAdmin")
                    } else {
                        Log.d(TAG, "No se encontró el documento del usuario")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al verificar estado de administrador", e)
                }
        }
        
        /**
         * Establece el estado de administrador para el usuario actual.
         * Útil para corregir permisos sin tener que volver a crear la cuenta.
         */
        fun setCurrentUserAsAdmin(isAdmin: Boolean, callback: (Boolean) -> Unit) {
            val auth = FirebaseAuth.getInstance()
            val firestore = FirebaseFirestore.getInstance()
              val currentUserId = auth.currentUser?.uid ?: return
            
            firestore.collection(Constants.COLLECTION_USERS)
                .document(currentUserId)
                .update("admin", isAdmin)
                .addOnSuccessListener {
                    Log.d(TAG, "Estado de administrador actualizado a: $isAdmin")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al actualizar estado de administrador", e)
                    callback(false)
                }
        }
    }
}
