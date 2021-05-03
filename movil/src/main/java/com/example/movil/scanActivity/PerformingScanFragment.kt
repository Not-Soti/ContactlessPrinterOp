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

class PerformingScanFragment : DialogFragment() {

    private val TAG = "---ScanningFragment---"
    private lateinit var button : Button
    private lateinit var statusTv : TextView
    private lateinit var exceptionTv : TextView
    private lateinit var viewModel : ScanActivityViewModel
    private lateinit var progressBar : ProgressBar

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
        button.setOnClickListener { stopScanning() }

        return theView
    }

    private fun stopScanning(){
        val parentAct = activity as ScanAct
        viewModel.chosenScanner!!.cancelScanning()
        parentAct.supportFragmentManager.beginTransaction().remove(this).commit()
    }

    fun showException(reason: String) {
        progressBar.visibility = View.INVISIBLE
        statusTv.text = getString(R.string.SCANNER_STATUS_STOPPED)
        exceptionTv.text = reason;
    }

}