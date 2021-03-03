package com.example.movil.printActivity

import android.net.Uri
import androidx.lifecycle.ViewModel

class PrintActViewModel(
    private var resourceUri: Uri? = null, //selected resource uri
    private var resourcePath : String? = null, //using the uri does not work when trying to print documents
    private var resourceType: ResourceTypeEnum = ResourceTypeEnum.NOT_DEFINED
) : ViewModel() {

    //Enum needed to check the extension of the selected file to print
    enum class ResourceTypeEnum {
        NOT_DEFINED,
        IMAGE,
        PDF,
        HTML,
        DOC
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