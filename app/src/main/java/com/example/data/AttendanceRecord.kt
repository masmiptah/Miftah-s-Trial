package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: Int,
    val date: String, // YYYY-MM-DD
    val status: String, // "PRESENT", "SICK", "LEAVE", "ABSENT"
    val remarks: String = ""
)
