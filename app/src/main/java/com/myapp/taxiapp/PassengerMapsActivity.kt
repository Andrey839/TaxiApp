package com.myapp.taxiapp

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQueryEventListener
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_passenger_maps.*
import java.util.*
import kotlin.collections.ArrayList

class PassengerMapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var mMap: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var locationRequest: LocationRequest? = null
    private lateinit var client: SettingsClient
    private lateinit var locationCallback: LocationCallback
    private var locationSettingRequest: LocationSettingsRequest? = null
    private var location: Location? = null

    private lateinit var driversGeoFire: DatabaseReference
    private var nearestTaxi: DatabaseReference? = null

    private var isLocationActive: Boolean = true

    lateinit var auth: FirebaseAuth
    private var currentUser: FirebaseUser? = null

    private companion object {
        const val REQUEST_CHECK_SETTING = 111
        const val REQUEST_LOCATION_PERMISSION = 222
    }

    private var searchRadius = 1.0

    var isDriveFound: Boolean = false
    var nearestDriver: String? = null

    private lateinit var driverMarker: Marker

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passenger_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        driversGeoFire = FirebaseDatabase.getInstance().reference.child("driversGeoFire")

        auth = FirebaseAuth.getInstance()
        currentUser = auth.currentUser

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        client = LocationServices.getSettingsClient(this)

        createLocationRequest()

        buildLocationCallBack()

        buildLocationSettingRequest()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Add a marker in driverLocation and move the camera
        if (location != null) {
            val passengerLocation = LatLng(location!!.latitude, location!!.longitude)
            mMap.addMarker(MarkerOptions().position(passengerLocation).title("Marker driver"))
            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation))
        }
    }

    private fun buildLocationSettingRequest() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)
        locationSettingRequest = builder.build()
    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                location = locationResult.lastLocation
                updateLocationUi()
            }
        }
    }

    fun updateLocationUi() {
        if (location != null) {
            val passengerLocation: LatLng = LatLng(location!!.latitude, location!!.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12F))
            mMap.addMarker(MarkerOptions().position(passengerLocation).title("Marker driver"))

            val passengerId = currentUser?.uid
            val passengersGeoFire =
                FirebaseDatabase.getInstance().reference.child("passengerGeoFire")
            val geoFire = GeoFire(passengersGeoFire)
            geoFire.setLocation(passengerId, GeoLocation(location!!.latitude, location!!.longitude))
            val passengers = FirebaseDatabase.getInstance().reference.child("passenger")
            passengers.setValue(true)
        }
    }

    fun createLocationRequest() {
        locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    private fun startLocation() {

        isLocationActive = true

        val task = client.checkLocationSettings(locationSettingRequest)
        task.addOnSuccessListener { locationSettingsResponse ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@addOnSuccessListener
            }
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.myLooper()
            )

            updateLocationUi()
        }
        task.addOnFailureListener { exception: Exception ->
            exception as ApiException
            when (exception.statusCode) {
                LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                    val resolvableApiException = exception as ResolvableApiException
                    resolvableApiException.startResolutionForResult(this, REQUEST_CHECK_SETTING)
                } catch (sendEx: IntentSender.SendIntentException) {
                    sendEx.printStackTrace()
                }
                LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                    Toast.makeText(
                        this,
                        "Adjust location settings in your device",
                        Toast.LENGTH_LONG
                    ).show()

                    isLocationActive = false
                }
            }
            updateLocationUi()
        }
    }


    private fun stopLocation() {
        if (!isLocationActive) return
        else fusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener(
            this
        ) { _ ->
            isLocationActive = false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTING) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.e("MainActivity", "User has agreed")
                    startLocation()

                }
                Activity.RESULT_CANCELED -> {
                    Log.e("MainActivity", "User has not agreed")
                    isLocationActive = false

                    updateLocationUi()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (isLocationActive && checkLocationPermission()) {
            startLocation()
        } else if (!checkLocationPermission()) {
            requestPermission()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocation()
    }

    private fun requestPermission() {
        val shouldProviderRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (shouldProviderRationale) {
            showSnackBar(
                "Location permission is needed for app functionality",
                "OK",
                View.OnClickListener { _: View? ->
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                })
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun showSnackBar(mainText: String, action: String, listener: View.OnClickListener) {
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE)
            .setAction(action, listener).show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isEmpty()) Log.e("onRequestPermissionsResult", "Request was canceled")
            else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationActive) startLocation()
                else showSnackBar("Turn on location on settings", "settings", View.OnClickListener {
                    val intent = Intent()
                    intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    val uri = Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)

                })

            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        val permission =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return permission == PackageManager.PERMISSION_GRANTED
    }

    fun singOut(view: View) {
        auth.signOut()
        singOutPassenger()
    }

    private fun singOutPassenger() {
        val passengerId = currentUser?.uid
        val passengers = FirebaseDatabase.getInstance().reference.child("passenger")
        val geoFire = GeoFire(passengers)
        geoFire.removeLocation(passengerId)

        val intent = Intent(this, ChooseModeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()

    }

    fun bookTaxi(view: View) {
        taxiButton.text = getString(R.string.gettingTaxi)

        gettingTaxi()
    }

    private fun gettingTaxi() {

        val geoFire = GeoFire(driversGeoFire)
        val geoQuery = geoFire.queryAtLocation(
            GeoLocation(location!!.latitude, location!!.longitude),
            searchRadius
        )
        geoQuery.removeAllListeners()

        geoQuery.addGeoQueryEventListener(object : GeoQueryEventListener {
            override fun onGeoQueryReady() {
                if (!isDriveFound) {
                    searchRadius++
                    gettingTaxi()
                }
            }

            override fun onKeyEntered(key: String?, location: GeoLocation?) {
                if (!isDriveFound) {
                    isDriveFound = true
                    nearestDriver = key

                    getNearestDriver()
                }
            }

            override fun onKeyMoved(key: String?, location: GeoLocation?) {
                TODO("Not yet implemented")
            }

            override fun onKeyExited(key: String?) {
                TODO("Not yet implemented")
            }

            override fun onGeoQueryError(error: DatabaseError?) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun getNearestDriver() {
        taxiButton.text = getString(R.string.gettingYourTaxi)

        nearestTaxi =
            FirebaseDatabase.getInstance().reference.child("driversGeoFire").child(nearestDriver!!)
                .child("l")

        nearestTaxi!!.addValueEventListener(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                TODO("Not yet implemented")
            }

            override fun onDataChange(p0: DataSnapshot) {
                var latitude: Double = 0.0
                var longitude: Double = 0.0
                if (p0.exists()) {
                    val driversLocationParameters: ArrayList<Objects> =
                        arrayListOf(p0.getValue() as Objects)

                    latitude = driversLocationParameters[0].toString().toDouble()
                    longitude = driversLocationParameters[1].toString().toDouble()
                }

                val driverLatLng = LatLng(latitude, longitude)
                if (driverMarker != null) {
                    driverMarker.remove()
                }
                val driveLocation = Location("")
                driveLocation.latitude = latitude
                driveLocation.longitude = longitude

                var distanceToDriver = driveLocation.distanceTo(location)
                taxiButton.text = getString(R.string.distance_to_driver) + distanceToDriver

                driverMarker = mMap.addMarker(
                    MarkerOptions().position(driverLatLng).title("Ваш водитель здесь")
                )
            }
        })

    }
}