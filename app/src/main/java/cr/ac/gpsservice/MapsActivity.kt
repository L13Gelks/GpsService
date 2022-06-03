package cr.ac.gpsservice

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.PolyUtil
import com.google.maps.android.data.geojson.GeoJsonPolygon
import cr.ac.gpsservice.databinding.ActivityMapsBinding
import cr.ac.gpsservice.db.LocationDatabase
import cr.ac.gpsservice.entity.Location
import cr.ac.gpsservice.service.GpsService
import org.json.JSONObject

private lateinit var mMap: GoogleMap
private lateinit var locationDatabase: LocationDatabase
private lateinit var layer : GeoJsonLayer


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private val SOLICITAR_GPS = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationDatabase=LocationDatabase.getInstance(this)
        validaPermisos()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        iniciaServicio()
        definePoligono(mMap)
        recuperarPuntos(mMap)
    }

    private fun recuperarPuntos(googleMap:GoogleMap){
        mMap = googleMap

        for(location in locationDatabase.locationDao.query()){
            val costaRica = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(costaRica).title("Marker in Costa Rica"))
        }

    }

    fun definePoligono(googleMap: GoogleMap){
        val geoJsonData= JSONObject("{\n" +
                "  \"type\": \"FeatureCollection\",\n" +
                "  \"features\": [\n" +
                "    {\n" +
                "      \"type\": \"Feature\",\n" +
                "      \"properties\": {},\n" +
                "      \"geometry\": {\n" +
                "        \"type\": \"Polygon\",\n" +
                "        \"coordinates\": [\n" +
                "          [\n" +
                "            [\n" +
                "              -117.0703125,\n" +
                "              20.632784250388028\n" +
                "            ],\n" +
                "            [\n" +
                "              -114.9609375,\n" +
                "              -1.0546279422758742\n" +
                "            ],\n" +
                "            [\n" +
                "              -80.5078125,\n" +
                "              -22.91792293614603\n" +
                "            ],\n" +
                "            [\n" +
                "              -51.328125,\n" +
                "              -5.266007882805485\n" +
                "            ],\n" +
                "            [\n" +
                "              -48.515625,\n" +
                "              27.059125784374068\n" +
                "            ],\n" +
                "            [\n" +
                "              -85.078125,\n" +
                "              37.71859032558816\n" +
                "            ],\n" +
                "            [\n" +
                "              -117.0703125,\n" +
                "              20.632784250388028\n" +
                "            ]\n" +
                "          ]\n" +
                "        ]\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}")
        layer = GeoJsonLayer(googleMap, geoJsonData)
        layer.addLayerToMap()
    }

    fun iniciaServicio(){
        val filter= IntentFilter()
        filter.addAction(GpsService.GPS)
        val rcv = ProgressReceiver()
        registerReceiver(rcv,filter)
        startService(Intent(this,GpsService::class.java))
    }

    fun validaPermisos(){
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                SOLICITAR_GPS
            )
        }
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            SOLICITAR_GPS -> {
                if ( grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.exit(1)
                }
            }
        }
    }

    class ProgressReceiver:BroadcastReceiver() {
        fun getPolygon(layer: GeoJsonLayer): GeoJsonPolygon? {
            for (feature in layer.features) {
                return feature.geometry as GeoJsonPolygon
            }
            return null
        }

        override fun onReceive(p0: Context, p1: Intent) {
            if (p1.action == GpsService.GPS) {
                val localizacion: Location = p1.getSerializableExtra("localizacion") as Location
                val punto = LatLng(localizacion.latitude, localizacion.longitude)
                mMap.addMarker(MarkerOptions().position(punto).title("Marker in Costa Rica"))

                if (PolyUtil.containsLocation(
                        localizacion.latitude,
                        localizacion.longitude,
                        getPolygon(layer)!!.outerBoundaryCoordinates, false)) {
                            Toast.makeText(p0,"adentro",Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(p0,"fuera",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}