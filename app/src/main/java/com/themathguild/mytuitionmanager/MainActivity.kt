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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

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
    val date: String
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
                o.getString("date")
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
            })
        }
        prefs.edit().putString("payments", a.toString()).apply()
    }
}

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

fun currentMonth(): String =
    SimpleDateFormat("MMMM yyyy", Locale.ENGLISH).format(Date())

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
    payment: Payment,
    receiptNo: String
): Uri {
    val doc = PdfDocument()
    val page = doc.startPage(
        PdfDocument.PageInfo.Builder(595, 842, 1).create()
    )
    val canvas = page.canvas
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val left = 45f
    val right = 550f
    var y = 45f

    fun text(v: String, x: Float = left, size: Float = 13f, bold: Boolean = false) {
        paint.style = Paint.Style.FILL
        paint.textSize = size
        paint.isFakeBoldText = bold
        canvas.drawText(v, x, y, paint)
    }

    fun center(v: String, size: Float, bold: Boolean = false) {
        paint.textSize = size
        paint.isFakeBoldText = bold
        paint.style = Paint.Style.FILL
        canvas.drawText(v, (595f - paint.measureText(v)) / 2f, y, paint)
    }

    fun line() {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawLine(left, y, right, y, paint)
        paint.style = Paint.Style.FILL
    }

    fun box(top: Float, bottom: Float) {
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        canvas.drawRect(left, top, right, bottom, paint)
        paint.style = Paint.Style.FILL
    }

    center("THE MATH GUILD", 28f, true); y += 23f
    center("Mastering the Craft of Problem Solving", 11f); y += 28f
    center("FEE PAYMENT RECEIPT", 19f, true); y += 25f
    line(); y += 22f

    text("Receipt No.: $receiptNo", left, 12f, true)
    text("Date: ${payment.date}", 365f, 12f, true); y += 25f
    line(); y += 25f

    text("STUDENT DETAILS", left, 15f, true); y += 24f
    box(y - 18f, y + 140f)
    text("Student Name", left + 15f, 10f)
    y += 18f; text(student.name, left + 15f, 14f, true)
    y += 32f; text("Class", left + 15f, 10f)
    text(student.className, left + 15f, 13f, true)
    text("Batch", 300f, 10f); text(student.batch, 300f, 13f, true)
    y += 32f; text("Joining Month", left + 15f, 10f)
    text(if (student.joiningMonth.isBlank()) "Not provided" else student.joiningMonth, left + 15f, 13f, true)
    y += 32f; text("Phone", left + 15f, 10f)
    text(if (student.phone.isBlank()) "Not provided" else student.phone, left + 15f, 13f)
    y += 42f

    text("PAYMENT DETAILS", left, 15f, true); y += 25f
    box(y - 18f, y + 125f)
    text("Fee Month", left + 15f, 11f); text(payment.month, 300f, 13f, true)
    y += 32f; line(); y += 28f
    text("Monthly Fee", left + 15f, 11f); text("₹${student.monthlyFee}", 430f, 13f, true)
    y += 32f; line(); y += 28f
    text("AMOUNT PAID", left + 15f, 13f, true); text("₹${payment.amount}", 410f, 18f, true)
    y += 40f
    box(y - 18f, y + 35f)
    text("PAYMENT STATUS", left + 15f, 11f, true); text("PAID", 450f, 14f, true)
    y += 65f

    center("Thank you for your payment.", 12f); y += 40f
    line(); y += 35f
    text("Authorized by", left, 10f); y += 20f
    text("Ashraful Hoque", left, 13f, true); y += 18f
    text("The Math Guild", left, 11f); text("Phone: 9732956571", 365f, 11f)
    y += 30f; center("This is a computer-generated receipt.", 9f)

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

fun exportBackup(context: Context): Uri {
    val prefs = context.getSharedPreferences("tuition_data", Context.MODE_PRIVATE)
    val json = JSONObject().apply {
        put("app", "My Tuition Manager")
        put("version", "4.0")
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
    var settingsOpen by remember { mutableStateOf(false) }
    var receiptUri by remember { mutableStateOf<Uri?>(null) }

    val thisMonth = currentMonth()

    fun currentPaid(student: Student): Int =
        payments.filter {
            it.studentId == student.id && it.month.equals(thisMonth, true)
        }.sumOf { it.amount }

    fun outstanding(student: Student): Int {
        val start = monthKey(student.joiningMonth) ?: payments
            .filter { it.studentId == student.id }
            .mapNotNull { monthKey(it.month) }
            .minOrNull() ?: YearMonth.now()

        val now = YearMonth.now()
        val studentPayments = payments.filter { it.studentId == student.id }
        var m = start
        var due = 0

        while (!m.isAfter(now)) {
            val name = m.format(monthFormatter)
            val paid = studentPayments
                .filter { it.month.equals(name, true) }
                .sumOf { it.amount }
            due += maxOf(0, student.monthlyFee - paid)
            m = m.plusMonths(1)
        }
        return due
    }

    val paidStudents = students.count { currentPaid(it) >= it.monthlyFee }
    val unpaidStudents = students.size - paidStudents
    val totalOutstanding = students.sumOf { outstanding(it) }
    val totalCollected = payments
        .filter { it.month.equals(thisMonth, true) }
        .sumOf { it.amount }

    val batches = listOf("All") + students
        .map { it.batch }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    val visibleStudents = students
        .filter {
            it.name.contains(search, true) ||
            it.className.contains(search, true) ||
            it.batch.contains(search, true) ||
            it.phone.contains(search, true)
        }
        .filter { selectedBatch == "All" || it.batch == selectedBatch }
        .filter {
            when (paymentFilter) {
                "Paid" -> currentPaid(it) >= it.monthlyFee
                "Unpaid" -> currentPaid(it) < it.monthlyFee
                else -> true
            }
        }
        .sortedBy { it.name.lowercase() }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("The Math Guild", fontWeight = FontWeight.Bold)
                            Text(
                                "My Tuition Manager",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = { settingsOpen = true }) {
                            Text("Settings")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        thisMonth,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard("Students", students.size.toString(), Modifier.weight(1f))
                        SummaryCard("Collected", money(totalCollected), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard("Outstanding", money(totalOutstanding), Modifier.weight(1f))
                        SummaryCard("Paid", paidStudents.toString(), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    SummaryCard("Unpaid", unpaidStudents.toString(), Modifier.fillMaxWidth())
                }

                item {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search student") },
                        singleLine = true
                    )
                }

                item {
                    Text("Payment Status", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("All", "Paid", "Unpaid").forEach { status ->
                            FilterChip(
                                selected = paymentFilter == status,
                                onClick = { paymentFilter = status },
                                label = { Text(status) }
                            )
                        }
                    }
                }

                item {
                    Text("Batch", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        batches.take(5).forEach { batch ->
                            FilterChip(
                                selected = selectedBatch == batch,
                                onClick = { selectedBatch = batch },
                                label = { Text(batch) }
                            )
                        }
                    }
                }

                item {
                    Text(
                        "Students (${visibleStudents.size})",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (visibleStudents.isEmpty()) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp)) {
                                Text("No students found.", fontWeight = FontWeight.Bold)
                                Text("Try changing the search or payment filter.")
                            }
                        }
                    }
                }

                items(visibleStudents, key = { it.id }) { student ->
                    val paid = currentPaid(student)
                    val due = maxOf(0, student.monthlyFee - paid)

                    Card(
                        onClick = { selectedStudent = student },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    student.name,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    if (due == 0) "PAID" else "DUE ₹$due",
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${student.className} • ${student.batch}")
                            Text("Monthly fee: ₹${student.monthlyFee}")
                            if (student.joiningMonth.isNotBlank()) {
                                Text("Joined: ${student.joiningMonth}")
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { collectStudent = student },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Collect Fee")
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        if (settingsOpen) {
            SettingsDialog(
                students = students,
                onDismiss = { settingsOpen = false },
                onAddStudent = {
                    settingsOpen = false
                    addOpen = true
                },
                onEditStudent = { student ->
                    settingsOpen = false
                    editStudent = student
                },
                onDeleteStudent = { student ->
                    students = students.filter { it.id != student.id }
                    payments = payments.filter { it.studentId != student.id }
                    store.saveStudents(students)
                    store.savePayments(payments)
                },
                onBackup = {
                    shareReceipt(context, exportBackup(context))
                },
                onRestore = {
                    settingsOpen = false
                    (context as? MainActivity)?.pickBackup { ok ->
                        if (ok) {
                            students = store.loadStudents()
                            payments = store.loadPayments()
                        }
                        Toast.makeText(
                            context,
                            if (ok) "Backup restored successfully." else "Restore failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                onReports = {
                    settingsOpen = false
                    reportOpen = true
                }
            )
        }

        if (addOpen) {
            StudentEditorDialog(
                title = "Add Student",
                initialStudent = null,
                onDismiss = { addOpen = false },
                onSave = { student ->
                    students = students + student
                    store.saveStudents(students)
                    addOpen = false
                }
            )
        }

        editStudent?.let { student ->
            StudentEditorDialog(
                title = "Edit Student",
                initialStudent = student,
                onDismiss = { editStudent = null },
                onSave = { updated ->
                    students = students.map { if (it.id == updated.id) updated else it }
                    store.saveStudents(students)
                    editStudent = null
                }
            )
        }

        selectedStudent?.let { student ->
            StudentDetailsDialog(
                student = student,
                payments = payments.filter { it.studentId == student.id },
                outstanding = outstanding(student),
                onDismiss = { selectedStudent = null },
                onCollect = {
                    collectStudent = student
                    selectedStudent = null
                }
            )
        }

        collectStudent?.let { student ->
            CollectFeeDialog(
                student = student,
                onDismiss = { collectStudent = null },
                onSave = { month, amount ->
                    val payment = Payment(
                        student.id,
                        month,
                        amount,
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    )
                    payments = payments + payment
                    store.savePayments(payments)
                    val receiptNo = "TMG-" +
                        SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) +
                        "-" + payments.size.toString().padStart(5, '0')
                    receiptUri = createReceiptPdf(context, student, payment, receiptNo)
                    collectStudent = null
                }
            )
        }

        receiptUri?.let { uri ->
            AlertDialog(
                onDismissRequest = { receiptUri = null },
                title = { Text("Payment Saved ✓") },
                text = { Text("Your professional PDF receipt has been created.") },
                confirmButton = {
                    Button(onClick = {
                        shareReceiptWhatsApp(context, uri)
                        receiptUri = null
                    }) { Text("WhatsApp") }
                },
                dismissButton = {
                    TextButton(onClick = {
                        shareReceipt(context, uri)
                        receiptUri = null
                    }) { Text("Other Share") }
                }
            )
        }

        if (reportOpen) {
            ReportsDialog(students, payments) { reportOpen = false }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    students: List<Student>,
    onDismiss: () -> Unit,
    onAddStudent: () -> Unit,
    onEditStudent: (Student) -> Unit,
    onDeleteStudent: (Student) -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onReports: () -> Unit
) {
    var deleteStudent by remember { mutableStateOf<Student?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Settings", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Student Management", fontWeight = FontWeight.Bold)

                Button(
                    onClick = onAddStudent,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Add Student") }

                HorizontalDivider()

                if (students.isEmpty()) {
                    Text("No students added yet.")
                } else {
                    students.sortedBy { it.name.lowercase() }.forEach { student ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text(student.name, fontWeight = FontWeight.Bold)
                                Text("${student.className} • ${student.batch}")
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(onClick = { onEditStudent(student) }) {
                                        Text("Edit")
                                    }
                                    TextButton(onClick = { deleteStudent = student }) {
                                        Text("Delete")
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()
                Text("Data & Reports", fontWeight = FontWeight.Bold)

                OutlinedButton(
                    onClick = onBackup,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Backup Data") }

                OutlinedButton(
                    onClick = onRestore,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Restore Data") }

                OutlinedButton(
                    onClick = onReports,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reports") }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )

    deleteStudent?.let { student ->
        AlertDialog(
            onDismissRequest = { deleteStudent = null },
            title = { Text("Delete Student?") },
            text = {
                Text("Delete ${student.name} and all of this student's payment history? This cannot be undone.")
            },
            confirmButton = {
                Button(onClick = {
                    onDeleteStudent(student)
                    deleteStudent = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteStudent = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SummaryCard(title: String, value: String, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(title, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

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
    var cls by remember { mutableStateOf(if (existing.isBlank() || existing in standardClasses) existing else "Others") }
    var otherClass by remember { mutableStateOf(if (existing.isNotBlank() && existing !in standardClasses) existing else "") }
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
                OutlinedTextField(name, { name = it }, label = { Text("Student name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { classOpen = true }, Modifier.fillMaxWidth()) {
                        Text(if (cls.isBlank()) "Select Class" else cls)
                    }
                    DropdownMenu(classOpen, { classOpen = false }) {
                        standardClasses.forEach { c ->
                            DropdownMenuItem({ Text(c) }, { cls = c; classOpen = false })
                        }
                        DropdownMenuItem({ Text("Others") }, { cls = "Others"; classOpen = false })
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
                    OutlinedButton(onClick = { joiningOpen = true }, Modifier.fillMaxWidth()) {
                        Text(if (joiningMonth.isBlank()) "Select Joining Month" else "Joining Month: $joiningMonth")
                    }
                    DropdownMenu(joiningOpen, { joiningOpen = false }) {
                        months.forEach { m ->
                            DropdownMenuItem({ Text(m) }, { joiningMonth = m; joiningOpen = false })
                        }
                        DropdownMenuItem({ Text("Enter manually") }, { joiningOpen = false; otherMonth = true })
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

                OutlinedTextField(batch, { batch = it }, label = { Text("Batch") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.name, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("${student.className} • ${student.batch}")
                Text("Monthly fee: ₹${student.monthlyFee}")
                if (student.joiningMonth.isNotBlank()) {
                    Text("Joining Month: ${student.joiningMonth}")
                }
                if (student.phone.isNotBlank()) {
                    Text("Phone: ${student.phone}")
                }
                HorizontalDivider()
                Text("Total Outstanding: ₹$outstanding", fontWeight = FontWeight.Bold)
                Text("Payment History", fontWeight = FontWeight.Bold)
                if (payments.isEmpty()) {
                    Text("No payments recorded.")
                } else {
                    payments.reversed().forEach { p ->
                        Card {
                            Column(Modifier.padding(10.dp)) {
                                Text(p.month, fontWeight = FontWeight.Bold)
                                Text("Paid: ₹${p.amount}")
                                Text("Date: ${p.date}")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onCollect) { Text("Collect Fee") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun CollectFeeDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {
    var month by remember { mutableStateOf(currentMonth()) }
    var amount by remember { mutableStateOf(student.monthlyFee.toString()) }
    var monthOpen by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collect Fee") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(student.name, fontWeight = FontWeight.Bold)
                Text("Monthly fee: ₹${student.monthlyFee}")
                Box(Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { monthOpen = true }, Modifier.fillMaxWidth()) {
                        Text("Fee Month: $month")
                    }
                    DropdownMenu(monthOpen, { monthOpen = false }) {
                        recentMonths(24).forEach { m ->
                            DropdownMenuItem({ Text(m) }, { month = m; monthOpen = false })
                        }
                    }
                }
                OutlinedTextField(
                    amount, { amount = it },
                    label = { Text("Amount paid") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Text("Partial payment is allowed.")
            }
        },
        confirmButton = {
            Button(
                enabled = month.isNotBlank() && amount.toIntOrNull()?.let { it > 0 } == true,
                onClick = { onSave(month.trim(), amount.toInt()) }
            ) { Text("Save Payment") }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancel") } }
    )
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
                    listOf("Monthly", "Batch", "Students").forEach { r ->
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
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(month)
                                Text("₹$total", fontWeight = FontWeight.Bold)
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
