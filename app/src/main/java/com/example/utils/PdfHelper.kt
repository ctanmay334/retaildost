package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.CancellationSignal
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.content.FileProvider
import com.example.data.model.CustomerEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.KhataCustomerEntity
import com.example.data.model.KhataTransactionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfHelper {

    // --- Legacy methods for backward compatibility ---

    fun generateAndSharePdf(context: Context, customer: CustomerEntity, transactions: List<TransactionEntity>) {
        val htmlContent = buildHtml(customer, transactions)
        
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                createPdfFromWebViewLegacy(context, view, customer)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    }

    private fun buildHtml(customer: CustomerEntity, transactions: List<TransactionEntity>): String {
        val sb = StringBuilder()
        sb.append("<html><head><style>")
        sb.append("body { font-family: sans-serif; padding: 20px; }")
        sb.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
        sb.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }")
        sb.append("th { background-color: #f2f2f2; }")
        sb.append(".debit { color: #D32F2F; font-weight: bold; }")
        sb.append(".credit { color: #388E3C; font-weight: bold; }")
        sb.append("</style></head><body>")
        
        sb.append("<h2>KiranaOS Ledger Statement</h2>")
        sb.append("<p><b>Customer:</b> ${customer.name}</p>")
        sb.append("<p><b>Phone:</b> ${customer.phone}</p>")
        sb.append("<p><b>Total Balance:</b> ₹${Math.abs(customer.balance)} ${if (customer.balance >= 0) "(They Owe)" else "(Advance)"}</p>")
        
        sb.append("<table>")
        sb.append("<tr><th>Date</th><th>Details</th><th>Amount</th><th>Balance</th></tr>")
        
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        transactions.forEach { tx ->
            val isDebit = tx.type == "debit"
            val amtClass = if (isDebit) "debit" else "credit"
            val sign = if (isDebit) "-" else "+"
            sb.append("<tr>")
            sb.append("<td>${sdf.format(Date(tx.date))}</td>")
            sb.append("<td>${tx.rawInput}</td>")
            sb.append("<td class='$amtClass'>$sign ₹${tx.amount}</td>")
            sb.append("<td>₹${tx.balanceAfter}</td>")
            sb.append("</tr>")
        }
        sb.append("</table></body></html>")
        return sb.toString()
    }

    private fun createPdfFromWebViewLegacy(context: Context, webView: WebView, customer: CustomerEntity) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size approx
        val page = document.startPage(pageInfo)
        
        // Measure and draw
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(pageInfo.pageWidth, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(pageInfo.pageHeight, View.MeasureSpec.EXACTLY)
        )
        webView.layout(0, 0, pageInfo.pageWidth, pageInfo.pageHeight)
        webView.draw(page.canvas)
        
        document.finishPage(page)
        
        try {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val file = File(dir, "Statement_${customer.name.replace(" ", "_")}.pdf")
            val outputStream = FileOutputStream(file)
            document.writeTo(outputStream)
            document.close()
            outputStream.close()
            
            sharePdfGeneral(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- New Khata Ledger PDF & Sharing Flow ---

    fun generateLedgerPdf(
        context: Context,
        customer: KhataCustomerEntity,
        transactions: List<KhataTransactionEntity>,
        onComplete: (File?) -> Unit
    ) {
        val htmlContent = buildKhataHtml(customer, transactions)
        
        val webView = WebView(context)
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                createPdfFromWebView(context, view, customer, onComplete)
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/html", "utf-8", null)
    }

    private fun buildKhataHtml(
        customer: KhataCustomerEntity,
        transactions: List<KhataTransactionEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html><html><head><meta charset='utf-8'><style>")
        sb.append("body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; color: #1e293b; padding: 30px; margin: 0; }")
        sb.append(".header { border-bottom: 2px solid #e2e8f0; padding-bottom: 20px; margin-bottom: 25px; }")
        sb.append(".header-title { font-size: 24px; font-weight: bold; color: #0b1a7d; margin: 0; }")
        sb.append(".header-meta { font-size: 12px; color: #64748b; margin-top: 5px; }")
        
        sb.append(".info-section { display: flex; justify-content: space-between; margin-bottom: 30px; }")
        sb.append(".info-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 15px; width: 48%; box-sizing: border-box; }")
        sb.append(".info-card h3 { font-size: 14px; color: #64748b; margin: 0 0 10px 0; text-transform: uppercase; letter-spacing: 0.5px; }")
        sb.append(".info-card p { font-size: 15px; margin: 4px 0; font-weight: 500; }")
        
        sb.append(".summary-badge { display: inline-block; padding: 6px 12px; border-radius: 20px; font-size: 13px; font-weight: bold; margin-top: 10px; }")
        sb.append(".summary-owe { background-color: #fee2e2; color: #b91c1c; }")
        sb.append(".summary-advance { background-color: #dcfce7; color: #15803d; }")
        sb.append(".summary-settled { background-color: #f1f5f9; color: #475569; }")

        sb.append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
        sb.append("th { background-color: #0b1a7d; color: white; font-weight: 600; text-align: left; padding: 12px 10px; font-size: 13px; }")
        sb.append("td { padding: 12px 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px; }")
        sb.append("tr:nth-child(even) { background-color: #f8fafc; }")
        sb.append(".debit { color: #b91c1c; font-weight: bold; }")
        sb.append(".credit { color: #15803d; font-weight: bold; }")
        sb.append(".notes { font-size: 12px; color: #64748b; font-style: italic; }")
        sb.append(".footer { text-align: center; margin-top: 40px; font-size: 11px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 15px; }")
        sb.append("</style></head><body>")

        // Header
        sb.append("<div class='header'>")
        sb.append("<div class='header-title'>RetailDost Khata Ledger Statement</div>")
        val currentDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        sb.append("<div class='header-meta'>Generated on: ${currentDateFormat.format(Date())}</div>")
        sb.append("</div>")

        // Info Cards
        sb.append("<div style='overflow: hidden; margin-bottom: 25px;'>")
        // Card 1: Merchant Details
        sb.append("<div class='info-card' style='float: left;'>")
        sb.append("<h3>Store Details</h3>")
        sb.append("<p><b>Store ID:</b> ${customer.storeId}</p>")
        sb.append("<p><b>Status:</b> Active Ledger Account</p>")
        sb.append("</div>")
        // Card 2: Customer Details
        sb.append("<div class='info-card' style='float: right;'>")
        sb.append("<h3>Customer Profile</h3>")
        sb.append("<p><b>Name:</b> ${customer.name}</p>")
        sb.append("<p><b>Phone:</b> ${customer.phone ?: "N/A"}</p>")
        
        val runningBal = customer.runningBalance
        val balanceText = String.format("%,.2f", Math.abs(runningBal))
        if (runningBal > 0) {
            sb.append("<div class='summary-badge summary-owe'>They Owe: ₹$balanceText</div>")
        } else if (runningBal < 0) {
            sb.append("<div class='summary-badge summary-advance'>Advance Deposit: ₹$balanceText</div>")
        } else {
            sb.append("<div class='summary-badge summary-settled'>Settled (₹0.00)</div>")
        }
        sb.append("</div>")
        sb.append("</div>")

        // Table
        sb.append("<table>")
        sb.append("<tr><th>Date</th><th>Type</th><th>Notes / Details</th><th>Amount</th></tr>")

        val txDateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        transactions.forEach { tx ->
            val isDebit = tx.txType == "debit"
            val typeText = if (isDebit) "CREDIT GIVEN (Maine Diya)" else "PAYMENT RECEIVED (Mujhe Mile)"
            val amtClass = if (isDebit) "debit" else "credit"
            val sign = if (isDebit) "-" else "+"
            val notesText = tx.notes?.ifEmpty { null } ?: tx.rawInput?.ifEmpty { null } ?: "Manual Entry"

            sb.append("<tr>")
            sb.append("<td>${txDateFormat.format(Date(tx.createdAt))}</td>")
            sb.append("<td class='$amtClass'>$typeText</td>")
            sb.append("<td class='notes'>$notesText</td>")
            sb.append("<td class='$amtClass'>$sign ₹${String.format("%,.2f", tx.amount)}</td>")
            sb.append("</tr>")
        }
        sb.append("</table>")

        // Footer
        sb.append("<div class='footer'>")
        sb.append("Thank you for your business! Powered by RetailDost - Your Smart Kirana Companion")
        sb.append("</div>")

        sb.append("</body></html>")
        return sb.toString()
    }

    private fun createPdfFromWebView(
        context: Context,
        webView: WebView,
        customer: KhataCustomerEntity,
        onComplete: (File?) -> Unit
    ) {
        val displayName = "Statement_${customer.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val localFile = File(context.cacheDir, displayName)

        try {
            val printAdapter = webView.createPrintDocumentAdapter("Statement")
            val attributes = PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setResolution(PrintAttributes.Resolution("pdf", "pdf", 600, 600))
                .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                .build()

            localFile.parentFile?.mkdirs()
            localFile.createNewFile()
            val pfd = ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_WRITE or ParcelFileDescriptor.MODE_CREATE)
            
            val layoutCallback = android.print.PrintResultCallbackBridge.createLayoutCallback(
                object : android.print.PrintResultCallbackBridge.LayoutResultCallbackDelegate {
                    override fun onLayoutFinished(info: PrintDocumentInfo, changed: Boolean) {
                        val writeCallback = android.print.PrintResultCallbackBridge.createWriteCallback(
                            object : android.print.PrintResultCallbackBridge.WriteResultCallbackDelegate {
                                override fun onWriteFinished(pages: Array<out PageRange>?) {
                                    try {
                                        pfd.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    // Download copy to the device's public Downloads directory
                                    try {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                            val resolver = context.contentResolver
                                            val contentValues = android.content.ContentValues().apply {
                                                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                                                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                                                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                                            }
                                            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                                            if (uri != null) {
                                                resolver.openOutputStream(uri)?.use { output ->
                                                    localFile.inputStream().use { input ->
                                                        input.copyTo(output)
                                                    }
                                                }
                                            }
                                        } else {
                                            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                            if (!downloadsDir.exists()) {
                                                downloadsDir.mkdirs()
                                            }
                                            val destFile = File(downloadsDir, displayName)
                                            localFile.copyTo(destFile, overwrite = true)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }

                                    onComplete(localFile)
                                }

                                override fun onWriteFailed(error: CharSequence?) {
                                    try {
                                        pfd.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    onComplete(null)
                                }

                                override fun onWriteCancelled() {
                                    try {
                                        pfd.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    onComplete(null)
                                }
                            }
                        )

                        printAdapter.onWrite(
                            arrayOf(PageRange.ALL_PAGES),
                            pfd,
                            CancellationSignal(),
                            writeCallback
                        )
                    }

                    override fun onLayoutFailed(error: CharSequence?) {
                        try {
                            pfd.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onComplete(null)
                    }

                    override fun onLayoutCancelled() {
                        try {
                            pfd.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        onComplete(null)
                    }
                }
            )

            printAdapter.onLayout(null, attributes, CancellationSignal(), layoutCallback, null)

        } catch (e: Exception) {
            e.printStackTrace()
            onComplete(null)
        }
    }

    fun sharePdfToWhatsApp(context: Context, file: File, phoneNumber: String?) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage("com.whatsapp")
            if (!phoneNumber.isNullOrBlank()) {
                putExtra("jid", "${phoneNumber.replace("+", "").replace(" ", "")}@s.whatsapp.net")
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general system share if WhatsApp is not installed
            sharePdfGeneral(context, file)
        }
    }

    fun sharePdfGeneral(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Statement"))
    }
}
