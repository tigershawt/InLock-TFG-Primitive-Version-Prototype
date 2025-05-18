package com.jetbrains.kmpapp.screens.qrcode

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference

object MainActivityHolder {
    private var activityRef: WeakReference<Activity>? = null
    private var permissionCallbacks = mutableMapOf<Int, Pair<() -> Unit, () -> Unit>>()

    fun setActivity(activity: Activity) {
        activityRef = WeakReference(activity)
    }

    fun getActivity(): Activity {
        return activityRef?.get() ?: throw IllegalStateException("Activity not available")
    }

    fun requestPermission(
        permission: String,
        requestCode: Int,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ): Boolean {
        val activity = getActivity()

        if (ContextCompat.checkSelfPermission(activity, permission) ==
            PackageManager.PERMISSION_GRANTED) {
            onGranted()
            return true
        }

        permissionCallbacks[requestCode] = Pair(onGranted, onDenied)

        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            requestCode
        )

        return false
    }

    fun requestCameraPermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ): Boolean {
        return requestPermission(
            Manifest.permission.CAMERA,
            CAMERA_PERMISSION_REQUEST_CODE,
            onGranted,
            onDenied
        )
    }

    fun handlePermissionResult(requestCode: Int, grantResults: IntArray) {
        val callbacks = permissionCallbacks[requestCode] ?: return

        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            callbacks.first.invoke()
        } else {
            callbacks.second.invoke()
        }

        permissionCallbacks.remove(requestCode)
    }

    const val CAMERA_PERMISSION_REQUEST_CODE = 100
}