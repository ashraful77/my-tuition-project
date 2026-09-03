package com.themathguild.mytuitionmanager

import com.themathguild.mytuitionmanager.receipts.*

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.Settings
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

data class Batch(
    val id: Long, val name: String, val address: String = ""
)

data class LibraryItem(
    val id: Long, val name: String, val uri: String, val category: String = "General", val addedDate: String = ""
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

    fun securityPin(): String {
        val saved = prefs.getString("securityPin", "") ?: ""
        return if (saved.matches(Regex("\\d{4}"))) saved else "1234"
    }

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
    fun loadBatches(): List<Batch> {
        val a = JSONArray(prefs.getString("batches", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Batch(o.getLong("id"), o.optString("name"), o.optString("address"))
        }
    }

    fun saveBatches(list: List<Batch>) {
        val a = JSONArray()
        list.forEach { b -> a.put(JSONObject().apply { put("id", b.id); put("name", b.name); put("address", b.address) }) }
        prefs.edit().putString("batches", a.toString()).apply()
    }

    fun loadLibrary(): List<LibraryItem> {
        val a = JSONArray(prefs.getString("library", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            LibraryItem(o.getLong("id"), o.optString("name"), o.optString("uri"), o.optString("category", "General"), o.optString("addedDate"))
        }
    }

    fun saveLibrary(list: List<LibraryItem>) {
        val a = JSONArray()
        list.forEach { f -> a.put(JSONObject().apply { put("id", f.id); put("name", f.name); put("uri", f.uri); put("category", f.category); put("addedDate", f.addedDate) }) }
        prefs.edit().putString("library", a.toString()).apply()
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
        val s = students.optJSONObject(i) ?: return "Invalid student data."
        val id = s.optLong("id", Long.MIN_VALUE)
        if (id == Long.MIN_VALUE || !studentIds.add(id)) return "Invalid or duplicate student ID."
        if (s.optString("name").isBlank()) return "A student has no name."
    }

    for (i in 0 until payments.length()) {
        val p = payments.optJSONObject(i) ?: return "Invalid payment data."
        if (!studentIds.contains(p.optLong("studentId", Long.MIN_VALUE))) return "Payment refers to an unknown student."
        if (p.optString("month").isBlank()) return "A payment has no month."
        if (p.optInt("amount", -1) <= 0) return "A payment has an invalid amount."
    }

    for (i in 0 until attendance.length()) {
        val a = attendance.optJSONObject(i) ?: return "Invalid attendance data."
        if (!studentIds.contains(a.optLong("studentId", Long.MIN_VALUE))) return "Attendance refers to an unknown student."
    }

    for (i in 0 until academic.length()) {
        val a = academic.optJSONObject(i) ?: return "Invalid academic record data."
        if (!studentIds.contains(a.optLong("studentId", Long.MIN_VALUE))) return "Academic record refers to an unknown student."
    }
    return null
}

fun exportBackup(context: Context): Uri {
    val store = LocalStore(context)
    val obj = JSONObject().apply {
        put("app", "The Math Guide")
        put("backupFormat", BACKUP_FORMAT_VERSION)
        put("exportedAt", currentDate())
        put("students", JSONArray().apply { store.loadStudents().forEach { s -> put(JSONObject().apply { put("id",s.id);put("name",s.name);put("className",s.className);put("batch",s.batch);put("monthlyFee",s.monthlyFee);put("phone",s.phone);put("joiningMonth",s.joiningMonth);put("school",s.school);put("fatherName",s.fatherName);put("motherName",s.motherName);put("address",s.address);put("subjects",s.subjects);put("notes",s.notes);put("status",s.status) }) } })
        put("payments", JSONArray().apply { store.loadPayments().forEach { p -> put(JSONObject().apply { put("studentId",p.studentId);put("month",p.month);put("amount",p.amount);put("date",p.date);put("receiptNo",p.receiptNo) }) } })
        put("attendance", JSONArray().apply { store.loadAttendance().forEach { a -> put(JSONObject().apply { put("studentId",a.studentId);put("date",a.date);put("status",a.status) }) } })
        put("academicRecords", JSONArray().apply { store.loadAcademicRecords().forEach { a -> put(JSONObject().apply { put("id",a.id);put("studentId",a.studentId);put("date",a.date);put("subject",a.subject);put("title",a.title);put("marks",a.marks);put("maxMarks",a.maxMarks);put("remarks",a.remarks) }) } })
        put("batchRoutines", JSONArray().apply { store.loadBatchRoutines().forEach { r -> put(JSONObject().apply { put("id",r.id);put("batch",r.batch);put("day",r.day);put("start",r.start);put("end",r.end) }) } })
        put("batchNotes", JSONArray().apply { store.loadBatchNotes().forEach { n -> put(JSONObject().apply { put("id",n.id);put("batch",n.batch);put("date",n.date);put("type",n.type);put("text",n.text) }) } })
        put("batches", JSONArray().apply { store.loadBatches().forEach { b -> put(JSONObject().apply { put("id",b.id);put("name",b.name);put("address",b.address) }) } })
        put("library", JSONArray().apply { store.loadLibrary().forEach { f -> put(JSONObject().apply { put("id",f.id);put("name",f.name);put("uri",f.uri);put("category",f.category);put("addedDate",f.addedDate) }) } })
        put("tuitionProfile", JSONObject().apply { val p=store.loadTuitionProfile();put("tuitionName",p.tuitionName);put("tagline",p.tagline);put("teacherName",p.teacherName);put("qualification",p.qualification);put("phone",p.phone);put("address",p.address) })
    }
    val file=File(context.cacheDir,"the_math_guide_backup_${SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(Date())}.json")
    file.writeText(obj.toString(2))
    return FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
}

fun importBackup(context: Context, uri: Uri): Boolean {
    return try {
        val json=context.contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()} ?: return false
        val obj=JSONObject(json)
        if (validateBackupData(obj) != null) return false
        val students=obj.optJSONArray("students") ?: return false
        val payments=obj.optJSONArray("payments") ?: return false
        val attendance=obj.optJSONArray("attendance") ?: JSONArray()
        val academicRecords=obj.optJSONArray("academicRecords") ?: JSONArray()
        val batchRoutines=obj.optJSONArray("batchRoutines") ?: JSONArray()
        val batchNotes=obj.optJSONArray("batchNotes") ?: JSONArray()
        val batches=obj.optJSONArray("batches") ?: JSONArray()
        val library=obj.optJSONArray("library") ?: JSONArray()
        val prefs=context.getSharedPreferences("tuition_data",Context.MODE_PRIVATE)
        val editor=prefs.edit().putString("students",students.toString()).putString("payments",payments.toString()).putString("attendance",attendance.toString()).putString("academicRecords",academicRecords.toString()).putString("batchRoutines",batchRoutines.toString()).putString("batchNotes",batchNotes.toString()).putString("batches",batches.toString()).putString("library",library.toString())
        obj.optJSONObject("tuitionProfile")?.let{p->editor.putString("tuitionName",p.optString("tuitionName","The Math Guide")).putString("tuitionTagline",p.optString("tagline","Tuition & Academic Support")).putString("teacherName",p.optString("teacherName","Ashraful Hoque")).putString("qualification",p.optString("qualification","B.SC Maths")).putString("tuitionPhone",p.optString("phone","9732956571")).putString("tuitionAddress",p.optString("address","Jatarpur, Murshidabad, West Bengal, 742147"))}
        editor.apply(); true
    } catch(_:Exception){false}
}

class MainActivity : ComponentActivity() {
    private var restoreCallback: ((Boolean) -> Unit)? = null
    private val restoreLauncher=registerForActivityResult(ActivityResultContracts.OpenDocument()){uri->val ok=uri?.let{importBackup(this,it)}?:false;restoreCallback?.invoke(ok);restoreCallback=null}
    fun pickBackup(callback:(Boolean)->Unit){restoreCallback=callback;restoreLauncher.launch(arrayOf("application/json","text/*"))}
    override fun onCreate(savedInstanceState: Bundle?){super.onCreate(savedInstanceState);setContent{TuitionApp(LocalStore(this))}}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuitionApp(store: LocalStore) {
    val context = LocalContext.current
    var students by remember { mutableStateOf(store.loadStudents()) }
    var payments by remember { mutableStateOf(store.loadPayments()) }
    var attendance by remember { mutableStateOf(store.loadAttendance()) }
    var academicRecords by remember { mutableStateOf(store.loadAcademicRecords()) }
    var tuitionProfile by remember { mutableStateOf(store.loadTuitionProfile()) }
    var routines by remember { mutableStateOf(store.loadBatchRoutines()) }
    var batchNotes by remember { mutableStateOf(store.loadBatchNotes()) }
    var batches by remember { mutableStateOf(store.loadBatches()) }
    var libraryItems by remember { mutableStateOf(store.loadLibrary()) }
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
    var manageStudentsOpen by remember { mutableStateOf(false) }
    var tuitionProfileOpen by remember { mutableStateOf(false) }
    var themeOpen by remember { mutableStateOf(false) }
    var securityControlsOpen by remember { mutableStateOf(false) }
    var todayWorkOpen by remember { mutableStateOf(false) }
    var liveBatchOpen by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }
    var demoToolsOpen by remember { mutableStateOf(false) }
    var restoreConfirmOpen by remember { mutableStateOf(false) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }
    var shareUri by remember { mutableStateOf<Uri?>(null) }
    var shareStudent by remember { mutableStateOf<Student?>(null) }
    var sharePayments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var pendingReceiptNo by remember { mutableStateOf("") }
    var selectedReceipt by remember { mutableStateOf<Student?>(null) }
    var selectedReceiptPayments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var selectedReceiptNo by remember { mutableStateOf("") }
    var selectedBottomTab by remember { mutableStateOf(0) }
    var themeMode by remember { mutableStateOf(store.themeMode()) }
    var accentName by remember { mutableStateOf(store.accentName()) }

    val batchesFromStudents = students.map { it.batch }.filter { it.isNotBlank() }.distinct().sorted()
    val allBatchNames = (batches.map { it.name } + batchesFromStudents).distinct().sorted()
    val filteredStudents = students.filter { s ->
        val matchesSearch = search.isBlank() || s.name.contains(search,true) || s.phone.contains(search,true) || s.className.contains(search,true)
        val matchesBatch = selectedBatch == "All" || s.batch == selectedBatch
        val paid = payments.filter { it.studentId == s.id }.filter { monthKey(it.month) == YearMonth.now() }.sumOf { it.amount }
        val due = s.monthlyFee - paid
        val matchesPayment = when(paymentFilter){"Paid"->due<=0;"Due"->due>0;else->true}
        matchesSearch && matchesBatch && matchesPayment
    }

    MaterialTheme {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selectedBottomTab==0,{selectedBottomTab=0},icon={},label={Text("Home")})
                    NavigationBarItem(selectedBottomTab==1,{selectedBottomTab=1},icon={},label={Text("Students")})
                    NavigationBarItem(selectedBottomTab==2,{selectedBottomTab=2},icon={},label={Text("Live Batch")})
                    NavigationBarItem(selectedBottomTab==3,{selectedBottomTab=3},icon={},label={Text("Academic")})
                    NavigationBarItem(selectedBottomTab==4,{selectedBottomTab=4},icon={},label={Text("Settings")})
                }
            }
        ) { padding ->
            Column(Modifier.padding(padding).fillMaxSize()) {
                when(selectedBottomTab){
                    0 -> HomeContent(students, payments, filteredStudents, search, {search=it}, selectedBatch, {selectedBatch=it}, paymentFilter, {paymentFilter=it}, allBatchNames, {selectedStudent=it}, {collectStudent=it}, {settingsOpen=true})
                    1 -> StudentsContent(filteredStudents,{selectedStudent=it},{collectStudent=it})
                    2 -> LiveBatchDialog(routines,batchNotes,{selectedBottomTab=0},{updated->routines=updated;store.saveBatchRoutines(updated)},{updated->batchNotes=updated;store.saveBatchNotes(updated)})
                    3 -> AcademicScreen(students, academicRecords, {academicRecords=it;store.saveAcademicRecords(it)})
                    4 -> SettingsScreen({settingsOpen=true},{tuitionProfileOpen=true},{themeOpen=true},{securityControlsOpen=true},{todayWorkOpen=true},{libraryOpen=true},{restoreConfirmOpen=true},{demoToolsOpen=true})
                }
            }
        }

        if(settingsOpen) SettingsDialog(
            onDismiss={settingsOpen=false},
            onAdd={settingsOpen=false;addOpen=true},
            onManage={manageStudentsOpen=true},
            onReports={settingsOpen=false;reportOpen=true},
            onReceiptHistory={settingsOpen=false;receiptHistoryOpen=true},
            onTuitionProfile={settingsOpen=false;tuitionProfileOpen=true},
            onBackup={shareReceipt(context,exportBackup(context))},
            onRestore={restoreConfirmOpen=true}
        )
        if(manageStudentsOpen) ManageStudentsDialog(students,{manageStudentsOpen=false},{s->manageStudentsOpen=false;settingsOpen=false;editStudent=s},{s->students=students.filter{it.id!=s.id};payments=payments.filter{it.studentId!=s.id};store.saveStudents(students);store.savePayments(payments)})
        if(addOpen) StudentEditorDialog("Add Student",null,{addOpen=false}){s->students=students+s;store.saveStudents(students);addOpen=false}
        editStudent?.let{s->StudentEditorDialog("Edit Student",s,{editStudent=null}){u->students=students.map{if(it.id==u.id)u else it};store.saveStudents(students);editStudent=null}}
        selectedStudent?.let{s->StudentDetailsDialog(s,payments.filter{it.studentId==s.id},outstanding(s),{selectedStudent=null},{collectStudent=s;selectedStudent=null})}
        collectStudent?.let{s->CollectFeeDialog(s,{collectStudent=null}){month,amount->
            val payment=Payment(s.id,month,amount,currentDate())
            val newPayments=payments+payment;payments=newPayments;store.savePayments(newPayments)
            val receiptNo="TMG-${SimpleDateFormat("yyyy",Locale.getDefault()).format(Date())}-${newPayments.size.toString().padStart(5,'0')}"
            receiptUri=createReceiptPdf(context,s,newPayments.filter{it.studentId==s.id},receiptNo,tuitionProfile);collectStudent=null
        }}
        receiptUri?.let{uri->AlertDialog(onDismissRequest={receiptUri=null},title={Text("Payment Saved ✓")},text={Text("Your professional PDF receipt has been created.")},confirmButton={Button(onClick={shareReceipt(context,uri);receiptUri=null}){Text("Share PDF")}},dismissButton={TextButton(onClick={receiptUri=null}){Text("Close")}})}
        if(reportOpen) ReportsDialog(students,payments,{reportOpen=false})
        if(receiptHistoryOpen) ReceiptHistoryDialog(students,payments,{receiptHistoryOpen=false},{student,ps,no->selectedReceipt=student;selectedReceiptPayments=ps;selectedReceiptNo=no;receiptHistoryOpen=false})
        selectedReceipt?.let{student->
            AlertDialog(onDismissRequest={selectedReceipt=null},title={Text("Payment Receipt")},text={Text(paymentReceiptText(tuitionProfile,student,selectedReceiptPayments,selectedReceiptNo))},confirmButton={},dismissButton={Row{TextButton(onClick={shareReceiptText(context,student,selectedReceiptPayments,selectedReceiptNo,tuitionProfile,"WhatsApp");selectedReceipt=null}){Text("WhatsApp")};TextButton(onClick={shareReceiptText(context,student,selectedReceiptPayments,selectedReceiptNo,tuitionProfile,"SMS");selectedReceipt=null}){Text("SMS")};TextButton(onClick={shareReceipt(context,createReceiptPdf(context,student,selectedReceiptPayments,selectedReceiptNo,tuitionProfile));selectedReceipt=null}){Text("PDF / Other")}}})
        }
        if(tuitionProfileOpen) TuitionProfileDialog(tuitionProfile,{tuitionProfileOpen=false}){tuitionProfile=it;store.saveTuitionProfile(it);tuitionProfileOpen=false}
        if(themeOpen) ThemeDialog(themeMode,accentName,{themeOpen=false}){m,a->themeMode=m;accentName=a;store.saveTheme(m,a)}
        if(securityControlsOpen) SecurityControlsDialog(store){securityControlsOpen=false}
        if(todayWorkOpen) TodayWorkDialog(routines,batchNotes,{todayWorkOpen=false},{selectedBottomTab=2;todayWorkOpen=false})
        if(liveBatchOpen) LiveBatchDialog(routines,batchNotes,{liveBatchOpen=false},{updated->routines=updated;store.saveBatchRoutines(updated)},{updated->batchNotes=updated;store.saveBatchNotes(updated)})
        if(libraryOpen) LibraryDialog(libraryItems,{libraryOpen=false},{(context as? MainActivity)?.pickLibraryFile{uri->if(uri!=null){val name=uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.ifBlank{"Library file"}?:"Library file";val item=LibraryItem(System.currentTimeMillis(),name,uri.toString(),"General",currentDate());libraryItems=libraryItems+item;store.saveLibrary(libraryItems)}}},{item->libraryItems=libraryItems.filterNot{it.id==item.id};store.saveLibrary(libraryItems)})
        if(restoreConfirmOpen) AlertDialog(onDismissRequest={restoreConfirmOpen=false},title={Text("Restore backup?")},text={Text("Restoring will replace the students, payment history and tuition profile currently stored on this phone. For safety, make a fresh backup before continuing.")},confirmButton={Button(onClick={restoreConfirmOpen=false;(context as? MainActivity)?.pickBackup{ok->if(ok){students=store.loadStudents();payments=store.loadPayments();attendance=store.loadAttendance();academicRecords=store.loadAcademicRecords();tuitionProfile=store.loadTuitionProfile();routines=store.loadBatchRoutines();batchNotes=store.loadBatchNotes();batches=store.loadBatches();libraryItems=store.loadLibrary()};Toast.makeText(context,if(ok)"Backup restored successfully." else "Restore failed. No data was changed.",Toast.LENGTH_LONG).show()}}){Text("Choose Backup")}},dismissButton={TextButton(onClick={restoreConfirmOpen=false}){Text("Cancel")}})
        if(demoToolsOpen) DemoToolsDialog(students,{demoToolsOpen=false},{demo->students=students+demo;store.saveStudents(students);demoToolsOpen=false},{val ids=students.filter{it.id<0L}.map{it.id}.toSet();students=students.filterNot{it.id in ids};payments=payments.filterNot{it.studentId in ids};store.saveStudents(students);store.savePayments(payments);demoToolsOpen=false})
    }
}

