package com.example.movil.scanActivity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.example.movil.R
import com.example.movil.ScannerSearchFragment
import com.hp.mobile.scan.sdk.*
import com.hp.mobile.scan.sdk.model.*
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.log

class ScanOptFragment : Fragment() {

    private val tempScanFolder = "TempScan"
    private val TAG = "--- ScanOptFragment ---"

    private var tempPathAux = ""

    private lateinit var nameTv : TextView
    private lateinit var statusTv : TextView
    private lateinit var scanButton : Button
    private lateinit var sourceSpinner : Spinner
    private lateinit var facesSpinner : Spinner
    private lateinit var colorSpinner : Spinner
    private lateinit var resolutionSpinner : Spinner
    private lateinit var formatSpinner : Spinner
    private lateinit var combineCheckBox : CheckBox

    private lateinit var sourceAdapter : ArrayAdapter<String>
    private lateinit var facesAdapter : ArrayAdapter<String>
    private lateinit var colorAdapter : ArrayAdapter<String>
    private lateinit var resolutionAdapter : ArrayAdapter<String>
    private lateinit var formatAdapter : ArrayAdapter<String>

    private lateinit var chosenSource : ScanOptions.ScanSource
    private lateinit var chosenNFaces : ScanOptions.Faces
    private lateinit var chosenColorMode : ScanOptions.ColorMode
    private lateinit var chosenFormat : ScanOptions.Format
    private var combineFiles = false
    //private lateinit var chosenRes : List<Resolution>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val theView = inflater.inflate(R.layout.fragment_scan_options, container, false)
        val chosenScanner = (activity as ScanActivity).chosenScanner
        val chosenTicket = (activity as ScanActivity).chosenTicket

        Log.d(TAG, "AAAAAAAAAAAAAAAAAAAa")

        val tempFolder = activity?.getExternalFilesDir(tempScanFolder)
        if(tempFolder!=null && !tempFolder.exists()){
            if(tempFolder.mkdirs()){
                Log.d(tag, "temp folder created")
            }
        }

        PDFBoxResourceLoader.init(context) //Recommended to init

        scanButton = theView.findViewById(R.id.frag_scan_op_button)
        nameTv = theView.findViewById(R.id.frag_scan_op_deviceName)
        statusTv = theView.findViewById(R.id.frag_scan_op_deviceStatus)
        sourceSpinner = theView.findViewById(R.id.frag_scan_op_sourceSpinner)
        facesSpinner = theView.findViewById(R.id.frag_scan_op_facesSpinner)
        colorSpinner = theView.findViewById(R.id.frag_scan_op_colorSpinner)
        resolutionSpinner = theView.findViewById(R.id.frag_scan_op_resSpinner)
        formatSpinner = theView.findViewById(R.id.frag_scan_op_formatSpinner)
        combineCheckBox = theView.findViewById(R.id.frag_scan_op_combineFiles)

        //Adapters for each spinner
        sourceAdapter = ArrayAdapter<String>(activity as ScanActivity, R.layout.support_simple_spinner_dropdown_item)
        facesAdapter = ArrayAdapter<String>(activity as ScanActivity, R.layout.support_simple_spinner_dropdown_item)
        colorAdapter = ArrayAdapter<String>(activity as ScanActivity, R.layout.support_simple_spinner_dropdown_item)
        resolutionAdapter = ArrayAdapter<String>(activity as ScanActivity, R.layout.support_simple_spinner_dropdown_item)
        formatAdapter = ArrayAdapter<String>(activity as ScanActivity, R.layout.support_simple_spinner_dropdown_item)

        sourceAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        facesAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        colorAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        resolutionAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        formatAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)

        sourceSpinner.adapter = sourceAdapter
        facesSpinner.adapter = facesAdapter
        colorSpinner.adapter = colorAdapter
        resolutionSpinner.adapter = resolutionAdapter
        formatSpinner.adapter = formatAdapter

        nameTv.text = chosenScanner.humanReadableName
        statusTv.text = "Va tirando"

        scanButton.setOnClickListener{
            setChosenSettings()
            val newTicket = setTicketOptions(chosenTicket)
            validateTicket(chosenScanner, newTicket) //Validates ticket and prints
        }

        //If source is ADF, show combine files checkbox
        sourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedSource = parent?.getItemAtPosition(position).toString()
                val adfStr = context!!.getString(R.string.ScanOption_source_adf)
                if(selectedSource == adfStr){
                    combineCheckBox.visibility = View.VISIBLE
                }else{
                    combineCheckBox.visibility = View.INVISIBLE
                    combineCheckBox.isSelected = false
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

        }

        getScannerCapabilities(chosenScanner)
        return theView
    }

    /**
     * Fun that gets the chosen scanner capabilities and set
     * them on the screen options
     */
    private fun getScannerCapabilities(theScanner : Scanner){
        theScanner.fetchCapabilities(object :
            ScannerCapabilitiesFetcher.ScannerCapabilitiesListener {
            override fun onFetchCapabilities(cap: ScannerCapabilities?) {
                var hasADF = false //check if adf was already added
                val capabilities = cap!!.capabilities //Map

                //Get sources
                if(capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_SIMPLEX)){
                    if(!hasADF) {
                        hasADF = true
                        sourceAdapter.add(getString(R.string.ScanOption_source_adf))
                        sourceAdapter.notifyDataSetChanged()
                    }
                    facesAdapter.add(getString(R.string.ScanOption_faces_1face))
                    facesAdapter.notifyDataSetChanged()
                }
                if(capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_DUPLEX)){
                    if(!hasADF) {
                        hasADF = true
                        sourceAdapter.add(getString(R.string.ScanOption_source_adf))
                        sourceAdapter.notifyDataSetChanged()
                    }
                    facesAdapter.add(getString(R.string.ScanOption_faces_2face))
                    facesAdapter.notifyDataSetChanged()
                }
                if(capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_PLATEN)){
                    sourceAdapter.add(getString(R.string.ScanOption_source_platen))
                    sourceAdapter.notifyDataSetChanged()
                }

                //Resolution
                val resCap : ResolutionCapability = capabilities.getValue(ScannerCapabilities.SOURCE_CAPABILITY_RESOLUTIONS) as ResolutionCapability
                val resolutions = resCap.discreteResolutions
                resolutions.forEach{
                    resolutionAdapter.add(it.toString())
                }
                resolutionAdapter.notifyDataSetChanged()
            }

            override fun onFetchCapabilitiesError(exception: ScannerException?) {
                Log.d(TAG, "Error obteniendo las caracteristicas del escaner")
                Log.e(TAG, exception?.message!!)
            }
        })

        //Other scanning options
        //Color
        colorAdapter.add(getString(R.string.ScanOption_colorMode_BW))
        colorAdapter.add(getString(R.string.ScanOption_colorMode_grey8))
        colorAdapter.add(getString(R.string.ScanOption_colorMode_grey16))
        colorAdapter.add(getString(R.string.ScanOption_colorMode_color24))
        colorAdapter.add(getString(R.string.ScanOption_colorMode_color48))
        colorAdapter.notifyDataSetChanged()

        //File format
        formatAdapter.add(getString(R.string.ScanOption_format_PDF))
        formatAdapter.add(getString(R.string.ScanOption_format_JPEG))
        formatAdapter.add(getString(R.string.ScanOption_format_RAW))
        formatAdapter.notifyDataSetChanged()
    }

    /**
     * Function that gets scanning options from the chosen settings
     */
    private fun setTicketOptions(ticket : ScanTicket) : ScanTicket{

        //Source
        when(chosenSource){
            ScanOptions.ScanSource.ADF -> { ticket.inputSource = ScanValues.INPUT_SOURCE_ADF }
            ScanOptions.ScanSource.PLATEN -> { ticket.inputSource = ScanValues.INPUT_SOURCE_PLATEN }
            ScanOptions.ScanSource.CAMERA -> { ticket.inputSource = ScanValues.INPUT_SOURCE_CAMERA }
            ScanOptions.ScanSource.AUTO -> { ticket.inputSource = ScanValues.INPUT_SOURCE_AUTO }
        }

        //Sheet faces
        if (chosenNFaces == ScanOptions.Faces.ONE_FACE){
            ticket.setSetting(ScanTicket.SCAN_SETTING_DUPLEX, false)
        }else{
            ticket.setSetting(ScanTicket.SCAN_SETTING_DUPLEX, true)
        }

        when(chosenColorMode){
            ScanOptions.ColorMode.BW -> ticket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_BLACK_AND_WHITE)
            ScanOptions.ColorMode.GREY_8 -> ticket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_GRAYSCALE_8)
            ScanOptions.ColorMode.GREY_16 -> ticket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_GRAYSCALE_16)
            ScanOptions.ColorMode.COLOR_24 -> ticket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_RGB_24)
            ScanOptions.ColorMode.COLOR_48 -> ticket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_RGB_48)
        }

        when(chosenFormat){
            ScanOptions.Format.JPEG -> ticket.setSetting(ScanTicket.SCAN_SETTING_FORMAT, ScanValues.DOCUMENT_FORMAT_JPEG)
            ScanOptions.Format.PDF -> ticket.setSetting(ScanTicket.SCAN_SETTING_FORMAT, ScanValues.DOCUMENT_FORMAT_PDF)
            ScanOptions.Format.RAW -> ticket.setSetting(ScanTicket.SCAN_SETTING_FORMAT, ScanValues.DOCUMENT_FORMAT_RAW)
        }

        //TODO Resolucion

        return ticket
    }

    /**
     * Fun that validates chosen options with the scanner
     */
    private fun validateTicket(scanner : Scanner, ticket : ScanTicket){
        scanner.validateTicket(ticket, object : ScanTicketValidator.ScanTicketValidationListener{
            override fun onScanTicketValidationComplete(p0: ScanTicket?) {
                startScanning(scanner, ticket)
            }

            override fun onScanTicketValidationError(e: ScannerException?) {
                val alertDialog: AlertDialog = this.let {
                    val builder = AlertDialog.Builder(activity!!)
                    builder.apply {

                        var message = "Desconocido"

                        if(e is AdfException){
                            message = "Error en el ADF.\n Estado: ${e.adfStatus}"
                        }else {
                            when (e!!.reason) {
                                ScannerException.REASON_AUTHENTICATION_REQUIRED -> message = "AUTHENTICATION_REQUIRED"
                                ScannerException.REASON_CANCELED_BY_DEVICE -> message = "CANCELED_BY_DEVICE"
                                ScannerException.REASON_CANCELED_BY_USER -> message = "CANCELED_BY_USER"
                                ScannerException.REASON_CONNECTION_ERROR -> message = "CONNECTION ERROR"
                                ScannerException.REASON_CONNECTION_TIMEOUT -> message = "CONNECTION_TIMEOUT"
                                ScannerException.REASON_DEVICE_BUSY -> message = "DEVICE_BUSY"
                                ScannerException.REASON_DEVICE_INTERNAL_ERROR -> message = "DEVICE_INTERNAL_ERROR"
                                ScannerException.REASON_DEVICE_STOPPED -> message = "DEVICE_STOPPED"
                                ScannerException.REASON_DEVICE_UNAVAILABLE -> message = "DEVICE_UNAVAILABLE"
                                ScannerException.REASON_INVALID_SCAN_TICKET -> message = "INVALID_SCAN_TICKET"
                                ScannerException.REASON_OPERATION_IS_ALREADY_STARTED -> message = "OPERATION_IS_ALREADY_STARTED"
                                ScannerException.REASON_SCAN_RESULT_WRITE_ERROR -> message = "RESULT_WRITE_ERROR"
                            }
                        }

                        setTitle("ERROR")
                        setMessage(message)
                        setNeutralButton(R.string.accept
                        ) { dialog, _ ->
                            dialog.dismiss()
                        }
                    }
                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog.show()
            }
        })
    }

    /**
     * Function that performs the scan
     */
    private fun startScanning(theScanner : Scanner, theTicket : ScanTicket){
        //Log.d(tag, "startScanning()")

        //Open scanning fragment
        val fragmentManager = activity?.supportFragmentManager!!
        val fragmentTransaction = fragmentManager.beginTransaction()
        val scanningFragment = PerformingScanFragment()
        scanningFragment.show(fragmentTransaction, "scanningFragment")

        //Create the temp file to save the scanning
        val tempFolder = activity?.getExternalFilesDir(tempScanFolder)!!
        //val tempFile = File(tempFolder, "tempScanFile")
        val scanResultUris = arrayListOf<Uri>()

        //tempPathAux = tempFile.absolutePath
        tempPathAux = tempFolder.absolutePath
        //Log.d(tag, "Temp file abs path: $tempPathAux")

        theScanner.scan(
            tempFolder.absolutePath,
            theTicket,
            object : ScanCapture.ScanningProgressListener {
                override fun onScanningPageDone(p0: ScanPage?) {
                    //Toast.makeText(this@ScanActivity, "Pagina escaneada", Toast.LENGTH_LONG).show()
                    Log.d(tag, "Page scanned")
                    //scanResultUris = p0?.uri
                    scanResultUris.add(p0?.uri!!)
                }

                override fun onScanningComplete() {
                    //Toast.makeText(this@ScanActivity, "Escaneo completado", Toast.LENGTH_LONG).show()
                    Log.d(tag, "Scanning completed")


                    if (combineFiles) {
                        //Create inputStreams from the ScanPages
                        val resInStreams = mutableListOf<InputStream>()
                        scanResultUris.forEach {
                            val inStream = context!!.contentResolver.openInputStream(it)
                            resInStreams.add(inStream!!)
                        }

                        val pdfMerger = PDFMergerUtility()
                        resInStreams.forEach { pdfMerger.addSource(it) }
                        val combinedFile = File("combinedFile.pdf", tempScanFolder)
                        val outStream = FileOutputStream(combinedFile)
                        pdfMerger.destinationStream = outStream
                        pdfMerger.mergeDocuments(false) //TODO ver que es el false
                        outStream.close()

                        //Delete all elements and insert the new one
                        scanResultUris.forEach{
                            File(it.path!!).delete()
                        }
                        scanResultUris.clear()
                        scanResultUris.add(Uri.fromFile(combinedFile))
                        Log.d(tag, "COMBINED FILE URI: ${Uri.fromFile(combinedFile)}")
                    }

                    val i = Intent(activity?.applicationContext, ScanPreview::class.java)
                    //i.putExtra("tempUris", scanResultUris)
                    i.putParcelableArrayListExtra("tempUris", scanResultUris)
                    i.putExtra("chosenFormat", chosenFormat)
                    startActivity(i)
                }

                override fun onScanningError(theException: ScannerException?) {
                    try {

                        theScanner.cancelScanning()
                        deleteTempFiles(tempFolder)
                        //Toast.makeText(applicationContext, "Error, ${theException!!.message}", Toast.LENGTH_LONG).show()

                        scanningFragment.dismiss()
                        //supportFragmentManager.beginTransaction().remove(scanningFragment)
                        val scanErrorFragment = ScanErrorFragment()
                        scanErrorFragment.show(fragmentTransaction, "scanErrorFragment")

                        throw theException!!

                    } catch (e: AdfException) {
                        Log.d(tag, "AdfException\n Status: ${e.adfStatus}")

                    } catch (e: ScannerException) {
                        Log.d(tag, "ScannerException\n Reason: ${e.reason}")
                    }
                }

            })
    }

    private fun deleteTempFiles(tempFileOrFolder : File){
        if(tempFileOrFolder.isDirectory){
            val fileList = tempFileOrFolder.listFiles()
            if(fileList!=null && fileList.isNotEmpty()) {
                for (i in fileList) {
                    deleteTempFiles(i)
                }
            }
        }
        tempFileOrFolder.delete()
    }

    /**
     * Fun that set chosen options from the UI
     */
    private fun setChosenSettings(){

        val auto = "auto"

        val source = if(sourceSpinner.selectedItem != null) sourceSpinner.selectedItem.toString()
                     else auto

        val nFaces = if(facesSpinner.selectedItem != null) facesSpinner.selectedItem.toString()
                     else auto
        val color = colorSpinner.selectedItem.toString()
        val format = formatSpinner.selectedItem.toString()
        //val resolution = resolutionSpinner.selectedItem.toString()
        //TODO Resolucion

        //set source
        when(source){
            getString(R.string.ScanOption_source_adf) -> chosenSource = ScanOptions.ScanSource.ADF
            getString(R.string.ScanOption_source_platen) -> chosenSource = ScanOptions.ScanSource.PLATEN
            getString(R.string.ScanOption_source_camera) -> chosenSource = ScanOptions.ScanSource.CAMERA
            else -> chosenSource = ScanOptions.ScanSource.AUTO
        }

        //set number of faces
        when(nFaces){
            getString(R.string.ScanOption_faces_1face) -> chosenNFaces = ScanOptions.Faces.ONE_FACE
            getString(R.string.ScanOption_faces_2face) -> chosenNFaces = ScanOptions.Faces.TWO_FACES
            else -> chosenNFaces = ScanOptions.Faces.ONE_FACE
        }

        //set colot
        when(color){
            getString(R.string.ScanOption_colorMode_BW) -> chosenColorMode = ScanOptions.ColorMode.BW
            getString(R.string.ScanOption_colorMode_grey8) -> chosenColorMode = ScanOptions.ColorMode.GREY_8
            getString(R.string.ScanOption_colorMode_grey16) -> chosenColorMode = ScanOptions.ColorMode.GREY_16
            getString(R.string.ScanOption_colorMode_color24) -> chosenColorMode = ScanOptions.ColorMode.COLOR_24
            getString(R.string.ScanOption_colorMode_color48) -> chosenColorMode = ScanOptions.ColorMode.COLOR_48
        }

        //set format
        when(format){
            getString(R.string.ScanOption_format_PDF) -> chosenFormat = ScanOptions.Format.PDF
            getString(R.string.ScanOption_format_JPEG) -> chosenFormat = ScanOptions.Format.JPEG
            getString(R.string.ScanOption_format_RAW) -> chosenFormat = ScanOptions.Format.RAW
        }

        //TODO res
        //Crear array de resoluciones posibles y coger de ahi directamente

        combineFiles = combineCheckBox.isChecked
    }

}