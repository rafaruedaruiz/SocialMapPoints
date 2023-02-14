package com.rafa.socialmappoints

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_home.*

enum class ProviderType{
    BASIC,
    GOOGLE
}

class HomeActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    var marker: Marker? = null

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
            Toast.makeText(this, "Ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Para activar la localización ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
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
            Toast.makeText(this, "Para activar la localización ve a ajustes y acepta los permisos", Toast.LENGTH_SHORT).show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Habilitar ubicación del usuario
        enableMyLocation()

        // Zoom en la ubicación del usuario
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        val latLng = location?.let { LatLng(it.latitude, location.longitude) }
        val cameraUpdate = latLng?.let { CameraUpdateFactory.newLatLngZoom(it, 15f) }
        if (cameraUpdate != null) {
            map.animateCamera(cameraUpdate)
        }


        // Map style sin los puntos de interes que trae google maps por defecto
        val success = googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style))
        if (!success) {
            Log.e("MapActivity", "Style parsing failed.")
        }

        // Punto temporal para ser añadido
        map.setOnMapClickListener { latLng ->
            marker?.remove()
            marker = map.addMarker(MarkerOptions().position(latLng))
        }

        // Cargar puntos del mapa
        val databaseReference = FirebaseDatabase.getInstance().getReference("social_points")

        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataSnapshot.children.forEach { childSnapshot ->
                    val socialPoint = childSnapshot.getValue(SocialPoint::class.java)
                    if (socialPoint != null) {
                        val position = LatLng(socialPoint.latitude, socialPoint.longitude)
                        val marker = map.addMarker(MarkerOptions().position(position).title(socialPoint.title))
                        marker?.tag = childSnapshot.key
                        marker?.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.social_point_marker_icon))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Controlar error
            }
        })

        map.setOnMarkerClickListener { clickedMarker ->
            val SocialPointInfoIntent = Intent(this, SocialPointInfoActivity::class.java)
            SocialPointInfoIntent.putExtra("socialPointId", clickedMarker.tag.toString())
            startActivity(SocialPointInfoIntent)
            true
        }
    }

    private fun setup(email: String, provider: String){
        title = "Inicio"

        emailTextView.text = FirebaseAuth.getInstance().currentUser?.email.toString().split("@")[0]
        //providerTextView.text = provider

        logoutButton.setOnClickListener{
            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            FirebaseAuth.getInstance().signOut()
            val authIntent = Intent(this, AuthActivity::class.java)
            startActivity(authIntent)
        }

        addButton.setOnClickListener{
            if (marker != null) {
                val addPointIntent = Intent(this, AddPointActivity::class.java)
                addPointIntent.putExtra("latitude", marker!!.position.latitude)
                addPointIntent.putExtra("longitude", marker!!.position.longitude)
                startActivity(addPointIntent)
            } else {
                Toast.makeText(this, "Por favor, primero selecciona el punto en el mapa", Toast.LENGTH_SHORT).show()
            }
        }
    }
}