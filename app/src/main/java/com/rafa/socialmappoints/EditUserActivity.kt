package com.rafa.socialmappoints

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
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
import kotlinx.android.synthetic.main.activity_edit_user.*
import kotlinx.android.synthetic.main.activity_info_user.*
import java.io.ByteArrayOutputStream

class EditUserActivity : AppCompatActivity() {

    // fotos permitidas: 1
    val PICK_IMAGE_REQUEST = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_user)

        val userId = intent.getStringExtra("userId")

        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId!!)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    usernameEditText.setText(dataSnapshot.child("username").value.toString())
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar el error
            }
        })

        val storageRef = Firebase.storage.reference
        val photoRef = storageRef.child("profile_photos/${userId}/user_photo.jpg")
        photoRef.downloadUrl.addOnSuccessListener { uri ->
            Glide.with(this).load(uri).transform(CircleCrop()).into(editUserProfilePhoto)
        }.addOnFailureListener { exception ->
            // Manejar el error
        }

        uploadProfilePhotoButton.setOnClickListener {
            // Abre galeria
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        saveUserButton.setOnClickListener {
            if(!usernameEditText.text.toString().isNullOrEmpty()){
                val username = usernameEditText.text.toString()

                // Guarda la imagen en el storage
                val storageRef = Firebase.storage.reference
                val photoRef = storageRef.child("profile_photos/${userId}/user_photo.jpg")
                val bitmap = (editUserProfilePhoto.drawable as BitmapDrawable).bitmap
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
                val data = baos.toByteArray()
                val uploadTask = photoRef.putBytes(data)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                    userRef.child("username").setValue(username)
                    val profileIntent = Intent(this, InfoUserActivity::class.java)
                    profileIntent.putExtra("userId", FirebaseAuth.getInstance().currentUser?.uid)
                    startActivity(profileIntent)
                    finish()
                }.addOnFailureListener { exception ->
                }
            }
        }
    }

    // Manejar el resultado del intent de galería
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.data != null) {
            // Coge la imagen de la galeria y la pone en el ImageView
            val imageUri = data.data
            Glide.with(this).load(imageUri).transform(CircleCrop()).into(editUserProfilePhoto)
        }
    }
}