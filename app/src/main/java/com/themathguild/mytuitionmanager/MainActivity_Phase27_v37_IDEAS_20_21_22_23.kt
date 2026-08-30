package com.themathguild.mytuitionmanager

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

data class Student(
    val id: Long,
    val name: String,
    val className: String,
    val batch: String,
    val monthlyFee: Int,
    val phone: String,
    val joiningMonth: String = "",
    val school: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val address: String = "",
    val subjects: String = "",
    val notes: String = "",
    val status: String = "Active"
)

data class Payment(
    val studentId: Long,
    val month: String,
    val amount: Int,
    val date: String,
    val receiptNo: String = ""
)

data class AttendanceRecord(
    val studentId: Long,
    val date: String,
    val status: String
)

data class AcademicRecord(
    val id: Long,
    val studentId: Long,
    val date: String,
    val subject: String,
    val title: String,
    val marks: Double,
    val maxMarks: Double,
    val remarks: String
)

data class BatchRoutine(
    val id: Long, val batch: String, val day: String, val start: String, val end: String
)

data class BatchNote(
    val id: Long, val batch: String, val date: String, val type: String, val text: String
)

data class TuitionProfile(
    val tuitionName: String = "The Math Guide",
    val tagline: String = "Tuition & Academic Support",
    val teacherName: String = "Ashraful Hoque",
    val qualification: String = "B.SC Maths",
    val phone: String = "9732956571",
    val address: String = "Jatarpur, Murshidabad, West Bengal, 742147"
)

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("tuition_data", Context.MODE_PRIVATE)

    fun loadStudents(): List<Student> {
        val a = JSONArray(prefs.getString("students", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Student(
                o.getLong("id"),
                o.getString("name"),
                o.getString("className"),
                o.getString("batch"),
                o.getInt("monthlyFee"),
                o.optString("phone"),
                o.optString("joiningMonth", ""),
                o.optString("school", ""),
                o.optString("fatherName", ""),
                o.optString("motherName", ""),
                o.optString("address", ""),
                o.optString("subjects", ""),
                o.optString("notes", ""),
                o.optString("status", "Active")
            )
        }
    }

    fun saveStudents(list: List<Student>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("className", it.className)
                put("batch", it.batch)
                put("monthlyFee", it.monthlyFee)
                put("phone", it.phone)
                put("joiningMonth", it.joiningMonth)
                put("school", it.school)
                put("fatherName", it.fatherName)
                put("motherName", it.motherName)
                put("address", it.address)
                put("subjects", it.subjects)
                put("notes", it.notes)
                put("status", it.status)
            })
        }
        prefs.edit().putString("students", a.toString()).apply()
    }

    fun loadPayments(): List<Payment> {
        val a = JSONArray(prefs.getString("payments", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Payment(
                o.getLong("studentId"),
                o.getString("month"),
                o.getInt("amount"),
                o.getString("date"),
                o.optString("receiptNo", "")
            )
        }
    }

    fun loadAttendance(): List<AttendanceRecord> {
        val a = JSONArray(prefs.getString("attendance", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            AttendanceRecord(
                o.getLong("studentId"),
                o.getString("date"),
                o.getString("status")
            )
        }
    }

    fun saveAttendance(list: List<AttendanceRecord>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("studentId", it.studentId)
                put("date", it.date)
                put("status", it.status)
            })
        }
        prefs.edit().putString("attendance", a.toString()).apply()
    }

    fun loadAcademicRecords(): List<AcademicRecord> {
        val a = JSONArray(prefs.getString("academicRecords", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            AcademicRecord(
                o.getLong("id"),
                o.getLong("studentId"),
                o.getString("date"),
                o.optString("subject"),
                o.optString("title"),
                o.optDouble("marks", 0.0),
                o.optDouble("maxMarks", 0.0),
                o.optString("remarks")
            )
        }
    }

    fun saveAcademicRecords(list: List<AcademicRecord>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("id", it.id)
                put("studentId", it.studentId)
                put("date", it.date)
                put("subject", it.subject)
                put("title", it.title)
                put("marks", it.marks)
                put("maxMarks", it.maxMarks)
                put("remarks", it.remarks)
            })
        }
        prefs.edit().putString("academicRecords", a.toString()).apply()
    }

    fun nextAcademicId(): Long =
        loadAcademicRecords().maxOfOrNull { it.id }?.plus(1L) ?: 1L

    fun loadTuitionProfile(): TuitionProfile {
        return TuitionProfile(
            tuitionName = prefs.getString("tuitionName", "The Math Guide") ?: "The Math Guide",
            tagline = prefs.getString("tuitionTagline", "Tuition & Academic Support") ?: "Tuition & Academic Support",
            teacherName = prefs.getString("teacherName", "Ashraful Hoque") ?: "Ashraful Hoque",
            qualification = prefs.getString("qualification", "B.SC Maths") ?: "B.SC Maths",
            phone = prefs.getString("tuitionPhone", "9732956571") ?: "9732956571",
            address = prefs.getString("tuitionAddress", "Jatarpur, Murshidabad, West Bengal, 742147")
                ?: "Jatarpur, Murshidabad, West Bengal, 742147"
        )
    }

    fun saveTuitionProfile(profile: TuitionProfile) {
        prefs.edit()
            .putString("tuitionName", profile.tuitionName)
            .putString("tuitionTagline", profile.tagline)
            .putString("teacherName", profile.teacherName)
            .putString("qualification", profile.qualification)
            .putString("tuitionPhone", profile.phone)
            .putString("tuitionAddress", profile.address)
            .apply()
    }

    fun hasSecurityPin(): Boolean {
        val saved = prefs.getString("securityPin", "") ?: ""
        return saved.matches(Regex("\\d{4}"))
    }

    fun securityPin(): String = prefs.getString("securityPin", "") ?: ""

    fun saveSecurityPin(pin: String) {
        if (pin.matches(Regex("\\d{4}"))) {
            prefs.edit().putString("securityPin", pin).apply()
        }
    }

    fun lastBackupDate(): String =
        prefs.getString("lastBackupDate", "") ?: ""

    fun loadBatchRoutines(): List<BatchRoutine> {
        val a = JSONArray(prefs.getString("batchRoutines", "[]"))
        return List(a.length()) { i -> val o=a.getJSONObject(i); BatchRoutine(o.getLong("id"),o.optString("batch"),o.optString("day"),o.optString("start"),o.optString("end")) }
    }
    fun saveBatchRoutines(list: List<BatchRoutine>) {
        val a=JSONArray(); list.forEach { r -> a.put(JSONObject().apply { put("id",r.id);put("batch",r.batch);put("day",r.day);put("start",r.start);put("end",r.end) }) }; prefs.edit().putString("batchRoutines",a.toString()).apply()
    }
    fun loadBatchNotes(): List<BatchNote> {
        val a=JSONArray(prefs.getString("batchNotes", "[]")); return List(a.length()){i->val o=a.getJSONObject(i);BatchNote(o.getLong("id"),o.optString("batch"),o.optString("date"),o.optString("type"),o.optString("text"))}
    }
    fun saveBatchNotes(list: List<BatchNote>) {
        val a=JSONArray(); list.forEach{n->a.put(JSONObject().apply{put("id",n.id);put("batch",n.batch);put("date",n.date);put("type",n.type);put("text",n.text)})};prefs.edit().putString("batchNotes",a.toString()).apply()
    }
    fun themeMode(): String = prefs.getString("themeMode", "System") ?: "System"
    fun accentName(): String = prefs.getString("accentName", "Blue") ?: "Blue"
    fun saveTheme(mode:String, accent:String) { prefs.edit().putString("themeMode",mode).putString("accentName",accent).apply() }
    fun isProtected(action:String): Boolean = prefs.getBoolean("protect_$action", action in setOf("payment","addStudent","editStudent","deleteStudent","editPayment","deletePayment","restore","attendance","academic","profile","demo"))
    fun setProtected(action:String, value:Boolean) { prefs.edit().putBoolean("protect_$action",value).apply() }

    fun savePayments(list: List<Payment>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("studentId", it.studentId)
                put("month", it.month)
                put("amount", it.amount)
                put("date", it.date)
                put("receiptNo", it.receiptNo)
            })
        }
        prefs.edit().putString("payments", a.toString()).apply()
    }
}

private val monthFormatter =
    DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

fun currentMonth(): String =
    SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date())

fun currentDate(): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

fun backupAgeDays(lastBackup: String): Long? {
    if (lastBackup.isBlank()) return null
    return try {
        val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val then = format.parse(lastBackup)?.time ?: return null
        ((System.currentTimeMillis() - then).coerceAtLeast(0L)) / (24L * 60L * 60L * 1000L)
    } catch (_: Exception) {
        null
    }
}

fun monthKey(month: String): YearMonth? = try {
    YearMonth.parse(month.trim(), monthFormatter)
} catch (_: Exception) {
    null
}

fun money(value: Int) = "₹$value"

private fun recentMonths(count: Long = 18): List<String> {
    val now = YearMonth.now()
    return (0 until count.toInt()).map {
        now.minusMonths(it.toLong()).format(monthFormatter)
    }
}

fun eligibleFeeMonths(student: Student, existingPayments: List<Payment>): List<String> {
    val join = monthKey(student.joiningMonth) ?: return emptyList()
    val latestAllowed = YearMonth.now().plusMonths(1)
    if (join.isAfter(latestAllowed)) return emptyList()

    val paidMonths = existingPayments.mapNotNull { monthKey(it.month) }.toSet()
    return buildList {
        var m = join
        while (!m.isAfter(latestAllowed)) {
            if (m !in paidMonths) add(m.format(monthFormatter))
            m = m.plusMonths(1)
        }
    }
}

fun validateFeeCollection(
    student: Student,
    existingPayments: List<Payment>,
    selectedMonths: List<String>
): String? {
    if (student.monthlyFee <= 0) return "Monthly fee must be greater than ₹0."
    if (monthKey(student.joiningMonth) == null) return "Please enter a valid joining month."
    if (selectedMonths.isEmpty()) return "Select at least one month."

    val duplicate = selectedMonths.groupingBy { it.lowercase() }.eachCount().any { it.value > 1 }
    if (duplicate) return "The same month cannot be selected twice."

    val allowed = eligibleFeeMonths(student, existingPayments).toSet()
    val invalid = selectedMonths.filter { it !in allowed }
    if (invalid.isNotEmpty()) return "Payment not allowed for: ${invalid.joinToString(", ")}"

    return null
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

    // Branded header
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

    // Receipt metadata
    rounded(158f, 213f, light)
    text("RECEIPT NO.", left + 16f, 178f, 8f, true, muted)
    text(receiptNo, left + 16f, 198f, 13f, true, navy)
    text("PAYMENT DATE", 390f, 178f, 8f, true, muted)
    text(date, 390f, 198f, 13f, true, navy)

    // Student section
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

    // Payment section
    text("PAYMENT DETAILS", left, 374f, 11f, true, navy)
    rounded(384f, 545f, Color.WHITE, Color.LTGRAY)
    paint.color = light
    canvas.drawRect(left + 1f, 385f, right - 1f, 414f, paint)
    text("FEE MONTH(S)", left + 16f, 403f, 8f, true, muted)
    text("AMOUNT", 465f, 403f, 8f, true, muted)

    var y = 434f
    val monthsText = sortedPayments.joinToString(", ") { it.month }
    y = wrapped(monthsText.ifBlank { "—" }, left + 16f, y, 395f, 10f, true)
    text("₹$totalPaid", 465f, 434f, 12f, true, navy)
    rule(maxOf(y + 2f, 465f), Color.LTGRAY, 0.8f)
    text("Months Covered", left + 16f, maxOf(y + 24f, 490f), 9f, false, muted)
    text(sortedPayments.size.toString(), 465f, maxOf(y + 24f, 490f), 10f, true)
    text("Monthly Fee", left + 16f, maxOf(y + 47f, 517f), 9f, false, muted)
    text("₹${student.monthlyFee}", 440f, maxOf(y + 47f, 517f), 10f, true)

    // Total and status
    rounded(559f, 640f, light)
    text("TOTAL PAID", left + 16f, 590f, 9f, true, muted)
    text("₹$totalPaid", left + 16f, 620f, 23f, true, navy)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1.5f
    paint.color = if (status == "PAID") green else blue
    canvas.drawRoundRect(428f, 578f, 537f, 623f, 10f, 10f, paint)
    paint.style = Paint.Style.FILL
    center(if (status == "PAID") "PAID" else "PARTIAL", 607f, 13f, true, if (status == "PAID") green else blue)

    // Footer / signature
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
    val months = payments.sortedBy { monthKey(it.month) }.joinToString(", ") { it.month }
    val total = payments.sumOf { it.amount }
    return "${profile.tuitionName}\nPayment Receipt\nStudent: ${student.name}\nMonths: $months\nAmount Paid: ₹$total\nReceipt No: $receiptNo\nDate: ${payments.firstOrNull()?.date ?: currentDate()}\nThank you for your payment."
}

fun shareReceiptText(context: Context, student: Student, payments: List<Payment>, receiptNo: String, profile: TuitionProfile, mode: String) {
    val body = paymentReceiptText(profile, student, payments, receiptNo)
    try {
        val intent = if (mode == "SMS" && student.phone.isNotBlank()) {
            Intent(Intent.ACTION_SENDTO).apply { data = Uri.parse("smsto:${student.phone}"); putExtra("sms_body", body) }
        } else if (mode == "WhatsApp" && student.phone.isNotBlank()) {
            val number = student.phone.filter(Char::isDigit)
            Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$number?text=${Uri.encode(body)}"))
        } else {
            Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, body) }
        }
        context.startActivity(intent)
    } catch (_: Exception) { Toast.makeText(context, "Unable to open $mode.", Toast.LENGTH_LONG).show() }
}


private const val BACKUP_FORMAT_VERSION = 7

