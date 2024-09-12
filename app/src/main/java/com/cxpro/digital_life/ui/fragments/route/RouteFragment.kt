package com.cxpro.digital_life.ui.fragments.route

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.cxpro.digital_life.R
import com.cxpro.digital_life.databinding.FragmentRouteBinding
import com.cxpro.digital_life.utils.PermissionManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.IMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


const val USER_AGENT = "DigitalLifeAgent/1.0"

var lastRoadFetchingJob: Job? = null
var lastEndMarker: Marker? = null
var lastEndPoint: GeoPoint? = null
var lastStartPoint: GeoPoint? = null
var beforeLastEndPoint: GeoPoint? = null
var lastRoute: Polyline? = null

class RouteFragment : Fragment() {

    private var _binding: FragmentRouteBinding? = null
    private val binding get() = _binding!!
    private lateinit var factory: RouteViewModel.Factory
    private val viewModel by viewModels<RouteViewModel>()

    @SuppressLint("InflateParams", "SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRouteBinding.inflate(inflater, container, false)
        val root: View = binding.root
        Configuration.getInstance().userAgentValue = USER_AGENT
        val activity=requireActivity()
        if(!PermissionManager.checkOrGetLocationPermission(activity)){
            val popupView: View = LayoutInflater.from(activity).inflate(R.layout.location_access_pop_window, null)
            val popupWindow = PopupWindow(
                popupView,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT
            )
            (popupView.findViewById<View>(R.id.button) as Button).setOnClickListener{
                popupWindow.dismiss()
            }
            popupWindow.showAsDropDown(popupView, 0, 0)
            activity.findNavController(R.id.nav_host_fragment_activity_main)
                .navigate(R.id.navigation_home)
        }
        val mLocationOverlay =
            object : MyLocationNewOverlay(GpsMyLocationProvider(context), binding.map) {
                override fun onLocationChanged(location: Location?, source: IMyLocationProvider?) {
                    super.onLocationChanged(location, source)
                    location?.let {
                        val startPoint = GeoPoint(it)
                        if(lastEndPoint != null && startPoint != lastStartPoint)
                            fetchBestRoute(context, binding.map, startPoint, lastEndPoint)
                        beforeLastEndPoint = lastEndPoint
                        lastStartPoint = startPoint
                    }
                }
            }
        mLocationOverlay.enableFollowLocation()

        CoroutineScope(Dispatchers.IO).launch {
            if(Looper.myLooper() == null)Looper.prepare()
            mLocationOverlay.enableMyLocation()
        }
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        fetchCurrentLocationAndPerform(activity).addOnSuccessListener {
            val startPoint=GeoPoint(it)
            val mapEventsOverlay = MapEventsOverlay(MapEventsReceiverImpl(context, startPoint, binding.map))
            binding.map.overlays.add(mapEventsOverlay)
            binding.map.setExpectedCenter(startPoint)
        }
        binding.map.setMultiTouchControls(true)
        binding.map.setZoomLevel(20.0)



        binding.map.overlays.add(mLocationOverlay)
        return root
    }



    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this)
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        binding.map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this)
        //Configuration.getInstance().save(this, prefs)
        binding.map.onPause() //needed for compass, my location overlays, v6.0.0 and up
    }
}


fun fetchingRouteMessage(context: Context?): Job {
    return CoroutineScope(Dispatchers.IO).launch {
        if(Looper.myLooper() == null)Looper.prepare()
        Toast.makeText(context, R.string.loading_route_msg, Toast.LENGTH_LONG).show()
    }
}

@SuppressLint("MissingPermission")
fun fetchCurrentLocationAndPerform(activity: Activity): Task<Location> {
    return (
            LocationServices
                .getFusedLocationProviderClient(activity)
                .getCurrentLocation(
                    CurrentLocationRequest.Builder()
                        .setDurationMillis(30000).build()
                    ,null
                )
            )
}

class MapEventsReceiverImpl(private val context: Context?, private val startPoint: GeoPoint?, private val map: MapView) : MapEventsReceiver {

    override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
        return true
    }

    override fun longPressHelper(endPoint: GeoPoint?): Boolean {
        if (lastEndMarker != null) map.overlays.remove(lastEndMarker)
        lastEndPoint = endPoint
        val endMarker = Marker(map)
        endMarker.position = endPoint
        endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        map.overlays.add(endMarker)
        lastEndMarker = endMarker
        fetchBestRoute(context, map, startPoint, endPoint)
        return false
    }
}

fun fetchBestRoute(context: Context?, map: MapView, startPoint: GeoPoint?, endPoint: GeoPoint?){

    lastRoadFetchingJob?.cancel()
    lastRoadFetchingJob = (CoroutineScope(Dispatchers.IO).launch {
        val roadManager = OSRMRoadManager(context, USER_AGENT)
        roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR)
        fetchingRouteMessage(context)
        var road=roadManager.getRoad(arrayListOf(startPoint,endPoint))

        while(road.mStatus!=Road.STATUS_OK)
            road = roadManager.getRoad(arrayListOf(startPoint, endPoint))
        if (lastRoute != null) map.overlays.remove(lastRoute)
        lastRoute = RoadManager.buildRoadOverlay(road)
        map.overlays.add(lastRoute)
    })
}