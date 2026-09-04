package com.themathguild.mytuitionmanager.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.themathguild.mytuitionmanager.AttendanceRecord
import com.themathguild.mytuitionmanager.Payment
import com.themathguild.mytuitionmanager.Student
import com.themathguild.mytuitionmanager.StatusBadge
import com.themathguild.mytuitionmanager.currentMonth

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
    val paid = payments.filter { it.studentId == student.id && it.month.equals(currentMonth(), true) }.sumOf { it.amount }
    val due = maxOf(0, student.monthlyFee - paid)
    val paymentStatus = when {
        paid >= student.monthlyFee && student.monthlyFee > 0 -> "PAID"
        paid > 0 -> "PARTIAL"
        else -> "DUE"
    }

    Card(onClick = { onOpenProfile(student) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(student.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Class ${student.className}  •  Batch ${student.batch.ifBlank { "—" }}", style = MaterialTheme.typography.bodyMedium)
                }
                StatusBadge(paymentStatus) { onOpenProfile(student) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(student.status)
                if (paymentStatus != "PAID") Text("Due ₹$due", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                else Text("Paid this month", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Text("Monthly fee", style = MaterialTheme.typography.labelSmall); Text("₹${student.monthlyFee}", fontWeight = FontWeight.SemiBold) }
                Column(horizontalAlignment = Alignment.End) { Text("Joined", style = MaterialTheme.typography.labelSmall); Text(student.joiningMonth.ifBlank { "—" }, fontWeight = FontWeight.SemiBold) }
            }
            Row(Modifier.fillMaxWidth()) {
                Text("Tap card to see full profile", style = MaterialTheme.typography.bodySmall)
            }
            if (student.phone.isNotBlank()) Text("☎ ${student.phone}", style = MaterialTheme.typography.bodySmall)
            if (isActiveTab) {
                FilledTonalButton(onClick = { onCollect(student) }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (due > 0) "Collect Fee • ₹$due" else "View / Collect Fee")
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onEdit(student) }) { Text("Edit") }
                TextButton(onClick = { onDelete(student) }) { Text("Delete") }
            }
        }
    }
}
