package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AttendanceRecord
import com.example.data.AttendanceRepository
import com.example.data.Student
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalCoroutinesApi::class)
class AttendanceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = AttendanceRepository(database.attendanceDao())
        
        // Ensure database is prepopulated with default student lists
        viewModelScope.launch(Dispatchers.IO) {
            repository.checkAndPrepopulateIfEmpty()
        }
    }

    // Navigation State
    private val _currentClass = MutableStateFlow("2A")
    val currentClass: StateFlow<String> = _currentClass.asStateFlow()

    private val _selectedDate = MutableStateFlow(getCurrentDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Screen State
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Students for current class
    val currentClassStudents: StateFlow<List<Student>> = _currentClass
        .flatMapLatest { classLevel -> repository.getStudentsByClass(classLevel) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered students for attendance screen
    val filteredStudents: StateFlow<List<Student>> = combine(
        currentClassStudents,
        _searchQuery
    ) { students, query ->
        if (query.isBlank()) {
            students
        } else {
            students.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Attendance Records for current class and selected date
    val attendanceRecordsForDate: StateFlow<Map<Int, AttendanceRecord>> = combine(
        _currentClass,
        _selectedDate
    ) { classLevel, date ->
        Pair(classLevel, date)
    }.flatMapLatest { (classLevel, date) ->
        repository.getAttendanceForClassAndDate(classLevel, date)
    }.map { records ->
        records.associateBy { it.studentId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Overall attendance statistics for selected date (cached of all database)
    val totalStatsForDate: Flow<Stats> = _selectedDate.flatMapLatest { date ->
        repository.getAttendanceForDate(date)
    }.combine(repository.getAllStudents()) { records, students ->
        val totalStudents = students.size
        if (totalStudents == 0) return@combine Stats()

        val markedRecords = records.associateBy { it.studentId }
        val sick = records.count { it.status == "SICK" }
        val leave = records.count { it.status == "LEAVE" }
        val absent = records.count { it.status == "ABSENT" }
        
        // Students are PRESENT by default unless marked sick/leave/absent
        val present = students.count { student ->
            val record = markedRecords[student.id]
            record == null || record.status == "PRESENT"
        }

        Stats(
            totalStudents = totalStudents,
            totalPresent = present,
            totalSick = sick,
            totalLeave = leave,
            totalAbsent = absent
        )
    }

    // Active stats specifically for the chosen class on the chosen date
    val activeClassStats: StateFlow<Stats> = combine(
        currentClassStudents,
        attendanceRecordsForDate
    ) { students, recordsMap ->
        val total = students.size
        if (total == 0) return@combine Stats()

        var sick = 0
        var leave = 0
        var absent = 0
        var present = 0

        for (student in students) {
            val record = recordsMap[student.id]
            when (record?.status) {
                "SICK" -> sick++
                "LEAVE" -> leave++
                "ABSENT" -> absent++
                "PRESENT", null -> present++
            }
        }

        Stats(
            totalStudents = total,
            totalPresent = present,
            totalSick = sick,
            totalLeave = leave,
            totalAbsent = absent
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats())

    // All registered students across all classes
    val allStudents: StateFlow<List<Student>> = repository.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // State actions
    fun selectClass(classLevel: String) {
        _currentClass.value = classLevel
        _searchQuery.value = ""
    }

    fun setDate(date: String) {
        _selectedDate.value = date
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun updateAttendance(studentId: Int, status: String, remarks: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val record = AttendanceRecord(
                    studentId = studentId,
                    date = _selectedDate.value,
                    status = status,
                    remarks = remarks
                )
                repository.insertOrUpdateAttendance(record)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateAttendanceBulk(studentIds: List<Int>, status: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val records = studentIds.map { id ->
                    AttendanceRecord(
                        studentId = id,
                        date = _selectedDate.value,
                        status = status
                    )
                }
                repository.insertOrUpdateAttendanceList(records)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun addStudent(name: String, classLevel: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (name.isNotBlank()) {
                    repository.insertStudent(Student(name = name.trim(), classLevel = classLevel))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun deleteStudent(student: Student) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteStudent(student)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateStudent(student: Student) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.updateStudent(student)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }

        fun getReadableDate(dateStr: String): String {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                // Set to Indonesian locale or US fallback depending on configuration
                val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
                val date = parser.parse(dateStr) ?: return dateStr
                return formatter.format(date)
            } catch (e: Exception) {
                try {
                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formatter = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US)
                    val date = parser.parse(dateStr) ?: return dateStr
                    return formatter.format(date)
                } catch (ex: Exception) {
                    return dateStr
                }
            }
        }

        fun getPreviousDate(dateStr: String): String {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(dateStr) ?: return dateStr
                val cal = Calendar.getInstance()
                cal.time = date
                cal.add(Calendar.DATE, -1)
                return sdf.format(cal.time)
            } catch (e: Exception) {
                return dateStr
            }
        }

        fun getNextDate(dateStr: String): String {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val date = sdf.parse(dateStr) ?: return dateStr
                val cal = Calendar.getInstance()
                cal.time = date
                cal.add(Calendar.DATE, 1)
                return sdf.format(cal.time)
            } catch (e: Exception) {
                return dateStr
            }
        }
    }
}

sealed class Screen {
    object Home : Screen()
    data class ClassAttendance(val classLevel: String) : Screen()
    object StudentManagement : Screen()
    object Reports : Screen()
}

data class Stats(
    val totalStudents: Int = 0,
    val totalPresent: Int = 0,
    val totalSick: Int = 0,
    val totalLeave: Int = 0,
    val totalAbsent: Int = 0
)
