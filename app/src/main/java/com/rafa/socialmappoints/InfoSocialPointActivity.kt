package com.rafa.socialmappoints

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_home.*
import kotlinx.android.synthetic.main.activity_info_social_point.*
import kotlinx.android.synthetic.main.activity_info_user.*
import me.relex.circleindicator.CircleIndicator3
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class InfoSocialPointActivity : AppCompatActivity() {

    private lateinit var socialPoint: SocialPoint

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info_social_point)

        val socialPointId = intent.getStringExtra("socialPointId")
        val storageRef = Firebase.storage.reference

        val viewPager2 = findViewById<ViewPager2>(R.id.viewPager2)
        val indicator = findViewById<CircleIndicator3>(R.id.indicator)
        indicator.setViewPager(viewPager2)

        val titleTextView = findViewById<TextView>(R.id.titleTextView)
        val descriptionTextView = findViewById<TextView>(R.id.descriptionTextView)
        val deleteButton = findViewById<ImageButton>(R.id.deleteButton)
        val editButton = findViewById<ImageButton>(R.id.editButton)

        val commentList = mutableListOf<Comment>()
        val commentAdapter = CommentAdapter(commentList)
        val commentRecyclerView = findViewById<RecyclerView>(R.id.commentsRecyclerView)

        val database = FirebaseDatabase.getInstance().reference
        val socialPointRef = socialPointId?.let { database.child("social_points").child(socialPointId) }!!
        socialPointRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val sp = dataSnapshot.getValue(SocialPoint::class.java)
                if (sp != null) {
                    socialPoint = sp
                    titleTextView.text = socialPoint.title
                    descriptionTextView.text = socialPoint.description
                    if (FirebaseAuth.getInstance().currentUser?.uid.toString() == socialPoint.userID) {
                        deleteButton.visibility = View.VISIBLE
                        editButton.visibility = View.VISIBLE
                    } else {
                        deleteButton.visibility = View.GONE
                        editButton.visibility = View.GONE
                    }
                    // Actualiza la caja de comentarios
                    commentList.clear()
                    commentList.addAll(socialPoint.comments.reversed())
                    commentAdapter.notifyDataSetChanged()

                    // Carga imagen y username del "Creado por"
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(socialPoint.userID!!)
                    userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            if (dataSnapshot.exists()) {
                                userUsername.text = dataSnapshot.child("username").value.toString()
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                        }
                    })

                    val photoRef = storageRef.child("profile_photos/${socialPoint.userID}/user_photo.jpg")
                    photoRef.downloadUrl.addOnSuccessListener { uri ->
                        if (!isDestroyed) {
                            Glide.with(this@InfoSocialPointActivity).load(uri).transform(CircleCrop()).into(userProfilePhoto2)
                        }

                    }.addOnFailureListener { exception ->
                    }

                    userUsername.setOnClickListener {
                        val profileIntent = Intent(this@InfoSocialPointActivity, InfoUserActivity::class.java)
                        profileIntent.putExtra("userId", socialPoint.userID)
                        this@InfoSocialPointActivity.startActivity(profileIntent)
                    }

                    userProfilePhoto2.setOnClickListener {
                        val profileIntent = Intent(this@InfoSocialPointActivity, InfoUserActivity::class.java)
                        profileIntent.putExtra("userId", socialPoint.userID)
                        this@InfoSocialPointActivity.startActivity(profileIntent)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        commentRecyclerView.layoutManager = LinearLayoutManager(this)
        commentRecyclerView.adapter = commentAdapter
        val verticalSpaceItemDecoration = VerticalSpaceItemDecoration(20)
        commentRecyclerView.addItemDecoration(verticalSpaceItemDecoration)


        // Recoger imagenes de firebase storage
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
                    val user = FirebaseAuth.getInstance().currentUser
                    val userId = user?.uid

                    val timestamp = System.currentTimeMillis()
                    val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                    val formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm")
                    val formattedDateTime = dateTime.format(formatter)
                    val dateString = "El " + formattedDateTime.substring(0, 5) + " a las " + formattedDateTime.substring(6)

                    val comment = Comment(userId!!, commentText, dateString)

                    // Actualiza la lista de comentarios del SocialPoint en la base de datos
                    val socialPointRef = FirebaseDatabase.getInstance().getReference().child("social_points").child(socialPointId)
                    socialPointRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val socialPoint = dataSnapshot.getValue(SocialPoint::class.java)
                            socialPoint?.let {
                                it.comments.add(comment)
                                socialPointRef.setValue(it)
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {
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

                    val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
                    val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
                    toastView.findViewById<TextView>(R.id.toastMessage).text = "Punto Social eliminado con éxito"
                    toast.view = toastView
                    toast.show()
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
            homeIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(homeIntent)
            finish()
        }

    }
}

private class VerticalSpaceItemDecoration(private val spaceHeight: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.bottom = spaceHeight
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

private class CommentAdapter(private val comments: MutableList<Comment>) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.view_comment_on_list, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        holder.bind(comment)
    }

    override fun getItemCount() : Int = comments.size
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val authorPhoto: CircleImageView = view.findViewById(R.id.commentAuthorPhoto)
        val authorUsername: TextView = view.findViewById(R.id.commentAuthor)
        val message : TextView = view.findViewById(R.id.commentMessage)
        val date : TextView = view.findViewById(R.id.commentTimestamp)
        fun bind(comment: Comment) {
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(comment.userId!!)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        authorUsername.text = dataSnapshot.child("username").value.toString()
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                }
            })

            message.text = comment.message
            date.text = comment.dateAndTime

            val storageRef = Firebase.storage.reference
            val photoRef = storageRef.child("profile_photos/${comment.userId}/user_photo.jpg")
            photoRef.downloadUrl.addOnSuccessListener { uri ->
                if (!(itemView.context as InfoSocialPointActivity).isDestroyed) {
                    Glide.with(itemView.context).load(uri).transform(CircleCrop()).into(authorPhoto)
                }
            }.addOnFailureListener { exception ->
            }

            authorPhoto.setOnClickListener {
                val profileIntent = Intent(itemView.context, InfoUserActivity::class.java)
                profileIntent.putExtra("userId", comment.userId)
                itemView.context.startActivity(profileIntent)
            }

            authorUsername.setOnClickListener {
                val profileIntent = Intent(itemView.context, InfoUserActivity::class.java)
                profileIntent.putExtra("userId", comment.userId)
                itemView.context.startActivity(profileIntent)
            }
        }
    }
}