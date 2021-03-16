package com.example.movil.scanActivity

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.util.Log
import com.example.movil.ScannerSearchAct
import com.hp.mobile.scan.sdk.*
import com.hp.mobile.scan.sdk.model.ScanTicket
import java.io.File
import java.io.FileOutputStream
import java.net.URL


class ScannerImp(
    val name: String
) : Scanner{

    lateinit var act : Activity

    override fun scan(p0: String?, p1: ScanTicket?, p2: ScanCapture.ScanningProgressListener?) {
        Log.d("ScannerImp", "escaneando")

        val file  = File(p0!!)
        val fOut = FileOutputStream(file)

        var document = PdfDocument()
        var pageInfo = PdfDocument.PageInfo.Builder(100, 100, 1).create()
        var page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        canvas.drawText("Texto de prueba", 10.toFloat(), 10.toFloat(), paint)

        document.finishPage(page)
        document.writeTo(fOut)
        document.close()

        Log.d("ScannerImp", "Archivo escrito")

        lateinit var actAux: Activity

        actAux = act as ScannerSearchAct
        //actAux.scanningCompleted()

    }

    override fun cancelScanning() {
        Log.d("ScannerImp", "cancelando escaneo")
    }

    override fun isScanning(): Boolean {
        TODO("Not yet implemented")
    }

    override fun monitorDeviceStatus(p0: Int, p1: DeviceStatusMonitor.ScannerStatusListener?) {
        TODO("Not yet implemented")
    }

    override fun stopMonitoringDeviceStatus() {
        TODO("Not yet implemented")
    }

    override fun isDeviceStatusMonitoring(): Boolean {
        TODO("Not yet implemented")
    }

    override fun fetchCapabilities(p0: ScannerCapabilitiesFetcher.ScannerCapabilitiesListener?) {
    }

    override fun isFetchingCapabilities(): Boolean {
        TODO("Not yet implemented")
    }

    override fun cancelFetchingCapabilities() {
        TODO("Not yet implemented")
    }

    override fun validateTicket(
        p0: ScanTicket?,
        p1: ScanTicketValidator.ScanTicketValidationListener?
    ) {
        TODO("Not yet implemented")
    }

    override fun cancelValidation() {
        TODO("Not yet implemented")
    }

    override fun isValidating(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getModelName(): String {
        TODO("Not yet implemented")
    }

    override fun getHumanReadableName(): String {
        return name
    }

    override fun getIdentifier(): String {
        TODO("Not yet implemented")
    }

    override fun getIconUrl(): URL {
        TODO("Not yet implemented")
    }
}