fun validateBackupData(obj: JSONObject): String? {
    if (obj.optString("app") != "The Math Guide") return "This is not a The Math Guide backup."
    val format = obj.optInt("backupFormat", 0)
    if (format !in 1..BACKUP_FORMAT_VERSION) return "Unsupported backup format."
    val students = obj.optJSONArray("students") ?: return "Students data is missing."
    val payments = obj.optJSONArray("payments") ?: return "Payment data is missing."
    val attendance = obj.optJSONArray("attendance") ?: JSONArray()
    val academic = obj.optJSONArray("academicRecords") ?: JSONArray()

    val studentIds = mutableSetOf<Long>()
    for (i in 0 until students.length()) {
        val s = students.optJSONObject(i) ?: return "Invalid student record."
        if (!s.has("id") || !s.has("name") || !s.has("className") ||
            !s.has("monthlyFee") || !s.has("joiningMonth")) return "Invalid student record."
        val id = s.optLong("id", Long.MIN_VALUE)
        if (id == Long.MIN_VALUE || !studentIds.add(id)) return "Duplicate or invalid student ID."
        if (s.optString("name").isBlank()) return "A student has no name."
        if (s.optInt("monthlyFee", -1) < 0) return "Invalid student fee."
    }

    for (i in 0 until payments.length()) {
        val p = payments.optJSONObject(i) ?: return "Invalid payment record."
        if (!p.has("studentId") || !p.has("month") || !p.has("amount") || !p.has("date"))
            return "Invalid payment record."
        if (p.optLong("studentId", Long.MIN_VALUE) !in studentIds) return "Payment references a missing student."
        if (p.optInt("amount", -1) < 0) return "Invalid payment amount."
    }

    for (i in 0 until attendance.length()) {
        val a = attendance.optJSONObject(i) ?: return "Invalid attendance record."
        if (!a.has("studentId") || !a.has("date") || !a.has("status")) return "Invalid attendance record."
        if (a.optLong("studentId", Long.MIN_VALUE) !in studentIds) return "Attendance references a missing student."
        if (a.optString("status") !in setOf("PRESENT", "ABSENT", "LATE")) return "Invalid attendance status."
    }

    for (i in 0 until academic.length()) {
        val a = academic.optJSONObject(i) ?: return "Invalid academic record."
        if (!a.has("id") || !a.has("studentId") || !a.has("date") ||
            !a.has("marks") || !a.has("maxMarks")) return "Invalid academic record."
        if (a.optLong("studentId", Long.MIN_VALUE) !in studentIds) return "Academic record references a missing student."
        val marks = a.optDouble("marks", -1.0)
        val max = a.optDouble("maxMarks", -1.0)
        if (marks < 0 || max <= 0 || marks > max) return "Invalid academic marks."
    }
    return null
}

