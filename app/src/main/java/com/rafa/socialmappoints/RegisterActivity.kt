package com.rafa.socialmappoints

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_auth.emailEditText
import kotlinx.android.synthetic.main.activity_auth.passwordEditText
import kotlinx.android.synthetic.main.activity_auth.registerButton
import kotlinx.android.synthetic.main.activity_register.*
import java.io.ByteArrayOutputStream

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setup()
    }

    private fun setup() {
        title = "Autenticación"

        registerButton.setOnClickListener {
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()) {
                if (passwordEditText.text.toString() == passwordEditText2.text.toString()) {
                    FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                        emailEditText.text.toString(),
                        passwordEditText.text.toString()
                    ).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            showHome(task.result?.user?.email ?: "", ProviderType.BASIC)
                            val uid = task.result?.user?.uid ?: ""
                            uploadDefaultProfileImage(uid, emailEditText.text.toString())
                        } else {
                            val exception = task.exception as? FirebaseAuthException
                            showAlert(exception ?: return@addOnCompleteListener)
                        }
                    }
                } else {
                    showAlert(null, "ERROR_PASSWORD_MISMATCH")
                }
            }
        }
    }

    private fun uploadDefaultProfileImage(uid: String, email: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    // El usuario no existe en la base de datos, lo creamos
                    val username = email.split("@")[0]
                    val newUser = User(username)
                    userRef.setValue(newUser)
                    val storage = Firebase.storage
                    val storageRef = storage.reference
                    val path = "profile_photos/${uid}/user_photo.jpg"
                    val photoRef = storageRef.child(path)
                    val defaultUserPhotoBitmap = BitmapFactory.decodeResource(resources, R.drawable.default_user_photo)
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    defaultUserPhotoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val data = byteArrayOutputStream.toByteArray()
                    val uploadTask = photoRef.putBytes(data)
                    uploadTask.addOnSuccessListener {
                    }.addOnFailureListener { exception ->
                    }
                }
            }
            override fun onCancelled(databaseError: DatabaseError) {
            }
        })
    }


    private fun showAlert(exception: FirebaseAuthException?, errorCode: String? = null) {
        val errorMessage = when (errorCode ?: exception?.errorCode) {
            "ERROR_WEAK_PASSWORD" -> "Error de autenticación: La contraseña es demasiado débil."
            "ERROR_EMAIL_ALREADY_IN_USE" -> "Error de autenticación: Email ya en uso."
            "ERROR_PASSWORD_MISMATCH" -> "Error de autenticación: Las contraseñas no coinciden."
            else -> "Error de autenticación."
        }
        val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
        toastView.findViewById<TextView>(R.id.toastMessage).text = errorMessage
        toast.view = toastView
        toast.show()
    }


    private fun showHome(email: String, provider: ProviderType) {
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
    }
}