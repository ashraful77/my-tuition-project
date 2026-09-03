package com.themathguild.mytuitionmanager.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import com.themathguild.mytuitionmanager.Student
import com.themathguild.mytuitionmanager.Payment
import com.themathguild.mytuitionmanager.AttendanceRecord
import com.themathguild.mytuitionmanager.AcademicRecord
import com.themathguild.mytuitionmanager.BatchRoutine
import com.themathguild.mytuitionmanager.Batch
import com.themathguild.mytuitionmanager.LibraryItem
import com.themathguild.mytuitionmanager.BatchNote
import com.themathguild.mytuitionmanager.TuitionProfile
import com.themathguild.mytuitionmanager.DailySpent

class LocalStore(context: Context) {
    private val prefs = context.getSharedPreferences("tuition_data", Context.MODE_PRIVATE)

    fun loadStudents(): List<Student> {
        val a = JSONArray(prefs.getString("students", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Student(
                o.getLong("id"),
                o.getString("name"),
                o.getString("className"),
                o.getString("batch"),
                o.getInt("monthlyFee"),
                o.optString("phone"),
                o.optString("joiningMonth", ""),
                o.optString("school", ""),
                o.optString("fatherName", ""),
                o.optString("motherName", ""),
                o.optString("address", ""),
                o.optString("subjects", ""),
                o.optString("notes", ""),
                o.optString("status", "Active")
            )
        }
    }

    fun saveStudents(list: List<Student>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("className", it.className)
                put("batch", it.batch)
                put("monthlyFee", it.monthlyFee)
                put("phone", it.phone)
                put("joiningMonth", it.joiningMonth)
                put("school", it.school)
                put("fatherName", it.fatherName)
                put("motherName", it.motherName)
                put("address", it.address)
                put("subjects", it.subjects)
                put("notes", it.notes)
                put("status", it.status)
            })
        }
        prefs.edit().putString("students", a.toString()).apply()
    }

    fun loadPayments(): List<Payment> {
        val a = JSONArray(prefs.getString("payments", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Payment(
                o.getLong("studentId"),
                o.getString("month"),
                o.getInt("amount"),
                o.getString("date"),
                o.optString("receiptNo", "")
            )
        }
    }

    fun loadAttendance(): List<AttendanceRecord> {
        val a = JSONArray(prefs.getString("attendance", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            AttendanceRecord(
                o.getLong("studentId"),
                o.getString("date"),
                o.getString("status")
            )
        }
    }

    fun saveAttendance(list: List<AttendanceRecord>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("studentId", it.studentId)
                put("date", it.date)
                put("status", it.status)
            })
        }
        prefs.edit().putString("attendance", a.toString()).apply()
    }

    fun loadAcademicRecords(): List<AcademicRecord> {
        val a = JSONArray(prefs.getString("academicRecords", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            AcademicRecord(
                o.getLong("id"),
                o.getLong("studentId"),
                o.getString("date"),
                o.optString("subject"),
                o.optString("title"),
                o.optDouble("marks", 0.0),
                o.optDouble("maxMarks", 0.0),
                o.optString("remarks")
            )
        }
    }

    fun saveAcademicRecords(list: List<AcademicRecord>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("id", it.id)
                put("studentId", it.studentId)
                put("date", it.date)
                put("subject", it.subject)
                put("title", it.title)
                put("marks", it.marks)
                put("maxMarks", it.maxMarks)
                put("remarks", it.remarks)
            })
        }
        prefs.edit().putString("academicRecords", a.toString()).apply()
    }

    fun nextAcademicId(): Long =
        loadAcademicRecords().maxOfOrNull { it.id }?.plus(1L) ?: 1L

    fun loadTuitionProfile(): TuitionProfile {
        return TuitionProfile(
            tuitionName = prefs.getString("tuitionName", "The Math Guide") ?: "The Math Guide",
            tagline = prefs.getString("tuitionTagline", "Tuition & Academic Support") ?: "Tuition & Academic Support",
            teacherName = prefs.getString("teacherName", "Ashraful Hoque") ?: "Ashraful Hoque",
            qualification = prefs.getString("qualification", "B.SC Maths") ?: "B.SC Maths",
            phone = prefs.getString("tuitionPhone", "9732956571") ?: "9732956571",
            address = prefs.getString("tuitionAddress", "Jatarpur, Murshidabad, West Bengal, 742147")
                ?: "Jatarpur, Murshidabad, West Bengal, 742147"
        )
    }

    fun saveTuitionProfile(profile: TuitionProfile) {
        prefs.edit()
            .putString("tuitionName", profile.tuitionName)
            .putString("tuitionTagline", profile.tagline)
            .putString("teacherName", profile.teacherName)
            .putString("qualification", profile.qualification)
            .putString("tuitionPhone", profile.phone)
            .putString("tuitionAddress", profile.address)
            .apply()
    }

    fun securityPin(): String {
        val saved = prefs.getString("securityPin", "") ?: ""
        return if (saved.matches(Regex("\\d{4}"))) saved else "1234"
    }

    fun saveSecurityPin(pin: String) {
        if (pin.matches(Regex("\\d{4}"))) {
            prefs.edit().putString("securityPin", pin).apply()
        }
    }

    fun lastBackupDate(): String =
        prefs.getString("lastBackupDate", "") ?: ""

    fun loadBatchRoutines(): List<BatchRoutine> {
        val a = JSONArray(prefs.getString("batchRoutines", "[]"))
        return List(a.length()) { i -> val o=a.getJSONObject(i); BatchRoutine(o.getLong("id"),o.optString("batch"),o.optString("day"),o.optString("start"),o.optString("end")) }
    }
    fun saveBatchRoutines(list: List<BatchRoutine>) {
        val a=JSONArray(); list.forEach { r -> a.put(JSONObject().apply { put("id",r.id);put("batch",r.batch);put("day",r.day);put("start",r.start);put("end",r.end) }) }; prefs.edit().putString("batchRoutines",a.toString()).apply()
    }
    fun loadBatchNotes(): List<BatchNote> {
        val a=JSONArray(prefs.getString("batchNotes", "[]")); return List(a.length()){i->val o=a.getJSONObject(i);BatchNote(o.getLong("id"),o.optString("batch"),o.optString("date"),o.optString("type"),o.optString("text"))}
    }
    fun saveBatchNotes(list: List<BatchNote>) {
        val a=JSONArray(); list.forEach{n->a.put(JSONObject().apply{put("id",n.id);put("batch",n.batch);put("date",n.date);put("type",n.type);put("text",n.text)})};prefs.edit().putString("batchNotes",a.toString()).apply()
    }
    fun loadBatches(): List<Batch> {
        val a = JSONArray(prefs.getString("batches", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            Batch(o.getLong("id"), o.optString("name"), o.optString("address"))
        }
    }

    fun saveBatches(list: List<Batch>) {
        val a = JSONArray()
        list.forEach { b -> a.put(JSONObject().apply { put("id", b.id); put("name", b.name); put("address", b.address) }) }
        prefs.edit().putString("batches", a.toString()).apply()
    }

    fun loadLibrary(): List<LibraryItem> {
        val a = JSONArray(prefs.getString("library", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            LibraryItem(o.getLong("id"), o.optString("name"), o.optString("uri"), o.optString("category", "General"), o.optString("addedDate"))
        }
    }

    fun saveLibrary(list: List<LibraryItem>) {
        val a = JSONArray()
        list.forEach { f -> a.put(JSONObject().apply { put("id", f.id); put("name", f.name); put("uri", f.uri); put("category", f.category); put("addedDate", f.addedDate) }) }
        prefs.edit().putString("library", a.toString()).apply()
    }


    fun loadDailySpent(): List<DailySpent> {
        val a = JSONArray(prefs.getString("dailySpent", "[]"))
        return List(a.length()) { i ->
            val o = a.getJSONObject(i)
            DailySpent(o.getLong("id"), o.optString("date"), o.optString("description"), o.optInt("amount", 0))
        }
    }

    fun saveDailySpent(list: List<DailySpent>) {
        val a = JSONArray()
        list.forEach { item ->
            a.put(JSONObject().apply {
                put("id", item.id)
                put("date", item.date)
                put("description", item.description)
                put("amount", item.amount)
            })
        }
        prefs.edit().putString("dailySpent", a.toString()).apply()
    }

    fun themeMode(): String = prefs.getString("themeMode", "System") ?: "System"
    fun accentName(): String = prefs.getString("accentName", "Blue") ?: "Blue"
    fun saveTheme(mode:String, accent:String) { prefs.edit().putString("themeMode",mode).putString("accentName",accent).apply() }
    fun isProtected(action:String): Boolean = prefs.getBoolean("protect_$action", action in setOf("payment","addStudent","editStudent","deleteStudent","editPayment","deletePayment","restore","attendance","academic","profile","demo"))
    fun setProtected(action:String, value:Boolean) { prefs.edit().putBoolean("protect_$action",value).apply() }

    fun savePayments(list: List<Payment>) {
        val a = JSONArray()
        list.forEach {
            a.put(JSONObject().apply {
                put("studentId", it.studentId)
                put("month", it.month)
                put("amount", it.amount)
                put("date", it.date)
                put("receiptNo", it.receiptNo)
            })
        }
        prefs.edit().putString("payments", a.toString()).apply()
    }
}
