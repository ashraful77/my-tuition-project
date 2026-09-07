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
    var batchName by remember { mutableStateOf("") }
    var batchAddress by remember { mutableStateOf("") }
    var routineBatch by remember { mutableStateOf("") }
    var routineDay by remember { mutableStateOf("Monday") }
    var routineStart by remember { mutableStateOf("") }
    var routineEnd by remember { mutableStateOf("") }
    var deleteBatchTarget by remember { mutableStateOf<Batch?>(null) }

    val sortedRoutines = localRoutines.sortedWith(
        compareBy<BatchRoutine> { routineDays.indexOf(it.day.replaceFirstChar { c -> c.uppercase() }).let { i -> if (i < 0) 99 else i } }
            .thenBy { it.start.lowercase() }
            .thenBy { it.batch.lowercase() }
    )

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
                Text("Weekly Routine", fontWeight = FontWeight.Bold)
                Text("Your weekly teaching schedule", style = MaterialTheme.typography.bodySmall)

                if (sortedRoutines.isEmpty()) {
                    Text("No routines yet. Add a routine below.", style = MaterialTheme.typography.bodySmall)
                } else {
                    routineDays.forEach { day ->
                        val dayRoutines = sortedRoutines.filter { it.day.equals(day, ignoreCase = true) }
                        if (dayRoutines.isNotEmpty()) {
                            Text(day, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            dayRoutines.forEach { routine ->
                                Card(Modifier.fillMaxWidth()) {
                                    Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(routine.batch, fontWeight = FontWeight.SemiBold)
                                            Text("${routine.start} – ${routine.end}", style = MaterialTheme.typography.bodySmall)
                                        }
                                        TextButton(onClick = {
                                            editingRoutineId = routine.id
                                            routineBatch = routine.batch
                                            routineDay = routine.day
                                            routineStart = routine.start
                                            routineEnd = routine.end
                                            showRoutineEditor = true
                                        }) { Text("Edit") }
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

                Button(
                    onClick = {
                        editingRoutineId = null
                        routineBatch = localBatches.firstOrNull()?.name ?: ""
                        routineDay = "Monday"
                        routineStart = ""
                        routineEnd = ""
                        showRoutineEditor = true
                    },
                    enabled = localBatches.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("+ Add Routine") }
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
                            onRenameBatch(oldName, cleanName)
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
                    OutlinedTextField(
                        value = routineStart,
                        onValueChange = { routineStart = it },
                        label = { Text("Start time (e.g. 7:00 AM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = routineEnd,
                        onValueChange = { routineEnd = it },
                        label = { Text("End time (e.g. 8:30 AM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
