package com.rafa.socialmappoints

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_add_point.*

class AddPointActivity : AppCompatActivity() {

    private lateinit var addPointButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_point)

        addPointButton = findViewById(R.id.addPointButton)

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
    }
}
