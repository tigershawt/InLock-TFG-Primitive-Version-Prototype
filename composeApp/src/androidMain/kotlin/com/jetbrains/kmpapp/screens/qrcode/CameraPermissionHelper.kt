package com.jetbrains.kmpapp.screens.qrcode

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class CameraPermissionHelper(private val activity: Activity) {
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private var onPermissionGranted: (() -> Unit)? = null
    private var onPermissionDenied: (() -> Unit)? = null

    fun requestCameraPermission(
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        this.onPermissionGranted = onGranted
        this.onPermissionDenied = onDenied

        if (hasCameraPermission(activity)) {
            onGranted()
            return
        }

        try {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                if (hasCameraPermission(activity)) {
                    onPermissionGranted?.invoke()
                } else {
                    onPermissionDenied?.invoke()
                }
            }, 2000)
        } catch (e: Exception) {
            onDenied()
        }
    }

    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun handlePermissionResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted?.invoke()
            } else {
                onPermissionDenied?.invoke()
            }
        }
    }
}