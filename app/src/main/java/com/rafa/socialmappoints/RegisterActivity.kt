package com.rafa.socialmappoints

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.android.synthetic.main.activity_auth.emailEditText
import kotlinx.android.synthetic.main.activity_auth.passwordEditText
import kotlinx.android.synthetic.main.activity_auth.registerButton
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Setup
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