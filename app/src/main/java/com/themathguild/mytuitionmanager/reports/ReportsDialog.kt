package com.themathguild.mytuitionmanager.reports

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.themathguild.mytuitionmanager.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private fun shareReceipt(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share PDF"))
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
                            FilterChip(selected=if(report=="Monthly")selectedMonth==period else selectedYear==period,onClick={if(report=="Monthly")selectedMonth=period else selectedYear=period},label={Text(period)})
                        }
                    }
                    val period=if(report=="Monthly")selectedMonth else selectedYear
                    val relevant=if(report=="Monthly")payments.filter{it.month.equals(selectedMonth,true)} else payments.filter{Regex("\\b(20\\d{2})\\b").find(it.month)?.groupValues?.get(1)==selectedYear||it.date.takeLast(4)==selectedYear}
                    val expected=if(report=="Monthly")students.sumOf{it.monthlyFee}else 0
                    val collected=relevant.sumOf{it.amount}
                    val due=if(report=="Monthly")maxOf(0,expected-collected)else 0
                    val rate=if(expected>0)((collected.toFloat()/expected)*100).roundToInt()else 0
                    Card{Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                        Text("$report Summary",fontWeight=FontWeight.Bold);Text("Students: ${students.size}")
                        if(report=="Monthly"){Text("Expected: ₹$expected");Text("Outstanding: ₹$due");Text("Collection rate: $rate%")}
                        Text("Collected: ₹$collected",fontWeight=FontWeight.Bold);Text("Payment records: ${relevant.size}");Text("Students with payments: ${relevant.map{it.studentId}.distinct().size}")
                    }}
                    if(report=="Yearly"){
                        Text("Month-wise Collection",fontWeight=FontWeight.Bold)
                        relevant.groupBy{it.month}.mapValues{(_,ps)->ps.sumOf{it.amount}}.entries.sortedBy{monthKey(it.key)}.forEach{(month,total)->Card{Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(month);Text("₹$total",fontWeight=FontWeight.Bold)}}}
                    }else{
                        Text("Student Status",fontWeight=FontWeight.Bold)
                        val paid=students.count{s->payments.filter{it.studentId==s.id&&it.month.equals(selectedMonth,true)}.sumOf{it.amount}>=s.monthlyFee}
                        val partial=students.count{s->val a=payments.filter{it.studentId==s.id&&it.month.equals(selectedMonth,true)}.sumOf{it.amount};a>0&&a<s.monthlyFee}
                        Card{Column(Modifier.padding(12.dp)){Text("Paid: $paid");Text("Partial: $partial");Text("Unpaid: ${students.size-paid-partial}")}}
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End){Button(onClick={shareUri=createReportPdf(context,students,payments,report,period)}){Text("Share PDF")}}
                }else if(report=="Progress"){
                    Text("Student Progress Report",fontWeight=FontWeight.Bold)
                    if(students.isEmpty()){Text("Add students before creating a progress report.")}else{
                        val progressStudent=students.firstOrNull{it.id==progressStudentId}?:students.first()
                        var studentMenu by remember{mutableStateOf(false)}
                        Box{OutlinedButton(onClick={studentMenu=true},modifier=Modifier.fillMaxWidth()){Text(progressStudent.name)};DropdownMenu(expanded=studentMenu,onDismissRequest={studentMenu=false}){students.sortedBy{it.name.lowercase()}.forEach{s->DropdownMenuItem(text={Text("${s.name} • Class ${s.className}")},onClick={progressStudentId=s.id;studentMenu=false})}}}
                        val sa=academicRecords.filter{it.studentId==progressStudent.id&&it.maxMarks>0};val ar=attendance.filter{it.studentId==progressStudent.id};val present=ar.count{it.status=="PRESENT"};val absent=ar.count{it.status=="ABSENT"};val late=ar.count{it.status=="LATE"};val marked=present+absent+late
                        val attendanceRate=if(marked>0)((present.toFloat()/marked)*100).roundToInt()else 0
                        val academicRate=if(sa.isNotEmpty()){((sa.sumOf{it.marks}.toFloat()/sa.sumOf{it.maxMarks}.toFloat())*100).roundToInt()}else 0
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){DashboardCard("Attendance","$attendanceRate%",Modifier.weight(1f));DashboardCard("Academic","$academicRate%",Modifier.weight(1f))}
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){DashboardCard("Present",present.toString(),Modifier.weight(1f));DashboardCard("Tests",sa.size.toString(),Modifier.weight(1f))}
                        Text("Recent Results",fontWeight=FontWeight.Bold)
                        if(sa.isEmpty())Text("No academic results recorded for this student.")else sa.sortedByDescending{it.date}.take(5).forEach{r->val pct=((r.marks.toFloat()/r.maxMarks.toFloat())*100).roundToInt();Card(Modifier.fillMaxWidth()){Row(Modifier.fillMaxWidth().padding(10.dp),horizontalArrangement=Arrangement.SpaceBetween){Column(Modifier.weight(1f)){Text("${r.subject.ifBlank{"Subject"}} • ${r.title.ifBlank{"Test"}}",fontWeight=FontWeight.Bold);Text(r.date,style=MaterialTheme.typography.labelSmall)};Text("${r.marks.toCleanNumber()}/${r.maxMarks.toCleanNumber()} ($pct%)",fontWeight=FontWeight.Bold)}}}
                        Button(onClick={shareUri=createReportPdf(context,students,payments,"Progress",progressStudent.name,attendance,academicRecords,progressStudent)},modifier=Modifier.fillMaxWidth()){Text("Create & Share Progress PDF")}
                    }
                }else if(report=="Batch"){
                    Text("Batch-wise Collection",fontWeight=FontWeight.Bold)
                    students.map{it.batch}.filter{it.isNotBlank()}.distinct().sorted().forEach{batch->val bs=students.filter{it.batch==batch};val total=payments.filter{it.studentId in bs.map{it.id}}.sumOf{it.amount};Card{Column(Modifier.padding(12.dp)){Text("Batch $batch",fontWeight=FontWeight.Bold);Text("Students: ${bs.size}");Text("Collected: ₹$total")}}}
                }else{
                    Text("Student Payment History",fontWeight=FontWeight.Bold)
                    students.sortedBy{it.name.lowercase()}.forEach{s->val ps=payments.filter{it.studentId==s.id};Card{Column(Modifier.padding(12.dp)){Text(s.name,fontWeight=FontWeight.Bold);Text("${s.className} • ${s.batch}");Text("Joining Month: ${s.joiningMonth.ifBlank{"Not provided"}}");Text("Monthly fee: ₹${s.monthlyFee}");Text("Total paid: ₹${ps.sumOf{it.amount}}");Text("Payments: ${ps.size}")}}}
                }
            }
        },
        confirmButton={TextButton(onDismiss){Text("Close")}}
    )
    shareUri?.let{uri->AlertDialog(onDismissRequest={shareUri=null},title={Text("Share Report")},text={Text("Your $report report is ready.")},confirmButton={Button(onClick={shareReceipt(context,uri);shareUri=null}){Text("Share")}},dismissButton={TextButton(onClick={shareUri=null}){Text("Cancel")}})}
}
