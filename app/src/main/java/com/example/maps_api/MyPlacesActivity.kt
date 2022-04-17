package com.example.maps_api

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.maps_api.adapters.MyPlacesAdapter
import com.example.maps_api.models.MyPlacesModel
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.ArrayList

class MyPlacesActivity : AppCompatActivity() {

    private val db = Firebase.firestore
    private var mAuth = FirebaseAuth.getInstance()
    private val userID = mAuth.currentUser!!.uid
    private val userReference = db.document("users/$userID")

    private lateinit var myPlacesAdapter: MyPlacesAdapter
    private lateinit var myPlacesView: RecyclerView
    private var myPlacesList: MutableList<MyPlacesModel> = ArrayList()

    lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_places)
        this.title = "My places"
        navigationMenu()
        showMyPlaces()
    }

    private fun showMyPlaces() {
        userReference.collection("myPoi").addSnapshotListener { value, e ->
            if (e != null) {
                Log.d("tag", "Listen failed.", e)
            } else {
                for (document in value!!) {
                    val poiCategory = document.getString("category").toString()
                    val poiDesc = document.getString("description").toString()
                    val poiImage = document.getString("image").toString()

                    val list =  MyPlacesModel(poiCategory, poiDesc, poiImage, document.id)
                    myPlacesList.add(list)
                    initRecyclerView()
                    addData(myPlacesList)
                }
            }
        }
    }

    private fun initRecyclerView() {
        myPlacesView = findViewById<RecyclerView>(R.id.my_places_recycler_view)
        myPlacesView.apply {
            layoutManager = LinearLayoutManager(this@MyPlacesActivity)
            myPlacesAdapter = MyPlacesAdapter()
            adapter = myPlacesAdapter
        }
    }

    // Adds the data to recycler view
    private fun addData(list: MutableList<MyPlacesModel>) {
        myPlacesAdapter.submitList(list)
    }

    private fun navigationMenu() {
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawerLayout)
        val navigationView = findViewById<NavigationView>(R.id.nav_view)

        toggle = ActionBarDrawerToggle(this, drawerLayout, R.string.open, R.string.close)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        navigationView.setNavigationItemSelectedListener {
            when(it.itemId) {
                R.id.home_page ->  {
                    val intent = Intent(this, MapsActivity::class.java)
                    startActivity(intent)
                }
                R.id.my_places_page -> {}
                R.id.account_page -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawer(navigationView)
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (toggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}