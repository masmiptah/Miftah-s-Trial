package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AttendanceRepository(private val dao: AttendanceDao) {

    fun getStudentsByClass(classLevel: String): Flow<List<Student>> = dao.getStudentsByClass(classLevel)

    fun getAllStudents(): Flow<List<Student>> = dao.getAllStudents()

    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>> = dao.getAttendanceForDate(date)

    fun getAttendanceForClassAndDate(classLevel: String, date: String): Flow<List<AttendanceRecord>> = 
        dao.getAttendanceForClassAndDate(classLevel, date)

    fun getAttendanceForStudent(studentId: Int): Flow<List<AttendanceRecord>> = dao.getAttendanceForStudent(studentId)

    suspend fun insertStudent(student: Student) = dao.insertStudent(student)

    suspend fun updateStudent(student: Student) = dao.updateStudent(student)

    suspend fun deleteStudent(student: Student) = dao.deleteStudent(student)

    suspend fun insertOrUpdateAttendance(record: AttendanceRecord) = dao.insertOrUpdateAttendance(record)

    suspend fun insertOrUpdateAttendanceList(records: List<AttendanceRecord>) = dao.insertOrUpdateAttendanceList(records)

    suspend fun deleteAttendanceRecord(studentId: Int, date: String) = dao.deleteAttendanceRecord(studentId, date)

    suspend fun checkAndPrepopulateIfEmpty() {
        val currentStudents = dao.getAllStudents().first()
        if (currentStudents.isEmpty()) {
            val classesMap = mapOf(
                "2A" to listOf(
                    "Abdul Hamid", "Aisha Humaira", "Fardan Al-Ghifari", 
                    "Fatimah Zahra", "Muhammad Al-Ghazali", "Naura Shifa", 
                    "Rania Azzahra", "Rizky Aditya", "Salman Al-Farisi", "Yusuf Ibrahim"
                ),
                "2B" to listOf(
                    "Ahmad Fauzi", "Alyaa Nabila", "Bilal Ramadhan", 
                    "Clarissa Putri", "Farhan Maulana", "Hafiz Syahputra", 
                    "Khadijah Al-Kubra", "Muhammad Fatih", "Sarah Amira", "Zaky Mubarak"
                ),
                "2C" to listOf(
                    "Akmal Hakim", "Annisa Fitriani", "Fairuz Zaki", 
                    "Luthfi Ananda", "Nabila Salsabila", "Rafif Al-Hafiz", 
                    "Siti Aminah", "Tri Wardhana", "Umar bin Khattab", "Zahira Syifa"
                ),
                "3A" to listOf(
                    "Abidzar Al-Ghifari", "Amira Yasmin", "Devan Pratama", 
                    "Fahri Rahman", "Kaysha Nabila", "Muhammad Ali", 
                    "Nayla Az-Zahra", "Rayhan Ramadhan", "Siti Fatimah", "Ziyad Al-Farabi"
                ),
                "3B" to listOf(
                    "Adiba Khanza", "Anas Mushthofa", "Dzaki Al-Farabi", 
                    "Faizah Nurul", "Kayla Putri", "Muhammad Yusuf", 
                    "Rahma Amelia", "Syamil Basayev", "Tsabitah Azzahra", "Wildan Firdaus"
                )
            )

            for ((classLevel, students) in classesMap) {
                for (studentName in students) {
                    dao.insertStudent(Student(name = studentName, classLevel = classLevel))
                }
            }
        }
    }
}
