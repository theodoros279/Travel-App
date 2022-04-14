package com.example.maps_api.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.maps_api.MapsActivity
import com.example.maps_api.R
import com.example.maps_api.models.MyPlacesModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MyPlacesAdapter() : RecyclerView.Adapter<RecyclerView.ViewHolder>(){
    private var items: MutableList<MyPlacesModel> = ArrayList()
    private val db = Firebase.firestore
    private var mAuth = FirebaseAuth.getInstance()
    private val userID = mAuth.currentUser!!.uid
    private val userReference = db.document("users/$userID")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.recycler_my_places, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val info = items[position]

        when(holder){
            is MyPlacesAdapter.ViewHolder -> {
                holder.bind(info)
                holder.itemView.setOnClickListener{
                    val activity = holder.itemView.context as Activity
                    val intent = Intent(activity, MapsActivity::class.java)
                    activity.startActivity(intent)
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitList(reviewsList: MutableList<MyPlacesModel>) {
        items = reviewsList
    }

    inner class ViewHolder (itemView: View): RecyclerView.ViewHolder(itemView) {
        private val category = itemView.findViewById<TextView>(R.id.display_category)
        private val description = itemView.findViewById<TextView>(R.id.display_description)
        private val image = itemView.findViewById<ImageView>(R.id.display_poi_image)
        private val deleteButton = itemView.findViewById<ImageButton>(R.id.delete_btn)

        fun bind(modelItem: MyPlacesModel) {
            category.text = modelItem.category
            description.text = modelItem.description

            Glide.with(itemView)
                .load(modelItem.image)
                .placeholder(R.drawable.ic_launcher_background)
                .into(image)

            deleteButton.setOnClickListener{
                val activity = itemView.context as Activity
                userReference.collection("myPoi").document(modelItem.documentId).delete()
                    .addOnCompleteListener {
                        if (it.isSuccessful) {
                            activity.recreate()
                            Toast.makeText(activity, "item deleted", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(activity, "error", Toast.LENGTH_SHORT).show()
                        }
                    }
                db.collection("Points of interest").document(modelItem.documentId).delete()
            }
        }
    }
}