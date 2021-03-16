package com.example.movil.scanActivity

class ScanOptions {

    enum class ScanSource{
        ADF,
        PLATEN,
        CAMERA
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