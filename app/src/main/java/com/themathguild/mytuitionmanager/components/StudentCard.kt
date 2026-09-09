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
            val monthPaid = studentPayments
                .filter { it.month.equals(month.format(monthFormatter), true) }
                .sumOf { it.amount }
            amount += maxOf(0, student.monthlyFee - monthPaid)
            month = month.plusMonths(1)
        }
        amount
    }
    val paymentStatus = if (totalDue == 0 && student.monthlyFee > 0) "PAID" else "DUE"
    val canCollect = isActiveTab && paymentStatus != "PAID"

    Card(
        onClick = { onOpenProfile(student) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        student.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Class ${student.className}  •  Batch ${student.batch.ifBlank { "—" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(onClick = { onEdit(student) }) {
                    Text("Edit", fontWeight = FontWeight.SemiBold)
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge(student.status)
                if (paymentStatus == "PAID") {
                    StatusBadge("PAID")
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            "DUE  ₹$totalDue",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            if (canCollect) {
                Button(
                    onClick = { onCollect(student) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text("Collect Fee  •  ₹$totalDue", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(
                    onClick = { onOpenProfile(student) },
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 11.dp)
                ) {
                    Text("View Profile", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
