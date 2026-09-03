package com.themathguild.mytuitionmanager.receipts

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.themathguild.mytuitionmanager.R
import com.themathguild.mytuitionmanager.Payment
import com.themathguild.mytuitionmanager.Student
import com.themathguild.mytuitionmanager.TuitionProfile
import com.themathguild.mytuitionmanager.currentDate
import com.themathguild.mytuitionmanager.monthKey
import java.io.File
import java.time.format.DateTimeFormatter
import java.util.Locale

fun legacyReceiptPdf(
    context: Context,
    student: Student,
    paymentsForReceipt: List<Payment>,
    receiptNo: String,
    profile: TuitionProfile = TuitionProfile()
): Uri {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val pageWidth = 595f
    val left = 42f
    val right = 553f
    val navy = Color.rgb(28, 45, 78)
    val blue = Color.rgb(55, 92, 145)
    val light = Color.rgb(242, 245, 249)
    val muted = Color.rgb(90, 100, 115)
    val green = Color.rgb(35, 125, 75)

    fun text(value: String, x: Float, y: Float, size: Float, bold: Boolean = false, color: Int = Color.DKGRAY) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = size
        paint.isFakeBoldText = bold
        canvas.drawText(value, x, y, paint)
    }

    fun center(value: String, y: Float, size: Float, bold: Boolean = false, color: Int = Color.DKGRAY) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = size
        paint.isFakeBoldText = bold
        canvas.drawText(value, (pageWidth - paint.measureText(value)) / 2f, y, paint)
    }

    fun rounded(top: Float, bottom: Float, color: Int = Color.WHITE, stroke: Int? = null, width: Float = 1f) {
        paint.style = if (stroke == null) Paint.Style.FILL else Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = width
        canvas.drawRoundRect(left, top, right, bottom, 10f, 10f, paint)
        paint.style = Paint.Style.FILL
    }

    fun rule(y: Float, color: Int = Color.LTGRAY, width: Float = 1f) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = width
        canvas.drawLine(left, y, right, y, paint)
        paint.style = Paint.Style.FILL
    }

    fun wrapped(value: String, x: Float, y: Float, maxWidth: Float, size: Float, bold: Boolean = false, color: Int = Color.DKGRAY): Float {
        paint.textSize = size
        paint.isFakeBoldText = bold
        paint.color = color
        val words = value.split(" ")
        var line = ""
        var yy = y
        for (word in words) {
            val candidate = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(candidate) > maxWidth && line.isNotBlank()) {
                text(line, x, yy, size, bold, color)
                yy += size + 4f
                line = word
            } else line = candidate
        }
        if (line.isNotBlank()) {
            text(line, x, yy, size, bold, color)
            yy += size + 4f
        }
        return yy
    }

    val sortedPayments = paymentsForReceipt.sortedBy { monthKey(it.month) }
    val totalPaid = sortedPayments.sumOf { it.amount }
    val totalFee = student.monthlyFee * sortedPayments.size
    val status = if (totalPaid >= totalFee) "PAID" else "PARTIAL"
    val date = sortedPayments.firstOrNull()?.date ?: currentDate()

    paint.color = navy
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(left, 28f, right, 145f, 14f, 14f, paint)

    try {
        val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)
        if (bitmap != null) {
            val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 64, 64, true)
            canvas.drawBitmap(scaled, left + 18f, 48f, paint)
        }
    } catch (_: Exception) { }

    text(profile.tuitionName.uppercase(), left + 96f, 67f, 21f, true, Color.WHITE)
    text(profile.tagline, left + 96f, 88f, 10f, false, Color.WHITE)
    wrapped(profile.address, left + 96f, 107f, 315f, 8f, false, Color.WHITE)
    text("FEE RECEIPT", 438f, 69f, 10f, true, Color.WHITE)
    text("OFFICIAL", 462f, 88f, 8f, true, Color.WHITE)

    rounded(158f, 213f, light)
    text("RECEIPT NO.", left + 16f, 178f, 8f, true, muted)
    text(receiptNo, left + 16f, 198f, 13f, true, navy)
    text("PAYMENT DATE", 390f, 178f, 8f, true, muted)
    text(date, 390f, 198f, 13f, true, navy)

    text("STUDENT DETAILS", left, 239f, 11f, true, navy)
    rounded(248f, 345f, Color.WHITE, Color.LTGRAY)
    text("Student Name", left + 16f, 270f, 8f, true, muted)
    text(student.name, left + 16f, 290f, 14f, true, Color.DKGRAY)
    text("Class", left + 16f, 317f, 8f, true, muted)
    text(student.className.ifBlank { "Not provided" }, left + 16f, 334f, 11f, true)
    text("Batch", 215f, 317f, 8f, true, muted)
    text(student.batch.ifBlank { "Not provided" }, 215f, 334f, 11f, true)
    text("Joining Month", 350f, 317f, 8f, true, muted)
    text(student.joiningMonth.ifBlank { "Not provided" }, 350f, 334f, 11f, true)

    text("PAYMENT DETAILS", left, 374f, 11f, true, navy)
    rounded(384f, 545f, Color.WHITE, Color.LTGRAY)
    paint.color = light
    canvas.drawRect(left + 1f, 385f, right - 1f, 414f, paint)
    text("FEE MONTH(S)", left + 16f, 403f, 8f, true, muted)
    text("AMOUNT", 465f, 403f, 8f, true, muted)

    var y = 434f
    val monthsText = sortedPayments.joinToString(", ") { it.month }
    val monthsForReceipt = if (sortedPayments.size <= 6) {
        monthsText
    } else {
        val shortFormatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
        val first = sortedPayments.take(3).joinToString(", ") {
            monthKey(it.month)?.format(shortFormatter) ?: it.month
        }
        val last = sortedPayments.takeLast(3).joinToString(", ") {
            monthKey(it.month)?.format(shortFormatter) ?: it.month
        }
        "$first … $last (${sortedPayments.size} months total)"
    }
    y = wrapped(monthsForReceipt.ifBlank { "—" }, left + 16f, y, 395f, 10f, true)
    text("₹$totalPaid", 465f, 434f, 12f, true, navy)
    rule(maxOf(y + 2f, 465f), Color.LTGRAY, 0.8f)
    text("Months Covered", left + 16f, maxOf(y + 24f, 490f), 9f, false, muted)
    text(sortedPayments.size.toString(), 465f, maxOf(y + 24f, 490f), 10f, true)
    text("Monthly Fee", left + 16f, maxOf(y + 47f, 517f), 9f, false, muted)
    text("₹${student.monthlyFee}", 440f, maxOf(y + 47f, 517f), 10f, true)

    rounded(559f, 640f, light)
    text("TOTAL PAID", left + 16f, 590f, 9f, true, muted)
    text("₹$totalPaid", left + 16f, 620f, 23f, true, navy)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = if (status == "PAID") green else blue
    canvas.drawRoundRect(428f, 578f, 537f, 623f, 10f, 10f, paint)
    paint.style = Paint.Style.FILL
    center(if (status == "PAID") "PAID" else "PARTIAL", 607f, 13f, true, if (status == "PAID") green else blue)

    rule(667f, Color.LTGRAY, 1f)
    text("Thank you for your payment.", left, 694f, 11f, true, navy)
    text("Please keep this receipt for your records.", left, 712f, 9f, false, muted)

    text("Authorized by", left, 758f, 8f, true, muted)
    text(profile.teacherName, left, 778f, 12f, true, Color.DKGRAY)
    text("${profile.qualification} • ${profile.tuitionName}", left, 795f, 8f, false, muted)
    rule(806f, Color.DKGRAY, 0.8f)
    text("Signature", left, 820f, 7f, false, muted)

    text("Contact", 390f, 758f, 8f, true, muted)
    text(profile.phone, 390f, 778f, 11f, true, Color.DKGRAY)
    wrapped(profile.address, 390f, 796f, 155f, 7f, false, muted)
    center("Computer-generated receipt • The Math Guide", 836f, 7f, false, muted)

    doc.finishPage(page)
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "$receiptNo.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

