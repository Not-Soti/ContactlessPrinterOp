package com.example.movil

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Class made to centralise permission requests
 */
class PermissionHelper(
    var activity : Activity,
    var permission: String,
    var permissionCode : Int,
    var explanationTitle : String,
    var explanationMessage : String
) {

    val tag = "---PermissionHelper---"

    fun checkAndAskForPermission(){
        if(ContextCompat.checkSelfPermission(activity, permission)
            != PackageManager.PERMISSION_GRANTED)
        {
           //Check if explanation is required
            if(ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)){
                showExplanation(explanationTitle, explanationMessage, permission, permissionCode)
            } else{
                requestPermission(permission, permissionCode)
            }
            //If this is reached, permission is not granted
            Log.d(tag, "Permission $permission is not granted")
            return
        }
    }

    /**
     * Funcion utilizada para mostrar la explicacion de por que se solicitan
     * los permisos dados
     */
    private fun showExplanation(
        title: String,
        message: String,
        permissionName: String,
        permissionCode: Int
    ) {
        var builder: AlertDialog.Builder = AlertDialog.Builder(activity)
        builder.setTitle(title).setMessage(message)
            .setPositiveButton(android.R.string.ok, object : DialogInterface.OnClickListener {
                override fun onClick(p0: DialogInterface?, p1: Int) {
                    requestPermission(permissionName, permissionCode)
                }
            })
        builder.create().show()
    }

    /**
     * Funcion que solicita los permisos al usuario y reinicia la actividad
     */
    private fun requestPermission(name: String, code: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(name), code)
    }


}