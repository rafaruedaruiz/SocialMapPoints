package com.rafa.socialmappoints

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
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
import java.util.*


class EditSocialPointActivity : AppCompatActivity() {

    private lateinit var saveButton: Button
    private lateinit var uploadImageButton: Button
    private lateinit var socialPointId: String
    private lateinit var socialPoint: SocialPoint
    private lateinit var recyclerView: RecyclerView
    private val images = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_social_point)
        saveButton = findViewById(R.id.saveButton)
        uploadImageButton = findViewById(R.id.uploadImageButton)
        recyclerView = findViewById<RecyclerView>(R.id.imageList)

        socialPointId = intent.getStringExtra("socialPointId").toString()

        val database = FirebaseDatabase.getInstance().reference
        val socialPointRef = socialPointId.let { database.child("social_points").child(it) }!!
        socialPointRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val tempSocialPoint = dataSnapshot.getValue(SocialPoint::class.java)
                if (tempSocialPoint != null) {
                    socialPoint = tempSocialPoint
                    titleEditText2.setText(socialPoint.title)
                    descriptionET.setText(socialPoint.description)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                // gestionar errores
            }
        })



        // Obtengo las imagenes y se las paso al adapter
        val storage = FirebaseStorage.getInstance()
        val storageRef = storage.reference.child("images/$socialPointId")

        storageRef.listAll().addOnSuccessListener { listResult ->
            for (item in listResult.items) {
                val imageName = item.name
                images.add(imageName)
            }
            // adapter
            recyclerView.layoutManager = LinearLayoutManager(this@EditSocialPointActivity)
            recyclerView.adapter = ImageListAdapter(images, socialPointId)
        }

        saveButton.setOnClickListener {
            saveSocialPoint(socialPoint)
            val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
            val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
            toastView.findViewById<TextView>(R.id.toastMessage).text = "Punto Social editado con éxito"
            toast.view = toastView
            toast.show()

            // Volvemos a la vista anterior pero con los datos actualizados
            val socialPointInfoIntent = Intent(this, InfoSocialPointActivity::class.java)
            socialPointInfoIntent.putExtra("socialPointId", socialPointId)
            startActivity(socialPointInfoIntent)
            finish()
        }

        uploadImageButton.setOnClickListener {
            selectImages()
        }

    }

    private val PICK_IMAGES = 1

    private fun selectImages() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), PICK_IMAGES)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGES && resultCode == Activity.RESULT_OK && data != null) {
            val clipData = data.clipData
            if (clipData != null) {
                for (i in 0 until clipData.itemCount) {
                    val imageUri = clipData.getItemAt(i).uri
                    uploadImage(imageUri)
                }
            } else {
                val imageUri = data.data
                if (imageUri != null) {
                    uploadImage(imageUri)
                }
            }
        }
    }

    private fun uploadImage(imageUri: Uri) {
        val imageName = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().reference.child("images/$socialPointId/$imageName")
        storageRef.putFile(imageUri).addOnSuccessListener { taskSnapshot ->
            taskSnapshot.metadata?.reference?.downloadUrl?.addOnSuccessListener { uri ->
                images.add(imageName)
                recyclerView.adapter?.notifyItemInserted(images.size - 1)
            }
        }.addOnFailureListener { exception ->
            // Manejar la excepción
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

