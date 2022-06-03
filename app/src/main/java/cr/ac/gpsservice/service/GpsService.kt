package cr.ac.gpsservice.service

import android.annotation.SuppressLint
import android.app.IntentService
import android.content.Intent
import android.os.Looper
import com.google.android.gms.location.*
import cr.ac.gpsservice.db.LocationDatabase
import cr.ac.gpsservice.entity.Location

class GpsService : IntentService("GpsService") {
    lateinit var locationCallback: LocationCallback
    lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationDatabase: LocationDatabase
    private lateinit var locationRequest : LocationRequest

    companion object{
        var GPS= "cr.ac.GpsService.GPS_EVENT"
    }

    override fun onHandleIntent(intent: Intent?) {
        locationDatabase = LocationDatabase.getInstance(this)
        getLocation()
    }

    @SuppressLint("MissingPermission")
    fun getLocation(){

        fusedLocationClient= LocationServices.getFusedLocationProviderClient(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 500
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {

                for (location in locationResult.locations) {
                    val localizacion= Location(null,location.latitude,location.longitude)
                    val bcIntent=Intent()
                    bcIntent.action=GPS
                    bcIntent.putExtra("localizacion", localizacion)
                    sendBroadcast(bcIntent)
                    locationDatabase.locationDao.insert(Location(null, localizacion.latitude, localizacion.longitude))
                    LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
                }
            }
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null
        )
        Looper.loop()
    }

}
