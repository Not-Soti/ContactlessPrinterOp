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
import androidx.lifecycle.ViewModelProvider
import com.example.movil.R
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.hp.mobile.scan.sdk.*
import com.hp.mobile.scan.sdk.model.*
import java.io.File
import java.lang.Exception


class ScanSettingsFragment : Fragment() {

    private val tempScanFolder = "TempScan"
    private val TAG = "--- ScanOptFragment ---"

    private val previewActivityRequestCode = 1
    private val previewActivityResultOK = 1

    private lateinit var viewModel : ScanActivityViewModel

    private lateinit var nameTv : TextView
    private lateinit var deviceStatusTv : TextView
    private lateinit var adfStatusTv : TextView
    private lateinit var scanButton : ExtendedFloatingActionButton
    private lateinit var sourceSpinner : Spinner
    private lateinit var facesSpinner : Spinner
    private lateinit var colorSpinner : Spinner
    private lateinit var resolutionSpinner : Spinner
    private lateinit var formatSpinner : Spinner

    //Adapters for each spinner setting
    private lateinit var sourceAdapter : ArrayAdapter<String>
    private lateinit var facesAdapter : ArrayAdapter<String>
    private lateinit var colorAdapter : ArrayAdapter<String>
    private lateinit var resolutionAdapter : ArrayAdapter<String>
    private lateinit var formatAdapter : ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val theView = inflater.inflate(R.layout.fragment_scan_settings, container, false)

        viewModel = ViewModelProvider(requireActivity()).get(ScanActivityViewModel::class.java)

        //If the scanner or the ticket is null, shows a dialog that restarts the activity
        //so a new scanner can be chosen
        if(viewModel.chosenScanner == null || viewModel.chosenTicket == null){
            showNoScannerDialog()
        }

        //gets the scan result temporal folder where the scan results are saved
        val tempFolder = activity?.getExternalFilesDir(tempScanFolder)
        if(tempFolder!=null && !tempFolder.exists()){
            if(tempFolder.mkdirs()){
                //The folder is created
            }
        }

        //Sets the backPressed status that makes the ScanActivity perform correctly on onBackPressed()
        (activity as ScanActivity).backPressedStatus = 1

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

        //Sets the scanner name on it's TextView
        nameTv.text = viewModel.chosenScanner!!.humanReadableName

        //When the scan button is pressed
        scanButton.setOnClickListener{
            setChosenSettings() //Gets the user chosen settings from the UI
            viewModel.setTicketOptions() //Sets these settings on the ScanTicket
            validateTicket() //Lets the scanner check if it's compatible with the chosen settings
        }

        //Refresh settings depending on the source, since each source can have different settings
        sourceSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                    //Gets the chosen source in order to get its capabilities
                    val source = parent?.getItemAtPosition(position).toString()

                    /*
                     Gets the number of faces, required to get the chosen ADF
                     If Source == ADF and Number of faces == 1 -> the source is  ADF_Simplex
                     If Source == ADF and Number of faces == 2 -> the source is  ADF_Duplex
                     */
                    val nFaces = facesSpinner.selectedItem.toString()

