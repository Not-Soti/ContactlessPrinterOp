package com.example.movil.printActivity

import android.net.Uri
import androidx.lifecycle.ViewModel

class PrintActViewModel(

    //selected resource uri
    private var resourceUri: Uri? = null,

    //using the uri does not work when trying to print documents
    //so the path is needed to
    private var resourcePath : String? = null,

    //variable used to save the chosen file type from none, image, pdf or html
    private var resourceType: ResourceTypeEnum = ResourceTypeEnum.NOT_DEFINED
) : ViewModel() {

    //Enum needed to check the extension of the selected file to print
    enum class ResourceTypeEnum {
        NOT_DEFINED,
        IMAGE,
        PDF,
        HTML
    }


    //Getters and setters
    fun getUri() : Uri?{
        return resourceUri
    }
    fun getPath() : String? {
        return resourcePath
    }
    fun getType() : ResourceTypeEnum {
        return resourceType
    }
    fun setUri(uri : Uri?){
        resourceUri = uri
    }
    fun setPath(p : String?){
        resourcePath = p
    }
    fun setType(type : ResourceTypeEnum){
        resourceType = type
    }
}