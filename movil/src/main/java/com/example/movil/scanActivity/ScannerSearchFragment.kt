package com.example.movil

import com.example.movil.scanActivity.*

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.hp.mobile.scan.sdk.Scanner
import com.hp.mobile.scan.sdk.browsing.ScannersBrowser
import com.hp.mobile.scan.sdk.browsing.ScannersBrowser.ScannerAvailabilityListener
import com.hp.mobile.scan.sdk.model.ScanTicket

/**
 * Fragment that searchs for scanners in the current network and lists them
 */
class ScannerSearchFragment : Fragment() {


    //private val tempScanFolder = "TempScan"
    private val TAG = "--- ScannerSearchActivity ---"
    private lateinit var scannerListAdapter : ScannerListAdapter
    private lateinit var scannerListView : ListView
    private lateinit var scannerSearchButton : ExtendedFloatingActionButton
    private lateinit var auxText : TextView
    private var isSearching = false
    private lateinit var scannerBrowser : ScannersBrowser
    private lateinit var progressBar : ProgressBar
    private lateinit var viewModel : ScanActivityViewModel

    //Listener called when a scanner is detected or lost
    private val scannerBrowserListener: ScannerAvailabilityListener =
        object : ScannerAvailabilityListener {
            override fun onScannerFound(aScanner: Scanner) {
                //Log.d(tag, "Scanner found")
                scannerListAdapter.add(aScanner)
            }
            override fun onScannerLost(aScanner: Scanner) {
                //Log.d(tag, "Scanner lost")
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

        viewModel = ViewModelProvider(requireActivity()).get(ScanActivityViewModel::class.java)

        scannerListView = theView.findViewById(R.id.act_scanner_search_deviceListView)
        scannerSearchButton = theView.findViewById(R.id.act_scanner_search_searchScannerButton)
        auxText = theView.findViewById(R.id.act_scanner_search_aux)
        scannerBrowser = ScannersBrowser(context)
        progressBar = theView.findViewById(R.id.act_scanner_search_progressBar)
        scannerListAdapter = ScannerListAdapter(requireContext())
        scannerListView.adapter = scannerListAdapter

        //When the scanner search button if clicked
        scannerSearchButton.setOnClickListener {

            //If the device was not already searching, clears the previuous scanner list adapter
            // and starts searching for scanners
            if (!isSearching) {
                //Start searching
                scannerListAdapter.clear()
                auxText.text = getString(R.string.ScanAct_searchingLabel)
                scannerSearchButton.text = getString(R.string.ScanAct_stopSearchButton)
                progressBar.visibility = View.VISIBLE
                isSearching = true

                Log.d(tag, "Searching for scanners")
                scannerBrowser.start(scannerBrowserListener)

            } else {
                //Stop searching
                stopSearching()
            }
        }

        //Upong clicng on a scanner from the list, shows a popup in order
        //to choose a standard scanTicket containing standard scan settings
        scannerListView.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, _ -> //Stop searching
                stopSearching()
                viewModel.chosenScanner = parent?.adapter?.getItem(position) as Scanner
                //Show popup menu
                if (view != null) {
                    showPopupChooseTicket(view)
                }
            }

        //Sets the backPressedStatus son the ScanActivity can act accordingly
        (activity as ScanActivity).backPressedStatus = 0

        return theView
    }

    /**
     * Creates the popup menu when clicking on a scanner from the list, sets the chosen
     * scan ticket and opens the ScanSettingsFragment.
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
                            viewModel.chosenTicket = ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_PHOTO)
                            openScanFrag()
                            return true
                        }
                        R.id.scan_popup_document -> {
                            //Create document with images ScanTicket
                            Log.d(tag, "Scan document chosen")

                            viewModel.chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_AND_IMAGES)
                            openScanFrag()
                            return true
                        }
                        R.id.scan_popup_text -> {
                            //Create only text ScanTicket
                            Log.d(tag, "Scan text chosen")

                            viewModel.chosenTicket =
                                ScanTicket.createWithPreset(ScanTicket.SCAN_PRESET_TEXT_DOCUMENT)
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

    /**
     * Method that tells the activity to show the ScanSettings fragment
     */
    private fun openScanFrag(){
        (activity as ScanActivity).replaceSearchWithScan()
    }


    /**
     * Method called when the scanner search is required to stop
     */
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