                    /*
                     The number of faces can only be chousen when chosing the ADF (if duplex
                     is available). Otherwise only 1 face can be scanned at the same time.
                     */
                    if(source == getString(R.string.ScanOption_source_adf) && nFaces == getString(R.string.ScanOption_faces_1face)){
                        viewModel.setSource(ScanSettingsHelper.ScanSource.ADF_SIMPLEX)
                        viewModel.chosenNFaces = ScanSettingsHelper.Faces.ONE_FACE
                        facesSpinner.isFocusable = true
                    }else if (source == getString(R.string.ScanOption_source_adf) && nFaces == getString(R.string.ScanOption_faces_2face)){
                        viewModel.setSource(ScanSettingsHelper.ScanSource.ADF_DUPLEX)
                        viewModel.chosenNFaces = ScanSettingsHelper.Faces.TWO_FACES
                        facesSpinner.isFocusable = true
                    }else if(source == getString(R.string.ScanOption_source_platen)){
                        viewModel.setSource(ScanSettingsHelper.ScanSource.PLATEN)
                        facesSpinner.isFocusable = false
                    }else if(source == getString(R.string.ScanOption_source_camera)){
                        viewModel.setSource(ScanSettingsHelper.ScanSource.CAMERA)
                        facesSpinner.isFocusable = false
                    }else {
                        viewModel.chosenSource = ScanSettingsHelper.ScanSource.AUTO
                        facesSpinner.isFocusable = false
                    }
                    updateSettings()
                }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        }

        //Starts monitoring the scanner status
        viewModel.chosenScanner!!.monitorDeviceStatus(DeviceStatusMonitor.DEFAULT_MONITORING_PERIOD, object: DeviceStatusMonitor.ScannerStatusListener{
            override fun onStatusChanged(scannerSta: Int, adfSta: Int) {
                //Gets the scanner status
                val scannerStr = when(scannerSta){
                    DeviceStatusMonitor.SCANNER_STATUS_IDLE -> getString(R.string.SCANNER_STATUS_IDLE)
                    DeviceStatusMonitor.SCANNER_STATUS_PROCESSING -> getString(R.string.SCANNER_STATUS_PROCESSING)
                    DeviceStatusMonitor.SCANNER_STATUS_STOPPED -> getString(R.string.SCANNER_STATUS_STOPPED)
                    DeviceStatusMonitor.SCANNER_STATUS_TESTING -> getString(R.string.SCANNER_STATUS_TESTING)
                    DeviceStatusMonitor.SCANNER_STATUS_UNAVAILABLE -> getString(R.string.SCANNER_STATUS_UNAVAILABLE)
                    DeviceStatusMonitor.SCANNER_STATUS_UNKNOWN -> getString(R.string.SCANNER_STATUS_UNKNOWN)
                    else ->  getString(R.string.SCANNER_STATUS_UNKNOWN)
                }
                //Gets the ADF status
                val adfStr = getAdfStatusFromInt(adfSta) //TODO mirar si no hay adf

                //Sets both status on each TextView
                deviceStatusTv.text = scannerStr
                adfStatusTv.text = adfStr
            }
            override fun onStatusError(e: ScannerException?) {
                if (e != null) {
                    Log.e(TAG, e.message!!)
                }
            }
        })//Monitoring device status

        getScannerCapabilities() //Asks the scanner for it's available settings
        return theView
    }

    /**
     * Fun that gets the chosen scanner capabilities and set
     * them on the screen options
     */
    private fun getScannerCapabilities(){
        viewModel.chosenScanner!!.fetchCapabilities(object :
            ScannerCapabilitiesFetcher.ScannerCapabilitiesListener {
            override fun onFetchCapabilities(cap: ScannerCapabilities?) {

                if (cap == null) {
                    Log.d(TAG, "Scanner Capabilities == null")
                    return
                }

                var hasADF = false //check if adf was already added
                val capabilities = cap.capabilities //Map of capabilities
                var isSettingsSet = false //Used to set the 1st source options

                //Get sources
                if (capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_SIMPLEX)) {
                    viewModel.adfSimplex =
                        capabilities[ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_SIMPLEX] as MutableMap<String, Any>
                    if (!hasADF) {
                        //If ADF was not already added to the spinner, adds it
                        hasADF = true
                        sourceAdapter.add(getString(R.string.ScanOption_source_adf))
                        sourceAdapter.notifyDataSetChanged()
                    }
                    //Add 1 face to the number of faces spinner since this is the simplex adf mode
                    facesAdapter.add(getString(R.string.ScanOption_faces_1face))
                    facesAdapter.notifyDataSetChanged()

                    if (!isSettingsSet) {
                        //If no settings are chosen, set these ones
                        viewModel.setSource(ScanSettingsHelper.ScanSource.ADF_SIMPLEX)
                        updateSettings()
                        isSettingsSet = true
                    }
                }
                //Same for ADF duplex mode
                if (capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_DUPLEX)) {
                    viewModel.adfDuplex =
                        capabilities[ScannerCapabilities.SCANNER_CAPABILITY_IS_ADF_DUPLEX] as MutableMap<String, Any>
                    if (!hasADF) {
                        sourceAdapter.add(getString(R.string.ScanOption_source_adf))
                        sourceAdapter.notifyDataSetChanged()
                    }
                    facesAdapter.add(getString(R.string.ScanOption_faces_2face))
                    facesAdapter.notifyDataSetChanged()

                    if (!isSettingsSet) {
                        viewModel.setSource(ScanSettingsHelper.ScanSource.ADF_DUPLEX)
                        updateSettings()
                        isSettingsSet = true
                    }
                }
                //Same for the scanner's platen
                if (capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_PLATEN)) {
                    viewModel.platen =
                        capabilities[ScannerCapabilities.SCANNER_CAPABILITY_IS_PLATEN] as MutableMap<String, Any>
                    sourceAdapter.add(getString(R.string.ScanOption_source_platen))
                    sourceAdapter.notifyDataSetChanged()

                    if (!isSettingsSet) {
                        viewModel.setSource(ScanSettingsHelper.ScanSource.PLATEN)
                        updateSettings()
                        isSettingsSet = true
                    }
                }
                //Same for the scanner's camera
                if (capabilities.containsKey(ScannerCapabilities.SCANNER_CAPABILITY_IS_CAMERA)) {
                    viewModel.camera =
                        capabilities[ScannerCapabilities.SCANNER_CAPABILITY_IS_CAMERA] as MutableMap<String, Any>
                    sourceAdapter.add(getString(R.string.ScanOption_source_camera))
                    sourceAdapter.notifyDataSetChanged()

                    if (!isSettingsSet) {
                        viewModel.setSource(ScanSettingsHelper.ScanSource.CAMERA)
                        updateSettings()
                        isSettingsSet = true
                    }
                }

                //Scanner returned 0 sources. The source is AUTO in order to
                //let the scanner choose it in case that this happened due to an unknown error
                if (!isSettingsSet) {
                    viewModel.setSource(ScanSettingsHelper.ScanSource.AUTO)
                    updateSettings()
                }

            }

            override fun onFetchCapabilitiesError(exception: ScannerException?) {
                //Log.d(TAG, "Error obteniendo las caracteristicas del escaner")
                Log.e(TAG, exception?.message!!)

                //If the capabilities couldn't be obtained, shows a dialog that goes back
                //to the ScanSearchFragment so a scanner can be chosen again

                if (isAdded) {
                    val alertDialog: AlertDialog = this.let {
                        val builder = AlertDialog.Builder(activity!!)
                        builder.apply {

                            //gets the exception reason to show it in the dialog
                            val message = getReasonFromException(exception)

                            setTitle(getString(R.string.fragScanCapabilitiesErrorLabel))
                            setMessage(message)
                            setNeutralButton(R.string.accept) { _, _ ->
                                activity!!.supportFragmentManager.popBackStack()
                            }
                            setCancelable(false)
                        }
                        // Create the AlertDialog
                        builder.create()
                    }
                    alertDialog.show()
                }
            }
        })
    }

    /**
     * Sets source available settings on the UI
     */
    private fun updateSettings(){
        resolutionAdapter.clear()
        colorAdapter.clear()
        formatAdapter.clear()

        //Resolution: For each available resolution, set it on the UI
        viewModel.resolutionList.forEach{
            resolutionAdapter.add(it.toString())
        }
        resolutionAdapter.notifyDataSetChanged()

        //Color: Sets every available color mode in the UI
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_RGB_48)) colorAdapter.add(getString(R.string.ScanOption_colorMode_color48))
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_RGB_24)) colorAdapter.add(getString(R.string.ScanOption_colorMode_color24))
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_GRAYSCALE_16)) colorAdapter.add(getString(R.string.ScanOption_colorMode_grey16))
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_GRAYSCALE_8)) colorAdapter.add(getString(R.string.ScanOption_colorMode_grey8))
        if(viewModel.colorModes.contains(ScanValues.COLOR_MODE_BLACK_AND_WHITE)) colorAdapter.add(getString(R.string.ScanOption_colorMode_BW))
        colorAdapter.notifyDataSetChanged()

        //Format: Sets every available result format in the UI
        if(viewModel.resultFormats.contains(ScanValues.DOCUMENT_FORMAT_PDF)) formatAdapter.add(getString(R.string.ScanOption_format_PDF))
        if(viewModel.resultFormats.contains(ScanValues.DOCUMENT_FORMAT_JPEG)) formatAdapter.add(getString(R.string.ScanOption_format_JPEG))
        if(viewModel.resultFormats.contains(ScanValues.DOCUMENT_FORMAT_RAW)) formatAdapter.add(getString(R.string.ScanOption_format_RAW))
        formatAdapter.notifyDataSetChanged()
    }


    /**
     * Methods that validates chosen settings with the scanner.
     *
     */
    private fun validateTicket(){
        viewModel.chosenScanner!!.validateTicket(viewModel.chosenTicket, object : ScanTicketValidator.ScanTicketValidationListener{
            override fun onScanTicketValidationComplete(p0: ScanTicket?) {
                //If the ticket is valid, start scanning
                startScanning()
            }

            override fun onScanTicketValidationError(e: ScannerException?) {

                //If chosen settings are not valid, show a dialog explaining it.

                val alertDialog: AlertDialog = this.let {
                    val builder = AlertDialog.Builder(activity!!)
                    builder.apply {

                        val message = getReasonFromException(e)

                        setTitle(getString(R.string.fragScanValidateTicketError))
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

        //Open a fragment that shows a progress bar while the scan is on course
        val fragmentManager = activity?.supportFragmentManager!!
        var fragmentTransaction = fragmentManager.beginTransaction()
        val scanningFragment = PerformingScanFragment()
        scanningFragment.show(fragmentTransaction, "scanningFragment")

        //Gets the temporal folder where the scan result is going to be saved
        val tempFolder = activity?.getExternalFilesDir(tempScanFolder)!!
        val scanResultUris = arrayListOf<Uri>()

        viewModel.chosenScanner!!.scan(
            tempFolder.absolutePath,
            viewModel.chosenTicket,
            object : ScanCapture.ScanningProgressListener {
                override fun onScanningPageDone(p0: ScanPage?) {
                    Log.d(tag, "Page scanned")
                    if (p0 != null) {
                        //For each page scanner, saves their URI
                        scanResultUris.add(p0.uri!!)
                    }
                }

                override fun onScanningComplete() {
                    /*If the scan finishes, the list of result URIs are
                      given to the preview activity, where the results are
                      displayed, and the user decides to save or to discard them*/

                    Log.d(tag, "Scanning completed")

                    val i = Intent(activity?.applicationContext, ScanPreviewActivity::class.java)
                    i.putParcelableArrayListExtra("tempUris", scanResultUris)
                    i.putExtra("chosenFormat", viewModel.chosenFormat)

                    scanningFragment.dismiss()

                    //Starts activity for result in order to control when the user finishes
                    //the scannig process
                    startActivityForResult(i, previewActivityRequestCode)
                }

                override fun onScanningError(theException: ScannerException?) {
                    try {

                        //If an error happens, the scanner is told to stop and the files are
                        // deleted from the temporal folder
                        viewModel.chosenScanner!!.cancelScanning()
                        deleteTempFiles(tempFolder)

                        //The error reason is got from the exception and is shown in the progress fragment
                        if (theException != null) {
                            if (theException.reason != ScannerException.REASON_CANCELED_BY_USER) {
                                //Si el usuario lo cancela se crea una excepcion de este tipo
                                scanningFragment.showException(getReasonFromException(theException))
                            }
                        } else {
                            scanningFragment.dismiss()
                        }
                    } catch (e: AdfException) {
                        Log.d(tag, "AdfException\n Status: ${e.message}")

                    } catch (e: ScannerException) {
                        Log.d(tag, "ScannerException\n Reason: ${e.message}")
                    } catch (e: Exception) {
                        Log.d(tag, "Excepcion no controlada: ${e.message}")
                    }
                }
            })
    }

    /**
     * Method that deletes every temporal file from the given folder
     */
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
     * Fun that saves chosen options taking them from the UI
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

        //Set the scan source
        if(source == getString(R.string.ScanOption_source_adf) && nFaces == getString(R.string.ScanOption_faces_1face)){
            viewModel.chosenSource = ScanSettingsHelper.ScanSource.ADF_SIMPLEX
            viewModel.chosenNFaces = ScanSettingsHelper.Faces.ONE_FACE
        }else if (source == getString(R.string.ScanOption_source_adf) && nFaces == getString(R.string.ScanOption_faces_2face)){
            viewModel.chosenSource = ScanSettingsHelper.ScanSource.ADF_DUPLEX
            viewModel.chosenNFaces = ScanSettingsHelper.Faces.TWO_FACES
        }else if(source == getString(R.string.ScanOption_source_platen)){
            viewModel.chosenSource = ScanSettingsHelper.ScanSource.PLATEN

        }else if(source == getString(R.string.ScanOption_source_camera)){
            viewModel.chosenSource = ScanSettingsHelper.ScanSource.CAMERA
        }else {
            viewModel.chosenSource = ScanSettingsHelper.ScanSource.AUTO
        }

        //set color
        when(color){
            getString(R.string.ScanOption_colorMode_BW) -> viewModel.chosenColorMode = ScanSettingsHelper.ColorMode.BW
            getString(R.string.ScanOption_colorMode_grey8) -> viewModel.chosenColorMode = ScanSettingsHelper.ColorMode.GREY_8
            getString(R.string.ScanOption_colorMode_grey16) -> viewModel.chosenColorMode = ScanSettingsHelper.ColorMode.GREY_16
            getString(R.string.ScanOption_colorMode_color24) -> viewModel.chosenColorMode = ScanSettingsHelper.ColorMode.COLOR_24
            getString(R.string.ScanOption_colorMode_color48) -> viewModel.chosenColorMode = ScanSettingsHelper.ColorMode.COLOR_48
        }

        //set format
        when(format){
            getString(R.string.ScanOption_format_PDF) -> viewModel.chosenFormat = ScanSettingsHelper.Format.PDF
            getString(R.string.ScanOption_format_JPEG) -> viewModel.chosenFormat = ScanSettingsHelper.Format.JPEG
            getString(R.string.ScanOption_format_RAW) -> viewModel.chosenFormat = ScanSettingsHelper.Format.RAW
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

    /**
     * Method that gets the readable reason from a exception
     */
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

    /**
     * Method that gets the ADF status in a readable way
     */
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

    /**
     * Method that shows a dialog telling the user that a scanner
     * is not chosen, and restarts the activity.
     */
    private fun showNoScannerDialog(){
        val alertDialog: AlertDialog = this.let {
            val builder = AlertDialog.Builder(requireActivity())
            builder.apply {
                setTitle(getString(R.string.ScannerLostTitle))
                setMessage(getString(R.string.ScannerLostMessage))
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

    /**
     * Method that is called when de ScanPreview activity finishes in order to tell
     * the ScanActivity to navigate back to the MainActivity
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        //If PreviewActivity has finished, the scanning process is finished
        if(requestCode == previewActivityRequestCode){
            if(resultCode == previewActivityResultOK){
                (activity as ScanActivity).backPressedStatus = 2 //Sets the correct backPressedStatus
                (activity as ScanActivity).onBackPressed()
            }
        }
    }
}