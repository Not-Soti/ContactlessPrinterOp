package com.example.movil.readQrActivity

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.movil.MainActivity
import com.example.movil.PermissionHelper
import com.example.movil.R
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

class ReadQrActivity : AppCompatActivity() {

    private lateinit var surfaceView: SurfaceView
    private lateinit var cameraSource: CameraSource
    private lateinit var barcodeDetector: BarcodeDetector
    private lateinit var helpButton : ImageButton
    private lateinit var connectManuallyButton : Button


    private val requestCameraPermissionCode = 1 //Code needed to ask for permissions
    private val accessFineLocationPermissionCode = 2

    private val detectorHeight = 640
    private val detectorWidth = 640
    private var qrCodeRead: Boolean = false //Checks if a QR code has been detected

    private val tag = "---ReadQrActivity---"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        surfaceView = findViewById(R.id.act_readQR_cameraPreview)
        helpButton = findViewById(R.id.act_readQR_help_button)
        connectManuallyButton = findViewById(R.id.act_readQR_connectManuallyButton)
        barcodeDetector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build()
        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(detectorHeight, detectorWidth).setAutoFocusEnabled(true)
            .build()

        helpButton.setOnClickListener { //Create alert dialog
            val builder: AlertDialog.Builder? = this@ReadQrActivity?.let { AlertDialog.Builder(it) }

            builder?.apply { setNeutralButton(R.string.accept) { dialog, _ -> dialog.dismiss() } }

            builder?.setMessage(this@ReadQrActivity.getString(R.string.Dialog_qr_scanner_help))
                ?.setTitle(
                    R.string.help
                )
            val dialog: AlertDialog? = builder?.create()
            if (dialog != null) {
                dialog.show()
            }
        }

        connectManuallyButton.setOnClickListener { startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)) }

        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                startCamera(holder)
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

            override fun surfaceDestroyed(p0: SurfaceHolder) {cameraSource.stop()}
        })

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>?) {
                if (!qrCodeRead) {

                    var qrCodes: SparseArray<Barcode> =
                        detections?.detectedItems ?: throw Exception("QR codes not found") //TODO

                    if (qrCodes.size() != 0) {
                        qrCodeRead = true

                        //The device vibrates if a code is detected
                        val vibrator: Vibrator =
                            applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        vibrator.vibrate(500)

                        //Reading QR message
                        val infoWifi = qrCodes.valueAt(0).rawValue
                        Log.d(tag, "Mensaje QR: $infoWifi")

                        if(infoWifi.startsWith("WIFI", true)) {
                            //QR code contains wifi configuration
                            connectToWifi(infoWifi)

                        }else{
                            muestraToast(this@ReadQrActivity.getString(R.string.ReadQrAct_readQrFailed), Toast.LENGTH_LONG)
                            qrCodeRead = false
                        }
                    }
                }
            }
        })
    }

    /**
     * Method used to connect to the specified WiFi on Android Q+
     * @param infoWifi: WiFi configuration string following the pattern WIFI:T:WPA;S:mynetwork;P:mypass;;
     */
    private fun connectToWifi(infoWifi: String){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            //Android 10 or more
            connectToWifiPostQ(infoWifi)
        } else {
            //Android 9 or less
            //if (connectToWifiPreQ(infoWiFi[0], infoWiFi[1])) {
            connectToWifiPreQ(infoWifi)
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

        Log.d(tag, "Connecting to wifi on Android Q+")


        val wifiBuilder = WifiNetworkSuggestion.Builder().setSsid(networkSSID).setWpa2Passphrase(networkPass)
        val suggestion = wifiBuilder.build()

        var wifiList = mutableListOf<WifiNetworkSuggestion>()
        wifiList.add(suggestion)

        val wifiManager = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager

        //Checking if Wifi is enabled
        if(!wifiManager.isWifiEnabled){
            muestraToast(this@ReadQrActivity.getString(R.string.ReadQrAct_powerOnWifi), Toast.LENGTH_LONG)
        }

        muestraToast(this@ReadQrActivity.getString(R.string.ReadQrAct_checkNotifications), Toast.LENGTH_LONG)

        val permissionHelper = PermissionHelper(this@ReadQrActivity,
            Manifest.permission.ACCESS_FINE_LOCATION,
            accessFineLocationPermissionCode,
            this@ReadQrActivity.getString(R.string.permission_fineLocDeniedTitle),
            this@ReadQrActivity.getString(R.string.permission_fineLocDeniedMsg)
        ).checkAndAskForPermission()


        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            wifiManager.addSuggestionConnectionStatusListener(mainExecutor,
                (WifiManager.SuggestionConnectionStatusListener { wifiNetworkSuggestion, failureReason ->
                    Log.d(tag, "Unable to connect to the provided network")
                    muestraToast(this@ReadQrActivity.getString(R.string.ReadQrAct_unableToConnect), Toast.LENGTH_LONG)
                    wifiManager.removeNetworkSuggestions(wifiList)
                })
            )
        }

        val status = wifiManager.addNetworkSuggestions(wifiList)
        if(status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS){
            Log.d(tag, "Unable to connect to the provided network")
            muestraToast(this@ReadQrActivity.getString(R.string.ReadQrAct_unableToConnect), Toast.LENGTH_LONG)
            //qrCodeRead = false //So another one can be read
            wifiManager.removeNetworkSuggestions(wifiList)
        }

        //Wait for post connection broadcast to one of your suggestions)
        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return;
                }
                // do post connect processing here
            }
        };
        this@ReadQrActivity.registerReceiver(broadcastReceiver, intentFilter);
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

        val wifiManager = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager


        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

        if (!wifiManager.disconnect()) {
            muestraToast(this@ReadQrActivity.getString(R.string.ReadQrAct_unableDisconnectNet), Toast.LENGTH_LONG)
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
            muestraToast(this@ReadQrActivity.getString(R.string.ReadQrAct_unableToConnect), Toast.LENGTH_SHORT)
            Log.d(tag, "Fallo en wifiManager.reconnect()")
            return false
        }
    }

    //On back pressed go to main activity
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraSource.release()
    }

    /**
     * Funcion usada para mostrar Toasts
     */
    private fun muestraToast(mensaje: String, duracion: Int) {
        runOnUiThread { Toast.makeText(this@ReadQrActivity, mensaje, duracion).show() }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode){
            requestCameraPermissionCode -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    recreate()
                }else{
                    endActivityNoPermission()
                }
                return
            }
        }
    }

    private fun startCamera(holder: SurfaceHolder) {
        /*if(ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            val permissionHelper = PermissionHelper(
                this@ReadQrActivity,
                Manifest.permission.CAMERA,
                requestCameraPermissionCode,
                this@ReadQrActivity.getString(R.string.permission_camDeniedTitle),
                this@ReadQrActivity.getString(R.string.permission_camDeniedMsg)
            )
            permissionHelper.checkAndAskForPermission()
        }else{
            cameraSource.start(holder)
        }*/

        when {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                cameraSource.start(holder)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA) -> {
            //Show explanatory message
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

    private fun endActivityNoPermission(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.permission_camDenied_endAct))
            .setPositiveButton(android.R.string.ok){ _, _ ->
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }.show()
    }

}