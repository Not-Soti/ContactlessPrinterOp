package com.example.movil

import android.Manifest
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
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
    lateinit var textoQr: TextView
    lateinit var cameraSource: CameraSource
    lateinit var barcodeDetector: BarcodeDetector

    val requestCameraPermissionCode =
        1 //Codigo usado al solicitar los permisos de la camara y en la respuesta
    val requestWifiPermissionCode = 2

    val detectorHeight = 640
    val detectorWidth = 640
    var codigoLeido: Boolean = false //Indica si ya se ha leido un QR

    val TAG = "ReadQrActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        surfaceView = findViewById<SurfaceView>(R.id.act_readQR_cameraPreview)
        textoQr = findViewById<TextView>(R.id.act_readQR_texto_qr)
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
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    //Si no se tienen permisos, se comprueba si se necesita un mensaje explicatorio
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this@ReadQrActivity,
                            Manifest.permission.CAMERA
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
                        codigoLeido=true;
                        //Si se detecta un codigo QR el movil vibra
                        textoQr.post {
                            var vibrator: Vibrator =
                                applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            vibrator.vibrate(500) //No deprecado hasta api 26

                            //Se obtiene el mensaje del QR
                            textoQr.text = qrCodes.valueAt(0).displayValue
                            Log.d(TAG, qrCodes.valueAt(0).displayValue)

                            val infoWiFi = qrCodes.valueAt(0).displayValue.split(" ")
                            connectToWifi(infoWiFi[0], infoWiFi[1]);
                        }
                    }
                }
            }

        })
    }


    /**
     * Funcion usada para conectarse al wifi con los parámetros dados
     */
    private fun connectToWifi(networkSSID: String, networkPass: String) {

        //No hace falta comprobar los permisos de acceso y cambio del wifi
        //ya que los comprueba el sistema por si mismo

        Log.d(TAG, "Connectando al wifi")

        var wifiConfig = WifiConfiguration() //Deprecado en API 29
        wifiConfig.SSID = "\"" + networkSSID + "\""
        wifiConfig.preSharedKey = "\"" + networkPass + "\""
        Log.d(TAG, "SSID: " + wifiConfig.SSID)
        Log.d(TAG, "Pass: " + wifiConfig.preSharedKey)

        Log.d(TAG, "Intentando conectarse al wifi")

        val wifiManager = getApplicationContext().getSystemService(WIFI_SERVICE) as WifiManager

        if(!wifiManager.isWifiEnabled){
            wifiManager.isWifiEnabled = true
        }

        var netId = wifiManager.addNetwork(wifiConfig) //necesario para enableNetwork.
        wifiManager.disconnect()
        wifiManager.enableNetwork(netId, true)
        wifiManager.reconnect()

        Log.d(TAG, "Wifi conectado")

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


}