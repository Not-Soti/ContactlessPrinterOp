package com.example.movil.scanActivity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.movil.MainActivity
import com.example.movil.R
import com.example.movil.ScannerSearchFragment
import com.hp.mobile.scan.sdk.Scanner
import com.hp.mobile.scan.sdk.model.ScanTicket

class ScanActivity : AppCompatActivity() {

    private val TAG = "--- ScanActivity ---"
    lateinit var chosenScanner : Scanner
    lateinit var chosenTicket : ScanTicket
    var screenStatus = 0 //Check what to do on onBackPressed
                         //0 activity, 1 scannerSearchFragment, 2 ScanOptFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scan)

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
            0 -> {
                startActivity(Intent(this, MainActivity::class.java)); Log.d(TAG, "back 0")
            }
            1 -> {
                startActivity(Intent(this, MainActivity::class.java)); Log.d(TAG, "back 1")
            }
            2 -> {
                //Replace scanOptionsFragment with searchScannerFragment
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
}