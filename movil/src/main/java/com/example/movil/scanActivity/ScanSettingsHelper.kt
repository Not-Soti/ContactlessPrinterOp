package com.example.movil.scanActivity

/**
 * Class that helps control the available scan settings
 */
class ScanSettingsHelper {

    //Enum of supported scan sources
    enum class ScanSource{
        ADF_DUPLEX,
        ADF_SIMPLEX,
        PLATEN,
        CAMERA,
        AUTO
    }

    //Enum that indicates if the scanner needs to scan a sheet by 1 faces or both of them
    enum class Faces{
        ONE_FACE,
        TWO_FACES
    }

    //Supported result file formats
    enum class Format{
        PDF,
        JPEG,
        RAW
    }

    //Supported color modes
    enum class ColorMode{
        BW,
        GREY_8,
        GREY_16,
        COLOR_24,
        COLOR_48
    }
}