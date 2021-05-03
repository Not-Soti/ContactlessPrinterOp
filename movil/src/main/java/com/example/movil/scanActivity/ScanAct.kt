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

    var screenStatus = 0 //Check what to do on onBackPressed
                         //0 activity, 1 scannerSearchFragment, 2 ScanOptFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scan)

        viewModel = ViewModelProvider(this).get(ScanActivityViewModel::class.java)

        screenStatus = 1
        supportFragmentManager.beginTransaction().add(R.id.act_scan_root, ScannerSearchFragment::class.java, null).commit()
    }

    /**
     * Replaces the search scanner fragment with the scanning proccess fragment
     */
    fun replaceSearchWithScan(){
        screenStatus = 2
        val scanFrag = ScanOptFragment()
        val trans = supportFragmentManager.beginTransaction()

        trans.replace(R.id.act_scan_root, scanFrag)
        trans.addToBackStack(null)
        trans.commit()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        when(screenStatus){
            //0 -> startActivity(Intent(this, MainActivity::class.java))
            //1 -> startActivity(Intent(this, MainActivity::class.java))
            2 -> {
                //Replace scanOptionsFragment with searchScannerFragment
                if (viewModel.chosenScanner != null) {
                    viewModel.chosenScanner!!.stopMonitoringDeviceStatus()
                }
                screenStatus = 1
                val scanFrag = ScannerSearchFragment()
                val trans = supportFragmentManager.beginTransaction()

                trans.replace(R.id.act_scan_root, scanFrag)
                trans.addToBackStack(null)
                trans.commit()
                Log.d(TAG, "back 2")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(viewModel.chosenScanner!=null) {
            viewModel.chosenScanner!!.stopMonitoringDeviceStatus()
        }
    }
}