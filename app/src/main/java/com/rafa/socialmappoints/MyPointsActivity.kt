package com.rafa.socialmappoints

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MyPointsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_points)


        val socialPointsRef = FirebaseDatabase.getInstance().getReference("social_points")
        val socialPoints = mutableListOf<SocialPoint>()
        val pointsIds = mutableListOf<String>()

        val userId = FirebaseAuth.getInstance().currentUser?.uid

        socialPointsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (ds in dataSnapshot.children) {
                    val socialPoint = ds.getValue(SocialPoint::class.java)
                    if (socialPoint != null && socialPoint.userID == userId) {   // SOLO COGE LOS DEL USUARIO
                        socialPoints.add(socialPoint)
                        pointsIds.add(ds.key.toString())
                    }
                }
                val recyclerView = findViewById<RecyclerView>(R.id.recyclerViewMyPoints)
                recyclerView.layoutManager = LinearLayoutManager(this@MyPointsActivity)
                recyclerView.adapter = PointsAdapter(socialPoints, pointsIds)
            }
            override fun onCancelled(error: DatabaseError) {
                // Controlar error
            }
        })

    }
}
