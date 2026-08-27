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
    val phone: String
)

data class Payment(
    val studentId: Long,
    val month: String,
    val amount: Int,
    val date: String
)

class LocalStore(context: Context) {

    private val prefs =
        context.getSharedPreferences(
            "tuition_data",
            Context.MODE_PRIVATE
        )

    fun loadStudents(): List<Student> {

        val a =
            JSONArray(
                prefs.getString(
                    "students",
                    "[]"
                )
            )

        return List(a.length()) { i ->

            val o =
                a.getJSONObject(i)

            Student(
                o.getLong("id"),
                o.getString("name"),
                o.getString("className"),
                o.getString("batch"),
                o.getInt("monthlyFee"),
                o.optString("phone")
            )
        }
    }

    fun saveStudents(
        list: List<Student>
    ) {

        val a =
            JSONArray()

        list.forEach {

            a.put(
                JSONObject().apply {

                    put("id", it.id)
                    put("name", it.name)
                    put("className", it.className)
                    put("batch", it.batch)
                    put("monthlyFee", it.monthlyFee)
                    put("phone", it.phone)
                }
            )
        }

        prefs.edit()
            .putString(
                "students",
                a.toString()
            )
            .apply()
    }

    fun loadPayments(): List<Payment> {

        val a =
            JSONArray(
                prefs.getString(
                    "payments",
                    "[]"
                )
            )

        return List(a.length()) { i ->

            val o =
                a.getJSONObject(i)

            Payment(
                o.getLong("studentId"),
                o.getString("month"),
                o.getInt("amount"),
                o.getString("date")
            )
        }
    }

    fun savePayments(
        list: List<Payment>
    ) {

        val a =
            JSONArray()

        list.forEach {

            a.put(
                JSONObject().apply {

                    put(
                        "studentId",
                        it.studentId
                    )

                    put(
                        "month",
                        it.month
                    )

                    put(
                        "amount",
                        it.amount
                    )

                    put(
                        "date",
                        it.date
                    )
                }
            )
        }

        prefs.edit()
            .putString(
                "payments",
                a.toString()
            )
            .apply()
    }
}

/* =========================================================
   DATE HELPERS
========================================================= */

fun currentMonth(): String {

    return SimpleDateFormat(
        "MMMM yyyy",
        Locale.ENGLISH
    ).format(Date())
}

fun monthKey(
    month: String
): YearMonth? {

    return try {

        YearMonth.parse(
            month,
            DateTimeFormatter.ofPattern(
                "MMMM yyyy",
                Locale.ENGLISH
            )
        )

    } catch (_: Exception) {

        null
    }
}

fun money(
    value: Int
): String {

    return "₹$value"
}

/* =========================================================
   RECEIPT PDF
========================================================= */

