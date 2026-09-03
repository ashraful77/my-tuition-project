package com.themathguild.mytuitionmanager

import com.themathguild.mytuitionmanager.attendance.AttendanceDialog

import com.themathguild.mytuitionmanager.data.LocalStore

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
import com.themathguild.mytuitionmanager.components.StudentCard

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
    val batches = JSONArray(prefs.getString("batches", "[]") ?: "[]")
    val library = JSONArray(prefs.getString("library", "[]") ?: "[]")

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
        put("batches", batches)
        put("library", library)
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
        val batches = obj.optJSONArray("batches") ?: JSONArray()
        val library = obj.optJSONArray("library") ?: JSONArray()

        if (validateBackupData(obj) != null) return false

        val prefs = context.getSharedPreferences("tuition_data", Context.MODE_PRIVATE)
        val editor = prefs.edit()
            .putString("students", students.toString())
            .putString("payments", payments.toString())
            .putString("attendance", attendance.toString())
            .putString("academicRecords", academicRecords.toString())
            .putString("batchRoutines", batchRoutines.toString())
            .putString("batchNotes", batchNotes.toString())
            .putString("batches", batches.toString())
            .putString("library", library.toString())

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

    private var libraryCallback: ((Uri?) -> Unit)? = null

    private val libraryLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let {
                try { contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
            }
            libraryCallback?.invoke(uri)
            libraryCallback = null
        }

    fun pickBackup(callback: (Boolean) -> Unit) {
        restoreCallback = callback
        restoreLauncher.launch(arrayOf("application/json", "text/*"))
    }

    fun pickLibraryFile(callback: (Uri?) -> Unit) {
        libraryCallback = callback
        libraryLauncher.launch(arrayOf("application/pdf", "text/plain", "image/*", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "application/octet-stream"))
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
    var batchList by remember { mutableStateOf(store.loadBatches()) }
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
    var tuitionProfileOpen by remember { mutableStateOf(false) }
    var dashboardExpanded by remember { mutableStateOf(true) }
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
    var pendingReceiptStudent by remember { mutableStateOf<Student?>(null) }
    var pendingReceiptPayments by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var pendingReceiptNo by remember { mutableStateOf("") }
    var selectedBottomTab by remember { mutableStateOf(0) }
    var securityPinOpen by remember { mutableStateOf(false) }
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
    var batchManagerOpen by remember { mutableStateOf(false) }
    var libraryOpen by remember { mutableStateOf(false) }

    fun navigateToTab(tab: Int) {
        selectedBottomTab = tab
        manageStudentsOpen = false
        liveBatchOpen = false
        receiptHistoryOpen = false
        settingsOpen = false
        attendanceOpen = false
        academicOpen = false
        reportOpen = false
        tuitionProfileOpen = false
        demoToolsOpen = false
        batchManagerOpen = false
        libraryOpen = false
    }

    fun requireSecurityPin(title: String, action: () -> Unit) {
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
    val batches = listOf("All") + (batchList.map { it.name } + students.map { it.batch }).filter { it.isNotBlank() }.distinct().sorted()
    val classes = listOf("All") + students.map { it.className }.filter { it.isNotBlank() }.distinct().sorted()
    val visibleStudents = students.filter {
        it.status.equals("Active", true) &&
        (it.name.contains(search, true) || it.className.contains(search, true) || it.batch.contains(search, true) || it.phone.contains(search, true))
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
                                Text(tuitionProfile.tuitionName, fontWeight = FontWeight.Bold)
                                Text("My Tuition Manager", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(selected = selectedBottomTab == 0, onClick = { navigateToTab(0) }, icon = { Text("⌂") }, label = { Text("Home") })
                    NavigationBarItem(selected = selectedBottomTab == 1, onClick = { navigateToTab(1) }, icon = { Text("☷") }, label = { Text("Students") })
                    NavigationBarItem(selected = selectedBottomTab == 2, onClick = { navigateToTab(2) }, icon = { Text("🎓") }, label = { Text("Live Batch") })
                    NavigationBarItem(selected = selectedBottomTab == 3, onClick = { navigateToTab(3) }, icon = { Text("₹") }, label = { Text("Payments") })
                    NavigationBarItem(selected = selectedBottomTab == 4, onClick = { navigateToTab(4) }, icon = { Text("⚙") }, label = { Text("Settings") })
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                if (selectedBottomTab == 0) {
            LazyColumn(
                Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 10.dp),
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
                                    Text(tuitionProfile.teacherName, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${tuitionProfile.qualification} • ${tuitionProfile.tuitionName}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
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
                                    Text("This Month", fontWeight = FontWeight.Bold)
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
                                    DashboardCard("Students", students.size.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer) { paymentFilter = "All" }
                                    DashboardCard("Collected", money(totalCollected), Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer) { paymentFilter = "Paid" }
                                    DashboardCard("Due", money(totalOutstanding), Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer) { paymentFilter = "Due" }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    DashboardCard("Paid", "$paidStudents / ${students.size}", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer) { paymentFilter = "Paid" }
                                    DashboardCard("Partial", partialStudents.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer) { paymentFilter = "Partial" }
                                    DashboardCard("Unpaid", unpaidStudents.toString(), Modifier.weight(1f), MaterialTheme.colorScheme.errorContainer) { paymentFilter = "Unpaid" }
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
                                    modifier = Modifier.fillMaxWidth().height(6.dp)
                                )
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
            }
        }

        if (selectedBottomTab == 4) SettingsDialog(
            onDismiss = { selectedBottomTab = 0 },
            onAdd = { selectedBottomTab = 0; guarded("addStudent", "Add Student") { addOpen = true } },
            onManage = { selectedBottomTab = 1 },
            onAttendance = { selectedBottomTab = 0; guarded("attendance", "Attendance") { attendanceOpen = true } },
            onAcademic = { selectedBottomTab = 0; guarded("academic", "Academic Records") { academicOpen = true } },
            onReports = { selectedBottomTab = 0; reportOpen = true },
            onReceiptHistory = { selectedBottomTab = 3 },
            onTuitionProfile = { selectedBottomTab = 0; guarded("profile", "Tuition Profile") { tuitionProfileOpen = true } },
            onDemoTools = { selectedBottomTab = 0; guarded("demo", "Demo / Test Data") { demoToolsOpen = true } },
            onSecurityPin = { selectedBottomTab = 0; requireSecurityPin("Change Security PIN") { changePinOpen = true } },
            onTheme = { selectedBottomTab = 0; themeOpen = true },
            onSecurityControls = { selectedBottomTab = 0; securityControlsOpen = true },
            onBatches = { selectedBottomTab = 0; batchManagerOpen = true },
            onLibrary = { selectedBottomTab = 0; libraryOpen = true },
            lastBackupDate = lastBackupDate,
            onBackup = {
                shareReceipt(context, exportBackup(context))
                lastBackupDate = store.lastBackupDate()
            },
            onRestore = { guarded("restore", "Restore Backup") { restoreConfirmOpen = true } }
        )

        if (selectedBottomTab == 1) ManageStudentsDialog(
            students = students,
            payments = payments,
            attendance = attendance,
            outstandingFor = { outstanding(it) },
            onDismiss = { selectedBottomTab = 0 },
            onOpenProfile = { s -> selectedBottomTab = 0; selectedStudent = s },
            onCollect = { s -> guarded("payment", "Make Payment") { selectedBottomTab = 0; collectStudent = s } },
            onEdit = { s -> guarded("editStudent", "Edit Student") { selectedBottomTab = 0; editStudent = s } },
            onDelete = { s -> guarded("deleteStudent", "Delete Student") {
                students = students.filter { it.id != s.id }
                payments = payments.filter { it.studentId != s.id }
                attendance = attendance.filter { it.studentId != s.id }
                academicRecords = academicRecords.filter { it.studentId != s.id }
                store.saveStudents(students)
                store.savePayments(payments)
                store.saveAttendance(attendance)
                store.saveAcademicRecords(academicRecords)
            } }
        )

        if (addOpen) StudentEditorDialog("Add Student", null, batchList, { addOpen = false }) { s -> students = students + s; store.saveStudents(students); addOpen = false }
        editStudent?.let { s -> StudentEditorDialog("Edit Student", s, batchList, { editStudent = null }) { u -> students = students.map { if (it.id == u.id) u else it }; store.saveStudents(students); editStudent = null } }
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
                // Keep the exact receipt that was just created for all sharing
                // actions. Do not reconstruct it from payments.lastOrNull().
                pendingReceiptStudent = s
                pendingReceiptPayments = newPayments
                pendingReceiptNo = receiptNo
                receiptUri = createReceiptPdf(context, s, newPayments, receiptNo, tuitionProfile)
                collectStudent = null
            }
        }
        receiptUri?.let { uri ->
            val shareStudent = pendingReceiptStudent
            val sharePayments = pendingReceiptPayments
            val shareReceiptNo = pendingReceiptNo

            fun closeReceiptShare() {
                receiptUri = null
                pendingReceiptStudent = null
                pendingReceiptPayments = emptyList()
                pendingReceiptNo = ""
            }

            AlertDialog(
                onDismissRequest = { closeReceiptShare() },
                title = { Text("Payment Saved ✓") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Professional PDF receipt is ready. Choose a sending method.")
                        Text(
                            "WhatsApp / SMS sends only the payment receipt text. PDF remains available through Share PDF.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Button(
                        enabled = shareStudent != null && sharePayments.isNotEmpty() && shareReceiptNo.isNotBlank(),
                        onClick = {
                            val st = shareStudent
                            if (st != null) {
                                shareReceiptText(
                                    context,
                                    st,
                                    sharePayments,
                                    shareReceiptNo,
                                    tuitionProfile,
                                    "WhatsApp"
                                )
                            }
                            closeReceiptShare()
                        }
                    ) { Text("WhatsApp") }
                },
                dismissButton = {
                    Row {
                        TextButton(
                            enabled = shareStudent != null && sharePayments.isNotEmpty() && shareReceiptNo.isNotBlank(),
                            onClick = {
                                val st = shareStudent
                                if (st != null) {
                                    shareReceiptText(
                                        context,
                                        st,
                                        sharePayments,
                                        shareReceiptNo,
                                        tuitionProfile,
                                        "SMS"
                                    )
                                }
                                closeReceiptShare()
                            }
                        ) { Text("SMS") }

                        TextButton(onClick = {
                            shareReceipt(context, uri)
                            closeReceiptShare()
                        }) { Text("PDF / Other") }
                    }
                }
            )
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
        if (selectedBottomTab == 3) {
            ReceiptHistoryDialog(
                students = students,
                payments = payments,
                profile = tuitionProfile,
                onDismiss = { selectedBottomTab = 0 },
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
                title = { Text("Delete payment?") },
                text = {
                    Text(
                        "This will remove ${payment.month} (₹${payment.amount}) from receipt ${payment.receiptNo}. " +
                            "This action cannot be undone unless you restore a backup."
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        payments = payments.filterNot { it === payment }
                        store.savePayments(payments)
                        deletePaymentConfirm = null
                        Toast.makeText(context, "Payment deleted.", Toast.LENGTH_SHORT).show()
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
                                batchList = store.loadBatches()
                                libraryItems = store.loadLibrary()
                                routines = store.loadBatchRoutines()
                                batchNotes = store.loadBatchNotes()
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
        if (batchManagerOpen) BatchManagerDialog(
            batches = batchList,
            routines = routines,
            onDismiss = { batchManagerOpen = false },
            onSaveBatches = { batchList = it; store.saveBatches(it) },
            onSaveRoutines = { routines = it; store.saveBatchRoutines(it) },
            onRenameBatch = { oldName, newName ->
                students = students.map { if (it.batch == oldName) it.copy(batch = newName) else it }
                store.saveStudents(students)
                routines = routines.map { if (it.batch == oldName) it.copy(batch = newName) else it }
                store.saveBatchRoutines(routines)
                batchNotes = batchNotes.map { if (it.batch == oldName) it.copy(batch = newName) else it }
                store.saveBatchNotes(batchNotes)
            }
        )
        if (libraryOpen) LibraryDialog(
            items = libraryItems,
            onDismiss = { libraryOpen = false },
            onAdd = { (context as? MainActivity)?.pickLibraryFile { uri ->
                if (uri != null) {
                    val name = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.ifBlank { "Library file" } ?: "Library file"
                    val item = LibraryItem(System.currentTimeMillis(), name, uri.toString(), "General", currentDate())
                    libraryItems = libraryItems + item
                    store.saveLibrary(libraryItems)
                }
            } },
            onDelete = { item -> libraryItems = libraryItems.filterNot { it.id == item.id }; store.saveLibrary(libraryItems) }
        )
        if (selectedBottomTab == 2) LiveBatchDialog(routines, batchNotes, { selectedBottomTab=0 }, { updated -> batchNotes=updated; store.saveBatchNotes(updated) })
            }
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

        if (todayWorkOpen) TodayWorkDialog(routines, batchNotes, { todayWorkOpen=false }, { selectedBottomTab=2; todayWorkOpen=false })

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
private fun FullScreenPage(
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismissRequest) {
                    Text("‹", style = MaterialTheme.typography.headlineMedium)
                }
                Box(Modifier.weight(1f)) {
                    title()
                }
                if (dismissButton != null) dismissButton()
                confirmButton()
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) { text() }
            }
        }
    }
}

