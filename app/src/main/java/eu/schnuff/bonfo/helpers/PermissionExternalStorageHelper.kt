package eu.schnuff.bonfo.helpers

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private const val PERMISSION_EXTERNAL_STORAGE = 1
private var onGrantedCallbacks = mutableListOf<() -> Unit>()

class RequestHandler : Activity(), ActivityCompat.OnRequestPermissionsResultCallback {
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray){
        when (requestCode) {
            PERMISSION_EXTERNAL_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    onGrantedResolver()
                }
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSION_EXTERNAL_STORAGE)
    }
}

fun permissionExternalStorageHelper (context: Context, onGranted: () -> Unit) {
    onGrantedCallbacks.add(onGranted)
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        context.startActivity(
                Intent(context, RequestHandler::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
        )
    } else {
        onGrantedResolver()
    }
}

private fun onGrantedResolver() {
    while (onGrantedCallbacks.isNotEmpty()) {
        onGrantedCallbacks.removeAt(0)()
    }
}