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

class ScanOp3 : AppCompatActivity() {


    private val tempScanDir = "TempScan"
    private val tag = "--- ScanOp3 ---"
    private lateinit var scannerListAdapter : ScannerListAdapter
    private lateinit var scannerListView : ListView
    private lateinit var scannerSearchButton : Button
    private lateinit var auxText : TextView
    private var isSearching = false
    private lateinit var scannerBrowser : ScannersBrowser
    private lateinit var progressBar : ProgressBar
    var scannerNumber = 0 //Debug

    private var tempPathAux = ""

    lateinit var chosenScanner : Scanner
    var chosenTicket : ScanTicket? = null

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

        scannerListView = findViewById(R.id.act_scan_deviceListView)
        scannerSearchButton = findViewById(R.id.act_scan_searchScannerButton)
        auxText = findViewById(R.id.act_scan_aux)
        scannerBrowser = ScannersBrowser(this)
        progressBar = findViewById(R.id.act_scan_progressBar)
        scannerListAdapter = ScannerListAdapter(applicationContext)
        scannerListView.adapter = scannerListAdapter

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


                    /*val sc1 = ScannerImp("Scanner $scannerNumber")
                    sc1.act = this@ScanOp3
                    scannerListAdapter.add(sc1)
                    Log.d(tag, "Added scanner $scannerNumber")
                    ++scannerNumber

                    val sc2 = ScannerImp("Scanner $scannerNumber")
                    sc2.act = this@ScanOp3
                    scannerListAdapter.add(sc2)
                    Log.d(tag, "Added scanner $scannerNumber")
                    ++scannerNumber

                    scannerListView.adapter=scannerListAdapter*/


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
                    this@ScanOp3,
                    "Seleccionado el escaner ${chosenScanner.humanReadableName}",
                    Toast.LENGTH_SHORT
                ).show()

                //Show popup menu
                if (view != null) {
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
                            startScanning()

                            return true
                        }
                        R.id.scan_popup_document -> {
                            //Create document with images ScanTicket
                            Log.d(tag, "Scan document chosen")

                            chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_AND_IMAGES)
                            Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            startScanning()

                            return true
                        }

                        R.id.scan_popup_text -> {
                            //Create only text ScanTicket
                            Log.d(tag, "Scan text chosen")

                            chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_DOCUMENT)
                            Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            startScanning()

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

    private fun startScanning(){

        Log.d(tag, "startScanning()")

        //Open scanning fragment
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val scanningFragment = ScanningFragment()
        scanningFragment.show(fragmentTransaction, "scanningFragment")

        //Create the temp file to save the scanning
        val tempFile = File.createTempFile("tempScan", ".pdf", cacheDir)

        tempPathAux = tempFile.absolutePath

        chosenScanner.scan(tempFile.absolutePath, chosenTicket, object : ScanCapture.ScanningProgressListener{
            override fun onScanningPageDone(p0: ScanPage?) {
                Toast.makeText(this@ScanOp3, "Pagina escaneada", Toast.LENGTH_LONG).show()
                Log.d(tag, "Pagina Escaneada")
            }

            override fun onScanningComplete() {
                Toast.makeText(this@ScanOp3, "Escaneo completado", Toast.LENGTH_LONG).show()
                Log.d(tag, "Escaneo completado")

                scanningFragment.dismiss()
                val i = Intent(applicationContext, ScanPreview::class.java)
                i.putExtra("tempPath", tempFile.absolutePath)
                startActivity(i)
            }

            override fun onScanningError(theException: ScannerException?) {
                try{
                    //Toast.makeText(this@ScanOp3, "Error en el escaneo", Toast.LENGTH_LONG).show()

                    chosenScanner.cancelScanning()
                    Toast.makeText(applicationContext, "Error, ${theException!!.message}", Toast.LENGTH_LONG).show()

                    scanningFragment.dismiss()
                    //supportFragmentManager.beginTransaction().remove(scanningFragment)
                    val scanErrorFragment = ScanErrorFragment()
                    scanErrorFragment.show(fragmentTransaction, "scanErrorFragment")

                    throw theException

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

   fun scanningCompleted(){
       val i = Intent(this@ScanOp3, ScanPreview::class.java)
       i.putExtra("tempPath", tempPathAux)
       startActivity(i)
   }

}