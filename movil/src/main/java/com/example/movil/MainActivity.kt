package com.example.movil


import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.view.View
import android.widget.Button
import android.widget.TextView

import androidx.print.PrintHelper


class MainActivity : AppCompatActivity(){

    lateinit var buttonReadQr: Button
    //lateinit var buttonPrint: Button
    lateinit var buttonSendEmail: Button
    lateinit var textAreaEmail: TextView

    lateinit var buttonGoToPrint: Button //TODO cambiar


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonReadQr = findViewById(R.id.act_main_readQrButton)
        //buttonPrint = findViewById(R.id.act_main_printButton)
        buttonSendEmail = findViewById(R.id.act_main_sendEmail)
        textAreaEmail = findViewById(R.id.act_main_emailTextArea)
        buttonGoToPrint = findViewById(R.id.act_main_goPrintActivity)

        buttonGoToPrint.setOnClickListener(object : View.OnClickListener{
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

        /*
        //Print button listener
        buttonPrint.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {

                //Print the image
                var printHelper: PrintHelper = PrintHelper(this@MainActivity)
                printHelper.scaleMode = PrintHelper.SCALE_MODE_FIT
                var photo: Bitmap = BitmapFactory.decodeResource(resources, R.mipmap.foto_prueba)
                printHelper.printBitmap("App movil", photo)
                Log.d("---IMPRIMIR---", "Impreso")
            }

        })*/

        //Send email button listener
        buttonSendEmail.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                val intent = Intent(Intent.ACTION_SENDTO)

                intent.putExtra(Intent.EXTRA_EMAIL, Array(1){textAreaEmail.text})
                intent.putExtra(Intent.EXTRA_SUBJECT, "email de prueba")
                intent.putExtra(Intent.EXTRA_TEXT,"Email de prueba")

                var uriPhoto = ("android.resource://com.example.movil/" + R.mipmap.foto_prueba)
                intent.putExtra(Intent.EXTRA_STREAM, uriPhoto)

                intent.setType("text/plain")
                intent.setType("image/png")

                startActivityForResult(Intent.createChooser(intent, "Cliente de email"),77)
                Log.d("---Email---", "email enviado")
            }

        })
    }

    //Se cierra la app
    override fun onBackPressed() {
        System.exit(0)
    }
}