package com.example.movil.printActivity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Log
import android.view.*
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.print.PrintHelper
import com.example.movil.MainActivity
import com.example.movil.R
import com.example.movil.ZoomLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import java.io.File
import java.util.*

import com.example.movil.printActivity.PrintActViewModel.ResourceTypeEnum
import com.github.barteksc.pdfviewer.PDFView

class PrintActivity : AppCompatActivity() {

    private val tag = "--- PrintActivity ---"

    private lateinit var buttonChooseFile: Button
    private lateinit var buttonShare : FloatingActionButton
    private lateinit var buttonPrint : FloatingActionButton
    private lateinit var imagePreview: ImageView
    private lateinit var webPreview: WebView
    private lateinit var downloadFragment: DownloadingFileFragment
    private lateinit var rootLayout : ZoomLayout
    private lateinit var pdfView : PDFView

    private var imageViewOrigX = 0F
    private var imageViewOrigY = 0F

    private val chooseFileActRequestCode = 1 //Code used in the onActivityResult petition
    private val requestExternalStoragePermissionCode = 2 //Code used when asking for permissions

    private lateinit var viewModel : PrintActViewModel

    private lateinit var pickit : PickiT //returns real path from uris
    private val pickitListener = object: PickiTCallbacks{
        override fun PickiTonUriReturned() {
            //Used when the file is picked from the Cloud
            Log.d(tag, "Pickit on uri returned (Descargando archivo)")
            //Toast.makeText(applicationContext, "Obteniendo archivo", Toast.LENGTH_LONG).show()

            //Create downloading fragment
            val fragmentManager = supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            downloadFragment = DownloadingFileFragment()
            downloadFragment.show(fragmentTransaction, "downloadFragment")
        }

        override fun PickiTonStartListener() {
            Log.d(tag, "Pickit on start listener (Creando archivo de descarga)")
        }
        override fun PickiTonProgressUpdate(progress: Int) {
            Log.d(tag, "Pickit on progress update (Progreso de descarga $progress")
            downloadFragment.updateProgressBar(progress)
            if(progress==100) downloadFragment.dismiss()
        }
        override fun PickiTonCompleteListener(
            path: String?,
            wasDriveFile: Boolean,
            wasUnknownProvider: Boolean,
            wasSuccessful: Boolean,
            Reason: String?
        ) {
            Log.d(tag, "Pickit on complete listener Ruta: $path")
            viewModel.setPath(path)
            previewFile()
        }
    }


    override fun onStart() {
        super.onStart()
        if(viewModel.getType() != ResourceTypeEnum.NOT_DEFINED){
            previewFile()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print)

        viewModel = ViewModelProvider(this).get(PrintActViewModel::class.java)

        buttonChooseFile = findViewById(R.id.act_print_chooseFileButton)
        buttonPrint = findViewById(R.id.act_print_printFab)
        buttonShare = findViewById(R.id.act_scan_preview_shareFab)
        imagePreview = findViewById(R.id.act_print_imagePreview)
        webPreview = findViewById(R.id.act_print_webPreview)
        pdfView = findViewById(R.id.act_print_PDFView)

        pickit = PickiT(this, pickitListener, this)

        webPreview.visibility = View.INVISIBLE
        imagePreview.visibility = View.INVISIBLE

        //Pinch gesture for zoom is set on the root layout
        imageViewOrigX = imagePreview.x
        imageViewOrigY = imagePreview.y
        rootLayout = findViewById<ZoomLayout>(R.id.act_print_root)
        rootLayout.setImageView(imagePreview)

        //ChooseFile button listener
        buttonChooseFile.setOnClickListener { getFile() }

        //Share button listener
        buttonShare.setOnClickListener {
            //Nothing was selected
            if (viewModel.getType() == ResourceTypeEnum.NOT_DEFINED) {
                showFileNotChosenDialog()
            } else {

                val file = File(viewModel.getPath()!!)
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
        }//Share

        //Print button listener
        buttonPrint.setOnClickListener {
            //Check for the resource type (photo, doc...)
            when (viewModel.getType()) {

                ResourceTypeEnum.NOT_DEFINED->{
                    showFileNotChosenDialog()
                }

                //Printing an image
                ResourceTypeEnum.IMAGE -> {
                    val printHelper = PrintHelper(this@PrintActivity)
                    printHelper.scaleMode = PrintHelper.SCALE_MODE_FILL
                    val photo: Bitmap = MediaStore.Images.Media.getBitmap(
                        this@PrintActivity.contentResolver,
                        viewModel.getUri()
                    )
                    printHelper.printBitmap("Imagen", photo)
                }//Image

                //Printing a PDF
                ResourceTypeEnum.PDF -> {
                    val printManager: PrintManager = this@PrintActivity.getSystemService(
                        Context.PRINT_SERVICE
                    ) as PrintManager
                    val printAdapter = PdfDocumentAdapter(viewModel.getPath()!!)
                    printManager.print("PDF", printAdapter, PrintAttributes.Builder().build())
                }//PDF

                //Printing an HTML
                ResourceTypeEnum.HTML -> {
                    //Creating a webView for printing
                    val webView = WebView(this@PrintActivity)

                    webView.settings.allowContentAccess = true
                    webView.settings.allowFileAccess = true

                    webView.webViewClient = object : WebViewClient() {

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ) = false

                        override fun onPageFinished(view: WebView, url: String) {
                            Log.i(tag, "page finished loading $url")
                            //Create the print job
                            // Get a PrintManager instance
                            val printManager: PrintManager = this@PrintActivity.getSystemService(
                                Context.PRINT_SERVICE
                            ) as PrintManager

                            val printAdapter = view.createPrintDocumentAdapter("${getString(R.string.app_name)} HTML")
                            printManager.print("Documento HTML", printAdapter, PrintAttributes.Builder().build())
                        }
                    }
                    webView.loadUrl(viewModel.getUri().toString())
                }//HTML
            }
        }
    }

