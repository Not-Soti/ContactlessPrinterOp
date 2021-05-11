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
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.provider.Settings
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
import com.example.movil.BuildConfig
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

    //Variables that save the preview image position
    private var imageViewOrigX = 0F
    private var imageViewOrigY = 0F

    private val chooseFileActRequestCode = 1 //Code used in the onActivityResult petition
    private val requestExternalStoragePermissionCode = 2 //Code used when asking for permissions

    private lateinit var viewModel : PrintActViewModel

    /*Object used to return real file paths from their URI
    If the file is chosen from external file providers (Drive or similar),
    downloads and saves them on temporal files */
    private lateinit var pickit : PickiT
    private val pickitListener = object: PickiTCallbacks{
        override fun PickiTonUriReturned() {
            //Used when the file is picked from the Cloud

            //Create downloading fragment
            val fragmentManager = supportFragmentManager
            val fragmentTransaction = fragmentManager.beginTransaction()
            downloadFragment = DownloadingFileFragment()
            downloadFragment.show(fragmentTransaction, "downloadFragment")
        }

        override fun PickiTonStartListener() {
            //Called when the selected file is not local, and the file creation starts
        }
        override fun PickiTonProgressUpdate(progress: Int) {
            //Called when the file is not local, updating the download progress
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
            //Called when the path is got. If the file is local, this is called directly
            //Sets the path and renders the image preview
            viewModel.setPath(path)
            previewFile()
        }
    }


    override fun onStart() {
        super.onStart()
        //If a file was selected previously, display it
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

        //Sets the preview elements invisible, making them visible when needed
        webPreview.visibility = View.INVISIBLE
        imagePreview.visibility = View.INVISIBLE

        //Ask permissions for android 11+, since special permissions are needed
        askAccessAllFilesPermission()

        //Pinch gesture for zoom is set on the root layout
        imageViewOrigX = imagePreview.x //Saves the original image coordinates
        imageViewOrigY = imagePreview.y
        rootLayout = findViewById<ZoomLayout>(R.id.act_print_root) //creates the Zoom layout and sets the image
        rootLayout.setImageView(imagePreview)

        //ChooseFile button listener
        buttonChooseFile.setOnClickListener {
            askPermissions() //Firstly checks if file permissiosn are granted
        }

        //Share button listener
        buttonShare.setOnClickListener {
            //Nothing was selected
            if (viewModel.getType() == ResourceTypeEnum.NOT_DEFINED) {
                showFileNotChosenDialog()
            } else {
                //If a file was selected, creates a File Object from it, and uses the URI of the
                //new file to share it
                val file = File(viewModel.getPath()!!)
                val uriAux =
                    FileProvider.getUriForFile(applicationContext, "$packageName.provider", file)

                //Open share app chooser
                val emailIntent = Intent(Intent.ACTION_SEND)
                emailIntent.type = "*/*"

                //Sets intent info
                val to: Array<String> = emptyArray()
                emailIntent.putExtra(Intent.EXTRA_EMAIL, to) //sets receivers (not used)
                emailIntent.putExtra(Intent.EXTRA_STREAM, uriAux) //sets the image uri
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "") //sets the subject (not used)
                startActivity(Intent.createChooser(emailIntent, getString(R.string.PrintAct_shareFab_cd))) //Creates the ap choser
            }
        }//Share button listener

        //Print button listener
        buttonPrint.setOnClickListener {
            //Check for the resource type (photo, doc...)
            when (viewModel.getType()) {
                //If no file was chosen, show dialog
                ResourceTypeEnum.NOT_DEFINED->{
                    showFileNotChosenDialog()
                }

                //Printing an image
                //Creates a bitmap from the chosen image uri and prints it
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
                //Creates the print adapter from the PDF path and prints it
                ResourceTypeEnum.PDF -> {
                    val printManager: PrintManager = this@PrintActivity.getSystemService(
                        Context.PRINT_SERVICE
                    ) as PrintManager
                    val printAdapter = PdfDocumentAdapter(viewModel.getPath()!!)
                    printManager.print("PDF", printAdapter, PrintAttributes.Builder().build())
                }//PDF

                //Printing an HTML
                ResourceTypeEnum.HTML -> {
                    //Creates a webView, sets the chosent html from its uri and prints it
                    val webView = WebView(this@PrintActivity)

                    webView.settings.allowContentAccess = true
                    webView.settings.allowFileAccess = true

                    webView.webViewClient = object : WebViewClient() {

                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest
                        ) = false

                        override fun onPageFinished(view: WebView, url: String) {
                            //When the page is completely loaded, prints it

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
                    webView.loadUrl(viewModel.getUri().toString()) //sets the page in the webView
                }//HTML
            }
        }//Print button listener
    }

    /**
     * Method used to get the chosen file and show it
     */
    private fun previewFile(){
        //Create file from the chosen URI and get the extension (Ex: pdf, jpg...)
        val file = File(viewModel.getPath()!!)
        viewModel.setUri(Uri.fromFile(file))
        val extension = file.extension
        Log.d(tag, "extension  $extension")

        //reset previews in order to only make the correct one visible
        webPreview.visibility = View.GONE
        imagePreview.visibility = View.GONE
        pdfView.visibility = View.GONE
        imagePreview.scaleX = 1F
        imagePreview.scaleY = 1F
        imagePreview.x = imageViewOrigX //resets the image coordinates in case they changed
        imagePreview.y = imageViewOrigY

        //Continue depending on the file extension, gettint it in lowercase
        when (extension.toLowerCase(Locale.ROOT)) {
            "jpg", "jpeg", "jpe", "png", "bmp", "gif", "webp" -> {
                //Sets the type as an image
                viewModel.setType(ResourceTypeEnum.IMAGE)
                //Log.d(tag, "image uri $resourceUri")

                //Make the image preview visible
                imagePreview.visibility = View.VISIBLE
                imagePreview.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                imagePreview.setImageBitmap(BitmapFactory.decodeFile(file.absolutePath)) //sets the chosen image
                rootLayout.setImageView(imagePreview) //sets the image in the zoom layout too
            }
            "pdf" -> {
                //Sets the type as pdf, shows the preview container and sets the pdf in it
                viewModel.setType(ResourceTypeEnum.PDF)
                pdfView.visibility = View.VISIBLE
                pdfView.fromFile(file).load()
            }
            "html" ->{
                //Sets the type as html, shows the webVeiw and sets the loads the file in it
                viewModel.setType(ResourceTypeEnum.HTML)
                webPreview.visibility = View.VISIBLE
                webPreview.settings.allowContentAccess = true
                webPreview.settings.allowFileAccess = true
                webPreview.loadUrl(viewModel.getUri().toString())
            }
            else -> {
                //if the file extension is not supported, shows a message and resets the viewModel info
                Log.d(tag, "Extension no soportada")
                showExtensionNotSuppertedDialog()
                clearViewModelData()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //If a file is correctly chosen
        if(requestCode == chooseFileActRequestCode) {
            if (resultCode == RESULT_OK) {

                //Remove a image from the imagePreview if there was any
                imagePreview.setImageDrawable(null)
                webPreview.loadUrl("about:blank")

                //If the file is correctly chosen, calls pickit in order to get it's real path
                pickit.getPath(data!!.data, Build.VERSION.SDK_INT)
            }
        }//ChooseFileRequestCode
    }

    //Method called when the user chooses to give or not to give permissions
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            requestExternalStoragePermissionCode -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    //If permissions are granted, shows the file picker
                    pickFile()
                }else{
                    //If permissions are not granted, shows a dialog and ends the activity
                    endActivityNoPermission()
                }
            }
        }
    }

    /**
     * Methos that starts the file picker activity
     */
    private fun pickFile(){
        //Pick the file
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        startActivityForResult(intent, chooseFileActRequestCode)
    }


    /**
     * Method that ask for permissions
     */
    private fun askPermissions(){

        //In android 11 manage all files permission is needed
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){

            if(Environment.isExternalStorageManager()){
                //If the permission is granted, starts the file picker activity
                pickFile()
            }else {
                //Otherwise the permission is requested
                askAccessAllFilesPermission()
            }

        }else {
            //In lower Android vesions, external storage permissions are requested
            when {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //If the permission is granted, starts the file picker activity
                    pickFile()
                }
                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) -> {
                    //Show explanatory dialog if the system decides its
                    val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                    builder.setTitle(getString(R.string.permission_extStorageDeniedTitle))
                        .setMessage(getString(R.string.permission_extStorageDeniedMsg))
                        .setPositiveButton(android.R.string.ok) { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                                requestExternalStoragePermissionCode
                            )
                        }
                        .setNegativeButton(android.R.string.cancel) { _, _ ->
                            //If the permission is not granted, shows a dialog that ends the activity
                            endActivityNoPermission()
                        }
                    builder.create().show() //Shows the dialog
                }
                else -> {
                    // The permissions are directly asked for
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                        requestExternalStoragePermissionCode
                    )
                }
            }
        }
    }

    /**
     * Method that shows a non-cancelable dialog explaining that permissions are needed in order to use
     * the printing functionality and finishes the activity
     */
    private fun endActivityNoPermission(){
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setMessage(getString(R.string.permission_ExtStorageDenied_endAct))
            .setPositiveButton(android.R.string.ok){ _, _ ->
                this.finish()
            }.setCancelable(false).show()
    }

    //On back pressed removes the temporal created files if they exist
    override fun onBackPressed() {
        super.onBackPressed()
        removeTempFiles()
    }

    //On destroy removes the temporal created files if they exist.
    //Used here too to assert that they are erased.
    override fun onDestroy() {
        super.onDestroy()
        removeTempFiles()
    }

    //Deletes the temporal files created by PickiT if they were downloaded from external repositories
    private fun removeTempFiles(){
        Log.d(tag, "Removing temporary files")
        pickit.deleteTemporaryFile(this)
    }

    /**
     * Method that shows a dialog telling that a file was not chosen.
     */
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

    /**
     * Method that shows a dialog telling that the chosen file is not compatible
     * with the application workflow
     */
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

    /**
     * Method used to reset the viewModel data
     */
    private fun clearViewModelData(){
        viewModel.setUri(null)
        viewModel.setPath(null)
        viewModel.setType(ResourceTypeEnum.NOT_DEFINED)
    }

    /**
     * Method used to ask for file access special permission in Android 11+
     */
    private fun askAccessAllFilesPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val uri = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
                startActivity(Intent( Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri))
            }
        }
    }

}