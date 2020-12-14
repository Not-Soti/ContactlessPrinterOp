package com.example.movil


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.view.View
import android.widget.Button
import android.widget.TextView


class MainActivity : AppCompatActivity(){

    lateinit var buttonReadQr: Button
    lateinit var buttonPrint: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonReadQr = findViewById(R.id.act_main_readQrButton)
        buttonPrint = findViewById(R.id.act_main_goPrintActivity)

        buttonPrint.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                startActivity(Intent(this@MainActivity, PrintActivity::class.java))
            }

        })

        //Al pulsar este boton se va a la actividad que lee los QR
        buttonReadQr.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                val i = Intent(this@MainActivity, ReadQrActivity::class.java)
                startActivity(i)
            }
        })
    }

    //Se cierra la app
    override fun onBackPressed() {
        System.exit(0)
    }
}