package com.example.movil.scanActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.movil.R
import com.example.movil.ScannerSearchAct

class ScanningFragment : DialogFragment() {

    private val TAG = "---ScanningFragment---"
    private lateinit var button : Button
    private lateinit var textView : TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val theView = inflater.inflate(R.layout.fragment_scanning, container, false)

        button = theView.findViewById(R.id.frag_scanning_button)
        textView = theView.findViewById(R.id.frag_scanning_status)
        button.setOnClickListener { stopScanning() }

        return theView
    }

    private fun stopScanning(){
        val parentAct = activity as ScannerSearchAct
        parentAct.chosenScanner.cancelScanning()
        //Toast.makeText(activity, "Escaneo cancelado", Toast.LENGTH_LONG).show()
        parentAct.supportFragmentManager.beginTransaction().remove(this).commit()
    }

}