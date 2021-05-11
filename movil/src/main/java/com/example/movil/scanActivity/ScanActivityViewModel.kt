package com.example.movil.scanActivity

import androidx.lifecycle.ViewModel
import com.hp.mobile.scan.sdk.Scanner
import com.hp.mobile.scan.sdk.model.*

class ScanActivityViewModel : ViewModel() {


    var chosenTicket : ScanTicket? = null //Chosen scanner from the detectec ones in the network
    var chosenScanner : Scanner? = null   /*ScanTicket chosen from the standard ones.
                                            A ScanTicket saves the scan settings that are going
                                            to be used to scan*/

    var adfSimplex: MutableMap<String, Any>? = null //ADF from the Scanner in simplex mode if available
    var adfDuplex: MutableMap<String, Any>? = null  //ADF from the Scanner in duplex mode if available
    var platen: MutableMap<String, Any>? = null //Platen from the Scanner if available
    var camera: MutableMap<String, Any>? = null //Camera from the Scanner if available

    private lateinit var resolutions: ResolutionCapability //ResolutionCapability object got from the scanner capabilities
    lateinit var resolutionList : List<Resolution> //List of Resolution from the given ResolutionCapability

    var resultFormats: Collection<Int> = mutableListOf() //List of supported result formats by the scanner
    var colorModes: Collection<Int> = mutableListOf() //List of supported color modes by the scanner

    //User chosen settings:
    lateinit var chosenSource: ScanSettingsHelper.ScanSource
    lateinit var chosenNFaces : ScanSettingsHelper.Faces
    lateinit var chosenColorMode : ScanSettingsHelper.ColorMode
    lateinit var chosenFormat : ScanSettingsHelper.Format
    lateinit var chosenRes : Resolution
    var isResSelected = false //Controls if a resolution was selected by the user

    /**
     * Method that sets the scan source selected by the user
     * @param sourceType: Source selected by the user
     */
    fun setSource(sourceType : ScanSettingsHelper.ScanSource){
        chosenSource = sourceType

        //Gets the source from the one selected by the user
        val theSource : MutableMap<String, Any>? = when(sourceType){
            ScanSettingsHelper.ScanSource.ADF_DUPLEX -> adfDuplex
            ScanSettingsHelper.ScanSource.ADF_SIMPLEX -> adfSimplex
            ScanSettingsHelper.ScanSource.PLATEN -> platen
            ScanSettingsHelper.ScanSource.CAMERA -> camera
            ScanSettingsHelper.ScanSource.AUTO -> null
        }

        //If the user didn't pick any source, let the scanner choose one
        //and get the resolucion, result formats and color modes from this source
        if(theSource == null){
            //Source == AUTO
            val theRes : Resolution = chosenTicket!!.getSetting(ScanTicket.SCAN_SETTING_RESOLUTION) as Resolution
            resolutionList = listOf(theRes)

            val theFormat : Int = chosenTicket!!.getSetting(ScanTicket.SCAN_SETTING_FORMAT) as Int
            resultFormats = listOf(theFormat)

            val theColor : Int = chosenTicket!!.getSetting(ScanTicket.SCAN_SETTING_COLOR_MODE) as Int
            colorModes = listOf(theColor)
        }else{
            //If the user picked a source, get the capabilities from it
            resolutions = theSource[ScannerCapabilities.SOURCE_CAPABILITY_RESOLUTIONS] as ResolutionCapability
            resolutionList = resolutions.discreteResolutions
            resultFormats = theSource[ScannerCapabilities.SOURCE_CAPABILITY_FORMATS] as Collection<Int>
            colorModes = theSource[ScannerCapabilities.SOURCE_CAPABILITY_COLOR_MODES] as Collection<Int>
        }
    }

    /**
     * Function that gets scanning options from the user's chosen settings and
     * set them in the ScanTicket
     */
    fun setTicketOptions(){
        //Source
        when(chosenSource){
            ScanSettingsHelper.ScanSource.ADF_SIMPLEX -> { chosenTicket!!.inputSource = ScanValues.INPUT_SOURCE_ADF }
            ScanSettingsHelper.ScanSource.ADF_DUPLEX -> { chosenTicket!!.inputSource = ScanValues.INPUT_SOURCE_ADF }
            ScanSettingsHelper.ScanSource.PLATEN -> { chosenTicket!!.inputSource = ScanValues.INPUT_SOURCE_PLATEN }
            ScanSettingsHelper.ScanSource.CAMERA -> { chosenTicket!!.inputSource = ScanValues.INPUT_SOURCE_CAMERA }
            ScanSettingsHelper.ScanSource.AUTO -> { chosenTicket!!.inputSource = ScanValues.INPUT_SOURCE_AUTO }
        }

        //Sheet faces
        if ((chosenSource == ScanSettingsHelper.ScanSource.ADF_DUPLEX) || (chosenSource == ScanSettingsHelper.ScanSource.ADF_SIMPLEX)) {
            when(chosenNFaces){
                ScanSettingsHelper.Faces.ONE_FACE -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_DUPLEX, false)
                ScanSettingsHelper.Faces.TWO_FACES -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_DUPLEX, true)
            }
        }
        //Color mode
        when(chosenColorMode){
            ScanSettingsHelper.ColorMode.BW -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_BLACK_AND_WHITE)
            ScanSettingsHelper.ColorMode.GREY_8 -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_GRAYSCALE_8)
            ScanSettingsHelper.ColorMode.GREY_16 -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_GRAYSCALE_16)
            ScanSettingsHelper.ColorMode.COLOR_24 -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_RGB_24)
            ScanSettingsHelper.ColorMode.COLOR_48 -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_RGB_48)
        }
        //File format
        when(chosenFormat){
            ScanSettingsHelper.Format.JPEG -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_FORMAT, ScanValues.DOCUMENT_FORMAT_JPEG)
            ScanSettingsHelper.Format.PDF -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_FORMAT, ScanValues.DOCUMENT_FORMAT_PDF)
            ScanSettingsHelper.Format.RAW -> chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_FORMAT, ScanValues.DOCUMENT_FORMAT_RAW)
        }
        //Resolution
        if(isResSelected){ //A resolution was selected
            chosenTicket!!.setSetting(ScanTicket.SCAN_SETTING_RESOLUTION, chosenRes)
        }
    }





}