package com.example.movil

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Vibrator
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.lang.Exception

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

    val TAG = "---ReadQrActivity---"


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

                //Se comprueba si se tienen los permisos sobre la cámara
                if (ContextCompat.checkSelfPermission(
                        this@ReadQrActivity,
                        Manifest.permission.CAMERA
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    //Si no se tienen permisos, se comprueba si se necesita un mensaje explicatorio
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@ReadQrActivity, Manifest.permission.CAMERA
                        )
                    ) {
                        showExplanation(
                            "Camara denegada",
                            "Se necesita acceso a la cámara para escanear el codigo QR",
                            Manifest.permission.CAMERA,
                            requestCameraPermissionCode
                        )
                    } else {
                        requestPermission(Manifest.permission.CAMERA, requestCameraPermissionCode)
                    }
                    Log.d(TAG, "No se tienen permisos sobre la camara")
                    return
                }
                //Se tienen permisos sobre la camara
                Log.d(TAG, "Se tienen permisos sobre la camara")
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

                        if (connectToWifi(infoWiFi[0], infoWiFi[1])) {
                            //Si se conecta al wifi, va a la pantalla de inicio
                           // cameraSource.release()
                           // startActivity(Intent(this@ReadQrActivity, MainActivity::class.java))
                        } else {
                            //Si no, se reinicia la actividad
                          //  recreate()
                        }
                    }
                }
            }
        })
    }


    /**
     * Funcion usada para conectarse al wifi con los parámetros dados
     */
    private fun connectToWifi(networkSSID: String, networkPass: String): Boolean {

        //No hace falta comprobar los permisos de acceso y cambio del wifi
        //ya que los comprueba el sistema por si mismo

        Log.d(TAG, "Connectando al wifi")

        var wifiConfig = WifiConfiguration() //Deprecado en API 29
        wifiConfig.SSID = "\"" + networkSSID + "\""
        wifiConfig.preSharedKey = "\"" + networkPass + "\""
        wifiConfig.priority = 4000 //Asi se intenta conectar a esta red y no a otra disponible
        Log.d(TAG, "SSID: " + wifiConfig.SSID)
        Log.d(TAG, "Pass: " + wifiConfig.preSharedKey)

        Log.d(TAG, "Intentando conectarse al wifi")

        val wifiManager = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }

        if (!wifiManager.disconnect()) {
            muestraToast("No se ha podido desconectar de la red actual", Toast.LENGTH_LONG)
            return false
        }

        //registerReceiver(wifiReceiver, wifiFilter)


        var netId = wifiManager.addNetwork(wifiConfig) //necesario para enableNetwork.
        if(netId == -1){
            Log.d(TAG, "La red ya estaba guardada previamente")
        }else {
            Log.d(TAG, "La red se ha guardado")
            wifiManager.enableNetwork(netId, true)
        }
        if (wifiManager.reconnect()) {
            //var conectado = wifiManager.reconnect()
            return true;
        } else {
            muestraToast("No se ha podido conectar a la red", Toast.LENGTH_SHORT)
            Log.d(TAG, "Fallo en wifiManager.reconnect()")
            return false
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
        var builder: AlertDialog.Builder = AlertDialog.Builder(this)
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
        ActivityCompat.requestPermissions(this@ReadQrActivity, arrayOf(name), code)
        recreate()
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
     * Funcion usada par amostrar Toasts
     */
    private fun muestraToast(mensaje: String, duracion: Int) {
        runOnUiThread { Toast.makeText(this@ReadQrActivity, mensaje, duracion).show() }
    }

}