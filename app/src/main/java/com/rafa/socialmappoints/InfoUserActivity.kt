package com.rafa.socialmappoints

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
            }
        })

        val storageRef = Firebase.storage.reference
        val photoRef = storageRef.child("profile_photos/${userId}/user_photo.jpg")
        photoRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).transform(CircleCrop()).into(userProfilePhoto)
        }.addOnFailureListener { exception ->
        }


        if (userId == FirebaseAuth.getInstance().currentUser?.uid) {  // Si el usuario visita su propio perfil le sale el boton de editar
            editUserButton.visibility = View.VISIBLE
            editUserButton.setOnClickListener {
                val editProfileIntent = Intent(this, EditUserActivity::class.java)
                editProfileIntent.putExtra("userId", FirebaseAuth.getInstance().currentUser?.uid)
                startActivity(editProfileIntent)
                finish()
            }
        } else {
            editUserButton.visibility = View.GONE
        }

        // Cargar puntos creados por el usuario
        val socialPointsList = mutableListOf<SocialPoint>()
        val pointsIds = mutableListOf<String>()
        val databaseRef = FirebaseDatabase.getInstance().reference
        val socialPointsRef = databaseRef.child("social_points")
        socialPointsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (socialPointSnapshot in dataSnapshot.children) {
                    // Obtener los datos de cada "social_point"
                    val socialPoint = socialPointSnapshot.getValue(SocialPoint::class.java)

                    // Verificar si el "social_point" pertenece al usuario del perfil
                    if (socialPoint!!.userID == userId) {
                        // Agregar el "social_point" a la lista
                        socialPointsList.add(socialPoint)
                        pointsIds.add(socialPointSnapshot.key.toString())
                    }
                }
                val recyclerView = findViewById<RecyclerView>(R.id.pointsRecyclerView)
                recyclerView.layoutManager = LinearLayoutManager(this@InfoUserActivity)
                recyclerView.adapter = PointsAdapter(socialPointsList, pointsIds)
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

    }
}
