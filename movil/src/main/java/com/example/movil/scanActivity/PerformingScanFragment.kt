package com.example.movil.scanActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.movil.R

/**
 * Fragment that shows a progress bar while scanning, and an explanatory message
 * if an error happened while scanning
 */
class PerformingScanFragment : DialogFragment() {

    private val TAG = "---ScanningFragment---"
    private lateinit var button : Button
    private lateinit var statusTv : TextView
    private lateinit var exceptionTv : TextView
    private lateinit var viewModel : ScanActivityViewModel
    private lateinit var progressBar : ProgressBar

    private var isException = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val theView = inflater.inflate(R.layout.fragment_scan_progress, container, false)

        viewModel = ViewModelProvider(requireActivity()).get(ScanActivityViewModel::class.java)

        button = theView.findViewById(R.id.frag_performing_scan_button)
        statusTv = theView.findViewById(R.id.frag_performing_scan_status)
        exceptionTv = theView.findViewById(R.id.frag_performing_scan_error)
        progressBar = theView.findViewById(R.id.frag_performing_scan_progressBar)

        button.setOnClickListener {
            if(!isException){
                //If pressed while scanning correctly, stops the scan
                stopScanning()
            }else{
                //If pressed when an exception occured, just dismisses the dialog
                this.dismiss()
            }
        }

        return theView
    }

    private fun stopScanning(){
        viewModel.chosenScanner!!.cancelScanning() //tells the scanner to stop
        this.dismiss() //Close the dialog
    }

    /**
     * Method that shows the exception on the screen
     */
    fun showException(reason: String) {
        isException=true //sets the flag to true
        progressBar.visibility = View.INVISIBLE //Hides the progress bar
        statusTv.text = getString(R.string.SCANNER_STATUS_STOPPED)
        exceptionTv.text = reason //Sets the exception reason to the provided one
    }

}