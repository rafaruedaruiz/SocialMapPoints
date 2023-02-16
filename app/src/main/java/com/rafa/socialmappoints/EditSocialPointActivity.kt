package com.rafa.socialmappoints

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_add_point.*
import kotlinx.android.synthetic.main.activity_edit_social_point.*

class EditSocialPointActivity : AppCompatActivity() {

    private lateinit var saveButton: Button
    private lateinit var socialPointId: String
    private lateinit var socialPoint: SocialPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_social_point)

        socialPointId = intent.getStringExtra("socialPointId").toString()
        saveButton = findViewById(R.id.saveButton)

        val database = FirebaseDatabase.getInstance().reference
        val socialPointRef = socialPointId.let { database.child("social_points").child(it) }!!
        socialPointRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                socialPoint = dataSnapshot.getValue(SocialPoint::class.java)!!
                if (socialPoint != null) {
                    titleEditText2.setText(socialPoint.title)
                    descriptionET.setText(socialPoint.description)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // gestionar errores
            }
        })

        saveButton.setOnClickListener {
            saveSocialPoint(socialPoint)
            Toast.makeText(this@EditSocialPointActivity, "Punto Social editado con éxito", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveSocialPoint(socialPoint: SocialPoint?) {
        socialPoint?.let {
            it.title = titleEditText2.text.toString()     // AÑADIR EL RESTO DE ATRIBUTOS DE SOCIALPOINT !!!!
            it.description = descriptionET.text.toString()

            if(it.title.isNotEmpty()){
                val database = FirebaseDatabase.getInstance().reference
                val socialPointRef = socialPointId.let { database.child("social_points").child(it) }
                socialPointRef?.setValue(it)
            }
        }
    }
}