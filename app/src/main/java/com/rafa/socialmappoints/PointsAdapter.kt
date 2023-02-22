package com.rafa.socialmappoints

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

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