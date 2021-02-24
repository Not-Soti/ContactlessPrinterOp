package com.example.movil

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class ScanningFragment : DialogFragment() {

    private val TAG = "---ScanningFragment---"
    private lateinit var button : Button
    private lateinit var textView : TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.d(TAG, "Creando fragmento")
        val theView = inflater.inflate(R.layout.fragment_scanning, container, false)

        button = theView.findViewById(R.id.frag_scanning_button)
        textView = theView.findViewById(R.id.frag_scanning_status)


        button.setOnClickListener { stopScanning() }

        Log.d(TAG, "Fragment creado")
        return theView
    }

    private fun stopScanning(){
        val parentAct = activity as ScanActivity
        parentAct.chosenScanner.cancelScanning()
        Toast.makeText(activity, "Escaneo cancelado", Toast.LENGTH_LONG).show()
        parentAct.supportFragmentManager.beginTransaction().remove(this).commit()
    }

}