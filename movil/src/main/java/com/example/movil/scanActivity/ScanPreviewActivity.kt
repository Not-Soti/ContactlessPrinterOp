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

 class ScanPreviewActivity : AppCompatActivity() {

    private val tag = "ScanPreview"
    private val requestExternalStoragePermissionCode = 0
    private val createDocumentActCode = 1 //Code used to get the Creating document activity response code

    private val activityResultOK = 1 /*Code used to finish the activity and tell the ScanSettingsFragment
                                       that the scan proccess is finished, and the app needs to
                                       roll back to the main activity */

    private lateinit var imagePreview : ImageView
    private lateinit var saveButton : FloatingActionButton
    private lateinit var shareButton : FloatingActionButton
    private lateinit var discardButton: Button
    private lateinit var pdfView : PDFView
    private lateinit var chosenFormat : ScanSettingsHelper.Format //Tells the scan result format (pdf, image)

    private lateinit var scanResultUris : Queue<Uri> //List cointaining the scanning results' URIs

    private lateinit var pickit : PickiT //Object that gets files real path from their URI
    private val pickitListener = object: PickiTCallbacks {
        override fun PickiTonUriReturned() {
            //Used when the file is picked from the Cloud
        }
        override fun PickiTonStartListener() {
            //Called when the selected file is not local, and the file creation starts
        }
        override fun PickiTonProgressUpdate(progress: Int) {
            //Called when the file is not local, updating the download progress
        }

        override fun PickiTonCompleteListener(
            path: String?,
            wasDriveFile: Boolean,
            wasUnknownProvider: Boolean,
            wasSuccessful: Boolean,
            Reason: String?
        ) {
            //Called when the path is got. If the file is local, this is called directly
            //Sets the path and renders the image preview
            Log.d(tag, "Pickit on complete listener Ruta: $path")
            if(path == null){
                Log.d(tag, "No se ha podido obtener la ruta")
                //TODO excepcion
            }else {
                copyTempToPath(File(path)) //Copies the scanning result to the created one
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

        //If the device is Android 11+, checks if the special permission for accessing
        //files is granted
        askAccessAllFilesPermission()

        pickit = PickiT(this, pickitListener, this)

        //Pinch gesture for zoom is set on the root layout
        val rootLayout = findViewById<ZoomLayout>(R.id.act_scan_preview_root)
        rootLayout.setImageView(imagePreview)

        val bundle = intent.extras
        if (bundle != null) {
            //Gets the list of URIs from the bundle
            val uriList = bundle.getParcelableArrayList<Uri>("tempUris") as ArrayList<Uri>
            scanResultUris = LinkedList(uriList)
            //Gets the scanning result chosen format (pdf, jpg...)
            chosenFormat = bundle.getSerializable("chosenFormat") as ScanSettingsHelper.Format
        }

        saveButton.setOnClickListener {
            askPermissions() //Asks for file write permissions
        }

        discardButton.setOnClickListener{
            val discard = scanResultUris.poll()
            if(discard != null) {
                discardFile(discard) //Deletes the temp file
                useNextFile() //If more files were scanned, use the next one
            }else{
                //If no more files are aviable, shows a dialog that ends the activity
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

        //Use the first file got
        useNextFile()
    }

     /**
      * Renders the next file in the scanResultUris list if available
      */
     private fun useNextFile(){
         if (scanResultUris.isEmpty()){
             showNoFilesDialog() //If no more files are aviable, shows a dialog that ends the activity
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
                     //TODO
                 }
             }
         }
     }

     /**
      * Method that previews an image
      */
    private fun previewImage(uri : Uri){
        pdfView.visibility = View.GONE //Hide the pdfView and show the imageView
        imagePreview.visibility = View.VISIBLE

        //Creates a bitmap from the file path and shows it
        val file = File(uri.path!!)
        imagePreview.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        imagePreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
    }

     /**
      * Method that previews a PDF
      */
    private fun previewPdf(uri : Uri){
        //Hides the imageView, shows the pdfView and shows the pdf
        imagePreview.visibility = View.GONE
        pdfView.visibility = View.VISIBLE
        pdfView.fromUri(uri).load()
    }

     /**
      * Ask for file aces permission
      */
    private fun askPermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            //In android 11 manage all files special permission is needed
            if(Environment.isExternalStorageManager()){
                //If granted, creates the file
                askDirectory()
            }else {
                //If not granted, asks for the permission
                askAccessAllFilesPermission()
            }
        }else {
            //Android <11 permissions only ask for write storage permission
            when {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // If granted, creates the file
                    askDirectory()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) -> {
                    //Show explanatory message about asking for permissionsif the system decides to
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle(getString(R.string.permission_extStorageDeniedTitle)).setMessage(getString(R.string.permission_extStorageDeniedMsg))

                        //If the user accepts, asks for the permissino
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            ActivityCompat.requestPermissions(this,
                                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                                requestExternalStoragePermissionCode) }
                        //If the user doesn't accept, shows a dialog that finishes the activity
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

     /**
      * Method that asks for the acces files special permission on Android 11+
      */
    private fun askAccessAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                startActivity(Intent( Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri))
            }
        }
    }

     /**
      * Method that makes the user select a directory in order to save the scan result
      */
    private fun askDirectory(){
        val ext = when(chosenFormat){
            ScanSettingsHelper.Format.RAW -> "application/octet-stream"
            ScanSettingsHelper.Format.PDF -> "application/pdf"
            ScanSettingsHelper.Format.JPEG -> "image/jpeg"
        }
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE)
            type = ext
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

     /**
      * Method called when a file is created in order to save the scan result in it
      */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            createDocumentActCode ->{
                if(resultCode == Activity.RESULT_OK) {
                    val newDocUri = data?.data
                    //If the file was corretly created, gets it's path from PickiT
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

     /**
      * Method that shows a dialog explaining that permissions are needed
      * in order to continue using the app and finishes the activity
      */
    private fun endActivityNoPermission(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.permission_ExtStorageDenied_endAct))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok){ _, _ ->
                discardAllFiles() //Deletes the temporal files before closing
                this@ScanPreviewActivity.finish() //Finishes the activity
            }.show()
    }

     /**
      * Method that deletes the desired file from the devide
      * @param uri: URI of the file that is going to be deleted.
      */
    private fun discardFile(uri : Uri){
        val tempFile = File(uri.path!!)
        if(tempFile.exists()) {
            tempFile.delete()
        }
    }

     /**
      * Method that calls discardFile(uri) for every file left in the result list
      */
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
                    setResult(activityResultOK) /*Tells the ScanOptionsFragment that the
                                                scanning process is finished, so it can finish
                                                and go back to the main activity*/
                    finish()
                }
            }
            // Create the AlertDialog
            builder.create()
        }
        alertDialog.show()
    }
}