fun createReceiptPdf(
    context: Context,
    student: Student,
    payment: Payment,
    receiptNo: String
): Uri {

    val doc =
        PdfDocument()

    val page =
        doc.startPage(
            PdfDocument.PageInfo
                .Builder(
                    595,
                    842,
                    1
                )
                .create()
        )

    val canvas =
        page.canvas

    val paint =
        Paint().apply {
            isAntiAlias = true
        }

    var y = 55f

    fun text(
        value: String,
        size: Float = 14f,
        bold: Boolean = false
    ) {

        paint.textSize =
            size

        paint.isFakeBoldText =
            bold

        canvas.drawText(
            value,
            55f,
            y,
            paint
        )

        y +=
            size + 12f
    }

    fun separator() {

        canvas.drawLine(
            55f,
            y,
            540f,
            y,
            paint
        )

        y += 20f
    }

    text(
        "THE MATH GUILD",
        25f,
        true
    )

    text(
        "Mastering the Craft of Problem Solving",
        11f
    )

    y += 8f

    text(
        "FEE PAYMENT RECEIPT",
        20f,
        true
    )

    y += 10f

    separator()

    text(
        "Receipt No.: $receiptNo",
        13f,
        true
    )

    text(
        "Payment Date: ${payment.date}"
    )

    y += 8f

    separator()

    text(
        "STUDENT DETAILS",
        15f,
        true
    )

    text(
        "Student: ${student.name}",
        14f,
        true
    )

    text(
        "Class: ${student.className}"
    )

/* ---------------------------------------------------------
   PROFESSIONAL RECEIPT PDF
--------------------------------------------------------- */

fun createReceiptPdf(
    context: Context,
    student: Student,
    payment: Payment,
    receiptNo: String
): Uri {

    val doc = PdfDocument()

    val pageInfo =
        PdfDocument.PageInfo
            .Builder(595, 842, 1)
            .create()

    val page =
        doc.startPage(pageInfo)

    val canvas = page.canvas

    val paint = Paint().apply {
        isAntiAlias = true
    }

    val left = 45f
    val right = 550f
    val width = right - left

    var y = 45f

    /* -----------------------------------------------------
       HELPERS
    ----------------------------------------------------- */

    fun text(
        value: String,
        x: Float = left,
        size: Float = 13f,
        bold: Boolean = false
    ) {

        paint.style = Paint.Style.FILL
        paint.textSize = size
        paint.isFakeBoldText = bold

        canvas.drawText(
            value,
            x,
            y,
            paint
        )
    }

    fun centered(
        value: String,
        size: Float,
        bold: Boolean = false
    ) {

        paint.style = Paint.Style.FILL
        paint.textSize = size
        paint.isFakeBoldText = bold

        val x =
            (595f -
                    paint.measureText(value)) / 2f

        canvas.drawText(
            value,
            x,
            y,
            paint
        )
    }

    fun line() {

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f

        canvas.drawLine(
            left,
            y,
            right,
            y,
            paint
        )

        paint.style = Paint.Style.FILL
    }

    fun box(
        top: Float,
        bottom: Float
    ) {

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f

        canvas.drawRect(
            left,
            top,
            right,
            bottom,
            paint
        )

        paint.style = Paint.Style.FILL
    }

    /* -----------------------------------------------------
       HEADER
    ----------------------------------------------------- */

    centered(
        "THE MATH GUILD",
        28f,
        true
    )

    y += 23f

    centered(
        "Mastering the Craft of Problem Solving",
        11f
    )

    y += 28f

    centered(
        "FEE PAYMENT RECEIPT",
        19f,
        true
    )

    y += 25f

    line()

    y += 22f

    /* -----------------------------------------------------
       RECEIPT INFORMATION
    ----------------------------------------------------- */

    text(
        "Receipt No.: $receiptNo",
        left,
        12f,
        true
    )

    text(
        "Date: ${payment.date}",
        365f,
        12f,
        true
    )

    y += 25f

    line()

    y += 25f

    /* -----------------------------------------------------
       STUDENT DETAILS
    ----------------------------------------------------- */

    text(
        "STUDENT DETAILS",
        left,
        15f,
        true
    )

    y += 24f

    box(
        y - 18f,
        y + 105f
    )

    text(
        "Student Name",
        left + 15f,
        10f
    )

    text(
        student.name,
        left + 15f,
        14f,
        true
    )

    y += 35f

    text(
        "Class",
        left + 15f,
        10f
    )

    text(
        student.className,
        left + 15f,
        13f,
        true
    )

    text(
        "Batch",
        300f,
        10f
    )

    text(
        student.batch,
        300f,
        13f,
        true
    )

    y += 35f

    text(
        "Phone",
        left + 15f,
        10f
    )

    text(
        if (student.phone.isBlank())
            "Not provided"
        else
            student.phone,
        left + 15f,
        13f
    )

    y += 40f

    /* -----------------------------------------------------
       PAYMENT DETAILS
    ----------------------------------------------------- */

    text(
        "PAYMENT DETAILS",
        left,
        15f,
        true
    )

    y += 25f

    box(
        y - 18f,
        y + 125f
    )

    text(
        "Fee Month",
        left + 15f,
        11f
    )

    text(
        payment.month,
        300f,
        13f,
        true
    )

    y += 32f

    line()

    y += 28f

    text(
        "Monthly Fee",
        left + 15f,
        11f
    )

    text(
        "₹${student.monthlyFee}",
        430f,
        13f,
        true
    )

    y += 32f

    line()

    y += 28f

    text(
        "AMOUNT PAID",
        left + 15f,
        13f,
        true
    )

    text(
        "₹${payment.amount}",
        410f,
        18f,
        true
    )

    y += 40f

    /* -----------------------------------------------------
       STATUS
    ----------------------------------------------------- */

    box(
        y - 18f,
        y + 35f
    )

    text(
        "PAYMENT STATUS",
        left + 15f,
        11f,
        true
    )

    text(
        "PAID",
        450f,
        14f,
        true
    )

    y += 65f

    /* -----------------------------------------------------
       THANK YOU
    ----------------------------------------------------- */

    centered(
        "Thank you for your payment.",
        12f
    )

    y += 40f

    line()

    y += 35f

    /* -----------------------------------------------------
       FOOTER
    ----------------------------------------------------- */

    text(
        "Authorized by",
        left,
        10f
    )

    y += 20f

    text(
        "Ashraful Hoque",
        left,
        13f,
        true
    )

    y += 18f

    text(
        "The Math Guild",
        left,
        11f,
        true
    )

    text(
        "Phone: 9732956571",
        365f,
        11f
    )

    y += 30f

    centered(
        "This is a computer-generated receipt.",
        9f
    )

    /* -----------------------------------------------------
       FINISH PDF
    ----------------------------------------------------- */

    doc.finishPage(page)

    val dir =
        File(
            context.cacheDir,
            "receipts"
        ).apply {
            mkdirs()
        }

    val file =
        File(
            dir,
            "$receiptNo.pdf"
        )

    file.outputStream().use { output ->
        doc.writeTo(output)
    }

    doc.close()

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

/* =========================================================
   SHARE
========================================================= */

fun shareReceipt(
    context: Context,
    uri: Uri
) {

    val intent =
        Intent(
            Intent.ACTION_SEND
        ).apply {

            type =
                "application/pdf"

            putExtra(
                Intent.EXTRA_STREAM,
                uri
            )

            addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }

    context.startActivity(
        Intent.createChooser(
            intent,
            "Share Receipt"
        )
    )
}

fun shareReceiptWhatsApp(
    context: Context,
    uri: Uri
) {

    try {

        val intent =
            Intent(
                Intent.ACTION_SEND
            ).apply {

                type =
                    "application/pdf"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                setPackage(
                    "com.whatsapp"
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        context.startActivity(intent)

    } catch (_: Exception) {

        Toast.makeText(
            context,
            "WhatsApp is not available.",
            Toast.LENGTH_LONG
        ).show()
    }
}

/* =========================================================
   BACKUP
========================================================= */

fun exportBackup(
    context: Context
): Uri {

    val prefs =
        context.getSharedPreferences(
            "tuition_data",
            Context.MODE_PRIVATE
        )

    val students =
        prefs.getString(
            "students",
            "[]"
        ) ?: "[]"

    val payments =
        prefs.getString(
            "payments",
            "[]"
        ) ?: "[]"

    val json =
        JSONObject().apply {

            put(
                "app",
                "My Tuition Manager"
            )

            put(
                "version",
                "2.0"
            )

            put(
                "backupDate",
                SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    Locale.getDefault()
                ).format(Date())
            )

            put(
                "students",
                JSONArray(students)
            )

            put(
                "payments",
                JSONArray(payments)
            )
        }.toString(2)

    val dir =
        File(
            context.cacheDir,
            "backup"
        ).apply {
            mkdirs()
        }

    val file =
        File(
            dir,
            "MyTuitionManager_Backup.json"
        )

    file.writeText(json)

    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}

fun importBackup(
    context: Context,
    uri: Uri
): Boolean {

    return try {

        val json =
            context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.use {
                    it.readText()
                }
                ?: return false

        val obj =
            JSONObject(json)

        val prefs =
            context.getSharedPreferences(
                "tuition_data",
                Context.MODE_PRIVATE
            )

        prefs.edit()
            .putString(
                "students",
                obj.optJSONArray(
                    "students"
                )?.toString() ?: "[]"
            )
            .putString(
                "payments",
                obj.optJSONArray(
                    "payments"
                )?.toString() ?: "[]"
            )
            .apply()

        true

    } catch (_: Exception) {

        false
    }
}

/* =========================================================
   MAIN ACTIVITY
========================================================= */

class MainActivity :
    ComponentActivity() {

    private var restoreCallback:
            ((Boolean) -> Unit)? =
        null

    private val restoreLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->

            val ok =
                uri?.let {

                    importBackup(
                        this,
                        it
                    )

                } ?: false

            restoreCallback?.invoke(
                ok
            )

            restoreCallback = null
        }

    fun pickBackup(
        callback: (Boolean) -> Unit
    ) {

        restoreCallback =
            callback

        restoreLauncher.launch(
            arrayOf(
                "application/json",
                "text/*"
            )
        )
    }

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContent {

            TuitionApp(
                LocalStore(this)
            )
        }
    }
}

/* =========================================================
   MAIN APP
========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuitionApp(
    store: LocalStore
) {

    val context =
        androidx.compose.ui.platform
            .LocalContext.current

    var students by remember {
        mutableStateOf(
            store.loadStudents()
        )
    }

    var payments by remember {
        mutableStateOf(
            store.loadPayments()
        )
    }

    var search by remember {
        mutableStateOf("")
    }

    var selectedBatch by remember {
        mutableStateOf("All")
    }

    var selectedStudent by remember {
        mutableStateOf<Student?>(null)
    }

    var addOpen by remember {
        mutableStateOf(false)
    }

    var editStudent by remember {
        mutableStateOf<Student?>(null)
    }

    var collectStudent by remember {
        mutableStateOf<Student?>(null)
    }

    var reportOpen by remember {
        mutableStateOf(false)
    }

    var receiptUri by remember {
        mutableStateOf<Uri?>(null)
    }

    val thisMonth =
        currentMonth()

    val currentPaid =
        payments
            .filter {
                it.month.equals(
                    thisMonth,
                    true
                )
            }
            .sumOf {
                it.amount
            }

    fun studentOutstanding(
        student: Student
    ): Int {

        val studentPayments =
            payments.filter {
                it.studentId ==
                    student.id
            }

        val firstPaymentMonth =
            studentPayments
                .mapNotNull {
                    monthKey(it.month)
                }
                .minOrNull()

        val firstMonth =
            firstPaymentMonth
                ?: YearMonth.now()

        var month =
            firstMonth

        val now =
            YearMonth.now()

        var totalDue =
            0

        while (
            !month.isAfter(now)
        ) {

            val monthName =
                month.format(
                    DateTimeFormatter.ofPattern(
                        "MMMM yyyy",
                        Locale.ENGLISH
                    )
                )

            val paid =
                studentPayments
                    .filter {
                        it.month.equals(
                            monthName,
                            true
                        )
                    }
                    .sumOf {
                        it.amount
                    }

            totalDue +=
                maxOf(
                    0,
                    student.monthlyFee - paid
                )

            month =
                month.plusMonths(1)
        }

        return totalDue
    }

    val totalOutstanding =
        students.sumOf {
            studentOutstanding(it)
        }

    val paidStudents =
        students.count { student ->

            val paid =
                payments
                    .filter {
                        it.studentId ==
                                student.id &&
                        it.month.equals(
                            thisMonth,
                            true
                        )
                    }
                    .sumOf {
                        it.amount
                    }

            paid >=
                student.monthlyFee
        }

    val unpaidStudents =
        students.size -
                paidStudents

    val batches =
        listOf("All") +
                students
                    .map {
                        it.batch
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()
                    .sorted()

    val visibleStudents =
        students
            .filter {

                it.name.contains(
                    search,
                    true
                ) ||

                it.className.contains(
                    search,
                    true
                ) ||

                it.batch.contains(
                    search,
                    true
                ) ||

                it.phone.contains(
                    search,
                    true
                )
            }
            .filter {

                selectedBatch ==
                        "All" ||
                        it.batch ==
                        selectedBatch
            }
            .sortedBy {
                it.name.lowercase()
            }

    MaterialTheme {

        Scaffold(

            topBar = {

                CenterAlignedTopAppBar(

                    title = {

                        Column(
                            horizontalAlignment =
                                Alignment.CenterHorizontally
                        ) {

                            Text(
                                "The Math Guild",
                                fontWeight =
                                    FontWeight.Bold
                            )

                            Text(
                                "My Tuition Manager",
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelSmall
                            )
                        }
                    }
                )
            },

            floatingActionButton = {

                FloatingActionButton(
                    onClick = {
                        addOpen = true
                    }
                ) {

                    Text(
                        "+",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )
                }
            }

        ) { padding ->

            LazyColumn(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    )
            ) {

                /* =================================================
                   DASHBOARD
                ================================================= */

                item {

                    Text(
                        "Dashboard",
                        style =
                            MaterialTheme
                                .typography
                                .headlineSmall
                    )

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        SummaryCard(
                            "Students",
                            students.size.toString(),
                            Modifier.weight(1f)
                        )

                        SummaryCard(
                            "Collected",
                            money(currentPaid),
                            Modifier.weight(1f)
                        )

                        SummaryCard(
                            "Outstanding",
                            money(totalOutstanding),
                            Modifier.weight(1f)
                        )
                    }

                    Spacer(
                        Modifier.height(8.dp)
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        SummaryCard(
                            "Paid",
                            paidStudents.toString(),
                            Modifier.weight(1f)
                        )

                        SummaryCard(
                            "Unpaid",
                            unpaidStudents.toString(),
                            Modifier.weight(1f)
                        )
                    }
                }

                /* =================================================
                   SEARCH
                ================================================= */

                item {

                    OutlinedTextField(

                        value = search,

                        onValueChange = {
                            search = it
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        label = {
                            Text(
                                "Search student"
                            )
                        },

                        singleLine = true
                    )
                }

                /* =================================================
                   BATCH FILTER
                ================================================= */

                item {

                    Text(
                        "Batch",
                        fontWeight =
                            FontWeight.Bold
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                6.dp
                            )
                    ) {

                        batches
                            .take(5)
                            .forEach { batch ->

                                FilterChip(

                                    selected =
                                        selectedBatch ==
                                                batch,

                                    onClick = {

                                        selectedBatch =
                                            batch
                                    },

                                    label = {
                                        Text(batch)
                                    }
                                )
                            }
                    }
                }

                item {

                    Text(
                        "Students",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )
                }

                /* =================================================
                   STUDENTS
                ================================================= */

                items(
                    visibleStudents,
                    key = {
                        it.id
                    }
                ) { student ->

                    val monthlyPaid =
                        payments
                            .filter {

                                it.studentId ==
                                        student.id &&
                                it.month.equals(
                                    thisMonth,
                                    true
                                )
                            }
                            .sumOf {
                                it.amount
                            }

                    val monthlyDue =
                        maxOf(
                            0,
                            student.monthlyFee -
                                    monthlyPaid
                        )

                    Card(

                        onClick = {

                            selectedStudent =
                                student
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Column(
                            Modifier.padding(
                                16.dp
                            )
                        ) {

                            Row(

                                modifier =
                                    Modifier.fillMaxWidth(),

                                horizontalArrangement =
                                    Arrangement
                                        .SpaceBetween
                            ) {

                                Text(
                                    student.name,
                                    fontWeight =
                                        FontWeight.Bold,
                                    style =
                                        MaterialTheme
                                            .typography
                                            .titleMedium
                                )

                                Text(
                                    if (
                                        monthlyDue == 0
                                    )
                                        "PAID"
                                    else
                                        "DUE ₹$monthlyDue",
                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }

                            Text(
                                "${student.className} • ${student.batch}"
                            )

                            Text(
                                "Monthly fee: ₹${student.monthlyFee}"
                            )

                            if (
                                student.phone.isNotBlank()
                            ) {

                                Text(
                                    "Phone: ${student.phone}"
                                )
                            }

                            Spacer(
                                Modifier.height(8.dp)
                            )

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(
                                        8.dp
                                    )
                            ) {

                                Button(
                                    onClick = {

                                        collectStudent =
                                            student
                                    }
                                ) {

                                    Text(
                                        "Collect Fee"
                                    )
                                }

                                OutlinedButton(
                                    onClick = {

                                        editStudent =
                                            student
                                    }
                                ) {

                                    Text(
                                        "Edit"
                                    )
                                }
                            }
                        }
                    }
                }

                /* =================================================
                   REPORTS
                ================================================= */

                item {

                    Button(

                        onClick = {
                            reportOpen = true
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(
                            "Reports"
                        )
                    }
                }

                /* =================================================
                   BACKUP
                ================================================= */

                item {

                    Text(
                        "Backup & Restore",
                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        Button(

                            onClick = {

                                val uri =
                                    exportBackup(
                                        context
                                    )

                                shareReceipt(
                                    context,
                                    uri
                                )
                            }

                        ) {

                            Text(
                                "Backup"
                            )
                        }

                        OutlinedButton(

                            onClick = {

                                (
                                    context as?
                                        MainActivity
                                )?.pickBackup { ok ->

                                    Toast.makeText(

                                        context,

                                        if (ok)
                                            "Backup restored. Please restart the app."
                                        else
                                            "Restore failed.",

                                        Toast.LENGTH_LONG

                                    ).show()
                                }
                            }

                        ) {

                            Text(
                                "Restore"
                            )
                        }
                    }
                }
            }
        }

        /* =================================================
           ADD
        ================================================= */

        if (addOpen) {

            StudentEditorDialog(

                title =
                    "Add Student",

                initialStudent =
                    null,

                onDismiss = {
                    addOpen = false
                },

                onSave = { student ->

                    students =
                        students + student

                    store.saveStudents(
                        students
                    )

                    addOpen = false
                }
            )
        }

        /* =================================================
           EDIT
        ================================================= */

        editStudent?.let { student ->

            StudentEditorDialog(

                title =
                    "Edit Student",

                initialStudent =
                    student,

                onDismiss = {
                    editStudent = null
                },

                onSave = { updated ->

                    students =
                        students.map {

                            if (
                                it.id ==
                                    updated.id
                            )
                                updated
                            else
                                it
                        }

                    store.saveStudents(
                        students
                    )

                    editStudent = null
                }
            )
        }

        /* =================================================
           DETAILS
        ================================================= */

        selectedStudent?.let { student ->

            StudentDetailsDialog(

                student =
                    student,

                payments =
                    payments.filter {
                        it.studentId ==
                                student.id
                    },

                outstanding =
                    studentOutstanding(
                        student
                    ),

                onDismiss = {
                    selectedStudent = null
                },

                onCollect = {

                    collectStudent =
                        student

                    selectedStudent =
                        null
                },

                onEdit = {

                    editStudent =
                        student

                    selectedStudent =
                        null
                },

                onDelete = {

                    students =
                        students.filter {
                            it.id !=
                                    student.id
                        }

                    payments =
                        payments.filter {
                            it.studentId !=
                                    student.id
                        }

                    store.saveStudents(
                        students
                    )

                    store.savePayments(
                        payments
                    )

                    selectedStudent = null
                }
            )
        }

        /* =================================================
           COLLECT
        ================================================= */

        collectStudent?.let { student ->

            CollectFeeDialog(

                student =
                    student,

                onDismiss = {
                    collectStudent = null
                },

                onSave = { month, amount ->

                    val payment =
                        Payment(

                            student.id,

                            month,

                            amount,

                            SimpleDateFormat(
                                "dd/MM/yyyy",
                                Locale.getDefault()
                            ).format(Date())
                        )

                    payments =
                        payments + payment

                    store.savePayments(
                        payments
                    )

                    val receiptNo =
                        "TMG-" +
                                SimpleDateFormat(
                                    "yyyy",
                                    Locale.getDefault()
                                ).format(Date()) +
                                "-" +
                                payments.size
                                    .toString()
                                    .padStart(
                                        5,
                                        '0'
                                    )

                    receiptUri =
                        createReceiptPdf(
                            context,
                            student,
                            payment,
                            receiptNo
                        )

                    collectStudent =
                        null
                }
            )
        }

        /* =================================================
           RECEIPT
        ================================================= */

        receiptUri?.let { uri ->

            AlertDialog(

                onDismissRequest = {
                    receiptUri = null
                },

                title = {
                    Text(
                        "Payment Saved ✓"
                    )
                },

                text = {
                    Text(
                        "Your professional PDF receipt has been created."
                    )
                },

                confirmButton = {

                    Button(
                        onClick = {

                            shareReceiptWhatsApp(
                                context,
                                uri
                            )

                            receiptUri =
                                null
                        }
                    ) {

                        Text(
                            "WhatsApp"
                        )
                    }
                },

                dismissButton = {

                    TextButton(
                        onClick = {

                            shareReceipt(
                                context,
                                uri
                            )

                            receiptUri =
                                null
                        }
                    ) {

                        Text(
                            "Other Share"
                        )
                    }
                }
            )
        }

        /* =================================================
           REPORTS
        ================================================= */

        if (reportOpen) {

            ReportsDialog(

                students =
                    students,

                payments =
                    payments,

                onDismiss = {
                    reportOpen = false
                }
            )
        }
    }
}

