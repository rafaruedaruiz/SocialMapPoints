package com.rafa.socialmappoints

import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_info_user.*

class InfoUserActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_user)

        val userId = intent.getStringExtra("userId")

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId!!)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    usernameTextView.text = dataSnapshot.child("username").value.toString()
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar el error
            }
        })

        val storageRef = Firebase.storage.reference
        val photoRef = storageRef.child("profile_photos/${userId}/user_photo.jpg")
        photoRef.downloadUrl.addOnSuccessListener { uri ->
            // Cargar la imagen con Glide
            Glide.with(this).load(uri).transform(CircleCrop()).into(userProfilePhoto)
        }.addOnFailureListener { exception ->
            // Manejar el error
        }


        if(userId == FirebaseAuth.getInstance().currentUser?.uid){  // Si el usuario visita su propio perfil le sale el boton de editar
                editUserButton.visibility = View.VISIBLE

        }else{
            editUserButton.visibility = View.GONE
        }
    }
}