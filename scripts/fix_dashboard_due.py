from pathlib import Path

p = Path("app/src/main/java/com/themathguild/mytuitionmanager/MainActivity_Phase35_v46_STUDENT_SEARCH_FILTER_modularized.kt")
s = p.read_text(encoding="utf-8")

old = """        while (!m.isAfter(YearMonth.now())) {
            val paid = ps.filter { it.month.equals(m.format(monthFormatter), true) }.sumOf { it.amount }
            due += maxOf(0, student.monthlyFee - paid); m = m.plusMonths(1)
        }"""
new = """        val dueThrough = YearMonth.now().minusMonths(1)
        while (!m.isAfter(dueThrough)) {
            val paid = ps.filter { it.month.equals(m.format(monthFormatter), true) }.sumOf { it.amount }
            due += maxOf(0, student.monthlyFee - paid); m = m.plusMonths(1)
        }"""

if new in s:
    print("Dashboard due cutoff is already applied.")
elif old in s:
    s = s.replace(old, new, 1)
    p.write_text(s, encoding="utf-8")
    print("Dashboard due cutoff applied successfully.")
else:
    raise SystemExit("Dashboard outstanding calculation block not found.")
