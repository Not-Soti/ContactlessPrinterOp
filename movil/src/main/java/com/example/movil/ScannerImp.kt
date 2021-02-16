package com.example.movil

import android.util.Log
import com.hp.mobile.scan.sdk.*
import com.hp.mobile.scan.sdk.model.ScanTicket
import java.net.URL


class ScannerImp(
    val name : String
) : Scanner{

    override fun scan(p0: String?, p1: ScanTicket?, p2: ScanCapture.ScanningProgressListener?) {
        Log.d("ScannerImp", "escaneando")
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
        TODO("Not yet implemented")
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