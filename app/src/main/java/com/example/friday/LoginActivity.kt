package com.example.friday

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.friday.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loadingProgressBar: ProgressBar

    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)

        val loginButton = findViewById<Button>(R.id.loginButton)
        val passInput = findViewById<EditText>(R.id.passwordInput)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val gButton = findViewById<ImageView>(R.id.gButton)
        val signUp = findViewById<TextView>(R.id.Signup)
        val forgotPass = findViewById<TextView>(R.id.forgotPassword)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        gButton.setOnClickListener {
            showLoadingProgressBar()
            signInWithGoogle()
        }

        var backPressedTime: Long = 0
        val backToastDuration = 2000 // 2 seconds
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (backPressedTime + backToastDuration > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, "Press back again to exit", Toast.LENGTH_SHORT).show()
                    backPressedTime = System.currentTimeMillis()
                }
            }
        })


        loginButton.setOnClickListener {
            val email = emailInput.text.toString()
            val password = passInput.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please fill in both fields", Toast.LENGTH_SHORT).show()
            }
        }

        signUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        forgotPass.setOnClickListener {
            showForgotPasswordDialog()
        }
    }

    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    firebaseAuthWithGoogle(account)
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign-In Failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
                hideLoadingProgressBar()
            }
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential: AuthCredential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                hideLoadingProgressBar()
                if (task.isSuccessful) {
                    Toast.makeText(this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showLoadingProgressBar() {
        loadingProgressBar.visibility = ProgressBar.VISIBLE
    }

    private fun hideLoadingProgressBar() {
        loadingProgressBar.visibility = ProgressBar.GONE
    }
    private fun loginUser(email: String, password: String) {
        // Show progress bar & disable button
        showLoadingProgressBar()

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                hideLoadingProgressBar()
                if (task.isSuccessful) {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun showForgotPasswordDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.custom_dialog)

        // Set dialog width to match parent
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Initialize UI elements
        val dialogInput = dialog.findViewById<EditText>(R.id.dialogInput)
        val confirmButton = dialog.findViewById<Button>(R.id.confirm_button)
        val cancelButton = dialog.findViewById<Button>(R.id.cancel_button)
        val progressBar = dialog.findViewById<ProgressBar>(R.id.dialogProgressBar)

        confirmButton.setOnClickListener {
            val email = dialogInput.text.toString().trim()

            if (email.isNotEmpty()) {
                // Show progress & disable button
                progressBar.visibility = View.VISIBLE
                confirmButton.isEnabled = false
                cancelButton.isEnabled = false

                resetPassword(email, dialog, progressBar, confirmButton, cancelButton)
            } else {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
            }
        }

        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun resetPassword(
        email: String,
        dialog: Dialog,
        progressBar: ProgressBar,
        confirmButton: Button,
        cancelButton: Button
    ) {
        progressBar.visibility = View.VISIBLE
        confirmButton.isEnabled = false
        cancelButton.isEnabled = false

        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                progressBar.visibility = View.GONE
                confirmButton.isEnabled = true
                cancelButton.isEnabled = true

                if (task.isSuccessful) {
                    Toast.makeText(this, "Password reset email sent!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss() // Close the dialog
                } else {
                    Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }



}
