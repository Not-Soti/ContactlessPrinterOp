package com.example.movil.scanActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.movil.MainActivity
import com.example.movil.R
import com.example.movil.ScannerSearchFragment

class ScanAct : AppCompatActivity() {

    private val TAG = "--- ScanActivity ---"
    private lateinit var viewModel : ScanActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scan)

        viewModel = ViewModelProvider(this).get(ScanActivityViewModel::class.java)

        val trans = supportFragmentManager.beginTransaction()
        trans.add(R.id.act_scan_root, ScannerSearchFragment::class.java, null)
        trans.addToBackStack("frag_search_scanner")//Añadir para volver aqui al pulsar atras
        trans.commit()
    }

    /**
     * Replaces the search scanner fragment with the scanning proccess fragment
     */
    fun replaceSearchWithScan(){
        val scanFrag = ScanOptFragment()
        val trans = supportFragmentManager.beginTransaction()

        trans.add(R.id.act_scan_root, scanFrag)
        //trans.addToBackStack(null) //no se añade a la pila para no volver
        trans.commit()
    }


    override fun onDestroy() {
        super.onDestroy()
        if(viewModel.chosenScanner!=null) {
            viewModel.chosenScanner!!.stopMonitoringDeviceStatus()
        }
    }
}