    private fun previewFile(){

        //Create file and get extension
        val file = File(viewModel.getPath()!!)
        viewModel.setUri(Uri.fromFile(file))
        val extension = file.extension
        Log.d(tag, "extension  $extension")

        //reset previews
        webPreview.visibility = View.GONE
        imagePreview.visibility = View.GONE
        pdfView.visibility = View.GONE
        imagePreview.scaleX = 1F
        imagePreview.scaleY = 1F
        imagePreview.x = imageViewOrigX
        imagePreview.y = imageViewOrigY

        //Continue depending on the file extension
        when (extension.toLowerCase(Locale.ROOT)) {
            "jpg", "jpeg", "jpe", "png", "bmp", "gif", "webp" -> {
                viewModel.setType(ResourceTypeEnum.IMAGE)
                //Log.d(tag, "image uri $resourceUri")

                //Make the image preview bigger
                imagePreview.visibility = View.VISIBLE
                imagePreview.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                imagePreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                rootLayout.setImageView(imagePreview)
            }
            "pdf" -> {
                viewModel.setType(ResourceTypeEnum.PDF)
                pdfView.visibility = View.VISIBLE
                pdfView.fromFile(file).load()
            }
            "html" ->{
                viewModel.setType(ResourceTypeEnum.HTML)
                webPreview.visibility = View.VISIBLE
                webPreview.settings.allowContentAccess = true
                webPreview.settings.allowFileAccess = true
                webPreview.loadUrl(viewModel.getUri().toString())
            }
            else -> {
                Log.d(tag, "Extension no soportada")
                showExtensionNotSuppertedDialog()
                clearViewModelData()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == chooseFileActRequestCode) {
            if (resultCode == RESULT_OK) {

                //Remove a image from the imagePreview if there was any
                imagePreview.setImageDrawable(null)
                webPreview.loadUrl("about:blank")

                pickit.getPath(data!!.data, Build.VERSION.SDK_INT)
                Log.d(tag, "Path original = ${data.data}")

            }
        }//ChooseFileRequestCode
    }//onActResult

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            requestExternalStoragePermissionCode -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //Pick the file
                    val intent = Intent(Intent.ACTION_GET_CONTENT)
                    intent.type = "*/*"
                    startActivityForResult(intent, chooseFileActRequestCode)
                }else{
                    endActivityNoPermission()
                }
            }
        }
    }


    /**
     * Fun that opens the intent to pick a file
     */
    private fun getFile(){
        /*//Check permission
        if(ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_DENIED){
            val permissionHelper = PermissionHelper(
                this@PrintActivity,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                requestExternalStoragePermissionCode,
                getString(R.string.permission_extStorageDeniedTitle),
                getString(R.string.permission_extStorageDeniedMsg)
            )
            permissionHelper.checkAndAskForPermission()
        }else{
            //Seleccionar archivo
            val intent = Intent(Intent.ACTION_GET_CONTENT)*/
            //intent.type = "*/*"
           /* startActivityForResult(intent, chooseFileActRequestCode)
        }*/

        when {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                // You can use the API that requires the permission.
                //Seleccionar archivo
                val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                startActivityForResult(intent, chooseFileActRequestCode)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                //Show explanatory message
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle(getString(R.string.permission_extStorageDeniedTitle)).setMessage(getString(R.string.permission_extStorageDeniedMsg))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(this,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            requestExternalStoragePermissionCode) }
                    .setNegativeButton(android.R.string.cancel) { _, _ ->
                        endActivityNoPermission() }
                builder.create().show()
            }
            else -> {
                // You can directly ask for the permission.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    requestExternalStoragePermissionCode)
            }
        }
    }

    private fun endActivityNoPermission(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.permission_ExtStorageDenied_endAct))
            .setPositiveButton(android.R.string.ok){ _, _ ->
                startActivity(Intent(applicationContext, MainActivity::class.java))
            }.show()
    }

    //On back pressed go to main activity
    override fun onBackPressed() {
        super.onBackPressed()
        removeTempFiles()
        startActivity(Intent(this, MainActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        removeTempFiles()
    }

    private fun removeTempFiles(){
        Log.d(tag, "Removing temporary files")
        pickit.deleteTemporaryFile(this)
    }

    private fun showFileNotChosenDialog(){
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {

                setMessage(R.string.PrintAct_FileNotChosen_msg).setTitle(R.string.PrintAct_FileNotChosen_title)

                setNeutralButton(R.string.accept
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            }
            // Create the AlertDialog
            builder.create()
        }
        alertDialog?.show()
    }

    private fun showExtensionNotSuppertedDialog(){
        val alertDialog: AlertDialog? = this.let {
            val builder = AlertDialog.Builder(it)
            builder.apply {

                setMessage(R.string.PrintAct_extensionNotSupported_msg).setTitle(R.string.PrintAct_extensionNotSupported_title)

                setPositiveButton(R.string.accept
                ) { dialog, _ ->
                    dialog.dismiss()
                }
            }
            // Create the AlertDialog
            builder.create()
        }
        alertDialog?.show()
    }

    private fun clearViewModelData(){
        viewModel.setUri(null)
        viewModel.setPath(null)
        viewModel.setType(ResourceTypeEnum.NOT_DEFINED)
    }

}