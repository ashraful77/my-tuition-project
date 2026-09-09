package com.themathguild.mytuitionmanager.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.themathguild.mytuitionmanager.AttendanceRecord
import com.themathguild.mytuitionmanager.Payment
import com.themathguild.mytuitionmanager.Student
import com.themathguild.mytuitionmanager.StatusBadge
import com.themathguild.mytuitionmanager.monthKey
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)

@Composable
fun StudentCard(
    student: Student,
    payments: List<Payment>,
    attendance: List<AttendanceRecord>,
    isActiveTab: Boolean,
    onOpenProfile: (Student) -> Unit,
    onCollect: (Student) -> Unit,
    onEdit: (Student) -> Unit,
    onDelete: (Student) -> Unit
) {
    val totalDue = run {
        val start = monthKey(student.joiningMonth)
            ?: payments.filter { it.studentId == student.id }.mapNotNull { monthKey(it.month) }.minOrNull()
            ?: YearMonth.now()
        val dueThrough = YearMonth.now().minusMonths(1)
        val studentPayments = payments.filter { it.studentId == student.id }
        var month = start
        var amount = 0
        while (!month.isAfter(dueThrough)) {
            val monthPaid = studentPayments.filter { it.month.equals(month.format(monthFormatter), true) }.sumOf { it.amount }
            amount += maxOf(0, student.monthlyFee - monthPaid)
            month = month.plusMonths(1)
        }
        amount
    }
    val paymentStatus = if (totalDue == 0 && student.monthlyFee > 0) "PAID" else "DUE"

    Card(onClick = { onOpenProfile(student) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Class ${student.className}  •  Batch ${student.batch.ifBlank { "—" }}", style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { onEdit(student) }) { Text("Edit", fontWeight = FontWeight.SemiBold) }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                StatusBadge(student.status)
                if (paymentStatus == "PAID") StatusBadge("PAID")
                else Text("Due ₹$totalDue", color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
            }
            FilledTonalButton(onClick = { if (isActiveTab) onCollect(student) else onOpenProfile(student) }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isActiveTab && paymentStatus != "PAID") "Collect Fee" else "View Profile", fontWeight = FontWeight.Bold)
            }
        }
    }
}
