package com.example.movil.scanActivity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.movil.R
import com.example.movil.ScannerSearchFragment

class ScanActivity : AppCompatActivity() {

    private val TAG = "--- ScanActivity ---"
    private lateinit var viewModel : ScanActivityViewModel

    var backPressedStatus = 0 //controla estado donde se pulsa boton de "atras"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_scan)

        viewModel = ViewModelProvider(this).get(ScanActivityViewModel::class.java)

        val trans = supportFragmentManager.beginTransaction()
        trans.add(R.id.act_scan_root, ScannerSearchFragment::class.java, null)
        trans.addToBackStack("frag_search_scanner")//Añadir para volver aqui al pulsar atras
        backPressedStatus = 0
        trans.commit()
    }

    /**
     * Replaces the search scanner fragment with the scanning proccess fragment
     */
    fun replaceSearchWithScan(){
        val scanFrag = ScanOptionsFragment()
        val trans = supportFragmentManager.beginTransaction()

        trans.replace(R.id.act_scan_root, scanFrag)  //replace por que si no se ven transparentes
        trans.addToBackStack("frag_scan_options")
        backPressedStatus = 1
        trans.commit()
    }

    override fun onBackPressed() {
        //super.onBackPressed()

        when(backPressedStatus) {
            //Pulsado desde searchFragment
            0 -> {
                //on baksuper.onBackPressed() //vuelve hacia atras ya que es la 1ª pantalla
                this.finish() //se acaba
            }

            //Pulsado desde OptionsFragment
            1 -> {
                val fragManager = supportFragmentManager
                backPressedStatus = 0 //por si no lo mantiene
                fragManager.popBackStack("frag_search_scanner", 0) //vuelve hasta el de busqueda, sin popearlo
            }

            //Se acaba la historia de usuario de escanear -> vuelve a la pantalla de inicio
            2 -> {
                //super.onBackPressed() //vuelve hacia atras
                this.finish() //se acaba
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(viewModel.chosenScanner!=null) {
            viewModel.chosenScanner!!.stopMonitoringDeviceStatus()
        }
    }
}