package com.themathguild.mytuitionmanager

import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.roundToInt

data class Student(
    val id: Long,
    val name: String,
    val className: String,
    val batch: String,
    val monthlyFee: Int,
    val phone: String,
    val joiningMonth: String = ""
)

data class Payment(
    val studentId: Long,
    val month: String,
    val amount: Int,
    val date: String,
    val receiptNo: String = ""
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
                o.optString("joiningMonth", "")
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

fun createReceiptPdf(
    context: Context,
    student: Student,
    paymentsForReceipt: List<Payment>,
    receiptNo: String
): Uri {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    val left = 45f
    val right = 550f

    fun fillText(value: String, x: Float, y: Float, size: Float, bold: Boolean = false) {
        paint.style = Paint.Style.FILL
        paint.textSize = size
        paint.isFakeBoldText = bold
        canvas.drawText(value, x, y, paint)
    }

    fun center(value: String, y: Float, size: Float, bold: Boolean = false) {
        paint.style = Paint.Style.FILL
        paint.textSize = size
        paint.isFakeBoldText = bold
        canvas.drawText(value, (595f - paint.measureText(value)) / 2f, y, paint)
    }

    fun line(y: Float, width: Float = 1f) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        canvas.drawLine(left, y, right, y, paint)
        paint.style = Paint.Style.FILL
    }

    fun box(top: Float, bottom: Float, width: Float = 1.2f) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = width
        canvas.drawRoundRect(left, top, right, bottom, 8f, 8f, paint)
        paint.style = Paint.Style.FILL
    }

    val sortedPayments = paymentsForReceipt.sortedBy { monthKey(it.month) }
    val totalPaid = sortedPayments.sumOf { it.amount }
    val totalFee = student.monthlyFee * sortedPayments.size
    val status = if (totalPaid >= totalFee) "PAID" else "PARTIAL"
    val date = sortedPayments.firstOrNull()?.date ?: currentDate()

    // Professional header
    center("THE MATH GUIDE", 58f, 27f, true)
    center("Tuition & Academic Support", 78f, 11f)
    center("FEE PAYMENT RECEIPT", 112f, 18f, true)
    line(132f, 1.5f)

    fillText("Receipt No.", left, 158f, 10f)
    fillText(receiptNo, left, 178f, 14f, true)
    fillText("Payment Date", 390f, 158f, 10f)
    fillText(date, 390f, 178f, 14f, true)

    // Student section
    fillText("STUDENT INFORMATION", left, 210f, 13f, true)
    box(222f, 330f)

    fillText("Student Name", left + 16f, 246f, 9f)
    fillText(student.name, left + 16f, 267f, 14f, true)

    fillText("Class", left + 16f, 295f, 9f)
    fillText(student.className.ifBlank { "Not provided" }, left + 16f, 315f, 12f, true)

    fillText("Batch", 300f, 295f, 9f)
    fillText(student.batch.ifBlank { "Not provided" }, 300f, 315f, 12f, true)

    fillText("Joining Month", left + 16f, 345f, 9f)
    fillText(
        student.joiningMonth.ifBlank { "Not provided" },
        left + 16f,
        365f,
        12f,
        true
    )

    // Payment section
    fillText("PAYMENT SUMMARY", left, 400f, 13f, true)
    box(412f, 600f)

    fillText("Fee Month(s)", left + 16f, 442f, 9f)
    fillText(
        sortedPayments.joinToString(", ") { it.month }.take(52),
        left + 16f,
        462f,
        11f,
        true
    )

    line(478f, 0.7f)

    fillText("Months Covered", left + 16f, 508f, 10f)
    fillText(sortedPayments.size.toString(), 470f, 508f, 12f, true)

    fillText("Monthly Fee", left + 16f, 540f, 10f)
    fillText("₹${student.monthlyFee}", 440f, 540f, 12f, true)

    line(554f, 0.7f)

    fillText("TOTAL PAID", left + 16f, 585f, 12f, true)
    fillText("₹$totalPaid", 420f, 585f, 18f, true)

    // Status badge
    box(620f, 664f, 1.5f)
    fillText("PAYMENT STATUS", left + 16f, 646f, 10f, true)
    fillText(status, 470f, 646f, 13f, true)

    center("Thank you for your payment.", 700f, 12f, true)
    center("Please keep this receipt for your records.", 718f, 9f)

    line(740f)

    fillText("Authorized by", left, 762f, 9f)
    fillText("Ashraful Hoque", left, 783f, 12f, true)
    fillText("B.SC Maths • The Math Guide", left, 801f, 9f)

    fillText("Contact", 390f, 762f, 9f)
    fillText("9732956571", 390f, 783f, 11f, true)

    center("Computer-generated receipt", 823f, 8f)

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

