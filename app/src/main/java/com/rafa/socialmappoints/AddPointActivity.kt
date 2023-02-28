package com.rafa.socialmappoints

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_add_point.*
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage




class AddPointActivity : AppCompatActivity() {

    private lateinit var addPointButton: Button
    private lateinit var imageButton: Button
    private lateinit var imageUriList: MutableList<Uri>
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_point)

        addPointButton = findViewById(R.id.addPointButton)
        imageButton = findViewById(R.id.addImageButton)
        imageUriList = mutableListOf()
        recyclerView = findViewById(R.id.imageList2)

        imageButton.setOnClickListener {
            // abre galeria
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(Intent.createChooser(intent, "Selecciona imágenes"), REQUEST_CODE_SELECT_IMAGES)
        }

        addPointButton.setOnClickListener {
            val userID = FirebaseAuth.getInstance().currentUser?.uid.toString()
            val title = titleEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val latitude = intent.getDoubleExtra("latitude", 0.0)
            val longitude = intent.getDoubleExtra("longitude", 0.0)
            val comments = mutableListOf<Comment>()

            val socialPoint = SocialPoint(userID, title, description, latitude, longitude, comments)


            if(title.isNotEmpty()){
                saveSocialPoint(socialPoint)

                val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
                val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
                toastView.findViewById<TextView>(R.id.toastMessage).text = "Punto Social añadido con éxito"
                toast.view = toastView
                toast.show()

                val homeIntent = Intent(this, HomeActivity::class.java)
                homeIntent.putExtra("lat", latitude)
                homeIntent.putExtra("lng", longitude)
                startActivity(homeIntent)
                finish()
            }else{
                val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
                val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
                toastView.findViewById<TextView>(R.id.toastMessage).text = "Por favor, rellene los campos necesarios"
                toast.view = toastView
                toast.show()
            }
        }
    }

    private fun saveSocialPoint(socialPoint: SocialPoint) {
        val database = Firebase.database
        val reference = database.getReference("social_points")
        val key = reference.push().key
        reference.child(key!!).setValue(socialPoint)
            .addOnSuccessListener {
                // Sube las imágenes a Firebase Storage
                uploadImages(key)
            }
    }

    private fun uploadImages(socialPointID: String) {
        val storage = Firebase.storage
        val reference = storage.reference.child("images/$socialPointID")
        for (i in 0 until imageUriList.size) {
            val imageReference = reference.child("image_$i")
            imageReference.putFile(imageUriList[i])
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SELECT_IMAGES && resultCode == RESULT_OK) {
            // Guarda las URIs de las imágenes seleccionadas
            if (data?.clipData != null) {
                val clipData = data.clipData!!
                for (i in 0 until clipData.itemCount) {
                    imageUriList.add(clipData.getItemAt(i).uri)
                }
                // adapter
                recyclerView.layoutManager = LinearLayoutManager(this@AddPointActivity)
                recyclerView.adapter = ImageList2Adapter(imageUriList)
                val verticalSpaceItemDecoration = VerticalSpaceItemDecoration2(0)
                recyclerView.addItemDecoration(verticalSpaceItemDecoration)
            } else if (data?.data != null) {
                imageUriList.add(data.data!!)
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_SELECT_IMAGES = 1
    }
}

private class VerticalSpaceItemDecoration2(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.top = spaceHeight
        outRect.bottom = spaceHeight
    }
}


private class ImageList2Adapter(private val images: MutableList<Uri>) : RecyclerView.Adapter<ImageList2Adapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_image_on_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val image = images[position]
        holder.bind(image)
    }

    override fun getItemCount() : Int = images.size
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageViewToDelete)
        val deleteButton: ImageButton = view.findViewById(R.id.redCrossImageButton)
        fun bind(image: Uri) {
            Glide.with(itemView.context).load(image).into(imageView)

            deleteButton.setOnClickListener {
                    images.removeAt(adapterPosition)
                    notifyItemRemoved(adapterPosition)
                    notifyItemRangeChanged(adapterPosition, images.size)
            }
        }
    }
}
