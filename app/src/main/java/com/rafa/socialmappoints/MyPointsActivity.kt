package com.rafa.socialmappoints

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_my_points.*

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


    private inner class PointsAdapter(private val points: List<SocialPoint>, private val ids: List<String>) :
        RecyclerView.Adapter<PointsAdapter.ViewHolder>() {

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
                    val intent = Intent(itemView.context, SocialPointInfoActivity::class.java)
                    intent.putExtra("socialPointId", idPoint)
                    itemView.context.startActivity(intent)
                }

                goButton.setOnClickListener {
                    val intent = Intent(itemView.context, SocialPointInfoActivity::class.java)
                    intent.putExtra("socialPointId", idPoint)
                    itemView.context.startActivity(intent)
                }
            }
        }
    }

}
