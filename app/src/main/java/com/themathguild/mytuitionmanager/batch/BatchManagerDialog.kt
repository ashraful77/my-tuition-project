package com.themathguild.mytuitionmanager.batch

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.themathguild.mytuitionmanager.Batch
import com.themathguild.mytuitionmanager.BatchRoutine
import java.util.Locale

private val routineDays = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

@Composable
fun BatchManagerDialog(
    batches: List<Batch>,
    routines: List<BatchRoutine>,
    onSaveBatches: (List<Batch>) -> Unit,
    onSaveRoutines: (List<BatchRoutine>) -> Unit,
    onDismiss: () -> Unit,
    onRenameBatch: (String, String) -> Unit = { _, _ -> }
) {
    var localBatches by remember { mutableStateOf(batches) }
    var localRoutines by remember { mutableStateOf(routines) }
    var showBatchEditor by remember { mutableStateOf(false) }
    var editingBatchId by remember { mutableStateOf<Long?>(null) }
    var showRoutineEditor by remember { mutableStateOf(false) }
    var editingRoutineId by remember { mutableStateOf<Long?>(null) }
    var selectedBatch by remember { mutableStateOf(localBatches.firstOrNull()?.name ?: "") }
    var routineBatch by remember { mutableStateOf(localBatches.firstOrNull()?.name ?: "") }
    var routineDay by remember { mutableStateOf("Monday") }
    var routineStart by remember { mutableStateOf("") }
    var routineEnd by remember { mutableStateOf("") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    var batchName by remember { mutableStateOf("") }
    var batchAddress by remember { mutableStateOf("") }
    var deleteBatchTarget by remember { mutableStateOf<Batch?>(null) }

    val selectedBatchRoutines = localRoutines.filter { it.batch == selectedBatch }
    val selectedBatchByDay = routineDays.associateWith { day ->
        selectedBatchRoutines.firstOrNull { it.day.equals(day, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Batches & Routine") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Batches", fontWeight = FontWeight.Bold)
                if (localBatches.isEmpty()) {
                    Text("No batches yet. Add your first batch below.", style = MaterialTheme.typography.bodySmall)
                }
                localBatches.forEach { batch ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(batch.name, fontWeight = FontWeight.SemiBold)
                                if (batch.address.isNotBlank()) Text(batch.address, style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = {
                                editingBatchId = batch.id
                                batchName = batch.name
                                batchAddress = batch.address
                                showBatchEditor = true
                            }) { Text("Edit") }
                            TextButton(onClick = { deleteBatchTarget = batch }) { Text("Delete") }
                        }
                    }
                }
                Button(
                    onClick = {
                        editingBatchId = null
                        batchName = ""
                        batchAddress = ""
                        showBatchEditor = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Add Batch") }

                HorizontalDivider()
                Text("Batch-wise Routine", fontWeight = FontWeight.Bold)
                Text("Select a batch to view and manage its weekly teaching schedule.", style = MaterialTheme.typography.bodySmall)

                if (localBatches.isEmpty()) {
                    Text("Add a batch first to create its routine.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LaunchedEffect(localBatches.map { it.name }) {
                        if (selectedBatch !in localBatches.map { it.name }) {
                            selectedBatch = localBatches.first().name
                        }
                    }
                    SimpleDropdownField("Batch", selectedBatch, localBatches.map { it.name }) { selectedBatch = it }

                    Text(
                        "$selectedBatch — Weekly Routine",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    routineDays.forEach { day ->
                        val routine = selectedBatchByDay[day]
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.fillMaxWidth().padding(10.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(day, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (routine == null) "No routine scheduled" else "${routine.start} – ${routine.end}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                TextButton(onClick = {
                                    editingRoutineId = routine?.id
                                    routineBatch = selectedBatch
                                    routineDay = day
                                    routineStart = routine?.start ?: ""
                                    routineEnd = routine?.end ?: ""
                                    showRoutineEditor = true
                                }) {
                                    Text(if (routine == null) "Add" else "Edit")
                                }
                                if (routine != null) {
                                    TextButton(onClick = {
                                        localRoutines = localRoutines.filterNot { it.id == routine.id }
                                        onSaveRoutines(localRoutines)
                                    }) { Text("Delete") }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )

    if (showBatchEditor) {
        AlertDialog(
            onDismissRequest = { showBatchEditor = false },
            title = { Text(if (editingBatchId == null) "Add Batch" else "Edit Batch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = batchName,
                        onValueChange = { batchName = it },
                        label = { Text("Batch name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = batchAddress,
                        onValueChange = { batchAddress = it },
                        label = { Text("Address (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleanName = batchName.trim()
                    if (cleanName.isNotBlank()) {
                        val oldName = localBatches.firstOrNull { it.id == editingBatchId }?.name
                        localBatches = if (editingBatchId == null) {
                            localBatches + Batch((localBatches.maxOfOrNull { it.id } ?: 0L) + 1L, cleanName, batchAddress.trim())
                        } else {
                            localBatches.map { if (it.id == editingBatchId) it.copy(name = cleanName, address = batchAddress.trim()) else it }
                        }
                        if (oldName != null && oldName != cleanName) {
                            localRoutines = localRoutines.map { if (it.batch == oldName) it.copy(batch = cleanName) else it }
                            if (selectedBatch == oldName) selectedBatch = cleanName
                            if (routineBatch == oldName) routineBatch = cleanName
                            onRenameBatch(oldName, cleanName)
                        }
                        if (editingBatchId == null) {
                            selectedBatch = cleanName
                            routineBatch = cleanName
                        }
                        onSaveBatches(localBatches)
                        onSaveRoutines(localRoutines)
                        showBatchEditor = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showBatchEditor = false }) { Text("Cancel") } }
        )
    }

    if (showRoutineEditor) {
        AlertDialog(
            onDismissRequest = { showRoutineEditor = false },
            title = { Text(if (editingRoutineId == null) "Add Routine" else "Edit Routine") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SimpleDropdownField("Batch", routineBatch, localBatches.map { it.name }) { routineBatch = it }
                    SimpleDropdownField("Day", routineDay, routineDays) { routineDay = it }

                    OutlinedButton(
                        onClick = { showStartTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start time: ${routineStart.ifBlank { "Select time" }}")
                    }

                    OutlinedButton(
                        onClick = { showEndTimePicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("End time: ${routineEnd.ifBlank { "Select time" }}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cleanBatch = routineBatch.trim()
                    val cleanDay = routineDay.trim()
                    val cleanStart = routineStart.trim()
                    val cleanEnd = routineEnd.trim()
                    if (cleanBatch.isNotBlank() && cleanDay in routineDays && cleanStart.isNotBlank() && cleanEnd.isNotBlank()) {
                        localRoutines = if (editingRoutineId == null) {
                            localRoutines + BatchRoutine(
                                (localRoutines.maxOfOrNull { it.id } ?: 0L) + 1L,
                                cleanBatch, cleanDay, cleanStart, cleanEnd
                            )
                        } else {
                            localRoutines.map {
                                if (it.id == editingRoutineId) it.copy(batch = cleanBatch, day = cleanDay, start = cleanStart, end = cleanEnd) else it
                            }
                        }
                        onSaveRoutines(localRoutines)
                        showRoutineEditor = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showRoutineEditor = false }) { Text("Cancel") } }
        )
    }

    if (showStartTimePicker) {
        RoutineTimePickerDialog(
            title = "Select start time",
            initialValue = routineStart,
            onDismiss = { showStartTimePicker = false },
            onTimeSelected = {
                routineStart = it
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        RoutineTimePickerDialog(
            title = "Select end time",
            initialValue = routineEnd,
            onDismiss = { showEndTimePicker = false },
            onTimeSelected = {
                routineEnd = it
                showEndTimePicker = false
            }
        )
    }

    deleteBatchTarget?.let { batch ->
        val routineCount = localRoutines.count { it.batch == batch.name }
        AlertDialog(
            onDismissRequest = { deleteBatchTarget = null },
            title = { Text("Delete ${batch.name}?") },
            text = {
                Text(
                    if (routineCount == 0) "This batch has no scheduled routines."
                    else "This will also remove $routineCount scheduled routine${if (routineCount == 1) "" else "s"} for this batch."
                )
            },
            confirmButton = {
                Button(onClick = {
                    localBatches = localBatches.filterNot { it.id == batch.id }
                    localRoutines = localRoutines.filterNot { it.batch == batch.name }
                    if (selectedBatch == batch.name) selectedBatch = localBatches.firstOrNull()?.name ?: ""
                    if (routineBatch == batch.name) routineBatch = localBatches.firstOrNull()?.name ?: ""
                    onSaveBatches(localBatches)
                    onSaveRoutines(localRoutines)
                    deleteBatchTarget = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteBatchTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RoutineTimePickerDialog(
    title: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onTimeSelected: (String) -> Unit
) {
    val initial = parseTime(initialValue)
    val timeState = rememberTimePickerState(
        initialHour = initial.first,
        initialMinute = initial.second,
        is24Hour = false
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            TimePicker(state = timeState)
        },
        confirmButton = {
            TextButton(onClick = {
                onTimeSelected(formatTime(timeState.hour, timeState.minute))
            }) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun parseTime(value: String): Pair<Int, Int> {
    val match = Regex("(\\d{1,2}):(\\d{2})\\s*([AaPp][Mm])").find(value.trim()) ?: return 8 to 0
    val hour12 = match.groupValues[1].toIntOrNull()?.coerceIn(1, 12) ?: 8
    val minute = match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
    val pm = match.groupValues[3].equals("PM", ignoreCase = true)
    val hour24 = when {
        pm && hour12 != 12 -> hour12 + 12
        !pm && hour12 == 12 -> 0
        else -> hour12
    }
    return hour24 to minute
}

private fun formatTime(hour24: Int, minute: Int): String {
    val suffix = if (hour24 >= 12) "PM" else "AM"
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    return String.format(Locale.US, "%d:%02d %s", hour12, minute, suffix)
}

@Composable
private fun SimpleDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelSmall)
                    Text(value.ifBlank { "Select" }, fontWeight = FontWeight.SemiBold)
                }
                Text("▾")
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = { onSelected(option); expanded = false }
                )
            }
        }
    }
}
