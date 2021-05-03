 package com.example.movil.scanActivity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.movil.*
import com.github.barteksc.pdfviewer.PDFView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

 class ScanPreviewAct : AppCompatActivity() {

    private val tag = "ScanPreview"
    private val requestExternalStoragePermissionCode = 0
    private val createDocumentActCode = 1

    private lateinit var imagePreview : ImageView
    private lateinit var saveButton : FloatingActionButton
    private lateinit var shareButton : FloatingActionButton
    private lateinit var discardButton: Button
    private lateinit var pdfView : PDFView
    private lateinit var chosenFormat : ScanSettingsHelper.Format

    private lateinit var scanResultUris : Queue<Uri>
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
            if(path == null){
                Log.d(tag, "No se ha podido obtener la ruta")
                //TODO excepcion
            }else {
                copyTempToPath(File(path))
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.scan_preview)

        imagePreview = findViewById(R.id.act_scan_preview_ImageView)
        pdfView = findViewById(R.id.act_scan_preview_PDFView)
        saveButton = findViewById(R.id.act_scan_preview_saveFab)
        shareButton = findViewById(R.id.act_scan_preview_shareFab)
        discardButton = findViewById(R.id.act_scan_preview_dicardButton)

        askAccessAllFilesPermission()
        pickit = PickiT(this, pickitListener, this)

        //Pinch gesture for zoom is set on the root layout
        val rootLayout = findViewById<ZoomLayout>(R.id.act_scan_preview_root)
        rootLayout.setImageView(imagePreview)

        val bundle = intent.extras
        if (bundle != null) {
            val uriList = bundle.getParcelableArrayList<Uri>("tempUris") as ArrayList<Uri>
            scanResultUris = LinkedList(uriList)
            chosenFormat = bundle.getSerializable("chosenFormat") as ScanSettingsHelper.Format
        }

        saveButton.setOnClickListener { askPermissions() }

        discardButton.setOnClickListener{
            val discard = scanResultUris.poll()
            if(discard != null) {
                discardFile(discard)
                useNextFile()
            }else{
                showNoFilesDialog()
            }
        }

        shareButton.setOnClickListener{
            val filePath = scanResultUris.peek()!!.path
            if(filePath == null){
                showNoFilesDialog()
            }else {
                val file = File(filePath)
                val uriAux =
                    FileProvider.getUriForFile(applicationContext, "$packageName.provider", file)

                //Open email app and load info
                val emailIntent = Intent(Intent.ACTION_SEND)
                emailIntent.type = "*/*"

                val to: Array<String> = emptyArray()
                emailIntent.putExtra(Intent.EXTRA_EMAIL, to)
                emailIntent.putExtra(Intent.EXTRA_STREAM, uriAux)
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "")
                startActivity(Intent.createChooser(emailIntent, "Enviar"))
            }
        }
        useNextFile()
    }

     /**
      * Renders the next file in the scanResultUris list if available
      */
     private fun useNextFile(){
         if (scanResultUris.isEmpty()){
             showNoFilesDialog()
         }else {
             //Check format in order to render it
             when (chosenFormat) {
                 ScanSettingsHelper.Format.PDF -> {
                     val currentUri = scanResultUris.peek()
                     if (currentUri != null) {
                         previewPdf(currentUri)
                     } else {
                         showNoFilesDialog()
                     }
                 }
                 ScanSettingsHelper.Format.JPEG -> {
                     val currentUri = scanResultUris.peek()
                     if (currentUri != null) {
                         previewImage(currentUri)
                     } else {
                         showNoFilesDialog()
                     }
                 }
                 ScanSettingsHelper.Format.RAW -> {
                     Toast.makeText(this, "Formato RAW", Toast.LENGTH_SHORT).show()
                 }
             }
         }
     }

    private fun previewImage(uri : Uri){
        pdfView.visibility = View.GONE
        imagePreview.visibility = View.VISIBLE
        val file = File(uri.path!!)
        imagePreview.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        imagePreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
    }

    private fun previewPdf(uri : Uri){
        imagePreview.visibility = View.GONE
        pdfView.visibility = View.VISIBLE
        pdfView.fromUri(uri).load()
    }

    private fun askPermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //In android 11 manage all files permission is needed
            if(Environment.isExternalStorageManager()){
                askDirectory()
            }else {
                askAccessAllFilesPermission()
            }
        }else {
            //Android >11 permissions
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
        val ext = when(chosenFormat){
            ScanSettingsHelper.Format.RAW -> "application/octet-stream"
            ScanSettingsHelper.Format.PDF -> "application/pdf"
            ScanSettingsHelper.Format.JPEG -> "image/jpeg"
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)
            type = ext
            //putExtra(Intent.EXTRA_TITLE, "") //file suggested name
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
                    Log.d(tag, "Create document canceled")
                }
            }
        }
    }

     /**
      * Copies the file from the temporal folder to the
      * one created by the user
      */
    private fun copyTempToPath(destFile : File){
        val tempUri = scanResultUris.peek()
        if(tempUri == null){
            showNoFilesDialog()
        }
        val temp = File(tempUri!!.path!!)
        temp.copyTo(destFile, true, 2048)
        temp.delete()
        scanResultUris.poll() //Delete from the queue
        useNextFile()
    }

    private fun endActivityNoPermission(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.permission_ExtStorageDenied_endAct))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok){ _, _ ->
                discardAllFiles()
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }.show()
    }

    private fun discardFile(uri : Uri){
        val tempFile = File(uri.path!!)
        if(tempFile.exists()) {
            tempFile.delete()
        }
    }

    private fun discardAllFiles(){
        scanResultUris.forEach{
            discardFile(it)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        discardAllFiles()
    }

    /**
     * Fun that creates a dialog when no more scan results are aviable and closes the activity
     */
    private fun showNoFilesDialog(){
        val alertDialog: AlertDialog = this.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {

                setCancelable(false)
                setTitle("Sin resultados")
                setMessage("No hay mas resultados")

                setPositiveButton(R.string.accept
                ) { _, _ ->
                    discardAllFiles()
                    startActivity(Intent(context, ScanAct::class.java))
                }
            }
            // Create the AlertDialog
            builder.create()
        }
        alertDialog.show()
    }
}