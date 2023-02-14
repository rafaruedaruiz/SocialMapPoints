package com.rafa.socialmappoints

import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SocialPointInfoActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_social_point_info)

        val socialPointId = intent.getStringExtra("socialPointId")

        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView)
        val deleteButton = findViewById<Button>(R.id.deleteButton)

        val database = FirebaseDatabase.getInstance().reference
        val socialPoint = socialPointId?.let { database.child("social_points").child(socialPointId) }   // .child(it)
        socialPoint?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val socialPoint = dataSnapshot.getValue(SocialPoint::class.java)
                if (socialPoint != null) {
                    titleTextView.text = socialPoint.title
                    descriptionTextView.text = socialPoint.description
                    if(FirebaseAuth.getInstance().currentUser?.uid.toString() == socialPoint.userID){
                        deleteButton.visibility = View.VISIBLE
                    }else {
                        deleteButton.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // gestionar errores
            }
        })

        deleteButton.setOnClickListener {
            if (socialPoint != null) {
                socialPoint.removeValue()
                Toast.makeText(this, "Punto Social eliminado con éxito", Toast.LENGTH_SHORT).show()
                val homeIntent = Intent(this, HomeActivity::class.java)
                startActivity(homeIntent)
            }
        }

    }
}