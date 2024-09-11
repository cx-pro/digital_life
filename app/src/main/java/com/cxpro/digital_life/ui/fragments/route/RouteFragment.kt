package com.cxpro.digital_life.ui.fragments.route

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.PopupWindow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.findNavController
import com.cxpro.digital_life.R
import com.cxpro.digital_life.databinding.FragmentRouteBinding
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay


class RouteFragment : Fragment() {

    private var _binding: FragmentRouteBinding? = null
    private val binding get() = _binding!!
    private lateinit var factory: RouteViewModel.Factory
    private val viewModel by viewModels<RouteViewModel>()
    private lateinit var map: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    @SuppressLint("InflateParams", "SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted){
                val popupView: View =
                    LayoutInflater.from(activity)
                        .inflate(R.layout.location_access_pop_window, null)
                val popupWindow = PopupWindow(
                    popupView,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
                (popupView.findViewById<View>(R.id.button) as Button).setOnClickListener {
                    popupWindow.dismiss()
                }
                popupWindow.showAsDropDown(popupView, 0, 0)
                activity?.findNavController(R.id.nav_host_fragment_activity_main)
                    ?.navigate(R.id.navigation_home)
            }
        }

        permissionLauncher.launch(ACCESS_FINE_LOCATION)


        _binding = FragmentRouteBinding.inflate(inflater, container, false)
        val root: View = binding.root
        map = binding.map
        Configuration.getInstance().userAgentValue = "Digital Life Agent"
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        val mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), map)
        mLocationOverlay.enableMyLocation()
        map.overlays.add(mLocationOverlay)
        map.setZoomLevel(9.5)
        fusedLocationClient = activity?.let { LocationServices.getFusedLocationProviderClient(it) }!!
        if(activity?.checkSelfPermission(ACCESS_FINE_LOCATION)!=PERMISSION_GRANTED){
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
            activity?.findNavController(R.id.nav_host_fragment_activity_main)
                ?.navigate(R.id.navigation_home)
        }

        fusedLocationClient.getCurrentLocation(
            CurrentLocationRequest.Builder().setDurationMillis(30000).build(),
            null
        )
            .addOnSuccessListener { location : Location? ->
                map.setExpectedCenter(GeoPoint(location))
            }
        return root
    }
    override fun onResume() {
        super.onResume()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume() //needed for compass, my location overlays, v6.0.0 and up
    }

    override fun onPause() {
        super.onPause()
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause() //needed for compass, my location overlays, v6.0.0 and up
    }
}