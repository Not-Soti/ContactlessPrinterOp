package com.example.movil.scanActivity

import androidx.lifecycle.ViewModel
import com.hp.mobile.scan.sdk.Scanner
import com.hp.mobile.scan.sdk.model.*

class ScanActivityViewModel : ViewModel() {

    lateinit var chosenTicket : ScanTicket
    lateinit var chosenScanner : Scanner

    var adfSimplex: MutableMap<String, Any>? = null
    var adfDuplex: MutableMap<String, Any>? = null
    var platen: MutableMap<String, Any>? = null
    var camera: MutableMap<String, Any>? = null
    private lateinit var resolutions: ResolutionCapability
    lateinit var resolutionList : List<Resolution>
    var resultFormats: Collection<Int> = mutableListOf()
    var colorModes: Collection<Int> = mutableListOf()

    lateinit var chosenSource: ScanOptions.ScanSource
    lateinit var chosenNFaces : ScanOptions.Faces
    lateinit var chosenColorMode : ScanOptions.ColorMode
    lateinit var chosenFormat : ScanOptions.Format
    lateinit var chosenRes : Resolution
    var isResSelected = false

    fun setSource(sourceType : ScanOptions.ScanSource){
        chosenSource = sourceType

        val theSource : MutableMap<String, Any>? = when(sourceType){
            ScanOptions.ScanSource.ADF_DUPLEX -> adfDuplex
            ScanOptions.ScanSource.ADF_SIMPLEX -> adfSimplex
            ScanOptions.ScanSource.PLATEN -> platen
            ScanOptions.ScanSource.CAMERA -> camera
            ScanOptions.ScanSource.AUTO -> null
        }

        if(theSource == null){
            //Source == AUTO
            val theRes : Resolution = chosenTicket.getSetting(ScanTicket.SCAN_SETTING_RESOLUTION) as Resolution
            resolutionList = listOf(theRes)

            val theFormat : Int = chosenTicket.getSetting(ScanTicket.SCAN_SETTING_FORMAT) as Int
            resultFormats = listOf(theFormat)

            val theColor : Int = chosenTicket.getSetting(ScanTicket.SCAN_SETTING_COLOR_MODE) as Int
            colorModes = listOf(theColor)
        }else{

            resolutions = theSource[ScannerCapabilities.SOURCE_CAPABILITY_RESOLUTIONS] as ResolutionCapability
            resolutionList = resolutions.discreteResolutions
            resultFormats = theSource[ScannerCapabilities.SOURCE_CAPABILITY_FORMATS] as Collection<Int>
            colorModes = theSource[ScannerCapabilities.SOURCE_CAPABILITY_COLOR_MODES] as Collection<Int>
        }
    }

    /**
     * Function that gets scanning options from the chosen settings
     */
    fun setTicketOptions(){
        //Source
        when(chosenSource){
            ScanOptions.ScanSource.ADF_SIMPLEX -> { chosenTicket.inputSource = ScanValues.INPUT_SOURCE_ADF }
            ScanOptions.ScanSource.ADF_DUPLEX -> { chosenTicket.inputSource = ScanValues.INPUT_SOURCE_ADF }
            ScanOptions.ScanSource.PLATEN -> { chosenTicket.inputSource = ScanValues.INPUT_SOURCE_PLATEN }
            ScanOptions.ScanSource.CAMERA -> { chosenTicket.inputSource = ScanValues.INPUT_SOURCE_CAMERA }
            ScanOptions.ScanSource.AUTO -> { chosenTicket.inputSource = ScanValues.INPUT_SOURCE_AUTO }
        }

        //Sheet faces
        if ((chosenSource == ScanOptions.ScanSource.ADF_DUPLEX) || (chosenSource == ScanOptions.ScanSource.ADF_SIMPLEX)) {
            when(chosenNFaces){
                ScanOptions.Faces.ONE_FACE -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_DUPLEX, false)
                ScanOptions.Faces.TWO_FACES -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_DUPLEX, true)
            }
        }
        //Color mode
        when(chosenColorMode){
            ScanOptions.ColorMode.BW -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_BLACK_AND_WHITE)
            ScanOptions.ColorMode.GREY_8 -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_GRAYSCALE_8)
            ScanOptions.ColorMode.GREY_16 -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_GRAYSCALE_16)
            ScanOptions.ColorMode.COLOR_24 -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_RGB_24)
            ScanOptions.ColorMode.COLOR_48 -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_COLOR_MODE, ScanValues.COLOR_MODE_RGB_48)
        }
        //File format
        when(chosenFormat){
            ScanOptions.Format.JPEG -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_FORMAT, ScanValues.DOCUMENT_FORMAT_JPEG)
            ScanOptions.Format.PDF -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_FORMAT, ScanValues.DOCUMENT_FORMAT_PDF)
            ScanOptions.Format.RAW -> chosenTicket.setSetting(ScanTicket.SCAN_SETTING_FORMAT, ScanValues.DOCUMENT_FORMAT_RAW)
        }
        //Resolution
        if(isResSelected){ //A resolution was selected
            chosenTicket.setSetting(ScanTicket.SCAN_SETTING_RESOLUTION, chosenRes)
        }
    }





}