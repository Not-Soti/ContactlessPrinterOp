package com.example.movil


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.example.movil.printActivity.PrintActivity
import com.example.movil.readQrActivity.ReadQrActivity
import com.example.movil.scanActivity.ScanActivity
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(){

    /*lateinit var buttonReadQr: Button
    lateinit var buttonPrint: Button
    lateinit var buttonScan: Button*/
    lateinit var path: String

    lateinit var linLayReadQr: LinearLayout
    lateinit var linLayPrint: LinearLayout
    lateinit var linLayScan: LinearLayout

    lateinit var buttonScan1: Button
    lateinit var buttonScan2: Button
    lateinit var buttonScan3: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*buttonReadQr = findViewById(R.id.act_main_readQrButton)
        buttonPrint = findViewById(R.id.act_main_startPrintActivity)
        buttonScan = findViewById(R.id.act_main_startScanActivity)*/

        buttonScan1 = findViewById(R.id.act_main_scanOp1)
        buttonScan2 = findViewById(R.id.act_main_scanOp2)
        buttonScan3 = findViewById(R.id.act_main_scanOp3)

        buttonScan1.visibility = View.INVISIBLE
        buttonScan2.visibility = View.INVISIBLE
        buttonScan3.visibility = View.INVISIBLE

        linLayPrint = findViewById(R.id.act_main_print_LinLay)
        linLayScan = findViewById(R.id.act_main_scan_LinLay)
        linLayReadQr = findViewById(R.id.act_main_readQr_LinLay)

        linLayPrint.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                startActivity(Intent(this@MainActivity, PrintActivity::class.java))
            }

        })

        linLayReadQr.setOnClickListener(object : View.OnClickListener{
            override fun onClick(p0: View?) {
                val i = Intent(this@MainActivity, ReadQrActivity::class.java)
                startActivity(i)
            }
        })

        linLayScan.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {
                //startActivity(Intent(this@MainActivity, ScanActivity::class.java))
                startActivity(Intent(this@MainActivity, ScanOp3::class.java))
            }

        })


        //Pruebas escaneo

        buttonScan1.setOnClickListener { startActivity(Intent(this@MainActivity,ScanOp1::class.java))} //Cambiar nombre del fichero en File(ruta, nombre)
        buttonScan2.setOnClickListener { startActivity(Intent(this@MainActivity,ScanOp2::class.java))} //Cambiar nombre del scanTicket
        buttonScan3.setOnClickListener { startActivity(Intent(this@MainActivity,ScanOp3::class.java))} //Guardar archivo temporal para previsualizar


    }


    //Se cierra la app
    override fun onBackPressed() {
        exitProcess(0)
    }
}