fun exportBackup(context: Context): Uri {
    val prefs = context.getSharedPreferences("tuition_data", Context.MODE_PRIVATE)
    val json = JSONObject().apply {
        put("app", "My Tuition Manager")
        put("version", "3.0")
        put("backupDate", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
        put("students", JSONArray(prefs.getString("students", "[]") ?: "[]"))
        put("payments", JSONArray(prefs.getString("payments", "[]") ?: "[]"))
    }.toString(2)

    val dir = File(context.cacheDir, "backup").apply { mkdirs() }
    val file = File(dir, "MyTuitionManager_Backup.json")
    file.writeText(json)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun importBackup(context: Context, uri: Uri): Boolean = try {
    val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: return false
    val obj = JSONObject(json)
    val prefs = context.getSharedPreferences("tuition_data", Context.MODE_PRIVATE)
    prefs.edit()
        .putString("students", obj.optJSONArray("students")?.toString() ?: "[]")
        .putString("payments", obj.optJSONArray("payments")?.toString() ?: "[]")
        .apply()
    true
} catch (_: Exception) {
    false
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
    var search by remember { mutableStateOf("") }
    var selectedBatch by remember { mutableStateOf("All") }
    var paymentFilter by remember { mutableStateOf("All") }
    var selectedStudent by remember { mutableStateOf<Student?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var editStudent by remember { mutableStateOf<Student?>(null) }
    var collectStudent by remember { mutableStateOf<Student?>(null) }
    var reportOpen by remember { mutableStateOf(false) }
    var receiptHistoryOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    var dashboardExpanded by remember { mutableStateOf(true) }
    var manageStudentsOpen by remember { mutableStateOf(false) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }

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
    val unpaidStudents = students.size - paidStudents
    val totalOutstanding = students.sumOf { outstanding(it) }
    val totalCollected = payments.filter { it.month.equals(thisMonth, true) }.sumOf { it.amount }
    val expectedThisMonth = students.sumOf { it.monthlyFee }
    val batches = listOf("All") + students.map { it.batch }.filter { it.isNotBlank() }.distinct().sorted()
    val visibleStudents = students.filter {
        it.name.contains(search, true) || it.className.contains(search, true) || it.batch.contains(search, true) || it.phone.contains(search, true)
    }.filter { selectedBatch == "All" || it.batch == selectedBatch }
     .filter { when (paymentFilter) { "Paid" -> currentPaid(it) >= it.monthlyFee; "Unpaid" -> currentPaid(it) < it.monthlyFee; else -> true } }
     .sortedBy { it.name.lowercase() }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.developer_photo),
                                contentDescription = "Ashraful Hoque",
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text("The Math Guide", fontWeight = FontWeight.Bold)
                                Text("My Tuition Manager", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    },
                    actions = { IconButton(onClick = { settingsOpen = true }) { Text("⚙", style = MaterialTheme.typography.titleLarge) } }
                )
            }
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Good day!", style = MaterialTheme.typography.titleMedium)
                    Text("Fee overview • $thisMonth", style = MaterialTheme.typography.bodyMedium)
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.developer_photo),
                                contentDescription = "Ashraful Hoque",
                                modifier = Modifier.size(64.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(
                                    "Ashraful Hoque",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "B.SC Maths • The Math Guide",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                TextButton(onClick = { dashboardExpanded = !dashboardExpanded }) {
                                    Text(if (dashboardExpanded) "Hide" else "Show")
                                }
                            }

                            if (dashboardExpanded) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DashboardCard("Students", students.size.toString(), Modifier.weight(1f))
                                DashboardCard("Collected", money(totalCollected), Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DashboardCard("Due", money(totalOutstanding), Modifier.weight(1f))
                                DashboardCard("Paid", "$paidStudents / ${students.size}", Modifier.weight(1f))
                            }
                            val percent = if (expectedThisMonth == 0) {
                                0
                            } else {
                                ((totalCollected.toFloat() / expectedThisMonth.toFloat()) * 100f)
                                    .roundToInt()
                                    .coerceIn(0, 100)
                            }

                            Text(
                                if (expectedThisMonth == 0)
                                    "No monthly fee target yet"
                                else
                                    "Collection rate: $percent%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            LinearProgressIndicator(
                                progress = {
                                    if (expectedThisMonth == 0) {
                                        0f
                                    } else {
                                        (totalCollected.toFloat() / expectedThisMonth.toFloat())
                                            .coerceIn(0f, 1f)
                                    }
                                },
                                Modifier.fillMaxWidth()
                            )
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), label = { Text("Search student") }, singleLine = true)
                }
                item {
                    Text("Payment Status", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Paid", "Unpaid").forEach { status ->
                            FilterChip(paymentFilter == status, { paymentFilter = status }, label = { Text(status) })
                        }
                    }
                }
                item {
                    Text("Batch", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { batches.take(5).forEach { batch -> FilterChip(selectedBatch == batch, { selectedBatch = batch }, label = { Text(batch) }) } }
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

                    Card(
                        onClick = { selectedStudent = student },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            Modifier.padding(16.dp),
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

                            if (student.phone.isNotBlank()) {
                                Text(
                                    "☎ ${student.phone}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            Button(
                                onClick = { collectStudent = student },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Collect Fee")
                            }

                            Text(
                                "Tap card for full profile",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                item {
                    if (students.isEmpty()) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("No students yet"); Text("Add your first student from Settings.") } }
                }
            }
        }

        if (settingsOpen) SettingsDialog(
            onDismiss = { settingsOpen = false },
            onAdd = { settingsOpen = false; addOpen = true },
            onManage = { manageStudentsOpen = true },
            onReports = { settingsOpen = false; reportOpen = true },
            onReceiptHistory = { settingsOpen = false; receiptHistoryOpen = true },
            onBackup = { shareReceipt(context, exportBackup(context)) },
            onRestore = {
                (context as? MainActivity)?.pickBackup { ok ->
                    if (ok) { students = store.loadStudents(); payments = store.loadPayments() }
                    Toast.makeText(context, if (ok) "Backup restored successfully." else "Restore failed.", Toast.LENGTH_LONG).show()
                }
            }
        )

        if (manageStudentsOpen) ManageStudentsDialog(
            students = students,
            onDismiss = { manageStudentsOpen = false },
            onEdit = { s -> manageStudentsOpen = false; settingsOpen = false; editStudent = s },
            onDelete = { s ->
                students = students.filter { it.id != s.id }
                payments = payments.filter { it.studentId != s.id }
                store.saveStudents(students)
                store.savePayments(payments)
            }
        )

        if (addOpen) StudentEditorDialog("Add Student", null, { addOpen = false }) { s -> students = students + s; store.saveStudents(students); addOpen = false }
        editStudent?.let { s -> StudentEditorDialog("Edit Student", s, { editStudent = null }) { u -> students = students.map { if (it.id == u.id) u else it }; store.saveStudents(students); editStudent = null } }
        selectedStudent?.let { s ->
            StudentDetailsDialog(
                student = s,
                payments = payments.filter { it.studentId == s.id },
                outstanding = outstanding(s),
                onDismiss = { selectedStudent = null },
                onCollect = { collectStudent = s; selectedStudent = null }
            )
        }
        collectStudent?.let { s ->
            CollectFeeDialog(
                student = s,
                existingPayments = payments.filter { it.studentId == s.id },
                onDismiss = { collectStudent = null }
            ) { selectedMonths ->
                val date = currentDate()
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                val nextReceiptNumber = payments
                    .mapNotNull { Regex("TMG-\\d{4}-(\\d+)").find(it.receiptNo)?.groupValues?.get(1)?.toIntOrNull() }
                    .maxOrNull()?.plus(1) ?: 1
                val receiptNo = "TMG-$year-${nextReceiptNumber.toString().padStart(5, '0')}"

                val newPayments = selectedMonths.map { month ->
                    Payment(s.id, month, s.monthlyFee, date, receiptNo)
                }

                payments = payments + newPayments
                store.savePayments(payments)
                receiptUri = createReceiptPdf(context, s, newPayments, receiptNo)
                collectStudent = null
            }
        }
        receiptUri?.let { uri -> AlertDialog(onDismissRequest = { receiptUri = null }, title = { Text("Payment Saved ✓") }, text = { Text("Your professional PDF receipt has been created.") }, confirmButton = { Button({ shareReceiptWhatsApp(context, uri); receiptUri = null }) { Text("WhatsApp") } }, dismissButton = { TextButton({ shareReceipt(context, uri); receiptUri = null }) { Text("Other Share") } }) }
        if (reportOpen) ReportsDialog(students, payments) { reportOpen = false }
        if (receiptHistoryOpen) ReceiptHistoryDialog(students, payments) { receiptHistoryOpen = false }
    }
}

