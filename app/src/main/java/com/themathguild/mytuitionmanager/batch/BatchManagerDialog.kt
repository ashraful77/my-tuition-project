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

    AlertDialog(onDismissRequest = onDismiss, title = { Text("Batches & Routine") }, text = {
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Batches", fontWeight = FontWeight.Bold)
            localBatches.forEach { batch ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(10.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(batch.name, fontWeight = FontWeight.SemiBold)
                            if (batch.address.isNotBlank()) Text(batch.address, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { editingBatchId=batch.id; batchName=batch.name; batchAddress=batch.address; showBatchEditor=true }) { Text("Edit") }
                        TextButton(onClick = { localBatches=localBatches.filterNot{it.id==batch.id}; localRoutines=localRoutines.filterNot{it.batch==batch.name}; onSaveBatches(localBatches); onSaveRoutines(localRoutines) }) { Text("Delete") }
                    }
                }
            }
            Button(onClick = { editingBatchId=null; batchName=""; batchAddress=""; showBatchEditor=true }) { Text("Add Batch") }
            HorizontalDivider()
            Text("Weekly Routine", fontWeight = FontWeight.Bold)
            localRoutines.forEach { routine ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(10.dp)) {
                        Column(Modifier.weight(1f)) { Text(routine.batch, fontWeight=FontWeight.SemiBold); Text("${routine.day} • ${routine.start} – ${routine.end}", style=MaterialTheme.typography.bodySmall) }
                        TextButton(onClick={editingRoutineId=routine.id; routineBatch=routine.batch; routineDay=routine.day; routineStart=routine.start; routineEnd=routine.end; showRoutineEditor=true}){Text("Edit")}
                        TextButton(onClick={localRoutines=localRoutines.filterNot{it.id==routine.id};onSaveRoutines(localRoutines)}){Text("Delete")}
                    }
                }
            }
            Button(onClick={editingRoutineId=null;routineBatch=localBatches.firstOrNull()?.name?:"";routineDay="Monday";routineStart="";routineEnd="";showRoutineEditor=true}){Text("Add Routine")}
        }
    }, confirmButton={TextButton(onClick=onDismiss){Text("Done")}})

    if (showBatchEditor) AlertDialog(onDismissRequest={showBatchEditor=false}, title={Text(if(editingBatchId==null)"Add Batch" else "Edit Batch")}, text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(batchName,{batchName=it},label={Text("Batch name")},singleLine=true)
        OutlinedTextField(batchAddress,{batchAddress=it},label={Text("Address")},singleLine=true)
    }}, confirmButton={TextButton(onClick={
        if(batchName.isNotBlank()){
            val oldName=localBatches.firstOrNull{it.id==editingBatchId}?.name
            localBatches=if(editingBatchId==null) localBatches+Batch((localBatches.maxOfOrNull{it.id}?:0L)+1L,batchName.trim(),batchAddress.trim()) else localBatches.map{if(it.id==editingBatchId)it.copy(name=batchName.trim(),address=batchAddress.trim())else it}
            if(oldName!=null&&oldName!=batchName.trim()){localRoutines=localRoutines.map{if(it.batch==oldName)it.copy(batch=batchName.trim())else it};onRenameBatch(oldName,batchName.trim())}
            onSaveBatches(localBatches);onSaveRoutines(localRoutines);showBatchEditor=false
        }
    }){Text("Save")}},dismissButton={TextButton(onClick={showBatchEditor=false}){Text("Cancel")}})

    if (showRoutineEditor) AlertDialog(onDismissRequest={showRoutineEditor=false},title={Text(if(editingRoutineId==null)"Add Routine" else "Edit Routine")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(routineBatch,{routineBatch=it},label={Text("Batch")},singleLine=true)
        OutlinedTextField(routineDay,{routineDay=it},label={Text("Day")},singleLine=true)
        OutlinedTextField(routineStart,{routineStart=it},label={Text("Start time (AM/PM)")},singleLine=true)
        OutlinedTextField(routineEnd,{routineEnd=it},label={Text("End time (AM/PM)")},singleLine=true)
    }},confirmButton={TextButton(onClick={
        if(routineBatch.isNotBlank()&&routineDay.isNotBlank()&&routineStart.isNotBlank()&&routineEnd.isNotBlank()){
            localRoutines=if(editingRoutineId==null)localRoutines+BatchRoutine((localRoutines.maxOfOrNull{it.id}?:0L)+1L,routineBatch.trim(),routineDay.trim(),routineStart.trim(),routineEnd.trim()) else localRoutines.map{if(it.id==editingRoutineId)it.copy(batch=routineBatch.trim(),day=routineDay.trim(),start=routineStart.trim(),end=routineEnd.trim())else it}
            onSaveRoutines(localRoutines);showRoutineEditor=false
        }
    }){Text("Save")}},dismissButton={TextButton(onClick={showRoutineEditor=false}){Text("Cancel")}})
}
