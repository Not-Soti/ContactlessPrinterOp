package com.example.movil

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import java.io.File

class ScanPreview : AppCompatActivity() {

    private val tag = "ScanPreview"
    private val writeExternalStoragePermissionCode = 0
    private val createDocumentPermissionCode = 1
    private lateinit var imagePreview : ImageView
    private lateinit var saveButton : Button
    private lateinit var tempFilePath : String
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
            copyTempToPath(File(path))
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_preview)

        imagePreview = findViewById(R.id.act_scan_preview_ImageView)
        saveButton = findViewById(R.id.act_scan_preview_saveButton)
        //imagePreview.visibility = View.INVISIBLE

        askAccessAllFilesPermission()

        saveButton.setOnClickListener { saveFile() }

        val bundle = intent.extras
        if (bundle != null) {
            tempFilePath = bundle.getString("tempPath", "")
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

    private fun saveFile(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //In android 11 manage all files permission is needed
            if(Environment.isExternalStorageManager()){
                askDirectory()
            }else {
                askAccessAllFilesPermission()
            }
        }else {

            if(ContextCompat.checkSelfPermission(applicationContext, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED){
                askDirectory()
            }else {
                val permissionHelper = PermissionHelper(
                    this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    writeExternalStoragePermissionCode,
                    "Acceso al almacenamiendo necesario",
                    "Acceso al almacenamiento necesario para crear el archivo escaneado"
                )
                permissionHelper.checkAndAskForPermission()
            }
        }
    }

    private fun askAccessAllFilesPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        uri
                    )
                )
            }
        }
    }

    private fun askDirectory(){
        //TODO pedir permisos
        Log.d(tag, "askDirectory()")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)

            type = "application/pdf"
            //type = "text/plain"
            putExtra(Intent.EXTRA_TITLE, "ArchivoPrueba1")
        }
        startActivityForResult(intent, createDocumentPermissionCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            writeExternalStoragePermissionCode ->{
                saveFile()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            createDocumentPermissionCode ->{
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

    }
}