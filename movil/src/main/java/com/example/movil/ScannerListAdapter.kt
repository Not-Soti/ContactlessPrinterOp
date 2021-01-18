package com.example.movil

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter

import com.hp.mobile.scan.sdk.Scanner


class ScannerListAdapter() : BaseAdapter() {

    private var scannerList = mutableListOf<Scanner>()
    private lateinit var layoutInflater : LayoutInflater


    constructor(context : Context) : this() {
        layoutInflater = LayoutInflater.from(context)
    }

    public fun add(scanner : Scanner){ scannerList.add(scanner)}

    public fun remove(scanner : Scanner){ scannerList.remove(scanner)}

    override fun getCount(): Int { return scannerList.size }

    override fun getItem(position: Int): Any { return scannerList.get(position) }

    override fun getItemId(position: Int): Long { return position.toLong() }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val selectedScanner = scannerList[position]
        var selectedView = convertView

        if(selectedView == null){
            selectedView = layoutInflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        }

        return  selectedView!!
    }
}