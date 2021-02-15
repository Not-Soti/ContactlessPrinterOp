package com.example.movil

import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * Adapter class made to print PDF documents
 */
class PdfDocumentAdapter(val resourcePath: String) : PrintDocumentAdapter() {

    //Provides info about the expected impression job
    override fun onLayout(
        oldAttributes: PrintAttributes?,
        newAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal?,
        callback: LayoutResultCallback,
        extras: Bundle?
    ) {

        // Respond to cancellation request
        if (cancellationSignal?.isCanceled == true) {
            callback.onLayoutCancelled()
            return
        }

        //Creates the document info needed by the printer
        val documentBuilder = PrintDocumentInfo.Builder("file name")

        //Indicates the paper used to print is normal paper (not photo paper)
        documentBuilder.setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
        documentBuilder.setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
        documentBuilder.build()

        callback.onLayoutFinished(documentBuilder.build(), newAttributes == oldAttributes)
    }

    override fun onWrite(
        pageRanges: Array<out PageRange>,
        destination: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal?,
        callback: WriteResultCallback
    ) {
        try {
            // copy file from the input stream to the output stream
            FileInputStream(File(resourcePath)).use { inStream ->
                FileOutputStream(destination.fileDescriptor).use { outStream ->
                    inStream.copyTo(outStream)
                }
            }

            // check for cancellation
            if (cancellationSignal?.isCanceled == true) {
                callback.onWriteCancelled()
                return
            } else {
                // Signal the print framework the document is complete
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }

        } catch (e: Exception) {
            callback.onWriteFailed(e.message)
        }
    }
}