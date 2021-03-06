package com.example.movil.readQrActivity

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.movil.R
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.material.snackbar.Snackbar

class ReadQrActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView //View that tisplays what the camera is capturing
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var helpButton : ImageButton
    private lateinit var connectManuallyButton : Button
    private lateinit var rootLayout : View

    private val requestCameraPermissionCode = 1 //Code needed to ask for permissions
    private val accessFineLocationPermissionCode = 2

    private val detectorHeight = 640 //Dimensions of the barcode detector
    private val detectorWidth = 640
    private var qrCodeRead: Boolean = false //Checks if a QR code has been detected

    private val tag = "---ReadQrActivity---"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        rootLayout = findViewById(R.id.act_readQR_root)
        surfaceView = findViewById(R.id.act_readQR_cameraPreview)
        helpButton = findViewById(R.id.act_readQR_help_button)
        connectManuallyButton = findViewById(R.id.act_readQR_connectManuallyButton)
        barcodeDetector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build()
        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(detectorHeight, detectorWidth).setAutoFocusEnabled(true)
            .build()

        helpButton.setOnClickListener { //Creates alert dialog showing the help explanatios
            val builder: AlertDialog.Builder? = this@ReadQrActivity?.let { AlertDialog.Builder(it) }

            builder?.apply { setNeutralButton(R.string.accept) { dialog, _ -> dialog.dismiss() } }
            builder?.setMessage(this@ReadQrActivity.getString(R.string.Dialog_qr_scanner_help))?.setTitle(R.string.help)
            val dialog: AlertDialog? = builder?.create()
            dialog?.show()
        }//help button listener

        connectManuallyButton.setOnClickListener { startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) }

        //Starts the surface view
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                //When it's created, starts capturing from the camera
                startCamera(holder)
            }
            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}
            override fun surfaceDestroyed(p0: SurfaceHolder) {
                //Closes the camera when the surface is destroyed
                cameraSource.stop()
            }
        })

        //Inits the barcode processor
        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>?) {
                //If a code wasn't already detected, processes the detection
                if (!qrCodeRead) {
                    val qrCodes: SparseArray<Barcode> =
                        detections?.detectedItems ?: throw Exception("QR codes not found")

                    if (qrCodes.size() != 0) {
                        qrCodeRead = true //If a code is detected, stops trying to detect more

                        //The device vibrates if a code is detected
                        val vibrator: Vibrator =
                            applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if(Build.VERSION.SDK_INT >= 26){
                            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                        }else {
                            vibrator.vibrate(200)
                        }

                        //Reads the QR message
                        val infoWifi = qrCodes.valueAt(0).rawValue

                        //If the QR contains wifi info, tries to connect to it
                        if(infoWifi.startsWith("WIFI", true)) {
                            //QR code contains wifi configuration
                            connectToWifi(infoWifi)

                        }else{
                            //If the QR didn't contain a wifi configuration
                            //shows dialog and in order to continue reading
                            showWrongCodeDialog()
                        }
                    }
                }
            }
        }) //barcode setProcessor
    }

    /**
     * Method used to connect to the specified WiFi on Android Q+
     * @param infoWifi: WiFi configuration string following the pattern WIFI:T:WPA;S:mynetwork;P:mypass;;
     */
    private fun connectToWifi(infoWifi: String){

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            //Android 10 or more
            if(wifiManager.isWifiEnabled){
                connectToWifiPostQ(infoWifi)
            }else{
                startEnableWifiActivity()
            }
        } else {
            //Android 9 or less
            if(wifiManager.isWifiEnabled){
                connectToWifiPreQ(infoWifi)
            }else{
                startEnableWifiActivity()
            }
        }
    }

    //Method that starts the activity in order to set Wifi = ON
    private fun startEnableWifiActivity(){
        runOnUiThread {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val snackbar = Snackbar.make(rootLayout, "Enciende el wifi", Snackbar.LENGTH_LONG)
                    .setAction("Encender", View.OnClickListener {
                        startActivity(Intent(Settings.Panel.ACTION_WIFI))
                    }).show()
            } else {
                val snackbar = Snackbar.make(rootLayout, "Enciende el wifi", Snackbar.LENGTH_LONG)
                    .setAction("Encender", View.OnClickListener {
                        startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }).show()
            }
        }
    }

    /**
     * Method used to connect to the specified WiFi on Android Q+ (10+)
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectToWifiPostQ(infoWifi: String){
        var networkSSID = ""
        var networkPass = ""
        var networkType = ""

        val infoWifiArr = infoWifi.split(";")

        //Getting configuration parameters, splitting the string
        //in the format WIFI:T:WPA;S:mynetwork;P:mypass;;
        for (i in infoWifiArr){
            if(i.startsWith("WIFI", false)){
                networkType = i.split(":")[2]
            }else if (i.startsWith("S:")){
                networkSSID = i.split(":")[1]
            }else if (i.startsWith("P:")){
                networkPass = i.split(":")[1]
            }
        }

        //Makes the wifiBuilder whith provided ssid and password
        val wifiBuilder = WifiNetworkSuggestion.Builder().setSsid(networkSSID).setWpa2Passphrase(networkPass)
        val suggestion = wifiBuilder.build()

        val wifiList = listOf(suggestion)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val status = wifiManager.addNetworkSuggestions(wifiList)
        if(status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS){
            Log.d(tag, "Error adding network suggestion")
        }else{
            showToast(this@ReadQrActivity.getString(R.string.ReadQrAct_checkNotifications), Toast.LENGTH_LONG)
        }

        // Optional (Wait for post connection broadcast to one of your suggestions)
        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return
                }
                Log.d(tag, "Conexion establecida")
            }
        }
        applicationContext.registerReceiver(broadcastReceiver, intentFilter);
    }

    /**
     * Method used to connect to the specified WiFi on Android 9-
     */
    //private fun connectToWifiPreQ(networkSSID: String, networkPass: String): Boolean {
    private fun connectToWifiPreQ(infoWifi: String): Boolean {

        var networkSSID = ""
        var networkPass = ""
        var networkType = ""

        val infoWifiArr = infoWifi.split(";")

        //Getting configuration parameters
        for (i in infoWifiArr){
            if(i.startsWith("WIFI", false)){
                networkType = i.split(":")[2]
            }else if (i.startsWith("S:")){
                networkSSID = i.split(":")[1]
            }else if (i.startsWith("P:")){
                networkPass = i.split(":")[1]
            }
        }
        Log.d(tag, "Network type: $networkType, SSID: $networkSSID, passwd = $networkPass");

        Log.d(tag, "Connectando al wifi")

        var wifiConfig = WifiConfiguration() //Deprecado en API 29
        wifiConfig.SSID = "\"" + networkSSID + "\""
        wifiConfig.preSharedKey = "\"" + networkPass + "\""
        wifiConfig.priority = 4000 //Asi se intenta conectar a esta red y no a otra disponible
        Log.d(tag, "SSID: " + wifiConfig.SSID)
        Log.d(tag, "Pass: " + wifiConfig.preSharedKey)

        Log.d(tag, "Intentando conectarse al wifi")

        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager


        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

        if (!wifiManager.disconnect()) {
            showToast(this@ReadQrActivity.getString(R.string.ReadQrAct_unableDisconnectNet), Toast.LENGTH_LONG)
            //return false
        }

        //registerReceiver(wifiReceiver, wifiFilter)


        var netId = wifiManager.addNetwork(wifiConfig) //necesario para enableNetwork.
        if(netId == -1){
            Log.d(tag, "La red ya estaba guardada previamente")
        }else {
            Log.d(tag, "La red se ha guardado")
            var wifiInfo = wifiManager.connectionInfo
            wifiManager.disableNetwork(wifiInfo.networkId)
            wifiManager.enableNetwork(netId, true)
        }
        if (wifiManager.reconnect()) {
            //var conectado = wifiManager.reconnect()
            return true;
        } else {
            showToast(this@ReadQrActivity.getString(R.string.ReadQrAct_unableToConnect), Toast.LENGTH_SHORT)
            Log.d(tag, "Fallo en wifiManager.reconnect()")
            return false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource.release()
    }

    /**
     * Funcion usada para mostrar Toasts
     */
    private fun showToast(mensaje: String, duracion: Int) {
        runOnUiThread { Toast.makeText(this@ReadQrActivity, mensaje, duracion).show() }
    }


    /**
     * Method called while asking for camera permission
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode){
            requestCameraPermissionCode -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //If the permission is granted, restart the activity and starts using the camera
                    recreate()
                }else{
                    //If it was not granted, shows a dialog that finishes the activity
                    endActivityNoPermission()
                }
                return
            }
        }
    }

    private fun startCamera(holder: SurfaceHolder) {
        //If the camera permissions are granted, starts using it
        when {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                cameraSource.start(holder)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
            //Shows a explanatory message finishing the activity if the system decides to do it
            val builder: AlertDialog.Builder = AlertDialog.Builder(this)
            builder.setTitle(getString(R.string.permission_camDeniedTitle)).setMessage(getString(R.string.permission_camDeniedMsg))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA),
                    requestCameraPermissionCode) }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    endActivityNoPermission() }
                builder.create().show()
        }
            else -> {
                // You can directly ask for the permission.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA),
                    requestCameraPermissionCode)
            }
        }
    }

    /**
     * Method that shows a dialog explaining that camera permissions
     * are needed to use the app, and returns to the main activity
     */
    private fun endActivityNoPermission(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.permission_camDenied_endAct))
            .setPositiveButton(android.R.string.ok){ _, _ ->
                this.finish()
            }.setCancelable(false).show()
    }

    /**
     * Method that shows a dialog explaining that the QR Code read
     * had invalid info, and sets var qrCodeRead to false in order
     * to continue reading codes
     */
    private fun showWrongCodeDialog(){
        runOnUiThread {
            val alertDialog: AlertDialog = this.let {
                val builder = AlertDialog.Builder(this@ReadQrActivity)
                builder.apply {
                    setTitle(getString(R.string.ReadQrAct_readQrFailed))
                    setMessage(getString(R.string.ReadQrAct_readQrFailedMsg))
                    setNeutralButton(
                        R.string.accept
                    ) { _, _ ->
                        qrCodeRead = false
                    }
                    setCancelable(false)
                }
                // Create the AlertDialog
                builder.create()
            }
            alertDialog.show()
        }
    }
}