fun exportBackup(context: Context): Uri {
    val prefs = context.getSharedPreferences("tuition_data", Context.MODE_PRIVATE)
    val backupDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
    val profile = JSONObject().apply {
        put("tuitionName", prefs.getString("tuitionName", "The Math Guide") ?: "The Math Guide")
        put("tagline", prefs.getString("tuitionTagline", "Tuition & Academic Support") ?: "Tuition & Academic Support")
        put("teacherName", prefs.getString("teacherName", "Ashraful Hoque") ?: "Ashraful Hoque")
        put("qualification", prefs.getString("qualification", "B.SC Maths") ?: "B.SC Maths")
        put("phone", prefs.getString("tuitionPhone", "9732956571") ?: "9732956571")
        put("address", prefs.getString("tuitionAddress", "Jatarpur, Murshidabad, West Bengal, 742147")
            ?: "Jatarpur, Murshidabad, West Bengal, 742147")
    }

    val students = JSONArray(prefs.getString("students", "[]") ?: "[]")
    val payments = JSONArray(prefs.getString("payments", "[]") ?: "[]")
    val attendance = JSONArray(prefs.getString("attendance", "[]") ?: "[]")
    val academicRecords = JSONArray(prefs.getString("academicRecords", "[]") ?: "[]")
    val batchRoutines = JSONArray(prefs.getString("batchRoutines", "[]") ?: "[]")
    val batchNotes = JSONArray(prefs.getString("batchNotes", "[]") ?: "[]")

    val json = JSONObject().apply {
        put("app", "The Math Guide")
        put("backupFormat", BACKUP_FORMAT_VERSION)
        put("backupDate", backupDate)
        put("studentCount", students.length())
        put("paymentCount", payments.length())
        put("attendanceCount", attendance.length())
        put("academicRecordCount", academicRecords.length())
        put("students", students)
        put("payments", payments)
        put("attendance", attendance)
        put("academicRecords", academicRecords)
        put("batchRoutines", batchRoutines)
        put("batchNotes", batchNotes)
        put("tuitionProfile", profile)
    }.toString(2)

    val dir = File(context.cacheDir, "backup").apply { mkdirs() }
    val file = File(dir, "TheMathGuide_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())}.json")
    file.writeText(json)

    prefs.edit().putString("lastBackupDate", backupDate).apply()

    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun importBackup(context: Context, uri: Uri): Boolean {
    return try {
        val json = context.contentResolver.openInputStream(uri)
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: return false

        val obj = JSONObject(json)
        val students = obj.optJSONArray("students") ?: return false
        val payments = obj.optJSONArray("payments") ?: return false
        val attendance = obj.optJSONArray("attendance") ?: JSONArray()
        val academicRecords = obj.optJSONArray("academicRecords") ?: JSONArray()
        val batchRoutines = obj.optJSONArray("batchRoutines") ?: JSONArray()
        val batchNotes = obj.optJSONArray("batchNotes") ?: JSONArray()

        if (validateBackupData(obj) != null) return false

        val prefs = context.getSharedPreferences("tuition_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString("students", students.toString())
            .putString("payments", payments.toString())
            .putString("attendance", attendance.toString())
            .putString("academicRecords", academicRecords.toString())
            .putString("batchRoutines", batchRoutines.toString())
            .putString("batchNotes", batchNotes.toString())

        obj.optJSONObject("tuitionProfile")?.let { profile ->
            editor.putString("tuitionName", profile.optString("tuitionName", "The Math Guide"))
                .putString("tuitionTagline", profile.optString("tagline", "Tuition & Academic Support"))
                .putString("teacherName", profile.optString("teacherName", "Ashraful Hoque"))
                .putString("qualification", profile.optString("qualification", "B.SC Maths"))
                .putString("tuitionPhone", profile.optString("phone", "9732956571"))
                .putString(
                    "tuitionAddress",
                    profile.optString(
                        "address",
                        "Jatarpur, Murshidabad, West Bengal, 742147"
                    )
                )
        }

        editor.apply()
        true
    } catch (_: Exception) {
        false
    }
}

class MainActivity : ComponentActivity() {
    private var restoreCallback: ((Boolean) -> Unit)? = null

    private val restoreLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val ok = uri?.let { importBackup(this, it) } ?: false
            restoreCallback?.invoke(ok)
            restoreCallback = null
        }

    fun pickBackup(callback: (Boolean) -> Unit) {
        restoreCallback = callback
        restoreLauncher.launch(arrayOf("application/json", "text/*"))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TuitionApp(LocalStore(this)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuitionApp(store: LocalStore) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var students by remember { mutableStateOf(store.loadStudents()) }
    var payments by remember { mutableStateOf(store.loadPayments()) }
    var attendance by remember { mutableStateOf(store.loadAttendance()) }
    var academicRecords by remember { mutableStateOf(store.loadAcademicRecords()) }
    var tuitionProfile by remember { mutableStateOf(store.loadTuitionProfile()) }
    var search by remember { mutableStateOf("") }
    var selectedBatch by remember { mutableStateOf("All") }
    var paymentFilter by remember { mutableStateOf("All") }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var editStudent by remember { mutableStateOf<Student?>(null) }
    var collectStudent by remember { mutableStateOf<Student?>(null) }
    var reportOpen by remember { mutableStateOf(false) }
    var receiptHistoryOpen by remember { mutableStateOf(false) }
    var editPaymentOpen by remember { mutableStateOf<Payment?>(null) }
    var deletePaymentConfirm by remember { mutableStateOf<Payment?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var tuitionProfileOpen by remember { mutableStateOf(false) }
    var dashboardExpanded by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf("Name A–Z") }
    var selectedClass by remember { mutableStateOf("All") }
    var filtersExpanded by remember { mutableStateOf(false) }
    var quickActionsExpanded by remember { mutableStateOf(true) }
    var demoToolsOpen by remember { mutableStateOf(false) }
    var restoreConfirmOpen by remember { mutableStateOf(false) }
    var lastBackupDate by remember {
        mutableStateOf(
            store.lastBackupDate()
        )
    }
    var manageStudentsOpen by remember { mutableStateOf(false) }
    var attendanceOpen by remember { mutableStateOf(false) }
    var academicOpen by remember { mutableStateOf(false) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBottomTab by remember { mutableStateOf(0) }
    var securityPinOpen by remember { mutableStateOf(false) }
    var setupPinOpen by remember { mutableStateOf(!store.hasSecurityPin()) }
    var securityPinTitle by remember { mutableStateOf("Security PIN") }
    var securityPinAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var changePinOpen by remember { mutableStateOf(false) }
    var routines by remember { mutableStateOf(store.loadBatchRoutines()) }
    var batchNotes by remember { mutableStateOf(store.loadBatchNotes()) }
    var liveBatchOpen by remember { mutableStateOf(false) }
    var todayWorkOpen by remember { mutableStateOf(false) }
    var statusFilter by remember { mutableStateOf("All") }
    var themeMode by remember { mutableStateOf(store.themeMode()) }
    var accentName by remember { mutableStateOf(store.accentName()) }
    var themeOpen by remember { mutableStateOf(false) }
    var securityControlsOpen by remember { mutableStateOf(false) }
    var backupReminderDismissed by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    var undoStudentSnapshot by remember { mutableStateOf<Triple<Student, List<Payment>, List<AttendanceRecord>>?>(null) }
    var undoStudentAcademicSnapshot by remember { mutableStateOf<List<AcademicRecord>>(emptyList()) }
    var undoPaymentSnapshot by remember { mutableStateOf<Payment?>(null) }

    fun requireSecurityPin(title: String, action: () -> Unit) {
        if (!store.hasSecurityPin()) {
            setupPinOpen = true
            return
        }
        securityPinTitle = title
        securityPinAction = action
        securityPinOpen = true
    }

    fun guarded(actionKey: String, title: String, action: () -> Unit) {
        if (store.isProtected(actionKey)) requireSecurityPin(title, action) else action()
    }

    val thisMonth = currentMonth()
    fun currentPaid(student: Student) = payments.filter { it.studentId == student.id && it.month.equals(thisMonth, true) }.sumOf { it.amount }
    fun outstanding(student: Student): Int {
        val start = monthKey(student.joiningMonth) ?: payments.filter { it.studentId == student.id }.mapNotNull { monthKey(it.month) }.minOrNull() ?: YearMonth.now()
        val ps = payments.filter { it.studentId == student.id }
        var m = start; var due = 0
        while (!m.isAfter(YearMonth.now())) {
            val paid = ps.filter { it.month.equals(m.format(monthFormatter), true) }.sumOf { it.amount }
            due += maxOf(0, student.monthlyFee - paid); m = m.plusMonths(1)
        }
        return due
    }

    val paidStudents = students.count { currentPaid(it) >= it.monthlyFee }
    val partialStudents = students.count { currentPaid(it) > 0 && currentPaid(it) < it.monthlyFee }
    val unpaidStudents = students.count { currentPaid(it) == 0 }
    val totalOutstanding = students.sumOf { outstanding(it) }
    val totalCollected = payments.filter { it.month.equals(thisMonth, true) }.sumOf { it.amount }
    val expectedThisMonth = students.sumOf { it.monthlyFee }
    val batches = listOf("All") + students.map { it.batch }.filter { it.isNotBlank() }.distinct().sorted()
    val classes = listOf("All") + students.map { it.className }.filter { it.isNotBlank() }.distinct().sorted()
    val visibleStudents = students.filter {
        it.name.contains(search, true) || it.className.contains(search, true) || it.batch.contains(search, true) || it.phone.contains(search, true)
    }.filter { selectedBatch == "All" || it.batch == selectedBatch }
     .filter { selectedClass == "All" || it.className == selectedClass }
     .filter { statusFilter == "All" || it.status.equals(statusFilter, true) }
     .filter {
         when (paymentFilter) {
             "Paid" -> currentPaid(it) >= it.monthlyFee
             "Partial" -> currentPaid(it) > 0 && currentPaid(it) < it.monthlyFee
             "Unpaid" -> currentPaid(it) == 0
             "Due" -> outstanding(it) > 0
             else -> true
         }
     }
     .let { list ->
         when (sortOption) {
             "Due high → low" -> list.sortedByDescending { outstanding(it) }
             "Joining newest" -> list.sortedByDescending { monthKey(it.joiningMonth) ?: YearMonth.of(1900, 1) }
             else -> list.sortedBy { it.name.lowercase() }
         }
     }

    val darkTheme = when (themeMode) {
        "Dark" -> true
        "Light" -> false
        else -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    val seed = when (accentName) {
        "Green" -> ComposeColor(0xFF2E7D32); "Purple" -> ComposeColor(0xFF6A1B9A); "Orange" -> ComposeColor(0xFFEF6C00); "Red" -> ComposeColor(0xFFC62828); "Teal" -> ComposeColor(0xFF00796B); else -> ComposeColor(0xFF1565C0)
    }
    val colorScheme = if (darkTheme) androidx.compose.material3.darkColorScheme(primary = seed) else androidx.compose.material3.lightColorScheme(primary = seed)
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.app_logo),
                                contentDescription = "The Math Guide logo",
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text("The Math Guide", fontWeight = FontWeight.Bold)
                                Text("My Tuition Manager", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedBottomTab == 0, onClick = { selectedBottomTab = 0 }, icon = { Text("⌂") }, label = { Text("Home") })
                    NavigationBarItem(selected = selectedBottomTab == 1, onClick = { selectedBottomTab = 1; manageStudentsOpen = true }, icon = { Text("☷") }, label = { Text("Students") })
                    NavigationBarItem(selected = selectedBottomTab == 2, onClick = { selectedBottomTab = 2; liveBatchOpen = true }, icon = { Text("🎓") }, label = { Text("Live Batch") })
                    NavigationBarItem(selected = selectedBottomTab == 3, onClick = { selectedBottomTab = 3; receiptHistoryOpen = true }, icon = { Text("₹") }, label = { Text("Payments") })
                    NavigationBarItem(selected = selectedBottomTab == 4, onClick = { selectedBottomTab = 4; settingsOpen = true }, icon = { Text("⚙") }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Good day!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Fee overview • $thisMonth", style = MaterialTheme.typography.bodySmall)
                    }
                }

                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.developer_photo),
                                    contentDescription = "Ashraful Hoque",
                                    modifier = Modifier.size(42.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Column(Modifier.weight(1f)) {
                                    Text("Ashraful Hoque", fontWeight = FontWeight.Bold)
                                    Text("B.SC Maths • The Math Guide", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Dashboard", fontWeight = FontWeight.Bold)
                                    Text(
                                        "${students.size} students • ${money(totalCollected)} collected • ${money(totalOutstanding)} due",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                TextButton(onClick = { dashboardExpanded = !dashboardExpanded }) {
                                    Text(if (dashboardExpanded) "Hide" else "Show")
                                }
                            }

                            if (dashboardExpanded) {
                                HorizontalDivider()
                                Spacer(Modifier.height(7.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DashboardCard("Students", students.size.toString(), Modifier.weight(1f), onClick = { paymentFilter = "All" })
                                    DashboardCard("Collected", money(totalCollected), Modifier.weight(1f), onClick = { paymentFilter = "Paid" })
                                    DashboardCard("Due", money(totalOutstanding), Modifier.weight(1f), onClick = { paymentFilter = "Due" })
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DashboardCard("Paid", "$paidStudents / ${students.size}", Modifier.weight(1f), onClick = { paymentFilter = "Paid" })
                                    DashboardCard("Partial", partialStudents.toString(), Modifier.weight(1f), onClick = { paymentFilter = "Partial" })
                                    DashboardCard("Unpaid", unpaidStudents.toString(), Modifier.weight(1f), onClick = { paymentFilter = "Unpaid" })
                                }

                                val percent = if (expectedThisMonth == 0) 0
                                else ((totalCollected.toFloat() / expectedThisMonth.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

                                Spacer(Modifier.height(7.dp))
                                Text(
                                    if (expectedThisMonth == 0) "No monthly fee target yet"
                                    else "Collection rate: $percent%",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LinearProgressIndicator(
                                    progress = {
                                        if (expectedThisMonth == 0) 0f
                                        else (totalCollected.toFloat() / expectedThisMonth.toFloat()).coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth().height(4.dp)
                                )
                            }
                        }
                    }
                }

                val backupAge = backupAgeDays(lastBackupDate)
                val backupNeedsAttention = !backupReminderDismissed && (backupAge == null || backupAge >= 7)

                if (backupNeedsAttention) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    if (backupAge == null) "⚠️ Protect your tuition data" else "⚠️ Backup reminder",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (backupAge == null) "No backup has been created yet."
                                    else "Your last backup was $backupAge day${if (backupAge == 1L) "" else "s"} ago.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { backupReminderDismissed = true }) { Text("Dismiss") }
                                    Button(onClick = {
                                        try {
                                            val uri = exportBackup(context)
                                            lastBackupDate = store.lastBackupDate()
                                            backupReminderDismissed = true
                                            Toast.makeText(context, "Backup created successfully.", Toast.LENGTH_SHORT).show()
                                            shareReceipt(context, uri)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, "Backup failed. No data was changed.", Toast.LENGTH_LONG).show()
                                        }
                                    }) { Text("Back Up Now") }
                                }
                            }
                        }
                    }
                }

                item {
                    val activeDueCount = students.count {
                        it.status.equals("Active", true) && studentDueAmount(it, payments) > 0
                    }
                    if (activeDueCount > 0) {
                        Card(
                            Modifier
                                .fillMaxWidth()
                                .clickable { selectedTab = 0 }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 12.dp, vertical = 10.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Fees Due", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "$activeDueCount active student${if (activeDueCount == 1) "" else "s"}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                Text("View Students →")
                            }
                        }
                    }
                }

                item {
                    val todayPayments = payments.filter {
                        it.date == currentDate() || it.date.startsWith(currentDate().substringBefore("/"))
                    }
                    val todayCollected = todayPayments.sumOf { it.amount }
                    val dueStudentsToday = students.count { student ->
                        student.status.equals("Active", true) && studentDueAmount(student, payments) > 0
                    }

                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Today", fontWeight = FontWeight.Bold)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("₹${todayCollected.toInt()}", fontWeight = FontWeight.SemiBold)
                                    Text("Collected", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${todayPayments.size}", fontWeight = FontWeight.SemiBold)
                                    Text("Payments", style = MaterialTheme.typography.labelSmall)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$dueStudentsToday", fontWeight = FontWeight.SemiBold)
                                    Text("Due", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }

                item {
                    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                    val todayRoutines = routines
                        .filter { it.day.equals(dayName, true) }
                        .sortedBy { it.startTime }

                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Today's Classes", fontWeight = FontWeight.Bold)
                                    Text(dayName, style = MaterialTheme.typography.labelSmall)
                                }
                                if (todayRoutines.isNotEmpty()) {
                                    TextButton(onClick = { selectedTab = 1 }) { Text("Live Batch") }
                                }
                            }

                            if (todayRoutines.isEmpty()) {
                                Text(
                                    "No classes scheduled today.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                todayRoutines.take(4).forEach { routine ->
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(routine.batchName, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                "${routine.startTime} – ${routine.endTime}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        TextButton(onClick = {
                                            selectedTab = 1
                                        }) {
                                            Text("Attendance")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                    val todayRoutines = routines.filter { it.day.equals(dayName, true) }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                                Column { Text("Today’s Work", fontWeight=FontWeight.Bold); Text("$dayName • ${todayRoutines.size} scheduled batch${if(todayRoutines.size==1) "" else "es"}", style=MaterialTheme.typography.labelSmall) }
                                TextButton(onClick={todayWorkOpen=true}){Text("View") }
                            }
                            todayRoutines.take(3).forEach { Text("🟢 ${it.start}–${it.end} • ${it.batch}", style=MaterialTheme.typography.bodySmall) }
                            if(todayRoutines.isEmpty()) Text("No routine set for today. Use Live Batch or Settings to add one.", style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        search,
                        { search = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("Search student") },
                        singleLine = true,
                        leadingIcon = { Text("🔎") },
                        trailingIcon = {
                            if (search.isNotBlank()) {
                                TextButton(onClick = { search = "" }) {
                                    Text("Clear")
                                }
                            }
                        }
                    )
                }

                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(horizontal = 10.dp, vertical = 5.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Filters & Sorting", fontWeight = FontWeight.Bold)
                                    val active = listOf(
                                        paymentFilter != "All",
                                        selectedBatch != "All",
                                        selectedClass != "All",
                                        sortOption != "Name A–Z"
                                    ).count { it }
                                    Text(
                                        if (active == 0) "No filters applied"
                                        else "$active filter${if (active == 1) "" else "s"} active",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                                TextButton(onClick = { filtersExpanded = !filtersExpanded }) {
                                    Text(if (filtersExpanded) "Collapse" else "Expand")
                                }
                            }

                            if (filtersExpanded) {
                                HorizontalDivider()
                                Spacer(Modifier.height(6.dp))

                                Text("Payment Status", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement=Arrangement.spacedBy(5.dp)) { listOf("All","Active","Paused","Left").forEach { st -> FilterChip(statusFilter==st,{statusFilter=st},label={Text(st)}) } }
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    listOf("All", "Paid", "Partial", "Unpaid", "Due").forEach { status ->
                                        FilterChip(
                                            paymentFilter == status,
                                            { paymentFilter = status },
                                            label = { Text(status) }
                                        )
                                    }
                                }

                                Spacer(Modifier.height(5.dp))
                                Text("Batch", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    batches.forEach { batch ->
                                        FilterChip(selectedBatch == batch, { selectedBatch = batch }, label = { Text(batch) })
                                    }
                                }

                                Spacer(Modifier.height(5.dp))
                                Text("Class", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    classes.forEach { cls ->
                                        FilterChip(selectedClass == cls, { selectedClass = cls }, label = { Text(cls) })
                                    }
                                }

                                Spacer(Modifier.height(5.dp))
                                Text("Sort", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                Row(
                                    Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    listOf("Name A–Z", "Due high → low", "Joining newest").forEach { option ->
                                        FilterChip(sortOption == option, { sortOption = option }, label = { Text(option) })
                                    }
                                }
                            }
                        }
                    }
                }
                item { Text("Students (${visibleStudents.size})", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(visibleStudents, key = { it.id }) { student ->
                    val paid = currentPaid(student)
                    val due = maxOf(0, student.monthlyFee - paid)
                    val status = when {
                        paid >= student.monthlyFee -> "PAID"
                        paid > 0 -> "PARTIAL"
                        else -> "UNPAID"
                    }

                    val monthsDue = if (student.monthlyFee > 0) ((outstanding(student).toFloat() / student.monthlyFee.toFloat()).toInt()).coerceAtLeast(0) else 0
                    val cardContainer = when {
                        student.status.equals("Paused", true) -> MaterialTheme.colorScheme.surfaceVariant
                        student.status.equals("Left", true) -> MaterialTheme.colorScheme.surfaceVariant
                        monthsDue >= 2 -> MaterialTheme.colorScheme.errorContainer
                        monthsDue == 1 -> androidx.compose.ui.graphics.Color(0xFFFFF3CD)
                        else -> androidx.compose.ui.graphics.Color(0xFFE8F5E9)
                    }
                    Card(colors=CardDefaults.cardColors(containerColor=cardContainer), onClick = { selectedStudent = student }, modifier = Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        student.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        "Class ${student.className}  •  Batch ${student.batch}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                AssistChip(
                                    onClick = { selectedStudent = student },
                                    label = {
                                        Text(
                                            if (status == "PAID") "PAID"
                                            else if (status == "PARTIAL") "PARTIAL"
                                            else "DUE ₹$due"
                                        )
                                    }
                                )
                            }

                            HorizontalDivider()

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Monthly fee", style = MaterialTheme.typography.labelSmall)
                                    Text("₹${student.monthlyFee}", fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Joined", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        student.joiningMonth.ifBlank { "—" },
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            val attendanceToday = attendance.firstOrNull {
                                it.studentId == student.id && it.date == LocalDate.now().toString()
                            }?.status

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Due ₹$due", fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (attendanceToday == null) "Today: Not marked"
                                    else "Today: ${attendanceToday.lowercase().replaceFirstChar { it.uppercase() }}"
                                )
                            }

                            if (student.phone.isNotBlank()) {
                                Text(
                                    "☎ ${student.phone}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            FilledTonalButton(
                                onClick = { guarded("payment", "Make Payment") { collectStudent = student } },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (due > 0) "Collect Fee • ₹$due" else "View / Collect Fee")
                            }

                            Text(
                                "Tap card for full profile",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                item {
                    if (students.isEmpty()) Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("👨‍🎓 No students yet", fontWeight = FontWeight.Bold)
                            Text(
                                "Add your first student to start managing fees, attendance and academic progress.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            OutlinedButton(onClick = { guarded("addStudent", "Add Student") { addOpen = true } }) {
                                Text("Add Student")
                            }
                        }
                    }
                }
            }
        }

        if (setupPinOpen) {
            CreateSecurityPinDialog(
                onCreated = { newPin ->
                    store.saveSecurityPin(newPin)
                    setupPinOpen = false
                    Toast.makeText(context, "Security PIN created successfully.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (securityPinOpen) {
            SecurityPinDialog(
                title = securityPinTitle,
                storedPin = store.securityPin(),
                onDismiss = { securityPinOpen = false; securityPinAction = null },
                onSuccess = {
                    val action = securityPinAction
                    securityPinOpen = false
                    securityPinAction = null
                    action?.invoke()
                }
            )
        }

        if (changePinOpen) {
            ChangeSecurityPinDialog(
                currentPin = store.securityPin(),
                onDismiss = { changePinOpen = false },
                onSaved = { newPin ->
                    store.saveSecurityPin(newPin)
                    changePinOpen = false
                    Toast.makeText(context, "Security PIN changed successfully.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (settingsOpen) SettingsDialog(
            onDismiss = { settingsOpen = false },
            onAdd = { settingsOpen = false; guarded("addStudent", "Add Student") { addOpen = true } },
            onManage = { manageStudentsOpen = true },
            onAttendance = { settingsOpen = false; guarded("attendance", "Attendance") { attendanceOpen = true } },
            onAcademic = { settingsOpen = false; guarded("academic", "Academic Records") { academicOpen = true } },
            onReports = { settingsOpen = false; reportOpen = true },
            onReceiptHistory = { settingsOpen = false; receiptHistoryOpen = true },
            onTuitionProfile = { settingsOpen = false; guarded("profile", "Tuition Profile") { tuitionProfileOpen = true } },
            onDemoTools = { settingsOpen = false; guarded("demo", "Demo / Test Data") { demoToolsOpen = true } },
            onSecurityPin = { settingsOpen = false; requireSecurityPin("Change Security PIN") { changePinOpen = true } },
            onTheme = { settingsOpen = false; themeOpen = true },
            onSecurityControls = { settingsOpen = false; securityControlsOpen = true },
            lastBackupDate = lastBackupDate,
            onBackup = {
                try {
                    val uri = exportBackup(context)
                    lastBackupDate = store.lastBackupDate()
                    Toast.makeText(
                        context,
                        "Backup created successfully • ${students.size} students • ${payments.size} payments",
                        Toast.LENGTH_LONG
                    ).show()
                    shareReceipt(context, uri)
                } catch (_: Exception) {
                    Toast.makeText(context, "Backup failed. No data was changed.", Toast.LENGTH_LONG).show()
                }
            },
            onRestore = { guarded("restore", "Restore Backup") { restoreConfirmOpen = true } }
        )

        if (manageStudentsOpen) ManageStudentsDialog(
            students = students,
            onDismiss = { manageStudentsOpen = false },
            onEdit = { s -> guarded("editStudent", "Edit Student") { manageStudentsOpen = false; settingsOpen = false; editStudent = s } },
            onDelete = { s -> guarded("deleteStudent", "Delete Student") {
                val deletedPayments = payments.filter { it.studentId == s.id }
                val deletedAttendance = attendance.filter { it.studentId == s.id }
                val deletedAcademic = academicRecords.filter { it.studentId == s.id }

                undoStudentSnapshot = Triple(s, deletedPayments, deletedAttendance)
                undoStudentAcademicSnapshot = deletedAcademic

                students = students.filter { it.id != s.id }
                payments = payments.filter { it.studentId != s.id }
                attendance = attendance.filter { it.studentId != s.id }
                academicRecords = academicRecords.filter { it.studentId != s.id }

                store.saveStudents(students)
                store.savePayments(payments)
                store.saveAttendance(attendance)
                store.saveAcademicRecords(academicRecords)

                snackbarScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "${s.name} deleted",
                        actionLabel = "UNDO",
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed &&
                        undoStudentSnapshot?.first?.id == s.id
                    ) {
                        val snapshot = undoStudentSnapshot
                        if (snapshot != null) {
                            students = (students + snapshot.first).distinctBy { it.id }
                            payments = payments + snapshot.second
                            attendance = attendance + snapshot.third
                            academicRecords = academicRecords + undoStudentAcademicSnapshot

                            store.saveStudents(students)
                            store.savePayments(payments)
                            store.saveAttendance(attendance)
                            store.saveAcademicRecords(academicRecords)

                            Toast.makeText(context, "Student restored.", Toast.LENGTH_SHORT).show()
                        }
                        undoStudentSnapshot = null
                        undoStudentAcademicSnapshot = emptyList()
                    }
                }
            } }
        )

        if (addOpen) StudentEditorDialog("Add Student", null, { addOpen = false }) { s -> students = students + s; store.saveStudents(students); addOpen = false }
        editStudent?.let { s -> StudentEditorDialog("Edit Student", s, { editStudent = null }) { u -> students = students.map { if (it.id == u.id) u else it }; store.saveStudents(students); editStudent = null } }
        selectedStudent?.let { s ->
            StudentDetailsDialog(
                student = s,
                payments = payments.filter { it.studentId == s.id },
                outstanding = outstanding(s),
                onDismiss = { selectedStudent = null },
                onCollect = { guarded("payment", "Make Payment") { collectStudent = s; selectedStudent = null } }
            )
        }
        collectStudent?.let { s ->
            CollectFeeDialog(
                student = s,
                existingPayments = payments.filter { it.studentId == s.id },
                onDismiss = { collectStudent = null }
            ) { selectedMonths ->
                val existingForStudent = payments.filter { it.studentId == s.id }
                val error = validateFeeCollection(s, existingForStudent, selectedMonths)

                if (error != null) {
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                    return@CollectFeeDialog
                }

                val date = currentDate()
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                val nextReceiptNumber = payments
                    .mapNotNull { Regex("TMG-\\d{4}-(\\d+)").find(it.receiptNo)?.groupValues?.get(1)?.toIntOrNull() }
                    .maxOrNull()?.plus(1) ?: 1
                val receiptNo = "TMG-$year-${nextReceiptNumber.toString().padStart(5, '0')}"

                // Store each month separately for accurate history, while sharing
                // one receipt number so a multi-month payment gets one receipt.
                val newPayments = selectedMonths.distinct().map { month ->
                    Payment(s.id, month, s.monthlyFee, date, receiptNo)
                }

                payments = payments + newPayments
                store.savePayments(payments)
                receiptUri = createReceiptPdf(context, s, newPayments, receiptNo, tuitionProfile)
                collectStudent = null
            }
        }
        receiptUri?.let { uri ->
            AlertDialog(onDismissRequest = { receiptUri = null }, title = { Text("Payment Saved ✓") }, text = {
                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Professional PDF receipt is ready. Choose a sending method.")
                    Text("WhatsApp / SMS sends a compact text receipt to the saved number. PDF remains available through Share PDF.", style=MaterialTheme.typography.bodySmall)
                }
            }, confirmButton = {
                Button(onClick={ val p=payments.filter{it.receiptNo.isNotBlank() && it.receiptNo==payments.lastOrNull()?.receiptNo}; val st=students.firstOrNull{p.firstOrNull()?.studentId==it.id}; if(st!=null&&p.isNotEmpty()) shareReceiptText(context,st,p,p.first().receiptNo,tuitionProfile,"WhatsApp"); receiptUri=null }){Text("WhatsApp") }
            }, dismissButton={
                Row { TextButton(onClick={ val p=payments.filter{it.receiptNo.isNotBlank() && it.receiptNo==payments.lastOrNull()?.receiptNo}; val st=students.firstOrNull{p.firstOrNull()?.studentId==it.id}; if(st!=null&&p.isNotEmpty()) shareReceiptText(context,st,p,p.first().receiptNo,tuitionProfile,"SMS"); receiptUri=null }){Text("SMS")}; TextButton(onClick={shareReceipt(context,uri);receiptUri=null}){Text("PDF / Other")}}
            })
        }
        if (attendanceOpen) {
            AttendanceDialog(
                students = students,
                records = attendance,
                onDismiss = { attendanceOpen = false },
                onSave = { updated ->
                    attendance = updated
                    store.saveAttendance(updated)
                }
            )
        }
        if (academicOpen) {
            AcademicRecordsDialog(
                students = students,
                records = academicRecords,
                onDismiss = { academicOpen = false },
                onSave = { updated ->
                    academicRecords = updated
                    store.saveAcademicRecords(updated)
                },
                nextId = { store.nextAcademicId() }
            )
        }
        if (reportOpen) {
            ReportsDialog(
                students = students,
                payments = payments,
                attendance = attendance,
                academicRecords = academicRecords,
                onDismiss = { reportOpen = false }
            )
        }
        if (receiptHistoryOpen) {
            ReceiptHistoryDialog(
                students = students,
                payments = payments,
                profile = tuitionProfile,
                onDismiss = { receiptHistoryOpen = false },
                onEditPayment = { payment -> guarded("editPayment", "Edit Payment") { editPaymentOpen = payment } },
                onDeletePayment = { payment -> guarded("deletePayment", "Delete Payment") { deletePaymentConfirm = payment } }
            )
        }

        editPaymentOpen?.let { payment ->
            EditPaymentDialog(
                payment = payment,
                onDismiss = { editPaymentOpen = null },
                onSave = { updated ->
                    payments = payments.map { if (it === payment) updated else it }
                    store.savePayments(payments)
                    editPaymentOpen = null
                    Toast.makeText(context, "Payment updated.", Toast.LENGTH_SHORT).show()
                }
            )
        }

        deletePaymentConfirm?.let { payment ->
            AlertDialog(
                onDismissRequest = { deletePaymentConfirm = null },
                title = { Text("Delete payment permanently?") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("You are about to remove a payment record.")
                        Text("Month: ${payment.month}")
                        Text("Amount: ₹${payment.amount}")
                        Text("Receipt: ${payment.receiptNo}")
                        Text("This cannot be undone unless you restore a backup.")
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        undoPaymentSnapshot = payment
                        payments = payments.filterNot { it === payment }
                        store.savePayments(payments)
                        deletePaymentConfirm = null

                        snackbarScope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = "Payment deleted",
                                actionLabel = "UNDO",
                                withDismissAction = true,
                                duration = SnackbarDuration.Short
                            )
                            if (result == SnackbarResult.ActionPerformed &&
                                undoPaymentSnapshot?.receiptNo == payment.receiptNo
                            ) {
                                if (payments.none { it === payment }) {
                                    payments = payments + payment
                                    store.savePayments(payments)
                                    Toast.makeText(context, "Payment restored.", Toast.LENGTH_SHORT).show()
                                }
                                undoPaymentSnapshot = null
                            }
                        }
                    }) { Text("Delete") }
                },
                dismissButton = {
                    TextButton(onClick = { deletePaymentConfirm = null }) { Text("Cancel") }
                }
            )
        }
        if (restoreConfirmOpen) {
            AlertDialog(
                onDismissRequest = { restoreConfirmOpen = false },
                title = { Text("Restore backup?") },
                text = {
                    Text(
                        "Restoring will replace the students, payment history and tuition profile currently stored on this phone. " +
                            "For safety, make a fresh backup before continuing."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        restoreConfirmOpen = false
                        (context as? MainActivity)?.pickBackup { ok ->
                            if (ok) {
                                students = store.loadStudents()
                                payments = store.loadPayments()
                                attendance = store.loadAttendance()
                                academicRecords = store.loadAcademicRecords()
                                tuitionProfile = store.loadTuitionProfile()
                            }
                            Toast.makeText(
                                context,
                                if (ok) "Backup restored successfully." else "Restore failed. No data was changed.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }) { Text("Choose Backup") }
                },
                dismissButton = { TextButton(onClick = { restoreConfirmOpen = false }) { Text("Cancel") } }
            )
        }

        if (demoToolsOpen) DemoToolsDialog(
            students = students,
            onDismiss = { demoToolsOpen = false },
            onAddDemo = { demo ->
                val seeded = demo // demo IDs are negative and cannot collide with normal student IDs
                students = students + seeded
                store.saveStudents(students)
                demoToolsOpen = false
            },
            onRemoveDemo = {
                val demoIds = students.filter { it.id < 0L }.map { it.id }.toSet()
                students = students.filterNot { it.id in demoIds }
                payments = payments.filterNot { it.studentId in demoIds }
                store.saveStudents(students)
                store.savePayments(payments)
                demoToolsOpen = false
            }
        )
        if (tuitionProfileOpen) TuitionProfileDialog(
            profile = tuitionProfile,
            onDismiss = { tuitionProfileOpen = false },
            onSave = {
                tuitionProfile = it
                store.saveTuitionProfile(it)
                tuitionProfileOpen = false
            }
        )
        if (themeOpen) ThemeDialog(themeMode, accentName, { themeOpen=false }, { m,a -> themeMode=m; accentName=a; store.saveTheme(m,a) })
        if (securityControlsOpen) SecurityControlsDialog(store, { securityControlsOpen=false })
        if (todayWorkOpen) TodayWorkDialog(routines, batchNotes, { todayWorkOpen=false }, { liveBatchOpen=true; todayWorkOpen=false })
        if (liveBatchOpen) LiveBatchDialog(routines, batchNotes, { liveBatchOpen=false }, { updated -> routines=updated; store.saveBatchRoutines(updated) }, { updated -> batchNotes=updated; store.saveBatchNotes(updated) })

    }
}

@Composable
fun ThemeDialog(mode:String, accent:String, onDismiss:()->Unit, onSave:(String,String)->Unit) {
    var m by remember{mutableStateOf(mode)}; var a by remember{mutableStateOf(accent)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Theme & Colors")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Theme",fontWeight=FontWeight.Bold); listOf("System","Light","Dark").forEach{x->FilterChip(m==x,{m=x},label={Text(x)})}
        Text("Accent color",fontWeight=FontWeight.Bold); Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Blue","Green","Purple","Orange","Red","Teal").forEach{x->FilterChip(a==x,{a=x},label={Text(x)})}}
    }},confirmButton={Button(onClick={onSave(m,a);onDismiss()}){Text("Apply")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})
}

