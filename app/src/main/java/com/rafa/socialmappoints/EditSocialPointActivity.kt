package com.rafa.socialmappoints

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
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



        // Obtengo las imagenes y se las paso al adapter
        val images = mutableListOf<String>()
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("images/$socialPointId")

        storageRef.listAll().addOnSuccessListener { listResult ->
            for (item in listResult.items) {
                val imageName = item.name
                images.add(imageName)
            }
            // adapter
            val recyclerView = findViewById<RecyclerView>(R.id.imageList)
            recyclerView.layoutManager = LinearLayoutManager(this@EditSocialPointActivity)
            recyclerView.adapter = ImageListAdapter(images, socialPointId)
        }

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

private class ImageListAdapter(private val images: MutableList<String>, private val socialPointId: String) : RecyclerView.Adapter<ImageListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_image_on_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = images[position]
        holder.bind(image, socialPointId)
    }

    override fun getItemCount() : Int = images.size
   inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewToDelete)
        val deleteButton: ImageButton = view.findViewById(R.id.redCrossImageButton)
       fun bind(imageName: String, socialPointId: String) {
           val imageRef = FirebaseStorage.getInstance().reference.child("images/$socialPointId/$imageName")
           imageRef.downloadUrl.addOnSuccessListener { uri ->
               val imageUrl = uri.toString()
               Glide.with(itemView.context).load(imageUrl).into(imageView)
           }.addOnFailureListener { exception ->
               // Manejar la excepción
           }

           deleteButton.setOnClickListener {
               val storage = FirebaseStorage.getInstance()
               Log.d("EditSocialPointActivity", "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXimages/$socialPointId/image_${adapterPosition + 1}")
               val storageRef = storage.reference.child("images/$socialPointId/$imageName")
               storageRef.delete().addOnSuccessListener {
                   images.removeAt(adapterPosition)
                   notifyItemRemoved(adapterPosition)
                   notifyItemRangeChanged(adapterPosition, images.size)
               }
           }
       }
   }
}

