package com.example.movil


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.view.View
import android.widget.Button

import androidx.print.PrintHelper


class MainActivity : AppCompatActivity(){

    lateinit var buttonReadQr: Button
    lateinit var buttonPrint: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonReadQr = findViewById(R.id.act_main_readQrButton)
        buttonPrint = findViewById(R.id.act_main_printButton)

        //Al pulsar este boton se va a la actividad que lee los QR
        buttonReadQr.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                val i = Intent(this@MainActivity, ReadQrActivity::class.java)
                startActivity(i)
            }
        })

        //Print button listener
        buttonPrint.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                var printHelper: PrintHelper = PrintHelper(this@MainActivity)
                printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
                var photo: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.foto_prueba)
                printHelper.printBitmap("App movil", photo)
                Log.d("---IMPRIMIR---", "Impreso")
            }

        })
    }

    //Se cierra la app
    override fun onBackPressed() {
        System.exit(0)
    }
}