@Composable
fun DashboardCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) { Column(Modifier.padding(12.dp)) { Text(title, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
}

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onManage: () -> Unit,
    onReports: () -> Unit,
    onReceiptHistory: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit
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

                Spacer(Modifier.height(4.dp))

                Text(
                    "DATA & REPORTS",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                SettingsButton("📊  Reports", onReports)
                SettingsButton("🧾  Receipt History", onReceiptHistory)
                SettingsButton("💾  Backup Data", onBackup)
                SettingsButton("♻️  Restore Data", onRestore)

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
                if (students.isEmpty()) Text("No students available.")
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
            text = { Text("This will also delete this student's payment history.") },
            confirmButton = { Button(onClick = { deleteTarget = null; onDelete(s) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
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
                            phone.trim(), joiningMonth.trim()
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
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
    val join = monthKey(student.joiningMonth) ?: YearMonth.now()
    val latestAllowed = YearMonth.now().plusMonths(1)

    val eligibleMonths = buildList {
        var m = join
        while (!m.isAfter(latestAllowed)) {
            val monthName = m.format(monthFormatter)
            val alreadyPaid = existingPayments.any { monthKey(it.month) == m }
            if (!alreadyPaid) add(monthName)
            m = m.plusMonths(1)
        }
    }

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
                Text("Select one or more unpaid months.")
                Text(
                    "Collection is allowed from the joining month up to one month in advance. " +
                        "A month can be collected only once."
                )

                if (eligibleMonths.isEmpty()) {
                    Text("No eligible months available for collection.", fontWeight = FontWeight.Bold)
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
fun ReceiptHistoryDialog(
    students: List<Student>,
    payments: List<Payment>,
    onDismiss: () -> Unit
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
                                    ) {
                                        Text("Share Receipt")
                                    }
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
                Button(onClick = {
                    shareReceiptWhatsApp(context, uri)
                    selectedReceipt = null
                    selectedStudent = null
                }) {
                    Text("WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    shareReceipt(context, uri)
                    selectedReceipt = null
                    selectedStudent = null
                }) {
                    Text("Other Share")
                }
            }
        )
    }
}

@Composable
fun ReportsDialog(
    students: List<Student>,
    payments: List<Payment>,
    onDismiss: () -> Unit
) {
    var report by remember { mutableStateOf("Monthly") }
    val months = payments.map { it.month }.distinct().sortedDescending()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reports", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("Monthly", "Yearly", "Batch", "Students").forEach { r ->
                        FilterChip(
                            selected = report == r,
                            onClick = { report = r },
                            label = { Text(r) }
                        )
                    }
                }
                HorizontalDivider()

                if (report == "Monthly") {
                    Text("Monthly Collection", fontWeight = FontWeight.Bold)
                    if (months.isEmpty()) Text("No payments recorded.")
                    else months.forEach { month ->
                        val total = payments.filter { it.month == month }.sumOf { it.amount }
                        Card {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(month)
                                Text("₹$total", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (report == "Yearly") {
                    Text("Yearly Collection", fontWeight = FontWeight.Bold)

                    val years = payments.mapNotNull { payment ->
                        Regex("\\b(20\\d{2})\\b").find(payment.month)?.groupValues?.get(1)
                            ?: payment.date.takeLast(4).takeIf { it.length == 4 && it.all(Char::isDigit) }
                    }.distinct().sortedDescending()

                    if (years.isEmpty()) {
                        Text("No payments recorded.")
                    } else {
                        years.forEach { year ->
                            val yearPayments = payments.filter { payment ->
                                val paymentYear =
                                    Regex("\\b(20\\d{2})\\b").find(payment.month)?.groupValues?.get(1)
                                        ?: payment.date.takeLast(4)
                                paymentYear == year
                            }
                            val total = yearPayments.sumOf { it.amount }
                            val paymentCount = yearPayments.size
                            val studentsPaid = yearPayments.map { it.studentId }.distinct().size

                            Card {
                                Column(Modifier.padding(12.dp)) {
                                    Text(year, fontWeight = FontWeight.Bold)
                                    Text("Collected: ₹$total")
                                    Text("Payments: $paymentCount")
                                    Text("Students with payments: $studentsPaid")
                                }
                            }
                        }
                    }
                } else if (report == "Batch") {
                    Text("Batch-wise Collection", fontWeight = FontWeight.Bold)
                    students.map { it.batch }.filter { it.isNotBlank() }.distinct().sorted().forEach { batch ->
                        val bs = students.filter { it.batch == batch }
                        val ids = bs.map { it.id }
                        val total = payments.filter { it.studentId in ids }.sumOf { it.amount }
                        val paid = bs.count { s ->
                            payments.filter { it.studentId == s.id && it.month.equals(currentMonth(), true) }
                                .sumOf { it.amount } >= s.monthlyFee
                        }
                        Card {
                            Column(Modifier.padding(12.dp)) {
                                Text("Batch $batch", fontWeight = FontWeight.Bold)
                                Text("Students: ${bs.size}")
                                Text("Collected: ₹$total")
                                Text("Paid this month: $paid")
                            }
                        }
                    }
                } else {
                    Text("Student Payment History", fontWeight = FontWeight.Bold)
                    students.sortedBy { it.name.lowercase() }.forEach { s ->
                        val ps = payments.filter { it.studentId == s.id }
                        Card {
                            Column(Modifier.padding(12.dp)) {
                                Text(s.name, fontWeight = FontWeight.Bold)
                                Text("${s.className} • ${s.batch}")
                                Text("Joining Month: ${s.joiningMonth.ifBlank { "Not provided" }}")
                                Text("Monthly fee: ₹${s.monthlyFee}")
                                Text("Total paid: ₹${ps.sumOf { it.amount }}")
                                Text("Payments: ${ps.size}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onDismiss) { Text("Close") } }
    )
}
