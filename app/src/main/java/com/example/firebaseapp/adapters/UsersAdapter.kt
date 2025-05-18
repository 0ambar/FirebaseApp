package com.example.firebaseapp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseapp.R
import com.example.firebaseapp.models.User

class UsersAdapter(
    private val users: List<User>
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.userNameTextView)
        val emailTextView: TextView = itemView.findViewById(R.id.userEmailTextView)
        val roleTextView: TextView = itemView.findViewById(R.id.userRoleTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        
        holder.nameTextView.text = user.name
        holder.emailTextView.text = user.email
        holder.roleTextView.text = if (user.admin) "Administrador" else "Usuario Normal"
    }

    override fun getItemCount(): Int = users.size
}
