package com.example.movil

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.hp.mobile.scan.sdk.Scanner
import com.hp.mobile.scan.sdk.browsing.ScannersBrowser

class ScanActivity : AppCompatActivity() {

    private val tag = "--- ScanActivity ---"
    private lateinit var scannerListAdapter : ScannerListAdapter
    private lateinit var scannerListView : ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        scannerListView = findViewById(R.id.act_scan_deviceList)

        scannerListAdapter = ScannerListAdapter(this)

        scannerListView.adapter = scannerListAdapter

        scannerListView.onItemClickListener = object: AdapterView.OnItemClickListener{
            override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long){

                val selectedScanner = parent?.adapter?.getItem(position) as Scanner
                Toast.makeText(this@ScanActivity,"Seleccionado el escaner ${selectedScanner.humanReadableName}", Toast.LENGTH_LONG).show()

            }

        }

    }


    /**
     * Serach for scanners in the actual network
     */
    private fun scannerBrowse() {
        val scannerBrowser = ScannersBrowser(this)
        scannerBrowser.start(object: ScannersBrowser.ScannerAvailabilityListener{

            override fun onScannerFound(scanner: Scanner?) {
                Log.d(tag, "Scanner found")
                if(scanner != null) {
                    scannerListAdapter.add(scanner)
                }
            }

            override fun onScannerLost(scanner: Scanner?) {
                Log.d(tag, "Scanner lost")
                if(scanner != null) {
                    scannerListAdapter.remove(scanner)
                }
            }

        })

    }

}