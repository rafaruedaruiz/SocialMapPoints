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
                editUserButton.setOnClickListener {
                    val editProfileIntent = Intent(this, EditUserActivity::class.java)
                    editProfileIntent.putExtra("userId", FirebaseAuth.getInstance().currentUser?.uid)
                    startActivity(editProfileIntent)
                }
        }else{
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
                recyclerView.adapter = PointsAdapter2(socialPointsList, pointsIds)
            }

            override fun onCancelled(error: DatabaseError) {
                // Manejar el error
            }
        })

    }

    private inner class PointsAdapter2(private val points: List<SocialPoint>, private val ids: List<String>) :
        RecyclerView.Adapter<PointsAdapter2.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_point_on_list, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val point = points[position]
            val idPoint = ids[position]
            holder.bind(point, idPoint)
        }

        override fun getItemCount(): Int = points.size

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

            private val logo: ImageView = view.findViewById(R.id.imageLogo)
            private val title: TextView = view.findViewById(R.id.tituloTextView)
            private val goButton: ImageButton = view.findViewById(R.id.goButton)

            fun bind(point: SocialPoint, idPoint: String) {                   // FALTA POR CONFIGURAR EL TIPO DE LOGO QUE SALE DEPENDIENDO DE SI ES SOCIALPOINT O EVENT
                title.text = point.title

                title.setOnClickListener {
                    val intent = Intent(itemView.context, InfoSocialPointActivity::class.java)
                    intent.putExtra("socialPointId", idPoint)
                    itemView.context.startActivity(intent)
                }

                goButton.setOnClickListener {
                    val intent = Intent(itemView.context, InfoSocialPointActivity::class.java)
                    intent.putExtra("socialPointId", idPoint)
                    itemView.context.startActivity(intent)
                }
            }
        }
    }
}