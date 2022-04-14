package com.example.maps_api

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class MyLifecycleObserver(private val registry : ActivityResultRegistry)
    : DefaultLifecycleObserver {
    private lateinit var getContent : ActivityResultLauncher<String>
    private var imageUri : Uri? = null

    override fun onCreate(owner: LifecycleOwner) {
        getContent = registry.register("key", owner, ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                val activity = owner as Activity
                Toast.makeText(activity, "image selected", Toast.LENGTH_SHORT).show()
            } else {
                Log.d("image", "not selected")
            }
        }
    }

    fun selectImage() {
        getContent.launch("image/*")
    }

    fun getSelectedImage(): Uri {
        return if (imageUri != null) {
            imageUri!!
        } else {
            Uri.EMPTY
        }
    }
}