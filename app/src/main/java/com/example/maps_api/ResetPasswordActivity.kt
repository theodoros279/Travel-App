package com.example.maps_api

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class ResetPasswordActivity : AppCompatActivity() {
    private var mAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)
        this.setTitle("Reset Password")

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        val resetPasswordEmail = findViewById<TextInputEditText>(R.id.reset_password_email)
        val resetPasswordBtn = findViewById<Button>(R.id.reset_password_btn)

        resetPasswordBtn.setOnClickListener{
            val userEmail = resetPasswordEmail.text.toString().trim(){it <= ' '}

            if (userEmail.isEmpty()) {
                Snackbar.make(
                    it,
                    R.string.email_empty,
                    Snackbar.LENGTH_SHORT
                ).show()
            } else {
                mAuth.sendPasswordResetEmail(userEmail)
                    .addOnCompleteListener(OnCompleteListener<Void?> { task ->
                        if (task.isSuccessful) {
                            Snackbar.make(
                                it,
                                R.string.check_email,
                                Snackbar.LENGTH_SHORT
                            ).show()
                        } else {
                            Snackbar.make(
                                it,
                                task.exception!!.message.toString(),
                                Snackbar.LENGTH_SHORT
                            ).show()

                        }
                    })
            }
        }
    }
}