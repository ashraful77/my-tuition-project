package com.themathguild.mytuitionmanager.dailyspent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.themathguild.mytuitionmanager.DailySpent
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dailySpentDateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun parseDailySpentDate(value: String): LocalDate? = try {
    LocalDate.parse(value, dailySpentDateFormatter)
} catch (_: Exception) {
    null
}

@Composable
fun DailySpentScreen(
    expenses: List<DailySpent>,
    onAdd: (String, Int) -> Unit,
    onEdit: (DailySpent, String, Int) -> Unit,
    onDelete: (DailySpent) -> Unit
) {
    var selectedDate by remember { mutableStateOf(currentDailySpentDate()) }
    var editor by remember { mutableStateOf<DailySpent?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<DailySpent?>(null) }

    val date = parseDailySpentDate(selectedDate) ?: LocalDate.now()
    val dateText = date.format(dailySpentDateFormatter)
    val todayText = currentDailySpentDate()
    val isToday = dateText == todayText
    val dayExpenses = expenses.filter { it.date == dateText }.sortedByDescending { it.id }
    val total = dayExpenses.sumOf { it.amount }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { if (isToday) addOpen = true },
                containerColor = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
            ) { Text("+") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Daily Spent", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Simple date-wise expense record", style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { selectedDate = date.minusDays(1).format(dailySpentDateFormatter) }) { Text("‹ Previous") }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dateText, fontWeight = FontWeight.Bold)
                                Text(date.format(DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())), style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { selectedDate = date.plusDays(1).format(dailySpentDateFormatter) }) { Text("Next ›") }
                        }
                        OutlinedButton(onClick = { selectedDate = currentDailySpentDate() }, modifier = Modifier.fillMaxWidth()) { Text("Today") }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Total spent", style = MaterialTheme.typography.labelMedium)
                            Text("₹$total", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                        }
                        Text("${dayExpenses.size} ${if (dayExpenses.size == 1) "expense" else "expenses"}")
                    }
                }
            }
            if (!isToday) {
                item {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Text(
                            "Historical date: viewing only. Add new spending from Today to keep the date entry accurate.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            if (dayExpenses.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("No spending recorded", fontWeight = FontWeight.Bold)
                            Text("Tap + to add an expense for $dateText.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else {
                items(dayExpenses, key = { it.id }) { expense ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(expense.description, fontWeight = FontWeight.SemiBold)
                                Text("$dateText • ₹${expense.amount}", style = MaterialTheme.typography.bodySmall)
                            }
                            TextButton(onClick = { editor = expense }) { Text("Edit") }
                            TextButton(onClick = { deleteTarget = expense }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }

    if (addOpen) {
        DailySpentEditorDialog("Add Expense", null, dateText, { addOpen = false }) { description, amount -> onAdd(description, amount); addOpen = false }
    }
    editor?.let { expense ->
        DailySpentEditorDialog("Edit Expense", expense, expense.date, { editor = null }) { description, amount -> onEdit(expense, description, amount); editor = null }
    }
    deleteTarget?.let { expense ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete expense?") },
            text = { Text("Delete ${expense.description} (₹${expense.amount}) from ${expense.date}?") },
            confirmButton = { Button(onClick = { deleteTarget = null; onDelete(expense) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun DailySpentEditorDialog(title: String, initial: DailySpent?, date: String, onDismiss: () -> Unit, onSave: (String, Int) -> Unit) {
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var amount by remember { mutableStateOf(initial?.amount?.toString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Date: $date", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(description, { description = it }, label = { Text("What did you spend on?") }, placeholder = { Text("e.g. Electricity, stationery, travel") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(amount, { amount = it.filter(Char::isDigit) }, label = { Text("Amount") }, leadingIcon = { Text("₹") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(enabled = description.isNotBlank() && (amount.toIntOrNull() ?: 0) > 0, onClick = { onSave(description.trim(), amount.toInt()) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun currentDailySpentDate(): String = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date())
