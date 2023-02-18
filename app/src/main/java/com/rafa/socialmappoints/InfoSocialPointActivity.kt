package com.rafa.socialmappoints

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_info_social_point.*
import java.io.File

class InfoSocialPointActivity : AppCompatActivity() {

    private lateinit var socialPoint: SocialPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_social_point)

        val socialPointId = intent.getStringExtra("socialPointId")

        val viewPager2 = findViewById<ViewPager2>(R.id.viewPager2)

        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView)
        val deleteButton = findViewById<ImageButton>(R.id.deleteButton)
        val editButton = findViewById<ImageButton>(R.id.editButton)

        val database = FirebaseDatabase.getInstance().reference
        val socialPointRef = socialPointId?.let { database.child("social_points").child(socialPointId) }!!
        socialPointRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                socialPoint = dataSnapshot.getValue(SocialPoint::class.java)!!
                if (socialPoint != null) {
                    titleTextView.text = socialPoint.title
                    descriptionTextView.text = socialPoint.description
                    if (FirebaseAuth.getInstance().currentUser?.uid.toString() == socialPoint.userID) {
                        deleteButton.visibility = View.VISIBLE
                        editButton.visibility = View.VISIBLE
                    } else {
                        deleteButton.visibility = View.GONE
                        editButton.visibility = View.GONE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // gestionar errores
            }
        })

        // Recoger imagenes de firebase storage
        val storageRef = Firebase.storage.reference
        val imagesRef = storageRef.child("images/$socialPointId")

        imagesRef.listAll().addOnSuccessListener { listResult ->
            val imageList = mutableListOf<Bitmap>()
            for (item in listResult.items) {
                val localFile = File.createTempFile("images", "jpg")
                item.getFile(localFile).addOnSuccessListener {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(localFile))
                    imageList.add(bitmap)
                    if (imageList.size == listResult.items.size) {
                        // Se han descargado todas las imágenes, se actualiza el ViewPager2
                        viewPager2.adapter = ImagePagerAdapter(imageList)
                    }
                }
            }
        }.addOnFailureListener { exception ->
            // Si no tiene imágenes asociadas no se muestra ninguna y hay que quitar el viewPager
            viewPager2.setVisibility(View.GONE)
        }


        deleteButton.setOnClickListener {
            if (socialPoint != null) {
                val builder = AlertDialog.Builder(this@InfoSocialPointActivity)
                builder.setTitle("Eliminar punto")
                builder.setMessage("¿Está seguro de que quiere eliminar este punto social?")
                builder.setPositiveButton("Eliminar") { _, _ ->
                    socialPointRef.removeValue()
                    Toast.makeText(this, "Punto Social eliminado con éxito", Toast.LENGTH_SHORT).show()
                    val homeIntent = Intent(this, HomeActivity::class.java)
                    startActivity(homeIntent)
                }
                builder.setNegativeButton("Cancelar") { _, _ ->
                    // El usuario ha cancelado (no hacer nada)
                }
                builder.create().show()
            }
        }

        editButton.setOnClickListener{
            val editIntent = Intent(this, EditSocialPointActivity::class.java)
            editIntent.putExtra("socialPointId", socialPointId)
            startActivity(editIntent)
        }

        comoLlegarButton.setOnClickListener {
            val uri = Uri.parse("google.navigation:q=${socialPoint.latitude},${socialPoint.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }
}

class ImagePagerAdapter(private val images: List<Bitmap>) : RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.image_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.imageView.setImageBitmap(images[position])
    }

    override fun getItemCount(): Int {
        return images.size
    }
}


