package com.example.movil.scanActivity

class ScanSettingsHelper {

    enum class ScanSource{
        ADF_DUPLEX,
        ADF_SIMPLEX,
        PLATEN,
        CAMERA,
        AUTO
    }

    enum class Faces{
        ONE_FACE,
        TWO_FACES
    }

    enum class Format{
        PDF,
        JPEG,
        RAW
    }

    enum class ColorMode{
        BW,
        GREY_8,
        GREY_16,
        COLOR_24,
        COLOR_48
    }
}