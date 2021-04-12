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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import com.example.movil.R
import com.hp.mobile.scan.sdk.*
import com.hp.mobile.scan.sdk.model.*
import java.io.File
import java.lang.Exception


class ScanOptFragment : Fragment() {

    private val tempScanFolder = "TempScan"
    private val TAG = "--- ScanOptFragment ---"

    private lateinit var viewModel : ScanActivityViewModel

    private lateinit var nameTv : TextView
    private lateinit var deviceStatusTv : TextView
    private lateinit var adfStatusTv : TextView
    private lateinit var scanButton : Button
    private lateinit var sourceSpinner : Spinner
    private lateinit var facesSpinner : Spinner
    private lateinit var colorSpinner : Spinner
    private lateinit var resolutionSpinner : Spinner
    private lateinit var formatSpinner : Spinner

    private lateinit var sourceAdapter : ArrayAdapter<String>
    private lateinit var facesAdapter : ArrayAdapter<String>
    private lateinit var colorAdapter : ArrayAdapter<String>
    private lateinit var resolutionAdapter : ArrayAdapter<String>
    private lateinit var formatAdapter : ArrayAdapter<String>

    /*private lateinit var chosenSource : ScanOptions.ScanSource
    private lateinit var chosenNFaces : ScanOptions.Faces
    private lateinit var chosenColorMode : ScanOptions.ColorMode
    private lateinit var chosenFormat : ScanOptions.Format
    private lateinit var chosenRes : Resolution*/
    //private lateinit var resolutionList : List<Resolution> //List of res given by the scannerCapabilities
    //private var isResSelected = false //Control when a resolution is selected

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)


        val theView = inflater.inflate(R.layout.fragment_scan_options, container, false)

        viewModel = ViewModelProvider(this).get(ScanActivityViewModel::class.java)

        /*viewModel.chosenScanner = (activity as ScanActivity).chosenScanner
        viewModel.chosenTicket = (activity as ScanActivity).chosenTicket*/
        if(viewModel.chosenScanner == null || viewModel.chosenTicket == null){
            showNoScannerDialog()
        }

        val tempFolder = activity?.getExternalFilesDir(tempScanFolder)
        if(tempFolder!=null && !tempFolder.exists()){
            if(tempFolder.mkdirs()){
                Log.d(tag, "temp folder created")
            }
        }

        scanButton = theView.findViewById(R.id.frag_scan_op_button)
        nameTv = theView.findViewById(R.id.frag_scan_op_deviceName)
        deviceStatusTv = theView.findViewById(R.id.frag_scan_op_deviceStatus)
        adfStatusTv = theView.findViewById(R.id.frag_scan_op_adfStatus)
        sourceSpinner = theView.findViewById(R.id.frag_scan_op_sourceSpinner)
        facesSpinner = theView.findViewById(R.id.frag_scan_op_facesSpinner)
        colorSpinner = theView.findViewById(R.id.frag_scan_op_colorSpinner)
        resolutionSpinner = theView.findViewById(R.id.frag_scan_op_resSpinner)
        formatSpinner = theView.findViewById(R.id.frag_scan_op_formatSpinner)

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

        nameTv.text = viewModel.chosenScanner.humanReadableName

        scanButton.setOnClickListener{
            setChosenSettings()
            //val newTicket = viewModel.setTicketOptions()
            //validateTicket(viewModel.chosenScanner, newTicket) //Validates ticket and prints
            viewModel.setTicketOptions()
            validateTicket()
        }

        //Refresh settings depending on the source
        sourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                    val source = parent?.getItemAtPosition(position).toString()
                    val nFaces = facesSpinner.selectedItem.toString()

                    if(source == getString(R.string.ScanOption_source_adf) && nFaces == getString(R.string.ScanOption_faces_1face)){
                        viewModel.setSource(ScanOptions.ScanSource.ADF_SIMPLEX)
                        viewModel.chosenNFaces = ScanOptions.Faces.ONE_FACE
                        facesSpinner.isFocusable = true
                    }else if (source == getString(R.string.ScanOption_source_adf) && nFaces == getString(R.string.ScanOption_faces_2face)){
                        viewModel.setSource(ScanOptions.ScanSource.ADF_DUPLEX)
                        viewModel.chosenNFaces = ScanOptions.Faces.TWO_FACES
                        facesSpinner.isFocusable = true
                    }else if(source == getString(R.string.ScanOption_source_platen)){
                        viewModel.setSource(ScanOptions.ScanSource.PLATEN)
                        facesSpinner.isFocusable = false
                    }else if(source == getString(R.string.ScanOption_source_camera)){
                        viewModel.setSource(ScanOptions.ScanSource.CAMERA)
                        facesSpinner.isFocusable = false
                    }else {
                        viewModel.chosenSource = ScanOptions.ScanSource.AUTO
                        facesSpinner.isFocusable = false
                    }
                    updateSettings()
                }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        viewModel.chosenScanner.monitorDeviceStatus(DeviceStatusMonitor.DEFAULT_MONITORING_PERIOD, object: DeviceStatusMonitor.ScannerStatusListener{
            override fun onStatusChanged(scannerSta: Int, adfSta: Int) {
                val scannerStr = when(scannerSta){
                    DeviceStatusMonitor.SCANNER_STATUS_IDLE -> getString(R.string.SCANNER_STATUS_IDLE)
                    DeviceStatusMonitor.SCANNER_STATUS_PROCESSING -> getString(R.string.SCANNER_STATUS_PROCESSING)
                    DeviceStatusMonitor.SCANNER_STATUS_STOPPED -> getString(R.string.SCANNER_STATUS_STOPPED)
                    DeviceStatusMonitor.SCANNER_STATUS_TESTING -> getString(R.string.SCANNER_STATUS_TESTING)
                    DeviceStatusMonitor.SCANNER_STATUS_UNAVAILABLE -> getString(R.string.SCANNER_STATUS_UNAVAILABLE)
                    DeviceStatusMonitor.SCANNER_STATUS_UNKNOWN -> getString(R.string.SCANNER_STATUS_UNKNOWN)
                    else ->  getString(R.string.SCANNER_STATUS_UNKNOWN)
                }
                val adfStr = getAdfStatusFromInt(adfSta)
                deviceStatusTv.text = scannerStr
                adfStatusTv.text = adfStr
            }
            override fun onStatusError(e: ScannerException?) {
                if (e != null) {
                    Log.e(TAG, e.message!!)
                }
            }
        })//Monitoring device status

        getScannerCapabilities()
        return theView
    }

    /**
     * Fun that gets the chosen scanner capabilities and set
     * them on the screen options
     */
    private fun getScannerCapabilities(){
        viewModel.chosenScanner.fetchCapabilities(object :
            ScannerCapabilitiesFetcher.ScannerCapabilitiesListener {
            override fun onFetchCapabilities(cap: ScannerCapabilities?) {

                if(cap == null){
                    //TODO
                    Log.d(TAG, "Scanner Capabilities == null")
                    return
                }

                var hasADF = false //check if adf was already added
                val capabilities = cap.capabilities //Map
                var isSettingsSet = false //Used to set the 1st source options


                //Get sources
                if(capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_SIMPLEX)){
                    viewModel.adfSimplex = capabilities[ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_SIMPLEX] as MutableMap<String, Any>
                    if(!hasADF) {
                        hasADF = true
                        sourceAdapter.add(getString(R.string.ScanOption_source_adf))
                        sourceAdapter.notifyDataSetChanged()
                    }
                    facesAdapter.add(getString(R.string.ScanOption_faces_1face))
                    facesAdapter.notifyDataSetChanged()

                    if(!isSettingsSet){
                        viewModel.setSource(ScanOptions.ScanSource.ADF_SIMPLEX)
                        updateSettings()
                        isSettingsSet = true
                    }
                }
                if(capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_DUPLEX)){
                    viewModel.adfDuplex = capabilities[ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_DUPLEX] as MutableMap<String, Any>
                    if(!hasADF) {
                        sourceAdapter.add(getString(R.string.ScanOption_source_adf))
                        sourceAdapter.notifyDataSetChanged()
                    }
                    facesAdapter.add(getString(R.string.ScanOption_faces_2face))
                    facesAdapter.notifyDataSetChanged()

                    if(!isSettingsSet){
                        viewModel.setSource(ScanOptions.ScanSource.ADF_DUPLEX)
                        updateSettings()
                        isSettingsSet = true
                    }
                }
                if(capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_PLATEN)){
                    viewModel.platen = capabilities[ScannerCapabilities.SCANNER_CAPABILITY_IS_PLATEN] as MutableMap<String, Any>
                    sourceAdapter.add(getString(R.string.ScanOption_source_platen))
                    sourceAdapter.notifyDataSetChanged()

                    if(!isSettingsSet){
                        viewModel.setSource(ScanOptions.ScanSource.PLATEN)
                        updateSettings()
                        isSettingsSet = true
                    }
                }

                if(capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_CAMERA)){
                    viewModel.camera = capabilities[ScannerCapabilities.SCANNER_CAPABILITY_IS_CAMERA] as MutableMap<String, Any>
                    sourceAdapter.add(getString(R.string.ScanOption_source_camera))
                    sourceAdapter.notifyDataSetChanged()

                    if(!isSettingsSet){
                        viewModel.setSource(ScanOptions.ScanSource.CAMERA)
                        updateSettings()
                        isSettingsSet = true
                    }
                }

                //Scanner returned 0 sources
                if(!isSettingsSet){
                    viewModel.setSource(ScanOptions.ScanSource.AUTO)
                    updateSettings()
                }

            }

            override fun onFetchCapabilitiesError(exception: ScannerException?) {
                Log.d(TAG, "Error obteniendo las caracteristicas del escaner")
                Log.e(TAG, exception?.message!!)

                val alertDialog: AlertDialog = this.let {
                    val builder = AlertDialog.Builder(activity!!)
                    builder.apply {

                        val message = getReasonFromException(exception)

                        setTitle(getString(R.string.fragScanCapabilitiesErrorLabel))
                        setMessage(message)
                        setNeutralButton(R.string.accept) { _, _ ->
                            startActivity(Intent(context, ScanActivity::class.java))
                        }
                        setCancelable(false)
                    }
                    // Create the AlertDialog
                    builder.create()
                }
                alertDialog.show()
            }
        })
    }

    /**
     * Sets source available settings on the UI
     */
    private fun updateSettings(){

        //Resolution
        viewModel.resolutionList.forEach{
            resolutionAdapter.add(it.toString())
        }
        resolutionAdapter.notifyDataSetChanged()

        //Color
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_RGB_48)) colorAdapter.add(getString(R.string.ScanOption_colorMode_color48))
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_RGB_24)) colorAdapter.add(getString(R.string.ScanOption_colorMode_color24))
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_GRAYSCALE_16)) colorAdapter.add(getString(R.string.ScanOption_colorMode_grey16))
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_GRAYSCALE_8)) colorAdapter.add(getString(R.string.ScanOption_colorMode_grey8))
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_BLACK_AND_WHITE)) colorAdapter.add(getString(R.string.ScanOption_colorMode_BW))
        colorAdapter.notifyDataSetChanged()

        //Format
        if(viewModel.resultFormats.contains(ScanValues.DOCUMENT_FORMAT_PDF)) formatAdapter.add(getString(R.string.ScanOption_format_PDF))
        if(viewModel.resultFormats.contains(ScanValues.DOCUMENT_FORMAT_JPEG)) formatAdapter.add(getString(R.string.ScanOption_format_JPEG))
        if(viewModel.resultFormats.contains(ScanValues.DOCUMENT_FORMAT_RAW)) formatAdapter.add(getString(R.string.ScanOption_format_RAW))
        formatAdapter.notifyDataSetChanged()
    }


    /**
     * Fun that validates chosen options with the scanner
     */
    private fun validateTicket(){
        viewModel.chosenScanner.validateTicket(viewModel.chosenTicket, object : ScanTicketValidator.ScanTicketValidationListener{
            override fun onScanTicketValidationComplete(p0: ScanTicket?) {
                startScanning()
            }

            override fun onScanTicketValidationError(e: ScannerException?) {
                val alertDialog: AlertDialog = this.let {
                    val builder = AlertDialog.Builder(activity!!)
                    builder.apply {

                        val message = getReasonFromException(e)

                        setTitle(getString(R.string.fragScanErrorLabel))
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
    private fun startScanning(){
        //Log.d(tag, "startScanning()")

        //Open scanning fragment
        val fragmentManager = activity?.supportFragmentManager!!
        var fragmentTransaction = fragmentManager.beginTransaction()
        val scanningFragment = PerformingScanFragment()
        scanningFragment.show(fragmentTransaction, "scanningFragment")

        //Create the temp file to save the scanning
        val tempFolder = activity?.getExternalFilesDir(tempScanFolder)!!
        val scanResultUris = arrayListOf<Uri>()

        viewModel.chosenScanner.scan(
            tempFolder.absolutePath,
            viewModel.chosenTicket,
            object : ScanCapture.ScanningProgressListener {
                override fun onScanningPageDone(p0: ScanPage?) {
                    Log.d(tag, "Page scanned")
                    if(p0 != null) {
                        scanResultUris.add(p0.uri!!)
                    }
                }

                override fun onScanningComplete() {
                    Log.d(tag, "Scanning completed")

                    val i = Intent(activity?.applicationContext, ScanPreview::class.java)
                    i.putParcelableArrayListExtra("tempUris", scanResultUris)
                    i.putExtra("chosenFormat", viewModel.chosenFormat)
                    startActivity(i)
                }

                override fun onScanningError(theException: ScannerException?) {
                    try {

                        viewModel.chosenScanner.cancelScanning()
                        deleteTempFiles(tempFolder)

                        scanningFragment.dismiss()
                        val scanErrorFragment = ScanErrorFragment()
                        fragmentTransaction = fragmentManager.beginTransaction()
                        scanErrorFragment.setReason(getReasonFromException(theException))
                        scanErrorFragment.show(fragmentTransaction, "scanErrorFragment")

                    } catch (e: AdfException) {
                        Log.d(tag, "AdfException\n Status: ${e.adfStatus}")

                    } catch (e: ScannerException) {
                        Log.d(tag, "ScannerException\n Reason: ${e.reason}")
                    } catch (e: Exception){
                        Log.d(tag, "Excepcion no controlada")
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

        val resolution = if(resolutionSpinner.selectedItem != null) resolutionSpinner.selectedItem.toString()
                    else auto

        val color = if(colorSpinner.selectedItem != null) colorSpinner.selectedItem.toString()
                    else auto

        val format = if(formatSpinner.selectedItem != null) formatSpinner.selectedItem.toString()
                    else auto


        //set source
/*        chosenSource = when(source){
            getString(R.string.ScanOption_source_adf) -> ScanOptions.ScanSource.ADF
            getString(R.string.ScanOption_source_platen) -> ScanOptions.ScanSource.PLATEN
            getString(R.string.ScanOption_source_camera) -> ScanOptions.ScanSource.CAMERA
            else -> ScanOptions.ScanSource.AUTO
        }

        //set number of faces
        chosenNFaces = when(nFaces){
            getString(R.string.ScanOption_faces_1face) -> ScanOptions.Faces.ONE_FACE
            getString(R.string.ScanOption_faces_2face) -> ScanOptions.Faces.TWO_FACES
            else -> ScanOptions.Faces.ONE_FACE
        }*/

        if(source == getString(R.string.ScanOption_source_adf) && nFaces == getString(R.string.ScanOption_faces_1face)){
            viewModel.chosenSource = ScanOptions.ScanSource.ADF_SIMPLEX
            viewModel.chosenNFaces = ScanOptions.Faces.ONE_FACE
        }else if (source == getString(R.string.ScanOption_source_adf) && nFaces == getString(R.string.ScanOption_faces_2face)){
            viewModel.chosenSource = ScanOptions.ScanSource.ADF_DUPLEX
            viewModel.chosenNFaces = ScanOptions.Faces.TWO_FACES
        }else if(source == getString(R.string.ScanOption_source_platen)){
            viewModel.chosenSource = ScanOptions.ScanSource.PLATEN

        }else if(source == getString(R.string.ScanOption_source_camera)){
            viewModel.chosenSource = ScanOptions.ScanSource.CAMERA
        }else {
            viewModel.chosenSource = ScanOptions.ScanSource.AUTO
        }

        //set color
        when(color){
            getString(R.string.ScanOption_colorMode_BW) -> viewModel.chosenColorMode = ScanOptions.ColorMode.BW
            getString(R.string.ScanOption_colorMode_grey8) -> viewModel.chosenColorMode = ScanOptions.ColorMode.GREY_8
            getString(R.string.ScanOption_colorMode_grey16) -> viewModel.chosenColorMode = ScanOptions.ColorMode.GREY_16
            getString(R.string.ScanOption_colorMode_color24) -> viewModel.chosenColorMode = ScanOptions.ColorMode.COLOR_24
            getString(R.string.ScanOption_colorMode_color48) -> viewModel.chosenColorMode = ScanOptions.ColorMode.COLOR_48
        }

        //set format
        when(format){
            getString(R.string.ScanOption_format_PDF) -> viewModel.chosenFormat = ScanOptions.Format.PDF
            getString(R.string.ScanOption_format_JPEG) -> viewModel.chosenFormat = ScanOptions.Format.JPEG
            getString(R.string.ScanOption_format_RAW) -> viewModel.chosenFormat = ScanOptions.Format.RAW
        }

        //set resolution
        when(resolution){
            auto -> {/*Do nothing*/}
            "AUTO" -> {/*Do nothing*/}
            else -> {
                viewModel.isResSelected = true
                val chosenResPosition = resolutionSpinner.selectedItemPosition
                viewModel.chosenRes = viewModel.resolutionList[chosenResPosition]
            }
        }

    }


    fun getReasonFromException(e: ScannerException?) : String{
        var message = getString(R.string.REASON_UNKNOWN)

        when (e) {
            null -> {
                return message
            }
            is AdfException -> {
                message = "Error en el ADF:\n  ${getAdfStatusFromInt(e.adfStatus)}"
            }
            else -> {
                when (e.reason) {
                    ScannerException.REASON_AUTHENTICATION_REQUIRED -> message = getString(R.string.REASON_AUTHENTICATION_REQUIRED)
                    ScannerException.REASON_CANCELED_BY_DEVICE -> message = getString(R.string.REASON_CANCELED_BY_DEVICE)
                    ScannerException.REASON_CANCELED_BY_USER -> message = getString(R.string.REASON_CANCELED_BY_USER)
                    ScannerException.REASON_CONNECTION_ERROR -> message = getString(R.string.REASON_CONNECTION_ERROR)
                    ScannerException.REASON_CONNECTION_TIMEOUT -> message = getString(R.string.REASON_CONNECTION_TIMEOUT)
                    ScannerException.REASON_DEVICE_BUSY -> message = getString(R.string.REASON_DEVICE_BUSY)
                    ScannerException.REASON_DEVICE_INTERNAL_ERROR -> message = getString(R.string.REASON_DEVICE_INTERNAL_ERROR)
                    ScannerException.REASON_DEVICE_STOPPED -> message = getString(R.string.REASON_DEVICE_STOPPED)
                    ScannerException.REASON_DEVICE_UNAVAILABLE -> message = getString(R.string.REASON_DEVICE_UNAVAILABLE)
                    ScannerException.REASON_INVALID_SCAN_TICKET -> message = getString(R.string.REASON_INVALID_SCAN_TICKET)
                    ScannerException.REASON_OPERATION_IS_ALREADY_STARTED -> message = getString(R.string.REASON_OPERATION_IS_ALREADY_STARTED)
                    ScannerException.REASON_SCAN_RESULT_WRITE_ERROR -> message = getString(R.string.REASON_SCAN_RESULT_WRITE_ERROR)
                }
            }
        }

        return message
    }

    fun getAdfStatusFromInt(sta : Int) : String{
        return when(sta){
            DeviceStatusMonitor.ADF_STATUS_DUPLEX_PAGE_TOO_LONG -> getString(R.string.ADF_STATUS_DUPLEX_PAGE_TOO_LONG)
            DeviceStatusMonitor.ADF_STATUS_DUPLEX_PAGE_TOO_SHORT -> getString(R.string.ADF_STATUS_DUPLEX_PAGE_TOO_SHORT)
            DeviceStatusMonitor.ADF_STATUS_EMPTY -> getString(R.string.ADF_STATUS_EMPTY)
            DeviceStatusMonitor.ADF_STATUS_HATCH_OPEN -> getString(R.string.ADF_STATUS_HATCH_OPEN)
            DeviceStatusMonitor.ADF_STATUS_INPUT_TRAY_FAILED -> getString(R.string.ADF_STATUS_INPUT_TRAY_FAILED)
            DeviceStatusMonitor.ADF_STATUS_INPUT_TRAY_OVERLOADED -> getString(R.string.ADF_STATUS_INPUT_TRAY_OVERLOADED)
            DeviceStatusMonitor.ADF_STATUS_JAM -> getString(R.string.ADF_STATUS_JAM)
            DeviceStatusMonitor.ADF_STATUS_LOADED -> getString(R.string.ADF_STATUS_LOADED)
            DeviceStatusMonitor.ADF_STATUS_MISPICK -> getString(R.string.ADF_STATUS_MISPICK)
            DeviceStatusMonitor.ADF_STATUS_MULTIPICK_DETECTED -> getString(R.string.ADF_STATUS_MULTIPICK_DETECTED)
            DeviceStatusMonitor.ADF_STATUS_PROCESSING -> getString(R.string.ADF_STATUS_PROCESSING)
            DeviceStatusMonitor.ADF_STATUS_UNKNOWN -> getString(R.string.ADF_STATUS_UNKNOWN)
            DeviceStatusMonitor.ADF_STATUS_UNSUPPORTED -> getString(R.string.ADF_STATUS_UNSUPPORTED)
            else -> getString(R.string.ADF_STATUS_UNKNOWN)
        }
    }

    private fun showNoScannerDialog(){
        val alertDialog: AlertDialog = this.let {
            val builder = AlertDialog.Builder(requireActivity())
            builder.apply {
                setTitle(getString(R.string.ScannerLost))
                setNeutralButton(R.string.accept
                ) { _, _ ->
                    requireActivity().recreate()
                }
                setCancelable(false)
            }
            // Create the AlertDialog
            builder.create()
        }
        alertDialog.show()
    }
}