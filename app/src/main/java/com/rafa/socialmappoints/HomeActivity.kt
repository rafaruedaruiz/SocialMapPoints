package com.rafa.socialmappoints

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_home.*
import androidx.appcompat.widget.SearchView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.ByteArrayOutputStream

enum class ProviderType{
    BASIC,
    GOOGLE
}

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    var marker: Marker? = null
    private var filter: String = ""

    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)


        // Cargar mapa
        createFragment()

        // Setup
        val bundle = intent.extras
        val email = bundle?.getString("email")
        val provider = bundle?.getString("provider")
        setup(email ?: "", provider ?: "")

        // Guardar datos login usuario
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        prefs.apply()

        //Recoger filtro
        val searchView: SearchView = findViewById(R.id.searchView) as SearchView
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                // usuario cambia texto y da enter
                return false
            }

            override fun onQueryTextChange(newText: String): Boolean {
                // usuario cambia texto
                filter = newText
                loadMarkers()
                return false
            }
        })

        // Imagen usuario abre menu desplegable
        userLayout.setOnClickListener {
            val popup = PopupMenu(this, userLayout)
            popup.menuInflater.inflate(R.menu.user_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.my_profile -> {
                        val myProfileIntent = Intent(this, InfoUserActivity::class.java)
                        myProfileIntent.putExtra("userId", FirebaseAuth.getInstance().currentUser?.uid)
                        startActivity(myProfileIntent)
                        true
                    }
                    R.id.my_social_points -> {
                        val myPointsIntent = Intent(this, MyPointsActivity::class.java)
                        startActivity(myPointsIntent)
                        true
                    }
                    R.id.my_social_events -> {
                        true
                    }
                    R.id.log_out -> {
                        val builder = AlertDialog.Builder(this@HomeActivity)
                        builder.setTitle("Cerrar sesión")
                        builder.setMessage("¿Está seguro de que quiere cerrar sesión?")
                        builder.setPositiveButton("Cerrar sesión") { _, _ ->
                            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
                            prefs.clear()
                            prefs.apply()

                            FirebaseAuth.getInstance().signOut()
                            val authIntent = Intent(this, AuthActivity::class.java)
                            startActivity(authIntent)
                        }
                        builder.setNegativeButton("Cancelar") { _, _ ->
                            // El usuario ha cancelado (no hacer nada)
                        }
                        builder.create().show()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

    }

    private fun createFragment(){
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun isPermissionsGranted() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED


    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (isPermissionsGranted()) {
            map.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
            val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
            toastView.findViewById<TextView>(R.id.toastMessage).text = "Ve a ajustes y acepta los permisos"
            toast.view = toastView
            toast.show()
        } else {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode){
            REQUEST_CODE_LOCATION -> if(grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                map.isMyLocationEnabled = true
            }else{

                val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
                val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
                toastView.findViewById<TextView>(R.id.toastMessage).text = "Para activar la localización ve a ajustes y acepta los permisos"
                toast.view = toastView
                toast.show()
            }
            else -> {}
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResumeFragments() {
        super.onResumeFragments()
        if (!::map.isInitialized) return
        if(!isPermissionsGranted()){
            map.isMyLocationEnabled = false

            val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
            val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
            toastView.findViewById<TextView>(R.id.toastMessage).text = "Para activar la localización ve a ajustes y acepta los permisos"
            toast.view = toastView
            toast.show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Habilitar ubicación del usuario
        enableMyLocation()

        // Zoom de la cámara del mapa
        val lat = intent.getDoubleExtra("lat", 0.0)     // Intento recoger los extras del intent
        val lng = intent.getDoubleExtra("lng", 0.0)

        if (lat != 0.0 && lng != 0.0) {   // En el caso de que se haya pasado extras por el intent, se hace zoom en esa location
            val location = LatLng(lat, lng)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(location, 14f)
            map.animateCamera(cameraUpdate)
        } else {   // En otro caso, se hace zoom en la ubicación del usuario
            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val latLng = location?.let { LatLng(it.latitude, location.longitude) }
            val cameraUpdate = latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 14f) }
            if (cameraUpdate != null) {
                map.animateCamera(cameraUpdate)
            }
        }



        // Map style sin los puntos de interes que trae google maps por defecto
        val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        if (!success) {
            Log.e("MapActivity", "Style parsing failed.")
        }

        // Punto temporal para ser añadido
        map.setOnMapClickListener { latLng ->
            val markerView = (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.marker_layout, null)
            val text = markerView.findViewById<TextView>(R.id.marker_title)
            val icon = markerView.findViewById<ImageView>(R.id.marker_icon)
            val cardView = markerView.findViewById<LinearLayout>(R.id.markerCardView)

            text.visibility = View.GONE
            icon.setImageResource(R.drawable.set_marker_icon)
            val bitmap1 = Bitmap.createScaledBitmap(viewToBitmap(cardView)!!, cardView.width, cardView.height, false)
            val smallMarkerIcon1 = BitmapDescriptorFactory.fromBitmap(bitmap1)

            marker?.remove()
            marker = map.addMarker(MarkerOptions().position(latLng))
            marker?.setIcon(smallMarkerIcon1)
            marker?.tag = "marker_set"
        }

        loadMarkers()

        map.setOnMarkerClickListener { clickedMarker ->
            if(clickedMarker.tag.toString() != "marker_set"){  // soluciona error de clickar sobre el marker_set
                val SocialPointInfoIntent = Intent(this, InfoSocialPointActivity::class.java)
                SocialPointInfoIntent.putExtra("socialPointId", clickedMarker.tag.toString())
                startActivity(SocialPointInfoIntent)
            }
            true
        }
    }

    private fun viewToBitmap(view: View): Bitmap?{
        view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.draw(canvas)
        return bitmap
    }

    private fun loadMarkers(){
        map.clear()

        // MARKERS CON TITULO
        val markerView = (getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater).inflate(R.layout.marker_layout, null)
        val text = markerView.findViewById<TextView>(R.id.marker_title)
        val icon = markerView.findViewById<ImageView>(R.id.marker_icon)
        val cardView = markerView.findViewById<LinearLayout>(R.id.markerCardView)


        // Cargar puntos del mapa
        val databaseReference = FirebaseDatabase.getInstance().getReference("social_points")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach { childSnapshot ->
                    val socialPoint = childSnapshot.getValue(SocialPoint::class.java)
                    if (socialPoint != null) {
                        if(filter.length == 0){  // si no tiene filtro carga todo
                            text.text = socialPoint.title
                            // icon = (tipo de icono a mostrar) si quiero cambiar colores entre SocialPoint - Event
                            val bitmap1 = Bitmap.createScaledBitmap(viewToBitmap(cardView)!!, cardView.width, cardView.height, false)
                            val smallMarkerIcon1 = BitmapDescriptorFactory.fromBitmap(bitmap1)


                            val position = LatLng(socialPoint.latitude, socialPoint.longitude)
                            val marker = map.addMarker(MarkerOptions().position(position).title(socialPoint.title))
                            marker?.tag = childSnapshot.key
                            marker?.setIcon(smallMarkerIcon1)
                        }else{  // si hay algo en el searchView, se filtra
                            // Se divide el filtro en palabras
                            val filterWords = filter.split("\\s+".toRegex())
                            val found = filterWords.any { word ->
                                socialPoint.title.contains(word, ignoreCase = true) || socialPoint.description.contains(word, ignoreCase = true)
                            }
                            if (found) {
                                text.text = socialPoint.title
                                // icon = (tipo de icono a mostrar) si quiero cambiar colores entre SocialPoint - Event
                                val bitmap1 = Bitmap.createScaledBitmap(viewToBitmap(cardView)!!, cardView.width, cardView.height, false)
                                val smallMarkerIcon1 = BitmapDescriptorFactory.fromBitmap(bitmap1)


                                val position = LatLng(socialPoint.latitude, socialPoint.longitude)
                                val marker = map.addMarker(MarkerOptions().position(position).title(socialPoint.title))
                                marker?.tag = childSnapshot.key
                                marker?.setIcon(smallMarkerIcon1)
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Controlar error
            }
        })
    }
    private fun setup(email: String, provider: String){
        title = "Inicio"

        val user = FirebaseAuth.getInstance().currentUser
        if(user != null){
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(user.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if(dataSnapshot.hasChild("username")){
                        // Cargamos los datos del usuario para el Home
                        userTextView.text = dataSnapshot.child("username").value.toString()
                        val storage = Firebase.storage
                        val storageRef = storage.reference
                        val userId = FirebaseAuth.getInstance().currentUser?.uid
                        val fileRef = storageRef.child("profile_photos/$userId/user_photo.jpg")
                        fileRef.metadata.addOnSuccessListener { metadata ->
                            if (metadata.sizeBytes == 0L) {
                                // El archivo no existe en el Storage.
                            } else {
                                // El archivo ya existe en el Storage.
                                fileRef.downloadUrl.addOnSuccessListener { uri ->
                                    // Cargar la imagen con Glide
                                    Glide.with(this@HomeActivity).load(uri).transform(CircleCrop()).into(userPhoto)
                                }.addOnFailureListener { exception ->
                                    // Manejar el error
                                }
                            }
                        }
                    }else{
                        userTextView.text = user.email.toString().split("@")[0]
                        Glide.with(this@HomeActivity).load(R.drawable.default_user_photo).transform(CircleCrop()).into(userPhoto)
                    }
                }
                override fun onCancelled(databaseError: DatabaseError) {
                }
            })
        }

        addButton.setOnClickListener{
            if (marker != null) {
                val addPointIntent = Intent(this, AddPointActivity::class.java)
                addPointIntent.putExtra("latitude", marker!!.position.latitude)
                addPointIntent.putExtra("longitude", marker!!.position.longitude)
                startActivity(addPointIntent)
            } else {
                val toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
                val toastView = LayoutInflater.from(this).inflate(R.layout.toast_layout, null)
                toastView.findViewById<TextView>(R.id.toastMessage).text = "Por favor, primero selecciona el punto en el mapa"
                toast.view = toastView
                toast.show()
            }
        }
    }
}