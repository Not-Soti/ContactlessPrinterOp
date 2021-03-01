package com.example.movil

import com.example.movil.scanActivity.*

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.movil.BuildConfig
import com.example.movil.PermissionHelper
import com.example.movil.R
import com.example.movil.printActivity.DownloadingFileFragment
import com.hbisoft.pickit.PickiT
import com.hbisoft.pickit.PickiTCallbacks
import com.hp.mobile.scan.sdk.AdfException
import com.hp.mobile.scan.sdk.ScanCapture
import com.hp.mobile.scan.sdk.Scanner
import com.hp.mobile.scan.sdk.ScannerException
import com.hp.mobile.scan.sdk.browsing.ScannersBrowser
import com.hp.mobile.scan.sdk.browsing.ScannersBrowser.ScannerAvailabilityListener
import com.hp.mobile.scan.sdk.model.ScanPage
import com.hp.mobile.scan.sdk.model.ScanTicket
import java.io.File
import java.io.FileOutputStream

class ScanOp2 : AppCompatActivity() {

    private val tag = "--- ScanOp2 ---"
    private lateinit var scannerListAdapter : ScannerListAdapter
    private lateinit var scannerListView : ListView
    private lateinit var scannerSearchButton : Button
    private lateinit var auxText : TextView
    private var isSearching = false
    private lateinit var scannerBrowser : ScannersBrowser
    private lateinit var progressBar : ProgressBar
    var scannerNumber = 0 //Debug

    lateinit var chosenScanner : Scanner
    var chosenTicket : ScanTicket? = null

