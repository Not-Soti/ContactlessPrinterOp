package com.example.movil.printActivity


import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.example.movil.MainActivity
import com.example.movil.PermissionHelper
import com.example.movil.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import java.io.File
import java.util.*


class PrintActivity : AppCompatActivity() {

    private val tag = "--- PrintActivity ---"

    private lateinit var buttonChooseFile: Button
    private lateinit var buttonShare : FloatingActionButton
    private lateinit var buttonPrint : FloatingActionButton
    private lateinit var imagePreview: ImageView
    private lateinit var webPreview: WebView
    private lateinit var downloadFragment: DownloadingFileFragment


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
            Toast.makeText(applicationContext, "Ruta: $path", Toast.LENGTH_LONG).show()
            resourcePath = path
            previewFile()
        }

    }

    //Request codes for each activity with a result
    private val chooseFileRequestCode = 1

    private var resourceUri: Uri? = null //selected resource uri
    private var resourcePath : String? = null //using the uri does not work when trying to print documents
    private var resourceType = ResourceTypeEnum.NOT_DEFINED

    private val requestExternalStoragePermissionCode = 10 //Code uses when asking for permissions

    //Enum needed to check the extension of the selected file to print
    enum class ResourceTypeEnum {
        NOT_DEFINED,
        IMAGE,
        PDF,
        HTML,
        DOC
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print)

        //buttonChooseImage = findViewById(R.id.act_print_chooseImageButton)
        buttonChooseFile = findViewById(R.id.act_print_chooseFileButton)
        //buttonSendEmail = findViewById(R.id.act_print_sendEmailButton)
        //buttonPrint = findViewById(R.id.act_print_printButton)
        buttonPrint = findViewById(R.id.act_print_printFab)
        buttonShare = findViewById(R.id.act_print_shareFab)
        imagePreview = findViewById(R.id.act_print_imagePreview)
        webPreview = findViewById(R.id.act_print_webPreview)

        pickit = PickiT(this, pickitListener, this)

        webPreview.visibility = View.INVISIBLE
        imagePreview.visibility = View.INVISIBLE

        //ChooseFile button listener
        buttonChooseFile.setOnClickListener { getFile() }

        //SendEmail button listener
        buttonShare.setOnClickListener {
            //Nothing was selected
            if (resourceType == ResourceTypeEnum.NOT_DEFINED) {
                Toast.makeText(
                    this@PrintActivity,
                    "Selecciona algún archivo",
                    Toast.LENGTH_LONG
                ).show()
            } else {

                val file = File(resourcePath!!)
                val uriAux =
                    FileProvider.getUriForFile(applicationContext, "$packageName.provider", file)

                //Open email app and load info
                val emailIntent = Intent(Intent.ACTION_SEND)
                emailIntent.type = "*/*"

                val to: Array<String> = emptyArray()
                emailIntent.putExtra(Intent.EXTRA_EMAIL, to)

                emailIntent.putExtra(Intent.EXTRA_STREAM, uriAux)


                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Sujeto")
                startActivity(Intent.createChooser(emailIntent, "Enviar"))
            }
        }

        //Print button listener
        buttonPrint.setOnClickListener {
            //Check for the resource type (photo, doc...)
            when (resourceType) {

                ResourceTypeEnum.NOT_DEFINED->{
                    Toast.makeText(
                        this@PrintActivity,
                        "Selecciona algún archivo",
                        Toast.LENGTH_LONG
                    ).show()
                }

                //Printing an image
                ResourceTypeEnum.IMAGE -> {
                    val printHelper = PrintHelper(this@PrintActivity)
                    printHelper.scaleMode = PrintHelper.SCALE_MODE_FILL
                    val photo: Bitmap = MediaStore.Images.Media.getBitmap(
                        this@PrintActivity.contentResolver,
                        resourceUri
                    )
                    printHelper.printBitmap("Imagen", photo)
                }//Image

                //Printing a PDF
                ResourceTypeEnum.PDF -> {
                    val printManager: PrintManager = this@PrintActivity.getSystemService(
                        Context.PRINT_SERVICE
                    ) as PrintManager
                    val printAdapter = PdfDocumentAdapter(resourcePath!!)
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
                            createWebPrintJob(view)
                            //mWebView = null
                        }
                    }
                    webView.loadUrl(resourceUri.toString())
                }//HTML
            }
        }
    }

    private fun previewFile(){

        //Create file and get extension
        val file = File(resourcePath!!)
        resourceUri = Uri.fromFile(file)
        val extension = file.extension
        Log.d(tag, "extension  $extension")
        webPreview.visibility = View.INVISIBLE
        imagePreview.visibility = View.INVISIBLE

        //Continue depending on the file extension
        when (extension.toLowerCase(Locale.ROOT)) {
            "jpg", "jpeg", "jpe", "png", "bmp", "gif", "webp" -> {
                resourceType = ResourceTypeEnum.IMAGE
                //Log.d(tag, "image uri $resourceUri")

                //Make the image preview bigger
                imagePreview.visibility = View.VISIBLE
                imagePreview.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                imagePreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
            }
            "pdf" -> {
                resourceType = ResourceTypeEnum.PDF

                //Make the image preview bigger
                imagePreview.visibility = View.VISIBLE
                imagePreview.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

                /* Render the first page of the document */
                // This is the PdfRenderer we use to render the PDF.
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
            "html" ->{
                resourceType = ResourceTypeEnum.HTML
                webPreview.visibility = View.VISIBLE
                webPreview.settings.allowContentAccess = true
                webPreview.settings.allowFileAccess = true
                webPreview.loadUrl(resourceUri.toString())
                Toast.makeText(this@PrintActivity,"Seleccionado HTML", Toast.LENGTH_LONG).show()
            }
            else -> {
                Log.d(tag, "Extension no soportada")
                Toast.makeText(
                    this@PrintActivity,
                    "Extension no soportada",
                    Toast.LENGTH_LONG
                ).show()//TODO crear dialogo de extensiones soportada
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == chooseFileRequestCode) {
            if (resultCode == RESULT_OK) {

                //Remove a image from the imagePreview if there was any
                imagePreview.setImageDrawable(null)
                webPreview.loadUrl("about:blank")

                pickit.getPath(data!!.data, Build.VERSION.SDK_INT)
                Log.d(tag, "Path original = ${data.data}")

            }
        }//ChooseFileRequestCode
    }//onActResult

    private fun createWebPrintJob(webView: WebView){
        //Create the print job
        // Get a PrintManager instance
        val printManager: PrintManager = this@PrintActivity.getSystemService(
            Context.PRINT_SERVICE
        ) as PrintManager

        val printAdapter = webView.createPrintDocumentAdapter("${getString(R.string.app_name)} HTML")
        printManager.print("Documento HTML", printAdapter, PrintAttributes.Builder().build())

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            requestExternalStoragePermissionCode -> getFile()
        }
    }


    /**
     * Fun that opens the intent to pick a file
     */
    private fun getFile(){
        //Check permission
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

            //val intent = Intent(Intent.ACTION_PICK)
            //intent.type = "*/*"
            //startActivityForResult(intent, chooseFileRequestCode)

            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            startActivityForResult(intent, chooseFileRequestCode)
        }

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
}