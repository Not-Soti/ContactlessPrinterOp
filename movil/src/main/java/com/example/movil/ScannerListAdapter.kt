package com.example.movil

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.hp.mobile.scan.sdk.Scanner


class ScannerListAdapter() : BaseAdapter() {

    private var scannerList = mutableListOf<Scanner>()
    private lateinit var layoutInflater : LayoutInflater


    constructor(context: Context) : this() {
        layoutInflater = LayoutInflater.from(context)
    }

    public fun add(scanner: Scanner){
        scannerList.add(scanner)
        notifyDataSetChanged()
    }

    public fun remove(scanner: Scanner){
        scannerList.remove(scanner)
        notifyDataSetChanged()
    }

    public fun clear(){
        scannerList.clear()
        notifyDataSetChanged()
    }

    override fun getCount(): Int { return scannerList.size }

    override fun getItem(position: Int): Any { return scannerList.get(position) }

    override fun getItemId(position: Int): Long { return position.toLong() }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val selectedScanner = scannerList[position]
        var selectedView = convertView

        if(selectedView == null){
            selectedView = layoutInflater.inflate(R.layout.scanner_list_item, parent, false)
        }

        var scannerName = selectedView?.findViewById<View>(R.id.listItem_scanner_name) as TextView
        scannerName.text = selectedScanner.humanReadableName

        var scannerIco = selectedView?.findViewById<View>(R.id.listItem_scanner_icon) as ImageView
        scannerIco.setImageResource(R.drawable.scanner_mock_ico)

        return selectedView
    }
}