fun createReceiptPdf(
    context: Context,
    student: Student,
    paymentsForReceipt: List<Payment>,
    receiptNo: String,
    profile: TuitionProfile = TuitionProfile()
): Uri {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val left = 38f
    val right = 557f
    val navy = Color.rgb(24, 43, 74)
    val blue = Color.rgb(44, 88, 148)
    val paleBlue = Color.rgb(237, 243, 251)
    val paleGreen = Color.rgb(232, 246, 237)
    val muted = Color.rgb(92, 103, 118)
    val sorted = paymentsForReceipt.sortedBy { monthKey(it.month) }
    val total = sorted.sumOf { it.amount }
    val isPaid = total >= student.monthlyFee * sorted.size

    fun text(value: String, x: Float, y: Float, size: Float, bold: Boolean = false, color: Int = Color.DKGRAY) {
        paint.style = Paint.Style.FILL; paint.color = color; paint.textSize = size; paint.isFakeBoldText = bold
        canvas.drawText(value, x, y, paint)
    }
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int = Color.LTGRAY, width: Float = 1f) {
        paint.style = Paint.Style.STROKE; paint.strokeWidth = width; paint.color = color
        canvas.drawLine(x1, y1, x2, y2, paint); paint.style = Paint.Style.FILL
    }
    fun fill(l: Float, top: Float, r: Float, bottom: Float, color: Int) {
        paint.style = Paint.Style.FILL; paint.color = color; canvas.drawRect(l, top, r, bottom, paint)
    }
    fun labelValue(label: String, value: String, x: Float, y: Float) {
        text(label.uppercase(), x, y, 7.5f, true, muted)
        text(value, x, y + 17f, 10.5f, true, navy)
    }

    fill(left, 30f, right, 113f, navy)
    try {
        BitmapFactory.decodeResource(context.resources, R.drawable.app_logo)?.let { bitmap ->
            canvas.drawBitmap(android.graphics.Bitmap.createScaledBitmap(bitmap, 46, 46, true), left + 14f, 47f, paint)
        }
    } catch (_: Exception) { }
    text(profile.tuitionName.uppercase(), left + 74f, 58f, 18f, true, Color.WHITE)
    text(profile.tagline.take(52), left + 74f, 77f, 9f, false, Color.WHITE)
    text(profile.address.take(72), left + 74f, 94f, 7.5f, false, Color.WHITE)
    text("PAYMENT RECEIPT", right - 128f, 58f, 10f, true, Color.WHITE)
    text("OFFICIAL COPY", right - 105f, 76f, 7.5f, true, Color.WHITE)

    fill(left, 131f, right, 181f, paleBlue)
    labelValue("Receipt no.", receiptNo, left + 14f, 146f)
    labelValue("Payment date", sorted.firstOrNull()?.date ?: currentDate(), 302f, 146f)

    text("STUDENT INFORMATION", left, 208f, 10f, true, navy)
    fill(left, 218f, right, 276f, Color.WHITE)
    line(left, 218f, right, 218f); line(left, 276f, right, 276f); line(left, 247f, right, 247f)
    line(298f, 218f, 298f, 276f)
    labelValue("Student name", student.name.take(42), left + 12f, 232f)
    labelValue("Class / batch", "${student.className.ifBlank { "—" }}  •  ${student.batch.ifBlank { "—" }}", 310f, 232f)
    labelValue("Joining month", student.joiningMonth.ifBlank { "Not provided" }, left + 12f, 261f)
    labelValue("Monthly fee", "₹${student.monthlyFee}", 310f, 261f)

    text("PAYMENT DETAILS", left, 304f, 10f, true, navy)
    fill(left, 314f, right, 340f, navy)
    text("FEE MONTH", left + 12f, 331f, 8f, true, Color.WHITE)
    text("DATE", 335f, 331f, 8f, true, Color.WHITE)
    text("AMOUNT", 474f, 331f, 8f, true, Color.WHITE)
    val visibleRows = sorted.take(5)
    visibleRows.forEachIndexed { index, payment ->
        val top = 340f + index * 27f
        if (index % 2 == 0) fill(left, top, right, top + 27f, Color.rgb(249, 250, 252))
        line(left, top + 27f, right, top + 27f)
        text(payment.month.take(32), left + 12f, top + 18f, 9.5f, index == 0)
        text(payment.date, 335f, top + 18f, 9f)
        text("₹${payment.amount}", 474f, top + 18f, 10f, true, navy)
    }
    var detailsBottom = 340f + maxOf(1, visibleRows.size) * 27f
    if (sorted.size > visibleRows.size) {
        fill(left, detailsBottom, right, detailsBottom + 25f, paleBlue)
        text("+ ${sorted.size - visibleRows.size} more month(s) recorded in payment history", left + 12f, detailsBottom + 17f, 8.5f, false, muted)
        detailsBottom += 25f
    }
    line(left, 314f, left, detailsBottom); line(right, 314f, right, detailsBottom)

    val totalTop = detailsBottom + 22f
    fill(left, totalTop, right, totalTop + 64f, if (isPaid) paleGreen else paleBlue)
    text("TOTAL PAID", left + 14f, totalTop + 20f, 8f, true, muted)
    text("₹$total", left + 14f, totalTop + 48f, 24f, true, navy)
    text("MONTHS COVERED", 300f, totalTop + 20f, 8f, true, muted)
    text(sorted.size.toString(), 300f, totalTop + 43f, 14f, true, navy)
    val statusColor = if (isPaid) Color.rgb(32, 120, 70) else blue
    text(if (isPaid) "PAID" else "PARTIAL", 455f, totalTop + 39f, 13f, true, statusColor)

    val footerTop = totalTop + 100f
    line(left, footerTop, right, footerTop, Color.LTGRAY)
    text("Authorized by", left, footerTop + 24f, 8f, true, muted)
    text(profile.teacherName, left, footerTop + 43f, 11f, true, navy)
    text("Contact: ${profile.phone}", 350f, footerTop + 43f, 10f, true, navy)
    text("Computer-generated receipt • Please retain for your records", left, footerTop + 66f, 8f, false, muted)

    doc.finishPage(page)
    val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
    val file = File(dir, "$receiptNo.pdf")
    file.outputStream().use { doc.writeTo(it) }
    doc.close()
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun shareReceipt(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share Receipt"))
}

