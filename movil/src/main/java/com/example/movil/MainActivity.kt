package com.example.movil


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import android.widget.LinearLayout
import com.example.movil.printActivity.PrintActivity
import com.example.movil.readQrActivity.ReadQrActivity
import com.example.movil.scanActivity.PerformingScanFragment
import com.example.movil.scanActivity.ScanActivity
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity(){

    lateinit var path: String

    lateinit var linLayReadQr: LinearLayout
    lateinit var linLayPrint: LinearLayout
    lateinit var linLayScan: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        linLayPrint = findViewById(R.id.act_main_print_LinLay)
        linLayScan = findViewById(R.id.act_main_scan_LinLay)
        linLayReadQr = findViewById(R.id.act_main_readQr_LinLay)

        linLayPrint.setOnClickListener {
            startActivity(Intent(this@MainActivity,PrintActivity::class.java))
        }

        linLayReadQr.setOnClickListener {
            val i = Intent(this@MainActivity, ReadQrActivity::class.java)
            startActivity(i)
        }

        linLayScan.setOnClickListener {
            startActivity(Intent(this@MainActivity, ScanActivity::class.java))
        }

    }

}