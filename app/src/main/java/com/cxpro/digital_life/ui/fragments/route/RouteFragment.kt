package com.cxpro.digital_life.ui.fragments.route

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupWindow
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import com.cxpro.digital_life.R
import com.cxpro.digital_life.databinding.FragmentRouteBinding
import com.cxpro.digital_life.utils.PermissionManager
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


const val USER_AGENT="DigitalLifeAgent/1.0"

class RouteFragment : Fragment() {

    private var _binding: FragmentRouteBinding? = null
    private val binding get() = _binding!!
    private lateinit var factory: RouteViewModel.Factory
    private val viewModel by viewModels<RouteViewModel>()
    private lateinit var map: MapView

    @SuppressLint("InflateParams", "SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentRouteBinding.inflate(inflater, container, false)
        val root: View = binding.root
        map = binding.map
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
        LocationServices.getFusedLocationProviderClient(activity).getCurrentLocation(
            CurrentLocationRequest.Builder()
                .setDurationMillis(30000).build(),
            null
        ).addOnSuccessListener {
            val startPoint = GeoPoint(23.716920, 120.528952)
            val endPoint = GeoPoint(23.692862, 120.538184)
            map.setExpectedCenter(startPoint)
            val startMarker = Marker(map)
            val endMarker = Marker(map)

            startMarker.position = startPoint
            endMarker.position = endPoint
            startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            endMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)

            map.overlays.add(startMarker)
            map.overlays.add(endMarker)
            CoroutineScope(Dispatchers.IO).launch {
                val roadManager = OSRMRoadManager(context, USER_AGENT)
                roadManager.setMean(OSRMRoadManager.MEAN_BY_CAR)

                fetchingRouteMessage(context)
                var road=roadManager.getRoad(arrayListOf(startPoint,endPoint))

                while(road.mStatus!=Road.STATUS_OK) {
                    fetchingRouteMessage(context)
                    road = roadManager.getRoad(arrayListOf(startPoint, endPoint))
                }
                map.overlays.add(RoadManager.buildRoadOverlay(road))
            }
        }

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.setZoomLevel(18.0)

        val mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
        mLocationOverlay.enableMyLocation()
        map.overlays.add(mLocationOverlay)

        return root
    }



    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this)
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this)
        //Configuration.getInstance().save(this, prefs)
        map.onPause() //needed for compass, my location overlays, v6.0.0 and up
    }
}
fun fetchingRouteMessage(context: Context?){
    CoroutineScope(Dispatchers.IO).launch {
        if(Looper.myLooper() == null)Looper.prepare()
        Toast.makeText(context, R.string.loading_route_msg, Toast.LENGTH_SHORT).show()
    }
}