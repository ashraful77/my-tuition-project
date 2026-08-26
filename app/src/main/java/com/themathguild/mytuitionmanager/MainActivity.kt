package com.themathguild.mytuitionmanager

import android.content.Context
import android.os.Bundle
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.app.Activity
import android.content.ContextWrapper
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class Student(
    val id: Long,
    val name: String,
    val className: String,
    val batch: String,
    val monthlyFee: Int,
    val phone: String
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
            Student(o.getLong("id"), o.getString("name"), o.getString("className"),
                o.getString("batch"), o.getInt("monthlyFee"), o.optString("phone"))
        }
    }

    fun saveStudents(list: List<Student>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("id", it.id); put("name", it.name); put("className", it.className)
                put("batch", it.batch); put("monthlyFee", it.monthlyFee); put("phone", it.phone)
            })
        }
        prefs.edit().putString("students", a.toString()).apply()
    }

    fun loadPayments(): List<Payment> {
        val a = JSONArray(prefs.getString("payments", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Payment(o.getLong("studentId"), o.getString("month"), o.getInt("amount"), o.getString("date"))
        }
    }

    fun savePayments(list: List<Payment>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("studentId", it.studentId); put("month", it.month)
                put("amount", it.amount); put("date", it.date)
            })
        }
        prefs.edit().putString("payments", a.toString()).apply()
    }
}


fun createReceiptPdf(context: Context, student: Student, payment: Payment, receiptNo: String): Uri {
    val doc = PdfDocument()
    val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
    val canvas = page.canvas
    val paint = Paint().apply { isAntiAlias = true }
    var y = 55f

    fun line(text: String, size: Float = 14f, bold: Boolean = false) {
        paint.textSize = size
        paint.isFakeBoldText = bold
        canvas.drawText(text, 45f, y, paint)
        y += size + 12f
    }

    line("THE MATH GUILD", 22f, true)
    line("Mastering the Craft of Problem Solving", 11f)
    line("Fee Payment Receipt", 18f, true)
    y += 10f
    line("Receipt No.: $receiptNo")
    line("Payment Date: ${payment.date}")
    y += 8f
    line("Student: ${student.name}", 15f, true)
    line("Class: ${student.className}")
    line("Batch: ${student.batch}")
    y += 8f
    line("Fee Month: ${payment.month}")
    line("Amount Paid: Rs. ${payment.amount}", 16f, true)
    line("Payment Status: PAID", 15f, true)
    y += 18f
    line("Thank you for your payment.")
    y += 20f
    line("Ashraful Hoque", 13f, true)
    line("Phone: 9732956571", 12f)

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
    context.startActivity(Intent.createChooser(intent, "Share receipt"))
}


