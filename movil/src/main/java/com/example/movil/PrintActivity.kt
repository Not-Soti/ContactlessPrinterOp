package com.example.movil


import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.AsyncTask
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
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.getSystemService
import androidx.print.PrintHelper
import com.izettle.html2bitmap.Html2Bitmap
import com.izettle.html2bitmap.content.WebViewContent
import java.io.File
import java.util.*


class PrintActivity : AppCompatActivity() {

    private val tag = "--- PrintActivity ---"

    //lateinit var buttonChooseImage: Button
    lateinit var buttonChooseFile: Button
    lateinit var buttonSendEmail: Button
    lateinit var buttonPrint: Button
    lateinit var imagePreview: ImageView
    lateinit var webPreview: WebView


    //Request codes for each activity with a result
    val chooseImageRequestCode = 0
    val chooseFileRequestCode = 1


    var resourceUri: Uri? = null //selected resource uri
    var resourcePath : String? = null //using the uri does not work when trying to print documents
    var resourceType = ResourceTypeEnum.NOT_DEFINED

    val requestExternalStoragePermissionCode = 10 //Code uses when asking for permissions

    //Enum needed to check the extension of the selected file to print
    enum class ResourceTypeEnum {
        NOT_DEFINED,
        IMAGE,
        PDF,
        HTML,
        DOCUMENT
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print)

        //buttonChooseImage = findViewById(R.id.act_print_chooseImageButton)
        buttonChooseFile = findViewById(R.id.act_print_chooseFileButton)
        buttonSendEmail = findViewById(R.id.act_print_sendEmailButton)
        buttonPrint = findViewById(R.id.act_print_printButton)
        imagePreview = findViewById(R.id.act_print_imagePreview)
        webPreview = findViewById(R.id.act_print_webPreview)

        //ChooseFile button listener
        buttonChooseFile.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                //Check for permission
                val permissionHelper = PermissionHelper(
                    this@PrintActivity,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    requestExternalStoragePermissionCode,
                    "Acceso al almacenamiento externo denegado",
                    "Se necesita acceso al almacenamiento exteno"
                )
                permissionHelper.checkAndAskForPermission()

                //Storage access is granted
                val intent = Intent(Intent.ACTION_PICK)
                //val intent = Intent(Intent.ACTION_GET_CONTENT)
                intent.type = "*/*"
                startActivityForResult(intent, chooseFileRequestCode)
            }
        })

        //SendEmail button listener
        buttonSendEmail.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {

                //Nothing was selected
                if (resourceType == ResourceTypeEnum.NOT_DEFINED) {
                    Toast.makeText(
                        this@PrintActivity,
                        "Selecciona algún archivo",
                        Toast.LENGTH_LONG
                    ).show()
                } else {

                    //Open email app and load info
                    val emailIntent = Intent(Intent.ACTION_SEND)
                    //emailIntent.type = "message/rfc822" //Shows only email clients
                    emailIntent.type = "*/*"

                    val to: Array<String> = emptyArray()
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, to)

                    if (resourceType == ResourceTypeEnum.IMAGE) {
                        emailIntent.putExtra(Intent.EXTRA_STREAM, resourceUri)
                    } else if (resourceType == ResourceTypeEnum.PDF) {
                        emailIntent.putExtra(Intent.EXTRA_STREAM, resourceUri)
                    }

                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Sujeto")
                    startActivity(Intent.createChooser(emailIntent, "Enviar email"))
                }
            }

        })

        //Print button listener
        buttonPrint.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {

                //Check for the resource type (photo, doc...)
                when (resourceType) {

                    //Printing an image
                    ResourceTypeEnum.IMAGE -> {
                        var printHelper: PrintHelper = PrintHelper(this@PrintActivity)
                        printHelper.scaleMode = PrintHelper.SCALE_MODE_FILL
                        var photo: Bitmap = MediaStore.Images.Media.getBitmap(
                            this@PrintActivity.contentResolver,
                            resourceUri
                        );
                        printHelper.printBitmap("Imagen", photo)
                    }

                    //Printing a PDF
                    ResourceTypeEnum.PDF -> {
                        val printManager: PrintManager = this@PrintActivity.getSystemService(
                            Context.PRINT_SERVICE
                        ) as PrintManager
                        val printAdapter = PdfDocumentAdapter(resourcePath!!)
                        printManager.print("PDF", printAdapter, PrintAttributes.Builder().build())
                    }

                    //Printing an HTML
                    ResourceTypeEnum.HTML -> {
                        //Creating a webView for printing
                        val webView = WebView(this@PrintActivity)
                        webView.webViewClient = object : WebViewClient() {

                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

                            override fun onPageFinished(view: WebView, url: String) {
                                Log.i(tag, "page finished loading $url")
                                createWebPrintJob(view)
                                //mWebView = null
                            }
                        }

                        webView.loadUrl(resourceUri.toString()!!)

                    }
                }
            }
        })
    }


    //Overriding function to get images or files from activity results
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Remove a image from the imagePreview if there was any
        imagePreview.setImageDrawable(null)
        webPreview.loadUrl("about:blank")

        when (requestCode) {
            //Document selected
            chooseFileRequestCode -> if (data != null) {
                resourceUri = data.data

                Log.d(tag, "file uri: " + resourceUri)

                //Get the file path from the uri
                val pathUtils = RealPathUtils(this, resourceUri!!)
                resourcePath = pathUtils.getRealPath(this@PrintActivity, resourceUri!!)
                Log.d(tag, "file path: $resourcePath")

                val file = File(resourcePath)
                val extension = file.extension
                Log.d(tag, "extension  $extension")

                //Continue depending on the file extension
                when (extension.toLowerCase()) {
                    "jpg", "jpeg", "jpe", "png", "bmp", "gif", "webp" -> {
                        resourceType = ResourceTypeEnum.IMAGE
                        Log.d(tag, "image uri $resourceUri")

                        //Make the image preview bigger
                        imagePreview.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        imagePreview.setImageURI(resourceUri)
                    }
                    "pdf" -> {
                        resourceType = ResourceTypeEnum.PDF
                        //Make the image preview bigger
                        imagePreview.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                        previewPdf(file)
                    }
                    "html" ->{
                        resourceType = ResourceTypeEnum.HTML
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
        }
    }


    /**
     * Function to operate when a pdf is selected
     */
    private fun previewPdf(file: File){

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

    private fun createWebPrintJob(webView: WebView){
        //Create the print job
        // Get a PrintManager instance
        val printManager: PrintManager = this@PrintActivity.getSystemService(
            Context.PRINT_SERVICE
        ) as PrintManager

        val printAdapter = webView.createPrintDocumentAdapter("${getString(R.string.app_name)} HTML")
        printManager.print("Documento HTML", printAdapter, PrintAttributes.Builder().build())

    }

    //On back pressed go to main activity
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
    }



}