@Composable
fun SecurityControlsDialog(store:LocalStore,onDismiss:()->Unit){
    val items=listOf("payment" to "Make Payment","addStudent" to "Add Student","editStudent" to "Edit Student","deleteStudent" to "Delete Student","editPayment" to "Edit Payment","deletePayment" to "Delete Payment","attendance" to "Attendance","academic" to "Academic Records","profile" to "Tuition Profile","demo" to "Demo / Test Data","restore" to "Restore Backup")
    AlertDialog(onDismissRequest=onDismiss,title={Text("Protected Actions")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)){
        Text("Choose which actions require your 4-digit PIN.",style=MaterialTheme.typography.bodySmall)
        items.forEach{(key,label)->var checked by remember{mutableStateOf(store.isProtected(key))};Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(checked,{checked=it;store.setProtected(key,it)})}}
    }},confirmButton={TextButton(onClick=onDismiss){Text("Done")}})
}

@Composable
fun TodayWorkDialog(routines:List<BatchRoutine>,notes:List<BatchNote>,onDismiss:()->Unit,onLive:()->Unit){
    val day=SimpleDateFormat("EEEE",Locale.getDefault()).format(Date());val rs=routines.filter{it.day.equals(day,true)}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Today’s Work",fontWeight=FontWeight.Bold)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        if(rs.isEmpty())Text("No tuition routine scheduled today.") else rs.forEach{Text("🟢 ${it.start}–${it.end} • ${it.batch}")}
        Text("Recent Live Batch notes",fontWeight=FontWeight.Bold);notes.takeLast(8).reversed().forEach{Text("${it.date} • ${it.batch} • ${it.type}: ${it.text}",style=MaterialTheme.typography.bodySmall)}
    }},confirmButton={Button(onClick=onLive){Text("Open Live Batch")}},dismissButton={TextButton(onClick=onDismiss){Text("Close")}})
}

