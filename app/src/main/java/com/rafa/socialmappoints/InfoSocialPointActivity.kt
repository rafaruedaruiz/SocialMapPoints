package com.rafa.socialmappoints

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_info_social_point.*
import me.relex.circleindicator.CircleIndicator3
import java.io.File

class InfoSocialPointActivity : AppCompatActivity() {

    private lateinit var socialPoint: SocialPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_social_point)

        val socialPointId = intent.getStringExtra("socialPointId")

        val viewPager2 = findViewById<ViewPager2>(R.id.viewPager2)
        val indicator = findViewById<CircleIndicator3>(R.id.indicator)
        indicator.setViewPager(viewPager2)

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
            if (listResult.items.isNotEmpty()) {
                val imageList = mutableListOf<Bitmap>()
                for (item in listResult.items) {
                    val localFile = File.createTempFile("images", "jpg")
                    item.getFile(localFile).addOnSuccessListener {
                        val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(localFile))
                        imageList.add(bitmap)
                        if (imageList.size == listResult.items.size) {
                            // Se han descargado todas las imágenes, se actualiza el ViewPager2
                            viewPager2.adapter = ImagePagerAdapter(imageList)
                            indicator.setViewPager(viewPager2)
                        }
                    }
                }
            } else {
                cardView.visibility = View.GONE
                viewPager2.visibility = View.GONE
                indicator.visibility = View.GONE
            }
        }.addOnFailureListener { exception ->

        }


        // El usuario envia un comentario
        val commentEditText = findViewById<EditText>(R.id.commentEditText)
        commentEditText.setOnKeyListener { view, keyCode, keyEvent ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && keyEvent.action == KeyEvent.ACTION_UP) {
                val commentText = commentEditText.text.toString().trim()
                if (commentText.isNotEmpty()) {
                    val userId = FirebaseAuth.getInstance().currentUser?.uid
                    val timestamp = System.currentTimeMillis()

                    val comment = userId?.let { Comment(it, commentText, timestamp) }

                    // Actualiza la lista de comentarios del SocialPoint en la base de datos
                    val socialPointRef = FirebaseDatabase.getInstance().getReference().child("social_points").child(socialPointId)
                    socialPointRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val socialPoint = dataSnapshot.getValue(SocialPoint::class.java)
                            socialPoint?.let {
                                it.comments.add(comment!!)
                                socialPointRef.setValue(it)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            // Manejar el error
                        }
                    })

                    commentEditText.text = null
                }
                return@setOnKeyListener true
            }
            false
        }

        deleteButton.setOnClickListener {
            if (socialPoint != null) {
                val builder = AlertDialog.Builder(this@InfoSocialPointActivity)
                builder.setTitle("Eliminar punto")
                builder.setMessage("¿Está seguro de que quiere eliminar este punto social?")
                builder.setPositiveButton("Eliminar") { _, _ ->
                    // Elimina el SocialPoint de Realtime Database
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
            finish()
        }

        comoLlegarButton.setOnClickListener {
            val uri = Uri.parse("google.navigation:q=${socialPoint.latitude},${socialPoint.longitude}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }

        verEnElMapaButton.setOnClickListener {
            val homeIntent = Intent(this, HomeActivity::class.java)
            homeIntent.putExtra("lat", socialPoint.latitude)
            homeIntent.putExtra("lng", socialPoint.longitude)
            startActivity(homeIntent)
            finish()
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


