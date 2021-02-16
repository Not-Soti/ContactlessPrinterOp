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
import androidx.fragment.app.Fragment

class ScanningFragment : DialogFragment() {

    val TAG = "---ScanningFragment---"
    lateinit var button : Button;
    lateinit var textView : TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.d(TAG, "Creando fragmento")
        val theView = inflater.inflate(R.layout.fragment_scanning, container, false)

        button = theView.findViewById(R.id.frag_scanning_button)

        button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                stopScanning()
            }

        })

        Log.d(TAG, "Fragment creado")
        return theView;
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        stopScanning()
    }

    private fun stopScanning(){
        val parentAct = activity as ScanActivity
        parentAct.chosenScanner.cancelScanning()
        Toast.makeText(activity, "Escaneo cancelado", Toast.LENGTH_LONG).show()
        parentAct.supportFragmentManager.beginTransaction().remove(this).commit()
    }
}