    private lateinit var pickit: PickiT
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
            startScanning(path!!)
        }

    }


    private val writeExternalStoragePermissionCode = 1
    private val createDocumentPermissionCode = 2

    private val scannerBrowserListener: ScannerAvailabilityListener =
        object : ScannerAvailabilityListener {
            override fun onScannerFound(aScanner: Scanner) {
                Log.d(tag, "Scanner found")
                scannerListAdapter.add(aScanner)
            }

            override fun onScannerLost(aScanner: Scanner) {
                Log.d(tag, "Scanner lost")
                scannerListAdapter.remove(aScanner)
            }
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        askAccessAllFilesPermission()

        scannerListView = findViewById(R.id.act_scan_deviceListView)
        scannerSearchButton = findViewById(R.id.act_scan_searchScannerButton)
        auxText = findViewById(R.id.act_scan_aux)
        scannerBrowser = ScannersBrowser(this)
        progressBar = findViewById(R.id.act_scan_progressBar)

        scannerListAdapter = ScannerListAdapter(applicationContext)

        scannerListView.adapter = scannerListAdapter

        pickit = PickiT(this, pickitListener, this)

        scannerSearchButton.setOnClickListener(object : View.OnClickListener {

            override fun onClick(v: View?) {
                if (!isSearching) {
                    //Start searching
                    scannerListAdapter.clear()
                    auxText.text = getString(R.string.ScanAct_searchingLabel)
                    scannerSearchButton.text = getString(R.string.ScanAct_stopSearchButton)
                    progressBar.visibility = View.VISIBLE
                    isSearching = true

                    Log.d(tag, "Searching for scanners")
                    scannerBrowser.start(scannerBrowserListener)


                    val sc1 = ScannerImp("Scanner $scannerNumber")
                    sc1.act = this@ScanOp2
                    scannerListAdapter.add(sc1)
                    Log.d(tag, "Added scanner $scannerNumber")
                    ++scannerNumber

                    val sc2 = ScannerImp("Scanner $scannerNumber")
                    sc2.act = this@ScanOp2
                    scannerListAdapter.add(sc2)
                    Log.d(tag, "Added scanner $scannerNumber")
                    ++scannerNumber

                    scannerListView.adapter=scannerListAdapter


                } else {
                    //Stop searching
                    stopSearching()
                }

            }
        })

        scannerListView.onItemClickListener = object: AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long){

                //Stop searching
                stopSearching()

                chosenScanner = parent?.adapter?.getItem(position) as Scanner
                Toast.makeText(
                    this@ScanOp2,
                    "Seleccionado el escaner ${chosenScanner.humanReadableName}",
                    Toast.LENGTH_SHORT
                ).show()

                //Show popup menu
                if (view != null) {
                    //showPopupAndPrint(view, chosenScanner)
                    showPopupChooseTicket(view)
                }
            }
        }

    }

    /**
     * Creates the popup menu when clicking on a scanner from the list, and returns
     * the ScanTicket based on the user's selection
     */
    //private fun showPopupAndPrint(view: View, scanner: Scanner) {
    private fun showPopupChooseTicket(view: View) {
        //chosenTicket = null

        //Create and inflate the menu
        var menu = PopupMenu(this, view)
        menu.menuInflater.inflate(R.menu.scanner_list_popup_menu, menu.menu)

        //Add click listener
        menu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem?): Boolean {
                if (item != null) {
                    when (item.itemId) {
                        R.id.scan_popup_photo -> {
                            //Create photo ScanTicket
                            Log.d(tag, "Scan photo chosen")
                            chosenTicket = ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_PHOTO)
                            Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            startScanningRoutine()
                            //askDirectory()
                            //ticket?.let { startScanning(scanner, it) } ?: Log.d(tag, "Ticket is null")

                            return true
                        }
                        R.id.scan_popup_document -> {
                            //Create document with images ScanTicket
                            Log.d(tag, "Scan document chosen")

                            chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_AND_IMAGES)
                            Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            startScanningRoutine()
                            //askDirectory()
                            //ticket?.let { startScanning(scanner, it) } ?: Log.d(tag, "Ticket is null")

                            return true
                        }

                        R.id.scan_popup_text -> {
                            //Create only text ScanTicket
                            Log.d(tag, "Scan text chosen")

                            chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_DOCUMENT)
                            Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            startScanningRoutine()
                            //askDirectory()
                            //ticket?.let { startScanning(scanner, it) } ?: Log.d(tag, "Ticket is null")

                            return true
                        }
                        else -> return false
                    }//when(itemId)
                } else {
                    return false;
                }
            }
        })
        menu.show()
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

    private fun startScanning(newDocPath : String){

        Log.d(tag, "startScanning()")

        //Open scanning fragment

        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val scanningFragment = ScanningFragment()
        //fragmentTransaction.add(R.id.activity_scan_root, scanningFragment)
        //fragmentTransaction.commit()
        scanningFragment.show(fragmentTransaction, "scanningFragment")



        //Create the file to save the scanning
        //val theExternalStorageDirectory = Environment.getExternalStorageDirectory()
        //val theExternalStorageDirectory = applicationContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
        //val scanFile = File(theExternalStorageDirectory, "ContactlessPrinterOp")
        val scanFile = File(newDocPath)
        chosenTicket?.name = "NombreTicket.pdf" //TODO NOMBRE AQUI

        Log.d(tag, "Se ha creado el archivo de destino en ${scanFile.absolutePath}")


        chosenScanner.scan(newDocPath, chosenTicket, object : ScanCapture.ScanningProgressListener{
            override fun onScanningPageDone(p0: ScanPage?) {
                Toast.makeText(this@ScanOp2, "Pagina escaneada", Toast.LENGTH_LONG).show()
                Log.d(tag, "Pagina Escaneada")
            }

            override fun onScanningComplete() {
                Toast.makeText(this@ScanOp2, "Escaneo completado", Toast.LENGTH_LONG).show()
                Log.d(tag, "Escaneo completado")

                //scanningFragment.dismiss()
                supportFragmentManager.beginTransaction().remove(scanningFragment)
                val scanCompletedFragment = ScanCompletedFragment()
                scanCompletedFragment.show(fragmentTransaction, "scanCompletedFragment")

                //MediaScannerConnection.scanFile(applicationContext, arrayOf(scanFile.absolutePath), arrayOf("application/pdf"), null)

            }

            override fun onScanningError(theException: ScannerException?) {
                try{
                    //Toast.makeText(this@ScanOp2, "Error en el escaneo", Toast.LENGTH_LONG).show()

                    chosenScanner.cancelScanning()
                    Toast.makeText(applicationContext, "Error, ${theException!!.message}", Toast.LENGTH_LONG).show()

                    //scanningFragment.dismiss()
                    supportFragmentManager.beginTransaction().remove(scanningFragment)
                    val scanErrorFragment = ScanErrorFragment()
                    scanErrorFragment.show(fragmentTransaction, "scanErrorFragment")

                    throw theException!!

                }catch (e: AdfException){
                    Log.d(tag, "Excepcion AdfException\n Estado: ${e.adfStatus}")

                }catch (e: ScannerException){
                    Log.d(tag, "Excepcion ScannerException\n Razón: ${e.reason}")

                }
            }

        })
    }

    //Stops the scanner searching over the network
    private fun stopSearching(){
        if(isSearching) {
            Log.d(tag, "Stopping scanner search")

            auxText.text = getString(R.string.ScanAct_stoppedLabel)
            scannerSearchButton.text = getString(R.string.ScanAct_startSearchButton)
            progressBar.visibility = View.INVISIBLE
            isSearching = false
            scannerBrowser.stop()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode){
            createDocumentPermissionCode ->{
                if(resultCode == Activity.RESULT_OK) {
                    val newDocPath = data?.data?.path
                    val newDocUri = data?.data
                    Log.d(tag, "Ruta del nuevo archivo: $newDocPath")
                    Log.d(tag, "Uri del nuevo archivo: $newDocUri")

                    //startScanning(newDocPath!!)
                    pickit.getPath(newDocUri, Build.VERSION.SDK_INT)
                }else{
                    Log.d(tag, "Create document cancelado")
                }
            }
        }
    }

    private fun startScanningRoutine(){
        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R){
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

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
            writeExternalStoragePermissionCode ->{
                startScanningRoutine()
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
}