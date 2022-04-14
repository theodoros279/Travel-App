package com.example.maps_api

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.example.maps_api.R
import com.example.maps_api.models.CustomInfoWindowModel
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class CustomInfoWindowAdapter(val context: Context) : GoogleMap.InfoWindowAdapter {

    override fun getInfoContents(p0: Marker): View? {
        val mInfoView = (context as Activity).layoutInflater.inflate(R.layout.custom_info_window, null)
        val mInfoWindow : CustomInfoWindowModel = p0.tag as CustomInfoWindowModel

        val windowUserName = mInfoView.findViewById<TextView>(R.id.window_username)
        val windowUserPhoto = mInfoView.findViewById<ImageView>(R.id.window_userphoto)
        val windowCategory = mInfoView.findViewById<TextView>(R.id.window_category)
        val windowDesc = mInfoView.findViewById<TextView>(R.id.window_description)
        val windowImg = mInfoView.findViewById<ImageView>(R.id.window_image)

        windowUserName.text = mInfoWindow.userName

        Glide.with(mInfoView)
            .load(mInfoWindow.userPhoto)
            .placeholder(R.drawable.ic_launcher_background)
            .into(windowUserPhoto)
        windowCategory.text = mInfoWindow.category
        windowDesc.text = mInfoWindow.description
        Glide.with(mInfoView)
            .load(mInfoWindow.image)
            .placeholder(R.drawable.ic_launcher_background)
            .into(windowImg)

        return mInfoView
    }

    override fun getInfoWindow(p0: Marker): View? {
        return null
    }
}