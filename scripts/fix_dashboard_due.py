from pathlib import Path

p = Path("app/src/main/java/com/themathguild/mytuitionmanager/MainActivity_Phase35_v46_STUDENT_SEARCH_FILTER_modularized.kt")
s = p.read_text(encoding="utf-8")

# Keep Home dashboard collapsed when the app opens.
s = s.replace(
    'var dashboardExpanded by remember { mutableStateOf(true) }',
    'var dashboardExpanded by remember { mutableStateOf(false) }',
    1,
)

# Keep the Home Due calculation through the previous month only.
old_due = """        while (!m.isAfter(YearMonth.now())) {
            val paid = ps.filter { it.month.equals(m.format(monthFormatter), true) }.sumOf { it.amount }
            due += maxOf(0, student.monthlyFee - paid); m = m.plusMonths(1)
        }"""
new_due = """        val dueThrough = YearMonth.now().minusMonths(1)
        while (!m.isAfter(dueThrough)) {
            val paid = ps.filter { it.month.equals(m.format(monthFormatter), true) }.sumOf { it.amount }
            due += maxOf(0, student.monthlyFee - paid); m = m.plusMonths(1)
        }"""
if old_due in s:
    s = s.replace(old_due, new_due, 1)

# Add a simple Tomorrow's Work card directly below Today's Work.
old_work = '''                item {
                    val dayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(Date())
                    val todayRoutines = routines.filter { it.day.equals(dayName, true) }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                                Column { Text("Today’s Work", fontWeight=FontWeight.Bold); Text("$dayName • ${todayRoutines.size} scheduled batch${if(todayRoutines.size==1) "" else "es"}", style=MaterialTheme.typography.labelSmall) }
                                TextButton(onClick={todayWorkOpen=true}){Text("View") }
                            }
                            todayRoutines.take(3).forEach { Text("🟢 ${it.start}–${it.end} • ${it.batch}", style=MaterialTheme.typography.bodySmall) }
                            if(todayRoutines.isEmpty()) Text("No routine set for today. Use Live Batch or Settings to add one.", style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }'''
new_work = old_work + '''

                item {
                    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
                    val tomorrowDayName = SimpleDateFormat("EEEE", Locale.getDefault()).format(tomorrow)
                    val tomorrowRoutines = routines.filter { it.day.equals(tomorrowDayName, true) }
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Tomorrow’s Work", fontWeight=FontWeight.Bold)
                            Text("$tomorrowDayName • ${tomorrowRoutines.size} scheduled batch${if(tomorrowRoutines.size==1) "" else "es"}", style=MaterialTheme.typography.labelSmall)
                            tomorrowRoutines.take(3).forEach { Text("🟢 ${it.start}–${it.end} • ${it.batch}", style=MaterialTheme.typography.bodySmall) }
                            if(tomorrowRoutines.isEmpty()) Text("No routine set for tomorrow. Use Live Batch or Settings to add one.", style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }'''
if 'Text("Tomorrow’s Work", fontWeight=FontWeight.Bold)' not in s:
    if old_work not in s:
        raise SystemExit("Home Today’s Work block not found")
    s = s.replace(old_work, new_work, 1)

p.write_text(s, encoding="utf-8")
print("Home dashboard update applied.")
