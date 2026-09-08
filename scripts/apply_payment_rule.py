from pathlib import Path

p = Path("app/src/main/java/com/themathguild/mytuitionmanager/MainActivity_Phase35_v46_STUDENT_SEARCH_FILTER_modularized.kt")
s = p.read_text(encoding="utf-8")

old = """fun eligibleFeeMonths(student: Student, existingPayments: List<Payment>): List<String> {
    val join = monthKey(student.joiningMonth) ?: return emptyList()
    val latestAllowed = YearMonth.now().plusMonths(1)
    if (join.isAfter(latestAllowed)) return emptyList()

    val paidMonths = existingPayments.mapNotNull { monthKey(it.month) }.toSet()
    return buildList {
        var m = join
        while (!m.isAfter(latestAllowed)) {
            if (m !in paidMonths) add(m.format(monthFormatter))
            m = m.plusMonths(1)
        }
    }
}"""
new = """fun eligibleFeeMonths(
    student: Student,
    existingPayments: List<Payment>,
    allowAdvance: Boolean = false
): List<String> {
    val join = monthKey(student.joiningMonth) ?: return emptyList()
    val now = YearMonth.now()
    val latestAllowed = if (allowAdvance) now.plusMonths(12) else now.minusMonths(1)
    if (join.isAfter(latestAllowed)) return emptyList()

    val paidMonths = existingPayments.mapNotNull { monthKey(it.month) }.toSet()
    return buildList {
        var m = join
        while (!m.isAfter(latestAllowed)) {
            val normal = !m.isAfter(now.minusMonths(1))
            val advance = allowAdvance && !m.isBefore(now)
            if (m !in paidMonths && (normal || advance)) add(m.format(monthFormatter))
            m = m.plusMonths(1)
        }
    }
}"""
if old not in s: raise SystemExit("eligibleFeeMonths block not found")
s = s.replace(old, new, 1)

old = """fun validateFeeCollection(
    student: Student,
    existingPayments: List<Payment>,
    selectedMonths: List<String>
): String? {
    if (student.monthlyFee <= 0) return "Monthly fee must be greater than ₹0."
    if (monthKey(student.joiningMonth) == null) return "Please enter a valid joining month."
    if (selectedMonths.isEmpty()) return "Select at least one month."

    val duplicate = selectedMonths.groupingBy { it.lowercase() }.eachCount().any { it.value > 1 }
    if (duplicate) return "The same month cannot be selected twice."

    val allowed = eligibleFeeMonths(student, existingPayments).toSet()
    val invalid = selectedMonths.filter { it !in allowed }
    if (invalid.isNotEmpty()) return "Payment not allowed for: ${invalid.joinToString(", ")}"

    return null
}"""
new = """fun validateFeeCollection(
    student: Student,
    existingPayments: List<Payment>,
    selectedMonths: List<String>,
    allowAdvance: Boolean = false
): String? {
    if (student.monthlyFee <= 0) return "Monthly fee must be greater than ₹0."
    if (monthKey(student.joiningMonth) == null) return "Please enter a valid joining month."
    if (selectedMonths.isEmpty()) return "Select at least one month."

    val duplicate = selectedMonths.groupingBy { it.lowercase() }.eachCount().any { it.value > 1 }
    if (duplicate) return "The same month cannot be selected twice."

    val allowed = eligibleFeeMonths(student, existingPayments, allowAdvance).toSet()
    val invalid = selectedMonths.filter { it !in allowed }
    if (invalid.isNotEmpty()) return "Payment not allowed for: ${invalid.joinToString(", ")}"

    return null
}"""
if old not in s: raise SystemExit("validateFeeCollection block not found")
s = s.replace(old, new, 1)

old = """            ) { selectedMonths ->
                val existingForStudent = payments.filter { it.studentId == s.id }
                val error = validateFeeCollection(s, existingForStudent, selectedMonths)"""
new = """            ) { selectedMonths, allowAdvance ->
                val existingForStudent = payments.filter { it.studentId == s.id }
                val error = validateFeeCollection(s, existingForStudent, selectedMonths, allowAdvance)"""
if old not in s: raise SystemExit("payment callback not found")
s = s.replace(old, new, 1)

old = """fun CollectFeeDialog(
    student: Student,
    existingPayments: List<Payment>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {"""
new = """fun CollectFeeDialog(
    student: Student,
    existingPayments: List<Payment>,
    onDismiss: () -> Unit,
    onSave: (List<String>, Boolean) -> Unit
) {
    var allowAdvance by remember { mutableStateOf(false) }"""
if old not in s: raise SystemExit("CollectFeeDialog signature not found")
s = s.replace(old, new, 1)

old = "val eligibleMonths = eligibleFeeMonths(student, existingPayments)"
new = "val eligibleMonths = eligibleFeeMonths(student, existingPayments, allowAdvance)"
if old not in s: raise SystemExit("eligibleMonths line not found")
s = s.replace(old, new, 1)

old = 'title = { Text("Collect Fee") },'
new = 'title = { Text(if (allowAdvance) "Pay in Advance" else "Collect Fee") },'
if old not in s: raise SystemExit("Collect Fee title not found")
s = s.replace(old, new, 1)

old = """                Text("Select one or more eligible months.")
                Text(
                    "Allowed: joining month through next month. " +
                        "A month can be collected only once."
                )"""
new = """                if (!allowAdvance) {
                    Text("Select unpaid fee months up to the previous month.")
                    Text("Current month and future months require Pay in Advance.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { selectedMonths = emptySet(); allowAdvance = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Pay in Advance") }
                } else {
                    Text("Select current or future months to pay in advance.")
                    Text("Advance payment is available for up to 12 months ahead.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { selectedMonths = emptySet(); allowAdvance = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Back to Normal Payment") }
                }"""
if old not in s: raise SystemExit("Collect Fee explanatory text not found")
s = s.replace(old, new, 1)

old = "onSave(ordered)"
new = "onSave(ordered, allowAdvance)"
if old not in s: raise SystemExit("onSave call not found")
s = s.replace(old, new, 1)

p.write_text(s, encoding="utf-8")
print("Payment rule applied successfully.")