fun exportBackup(context: Context): Uri {
    val prefs = context.getSharedPreferences("tuition_data", Context.MODE_PRIVATE)
    val students = prefs.getString("students", "[]") ?: "[]"
    val payments = prefs.getString("payments", "[]") ?: "[]"
    val json = JSONObject().apply {
        put("app", "My Tuition Manager")
        put("version", "1.3")
        put("students", JSONArray(students))
        put("payments", JSONArray(payments))
    }.toString(2)

    val dir = File(context.cacheDir, "backup").apply { mkdirs() }
    val file = File(dir, "MyTuitionManager_Backup.json")
    file.writeText(json)
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun importBackup(context: Context, uri: Uri): Boolean {
    return try {
        val json = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            ?: return false
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
}

class MainActivity : ComponentActivity() {
    private var restoreCallback: ((Boolean) -> Unit)? = null

    private val restoreLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
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

@Composable
fun TuitionApp(store: LocalStore) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var lastReceiptUri by remember { mutableStateOf<Uri?>(null) }

    var students by remember { mutableStateOf(store.loadStudents()) }
    var payments by remember { mutableStateOf(store.loadPayments()) }
    var selected by remember { mutableStateOf<Student?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var collectOpen by remember { mutableStateOf<Student?>(null) }

    val currentMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    val currentPaid = payments.filter { it.month == currentMonth }.sumOf { it.amount }
    val currentDue = students.sumOf { s ->
        maxOf(0, s.monthlyFee - payments.filter { it.studentId == s.id && it.month == currentMonth }.sumOf { it.amount })
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("The Math Guild", fontWeight = FontWeight.Bold)
                            Text("My Tuition Manager", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { addOpen = true }) { Text("+") }
            }
        ) { pad ->
            LazyColumn(
                Modifier.fillMaxSize().padding(pad).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Dashboard", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryCard("Students", students.size.toString(), Modifier.weight(1f))
                        SummaryCard("Collected", "₹$currentPaid", Modifier.weight(1f))
                        SummaryCard("Due", "₹$currentDue", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Students", style = MaterialTheme.typography.titleLarge)
                }
                items(students) { s ->
                    Card(Modifier.fillMaxWidth(), onClick = { selected = s }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(s.name, fontWeight = FontWeight.Bold)
                            Text("${s.className} • ${s.batch}")
                            Text("Monthly fee: ₹${s.monthlyFee}")
                            Button(onClick = { collectOpen = s }) { Text("Collect Fee") }
                        }
                    }
                }
                item {
                    Spacer(Modifier.height(12.dp))
                    Text("Backup & Restore", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            val uri = exportBackup(context)
                            shareReceipt(context, uri)
                        }) { Text("Backup") }
                        OutlinedButton(onClick = {
                            (context as? MainActivity)?.pickBackup { ok ->
                                android.widget.Toast.makeText(
                                    context,
                                    if (ok) "Backup restored. Restart the app to refresh." else "Restore failed.",
                                    android.widget.Toast.LENGTH_LONG
                                ).show()
                            }
                        }) { Text("Restore") }
                    }
                }
            }
        }

        if (addOpen) {
            AddStudentDialog(
                onDismiss = { addOpen = false },
                onAdd = {
                    students = students + it
                    store.saveStudents(students)
                    addOpen = false
                }
            )
        }

        selected?.let { s ->
            StudentDialog(
                student = s,
                payments = payments.filter { it.studentId == s.id },
                onDismiss = { selected = null },
                onCollect = { collectOpen = s; selected = null }
            )
        }

        collectOpen?.let { s ->
            CollectFeeDialog(
                student = s,
                onDismiss = { collectOpen = null },
                onSave = { month, amount ->
                    val payment = Payment(
                        s.id, month, amount,
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
                    )
                    payments = payments + payment
                    store.savePayments(payments)
                    val receiptNo = "TMG-" + SimpleDateFormat("yyyy", Locale.getDefault()).format(Date()) +
                            "-" + payments.size.toString().padStart(5, '0')
                    lastReceiptUri = createReceiptPdf(context, s, payment, receiptNo)
                    collectOpen = null
                }
            )
        }

        lastReceiptUri?.let { uri ->
            AlertDialog(
                onDismissRequest = { lastReceiptUri = null },
                title = { Text("Payment Saved") },
                text = { Text("Your PDF receipt has been created.") },
                confirmButton = {
                    Button(onClick = {
                        shareReceipt(context, uri)
                        lastReceiptUri = null
                    }) { Text("Share Receipt") }
                },
                dismissButton = {
                    TextButton(onClick = { lastReceiptUri = null }) { Text("Close") }
                }
            )
        }

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
fun AddStudentDialog(onDismiss: () -> Unit, onAdd: (Student) -> Unit) {
    var name by remember { mutableStateOf("") }
    var cls by remember { mutableStateOf("") }
    var batch by remember { mutableStateOf("") }
    var fee by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Student") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Student name") })
                OutlinedTextField(cls, { cls = it }, label = { Text("Class") })
                OutlinedTextField(batch, { batch = it }, label = { Text("Batch") })
                OutlinedTextField(fee, { fee = it }, label = { Text("Monthly fee") })
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone (optional)") })
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank() && fee.toIntOrNull() != null,
                onClick = { onAdd(Student(System.currentTimeMillis(), name, cls, batch, fee.toInt(), phone)) }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun StudentDialog(student: Student, payments: List<Payment>, onDismiss: () -> Unit, onCollect: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${student.className} • ${student.batch}")
                Text("Monthly fee: ₹${student.monthlyFee}")
                Text("Payment history", fontWeight = FontWeight.Bold)
                if (payments.isEmpty()) Text("No payments recorded.")
                payments.reversed().forEach { Text("${it.month}: ₹${it.amount} • ${it.date}") }
            }
        },
        confirmButton = { Button(onClick = onCollect) { Text("Collect Fee") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun CollectFeeDialog(student: Student, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    val defaultMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())
    var month by remember { mutableStateOf(defaultMonth) }
    var amount by remember { mutableStateOf(student.monthlyFee.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Collect Fee — ${student.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(month, { month = it }, label = { Text("Fee month") })
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount paid") })
                Text("Monthly fee: ₹${student.monthlyFee}")
            }
        },
        confirmButton = {
            Button(
                enabled = month.isNotBlank() && amount.toIntOrNull() != null,
                onClick = { onSave(month, amount.toInt()) }
            ) { Text("Save Payment") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
