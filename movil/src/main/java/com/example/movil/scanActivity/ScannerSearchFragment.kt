package com.example.movil

import com.example.movil.scanActivity.*

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.hp.mobile.scan.sdk.Scanner
import com.hp.mobile.scan.sdk.browsing.ScannersBrowser
import com.hp.mobile.scan.sdk.browsing.ScannersBrowser.ScannerAvailabilityListener
import com.hp.mobile.scan.sdk.model.ScanTicket

class ScannerSearchFragment : Fragment() {


    //private val tempScanFolder = "TempScan"
    private val TAG = "--- ScannerSearchActivity ---"
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
                scannerListAdapter.add(aScanner)
            }

            override fun onScannerLost(aScanner: Scanner) {
                Log.d(tag, "Scanner lost")
                scannerListAdapter.remove(aScanner)
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val theView = inflater.inflate(R.layout.fragment_scanner_search, container, false)

        scannerListView = theView.findViewById(R.id.act_scanner_search_deviceListView)
        scannerSearchButton = theView.findViewById(R.id.act_scanner_search_searchScannerButton)
        auxText = theView.findViewById(R.id.act_scanner_search_aux)
        scannerBrowser = ScannersBrowser(context)
        progressBar = theView.findViewById(R.id.act_scanner_search_progressBar)
        scannerListAdapter = ScannerListAdapter(requireContext())
        scannerListView.adapter = scannerListAdapter

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
                scannerListAdapter.add(sc1)
                Log.d(tag, "Added scanner $scannerNumber")
                ++scannerNumber
                val sc2 = ScannerImp("Scanner $scannerNumber")
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
                (activity as ScanActivity).chosenScanner = parent?.adapter?.getItem(position) as Scanner
                //Show popup menu
                if (view != null) {
                    showPopupChooseTicket(view)
                }
            }

        return theView
    }

    /**
     * Creates the popup menu when clicking on a scanner from the list, and returns
     * the ScanTicket based on the user's selection
     */
    //private fun showPopupAndPrint(view: View, scanner: Scanner) {
    private fun showPopupChooseTicket(view: View) {

        //Create and inflate the menu
        val menu = PopupMenu(requireContext(), view)
        menu.menuInflater.inflate(R.menu.scanner_list_popup_menu, menu.menu)

        //Add click listener
        menu.setOnMenuItemClickListener(object : PopupMenu.OnMenuItemClickListener {
            override fun onMenuItemClick(item: MenuItem?): Boolean {
                if (item != null) {
                    when (item.itemId) {
                        R.id.scan_popup_photo -> {
                            //Create photo ScanTicket
                            Log.d(tag, "Scan photo chosen")
                            (activity as ScanActivity).chosenTicket = ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_PHOTO)
                            //Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            //startScanning()
                            openScanFrag()

                            return true
                        }
                        R.id.scan_popup_document -> {
                            //Create document with images ScanTicket
                            Log.d(tag, "Scan document chosen")

                            (activity as ScanActivity).chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_AND_IMAGES)
                            //Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            //startScanning()
                            openScanFrag()

                            return true
                        }

                        R.id.scan_popup_text -> {
                            //Create only text ScanTicket
                            Log.d(tag, "Scan text chosen")

                            (activity as ScanActivity).chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_DOCUMENT)
                            //Log.d(tag, "Ticket ${chosenTicket!!.name}")
                            //startScanning()
                            openScanFrag()

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

    private fun openScanFrag(){
        (activity as ScanActivity).replaceSearchWithScan()
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