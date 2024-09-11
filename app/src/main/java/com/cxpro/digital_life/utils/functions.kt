package com.cxpro.digital_life.utils

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.pm.PackageManager.PERMISSION_GRANTED
import androidx.core.content.ContextCompat.checkSelfPermission
import android.app.Activity
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import com.cxpro.digital_life.ui.fragments.route.RouteFragment

fun checkOrGetPermissions(context: Activity, permissions: Array<String>){
    permissions.forEachIndexed{
            index,permission ->
        when {
            checkSelfPermission(context, permission) == PERMISSION_GRANTED -> {
            }
            shouldShowRequestPermissionRationale(context, permission) -> {
            }
            else -> requestPermissions(context, arrayOf(permission), index)
        }
    }
}

fun checkOrGetLocationPermission(context: Activity): Boolean {
    val permission=ACCESS_FINE_LOCATION
    when {
        checkSelfPermission(context, permission) == PERMISSION_GRANTED -> {
            return true
        }
        shouldShowRequestPermissionRationale(context, permission) -> {
        }
        else -> requestPermissions(context, arrayOf(permission), 0)
    }
    return checkSelfPermission(context, permission) == PERMISSION_GRANTED
}