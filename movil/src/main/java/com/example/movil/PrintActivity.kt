package com.example.movil


import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.print.PrintHelper
import java.io.File


class PrintActivity : AppCompatActivity() {

    private val tag = "--- PrintActivity ---"

    //lateinit var buttonChooseImage: Button
    lateinit var buttonChooseFile: Button
    lateinit var buttonSendEmail: Button
    lateinit var buttonPrint: Button
    lateinit var imagePreview: ImageView


    //Request codes for each activity with a result
    val chooseImageRequestCode = 0
    val chooseFileRequestCode = 1

    var imageUri: Uri? = null //selected image uri
    var resourcePath : String? = "" //using the uri does not work when using documents
    var resourceType = ResourceTypeEnum.NOT_DETERMINED

    val requestExternalStoragePermissionCode = 10 //Code uses when asking for permissions

    //Enum needed to check the extension of the selected file to print
    enum class ResourceTypeEnum {
        NOT_DETERMINED,
        IMAGE,
        PDF,
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


        //ChooseImage button listener
        /*
        buttonChooseImage.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, chooseImageRequestCode)
            }
        })
        */ */

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
                intent.type = "*/*"
                startActivityForResult(intent, chooseFileRequestCode)
            }
        })

        //Print button listener
        buttonPrint.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {

                //Check for the resource type (photo, doc...)
                when (resourceType) {

                    //Printing a image
                    ResourceTypeEnum.IMAGE -> {
                        var printHelper: PrintHelper = PrintHelper(this@PrintActivity)
                        printHelper.scaleMode = PrintHelper.SCALE_MODE_FILL
                        var photo: Bitmap = MediaStore.Images.Media.getBitmap(
                            this@PrintActivity.contentResolver,
                            imageUri
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
                }
            }

        })
    }

    //Overriding function to get images or files from activity results
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //Remove a image from the imagePreview if there was any
        imagePreview.setImageDrawable(null)

        when (requestCode) {
            //Image selected
                /*
            chooseImageRequestCode -> if (data != null) {
                resourceType = ResourceTypeEnum.IMAGE
                imageUri = data.data
                Log.d(tag, "image uri $imageUri")
                imagePreview.setImageURI(imageUri)
            }
            */

            //Document selected
            chooseFileRequestCode -> if (data != null) {
                val fileUri = data.data

                Log.d(tag, "file uri: " + fileUri)

                //Get the file path from the uri
                val pathUtils = RealPathUtils(this, fileUri!!)
                resourcePath = pathUtils.getRealPath(this@PrintActivity, fileUri!!)
                Log.d(tag, "file path: $resourcePath")

                val file = File(resourcePath)
                val extension = file.extension
                Log.d(tag,"extension  $extension")

                //Continue depending on the file extension
                when(extension.toLowerCase()){
                    "jpg","jpeg", "jpe", "png","bmp", "gif", "webp" -> {
                        resourceType = ResourceTypeEnum.IMAGE
                        imageUri = data.data
                        Log.d(tag, "image uri $imageUri")
                        imagePreview.setImageURI(imageUri)
                    }
                    "pdf" -> {
                        resourceType = ResourceTypeEnum.PDF
                        processPdf(file)
                    }
                    else -> {
                        Log.d(tag, "Extension no soportada")
                        Toast.makeText(this@PrintActivity, "Extension no soportada", Toast.LENGTH_LONG)//TODO crear dialogo de extensiones soportada
                    }
                }
            }
        }
    }

    /**
     * Function to operate when a pdf is selected
     */
    private fun processPdf(file : File){

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
    }

    //On back pressed go to main activity
    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
    }

}