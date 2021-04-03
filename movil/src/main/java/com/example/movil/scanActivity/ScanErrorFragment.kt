package com.example.movil.scanActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.movil.R
import com.example.movil.ScannerSearchFragment

class ScanErrorFragment : DialogFragment() {

    lateinit var button: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val theView = inflater.inflate(R.layout.fragment_scan_error, container, false)

        button = theView.findViewById(R.id.frag_scan_error_button)

        button.setOnClickListener { closeDialog() }
        return theView
    }

    private fun closeDialog() {
        val parentAct = activity as ScanActivity
        parentAct.supportFragmentManager.beginTransaction().remove(this@ScanErrorFragment).commit()
    }
}