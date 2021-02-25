package com.example.movil.printActivity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import com.example.movil.R

class DownloadingFileFragment : DialogFragment() {

    private lateinit var progressBar : ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)

        val theView = inflater.inflate(R.layout.fragment_downloading_file, container, false)

        progressBar = theView.findViewById(R.id.frag_download_file_progressBar)

        return theView
    }

    fun updateProgressBar(count : Int){
        progressBar.progress = count
    }

}