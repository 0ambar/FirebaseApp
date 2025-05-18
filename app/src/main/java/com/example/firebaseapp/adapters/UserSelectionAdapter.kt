package com.example.firebaseapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseapp.R
import com.example.firebaseapp.models.User

class UserSelectionAdapter(
    private val users: List<User>
) : RecyclerView.Adapter<UserSelectionAdapter.UserSelectionViewHolder>() {

    // Mapa para almacenar qué usuarios están seleccionados
    private val selectedUsers = mutableMapOf<String, Boolean>()

    init {
        // Inicialmente ningún usuario está seleccionado
        users.forEach { user ->
            selectedUsers[user.uid] = false
        }
    }

    class UserSelectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.userCheckBox)
        val nameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val emailTextView: TextView = itemView.findViewById(R.id.userEmailTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserSelectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_selection, parent, false)
        return UserSelectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserSelectionViewHolder, position: Int) {
        val user = users[position]
        
        holder.nameTextView.text = user.name
        holder.emailTextView.text = user.email
        holder.checkBox.isChecked = selectedUsers[user.uid] ?: false
        
        // Manejar clics en toda la fila para cambiar el estado de la casilla de verificación
        holder.itemView.setOnClickListener {
            val isChecked = !(selectedUsers[user.uid] ?: false)
            selectedUsers[user.uid] = isChecked
            holder.checkBox.isChecked = isChecked
        }
        
        // Manejar clics directamente en la casilla de verificación
        holder.checkBox.setOnClickListener {
            selectedUsers[user.uid] = holder.checkBox.isChecked
        }
    }

    override fun getItemCount(): Int = users.size
    
    // Método para obtener los usuarios seleccionados
    fun getSelectedUsers(): List<User> {
        return users.filter { user ->
            selectedUsers[user.uid] == true
        }
    }
}