@Composable fun ThemeDialog(mode:String,accent:String,onDismiss:()->Unit,onSave:(String,String)->Unit){var m by remember{mutableStateOf(mode)};var a by remember{mutableStateOf(accent)};AlertDialog(onDismissRequest=onDismiss,title={Text("Theme & Colors")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text("Theme",fontWeight=FontWeight.Bold);listOf("System","Light","Dark").forEach{x->FilterChip(m==x,{m=x},label={Text(x)})};Text("Accent color",fontWeight=FontWeight.Bold);Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)){listOf("Blue","Green","Purple","Orange","Red","Teal").forEach{x->FilterChip(a==x,{a=x},label={Text(x)})}}}},confirmButton={Button(onClick={onSave(m,a);onDismiss()}){Text("Apply")}},dismissButton={TextButton(onClick=onDismiss){Text("Cancel")}})}

@Composable fun SecurityControlsDialog(store:LocalStore,onDismiss:()->Unit){val items=listOf("payment" to "Make Payment","addStudent" to "Add Student","editStudent" to "Edit Student","deleteStudent" to "Delete Student","editPayment" to "Edit Payment","deletePayment" to "Delete Payment","attendance" to "Attendance","academic" to "Academic Records","profile" to "Tuition Profile","demo" to "Demo / Test Data","restore" to "Restore Backup");AlertDialog(onDismissRequest=onDismiss,title={Text("Protected Actions")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(5.dp)){Text("Choose which actions require your 4-digit PIN.",style=MaterialTheme.typography.bodySmall);items.forEach{(key,label)->var checked by remember{mutableStateOf(store.isProtected(key))};Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){Text(label,Modifier.weight(1f));Switch(checked,{checked=it;store.setProtected(key,it)})}}}},confirmButton={TextButton(onClick=onDismiss){Text("Done")}})}

@Composable fun TodayWorkDialog(routines:List<BatchRoutine>,notes:List<BatchNote>,onDismiss:()->Unit,onLive:()->Unit){val day=SimpleDateFormat("EEEE",Locale.getDefault()).format(Date());val rs=routines.filter{it.day.equals(day,true)};AlertDialog(onDismissRequest=onDismiss,title={Text("Today’s Work",fontWeight=FontWeight.Bold)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){if(rs.isEmpty())Text("No tuition routine scheduled today.") else rs.forEach{r->Text("${r.batch} • ${r.start}–${r.end}",fontWeight=FontWeight.Bold)};notes.filter{it.date==currentDate()}.forEach{n->Text("${n.type}: ${n.text}")}}},confirmButton={Button(onClick=onLive){Text("Live Batch")}},dismissButton={TextButton(onClick=onDismiss){Text("Close")}})}
