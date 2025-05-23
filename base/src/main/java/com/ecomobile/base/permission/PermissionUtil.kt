package com.ecomobile.base.permission

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import com.ecomobile.base.storage_data.AppStates

object PermissionUtil {

    val imagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    val selectedImagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    } else {
        ""
    }

    fun hasPermission(activity: Activity, permission: String): Boolean {
        return activity.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    fun neverAskAgainSelected(activity: Activity?, permission: String): Boolean {
        val prevShouldShowStatus: Boolean = AppStates.getBoolean(permission)
        val currShouldShowStatus = activity?.shouldShowRequestPermissionRationale(permission)
        return prevShouldShowStatus != currShouldShowStatus
    }

    fun hasPhotoPermission(activity: Activity): Boolean {
        return hasPermission(activity, imagePermission)
                || hasPermission(activity, selectedImagePermission)
    }
}