/* =========================================================
   SUMMARY CARD
========================================================= */

@Composable
fun SummaryCard(
    title: String,
    value: String,
    modifier: Modifier
) {

    Card(
        modifier = modifier
    ) {

        Column(
            Modifier.padding(12.dp)
        ) {

            Text(
                title,
                style =
                    MaterialTheme
                        .typography
                        .labelSmall
            )

            Text(
                value,
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold
            )
        }
    }
}

/* =========================================================
   STUDENT EDITOR
   CLASS: V-X + OTHERS
========================================================= */

@Composable
fun StudentEditorDialog(
    title: String,
    initialStudent: Student?,
    onDismiss: () -> Unit,
    onSave: (Student) -> Unit
) {

    val standardClasses =
        listOf(
            "V",
            "VI",
            "VII",
            "VIII",
            "IX",
            "X"
        )

    val existingClass =
        initialStudent?.className ?: ""

    var cls by remember {

        mutableStateOf(

            if (
                existingClass.isBlank() ||
                existingClass in standardClasses
            ) {

                existingClass

            } else {

                "Others"
            }
        )
    }

    var otherClass by remember {

        mutableStateOf(

            if (
                existingClass.isNotBlank() &&
                existingClass !in standardClasses
            ) {

                existingClass

            } else {

                ""
            }
        )
    }

    var classMenuOpen by remember {
        mutableStateOf(false)
    }

    var name by remember {

        mutableStateOf(
            initialStudent?.name ?: ""
        )
    }

    var batch by remember {

        mutableStateOf(
            initialStudent?.batch ?: ""
        )
    }

    var fee by remember {

        mutableStateOf(
            initialStudent
                ?.monthlyFee
                ?.toString()
                ?: ""
        )
    }

    var phone by remember {

        mutableStateOf(
            initialStudent?.phone ?: ""
        )
    }

    val finalClass =
        if (cls == "Others")
            otherClass.trim()
        else
            cls.trim()

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {
            Text(title)
        },

        text = {

            Column(

                modifier =
                    Modifier
                        .verticalScroll(
                            rememberScrollState()
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                /* -----------------------------------------
                   NAME
                ----------------------------------------- */

                OutlinedTextField(

                    value = name,

                    onValueChange = {
                        name = it
                    },

                    label = {
                        Text(
                            "Student name"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                /* -----------------------------------------
                   CLASS SELECTOR
                ----------------------------------------- */

                Box(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    OutlinedButton(

                        onClick = {
                            classMenuOpen = true
                        },

                        modifier =
                            Modifier.fillMaxWidth()
                    ) {

                        Text(

                            if (
                                cls.isBlank()
                            )
                                "Select Class"
                            else
                                cls
                        )
                    }

                    DropdownMenu(

                        expanded =
                            classMenuOpen,

                        onDismissRequest = {
                            classMenuOpen = false
                        }
                    ) {

                        standardClasses
                            .forEach { className ->

                                DropdownMenuItem(

                                    text = {
                                        Text(
                                            className
                                        )
                                    },

                                    onClick = {

                                        cls =
                                            className

                                        classMenuOpen =
                                            false
                                    }
                                )
                            }

                        DropdownMenuItem(

                            text = {
                                Text(
                                    "Others"
                                )
                            },

                            onClick = {

                                cls =
                                    "Others"

                                classMenuOpen =
                                    false
                            }
                        )
                    }
                }

                /* -----------------------------------------
                   MANUAL CLASS
                ----------------------------------------- */

                if (
                    cls == "Others"
                ) {

                    OutlinedTextField(

                        value =
                            otherClass,

                        onValueChange = {
                            otherClass = it
                        },

                        label = {
                            Text(
                                "Enter class"
                            )
                        },

                        placeholder = {
                            Text(
                                "e.g. XI, XII, JEE"
                            )
                        },

                        singleLine = true,

                        modifier =
                            Modifier.fillMaxWidth()
                    )
                }

                /* -----------------------------------------
                   BATCH
                ----------------------------------------- */

                OutlinedTextField(

                    value = batch,

                    onValueChange = {
                        batch = it
                    },

                    label = {
                        Text(
                            "Batch"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                /* -----------------------------------------
                   FEE
                ----------------------------------------- */

                OutlinedTextField(

                    value = fee,

                    onValueChange = {
                        fee = it
                    },

                    label = {
                        Text(
                            "Monthly fee"
                        )
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                /* -----------------------------------------
                   PHONE
                ----------------------------------------- */

                OutlinedTextField(

                    value = phone,

                    onValueChange = {
                        phone = it
                    },

                    label = {
                        Text(
                            "Phone"
                        )
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Phone
                        ),

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )
            }
        },

        confirmButton = {

            Button(

                enabled =
                    name.isNotBlank() &&
                            fee.toIntOrNull()
                                != null &&
                            finalClass.isNotBlank(),

                onClick = {

                    val id =
                        initialStudent
                            ?.id
                            ?: System
                                .currentTimeMillis()

                    onSave(

                        Student(

                            id,

                            name.trim(),

                            finalClass,

                            batch.trim(),

                            fee.toInt(),

                            phone.trim()
                        )
                    )
                }

            ) {

                Text(
                    "Save"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Cancel"
                )
            }
        }
    )
}

/* =========================================================
   STUDENT DETAILS
========================================================= */

@Composable
fun StudentDetailsDialog(
    student: Student,
    payments: List<Payment>,
    outstanding: Int,
    onDismiss: () -> Unit,
    onCollect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {

    var deleteConfirm by remember {
        mutableStateOf(false)
    }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                student.name,
                fontWeight =
                    FontWeight.Bold
            )
        },

        text = {

            Column(

                modifier =
                    Modifier.verticalScroll(
                        rememberScrollState()
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        7.dp
                    )
            ) {

                Text(
                    "${student.className} • ${student.batch}"
                )

                Text(
                    "Monthly fee: ₹${student.monthlyFee}"
                )

                if (
                    student.phone.isNotBlank()
                ) {

                    Text(
                        "Phone: ${student.phone}"
                    )
                }

                HorizontalDivider()

                Text(
                    "Total Outstanding: ₹$outstanding",
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "Payment History",
                    fontWeight =
                        FontWeight.Bold
                )

                if (
                    payments.isEmpty()
                ) {

                    Text(
                        "No payments recorded."
                    )

                } else {

                    payments
                        .reversed()
                        .forEach { payment ->

                            Card {

                                Column(
                                    Modifier.padding(
                                        10.dp
                                    )
                                ) {

                                    Text(
                                        payment.month,
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        "Paid: ₹${payment.amount}"
                                    )

                                    Text(
                                        "Date: ${payment.date}"
                                    )
                                }
                            }
                        }
                }
            }
        },

        confirmButton = {

            Button(
                onClick =
                    onCollect
            ) {

                Text(
                    "Collect Fee"
                )
            }
        },

        dismissButton = {

            Row {

                TextButton(
                    onClick =
                        onEdit
                ) {

                    Text(
                        "Edit"
                    )
                }

                TextButton(
                    onClick = {
                        deleteConfirm = true
                    }
                ) {

                    Text(
                        "Delete"
                    )
                }
            }
        }
    )

    if (
        deleteConfirm
    ) {

        AlertDialog(

            onDismissRequest = {
                deleteConfirm = false
            },

            title = {
                Text(
                    "Delete Student?"
                )
            },

            text = {
                Text(
                    "This will delete the student and all payment history. This cannot be undone."
                )
            },

            confirmButton = {

                Button(

                    onClick = {

                        deleteConfirm =
                            false

                        onDelete()
                    }
                ) {

                    Text(
                        "Delete"
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        deleteConfirm =
                            false
                    }
                ) {

                    Text(
                        "Cancel"
                    )
                }
            }
        )
    }
}

/* =========================================================
   COLLECT FEE
========================================================= */

@Composable
fun CollectFeeDialog(
    student: Student,
    onDismiss: () -> Unit,
    onSave: (String, Int) -> Unit
) {

    var month by remember {
        mutableStateOf(
            currentMonth()
        )
    }

    var amount by remember {
        mutableStateOf(
            student.monthlyFee
                .toString()
        )
    }

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {
            Text(
                "Collect Fee"
            )
        },

        text = {

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {

                Text(
                    student.name,
                    fontWeight =
                        FontWeight.Bold
                )

                Text(
                    "Monthly fee: ₹${student.monthlyFee}"
                )

                OutlinedTextField(

                    value = month,

                    onValueChange = {
                        month = it
                    },

                    label = {
                        Text(
                            "Fee month"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                OutlinedTextField(

                    value = amount,

                    onValueChange = {
                        amount = it
                    },

                    label = {
                        Text(
                            "Amount paid"
                        )
                    },

                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType =
                                KeyboardType.Number
                        ),

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                Text(
                    "Partial payment is allowed."
                )
            }
        },

        confirmButton = {

            Button(

                enabled =
                    month.isNotBlank() &&
                            amount.toIntOrNull()
                                != null &&
                            amount.toInt() > 0,

                onClick = {

                    onSave(
                        month.trim(),
                        amount.toInt()
                    )
                }
            ) {

                Text(
                    "Save Payment"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Cancel"
                )
            }
        }
    )
}

/* =========================================================
   REPORTS
========================================================= */

@Composable
fun ReportsDialog(
    students: List<Student>,
    payments: List<Payment>,
    onDismiss: () -> Unit
) {

    var selectedReport by remember {
        mutableStateOf(
            "Monthly"
        )
    }

    val months =
        payments
            .map {
                it.month
            }
            .distinct()
            .sortedDescending()

    AlertDialog(

        onDismissRequest =
            onDismiss,

        title = {

            Text(
                "Reports",
                fontWeight =
                    FontWeight.Bold
            )
        },

        text = {

            Column(

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState()
                        ),

                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                Row(
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            5.dp
                        )
                ) {

                    FilterChip(

                        selected =
                            selectedReport ==
                                    "Monthly",

                        onClick = {
                            selectedReport =
                                "Monthly"
                        },

                        label = {
                            Text(
                                "Monthly"
                            )
                        }
                    )

                    FilterChip(

                        selected =
                            selectedReport ==
                                    "Batch",

                        onClick = {
                            selectedReport =
                                "Batch"
                        },

                        label = {
                            Text(
                                "Batch"
                            )
                        }
                    )

                    FilterChip(

                        selected =
                            selectedReport ==
                                    "Students",

                        onClick = {
                            selectedReport =
                                "Students"
                        },

                        label = {
                            Text(
                                "Students"
                            )
                        }
                    )
                }

                HorizontalDivider()

                /* -----------------------------------------
                   MONTHLY
                ----------------------------------------- */

                if (
                    selectedReport ==
                    "Monthly"
                ) {

                    Text(
                        "Monthly Collection",
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        months.isEmpty()
                    ) {

                        Text(
                            "No payments recorded."
                        )

                    } else {

                        months.forEach {
                            month ->

                            val total =
                                payments
                                    .filter {
                                        it.month ==
                                                month
                                    }
                                    .sumOf {
                                        it.amount
                                    }

                            Card {

                                Row(

                                    Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            12.dp
                                        ),

                                    horizontalArrangement =
                                        Arrangement
                                            .SpaceBetween
                                ) {

                                    Text(
                                        month
                                    )

                                    Text(
                                        "₹$total",
                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                /* -----------------------------------------
                   BATCH
                ----------------------------------------- */

                } else if (
                    selectedReport ==
                    "Batch"
                ) {

                    Text(
                        "Batch-wise Collection",
                        fontWeight =
                            FontWeight.Bold
                    )

                    val batchList =
                        students
                            .map {
                                it.batch
                            }
                            .filter {
                                it.isNotBlank()
                            }
                            .distinct()
                            .sorted()

                    if (
                        batchList.isEmpty()
                    ) {

                        Text(
                            "No batches available."
                        )

                    } else {

                        batchList.forEach {
                            batch ->

                            val batchStudents =
                                students.filter {
                                    it.batch ==
                                            batch
                                }

                            val batchIds =
                                batchStudents.map {
                                    it.id
                                }

                            val total =
                                payments
                                    .filter {
                                        it.studentId in
                                                batchIds
                                    }
                                    .sumOf {
                                        it.amount
                                    }

                            val thisMonth =
                                currentMonth()

                            val paidStudents =
                                batchStudents.count {
                                    student ->

                                    val paid =
                                        payments
                                            .filter {

                                                it.studentId ==
                                                        student.id &&

                                                it.month.equals(
                                                    thisMonth,
                                                    true
                                                )
                                            }
                                            .sumOf {
                                                it.amount
                                            }

                                    paid >=
                                            student.monthlyFee
                                }

                            Card {

                                Column(
                                    Modifier.padding(
                                        12.dp
                                    ),

                                    verticalArrangement =
                                        Arrangement.spacedBy(
                                            5.dp
                                        )
                                ) {

                                    Text(
                                        "Batch $batch",
                                        fontWeight =
                                            FontWeight.Bold
                                    )

                                    Text(
                                        "Students: ${batchStudents.size}"
                                    )

                                    Text(
                                        "Collected: ₹$total"
                                    )

                                    Text(
                                        "Paid this month: $paidStudents"
                                    )
                                }
                            }
                        }
                    }

                /* -----------------------------------------
                   STUDENTS
                ----------------------------------------- */

                } else {

                    Text(
                        "Student Payment History",
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (
                        students.isEmpty()
                    ) {

                        Text(
                            "No students available."
                        )

                    } else {

                        students
                            .sortedBy {
                                it.name.lowercase()
                            }
                            .forEach {
                                student ->

                                val studentPayments =
                                    payments.filter {
                                        it.studentId ==
                                                student.id
                                    }

                                val totalPaid =
                                    studentPayments
                                        .sumOf {
                                            it.amount
                                        }

                                Card {

                                    Column(
                                        Modifier.padding(
                                            12.dp
                                        ),

                                        verticalArrangement =
                                            Arrangement.spacedBy(
                                                5.dp
                                            )
                                    ) {

                                        Text(
                                            student.name,
                                            fontWeight =
                                                FontWeight.Bold
                                        )

                                        Text(
                                            "${student.className} • ${student.batch}"
                                        )

                                        Text(
                                            "Monthly fee: ₹${student.monthlyFee}"
                                        )

                                        Text(
                                            "Total paid: ₹$totalPaid"
                                        )

                                        Text(
                                            "Payments: ${studentPayments.size}"
                                        )
                                    }
                                }
                            }
                    }
                }
            }
        },

        confirmButton = {

            TextButton(
                onClick =
                    onDismiss
            ) {

                Text(
                    "Close"
                )
            }
        }
    )
}
