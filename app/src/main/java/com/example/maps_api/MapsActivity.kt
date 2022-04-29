package com.example.maps_api

import android.Manifest
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.maps_api.adapters.CustomInfoWindowAdapter
import com.example.maps_api.models.CustomInfoWindowModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.PolyUtil
import com.koushikdutta.ion.Ion
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import kotlin.collections.ArrayList


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var toggle: ActionBarDrawerToggle
    private var mMap: GoogleMap? = null
    private var locationPermissionGranted = false
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private var cameraPosition: CameraPosition? = null
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)

    private lateinit var dialog: Dialog
    private val db = Firebase.firestore
    private var mAuth = FirebaseAuth.getInstance()
    private val userID = mAuth.currentUser!!.uid
    private val userReference = db.document("users/$userID")

    private lateinit var userName: String
    private lateinit var userPhoto: String
    private var imageUrl = ""
    private var poiLong = 1.1
    private var poiLat = 1.1

    lateinit var observer : MyLifecycleObserver
    private var notificationHelper: NotificationHelper? = null
    private lateinit var currentLatLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps) 
        this.title = "Home"

        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        navigationMenu()
        getUserInfo()

        observer = MyLifecycleObserver(activityResultRegistry)
        lifecycle.addObserver(observer)
        displayPois()

        notificationHelper = NotificationHelper(this)
    }

    private fun getUserInfo() {
        // get user's name and profile photo
        userReference.get()
            .addOnSuccessListener { document ->
                userName = document.getString("firstName").toString()
                if ( document.contains("profilePhoto")) {
                    userPhoto = document.getString("profilePhoto").toString()
                } else {
                    userPhoto = ""
                }
            } .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }
    }

    private fun displayPois() {
        val poiRef = db.collection("Points of interest")

        poiRef.addSnapshotListener{ value, e ->
            if( e != null) {
                Log.d("tag", "Listen failed.", e)
            } else {
                for (document in value!!) {
                    val poiCategory = document.getString("category").toString()
                    val poiDesc = document.getString("description").toString()
                    val poiImage = document.getString("image").toString()
                    val poiLatitude = document.getDouble("latitude")!!.toDouble()
                    val poiLongitude = document.getDouble("longitude")!!.toDouble()
                    val poiUserName = document.getString("userName").toString()
                    val poiUserPhoto = document.getString("userPhoto").toString()
                    val poiLoc = LatLng(poiLatitude, poiLongitude)

                    val info = CustomInfoWindowModel(poiUserName, poiUserPhoto, poiCategory, poiDesc, poiImage)

                    val markers = mMap?.addMarker(
                        MarkerOptions()
                            .position(poiLoc)
                    )!!
                    markers.tag = info

                    when (poiCategory) {
                        "Activities" -> markers.setIcon(bitmapDescriptorFromVector(this, R.drawable.activities_marker))
                        "Landmarks" -> markers.setIcon(bitmapDescriptorFromVector(this, R.drawable.landmarks_marker))
                        "Hidden Gems" -> markers.setIcon(bitmapDescriptorFromVector(this, R.drawable.gem_marker))
                        "Nature" -> markers.setIcon(bitmapDescriptorFromVector(this, R.drawable.nature_marker))
                        "Beaches" -> markers.setIcon(bitmapDescriptorFromVector(this, R.drawable.beaches_marker))
                    }
                }
            }
        }
    }

    private fun calculateDirections(marker: Marker) {
        var originLat: String = defaultLocation.latitude.toString()
        var originLong: String = defaultLocation.longitude.toString()

        if (currentLatLng != null) {
             originLat = currentLatLng.latitude.toString()
             originLong = currentLatLng.longitude.toString()
        }
        val destinationLat = marker.position.latitude
        val destinationLong = marker.position.longitude
        val apiKey = "AIzaSyAf5Axwtr0RXd1OFPZQh054c-6xUPr9UtM"

        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=$originLat,$originLong&destination=$destinationLat,$destinationLong&key=$apiKey"

        Ion.with(this)
            .load(url)
            .asString()
            .setCallback{_, result ->
                getJson(result)
            }
    }

    private fun getJson(result: String) {
        val jsonResult = JSONObject(result)
        val routes = jsonResult.getJSONArray("routes")
        val legs = routes.getJSONObject(0).getJSONArray("legs")
        val steps = legs.getJSONObject(0).getJSONArray("steps")
        val path: MutableList<List<LatLng>> = ArrayList()

        try {
            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }
            for (i in 0 until path.size) {
                this.mMap!!.addPolyline(PolylineOptions().addAll(path[i]).color(R.color.royal_blue))
            }
        }
        catch(e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun onMarkerClick() {
        mMap?.setOnMarkerClickListener {
            if (it.tag == "addMarker") {
                openPoiDialog()
                setDropdownMenu()
                loadImage()
                submitPoi()
                it.remove()
            }
            mMap?.setInfoWindowAdapter(CustomInfoWindowAdapter(this))
            it.showInfoWindow()
            mMap?.setOnInfoWindowClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle("go to location?")
                    .setNegativeButton(resources.getString(R.string.decline)) { _, _ ->
                        closeOptionsMenu()
                    }
                    .setPositiveButton(resources.getString(R.string.accept)) { _, _ ->
                        calculateDirections(it)
                    }
                    .show()
            }
            true
        }
    }

    private fun getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this.applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    private fun getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (locationPermissionGranted) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            val lat = lastKnownLocation!!.latitude
                            val long = lastKnownLocation!!.longitude
                            currentLatLng = LatLng(lat, long)
                            mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, DEFAULT_ZOOM.toFloat()))
                        }
                    } else {
                        Log.d("tag", "Current location is null. Using defaults.")
                        Log.e("tag", "Exception: %s", task.exception)
                        mMap?.moveCamera(CameraUpdateFactory
                            .newLatLngZoom(defaultLocation, DEFAULT_ZOOM.toFloat()))
                        mMap?.uiSettings?.isMyLocationButtonEnabled = false
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun updateLocationUI() {
        if (mMap == null) {
            return
        }
        try {
            if (locationPermissionGranted) {
                mMap?.isMyLocationEnabled = true
                mMap?.uiSettings?.isMyLocationButtonEnabled = true
            } else {
                mMap?.isMyLocationEnabled = false
                mMap?.uiSettings?.isMyLocationButtonEnabled = false
                lastKnownLocation = null
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        locationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true
                }
            }
        }
        updateLocationUI()
    }

    override fun onMapReady(mMap: GoogleMap) {
        this.mMap = mMap
        setMapStyle(mMap)
        mMap.uiSettings.isZoomControlsEnabled = true
        getLocationPermission()
        updateLocationUI()
        getDeviceLocation()
        placeMarkerOnMap()
        onMarkerClick()
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            val success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    this,
                    R.raw.map_style
                )
            )
            if (!success) {
                Log.e("TAG", "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e("TAG", "Can't find style. Error: ", e)
        }
    }

    private fun placeMarkerOnMap() {
        mMap?.setOnMapLongClickListener {
            mMap?.addMarker(
                MarkerOptions()
                    .position(it)
                    .icon(bitmapDescriptorFromVector(this, R.drawable.add_location_ic))
            )?.tag = "addMarker"
            poiLong = it.longitude
            poiLat = it.latitude
        }
    }

    private fun openPoiDialog() {
        dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.add_place_dialog)
        dialog.show()
    }

    private fun setDropdownMenu() {
        val spinner = dialog.findViewById<Spinner>(R.id.spinner)
        val options = resources.getStringArray(R.array.categories_array)

        spinner.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, options)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {}

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun postNotification(id: Int, title: String) {
        var notificationBuilder: NotificationCompat.Builder? = null
        when (id) {
            notification_one-> notificationBuilder = notificationHelper!!.getNotification(
                title,
                "check it out"
            )
        }
        if (notificationBuilder != null) {
            notificationHelper!!.notify(id, notificationBuilder)
        }
    }

    private fun submitPoi() {
        val submitPoiBtn = dialog.findViewById<Button>(R.id.submit_poi_btn)
        val poiDescription = dialog.findViewById<EditText>(R.id.description_text)
        val category = dialog.findViewById<Spinner>(R.id.spinner)
        val selectImageBtn = dialog.findViewById<Button>(R.id.add_image_btn)

        selectImageBtn.setOnClickListener{
            observer.selectImage()
        }

        submitPoiBtn.setOnClickListener {
            when {
                TextUtils.isEmpty(poiDescription.text.toString()) -> {
                    Snackbar.make(it,
                        "Please enter description",
                        Snackbar.LENGTH_SHORT).show()
                }
                TextUtils.isEmpty(category.selectedItem.toString()) -> {
                    Snackbar.make(it,
                        "Please enter category",
                        Snackbar.LENGTH_SHORT).show()
                }
                imageUrl == "" -> {
                    Snackbar.make(it,
                        "Please add an image",
                        Snackbar.LENGTH_SHORT).show()
                }
                else -> {
                    addPoiToDatabase()
                    dialog.dismiss()
                    poiDescription.text.clear()
                }
            }
        }
    }

    private fun uploadPoiImage() {
        val filename = UUID.randomUUID().toString()
        val storageRef = FirebaseStorage.getInstance().getReference("poi-images/$filename")
        val selectedPhotoUri = observer.getSelectedImage()

        if (selectedPhotoUri != null) {
            Toast.makeText(this,R.string.wait, Toast.LENGTH_SHORT).show()
            val uploadTask = storageRef.putFile(selectedPhotoUri!!)
            uploadTask.continueWith {
                if (!it.isSuccessful) {
                    it.exception?.let { t ->
                        Toast.makeText(this,
                            R.string.select_image,
                            Toast.LENGTH_SHORT).show()
                        throw t
                    }
                }
                storageRef.downloadUrl.addOnCompleteListener {
                    if (it.isSuccessful) {
                        it.addOnSuccessListener { task ->
                            imageUrl = task.toString()
                            Log.d("image", imageUrl)
                            Toast.makeText(this,
                                R.string.image_uploaded,
                                Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        imageUrl = ""
                        Log.d("error", " error uploading image")
                    }
                }
            }
        } else {
            Toast.makeText(this,
                R.string.select_image,
                Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadImage() {
        val btn = dialog.findViewById<Button>(R.id.load_image)
        btn.setOnClickListener{
            uploadPoiImage()
        }
    }

    private fun addPoiToDatabase() {
        val poiRef = db.collection("Points of interest")
        val poiDescription = dialog.findViewById<EditText>(R.id.description_text).text.toString()
        val category = dialog.findViewById<Spinner>(R.id.spinner).selectedItem.toString()

        val pointOfInterest = hashMapOf(
            "userName" to userName,
            "userPhoto" to userPhoto,
            "description" to poiDescription,
            "category" to category,
            "image" to imageUrl,
            "latitude" to poiLat,
            "longitude" to poiLong
        )

        // Add document to poi's collection
        val myPoi = poiRef.document()
        myPoi.set(pointOfInterest)
            .addOnSuccessListener {
                postNotification(notification_one, "New place added")
                Log.d("document", "added successfully")
            }
            .addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }

        // Add document to user's collection
        userReference.collection("myPoi")
            .document(myPoi.id)
            .set(pointOfInterest)
            .addOnSuccessListener {
                Log.d("review", "submitted successfully")
            }
            .addOnFailureListener {
                    e ->
                Log.w(ContentValues.TAG, "Error adding document", e)
            }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    companion object {
        private const val DEFAULT_ZOOM = 15
        private const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
        private const val KEY_CAMERA_POSITION = "camera_position"
        private const val KEY_LOCATION = "location"
        private const val notification_one = 101
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
                R.id.home_page ->  {}
                R.id.my_places_page -> {
                    val intent = Intent(this, MyPlacesActivity::class.java)
                    startActivity(intent)
                }
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