@Composable
fun LiveBatchDialog(routines:List<BatchRoutine>,notes:List<BatchNote>,onDismiss:()->Unit,onRoutines:(List<BatchRoutine>)->Unit,onNotes:(List<BatchNote>)->Unit){
    val batches=routines.map{it.batch}.filter{it.isNotBlank()}.distinct().sorted(); val dayNow=SimpleDateFormat("EEEE",Locale.getDefault()).format(Date()); val timeNow=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()); val auto=routines.firstOrNull{it.day.equals(dayNow,true) && timeNow >= it.start && timeNow <= it.end}?.batch; var batch by remember{mutableStateOf(auto ?: batches.firstOrNull()?:(""))};var type by remember{mutableStateOf("Homework")};var text by remember{mutableStateOf("")};var addRoutine by remember{mutableStateOf(false)};var day by remember{mutableStateOf("Monday")};var start by remember{mutableStateOf("16:00")};var end by remember{mutableStateOf("17:00")}
    AlertDialog(onDismissRequest=onDismiss,title={Text("Live Batch",fontWeight=FontWeight.Bold)},text={Column(Modifier.fillMaxWidth().heightIn(max=600.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Select batch",fontWeight=FontWeight.Bold);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){batches.forEach{FilterChip(batch==it,{batch=it},label={Text(it)})}}
        OutlinedTextField(batch,{batch=it},label={Text("Batch name")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Class Notes","Homework","New Task","Reminder").forEach{FilterChip(type==it,{type=it},label={Text(it)})}}
        OutlinedTextField(text,{text=it},label={Text("Write $type")},minLines=3,modifier=Modifier.fillMaxWidth())
        Button(enabled=batch.isNotBlank()&&text.isNotBlank(),onClick={onNotes(notes+BatchNote(System.currentTimeMillis(),batch,currentDate(),type,text.trim()));text=""},modifier=Modifier.fillMaxWidth()){Text("Save Note")}
        HorizontalDivider();Text("Weekly routine",fontWeight=FontWeight.Bold);routines.filter{it.batch==batch}.forEach{Text("${it.day} • ${it.start}–${it.end}")}
        OutlinedButton(onClick={addRoutine=!addRoutine},modifier=Modifier.fillMaxWidth()){Text(if(addRoutine)"Hide routine editor" else "Add routine")}
        if(addRoutine){Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)){listOf("Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday").forEach{FilterChip(day==it,{day=it},label={Text(it.take(3))})}};Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){OutlinedTextField(start,{start=it},label={Text("Start")},modifier=Modifier.weight(1f),singleLine=true);OutlinedTextField(end,{end=it},label={Text("End")},modifier=Modifier.weight(1f),singleLine=true)};Button(onClick={onRoutines(routines+BatchRoutine(System.currentTimeMillis(),batch,day,start,end));addRoutine=false}){Text("Save Routine")}}
    }},confirmButton={TextButton(onClick=onDismiss){Text("Done")}})
}

@Composable
fun CreateSecurityPinDialog(
    onCreated: (String) -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { },
        title = { Text("🔐 Create Security PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Create a 4-digit PIN. You will need it for the important actions you have selected in Security Controls.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value -> pin = value.filter(Char::isDigit).take(4); error = "" },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { value -> confirm = value.filter(Char::isDigit).take(4); error = "" },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                enabled = pin.length == 4 && confirm.length == 4,
                onClick = {
                    when {
                        pin != confirm -> { error = "PINs do not match."; confirm = "" }
                        pin.length != 4 -> error = "PIN must contain exactly 4 digits."
                        else -> onCreated(pin)
                    }
                }
            ) { Text("Create PIN") }
        }
    )
}

