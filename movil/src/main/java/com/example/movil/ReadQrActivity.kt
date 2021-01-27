package com.example.movil

import android.Manifest
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
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

class ReadQrActivity : AppCompatActivity() {
    lateinit var surfaceView: SurfaceView //lateinit hace que no tenga que inicializarse ahora

    //lateinit var textoQr: TextView
    lateinit var cameraSource: CameraSource
    lateinit var barcodeDetector: BarcodeDetector

    val requestCameraPermissionCode = 1 //Codigo usado al solicitar los permisos de la camara
    //val requestWifiPermissionCode = 2

    val detectorHeight = 640
    val detectorWidth = 640
    var codigoLeido: Boolean = false //Indica si ya se ha leido un QR



    val tag = "---ReadQrActivity---"


    //var wifiFilter = IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION) //Usado para mirar el estado del wifi
    //Utilizado para ver cuando se conecta el dispositivo a la red
    //val wifiReceiver = WifiReceiver()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)



        surfaceView = findViewById<SurfaceView>(R.id.act_readQR_cameraPreview)
        //textoQr = findViewById<TextView>(R.id.act_readQR_texto_qr)
        barcodeDetector = BarcodeDetector.Builder(this).setBarcodeFormats(Barcode.QR_CODE).build()
        cameraSource = CameraSource.Builder(this, barcodeDetector)
            .setRequestedPreviewSize(detectorHeight, detectorWidth).setAutoFocusEnabled(true)
            .build()


        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {

                //Check for camera permission
                val permissionHelper = PermissionHelper(
                    this@ReadQrActivity,
                    Manifest.permission.CAMERA,
                    requestCameraPermissionCode,
                    "Camara denegada",
                    "Se necesita acceso a la cámara para escanear el codigo QR"
                )
                permissionHelper.checkAndAskForPermission()


                Log.d(tag, "Se tienen permisos sobre la camara")
                cameraSource.start(holder)

            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                cameraSource.stop()
            }

        })

        barcodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {
            }

            override fun receiveDetections(detections: Detector.Detections<Barcode>?) {
                if (!codigoLeido) {

                    var qrCodes: SparseArray<Barcode> =
                        detections?.detectedItems ?: throw Exception("No se encontraron codigos qr")
                    if (qrCodes.size() != 0) {

                        codigoLeido = true;

                        //Si se detecta un codigo QR el movil vibra

                        var vibrator: Vibrator =
                            applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        vibrator.vibrate(500) //No deprecado hasta api 26

                        //Se obtiene el mensaje del QR
                        val infoWiFi = qrCodes.valueAt(0).displayValue.split(" ")

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
                            connectToWifiQ(infoWiFi[0], infoWiFi[1])
                        } else{
                            //Android 9 or less
                            if (connectToWifi(infoWiFi[0], infoWiFi[1])) {
                                //Si se conecta al wifi, va a la pantalla de inicio
                                // cameraSource.release()
                                // startActivity(Intent(this@ReadQrActivity, MainActivity::class.java))
                            } else {
                                //Si no, se reinicia la actividad
                                //  recreate()
                            }
                        }

                        /*
                        if (connectToWifi(infoWiFi[0], infoWiFi[1])) {
                            //Si se conecta al wifi, va a la pantalla de inicio
                            // cameraSource.release()
                            // startActivity(Intent(this@ReadQrActivity, MainActivity::class.java))
                        } else {
                            //Si no, se reinicia la actividad
                            //  recreate()
                        }
                        */
                    }
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun connectToWifiQ(networkSSID: String, networkPass: String){
        Log.d(tag, "Connecting to wifi on Android Q+")

        val wifiBuilder = WifiNetworkSuggestion.Builder().setSsid(networkSSID).setWpa2Passphrase(networkPass)
        val suggestion = wifiBuilder.build()

        var wifiList = mutableListOf<WifiNetworkSuggestion>()
        wifiList.add(suggestion)

        val wifiManager = getApplicationContext().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val status = wifiManager.addNetworkSuggestions(wifiList)
    }

    /**
     * Funcion usada para conectarse al wifi con los parámetros dados
     */
    private fun connectToWifi(networkSSID: String, networkPass: String): Boolean {

        //No hace falta comprobar los permisos de acceso y cambio del wifi
        //ya que los comprueba el sistema por si mismo

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
            muestraToast("No se ha podido desconectar de la red actual", Toast.LENGTH_LONG)
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
            muestraToast("No se ha podido conectar a la red", Toast.LENGTH_SHORT)
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

                var hasPermission = PackageManager.PERMISSION_DENIED

                //Check for camera permission
                for (i in 0..permissions.size) {
                    if (permissions[i] == Manifest.permission.CAMERA) {
                        hasPermission = grantResults[i]
                        break
                    }
                }

                if (hasPermission == PackageManager.PERMISSION_GRANTED) {
                    this.recreate()
                } else {
                    Toast.makeText(
                        this@ReadQrActivity,
                        "Se necesitan permisos para acceder a la camara",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }
        }
    }

}