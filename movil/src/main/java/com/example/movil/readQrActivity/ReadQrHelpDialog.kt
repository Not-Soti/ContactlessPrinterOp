package com.example.movil.readQrActivity

import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import com.example.movil.R

class ReadQrHelpDialog : AppCompatActivity()  {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        setContentView(R.layout.dialog_qr_scanner_help)
    }
}