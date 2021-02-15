package com.example.movil

import android.os.Bundle
import android.os.Environment
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

    private val tag = "--- ScanActivity ---"
    private lateinit var scannerListAdapter : ScannerListAdapter
    private lateinit var scannerListView : ListView
    private lateinit var scannerSearchButton : Button
    private lateinit var auxText : TextView
    private var isSearching = false
    private lateinit var scannerBrowser : ScannersBrowser
    private lateinit var progressBar : ProgressBar
    var scannerNumber = 0 //Debug

    private val scannerBrowserListener: ScannerAvailabilityListener =
        object : ScannerAvailabilityListener {
            override fun onScannerFound(aScanner: Scanner) {
                Log.d(tag, "Scanner found")
                if (aScanner != null) {
                    scannerListAdapter.add(aScanner)
                    scannerListView.adapter=scannerListAdapter
                }
            }

            override fun onScannerLost(aScanner: Scanner) {
                Log.d(tag, "Scanner lost")
                if (aScanner != null) {
                    scannerListAdapter.remove(aScanner)
                    scannerListView.adapter=scannerListAdapter

                }
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

                    auxText.text = getString(R.string.ScanAct_searchingLabel)
                    scannerSearchButton.text = getString(R.string.ScanAct_stopSearchButton)
                    progressBar.visibility = View.VISIBLE
                    isSearching = true

                    Log.d(tag, "Searching for scanners")
                    scannerBrowser.start(scannerBrowserListener)

                    /*scannerListAdapter.add(ScannerImp("Scanner $scannerNumber"))
                    Log.d(tag, "Added scanner $scannerNumber")
                    scannerListView.adapter=scannerListAdapter
                    ++scannerNumber

                    scannerListAdapter.add(ScannerImp("Scanner $scannerNumber"))
                    scannerListView.adapter=scannerListAdapter
                    Log.d(tag, "Added scanner $scannerNumber")
                    ++scannerNumber*/


                } else {
                    //Stop searching
                    stopSearching()
                    /*auxText.text = getString(R.string.ScanAct_stoppedLabel)
                    scannerSearchButton.text = getString(R.string.ScanAct_startSearchButton)
                    progressBar.visibility = View.INVISIBLE
                    isSearching = false
                    scannerBrowser.stop()*/
                }

            }
        })

        scannerListView.onItemClickListener = object: AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long){

                //Stop searching
                stopSearching()

                val selectedScanner = parent?.adapter?.getItem(position) as Scanner
                Toast.makeText(
                    this@ScanActivity,
                    "Seleccionado el escaner ${selectedScanner.humanReadableName}",
                    Toast.LENGTH_SHORT
                ).show()

                //Show popup menu
                if (view != null) {
                    val scanTicket = showPopupForScanTicket(view, selectedScanner)
                    /* if(scanTicket != null) {
                        Log.d(tag, "Preparing scan")
                        startScanning(selectedScanner, scanTicket)
                    }else{
                        Log.d(tag, "scanTicket is null")
                    }*/
                }
            }
        }

    }

    /**
     * Creates the popup menu when clicking on a scanner from the list, and returns
     * the ScanTicket based on the user's selection
     */
    private fun showPopupForScanTicket(view: View, scanner: Scanner) : ScanTicket? {

        var ticket : ScanTicket? = null

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
                            ticket = ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_PHOTO)
                            Log.d(tag, "Ticket ${ticket?.name}")

                            ticket?.let { startScanning(scanner, it) } ?: Log.d(tag, "Ticket is null")

                            return true
                        }
                        R.id.scan_popup_document -> {
                            //Create document with images ScanTicket
                            Log.d(tag, "Scan document chosen")

                            ticket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_AND_IMAGES)
                            Log.d(tag, "Ticket ${ticket?.name}")
                            ticket?.let { startScanning(scanner, it) } ?: Log.d(tag, "Ticket is null")

                            return true
                        }

                        R.id.scan_popup_text -> {
                            //Create only text ScanTicket
                            Log.d(tag, "Scan text chosen")

                            ticket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_DOCUMENT)
                            Log.d(tag, "Ticket ${ticket?.name}")
                            ticket?.let { startScanning(scanner, it) } ?: Log.d(tag, "Ticket is null")

                            return true
                        }
                        else -> return false
                    }
                } else {
                    return false;
                }
            }
        })
        menu.show()
        //Log.d(tag, "return ticket ${ticket?.name}")
        return ticket
    }

    private fun startScanning(theScanner: Scanner, scanTicket: ScanTicket){

        Log.d(tag, "startScanning()")

        //Create the file to save the scanning
        val theExternalStorageDirectory = Environment.getExternalStorageDirectory()
        val scanFile = File(theExternalStorageDirectory, "ContactlessPrinterOp")

        theScanner.scan(scanFile.absolutePath, scanTicket, object : ScanCapture.ScanningProgressListener{
            override fun onScanningPageDone(p0: ScanPage?) {
                Toast.makeText(this@ScanActivity, "Pagina escaneada", Toast.LENGTH_LONG).show()
                Log.d(tag, "Pagina Escaneada")
            }

            override fun onScanningComplete() {
                Toast.makeText(this@ScanActivity, "Escaneo completado", Toast.LENGTH_LONG).show()
                Log.d(tag, "Escaneo completado")
            }

            override fun onScanningError(theException: ScannerException?) {
                try{
                    Toast.makeText(this@ScanActivity, "Error en el escaneo", Toast.LENGTH_LONG).show()
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

}