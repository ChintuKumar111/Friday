package com.example.friday

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SignUpActivity : AppCompatActivity() {

    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance()

        // Initialize UI components
        nameEditText = findViewById(R.id.Name)
        emailEditText = findViewById(R.id.emailInput)
        passwordEditText = findViewById(R.id.passwordInput)
        signUpButton = findViewById(R.id.SignBtn)
        loadingProgressBar = findViewById(R.id.loadingProgressBar)

        // Set OnClickListener for Sign Up Button
        signUpButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                if (isPasswordValid(password)) {
                    showLoadingProgressBar()
                    signUpUser(email, password)
                } else {
                    Toast.makeText(this, "Password must be at least 8 characters, with at least one uppercase letter, one number or symbol", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signUpUser(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                hideLoadingProgressBar()  // Hide the ProgressBar after the process is done
                if (task.isSuccessful) {
                    // Sign-up successful
                    Toast.makeText(this, "Sign-Up Successful", Toast.LENGTH_SHORT).show()
                    clearInputFields()  // Clear the input fields
                    // Optionally, redirect to HomeActivity or another screen
                } else {
                    // Sign-up failed
                    Toast.makeText(this, "Sign-Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    clearInputFields()  // Optionally, clear fields even if the sign-up fails
                }
            }
    }

    private fun showLoadingProgressBar() {
        loadingProgressBar.visibility = android.view.View.VISIBLE  // Show the ProgressBar
    }

    private fun hideLoadingProgressBar() {
        loadingProgressBar.visibility = android.view.View.GONE  // Hide the ProgressBar
    }

    // Function to clear input fields
    private fun clearInputFields() {
        nameEditText.text.clear()  // Clear the Name field
        emailEditText.text.clear()  // Clear the Email field
        passwordEditText.text.clear()  // Clear the Password field
    }

    // Function to validate the password
    private fun isPasswordValid(password: String): Boolean {
        // Regular expression for password validation
        val passwordPattern = "^(?=.*[A-Z])(?=.*[0-9!@#\$%^&*])[A-Za-z0-9!@#\$%^&*]{8,}$"
        return password.matches(passwordPattern.toRegex())
    }
}
