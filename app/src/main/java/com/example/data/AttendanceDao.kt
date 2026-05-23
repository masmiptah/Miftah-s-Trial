package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {
    // Student Queries
    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE classLevel = :classLevel ORDER BY name ASC")
    fun getStudentsByClass(classLevel: String): Flow<List<Student>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Update
    suspend fun updateStudent(student: Student)

    @Delete
    suspend fun deleteStudent(student: Student)

    // Attendance Queries
    @Query("SELECT * FROM attendance_records WHERE date = :date")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :date AND studentId IN (SELECT id FROM students WHERE classLevel = :classLevel)")
    fun getAttendanceForClassAndDate(classLevel: String, date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE studentId = :studentId")
    fun getAttendanceForStudent(studentId: Int): Flow<List<AttendanceRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendance(record: AttendanceRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAttendanceList(records: List<AttendanceRecord>)

    @Query("DELETE FROM attendance_records WHERE studentId = :studentId AND date = :date")
    suspend fun deleteAttendanceRecord(studentId: Int, date: String)
}
