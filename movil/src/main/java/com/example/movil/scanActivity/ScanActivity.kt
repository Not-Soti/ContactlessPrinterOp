package com.example.movil

import com.example.movil.scanActivity.*

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.hp.mobile.scan.sdk.AdfException
import com.hp.mobile.scan.sdk.ScanCapture
import com.hp.mobile.scan.sdk.Scanner
import com.hp.mobile.scan.sdk.ScannerException
import com.hp.mobile.scan.sdk.browsing.ScannersBrowser
import com.hp.mobile.scan.sdk.browsing.ScannersBrowser.ScannerAvailabilityListener
import com.hp.mobile.scan.sdk.model.ScanPage
import com.hp.mobile.scan.sdk.model.ScanTicket
import java.io.File

class ScanActivity : AppCompatActivity() {


    private val tempScanFolder = "TempScan"
    private val tag = "--- ScanActivity ---"
    private lateinit var scannerListAdapter : ScannerListAdapter
    private lateinit var scannerListView : ListView
    private lateinit var scannerSearchButton : Button
    private lateinit var auxText : TextView
    private var isSearching = false
    private lateinit var scannerBrowser : ScannersBrowser
    private lateinit var progressBar : ProgressBar
    var scannerNumber = 0 //Debug
    var scanResultUri : Uri? = null

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

        val tempFolder = getExternalFilesDir(tempScanFolder)
        if(tempFolder!=null && !tempFolder.exists()){
            if(tempFolder.mkdirs()){
                Log.d(tag, "temp folder created")
            }
        }

        scannerSearchButton.setOnClickListener {
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
                sc1.act = this@ScanActivity
                sc1.isOp3 = false
                scannerListAdapter.add(sc1)
                Log.d(tag, "Added scanner $scannerNumber")
                ++scannerNumber

                val sc2 = ScannerImp("Scanner $scannerNumber")
                sc2.act = this@ScanActivity
                sc2.isOp3 = false
                scannerListAdapter.add(sc2)
                Log.d(tag, "Added scanner $scannerNumber")
                ++scannerNumber

                scannerListView.adapter=scannerListAdapter*/


            } else {
                //Stop searching
                stopSearching()
            }
        }

        scannerListView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, _ -> //Stop searching
                stopSearching()

                chosenScanner = parent?.adapter?.getItem(position) as Scanner
                /*Toast.makeText(
                            this@ScanActivity,
                            "Seleccionado el escaner ${chosenScanner.humanReadableName}",
                            Toast.LENGTH_SHORT
                        ).show()*/

                //Show popup menu
                if (view != null) {
                    showPopupChooseTicket(view)
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
        val menu = PopupMenu(this, view)
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
                            //Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            startScanning()

                            return true
                        }
                        R.id.scan_popup_document -> {
                            //Create document with images ScanTicket
                            Log.d(tag, "Scan document chosen")

                            chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_AND_IMAGES)
                            //Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            startScanning()

                            return true
                        }

                        R.id.scan_popup_text -> {
                            //Create only text ScanTicket
                            Log.d(tag, "Scan text chosen")

                            chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_DOCUMENT)
                            //Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            startScanning()

                            return true
                        }
                        else -> return false
                    }//when(itemId)
                } else {
                    return false
                }
            }
        })
        menu.show()
    }

    private fun startScanning(){
        //Log.d(tag, "startScanning()")

        //Open scanning fragment
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        val scanningFragment = ScanningFragment()
        scanningFragment.show(fragmentTransaction, "scanningFragment")

        //Create the temp file to save the scanning
        val tempFolder = getExternalFilesDir(tempScanFolder)
        val tempFile = File(tempFolder, "tempScanFile")

        tempPathAux = tempFile.absolutePath
        Log.d(tag, "Temp file abs path: $tempPathAux")

        chosenScanner.scan(tempFile.absolutePath, chosenTicket, object : ScanCapture.ScanningProgressListener{
            override fun onScanningPageDone(p0: ScanPage?) {
                //Toast.makeText(this@ScanActivity, "Pagina escaneada", Toast.LENGTH_LONG).show()
                Log.d(tag, "Page scanned")
                scanResultUri = p0?.uri
            }

            override fun onScanningComplete() {
                //Toast.makeText(this@ScanActivity, "Escaneo completado", Toast.LENGTH_LONG).show()
                Log.d(tag, "Scanning completed")

                val i = Intent(applicationContext, ScanPreview::class.java)
                i.putExtra("tempUri", scanResultUri.toString())
                startActivity(i)
            }

            override fun onScanningError(theException: ScannerException?) {
                try{

                    chosenScanner.cancelScanning()
                    tempFile.delete()
                    //Toast.makeText(applicationContext, "Error, ${theException!!.message}", Toast.LENGTH_LONG).show()

                    scanningFragment.dismiss()
                    //supportFragmentManager.beginTransaction().remove(scanningFragment)
                    val scanErrorFragment = ScanErrorFragment()
                    scanErrorFragment.show(fragmentTransaction, "scanErrorFragment")

                    throw theException!!

                }catch (e: AdfException){
                    Log.d(tag, "AdfException\n Status: ${e.adfStatus}")

                }catch (e: ScannerException){
                    Log.d(tag, "ScannerException\n Reason: ${e.reason}")
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

    //TODO("Borrar")
    fun scanningCompleted(){
        val i = Intent(this@ScanActivity, ScanPreview::class.java)
        i.putExtra("tempPath", tempPathAux)
        startActivity(i)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this, MainActivity::class.java))
    }
}