package com.example.movil


import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.print.PrintHelper
import java.io.File


class PrintActivity : AppCompatActivity() {

    private val TAG = "--- PrintActivity ---"

    lateinit var buttonChooseImage: Button
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

        buttonChooseImage = findViewById(R.id.act_print_chooseImageButton)
        buttonChooseFile = findViewById(R.id.act_print_chooseFileButton)
        buttonSendEmail = findViewById(R.id.act_print_sendEmailButton)
        buttonPrint = findViewById(R.id.act_print_printButton)
        imagePreview = findViewById(R.id.act_print_imagePreview)


        //ChooseImage button listener
        buttonChooseImage.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "image/*"
                startActivityForResult(intent, chooseImageRequestCode)
            }
        })

        buttonChooseFile.setOnClickListener(object : View.OnClickListener {
            override fun onClick(p0: View?) {
                if(ContextCompat.checkSelfPermission(
                        this@PrintActivity,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                 != PackageManager.PERMISSION_GRANTED)
                 {
                    //Check if we need to show a message
                     if (ActivityCompat.shouldShowRequestPermissionRationale(
                             this@PrintActivity, Manifest.permission.READ_EXTERNAL_STORAGE
                         )
                     ) {
                         showExplanation(
                             "Acceso al almacenamiento exteno denegado",
                             "Se necesita acceso al almacenamiento exteno",
                             Manifest.permission.READ_EXTERNAL_STORAGE,
                             requestExternalStoragePermissionCode
                         )
                     } else {
                         requestPermission(Manifest.permission.READ_EXTERNAL_STORAGE, requestExternalStoragePermissionCode)
                     }
                     Log.d(TAG, "No se tienen permisos sobre el almacenamiento externo")
                     return
                 }
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

        when (requestCode) {
            chooseImageRequestCode -> if (data != null) {
                resourceType = ResourceTypeEnum.IMAGE
                imageUri = data.data
                imagePreview.setImageURI(imageUri)
            }

            chooseFileRequestCode -> if (data != null) {
                resourceType = ResourceTypeEnum.PDF
                val fileUri = data.data

                Log.d(TAG, "file uri: " + fileUri)

                /* Render the first page of the document */
                //Get the PDF path from the uri

                resourcePath = getRealPath(this@PrintActivity, fileUri!!)
                Log.d(TAG, "file path: $resourcePath")


                val file = File(resourcePath)

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
        }
    }

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
        ActivityCompat.requestPermissions(this@PrintActivity, arrayOf(name), code)
        recreate()
    }

    fun getRealPath(context: Context, uri: Uri): String? {

        val isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            } else if (isDownloadsDocument(uri)) {
                var cursor: Cursor? = null
                try {
                    cursor = context.contentResolver.query(
                        uri,
                        arrayOf(MediaStore.MediaColumns.DISPLAY_NAME),
                        null,
                        null,
                        null
                    )
                    cursor!!.moveToNext()
                    val fileName = cursor.getString(0)
                    val path = Environment.getExternalStorageDirectory()
                        .toString() + "/Download/" + fileName
                    if (!TextUtils.isEmpty(path)) {
                        return path
                    }
                } finally {
                    cursor?.close()
                }
                val id = DocumentsContract.getDocumentId(uri)
                if (id.startsWith("raw:")) {
                    return id.replaceFirst("raw:".toRegex(), "")
                }
                val contentUri = ContentUris.withAppendedId(
                    Uri.parse("content://downloads"), java.lang.Long.valueOf(
                        id
                    )
                )

                return getDataColumn(context, contentUri, null, null)
            } else if (isMediaDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                val type = split[0]

                var contentUri: Uri? = null
                when (type) {
                    "image" -> contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    "video" -> contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    "audio" -> contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }

                val selection = "_id=?"
                val selectionArgs = arrayOf(split[1])

                return getDataColumn(context, contentUri, selection, selectionArgs)
            }// MediaProvider
            // DownloadsProvider
        } else if ("content".equals(uri.scheme!!, ignoreCase = true)) {

            // Return the remote address
            return if (isGooglePhotosUri(uri)) uri.lastPathSegment else getDataColumn(
                context,
                uri,
                null,
                null
            )
        } else if ("file".equals(uri.scheme!!, ignoreCase = true)) {
            return uri.path
        }// File
        // MediaStore (and general)

        return null
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context       The context.
     * @param uri           The Uri to query.
     * @param selection     (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     * @author Niks
     */
    private fun getDataColumn(
        context: Context, uri: Uri?, selection: String?,
        selectionArgs: Array<String>?
    ): String? {

        var cursor: Cursor? = null
        val column = "_data"
        val projection = arrayOf(column)

        try {
            cursor = context.contentResolver.query(
                uri!!,
                projection,
                selection,
                selectionArgs,
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndexOrThrow(column)
                return cursor.getString(index)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private fun isDownloadsDocument(uri: Uri): Boolean {
        return "com.android.providers.downloads.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private fun isMediaDocument(uri: Uri): Boolean {
        return "com.android.providers.media.documents" == uri.authority
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private fun isGooglePhotosUri(uri: Uri): Boolean {
        return "com.google.android.apps.photos.content" == uri.authority
    }


}