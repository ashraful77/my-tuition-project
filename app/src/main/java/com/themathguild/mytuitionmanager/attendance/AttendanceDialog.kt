package com.themathguild.mytuitionmanager.attendance

import com.themathguild.mytuitionmanager.AttendanceRecord
import com.themathguild.mytuitionmanager.Student

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AttendanceDialog(
    students: List<Student>,
    records: List<AttendanceRecord>,
    onDismiss: () -> Unit,
    onSave: (List<AttendanceRecord>) -> Unit
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedBatch by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }

    val batches = listOf("All") + students.map { it.batch }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()

    val dateKey = selectedDate.toString()
    val dateLabel = selectedDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH))

    fun statusFor(studentId: Long): String =
        records.firstOrNull { it.studentId == studentId && it.date == dateKey }?.status ?: "UNMARKED"

    fun setStatus(studentId: Long, status: String) {
        val updated = records
            .filterNot { it.studentId == studentId && it.date == dateKey }
            .toMutableList()

        if (status != "UNMARKED") {
            updated.add(AttendanceRecord(studentId, dateKey, status))
        }
        onSave(updated)
    }

    val visibleStudents = students
        .filter {
            it.name.contains(search, true) ||
                it.className.contains(search, true) ||
                it.batch.contains(search, true)
        }
        .filter { selectedBatch == "All" || it.batch == selectedBatch }
        .sortedBy { it.name.lowercase() }

    val presentCount = students.count { statusFor(it.id) == "PRESENT" }
    val absentCount = students.count { statusFor(it.id) == "ABSENT" }
    val lateCount = students.count { statusFor(it.id) == "LATE" }
    val markedCount = presentCount + absentCount + lateCount

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Attendance", fontWeight = FontWeight.Bold)
                Text(
                    dateLabel,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { selectedDate = selectedDate.minusDays(1) },
                        modifier = Modifier.weight(1f)
                    ) { Text("‹") }

                    Text(
                        dateLabel,
                        modifier = Modifier.weight(2f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedButton(
                        onClick = { selectedDate = selectedDate.plusDays(1) },
                        modifier = Modifier.weight(1f)
                    ) { Text("›") }
                }

                if (selectedDate != LocalDate.now()) {
                    TextButton(
                        onClick = { selectedDate = LocalDate.now() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) { Text("Today") }
                }

                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    FilterChip(
                        selected = selectedBatch == "All",
                        onClick = { selectedBatch = "All" },
                        label = { Text("All") }
                    )
                    batches.drop(1).forEach { batch ->
                        FilterChip(
                            selected = selectedBatch == batch,
                            onClick = { selectedBatch = batch },
                            label = { Text(batch) }
                        )
                    }
                }

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    label = { Text("Search student") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Card(Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("Present $presentCount")
                        Text("Absent $absentCount")
                        Text("Late $lateCount")
                        Text("Marked $markedCount/${students.size}")
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val visibleStudentIds = visibleStudents.map { it.id }.toSet()
                            var updated = records
                                .filterNot { it.date == dateKey && it.studentId in visibleStudentIds }
                                .toMutableList()
                            visibleStudents.forEach {
                                updated.add(AttendanceRecord(it.id, dateKey, "PRESENT"))
                            }
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("All Present") }

                    OutlinedButton(
                        onClick = {
                            val visibleStudentIds = visibleStudents.map { it.id }.toSet()
                            var updated = records
                                .filterNot { it.date == dateKey && it.studentId in visibleStudentIds }
                                .toMutableList()
                            visibleStudents.forEach {
                                updated.add(AttendanceRecord(it.id, dateKey, "ABSENT"))
                            }
                            onSave(updated)
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("All Absent") }
                }

                LazyColumn(
                    Modifier.fillMaxWidth().weight(1f, fill = true),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(visibleStudents, key = { it.id }) { student ->
                        val status = statusFor(student.id)

                        Card(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(student.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Class ${student.className} • Batch ${student.batch}",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Text(
                                        status,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    FilterChip(
                                        selected = status == "PRESENT",
                                        onClick = { setStatus(student.id, "PRESENT") },
                                        label = { Text("Present") }
                                    )
                                    FilterChip(
                                        selected = status == "ABSENT",
                                        onClick = { setStatus(student.id, "ABSENT") },
                                        label = { Text("Absent") }
                                    )
                                    FilterChip(
                                        selected = status == "LATE",
                                        onClick = { setStatus(student.id, "LATE") },
                                        label = { Text("Late") }
                                    )
                                    FilterChip(
                                        selected = status == "UNMARKED",
                                        onClick = { setStatus(student.id, "UNMARKED") },
                                        label = { Text("Clear") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
