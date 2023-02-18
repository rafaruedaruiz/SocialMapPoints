package com.rafa.socialmappoints

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_add_point.*
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage




class AddPointActivity : AppCompatActivity() {

    private lateinit var addPointButton: Button
    private lateinit var imageButton: Button
    private lateinit var imageUriList: MutableList<Uri>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_point)

        addPointButton = findViewById(R.id.addPointButton)
        imageButton = findViewById(R.id.addImageButton)
        imageUriList = mutableListOf()

        imageButton.setOnClickListener {
            // abre galeria
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), REQUEST_CODE_SELECT_IMAGES)
        }

        addPointButton.setOnClickListener {
            val userID = FirebaseAuth.getInstance().currentUser?.uid.toString()
            val title = titleEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val latitude = intent.getDoubleExtra("latitude", 0.0)
            val longitude = intent.getDoubleExtra("longitude", 0.0)
            val socialPoint = SocialPoint(userID, title, description, latitude, longitude)

            if(title.isNotEmpty()){
                saveSocialPoint(socialPoint)
                Toast.makeText(this, "Punto Social añadido con éxito", Toast.LENGTH_SHORT).show()
                val homeIntent = Intent(this, HomeActivity::class.java)
                startActivity(homeIntent)
            }else{
                Toast.makeText(this, "Por favor, rellene los campos necesarios", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveSocialPoint(socialPoint: SocialPoint) {
        val database = Firebase.database
        val reference = database.getReference("social_points")
        val key = reference.push().key
        reference.child(key!!).setValue(socialPoint)
            .addOnSuccessListener {
                // Sube las imágenes a Firebase Storage
                uploadImages(key)
            }
    }

    private fun uploadImages(socialPointID: String) {
        val storage = Firebase.storage
        val reference = storage.reference.child("images/$socialPointID")
        for (i in 0 until imageUriList.size) {
            val imageReference = reference.child("image_$i")
            imageReference.putFile(imageUriList[i])
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_IMAGES && resultCode == RESULT_OK) {
            // Guarda las URIs de las imágenes seleccionadas
            if (data?.clipData != null) {
                val clipData = data.clipData!!
                for (i in 0 until clipData.itemCount) {
                    imageUriList.add(clipData.getItemAt(i).uri)
                }
            } else if (data?.data != null) {
                imageUriList.add(data.data!!)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SELECT_IMAGES = 1
    }
}
