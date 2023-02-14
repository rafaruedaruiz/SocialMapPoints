package com.rafa.socialmappoints

import android.os.Bundle
import android.os.PersistableBundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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

        val database = FirebaseDatabase.getInstance().reference
        val socialPoint = socialPointId?.let { database.child("social_points").child(socialPointId) }   // .child(it)
        socialPoint?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val socialPoint = dataSnapshot.getValue(SocialPoint::class.java)
                if (socialPoint != null) {
                    titleTextView.text = socialPoint.title
                    descriptionTextView.text = socialPoint.description
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // gestionar errores
            }
        })

    }
}