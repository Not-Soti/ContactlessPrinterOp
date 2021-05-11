package com.example.movil.scanActivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.movil.R
import com.example.movil.ScannerSearchFragment

class ScanActivity : AppCompatActivity() {

    private val TAG = "--- ScanActivity ---"
    private lateinit var viewModel : ScanActivityViewModel

    var backPressedStatus = 0 /*Controls the status where the Back Button was pressed
                               0 = pressed on Scanner Search screeen
                               1 = pressed on the Scan Options / Settings screen
                               2 = Scanning proccess is finished and the app is rolling back
                                   to the Main activity
                               */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scan)

        viewModel = ViewModelProvider(this).get(ScanActivityViewModel::class.java)

        //Creates the SearchScanner fragment and shows it
        val trans = supportFragmentManager.beginTransaction()
        trans.add(R.id.act_scan_root, ScannerSearchFragment::class.java, null)

        //Adding to the backstack with a name so the stack can be popped until finding this fragment
        trans.addToBackStack("frag_search_scanner")
        backPressedStatus = 0 //ensures that the status is 0
        trans.commit()
    }

    /**
     * Replaces the search scanner fragment with the scanning settings fragment
     */
    fun replaceSearchWithScan(){
        val scanFrag = ScanSettingsFragment()
        val trans = supportFragmentManager.beginTransaction()

        trans.replace(R.id.act_scan_root, scanFrag)  //replaces the scanner search fragment with the new one
        trans.addToBackStack("frag_scan_options")
        backPressedStatus = 1 //Sets 1 to the flag in order to control the backPressed status
        trans.commit()
    }

    /**
     * Method called when the user press back in his device
     * This needs to act different according to the screen shown
     * to the user when the button was pressed
     */
    override fun onBackPressed() {

        when(backPressedStatus) {
            //Pressed from ScannerSearchFragment
            0 -> {
                this.finish() //Finishes the activity and goes back to the MainActivity
            }

            //Pressed from  ScanSettingsFragment
            1 -> {
                //If pressed here, the stack pops back to the ScannerSearch fragment and list the found scanners
                val fragManager = supportFragmentManager
                backPressedStatus = 0 //ensure that the status is the correct one

                //pops the fragment to the desired one, without popping it
                fragManager.popBackStack("frag_search_scanner", 0)
            }

            //The scanning proccess is finished -> go back to the main activity
            2 -> {
                this.finish() //Finishes the activity
            }
        }
    }

    /**
     * Method that asserts that the chosen scanner
     * is not monitored anymore when destroying the activity
     */
    override fun onDestroy() {
        super.onDestroy()
        if(viewModel.chosenScanner!=null) {
            viewModel.chosenScanner!!.stopMonitoringDeviceStatus()
        }
    }
}