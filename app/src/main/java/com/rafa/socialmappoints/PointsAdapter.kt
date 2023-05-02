package com.rafa.socialmappoints

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_edit_user.*
import soup.neumorphism.NeumorphCardView

class PointsAdapter (private val points: List<SocialPoint>, private val ids: List<String>) :
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
        private val spImage: ImageView = view.findViewById(R.id.spImage)
        private val spDescription: TextView = view.findViewById(R.id.spDescription)
        private val socialPointCardView2: NeumorphCardView = view.findViewById(R.id.socialPointCardView2)

        fun bind(point: SocialPoint, idPoint: String) {
            title.text = point.title
            spDescription.text = point.description

            val storageRef = Firebase.storage.reference.child("images").child(idPoint)
            storageRef.listAll().addOnSuccessListener { listResult ->
                // Obtener el primer archivo de la lista
                val imageRef = listResult.items.firstOrNull()
                if (imageRef != null) {
                    // Descargar la imagen
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        Glide.with(itemView.context).load(uri).into(spImage)
                    }.addOnFailureListener { exception ->
                    }
                }
            }.addOnFailureListener { exception ->
            }


            socialPointCardView2.setOnClickListener {
                val intent = Intent(itemView.context, InfoSocialPointActivity::class.java)
                intent.putExtra("socialPointId", idPoint)
                itemView.context.startActivity(intent)
            }
        }
    }
}