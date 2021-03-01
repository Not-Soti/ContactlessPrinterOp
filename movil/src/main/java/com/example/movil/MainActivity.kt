package com.example.movil


import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.example.movil.printActivity.PrintActivity
import com.example.movil.readQrActivity.ReadQrActivity
import com.example.movil.scanActivity.ScanActivity
import java.io.File
import java.io.FileOutputStream
import java.util.jar.Manifest


class MainActivity : AppCompatActivity(){

    /*lateinit var buttonReadQr: Button
    lateinit var buttonPrint: Button
    lateinit var buttonScan: Button*/
    lateinit var path: String

    lateinit var linLayReadQr: LinearLayout
    lateinit var linLayPrint: LinearLayout
    lateinit var linLayScan: LinearLayout

    lateinit var buttonSaveFile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*buttonReadQr = findViewById(R.id.act_main_readQrButton)
        buttonPrint = findViewById(R.id.act_main_startPrintActivity)
        buttonScan = findViewById(R.id.act_main_startScanActivity)*/

        buttonSaveFile = findViewById(R.id.act_main_saveFile)
        buttonSaveFile.visibility = View.INVISIBLE

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
                startActivity(Intent(this@MainActivity, ScanActivity::class.java))
            }

        })


        //save a file
        buttonSaveFile.setOnClickListener(object : View.OnClickListener{
            override fun onClick(v: View?) {

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_TITLE, "ArchivoPrueba1.pdf")
                }
                startActivityForResult(intent, 1)
            }
        })


    }


    //Se cierra la app
    override fun onBackPressed() {
        System.exit(0)
    }
}