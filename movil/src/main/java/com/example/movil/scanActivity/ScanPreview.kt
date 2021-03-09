 package com.example.movil.scanActivity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.movil.BuildConfig
import com.example.movil.MainActivity
import com.example.movil.R
import com.example.movil.ScanActivity
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import java.io.File

class ScanPreview : AppCompatActivity() {

    private val tag = "ScanPreview"
    private val requestExternalStoragePermissionCode = 0
    private val createDocumentActCode = 1

    private lateinit var imagePreview : ImageView
    private lateinit var saveButton : Button
    private lateinit var discardButton: Button

    private lateinit var tempFilePath : String
    private var tempFileUri : Uri? = null
    private lateinit var pickit : PickiT
    private val pickitListener = object: PickiTCallbacks {
        override fun PickiTonUriReturned() {
            //Used when the file is picked from the Cloud
            Log.d(tag, "Pickit on uri returned (Descargando archivo)")
        }

        override fun PickiTonStartListener() {
            Log.d(tag, "Pickit on start listener (Creando archivo de descarga)")
        }

        override fun PickiTonProgressUpdate(progress: Int) {
            Log.d(tag, "Pickit on progress update (Progreso de descarga $progress")
        }

        override fun PickiTonCompleteListener(
            path: String?,
            wasDriveFile: Boolean,
            wasUnknownProvider: Boolean,
            wasSuccessful: Boolean,
            Reason: String?
        ) {
            Log.d(tag, "Pickit on complete listener Ruta: $path")
            Toast.makeText(applicationContext, "Ruta: $path", Toast.LENGTH_LONG).show()

            Log.d(tag, "PickiT path: $path")
            copyTempToPath(File(path!!))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_preview)

        imagePreview = findViewById(R.id.act_scan_preview_ImageView)
        saveButton = findViewById(R.id.act_scan_preview_saveButton)
        discardButton = findViewById(R.id.act_scan_preview_dicardButton)

        askAccessAllFilesPermission()

        saveButton.setOnClickListener { saveFile() }
        discardButton.setOnClickListener{ discardFile() }

        val bundle = intent.extras
        if (bundle != null) {
            tempFileUri = Uri.parse(bundle.getString("tempUri", ""))
            tempFilePath = tempFileUri?.path!!
            Log.d(tag, "Temp file uri: $tempFileUri")
            Log.d(tag, "Temp file path: $tempFilePath")
        }

        pickit = PickiT(this, pickitListener, this)

        //render pdf
        val file = File(tempFilePath)
        imagePreview.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        val fileDescriptor: ParcelFileDescriptor = ParcelFileDescriptor.open(
            file,
            ParcelFileDescriptor.MODE_READ_ONLY
        )
        val pdfRenderer = PdfRenderer(fileDescriptor)
        val pageToRender: PdfRenderer.Page = pdfRenderer.openPage(0)
        val bitmap = Bitmap.createBitmap(
            pageToRender.width,
            pageToRender.height,
            Bitmap.Config.ARGB_8888
        )
        pageToRender.render(
            bitmap,
            null,
            null,
            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
        )
        //Set the page on the imageView
        imagePreview.setImageBitmap(bitmap)

        pageToRender.close()
        pdfRenderer.close()
        fileDescriptor.close()
    }

    private fun discardFile(){
        val tempFile = File(tempFilePath)
        tempFile.delete()
        startActivity(Intent(this, ScanActivity::class.java))
    }

    private fun saveFile(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //In android 11 manage all files permission is needed
            if(Environment.isExternalStorageManager()){
                askDirectory()
            }else {
                askAccessAllFilesPermission()
            }
        }else {
           /* if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
                askDirectory()
            }else {
                val permissionHelper = PermissionHelper(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    writeExternalStoragePermissionCode,
                    "Acceso al almacenamiendo necesario",
                    "Acceso al almacenamiento necesario para crear el archivo escaneado"
                )
                permissionHelper.checkAndAskForPermission()
            }*/
            when {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // You can use the API that requires the permission.
                    askDirectory()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    //Show explanatory message
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle(getString(R.string.permission_extStorageDeniedTitle)).setMessage(getString(R.string.permission_extStorageDeniedMsg))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            ActivityCompat.requestPermissions(this,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                requestExternalStoragePermissionCode) }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            endActivityNoPermission() }
                    builder.create().show()
                }
                else -> {
                    // You can directly ask for the permission.
                    ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        requestExternalStoragePermissionCode)
                }
            }
        }
    }

    private fun askAccessAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                startActivity(Intent( Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri))
            }
        }
    }

    private fun askDirectory(){
        Log.d(tag, "askDirectory()")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)

            type = "application/pdf"
            //type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "ArchivoPrueba1")
        }
        startActivityForResult(intent, createDocumentActCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            requestExternalStoragePermissionCode ->{
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    askDirectory()
                }else{
                    endActivityNoPermission()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            createDocumentActCode ->{
                if(resultCode == Activity.RESULT_OK) {
                    val newDocUri = data?.data
                    Log.d(tag, "Uri del nuevo archivo: $newDocUri")
                    pickit.getPath(newDocUri, Build.VERSION.SDK_INT)
                }else{
                    Log.d(tag, "Create document cancelado")
                }
            }
        }
    }

    private fun copyTempToPath(destFile : File){
        val temp = File(tempFilePath)
        Log.d(tag, "Copiando archivo")
        temp.copyTo(destFile, true, 2048)
        Log.d(tag, "Archivo copiado")
        temp.delete()
        Log.d(tag, "Temporal borrado")
        startActivity(Intent(this, ScanActivity::class.java))
    }

    private fun endActivityNoPermission(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.permission_ExtStorageDenied_endAct))
            .setPositiveButton(android.R.string.ok){ _, _ ->
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }.show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        discardFile()
    }
}