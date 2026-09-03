package com.themathguild.mytuitionmanager.academic

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.themathguild.mytuitionmanager.AcademicRecord
import com.themathguild.mytuitionmanager.Student
import com.themathguild.mytuitionmanager.TuitionProfile
import com.themathguild.mytuitionmanager.currentDate
import com.themathguild.mytuitionmanager.receipts.shareReceipt
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

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