fun shareReceiptWhatsApp(context: Context, uri: Uri) {
    try {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "WhatsApp is not available.", Toast.LENGTH_LONG).show()
    }
}

fun paymentReceiptText(profile: TuitionProfile, student: Student, payments: List<Payment>, receiptNo: String): String {
    val sortedPayments = payments.sortedBy { monthKey(it.month) }
    val months = sortedPayments.joinToString(", ") { it.month }
    val total = sortedPayments.sumOf { it.amount }
    val date = sortedPayments.firstOrNull()?.date ?: currentDate()

    return buildString {
        appendLine(profile.tuitionName)
        appendLine("Payment Receipt")
        appendLine("Student: ${student.name}")
        appendLine("Months: $months")
        appendLine("Amount Paid: ₹$total")
        appendLine("Receipt No: $receiptNo")
        appendLine("Date: $date")
        append("Thank you for your payment.")
    }
}

fun shareReceiptText(context: Context, student: Student, payments: List<Payment>, receiptNo: String, profile: TuitionProfile, mode: String) {
    val sortedPayments = payments.sortedBy { monthKey(it.month) }
    val months = sortedPayments.joinToString(", ") { it.month }
    val total = sortedPayments.sumOf { it.amount }
    val date = sortedPayments.firstOrNull()?.date ?: currentDate()
    val body = buildString {
        appendLine(profile.tuitionName)
        appendLine("Payment Receipt")
        appendLine("Student: ${student.name}")
        appendLine("Months: $months")
        appendLine("Amount Paid: ₹$total")
        appendLine("Receipt No: $receiptNo")
        appendLine("Date: $date")
        append("Thank you for your payment.")
    }

    try {
        val intent = if (mode == "SMS" && student.phone.isNotBlank()) {
            Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:${student.phone}")
                putExtra("sms_body", body)
            }
        } else if (mode == "WhatsApp" && student.phone.isNotBlank()) {
            val number = student.phone.filter(Char::isDigit)
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://wa.me/$number?text=${Uri.encode(body)}")
            )
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, body)
            }
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to open $mode.", Toast.LENGTH_LONG).show()
    }
}
