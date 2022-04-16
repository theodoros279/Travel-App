package com.example.maps_api

import android.content.ContentValues
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class SignUpActivity : AppCompatActivity() {

    private var mAuth = FirebaseAuth.getInstance()
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)
        this.title = "Sign Up"

        val userName = findViewById<TextInputEditText>(R.id.name_input)
        val userEmail = findViewById<TextInputEditText>(R.id.email_input)
        val userPassword = findViewById<TextInputEditText>(R.id.password_input)
        val registerBtn = findViewById<Button>(R.id.register_btn)

        //register user
        registerBtn.setOnClickListener {
            when {
                TextUtils.isEmpty(userName.text.toString().trim()) -> {
                    Snackbar.make(it,
                        R.string.name_empty,
                        Snackbar.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty(userEmail.text.toString().trim{it <= ' '}) -> {
                    Snackbar.make(it,
                        R.string.email_empty,
                        Snackbar.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty(userPassword.text.toString().trim{it <= ' '}) -> {
                    Snackbar.make(it,
                        R.string.password_empty,
                        Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    mAuth.createUserWithEmailAndPassword(userEmail.text.toString(), userPassword.text.toString())
                        .addOnCompleteListener(this,
                            OnCompleteListener<AuthResult?> { task ->
                                Log.d(ContentValues.TAG, "New user registration: " + task.isSuccessful)
                                if (userPassword.text.toString().trim().length < 6) {
                                    Snackbar.make(it,
                                        R.string.password_validation,
                                        Snackbar.LENGTH_SHORT).show()
                                }
                                else if (!task.isSuccessful) {
                                    Snackbar.make(it,
                                        R.string.email_validation,
                                        Snackbar.LENGTH_SHORT).show()
                                } else {
                                    val view = findViewById<TextView>(R.id.snackbar_register)
                                    addUserToDatabase()
                                    mAuth.currentUser!!.sendEmailVerification().addOnSuccessListener {
                                        Snackbar.make(view,
                                            R.string.email_verification_sent,
                                            Snackbar.LENGTH_SHORT).show()
                                    }.addOnFailureListener{
                                        Snackbar.make(view,
                                            R.string.email_verification_not_sent,
                                            Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            })
                }
            }
        }
    }

    fun openSignInActivity(view: View) {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
    }

    private fun addUserToDatabase() {
        val userID = mAuth.currentUser!!.uid
        val firstName = findViewById<TextInputEditText>(R.id.name_input).text.toString()
        val email = findViewById<TextInputEditText>(R.id.email_input).text.toString().trim()
        // add user's first name and email
        val user = hashMapOf(
            "firstName" to firstName,
            "email" to email,
        )
        // Add a new document with a generated ID
        db.collection("users")
            .document(userID)
            .set(user)
            .addOnSuccessListener { documentReference ->
                Log.d(ContentValues.TAG, "DocumentSnapshot added with ID: ${documentReference}")
            }
            .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }
    }
}