@Composable
fun LiveBatchDialog(
    routines: List<BatchRoutine>,
    notes: List<BatchNote>,
    onDismiss: () -> Unit,
    onNotes: (List<BatchNote>) -> Unit
) {
    val batches = routines.map { it.batch }.filter { it.isNotBlank() }.distinct().sorted()
    val dayNow = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
    val timeNow = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val auto = routines.firstOrNull {
        it.day.equals(dayNow, true) && timeNow >= it.start && timeNow <= it.end
    }?.batch
    var batch by remember { mutableStateOf(auto ?: batches.firstOrNull().orEmpty()) }
    var type by remember { mutableStateOf("Homework") }
    var text by remember { mutableStateOf("") }

    FullScreenPage(
        onDismissRequest = onDismiss,
        title = { Text("Live Batch", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select batch", fontWeight = FontWeight.Bold)
                if (batches.isEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("No batches available", fontWeight = FontWeight.Bold)
                            Text(
                                "Add your batches and routines from Settings → Batches & Routine.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        batches.forEach {
                            FilterChip(batch == it, { batch = it }, label = { Text(it) })
                        }
                    }

                    val selectedRoutines = routines.filter { it.batch == batch }
                    if (selectedRoutines.isNotEmpty()) {
                        Text("Today's / weekly routine", fontWeight = FontWeight.Bold)
                        selectedRoutines.sortedWith(compareBy({ it.day }, { it.start })).forEach {
                            Text("${it.day} • ${it.start}–${it.end}", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        listOf("Class Notes", "Homework", "New Task", "Reminder").forEach {
                            FilterChip(type == it, { type = it }, label = { Text(it) })
                        }
                    }
                    OutlinedTextField(
                        text,
                        { text = it },
                        label = { Text("Write $type") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = batch.isNotBlank() && text.isNotBlank(),
                        onClick = {
                            onNotes(notes + BatchNote(System.currentTimeMillis(), batch, currentDate(), type, text.trim()))
                            text = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Note") }

                    HorizontalDivider()
                    Text("Recent notes for this batch", fontWeight = FontWeight.Bold)
                    notes.filter { it.batch == batch }.takeLast(10).reversed().forEach {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text("${it.date} • ${it.type}", fontWeight = FontWeight.SemiBold)
                                Text(it.text, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
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
    containerColor: ComposeColor = MaterialTheme.colorScheme.surfaceVariant,
    onClick: () -> Unit = {}
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
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
fun StatusBadge(label: String, onClick: () -> Unit = {}) {
    val colors = when (label.uppercase()) {
        "PAID", "PRESENT", "ACTIVE" -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
        "PARTIAL", "LATE", "PAUSED" -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        "DUE", "ABSENT", "LEFT" -> AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            labelColor = MaterialTheme.colorScheme.onErrorContainer
        )
        else -> AssistChipDefaults.assistChipColors()
    }
    AssistChip(
        onClick = onClick,
        label = { Text(label, fontWeight = FontWeight.SemiBold) },
        colors = colors
    )
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
            title = { Text("Remove demo students?") },
            text = { Text("Only students whose names match the built-in demo list will be removed.") },
            confirmButton = {
                Button(onClick = { confirmRemove = false; onRemoveDemo() }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text("Cancel") } }
        )
    }
}





fun shareAcademicResultWhatsApp(context: Context, student: Student, record: AcademicRecord, previous: AcademicRecord?) {
    if (student.phone.isBlank()) { Toast.makeText(context, "No phone number saved for ${student.name}.", Toast.LENGTH_LONG).show(); return }
    val pct = if (record.maxMarks > 0) ((record.marks / record.maxMarks) * 100).roundToInt() else 0
    val prevText = if (previous != null) { val pp=((previous.marks/previous.maxMarks)*100).roundToInt(); "\nPrevious: ${previous.marks.toCleanNumber()}/${previous.maxMarks.toCleanNumber()} ($pp%)" } else ""
    val body="The Math Guide\nTest Progress Report\nStudent: ${student.name}\nSubject: ${record.subject.ifBlank{"—"}}\nTest: ${record.title.ifBlank{"Test"}}\nDate: ${record.date}\nMarks: ${record.marks.toCleanNumber()}/${record.maxMarks.toCleanNumber()} ($pct%)$prevText\n${record.remarks.takeIf{it.isNotBlank()}?.let{"Remarks: $it\n"} ?: ""}Thank you."
    try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${student.phone.filter(Char::isDigit)}?text=${Uri.encode(body)}"))) } catch(_:Exception){Toast.makeText(context,"Unable to open WhatsApp.",Toast.LENGTH_LONG).show()}
}


fun createAcademicProgressPdf(context: Context, student: Student, record: AcademicRecord, previous: AcademicRecord?, profile: TuitionProfile = TuitionProfile()): Uri {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val left=42f; val right=553f; val navy=Color.rgb(24,43,74); val pale=Color.rgb(237,243,251); val muted=Color.rgb(92,103,118)
    fun t(v:String,x:Float,y:Float,size:Float,bold:Boolean=false,color:Int=Color.DKGRAY){paint.color=color;paint.textSize=size;paint.isFakeBoldText=bold;paint.style=Paint.Style.FILL;canvas.drawText(v.take(60),x,y,paint)}
    fun fill(l:Float,top:Float,r:Float,bottom:Float,color:Int){paint.style=Paint.Style.FILL;paint.color=color;canvas.drawRect(l,top,r,bottom,paint)}
    fun line(x1:Float,y1:Float,x2:Float,y2:Float,color:Int=Color.LTGRAY){paint.style=Paint.Style.STROKE;paint.color=color;paint.strokeWidth=1f;canvas.drawLine(x1,y1,x2,y2,paint);paint.style=Paint.Style.FILL}
    fun field(label:String,value:String,x:Float,y:Float){t(label.uppercase(),x,y,7.5f,true,muted);t(value,x,y+17f,10f,true,navy)}
    val pct=if(record.maxMarks>0)((record.marks/record.maxMarks)*100).roundToInt() else 0
    val previousPct=previous?.let { if(it.maxMarks>0)((it.marks/it.maxMarks)*100).roundToInt() else 0 }

    fill(left,30f,right,106f,navy)
    t(profile.tuitionName.uppercase(),left+15f,58f,19f,true,Color.WHITE)
    t("ACADEMIC PROGRESS REPORT",left+15f,80f,10f,true,Color.WHITE)
    t("Issued: ${currentDate()}",right-112f,80f,8f,false,Color.WHITE)

    t("STUDENT & ASSESSMENT DETAILS",left,133f,10f,true,navy)
    fill(left,143f,right,230f,Color.WHITE)
    line(left,143f,right,143f);line(left,230f,right,230f);line(left,172f,right,172f);line(left,201f,right,201f);line(298f,143f,298f,230f)
    field("Student",student.name,left+12f,157f);field("Class / batch","${student.className.ifBlank{"—"}}  •  ${student.batch.ifBlank{"—"}}",310f,157f)
    field("Subject",record.subject.ifBlank{"Not provided"},left+12f,186f);field("Assessment",record.title.ifBlank{"Test / Exam"},310f,186f)
    field("Assessment date",record.date,left+12f,215f);field("Maximum marks",record.maxMarks.toCleanNumber(),310f,215f)

    t("MARKS SUMMARY",left,258f,10f,true,navy)
    fill(left,268f,right,295f,navy)
    t("ASSESSMENT",left+12f,286f,8f,true,Color.WHITE);t("MARKS OBTAINED",250f,286f,8f,true,Color.WHITE);t("PERCENTAGE",390f,286f,8f,true,Color.WHITE);t("CHANGE",485f,286f,8f,true,Color.WHITE)
    fun resultRow(top:Float,label:String,marks:String,percent:String,change:String="—") { fill(left,top,right,top+31f,if(label=="Current") pale else Color.WHITE); line(left,top+31f,right,top+31f); t(label,left+12f,top+20f,10f,label=="Current",navy);t(marks,250f,top+20f,10f,true,navy);t(percent,390f,top+20f,10f,true,navy);t(change,485f,top+20f,10f,true,navy) }
    resultRow(295f,"Current","${record.marks.toCleanNumber()} / ${record.maxMarks.toCleanNumber()}","$pct%",previousPct?.let { "${if(pct-it>=0) "+" else ""}${pct-it}%" } ?: "—")
    previous?.let { resultRow(326f,"Previous","${it.marks.toCleanNumber()} / ${it.maxMarks.toCleanNumber()}","${previousPct}%") }
    line(left,268f,left,if(previous==null)326f else 357f);line(right,268f,right,if(previous==null)326f else 357f)

    val remarksTop=if(previous==null)365f else 396f
    t("TEACHER REMARKS",left,remarksTop,10f,true,navy)
    fill(left,remarksTop+10f,right,remarksTop+76f,pale)
    val remark=record.remarks.ifBlank{"No remarks recorded for this assessment."}
    t(remark.take(95),left+12f,remarksTop+34f,10f,false,Color.DKGRAY)
    t("Attendance and fee information are available in the full student progress report.",left+12f,remarksTop+57f,8f,false,muted)

    val footer=remarksTop+122f
    line(left,footer,right,footer,Color.LTGRAY)
    t("Prepared by ${profile.teacherName}",left,footer+25f,10f,true,navy)
    t("${profile.qualification}  •  Contact: ${profile.phone}",left,footer+43f,8.5f,false,muted)
    t("Computer-generated academic progress report",left,footer+63f,8f,false,muted)
    doc.finishPage(page)
    val dir=File(context.cacheDir,"reports").apply{mkdirs()}; val file=File(dir,"${student.name.replace(Regex("[^A-Za-z0-9_-]"),"_")}_Progress_${record.date.replace('/','-')}.pdf"); file.outputStream().use{doc.writeTo(it)}; doc.close(); return FileProvider.getUriForFile(context,"${context.packageName}.fileprovider",file)
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
                        Text("No academic records yet.")
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
                                        TextButton(onClick={ if(student!=null) shareReceipt(context, createAcademicProgressPdf(context, student, record, previous)) }) { Text("PDF") }
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
    onSecurityControls: () -> Unit,
    onBatches: () -> Unit,
    onLibrary: () -> Unit
) {
    FullScreenPage(
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
                SettingsButton("👥  Batches & Routine", onBatches)
                SettingsButton("📖  Tuition Library", onLibrary)

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
    payments: List<Payment>,
    attendance: List<AttendanceRecord>,
    outstandingFor: (Student) -> Int,
    onDismiss: () -> Unit,
    onOpenProfile: (Student) -> Unit,
    onCollect: (Student) -> Unit,
    onEdit: (Student) -> Unit,
    onDelete: (Student) -> Unit
) {
    var tab by remember { mutableStateOf("Active") }
    var deleteTarget by remember { mutableStateOf<Student?>(null) }
    var studentSearch by remember { mutableStateOf("") }
    var filterOpen by remember { mutableStateOf(false) }
    var paymentFilter by remember { mutableStateOf("All") }
    var classFilter by remember { mutableStateOf("All") }
    var batchFilter by remember { mutableStateOf("All") }

    val classOptions = listOf("All") + students
        .map { it.className.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
    val batchOptions = listOf("All") + students
        .map { it.batch.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    val filtered = students
        .filter { it.status.equals(tab, true) }
        .filter { studentSearch.isBlank() || it.name.contains(studentSearch.trim(), true) }
        .filter { classFilter == "All" || it.className == classFilter }
        .filter { batchFilter == "All" || it.batch == batchFilter }
        .filter { student ->
            when (paymentFilter) {
                "Paid" -> {
                    val paid = payments
                        .filter { it.studentId == student.id && it.month.equals(currentMonth(), true) }
                        .sumOf { it.amount }
                    paid >= student.monthlyFee && student.monthlyFee > 0
                }
                "Due" -> {
                    val paid = payments
                        .filter { it.studentId == student.id && it.month.equals(currentMonth(), true) }
                        .sumOf { it.amount }
                    paid < student.monthlyFee
                }
                else -> true
            }
        }
        .sortedBy { it.name.lowercase() }

    FullScreenPage(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Students", fontWeight = FontWeight.Bold)
                Text("${filtered.size} $tab students", style = MaterialTheme.typography.labelMedium)
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = studentSearch,
                        onValueChange = { studentSearch = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text("Search student") }
                    )
                    FilterChip(
                        selected = filterOpen,
                        onClick = { filterOpen = !filterOpen },
                        label = { Text("Filter") }
                    )
                }

                if (filterOpen) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(7.dp)
                        ) {
                            Text("Filter students", fontWeight = FontWeight.Bold)

                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                listOf("All", "Paid", "Due").forEach { option ->
                                    FilterChip(
                                        selected = paymentFilter == option,
                                        onClick = { paymentFilter = option },
                                        label = { Text(option) }
                                    )
                                }
                            }

                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                classOptions.forEach { option ->
                                    FilterChip(
                                        selected = classFilter == option,
                                        onClick = { classFilter = option },
                                        label = { Text("Class: $option") }
                                    )
                                }
                            }

                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                batchOptions.forEach { option ->
                                    FilterChip(
                                        selected = batchFilter == option,
                                        onClick = { batchFilter = option },
                                        label = { Text("Batch: $option") }
                                    )
                                }
                            }

                            TextButton(onClick = {
                                paymentFilter = "All"
                                classFilter = "All"
                                batchFilter = "All"
                            }) {
                                Text("Clear filters")
                            }
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf("Active", "Paused", "Left").forEach { s ->
                        FilterChip(
                            selected = tab == s,
                            onClick = { tab = s },
                            label = { Text("$s (${students.count { it.status.equals(s, true) }})") }
                        )
                    }
                }

                if (filtered.isEmpty()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("No $tab students", fontWeight = FontWeight.Bold)
                            Text(
                                if (tab == "Active") "Active students will appear here." else "Students marked $tab will stay here without losing their history.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { student ->
                            StudentCard(
                                student = student,
                                payments = payments,
                                attendance = attendance,
                                isActiveTab = tab == "Active",
                                onOpenProfile = onOpenProfile,
                                onCollect = onCollect,
                                onEdit = onEdit,
                                onDelete = { deleteTarget = it }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )

    deleteTarget?.let { student ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete ${student.name}?") },
            text = { Text("This will also delete this student's payment, attendance and academic history.") },
            confirmButton = {
                Button(onClick = { deleteTarget = null; onDelete(student) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}


@Composable
fun BatchManagerDialog(
    batches: List<Batch>,
    routines: List<BatchRoutine>,
    onSaveBatches: (List<Batch>) -> Unit,
    onSaveRoutines: (List<BatchRoutine>) -> Unit,
    onDismiss: () -> Unit,
    onRenameBatch: (String, String) -> Unit = { _, _ -> }
) {
    var localBatches by remember { mutableStateOf(batches) }
    var localRoutines by remember { mutableStateOf(routines) }
    var showBatchEditor by remember { mutableStateOf(false) }
    var editingBatchId by remember { mutableStateOf<Long?>(null) }
    var showRoutineEditor by remember { mutableStateOf(false) }
    var editingRoutineId by remember { mutableStateOf<Long?>(null) }

    var batchName by remember { mutableStateOf("") }
    var batchAddress by remember { mutableStateOf("") }
    var routineBatch by remember { mutableStateOf("") }
    var routineDay by remember { mutableStateOf("Monday") }
    var routineStart by remember { mutableStateOf("") }
    var routineEnd by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batches & Routine") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Batches", fontWeight = FontWeight.Bold)

                localBatches.forEach { batch ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(batch.name, fontWeight = FontWeight.SemiBold)
                                if (batch.address.isNotBlank()) {
                                    Text(batch.address, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                            TextButton(onClick = {
                                editingBatchId = batch.id
                                batchName = batch.name
                                batchAddress = batch.address
                                showBatchEditor = true
                            }) { Text("Edit") }
                            TextButton(onClick = {
                                localBatches = localBatches.filterNot { it.id == batch.id }
                                localRoutines = localRoutines.filterNot { it.batch == batch.name }
                                onSaveBatches(localBatches)
                                onSaveRoutines(localRoutines)
                            }) { Text("Delete") }
                        }
                    }
                }

                Button(onClick = {
                    editingBatchId = null
                    batchName = ""
                    batchAddress = ""
                    showBatchEditor = true
                }) { Text("Add Batch") }

                HorizontalDivider()

                Text("Weekly Routine", fontWeight = FontWeight.Bold)

                localRoutines.forEach { routine ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(routine.batch, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${routine.day} • ${routine.start} – ${routine.end}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            TextButton(onClick = {
                                editingRoutineId = routine.id
                                routineBatch = routine.batch
                                routineDay = routine.day
                                routineStart = routine.start
                                routineEnd = routine.end
                                showRoutineEditor = true
                            }) { Text("Edit") }
                            TextButton(onClick = {
                                localRoutines = localRoutines.filterNot { it.id == routine.id }
                                onSaveRoutines(localRoutines)
                            }) { Text("Delete") }
                        }
                    }
                }

                Button(onClick = {
                    editingRoutineId = null
                    routineBatch = localBatches.firstOrNull()?.name ?: ""
                    routineDay = "Monday"
                    routineStart = ""
                    routineEnd = ""
                    showRoutineEditor = true
                }) { Text("Add Routine") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )

    if (showBatchEditor) {
        AlertDialog(
            onDismissRequest = { showBatchEditor = false },
            title = { Text(if (editingBatchId == null) "Add Batch" else "Edit Batch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = batchName,
                        onValueChange = { batchName = it },
                        label = { Text("Batch name") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = batchAddress,
                        onValueChange = { batchAddress = it },
                        label = { Text("Address") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (batchName.isNotBlank()) {
                        val oldName = localBatches.firstOrNull { it.id == editingBatchId }?.name
                        localBatches = if (editingBatchId == null) {
                            localBatches + Batch(
                                id = (localBatches.maxOfOrNull { it.id } ?: 0L) + 1L,
                                name = batchName.trim(),
                                address = batchAddress.trim()
                            )
                        } else {
                            localBatches.map {
                                if (it.id == editingBatchId) it.copy(
                                    name = batchName.trim(),
                                    address = batchAddress.trim()
                                ) else it
                            }
                        }
                        if (oldName != null && oldName != batchName.trim()) {
                            localRoutines = localRoutines.map {
                                if (it.batch == oldName) it.copy(batch = batchName.trim()) else it
                            }
                            onRenameBatch(oldName, batchName.trim())
                        }
                        onSaveBatches(localBatches)
                        onSaveRoutines(localRoutines)
                        showBatchEditor = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBatchEditor = false }) { Text("Cancel") }
            }
        )
    }

    if (showRoutineEditor) {
        AlertDialog(
            onDismissRequest = { showRoutineEditor = false },
            title = { Text(if (editingRoutineId == null) "Add Routine" else "Edit Routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = routineBatch,
                        onValueChange = { routineBatch = it },
                        label = { Text("Batch") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = routineDay,
                        onValueChange = { routineDay = it },
                        label = { Text("Day") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = routineStart,
                        onValueChange = { routineStart = it },
                        label = { Text("Start time (AM/PM)") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = routineEnd,
                        onValueChange = { routineEnd = it },
                        label = { Text("End time (AM/PM)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (routineBatch.isNotBlank() && routineDay.isNotBlank() &&
                        routineStart.isNotBlank() && routineEnd.isNotBlank()
                    ) {
                        localRoutines = if (editingRoutineId == null) {
                            localRoutines + BatchRoutine(
                                id = (localRoutines.maxOfOrNull { it.id } ?: 0L) + 1L,
                                batch = routineBatch.trim(),
                                day = routineDay.trim(),
                                start = routineStart.trim(),
                                end = routineEnd.trim()
                            )
                        } else {
                            localRoutines.map {
                                if (it.id == editingRoutineId) it.copy(
                                    batch = routineBatch.trim(),
                                    day = routineDay.trim(),
                                    start = routineStart.trim(),
                                    end = routineEnd.trim()
                                ) else it
                            }
                        }
                        onSaveRoutines(localRoutines)
                        showRoutineEditor = false
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showRoutineEditor = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun LibraryDialog(
    items: List<LibraryItem>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (LibraryItem) -> Unit
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    val visible = items.filter { search.isBlank() || it.name.contains(search,true) || it.category.contains(search,true) }
    AlertDialog(onDismissRequest=onDismiss,title={Text("Tuition Library")},text={Column(Modifier.fillMaxWidth().heightIn(max=560.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Text("Save notes, PDFs, books and tuition documents on this phone.",style=MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(search,{search=it},label={Text("Search library")},singleLine=true,modifier=Modifier.weight(1f));Button(onClick=onAdd){Text("Add")}}
        if(visible.isEmpty())Text("No library files yet.")
        visible.forEach { item -> Card{Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.name,fontWeight=FontWeight.SemiBold);Text("${item.category} • ${item.addedDate}",style=MaterialTheme.typography.labelSmall)};TextButton(onClick={try{context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(item.uri)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))}catch(_:Exception){Toast.makeText(context,"No app available to open this file.",Toast.LENGTH_LONG).show()}}){Text("Open")};TextButton(onClick={onDelete(item)}){Text("Delete")}}}}
    }},confirmButton={TextButton(onClick=onDismiss){Text("Close")}})
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
    batches: List<Batch> = emptyList(),
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

                var batchOpen by remember { mutableStateOf(false) }
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { batchOpen = true }, Modifier.fillMaxWidth()) {
                        Text(if (batch.isBlank()) "Select Batch" else "Batch: $batch")
                    }
                    DropdownMenu(batchOpen, { batchOpen = false }) {
                        batches.sortedBy { it.name.lowercase() }.forEach { b ->
                            DropdownMenuItem(text = { Text(b.name) }, onClick = { batch = b.name; batchOpen = false })
                        }
                        DropdownMenuItem(text = { Text("No batch / enter manually") }, onClick = { batchOpen = false })
                    }
                }
                if (batches.isEmpty()) {
                    OutlinedTextField(batch, { batch = it }, label = { Text("Batch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }

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
                            Text("No fee history yet.")
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
    var receiptSearch by remember { mutableStateOf("") }

    val receiptGroups = payments
        .filter { it.receiptNo.isNotBlank() }
        .groupBy { it.receiptNo }
        .values
        .filter { group ->
            val first = group.first()
            val studentName = studentMap[first.studentId]?.name.orEmpty()
            receiptSearch.isBlank() || first.receiptNo.contains(receiptSearch, true) || studentName.contains(receiptSearch, true)
        }
        .sortedByDescending { group ->
            try {
                LocalDate.parse(
                    group.firstOrNull()?.date.orEmpty(),
                    DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
                ).toEpochDay()
            } catch (_: Exception) {
                Long.MIN_VALUE
            }
        }

    FullScreenPage(
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
                OutlinedTextField(
                    value = receiptSearch,
                    onValueChange = { receiptSearch = it },
                    label = { Text("Search receipt no. or student name") },
                    singleLine = true,
                    trailingIcon = { if (receiptSearch.isNotBlank()) TextButton(onClick = { receiptSearch = "" }) { Text("Clear") } },
                    modifier = Modifier.fillMaxWidth()
                )

                if (receiptGroups.isEmpty()) {
                    Text("No receipt history yet.")
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
        val uri = remember(receiptNo, profile) {
            createReceiptPdf(context, receiptStudent, receiptPayments, receiptNo, profile)
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
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(45f, y, 550f, y, paint)
        paint.style = Paint.Style.FILL
    }
    fun fill(l: Float, top: Float, r: Float, bottom: Float, color: Int) {
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawRect(l, top, r, bottom, paint)
    }
    fun vertical(x: Float, top: Float, bottom: Float, color: Int = Color.LTGRAY) {
        paint.style = Paint.Style.STROKE
        paint.color = color
        paint.strokeWidth = 1f
        canvas.drawLine(x, top, x, bottom, paint)
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

        val present = studentAttendance.count { it.status == "PRESENT" }
        val absent = studentAttendance.count { it.status == "ABSENT" }
        val late = studentAttendance.count { it.status == "LATE" }
        val marked = present + absent + late
        val attendanceRate = if (marked > 0) ((present.toFloat() / marked) * 100).roundToInt() else 0

        val validAcademic = studentAcademic.filter { it.maxMarks > 0 }
        val totalMarks = validAcademic.sumOf { it.marks }
        val totalMax = validAcademic.sumOf { it.maxMarks }
        val average = if (totalMax > 0) ((totalMarks / totalMax) * 100).roundToInt() else 0

        val navy = Color.rgb(24, 43, 74)
        val paleBlue = Color.rgb(237, 243, 251)
        val muted = Color.rgb(92, 103, 118)
        fill(42f, 30f, 553f, 108f, navy)
        paint.color = Color.WHITE
        center("THE MATH GUIDE", 57f, 21f, true)
        center("STUDENT PROGRESS REPORT", 80f, 11f, true)
        center(s.name.take(48), 98f, 10f)

        paint.color = navy
        t("STUDENT DETAILS", 45f, 133f, 10f, true)
        fill(45f, 143f, 550f, 205f, Color.WHITE)
        line(143f); line(174f); line(205f); vertical(298f, 143f, 205f)
        paint.color = muted; t("CLASS", 57f, 157f, 7.5f, true); t("BATCH", 310f, 157f, 7.5f, true)
        paint.color = navy; t(s.className.ifBlank { "—" }, 57f, 170f, 10f, true); t(s.batch.ifBlank { "—" }, 310f, 170f, 10f, true)
        paint.color = muted; t("JOINING MONTH", 57f, 188f, 7.5f, true); t("MONTHLY FEE / PAID", 310f, 188f, 7.5f, true)
        paint.color = navy; t(s.joiningMonth.ifBlank { "Not provided" }, 57f, 201f, 10f, true); t("₹${s.monthlyFee}  /  ₹$paidTotal", 310f, 201f, 10f, true)

        fill(45f, 226f, 550f, 276f, paleBlue)
        paint.color = muted; t("ATTENDANCE", 57f, 243f, 7.5f, true); t("ACADEMIC AVERAGE", 184f, 243f, 7.5f, true); t("ASSESSMENTS", 334f, 243f, 7.5f, true); t("FEES PAID", 455f, 243f, 7.5f, true)
        paint.color = navy; t("$attendanceRate%", 57f, 263f, 15f, true); t("$average%", 184f, 263f, 15f, true); t(validAcademic.size.toString(), 334f, 263f, 15f, true); t("₹$paidTotal", 455f, 263f, 15f, true)

        paint.color = navy; t("ACADEMIC PERFORMANCE", 45f, 302f, 10f, true)
        fill(45f, 312f, 550f, 337f, navy)
        paint.color = Color.WHITE; t("DATE", 55f, 329f, 7.5f, true); t("SUBJECT", 120f, 329f, 7.5f, true); t("ASSESSMENT", 215f, 329f, 7.5f, true); t("MARKS", 360f, 329f, 7.5f, true); t("%", 435f, 329f, 7.5f, true); t("REMARKS", 475f, 329f, 7.5f, true)
        val results = validAcademic.sortedByDescending { it.date }.take(7)
        results.forEachIndexed { index, r ->
            val top = 337f + index * 28f
            if (index % 2 == 0) fill(45f, top, 550f, top + 28f, Color.rgb(249, 250, 252))
            line(top + 28f)
            val pct = ((r.marks / r.maxMarks) * 100).roundToInt()
            paint.color = Color.DKGRAY
            t(r.date, 55f, top + 18f, 8.5f); t(r.subject.ifBlank { "—" }.take(15), 120f, top + 18f, 8.5f, true); t(r.title.ifBlank { "Test" }.take(23), 215f, top + 18f, 8.5f); t("${r.marks.toCleanNumber()}/${r.maxMarks.toCleanNumber()}", 360f, top + 18f, 8.5f, true); t("$pct%", 435f, top + 18f, 8.5f, true); t(r.remarks.ifBlank { "—" }.take(14), 475f, top + 18f, 8f)
        }
        val tableBottom = 337f + maxOf(1, results.size) * 28f
        line(312f); line(tableBottom); vertical(45f, 312f, tableBottom); vertical(550f, 312f, tableBottom)

        val attendanceTop = tableBottom + 30f
        paint.color = navy; t("ATTENDANCE SUMMARY", 45f, attendanceTop, 10f, true)
        fill(45f, attendanceTop + 10f, 550f, attendanceTop + 47f, paleBlue)
        paint.color = muted; t("PRESENT", 58f, attendanceTop + 25f, 7.5f, true); t("ABSENT", 175f, attendanceTop + 25f, 7.5f, true); t("LATE", 290f, attendanceTop + 25f, 7.5f, true); t("TOTAL MARKED", 390f, attendanceTop + 25f, 7.5f, true)
        paint.color = navy; t(present.toString(), 58f, attendanceTop + 42f, 12f, true); t(absent.toString(), 175f, attendanceTop + 42f, 12f, true); t(late.toString(), 290f, attendanceTop + 42f, 12f, true); t(marked.toString(), 390f, attendanceTop + 42f, 12f, true)

        val footer = maxOf(attendanceTop + 82f, 735f)
        line(footer)
        paint.color = navy; t("Prepared by Ashraful Hoque • B.SC Maths", 45f, footer + 23f, 9f, true)
        paint.color = muted; t("The Math Guide • 9732956571 • Computer-generated student progress report", 45f, footer + 41f, 8f)

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
