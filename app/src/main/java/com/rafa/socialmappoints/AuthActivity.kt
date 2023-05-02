package com.rafa.socialmappoints

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_auth.*
import kotlinx.android.synthetic.main.activity_home.*
import java.io.ByteArrayOutputStream

class AuthActivity : AppCompatActivity() {

    private val GOOGLE_SIGN_IN = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        setup()
        session()
    }

    override fun onStart() {
        super.onStart()

        authLayout.visibility = View.VISIBLE
    }
    private fun session(){
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE)
        val email = prefs.getString("email", null)
        val provider = prefs.getString("provider", null)
        if(email != null && provider != null){
            authLayout.visibility = View.INVISIBLE
            showHome(email, ProviderType.valueOf(provider))
        }
    }

    private fun setup(){
        title = "Autenticación"

        registerButton.setOnClickListener{
            val registerIntent = Intent(this, RegisterActivity::class.java)
            startActivity(registerIntent)
        }

        loginButton.setOnClickListener{
            if (emailEditText.text.isNotEmpty() && passwordEditText.text.isNotEmpty()){
                FirebaseAuth.getInstance().signInWithEmailAndPassword(emailEditText.text.toString(),
                    passwordEditText.text.toString()).addOnCompleteListener(){
                    if(it.isSuccessful){
                        showHome(it.result?.user?.email ?: "", ProviderType.BASIC)
                    }else{
                        showAlert()
                    }
                }
            }
        }

        googleButton.setOnClickListener{
            val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

            val googleClient = GoogleSignIn.getClient(this, googleConf)
            googleClient.signOut()

            startActivityForResult(googleClient.signInIntent, GOOGLE_SIGN_IN)
        }
    }

    private fun showAlert(){
        val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
        toastView.findViewById<TextView>(R.id.toastMessage).text = "Error de autenticación: El email o contraseña no coinciden."
        toast.view = toastView
        toast.show()
    }

    private fun showHome(email: String, provider: ProviderType){
        // Cargamos el usuario en la base de datos si no ha sido cargado previamente
        val user = FirebaseAuth.getInstance().currentUser
        if(user != null){
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (!dataSnapshot.exists()) {
                        // El usuario no existe en la base de datos, lo creamos
                        val username = user.email.toString().split("@")[0]
                        val newUser = User(username)
                        userRef.setValue(newUser)
                        val storage = Firebase.storage
                        val storageRef = storage.reference
                        val path = "profile_photos/${user.uid}/user_photo.jpg"
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

        // Vamos a la activity_home
        val homeIntent = Intent(this, HomeActivity::class.java).apply {
            putExtra("email", email)
            putExtra("provider", provider.name)
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == GOOGLE_SIGN_IN){
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                val account = task.getResult(ApiException::class.java)

                if(account != null){
                    val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnCompleteListener(){
                        if(it.isSuccessful){
                            showHome(account.email ?: "", ProviderType.GOOGLE)
                        }else{
                            showAlert()
                        }
                    }
                }
            }catch(e: ApiException){
                showAlert()
            }
        }
    }
}