@Composable
fun SecurityPinDialog(
    title: String,
    storedPin: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🔐 $title", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Enter your 4-digit security PIN to continue.")
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value -> pin = value.filter(Char::isDigit).take(4); error = "" },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                enabled = pin.length == 4,
                onClick = {
                    if (pin == storedPin) onSuccess()
                    else { pin = ""; error = "Incorrect PIN. Try again." }
                }
            ) { Text("Unlock") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ChangeSecurityPinDialog(
    currentPin: String,
    onDismiss: () -> Unit,
    onSaved: (String) -> Unit
) {
    var current by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun pinField(value: String, label: String, onChange: (String) -> Unit) {
        // Helper body intentionally empty; fields are rendered below so Compose can preserve state.
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Security PIN", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Use exactly 4 digits. The PIN protects payments, student changes and other important actions.")
                OutlinedTextField(current, { current = it.filter(Char::isDigit).take(4); error = "" }, label = { Text("Current PIN") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(newPin, { newPin = it.filter(Char::isDigit).take(4); error = "" }, label = { Text("New 4-digit PIN") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(confirm, { confirm = it.filter(Char::isDigit).take(4); error = "" }, label = { Text("Confirm new PIN") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(
                enabled = current.length == 4 && newPin.length == 4 && confirm.length == 4,
                onClick = {
                    when {
                        current != currentPin -> error = "Current PIN is incorrect."
                        newPin == currentPin -> error = "Choose a different PIN."
                        newPin != confirm -> error = "New PINs do not match."
                        else -> onSaved(newPin)
                    }
                }
            ) { Text("Save PIN") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DashboardCard(
    title: String,
    value: String,
    modifier: Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Tap to filter",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun TuitionProfileDialog(
    profile: TuitionProfile,
    onDismiss: () -> Unit,
    onSave: (TuitionProfile) -> Unit
) {
    var tuitionName by remember { mutableStateOf(profile.tuitionName) }
    var tagline by remember { mutableStateOf(profile.tagline) }
    var teacherName by remember { mutableStateOf(profile.teacherName) }
    var qualification by remember { mutableStateOf(profile.qualification) }
    var phone by remember { mutableStateOf(profile.phone) }
    var address by remember { mutableStateOf(profile.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tuition Profile", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("These details are used on receipts and reports.", style = MaterialTheme.typography.bodySmall)

                OutlinedTextField(tuitionName, { tuitionName = it }, label = { Text("Tuition name") }, singleLine = true)
                OutlinedTextField(tagline, { tagline = it }, label = { Text("Tagline") }, singleLine = true)
                OutlinedTextField(teacherName, { teacherName = it }, label = { Text("Teacher / Developer name") }, singleLine = true)
                OutlinedTextField(qualification, { qualification = it }, label = { Text("Qualification") }, singleLine = true)
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                OutlinedTextField(address, { address = it }, label = { Text("Address") }, minLines = 2)
            }
        },
        confirmButton = {
            Button(
                enabled = tuitionName.isNotBlank() && teacherName.isNotBlank(),
                onClick = {
                    onSave(
                        TuitionProfile(
                            tuitionName.trim(),
                            tagline.trim(),
                            teacherName.trim(),
                            qualification.trim(),
                            phone.trim(),
                            address.trim()
                        )
                    )
                }
            ) { Text("Save Profile") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

val DEMO_STUDENT_NAMES = listOf(
    "Aarav Sharma", "Aditya Das", "Anik Ghosh", "Arjun Saha", "Ayush Roy",
    "Debjit Paul", "Ishita Das", "Kunal Mondal", "Moumita Ghosh", "Nandini Saha",
    "Niloy Das", "Pritam Roy", "Rahul Paul", "Riya Ghosh", "Rohan Saha",
    "Sayan Das", "Sneha Roy", "Soumik Paul", "Srija Ghosh", "Subhajit Das",
    "Tanmoy Roy", "Tisha Saha", "Uday Paul", "Ananya Das", "Abhishek Roy",
    "Diya Ghosh", "Iman Saha", "Joy Das", "Madhurima Roy", "Sourav Paul"
)

fun demoStudents(): List<Student> {
    val classes = listOf("V", "VI", "VII", "VIII", "IX", "X")
    val batches = listOf("Morning", "Evening", "Weekend")
    return DEMO_STUDENT_NAMES.mapIndexed { i, name ->
        Student(
            id = -(i + 1L),
            name = name,
            className = classes[i % classes.size],
            batch = batches[i % batches.size],
            monthlyFee = listOf(500, 600, 700, 800, 1000)[i % 5],
            phone = "900000${(1000 + i).toString()}",
            joiningMonth = currentMonth()
        )
    }
}

@Composable
fun DemoToolsDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onAddDemo: (List<Student>) -> Unit,
    onRemoveDemo: () -> Unit
) {
    var confirmRemove by remember { mutableStateOf(false) }
    val demoCount = students.count { it.name in DEMO_STUDENT_NAMES }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Demo / Test Data", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Use 30 clearly fake students to stress-test search, filters, profiles, fees and reports.")
                Text("Demo students currently loaded: $demoCount")
                Text("Their phone numbers begin with 900000 and are not real contact numbers.")
            }
        },
        confirmButton = {
            Button(
                enabled = demoCount == 0,
                onClick = { onAddDemo(demoStudents()) }
            ) { Text(if (demoCount == 0) "Add 30 Students" else "Already Added") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { confirmRemove = true }) { Text("Remove Demo") }
                TextButton(onClick = onDismiss) { Text("Close") }
            }
        }
    )

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Remove demo students permanently?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Only the built-in demo students will be removed.")
                    Text("Their related demo records will also be removed.")
                    Text("Your real students and records will not be affected.")
                }
            },
            confirmButton = {
                Button(onClick = { confirmRemove = false; onRemoveDemo() }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } }
        )
    }
}


@Composable
fun AttendanceDialog(
    students: List<Student>,
    records: List<AttendanceRecord>,
    onDismiss: () -> Unit,
    onSave: (List<AttendanceRecord>) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedBatch by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }

    val batches = listOf("All") + students.map { it.batch }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    val dateKey = selectedDate.toString()
    val dateLabel = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))

    fun statusFor(studentId: Long): String =
        records.firstOrNull { it.studentId == studentId && it.date == dateKey }?.status ?: "UNMARKED"

    fun setStatus(studentId: Long, status: String) {
        val updated = records
            .filterNot { it.studentId == studentId && it.date == dateKey }
            .toMutableList()

        if (status != "UNMARKED") {
            updated.add(AttendanceRecord(studentId, dateKey, status))
        }
        onSave(updated)
    }

    val visibleStudents = students
        .filter {
            it.name.contains(search, true) ||
                it.className.contains(search, true) ||
                it.batch.contains(search, true)
        }
        .filter { selectedBatch == "All" || it.batch == selectedBatch }
        .sortedBy { it.name.lowercase() }

    val presentCount = students.count { statusFor(it.id) == "PRESENT" }
    val absentCount = students.count { statusFor(it.id) == "ABSENT" }
    val lateCount = students.count { statusFor(it.id) == "LATE" }
    val markedCount = presentCount + absentCount + lateCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Attendance", fontWeight = FontWeight.Bold)
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { selectedDate = selectedDate.minusDays(1) },
                        modifier = Modifier.weight(1f)
                    ) { Text("‹") }

                    Text(
                        dateLabel,
                        modifier = Modifier.weight(2f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = { selectedDate = selectedDate.plusDays(1) },
                        modifier = Modifier.weight(1f)
                    ) { Text("›") }
                }

                if (selectedDate != LocalDate.now()) {
                    TextButton(
                        onClick = { selectedDate = LocalDate.now() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text("Today") }
                }

                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    FilterChip(
                        selected = selectedBatch == "All",
                        onClick = { selectedBatch = "All" },
                        label = { Text("All") }
                    )
                    batches.drop(1).forEach { batch ->
                        FilterChip(
                            selected = selectedBatch == batch,
                            onClick = { selectedBatch = batch },
                            label = { Text(batch) }
                        )
                    }
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search student") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("Present $presentCount")
                        Text("Absent $absentCount")
                        Text("Late $lateCount")
                        Text("Marked $markedCount/${students.size}")
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            var updated = records
                                .filterNot { it.date == dateKey }
                                .toMutableList()
                            visibleStudents.forEach {
                                updated.add(AttendanceRecord(it.id, dateKey, "PRESENT"))
                            }
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("All Present") }

                    OutlinedButton(
                        onClick = {
                            var updated = records
                                .filterNot { it.date == dateKey }
                                .toMutableList()
                            visibleStudents.forEach {
                                updated.add(AttendanceRecord(it.id, dateKey, "ABSENT"))
                            }
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("All Absent") }
                }

                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(visibleStudents, key = { it.id }) { student ->
                        val status = statusFor(student.id)

                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(student.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Class ${student.className} • Batch ${student.batch}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        status,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    FilterChip(
                                        selected = status == "PRESENT",
                                        onClick = { setStatus(student.id, "PRESENT") },
                                        label = { Text("Present") }
                                    )
                                    FilterChip(
                                        selected = status == "ABSENT",
                                        onClick = { setStatus(student.id, "ABSENT") },
                                        label = { Text("Absent") }
                                    )
                                    FilterChip(
                                        selected = status == "LATE",
                                        onClick = { setStatus(student.id, "LATE") },
                                        label = { Text("Late") }
                                    )
                                    FilterChip(
                                        selected = status == "UNMARKED",
                                        onClick = { setStatus(student.id, "UNMARKED") },
                                        label = { Text("Clear") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}


fun shareAcademicResultWhatsApp(context: Context, student: Student, record: AcademicRecord, previous: AcademicRecord?) {
    if (student.phone.isBlank()) { Toast.makeText(context, "No phone number saved for ${student.name}.", Toast.LENGTH_LONG).show(); return }
    val pct = if (record.maxMarks > 0) ((record.marks / record.maxMarks) * 100).roundToInt() else 0
    val prevText = if (previous != null) { val pp=((previous.marks/previous.maxMarks)*100).roundToInt(); "\nPrevious: ${previous.marks.toCleanNumber()}/${previous.maxMarks.toCleanNumber()} ($pp%)" } else ""
    val body="The Math Guide\nTest Progress Report\nStudent: ${student.name}\nSubject: ${record.subject.ifBlank{"—"}}\nTest: ${record.title.ifBlank{"Test"}}\nDate: ${record.date}\nMarks: ${record.marks.toCleanNumber()}/${record.maxMarks.toCleanNumber()} ($pct%)$prevText\n${record.remarks.takeIf{it.isNotBlank()}?.let{"Remarks: $it\n"} ?: ""}Thank you."
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${student.phone.filter(Char::isDigit)}?text=${Uri.encode(body)}"))) } catch(_:Exception){Toast.makeText(context,"Unable to open WhatsApp.",Toast.LENGTH_LONG).show()}
}

@Composable
fun AcademicRecordsDialog(
    students: List<Student>,
    records: List<AcademicRecord>,
    onDismiss: () -> Unit,
    onSave: (List<AcademicRecord>) -> Unit,
    nextId: () -> Long
) {
    val context = LocalContext.current
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: 0L) }
    var subject by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var marks by remember { mutableStateOf("") }
    var maxMarks by remember { mutableStateOf("100") }
    var remarks by remember { mutableStateOf("") }
    var filterStudent by remember { mutableStateOf("All") }

    val selectedStudent = students.firstOrNull { it.id == selectedStudentId }
    val visibleRecords = records
        .filter { filterStudent == "All" || it.studentId.toString() == filterStudent }
        .sortedByDescending { it.date }

    val averagePercent = run {
        val valid = records.filter { it.maxMarks > 0 }
        if (valid.isEmpty()) 0
        else ((valid.sumOf { it.marks } / valid.sumOf { it.maxMarks }) * 100).roundToInt()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Academic Records", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 600.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Add Test / Exam Result", fontWeight = FontWeight.Bold)

                if (students.isEmpty()) {
                    Text("Add a student before recording academic results.")
                } else {
                    var studentMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { studentMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(selectedStudent?.name ?: "Select student")
                        }
                        DropdownMenu(
                            expanded = studentMenu,
                            onDismissRequest = { studentMenu = false }
                        ) {
                            students.sortedBy { it.name.lowercase() }.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text("${s.name} • Class ${s.className}") },
                                    onClick = {
                                        selectedStudentId = s.id
                                        studentMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(
                            subject, { subject = it },
                            label = { Text("Subject") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            title, { title = it },
                            label = { Text("Test / Exam") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedTextField(
                            marks, { marks = it },
                            label = { Text("Marks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            maxMarks, { maxMarks = it },
                            label = { Text("Max marks") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        remarks, { remarks = it },
                        label = { Text("Teacher remarks") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        enabled = selectedStudentId != 0L &&
                            marks.toDoubleOrNull() != null &&
                            maxMarks.toDoubleOrNull()?.let { it > 0 } == true &&
                            (marks.toDoubleOrNull() ?: -1.0) >= 0 &&
                            (marks.toDoubleOrNull() ?: 0.0) <= (maxMarks.toDoubleOrNull() ?: 0.0),
                        onClick = {
                            val markValue = marks.toDoubleOrNull() ?: return@Button
                            val maxValue = maxMarks.toDoubleOrNull() ?: return@Button
                            val record = AcademicRecord(
                                id = nextId(),
                                studentId = selectedStudentId,
                                date = currentDate(),
                                subject = subject.trim(),
                                title = title.trim(),
                                marks = markValue,
                                maxMarks = maxValue,
                                remarks = remarks.trim()
                            )
                            onSave(records + record)
                            marks = ""
                            title = ""
                            subject = ""
                            remarks = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Result") }

                    HorizontalDivider()

                    Text("Academic Overview", fontWeight = FontWeight.Bold)
                    Text("Total records: ${records.size}")
                    Text("Overall average: $averagePercent%")

                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        FilterChip(
                            selected = filterStudent == "All",
                            onClick = { filterStudent = "All" },
                            label = { Text("All Students") }
                        )
                        students.sortedBy { it.name.lowercase() }.forEach { s ->
                            FilterChip(
                                selected = filterStudent == s.id.toString(),
                                onClick = { filterStudent = s.id.toString() },
                                label = { Text(s.name) }
                            )
                        }
                    }

                    if (visibleRecords.isEmpty()) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("📝 No academic records yet.", fontWeight = FontWeight.SemiBold)
                            Text("Test results will appear here after you record a test.", style = MaterialTheme.typography.bodySmall)
                        }
                    } else {
                        visibleRecords.forEach { record ->
                            val student = students.firstOrNull { it.id == record.studentId }
                            val percent = if (record.maxMarks > 0) ((record.marks / record.maxMarks) * 100).roundToInt() else 0
                            val previous = records.filter { it.studentId == record.studentId && it.subject.equals(record.subject, true) && it.date < record.date }.maxByOrNull { it.date }

                            Card(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        student?.name ?: "Unknown student",
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${record.subject.ifBlank { "Subject not specified" }} • " +
                                            record.title.ifBlank { "Test / Exam" }
                                    )
                                    Text("${record.marks.toCleanNumber()} / ${record.maxMarks.toCleanNumber()}  •  $percent%", fontWeight = FontWeight.SemiBold)
                                    if (previous != null) {
                                        val pp = ((previous.marks / previous.maxMarks) * 100).roundToInt()
                                        val diff = percent - pp
                                        Text("Previous: ${previous.marks.toCleanNumber()}/${previous.maxMarks.toCleanNumber()} • ${if(diff>=0) "+" else ""}$diff%", style=MaterialTheme.typography.labelSmall)
                                    }
                                    Text(record.date, style = MaterialTheme.typography.labelSmall)
                                    if (record.remarks.isNotBlank()) {
                                        Text("Remarks: ${record.remarks}")
                                    }
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.End) {
                                        TextButton(onClick={ if(student!=null) shareAcademicResultWhatsApp(context, student, record, previous) }) { Text("WhatsApp") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

fun Double.toCleanNumber(): String =
    if (this % 1.0 == 0.0) this.toInt().toString()
    else String.format(Locale.ENGLISH, "%.1f", this)

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onManage: () -> Unit,
    onAttendance: () -> Unit,
    onAcademic: () -> Unit,
    onReports: () -> Unit,
    onReceiptHistory: () -> Unit,
    onTuitionProfile: () -> Unit,
    onDemoTools: () -> Unit,
    lastBackupDate: String,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onSecurityPin: () -> Unit,
    onTheme: () -> Unit,
    onSecurityControls: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "STUDENTS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                SettingsButton("＋  Add Student", onAdd)
                SettingsButton("👨‍🎓  Manage Students", onManage)
                SettingsButton("📅  Attendance", onAttendance)
                SettingsButton("📚  Academic Records", onAcademic)

                Spacer(Modifier.height(4.dp))

                Text(
                    "DATA & REPORTS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                SettingsButton("📊  Reports", onReports)
                SettingsButton("🧾  Receipt History", onReceiptHistory)
                SettingsButton("🏫  Tuition Profile", onTuitionProfile)
                SettingsButton("🧪  Demo / Test Data", onDemoTools)
                SettingsButton("💾  Backup Data", onBackup)
                if (lastBackupDate.isNotBlank()) {
                    Text(
                        "Last backup: $lastBackupDate",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                } else {
                    Text(
                        "No backup created yet",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                SettingsButton("♻️  Restore Data", onRestore)

                HorizontalDivider(Modifier.padding(vertical = 2.dp))
                Text(
                    "SAFETY",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                SettingsButton("🔐  Security PIN", onSecurityPin)
                SettingsButton("🎨  Theme & Colors", onTheme)
                SettingsButton("🛡️  Protected Actions", onSecurityControls)
                Text(
                    "A 4-digit PIN is required before payments, student changes and other important modifications.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(4.dp))

                Text(
                    "Tip: keep a backup before editing or deleting payments.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                HorizontalDivider(Modifier.padding(vertical = 6.dp))

                Text(
                    "The Math Guide",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "My Tuition Manager",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Version 1.3",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Tap a student card to view the complete profile.",
                    style = MaterialTheme.typography.bodySmall
                )

                HorizontalDivider(Modifier.padding(vertical = 6.dp))

                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Developer",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Ashraful Hoque",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            "B.SC Maths",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            "Jatarpur, Murshidabad, West Bengal, 742147",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun ManageStudentsDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onEdit: (Student) -> Unit,
    onDelete: (Student) -> Unit
) {
    var deleteTarget by remember { mutableStateOf<Student?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Students") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (students.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("👨‍🎓 No students available.", fontWeight = FontWeight.Bold)
                        Text("Add a student to see them here.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                students.sortedBy { it.name.lowercase() }.forEach { s ->
                    Card {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(s.name, fontWeight = FontWeight.Bold)
                                Text("Class ${s.className} • ${s.batch}", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { onEdit(s) }) { Text("Edit") }
                            TextButton(onClick = { deleteTarget = s }) { Text("Delete") }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
    deleteTarget?.let { s ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${s.name}?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This is a permanent action.")
                    Text("Deleting this student will also remove their:")
                    Text("• Payment history")
                    Text("• Attendance records")
                    Text("• Academic / test records")
                    Text("This cannot be undone unless you restore a backup.")
                }
            },
            confirmButton = {
                Button(onClick = { deleteTarget = null; onDelete(s) }) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsButton(text: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, Modifier.fillMaxWidth()) { Text(text) }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier) = DashboardCard(title, value, modifier)

@Composable
fun StudentEditorDialog(
    title: String,
    initialStudent: Student?,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {
    val standardClasses = listOf("V", "VI", "VII", "VIII", "IX", "X")
    val existing = initialStudent?.className ?: ""

    var name by remember { mutableStateOf(initialStudent?.name ?: "") }
    var cls by remember {
        mutableStateOf(if (existing.isBlank() || existing in standardClasses) existing else "Others")
    }
    var otherClass by remember {
        mutableStateOf(if (existing.isNotBlank() && existing !in standardClasses) existing else "")
    }
    var classOpen by remember { mutableStateOf(false) }
    var batch by remember { mutableStateOf(initialStudent?.batch ?: "") }
    var fee by remember { mutableStateOf(initialStudent?.monthlyFee?.toString() ?: "") }
    var phone by remember { mutableStateOf(initialStudent?.phone ?: "") }
    var school by remember { mutableStateOf(initialStudent?.school ?: "") }
    var fatherName by remember { mutableStateOf(initialStudent?.fatherName ?: "") }
    var motherName by remember { mutableStateOf(initialStudent?.motherName ?: "") }
    var address by remember { mutableStateOf(initialStudent?.address ?: "") }
    var subjects by remember { mutableStateOf(initialStudent?.subjects ?: "") }
    var notes by remember { mutableStateOf(initialStudent?.notes ?: "") }
    var studentStatus by remember { mutableStateOf(initialStudent?.status ?: "Active") }
    var joiningMonth by remember { mutableStateOf(initialStudent?.joiningMonth ?: currentMonth()) }
    var joiningOpen by remember { mutableStateOf(false) }
    var otherMonth by remember { mutableStateOf(false) }

    val finalClass = if (cls == "Others") otherClass.trim() else cls.trim()
    val months = recentMonths()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    name, { name = it }, label = { Text("Student name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { classOpen = true },
                        Modifier.fillMaxWidth()
                    ) { Text(if (cls.isBlank()) "Select Class" else cls) }

                    DropdownMenu(classOpen, { classOpen = false }) {
                        standardClasses.forEach { c ->
                            DropdownMenuItem({ Text(c) }, {
                                cls = c
                                classOpen = false
                            })
                        }
                        DropdownMenuItem({ Text("Others") }, {
                            cls = "Others"
                            classOpen = false
                        })
                    }
                }

                if (cls == "Others") {
                    OutlinedTextField(
                        otherClass, { otherClass = it },
                        label = { Text("Enter class") },
                        placeholder = { Text("e.g. XI, XII, JEE") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }

                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { joiningOpen = true },
                        Modifier.fillMaxWidth()
                    ) {
                        Text(if (joiningMonth.isBlank()) "Select Joining Month" else "Joining Month: $joiningMonth")
                    }

                    DropdownMenu(joiningOpen, { joiningOpen = false }) {
                        months.forEach { m ->
                            DropdownMenuItem({ Text(m) }, {
                                joiningMonth = m
                                joiningOpen = false
                            })
                        }
                        DropdownMenuItem({ Text("Enter manually") }, {
                            joiningOpen = false
                            otherMonth = true
                        })
                    }
                }

                if (otherMonth) {
                    OutlinedTextField(
                        joiningMonth, { joiningMonth = it },
                        label = { Text("Joining Month") },
                        placeholder = { Text("e.g. August 2026") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    batch, { batch = it }, label = { Text("Batch") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    fee, { fee = it }, label = { Text("Monthly fee") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    phone, { phone = it }, label = { Text("Phone") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text("Student Status", fontWeight = FontWeight.Bold)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Active","Paused","Left").forEach { st -> FilterChip(studentStatus == st, { studentStatus = st }, label = { Text(st) }) }
                }
                Text("Additional Information", fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    school, { school = it }, label = { Text("School / Institution") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    fatherName, { fatherName = it }, label = { Text("Father / Guardian Name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    motherName, { motherName = it }, label = { Text("Mother Name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    address, { address = it }, label = { Text("Address") },
                    minLines = 2, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    subjects, { subjects = it }, label = { Text("Subjects") },
                    placeholder = { Text("e.g. Maths, Science, English") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    notes, { notes = it }, label = { Text("Teacher Notes") },
                    minLines = 2, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && fee.toIntOrNull() != null && finalClass.isNotBlank() && joiningMonth.isNotBlank(),
                onClick = {
                    onSave(
                        Student(
                            initialStudent?.id ?: System.currentTimeMillis(),
                            name.trim(), finalClass, batch.trim(), fee.toInt(),
                            phone.trim(), joiningMonth.trim(),
                            school.trim(), fatherName.trim(), motherName.trim(),
                            address.trim(), subjects.trim(), notes.trim(), studentStatus
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StudentDetailsDialog(
    student: Student,
    payments: List<Payment>,
    outstanding: Int,
    onDismiss: () -> Unit,
    onCollect: () -> Unit
) {

    val thisMonth = currentMonth()
    val currentPaid = payments
        .filter { it.month.equals(thisMonth, true) }
        .sumOf { it.amount }

    val totalPaid = payments.sumOf { it.amount }
    val currentDue = maxOf(0, student.monthlyFee - currentPaid)

    val status = when {
        currentPaid >= student.monthlyFee -> "PAID"
        currentPaid > 0 -> "PARTIAL"
        else -> "UNPAID"
    }

    /*
     * Build a month-by-month timeline.
     * We start from the joining month when it is available, and also
     * include every month for which a payment has been recorded.
     */
    val joiningKey = monthKey(student.joiningMonth)
    val paymentKeys = payments.mapNotNull { monthKey(it.month) }

    val firstKey = listOfNotNull(joiningKey, paymentKeys.minOrNull()).minOrNull()
    val lastKey = listOfNotNull(YearMonth.now(), paymentKeys.maxOrNull()).maxOrNull()

    val timelineMonths = mutableListOf<YearMonth>()

    if (firstKey != null && lastKey != null) {
        var m: YearMonth = firstKey!!
        val endMonth: YearMonth = lastKey!!

        while (!m.isAfter(endMonth)) {
            timelineMonths.add(m)
            m = m.plusMonths(1)
        }
    } else {
        timelineMonths.addAll(paymentKeys.distinct().sorted())
    }

    val monthFormatter = DateTimeFormatter.ofPattern(
        "MMMM yyyy",
        Locale.ENGLISH
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    student.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    "Student Profile",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                /* STUDENT INFORMATION */
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Class ${student.className}",
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Batch ${student.batch.ifBlank { "—" }}"
                                )
                            }

                            AssistChip(
                                onClick = {},
                                label = { Text(status) }
                            )
                        }

                        HorizontalDivider()

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Monthly fee",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    "₹${student.monthlyFee}",
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "Joined",
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    student.joiningMonth.ifBlank { "Not provided" },
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (student.phone.isNotBlank()) {
                            Text("☎ ${student.phone}")
                        }

                        Text(
                            "Student ID: ${student.id}",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                val hasAdditionalInfo = student.school.isNotBlank() ||
                    student.fatherName.isNotBlank() ||
                    student.motherName.isNotBlank() ||
                    student.address.isNotBlank() ||
                    student.subjects.isNotBlank() ||
                    student.notes.isNotBlank()

                if (hasAdditionalInfo) {
                    Text(
                        "Student Information",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (student.school.isNotBlank()) {
                                ProfileInfoRow("School", student.school)
                            }
                            if (student.fatherName.isNotBlank()) {
                                ProfileInfoRow("Father / Guardian", student.fatherName)
                            }
                            if (student.motherName.isNotBlank()) {
                                ProfileInfoRow("Mother", student.motherName)
                            }
                            if (student.address.isNotBlank()) {
                                ProfileInfoRow("Address", student.address)
                            }
                            if (student.subjects.isNotBlank()) {
                                ProfileInfoRow("Subjects", student.subjects)
                            }
                            if (student.notes.isNotBlank()) {
                                ProfileInfoRow("Teacher Notes", student.notes)
                            }
                        }
                    }
                }

                /* PAYMENT OVERVIEW */
                Text(
                    "Payment Overview",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardCard(
                        "This month",
                        money(currentPaid),
                        Modifier.weight(1f)
                    )
                    DashboardCard(
                        "Current due",
                        money(currentDue),
                        Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DashboardCard(
                        "Total paid",
                        money(totalPaid),
                        Modifier.weight(1f)
                    )
                    DashboardCard(
                        "Outstanding",
                        money(outstanding),
                        Modifier.weight(1f)
                    )
                }

                /* MONTHLY TIMELINE */
                Text(
                    "Fee Timeline",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                if (timelineMonths.isEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("💰 No fee history yet.", fontWeight = FontWeight.SemiBold)
                                Text("Payments for this student will appear here.", style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                "Use Collect Fee to record a payment.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    timelineMonths
                        .sortedDescending()
                        .forEach { monthKeyValue ->
                            val monthName = monthKeyValue.format(monthFormatter)

                            val monthPayments = payments.filter {
                                it.month.equals(monthName, true)
                            }

                            val paid = monthPayments.sumOf { it.amount }
                            val due = maxOf(0, student.monthlyFee - paid)

                            val monthStatus = when {
                                paid >= student.monthlyFee -> "PAID"
                                paid > 0 -> "PARTIAL"
                                else -> "DUE"
                            }

                            Card(Modifier.fillMaxWidth()) {
                                Column(
                                    Modifier.padding(13.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            monthName,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Text(
                                            monthStatus,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Paid: ₹$paid",
                                            style = MaterialTheme.typography.bodySmall
                                        )

                                        if (due > 0) {
                                            Text(
                                                "Due: ₹$due",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        } else {
                                            Text(
                                                "Due: ₹0",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }

                                    if (monthPayments.isNotEmpty()) {
                                        monthPayments.forEach { payment ->
                                            Text(
                                                "Payment ₹${payment.amount} • ${payment.date}",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCollect) {
                Text("Collect Fee")
            }
        },
        dismissButton = {}
    )
}

@Composable
fun CollectFeeDialog(
    student: Student,
    existingPayments: List<Payment>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    val eligibleMonths = eligibleFeeMonths(student, existingPayments)

    var selectedMonths by remember { mutableStateOf(emptySet<String>()) }
    val total = selectedMonths.size * student.monthlyFee

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collect Fee") },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(student.name, fontWeight = FontWeight.Bold)
                Text("Monthly fee: ₹${student.monthlyFee}")
                Text("Select one or more eligible months.")
                Text(
                    "Allowed: joining month through next month. " +
                        "A month can be collected only once."
                )

                if (eligibleMonths.isEmpty()) {
                    val joinValid = monthKey(student.joiningMonth) != null
                    Text(
                        if (!joinValid) "No eligible months: the joining month is invalid."
                        else "No eligible months available. This student may already be paid through next month.",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    eligibleMonths.forEach { month ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                selectedMonths = if (selectedMonths.contains(month)) {
                                    selectedMonths - month
                                } else {
                                    selectedMonths + month
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedMonths.contains(month),
                                onCheckedChange = {
                                    selectedMonths = if (it) selectedMonths + month
                                    else selectedMonths - month
                                }
                            )
                            Text(month)
                        }
                    }
                }

                if (selectedMonths.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Months selected: ${selectedMonths.size}", fontWeight = FontWeight.Bold)
                    Text("Total amount: ₹$total", fontWeight = FontWeight.Bold)
                    Text("One receipt will be generated for all selected months.")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedMonths.isNotEmpty(),
                onClick = {
                    val ordered = selectedMonths.sortedWith(
                        compareBy<String> { monthKey(it)?.toString() ?: "" }
                    )
                    onSave(ordered)
                }
            ) { Text("Save Payment") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun EditPaymentDialog(
    payment: Payment,
    onDismiss: () -> Unit,
    onSave: (Payment) -> Unit
) {
    var amount by remember { mutableStateOf(payment.amount.toString()) }
    var date by remember { mutableStateOf(payment.date) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Payment", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Receipt: ${payment.receiptNo}")
                Text("Month: ${payment.month}")
                OutlinedTextField(
                    amount,
                    { amount = it.filter(Char::isDigit) },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                OutlinedTextField(
                    date,
                    { date = it },
                    label = { Text("Payment date") },
                    singleLine = true
                )
                Text(
                    "Edit only factual payment details. The receipt number and month are kept unchanged.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                enabled = (amount.toIntOrNull() ?: 0) > 0 && date.isNotBlank(),
                onClick = {
                    onSave(
                        payment.copy(
                            amount = amount.toIntOrNull() ?: payment.amount,
                            date = date.trim()
                        )
                    )
                }
            ) { Text("Save Changes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ReceiptHistoryDialog(
    students: List<Student>,
    payments: List<Payment>,
    profile: TuitionProfile = TuitionProfile(),
    onDismiss: () -> Unit,
    onEditPayment: (Payment) -> Unit,
    onDeletePayment: (Payment) -> Unit
) {
    val context = LocalContext.current
    val studentMap = students.associateBy { it.id }
    var selectedReceipt by remember { mutableStateOf<List<Payment>?>(null) }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }

    val receiptGroups = payments
        .filter { it.receiptNo.isNotBlank() }
        .groupBy { it.receiptNo }
        .values
        .sortedByDescending { it.firstOrNull()?.date.orEmpty() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Receipt History", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Open or share any previous receipt.",
                    style = MaterialTheme.typography.bodySmall
                )

                if (receiptGroups.isEmpty()) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("🧾 No receipt history yet.", fontWeight = FontWeight.SemiBold)
                        Text("Completed payments will appear here.", style = MaterialTheme.typography.bodySmall)
                    }
                } else {
                    receiptGroups.forEach { group ->
                        val first = group.first()
                        val student = studentMap[first.studentId]
                        val total = group.sumOf { it.amount }
                        val months = group.sortedBy { monthKey(it.month) }
                            .joinToString(", ") { it.month }

                        Card {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(first.receiptNo, fontWeight = FontWeight.Bold)
                                Text(student?.name ?: "Unknown student")
                                Text("Date: ${first.date}")
                                Text("Months: $months")
                                Text("Amount: ₹$total", fontWeight = FontWeight.Bold)

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(
                                        enabled = student != null,
                                        onClick = {
                                            selectedStudent = student
                                            selectedReceipt = group
                                        }
                                    ) { Text("Share Receipt") }

                                    TextButton(onClick = {
                                        // Edit the individual month/payment while keeping receipt history intact.
                                        onEditPayment(group.first())
                                    }) { Text("Edit") }

                                    TextButton(onClick = {
                                        onDeletePayment(group.first())
                                    }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    val receiptPayments = selectedReceipt
    val receiptStudent = selectedStudent

    if (receiptPayments != null && receiptStudent != null) {
        val receiptNo = receiptPayments.first().receiptNo
        val uri = remember(receiptNo) {
            createReceiptPdf(context, receiptStudent, receiptPayments, receiptNo)
        }

        AlertDialog(
            onDismissRequest = {
                selectedReceipt = null
                selectedStudent = null
            },
            title = { Text("Share Receipt") },
            text = {
                Text(
                    "Receipt $receiptNo is ready. Choose how you want to send it."
                )
            },
            confirmButton = {
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                    Button(onClick = { shareReceiptText(context, receiptStudent, receiptPayments, receiptNo, profile, "WhatsApp"); selectedReceipt=null; selectedStudent=null }) { Text("WhatsApp") }
                    TextButton(onClick = { shareReceiptText(context, receiptStudent, receiptPayments, receiptNo, profile, "SMS"); selectedReceipt=null; selectedStudent=null }) { Text("SMS") }
                }
            },
            dismissButton = {
                TextButton(onClick = { shareReceipt(context, uri); selectedReceipt=null; selectedStudent=null }) { Text("PDF / Other") }
            }
        )
    }
}

fun createReportPdf(
    context: Context,
    students: List<Student>,
    payments: List<Payment>,
    reportType: String,
    selectedPeriod: String,
    attendance: List<AttendanceRecord> = emptyList(),
    academicRecords: List<AcademicRecord> = emptyList(),
    progressStudent: Student? = null
): Uri {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun t(v: String, x: Float, y: Float, size: Float, bold: Boolean = false) {
        paint.style = Paint.Style.FILL
        paint.textSize = size
        paint.isFakeBoldText = bold
        canvas.drawText(v.take(75), x, y, paint)
    }
    fun center(v: String, y: Float, size: Float, bold: Boolean = false) {
        paint.textSize = size
        paint.isFakeBoldText = bold
        canvas.drawText(v, (595f - paint.measureText(v)) / 2f, y, paint)
    }
    fun line(y: Float) {
        paint.style = Paint.Style.STROKE
        canvas.drawLine(45f, y, 550f, y, paint)
        paint.style = Paint.Style.FILL
    }

    center("THE MATH GUIDE", 55f, 25f, true)
    center("TUITION MANAGEMENT REPORT", 82f, 16f, true)
    center("$reportType • $selectedPeriod", 103f, 11f)
    line(120f)

    if (reportType == "Progress" && progressStudent != null) {
        val s = progressStudent
        val studentAcademic = academicRecords.filter { it.studentId == s.id }
        val studentAttendance = attendance.filter { it.studentId == s.id }
        val paidTotal = payments.filter { it.studentId == s.id }.sumOf { it.amount }

        center("THE MATH GUIDE", 55f, 25f, true)
        center("STUDENT PROGRESS REPORT", 82f, 16f, true)
        center(s.name, 105f, 15f, true)
        line(122f)

        t("STUDENT DETAILS", 45f, 150f, 13f, true)
        t("Class", 60f, 178f, 10f); t(s.className, 300f, 178f, 10f, true)
        t("Batch", 60f, 201f, 10f); t(s.batch.ifBlank { "—" }, 300f, 201f, 10f, true)
        t("Joining month", 60f, 224f, 10f); t(s.joiningMonth.ifBlank { "Not provided" }, 300f, 224f, 10f, true)
        t("Monthly fee", 60f, 247f, 10f); t("₹${s.monthlyFee}", 300f, 247f, 10f, true)
        t("Total fees paid", 60f, 270f, 10f); t("₹$paidTotal", 300f, 270f, 10f, true)

        val present = studentAttendance.count { it.status == "PRESENT" }
        val absent = studentAttendance.count { it.status == "ABSENT" }
        val late = studentAttendance.count { it.status == "LATE" }
        val marked = present + absent + late
        val attendanceRate = if (marked > 0) ((present.toFloat() / marked) * 100).roundToInt() else 0

        line(292f)
        t("ATTENDANCE", 45f, 318f, 13f, true)
        t("Present", 60f, 346f, 10f); t(present.toString(), 300f, 346f, 10f, true)
        t("Absent", 60f, 369f, 10f); t(absent.toString(), 300f, 369f, 10f, true)
        t("Late", 60f, 392f, 10f); t(late.toString(), 300f, 392f, 10f, true)
        t("Attendance rate", 60f, 415f, 10f); t("$attendanceRate%", 300f, 415f, 10f, true)

        line(437f)
        t("ACADEMIC PERFORMANCE", 45f, 463f, 13f, true)

        val validAcademic = studentAcademic.filter { it.maxMarks > 0 }
        val totalMarks = validAcademic.sumOf { it.marks }
        val totalMax = validAcademic.sumOf { it.maxMarks }
        val average = if (totalMax > 0) ((totalMarks / totalMax) * 100).roundToInt() else 0
        t("Overall average", 60f, 491f, 10f); t("$average%", 300f, 491f, 10f, true)
        t("Tests / exams", 60f, 514f, 10f); t(validAcademic.size.toString(), 300f, 514f, 10f, true)

        var py = 545f
        studentAcademic.sortedByDescending { it.date }.take(10).forEach { r ->
            val pct = if (r.maxMarks > 0) ((r.marks / r.maxMarks) * 100).roundToInt() else 0
            val label = "${r.subject.ifBlank { "Subject" }} • ${r.title.ifBlank { "Test" }}"
            t(label, 60f, py, 9f)
            t("${r.marks.toCleanNumber()}/${r.maxMarks.toCleanNumber()} ($pct%)", 380f, py, 9f, true)
            py += 20f
        }

        line(760f)
        t("Prepared by Ashraful Hoque • B.SC Maths", 45f, 785f, 9f, true)
        t("The Math Guide • 9732956571", 45f, 802f, 9f)
        center("Computer-generated student progress report", 823f, 8f)

        doc.finishPage(page)
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        val safeName = s.name.replace(Regex("[^A-Za-z0-9_-]"), "_").take(30)
        val file = File(dir, "Progress_${safeName}_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val relevant = when (reportType) {
        "Monthly" -> payments.filter { it.month.equals(selectedPeriod, true) }
        "Yearly" -> payments.filter {
            Regex("\\b(20\\d{2})\\b").find(it.month)?.groupValues?.get(1) == selectedPeriod ||
                it.date.takeLast(4) == selectedPeriod
        }
        else -> payments
    }
    val expected = if (reportType == "Monthly") students.sumOf { it.monthlyFee } else 0
    val collected = relevant.sumOf { it.amount }
    val due = if (reportType == "Monthly") maxOf(0, expected - collected) else 0
    val rate = if (expected > 0) ((collected.toFloat() / expected) * 100).roundToInt() else 0

    t("SUMMARY",45f,150f,13f,true)
    t("Students",60f,178f,10f); t(students.size.toString(),450f,178f,12f,true)
    if (reportType == "Monthly") {
        t("Expected fees",60f,205f,10f); t("₹$expected",450f,205f,12f,true)
        t("Outstanding",60f,232f,10f); t("₹$due",450f,232f,12f,true)
        t("Collection rate",60f,259f,10f); t("$rate%",450f,259f,12f,true)
    }
    t("Collected",60f,286f,10f); t("₹$collected",450f,286f,12f,true)
    t("Payment records",60f,313f,10f); t(relevant.size.toString(),450f,313f,12f,true)
    t("Students with payments",60f,340f,10f); t(relevant.map{it.studentId}.distinct().size.toString(),450f,340f,12f,true)
    line(360f)

    var y=390f
    if (reportType=="Yearly") {
        t("MONTH-WISE COLLECTION",45f,y,13f,true); y+=28f
        relevant.groupBy{it.month}.mapValues{(_,ps)->ps.sumOf{it.amount}}
            .entries.sortedBy{monthKey(it.key)}.take(12).forEach{(m,total)->
                t(m,60f,y,10f); t("₹$total",450f,y,11f,true); y+=23f
            }
    } else if (reportType=="Monthly") {
        t("STUDENT STATUS",45f,y,13f,true); y+=28f
        val paid=students.count{s->payments.filter{it.studentId==s.id&&it.month.equals(selectedPeriod,true)}.sumOf{it.amount}>=s.monthlyFee}
        val partial=students.count{s->val a=payments.filter{it.studentId==s.id&&it.month.equals(selectedPeriod,true)}.sumOf{it.amount};a>0&&a<s.monthlyFee}
        t("Paid students",60f,y,10f);t(paid.toString(),450f,y,11f,true);y+=23f
        t("Partial students",60f,y,10f);t(partial.toString(),450f,y,11f,true);y+=23f
        t("Unpaid students",60f,y,10f);t((students.size-paid-partial).toString(),450f,y,11f,true)
    }

    line(760f)
    t("Prepared by Ashraful Hoque • B.SC Maths",45f,785f,9f,true)
    t("The Math Guide • 9732956571",45f,802f,9f)
    center("Computer-generated report",823f,8f)

    doc.finishPage(page)
    val dir=File(context.cacheDir,"reports").apply{mkdirs()}
    val file=File(dir,"${reportType}_${selectedPeriod.replace(" ","_")}.pdf")
    file.outputStream().use{doc.writeTo(it)}
    doc.close()
    return FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
}

@Composable
fun ReportsDialog(
    students: List<Student>,
    payments: List<Payment>,
    attendance: List<AttendanceRecord>,
    academicRecords: List<AcademicRecord>,
    onDismiss: () -> Unit
) {
    val context=LocalContext.current
    var report by remember{mutableStateOf("Monthly")}
    val months=payments.map{it.month}.distinct().sortedDescending()
    val years=payments.mapNotNull{
        Regex("\\b(20\\d{2})\\b").find(it.month)?.groupValues?.get(1)
            ?:it.date.takeLast(4).takeIf{y->y.length==4}
    }.distinct().sortedDescending()
    var selectedMonth by remember{mutableStateOf(months.firstOrNull()?:currentMonth())}
    var selectedYear by remember{mutableStateOf(years.firstOrNull()?:SimpleDateFormat("yyyy",Locale.getDefault()).format(Date()))}
    var progressStudentId by remember{mutableStateOf(students.firstOrNull()?.id ?: 0L)}
    var shareUri by remember{mutableStateOf<Uri?>(null)}

    AlertDialog(
        onDismissRequest=onDismiss,
        title={Text("Reports",fontWeight=FontWeight.Bold)},
        text={
            Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
                Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    listOf("Monthly","Yearly","Batch","Students","Progress").forEach{r->
                        FilterChip(report==r,{report=r},label={Text(r)})
                    }
                }

                if(report=="Monthly"||report=="Yearly"){
                    val options=if(report=="Monthly")months else years
                    Text(if(report=="Monthly")"Select Month" else "Select Year",fontWeight=FontWeight.Bold)
                    Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)){
                        options.forEach{period->
                            FilterChip(
                                selected=if(report=="Monthly")selectedMonth==period else selectedYear==period,
                                onClick={if(report=="Monthly")selectedMonth=period else selectedYear=period},
                                label={Text(period)}
                            )
                        }
                    }

                    val period=if(report=="Monthly")selectedMonth else selectedYear
                    val relevant=if(report=="Monthly")payments.filter{it.month.equals(selectedMonth,true)}
                    else payments.filter{Regex("\\b(20\\d{2})\\b").find(it.month)?.groupValues?.get(1)==selectedYear||it.date.takeLast(4)==selectedYear}
                    val expected=if(report=="Monthly")students.sumOf{it.monthlyFee}else 0
                    val collected=relevant.sumOf{it.amount}
                    val due=if(report=="Monthly")maxOf(0,expected-collected)else 0
                    val rate=if(expected>0)((collected.toFloat()/expected)*100).roundToInt()else 0

                    Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text("$report Summary",fontWeight=FontWeight.Bold)
                        Text("Students: ${students.size}")
                        if(report=="Monthly"){Text("Expected: ₹$expected");Text("Outstanding: ₹$due");Text("Collection rate: $rate%")}
                        Text("Collected: ₹$collected",fontWeight=FontWeight.Bold)
                        Text("Payment records: ${relevant.size}")
                        Text("Students with payments: ${relevant.map{it.studentId}.distinct().size}")
                    }}

                    if(report=="Yearly"){
                        Text("Month-wise Collection",fontWeight=FontWeight.Bold)
                        relevant.groupBy{it.month}.mapValues{(_,ps)->ps.sumOf{it.amount}}.entries.sortedBy{monthKey(it.key)}.forEach{(month,total)->
                            Card{Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(month);Text("₹$total",fontWeight=FontWeight.Bold)}}
                        }
                    }else{
                        Text("Student Status",fontWeight=FontWeight.Bold)
                        val paid=students.count{s->payments.filter{it.studentId==s.id&&it.month.equals(selectedMonth,true)}.sumOf{it.amount}>=s.monthlyFee}
                        val partial=students.count{s->val a=payments.filter{it.studentId==s.id&&it.month.equals(selectedMonth,true)}.sumOf{it.amount};a>0&&a<s.monthlyFee}
                        Card{Column(Modifier.padding(12.dp)){Text("Paid: $paid");Text("Partial: $partial");Text("Unpaid: ${students.size-paid-partial}")}}
                    }

                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){
                        Button(onClick={shareUri=createReportPdf(context,students,payments,report,period)}){Text("Share PDF")}
                    }
                }else if(report=="Progress"){
                    Text("Student Progress Report",fontWeight=FontWeight.Bold)
                    if (students.isEmpty()) {
                        Text("Add students before creating a progress report.")
                    } else {
                        val progressStudent = students.firstOrNull { it.id == progressStudentId }
                            ?: students.first()
                        var studentMenu by remember { mutableStateOf(false) }

                        Box {
                            OutlinedButton(
                                onClick = { studentMenu = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(progressStudent.name)
                            }
                            DropdownMenu(
                                expanded = studentMenu,
                                onDismissRequest = { studentMenu = false }
                            ) {
                                students.sortedBy { it.name.lowercase() }.forEach { s ->
                                    DropdownMenuItem(
                                        text = { Text("${s.name} • Class ${s.className}") },
                                        onClick = {
                                            progressStudentId = s.id
                                            studentMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        val sa = academicRecords.filter { it.studentId == progressStudent.id && it.maxMarks > 0 }
                        val ar = attendance.filter { it.studentId == progressStudent.id }
                        val present = ar.count { it.status == "PRESENT" }
                        val absent = ar.count { it.status == "ABSENT" }
                        val late = ar.count { it.status == "LATE" }
                        val marked = present + absent + late
                        val attendanceRate = if (marked > 0) ((present.toFloat()/marked)*100).roundToInt() else 0
                        val academicRate = if (sa.isNotEmpty()) {
                            ((sa.sumOf { it.marks } / sa.sumOf { it.maxMarks }) * 100).roundToInt()
                        } else 0

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DashboardCard("Attendance", "$attendanceRate%", Modifier.weight(1f))
                            DashboardCard("Academic", "$academicRate%", Modifier.weight(1f))
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DashboardCard("Present", present.toString(), Modifier.weight(1f))
                            DashboardCard("Tests", sa.size.toString(), Modifier.weight(1f))
                        }

                        Text("Recent Results", fontWeight=FontWeight.Bold)
                        if (sa.isEmpty()) {
                            Text("No academic results recorded for this student.")
                        } else {
                            sa.sortedByDescending { it.date }.take(5).forEach { r ->
                                val pct = ((r.marks / r.maxMarks) * 100).roundToInt()
                                Card(Modifier.fillMaxWidth()) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement=Arrangement.SpaceBetween
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "${r.subject.ifBlank{"Subject"}} • ${r.title.ifBlank{"Test"}}",
                                                fontWeight=FontWeight.Bold
                                            )
                                            Text(r.date, style=MaterialTheme.typography.labelSmall)
                                        }
                                        Text(
                                            "${r.marks.toCleanNumber()}/${r.maxMarks.toCleanNumber()} ($pct%)",
                                            fontWeight=FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                shareUri = createReportPdf(
                                    context, students, payments, "Progress",
                                    progressStudent.name, attendance, academicRecords, progressStudent
                                )
                            },
                            modifier=Modifier.fillMaxWidth()
                        ) { Text("Create & Share Progress PDF") }
                    }
                }else if(report=="Batch"){
                    Text("Batch-wise Collection",fontWeight=FontWeight.Bold)
                    students.map{it.batch}.filter{it.isNotBlank()}.distinct().sorted().forEach{batch->
                        val bs=students.filter{it.batch==batch}
                        val total=payments.filter{it.studentId in bs.map{it.id}}.sumOf{it.amount}
                        Card{Column(Modifier.padding(12.dp)){Text("Batch $batch",fontWeight=FontWeight.Bold);Text("Students: ${bs.size}");Text("Collected: ₹$total")}}
                    }
                }else{
                    Text("Student Payment History",fontWeight=FontWeight.Bold)
                    students.sortedBy{it.name.lowercase()}.forEach{s->
                        val ps=payments.filter{it.studentId==s.id}
                        Card{Column(Modifier.padding(12.dp)){Text(s.name,fontWeight=FontWeight.Bold);Text("${s.className} • ${s.batch}");Text("Joining Month: ${s.joiningMonth.ifBlank{"Not provided"}}");Text("Monthly fee: ₹${s.monthlyFee}");Text("Total paid: ₹${ps.sumOf{it.amount}}");Text("Payments: ${ps.size}")}}
                    }
                }
            }
        },
        confirmButton={TextButton(onDismiss){Text("Close")}}
    )

    shareUri?.let{uri->
        AlertDialog(
            onDismissRequest={shareUri=null},
            title={Text("Share Report")},
            text={Text("Your $report report is ready.")},
            confirmButton={Button(onClick={shareReceipt(context,uri);shareUri=null}){Text("Share")}},
            dismissButton={TextButton(onClick={shareUri=null}){Text("Cancel")}}
        )
    }
}
