package com.example.movil

import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment

class ScanningFragment : DialogFragment() {

    val TAG = "---ScanningFragment---"
    lateinit var button : Button;
    lateinit var textView : TextView
    lateinit var progressBar : ProgressBar

    var isScanning = false //determines if scanning is going on
                       //0 scanning, 1 finished, 2 error

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        Log.d(TAG, "Creando fragmento")
        val theView = inflater.inflate(R.layout.fragment_scanning, container, false)

        button = theView.findViewById(R.id.frag_scanning_button)
        textView = theView.findViewById(R.id.frag_scanning_status)


        button.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                if(isScanning) {
                    //If it's scanning it can be canceled
                    stopScanning()
                }else{
                    //Scanning is stopped
                    dismiss()
                }
            }

        })

        Log.d(TAG, "Fragment creado")
        return theView;
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        //stopScanning()
    }

    private fun stopScanning(){
        val parentAct = activity as ScanActivity
        parentAct.chosenScanner.cancelScanning()
        Toast.makeText(activity, "Escaneo cancelado", Toast.LENGTH_LONG).show()
        parentAct.supportFragmentManager.beginTransaction().remove(this).commit()
    }

    public fun scanningCompleted(){
        isScanning=false;
        textView.text = "Completado"
        progressBar.visibility = View.INVISIBLE
        button.text = "Cerrar"
    }

    public fun scanningErrorOccurred(){
        textView.text = "Ha ocurrido un error"
        progressBar.visibility=View.INVISIBLE
        button.text = "Cerrar"
    }
}