package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Student
import com.example.ui.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: AttendanceViewModel = viewModel()
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
                
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        if (currentScreen == Screen.Home || currentScreen == Screen.StudentManagement || currentScreen == Screen.Reports) {
                            BottomNavBar(
                                currentScreen = currentScreen,
                                onTabSelected = { screen -> viewModel.navigateTo(screen) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                            },
                            label = "ScreenTransition"
                        ) { targetScreen ->
                            when (targetScreen) {
                                is Screen.Home -> DashboardScreen(viewModel = viewModel)
                                is Screen.ClassAttendance -> AttendanceTakingScreen(
                                    classLevel = targetScreen.classLevel,
                                    viewModel = viewModel
                                )
                                is Screen.StudentManagement -> StudentManagementScreen(viewModel = viewModel)
                                is Screen.Reports -> ReportsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onTabSelected: (Screen) -> Unit
) {
    NavigationBar(
        modifier = Modifier.testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    ) {
        NavigationBarItem(
            modifier = Modifier.testTag("nav_item_attendance"),
            selected = currentScreen is Screen.Home,
            onClick = { onTabSelected(Screen.Home) },
            label = { Text("Presensi", fontWeight = FontWeight.Medium) },
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Presensi") }
        )
        NavigationBarItem(
            modifier = Modifier.testTag("nav_item_students"),
            selected = currentScreen is Screen.StudentManagement,
            onClick = { onTabSelected(Screen.StudentManagement) },
            label = { Text("Siswa", fontWeight = FontWeight.Medium) },
            icon = { Icon(Icons.Default.Person, contentDescription = "Siswa") }
        )
        NavigationBarItem(
            modifier = Modifier.testTag("nav_item_reports"),
            selected = currentScreen is Screen.Reports,
            onClick = { onTabSelected(Screen.Reports) },
            label = { Text("Laporan", fontWeight = FontWeight.Medium) },
            icon = { Icon(Icons.Default.Info, contentDescription = "Laporan") }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(viewModel: AttendanceViewModel) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val todayStats by viewModel.totalStatsForDate.collectAsStateWithLifecycle(initialValue = Stats())
    val context = LocalContext.current

    val classes = remember { listOf("2A", "2B", "2C", "3A", "3B") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Custom Minimal Header
        item {
            HeaderSection(selectedDate)
        }

        // Horizontal Class Selector Tabs (Mock Navigation)
        item {
            ClassSelectorTabs(
                selectedClass = null,
                onClassSelected = { classLevel ->
                    viewModel.selectClass(classLevel)
                    viewModel.navigateTo(Screen.ClassAttendance(classLevel))
                }
            )
        }

        // Beautiful Date Switcher Row
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.setDate(AttendanceViewModel.getPreviousDate(selectedDate)) },
                        modifier = Modifier.testTag("previous_date_button")
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Tanggal Sebelumnya",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "TANGGAL PRESENSI",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = AttendanceViewModel.getReadableDate(selectedDate),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = { viewModel.setDate(AttendanceViewModel.getNextDate(selectedDate)) },
                        modifier = Modifier.testTag("next_date_button")
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = "Tanggal Berikutnya",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Core Consolidated Stats Section
        item {
            Text(
                text = "Ringkasan Kehadiran",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            StatsSummaryCard(
                currentLabel = "TOTAL REKAP HARI INI",
                totalStudents = todayStats.totalStudents,
                presentCount = todayStats.totalPresent,
                sickCount = todayStats.totalSick,
                leaveCount = todayStats.totalLeave,
                absentCount = todayStats.totalAbsent
            )
        }

        // Classes Header
        item {
            Text(
                text = "Daftar Kelas BTQ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // List of Available Classes with custom indicators
        items(classes) { classLevel ->
            ClassCard(
                classLevel = classLevel,
                onClick = {
                    viewModel.selectClass(classLevel)
                    viewModel.navigateTo(Screen.ClassAttendance(classLevel))
                }
            )
        }
    }
}

@Composable
fun HeaderSection(selectedDate: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Menu,
                        contentDescription = "Menu bar",
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column {
                Text(
                    text = "BTQ Attendance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = AttendanceViewModel.getReadableDate(selectedDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        Surface(
            color = MaterialTheme.colorScheme.secondary,
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Akun Pengguna",
                    tint = MaterialTheme.colorScheme.onSecondary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ClassSelectorTabs(
    selectedClass: String?,
    onClassSelected: (String) -> Unit
) {
    val classes = remember { listOf("2A", "2B", "2C", "3A", "3B") }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("class_selector_tabs"),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(classes) { classLevel ->
            val isSelected = classLevel == selectedClass
            Surface(
                onClick = { onClassSelected(classLevel) },
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .wrapContentWidth()
                    .testTag("class_tab_$classLevel")
            ) {
                Text(
                    text = "Class $classLevel",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSecondary,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun StatsSummaryCard(
    currentLabel: String = "STATUS SEKARANG",
    totalStudents: Int,
    presentCount: Int,
    sickCount: Int,
    leaveCount: Int,
    absentCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MinimalStatsBg
        ),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("stats_summary_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$presentCount / $totalStudents Present",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Absent Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = absentCount.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorAbsent
                    )
                    Text(
                        text = "Abs",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Sick Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = sickCount.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorSick
                    )
                    Text(
                        text = "Sick",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Leave Column
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = leaveCount.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = ColorLeave
                    )
                    Text(
                        text = "Lve",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
fun ClassCard(
    classLevel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("class_card_$classLevel"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = classLevel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Column {
                    Text(
                        text = "Class $classLevel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Students Roll Call",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Buka Presensi",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AttendanceTakingScreen(
    classLevel: String,
    viewModel: AttendanceViewModel
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val students by viewModel.filteredStudents.collectAsStateWithLifecycle()
    val attendanceRecords by viewModel.attendanceRecordsForDate.collectAsStateWithLifecycle()
    val classStats by viewModel.activeClassStats.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("attendance_screen_root")
    ) {
        // App bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Home) },
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Beranda"
                    )
                }

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Class $classLevel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = AttendanceViewModel.getReadableDate(selectedDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Mark all present button
                TextButton(
                    onClick = {
                        viewModel.updateAttendanceBulk(students.map { it.id }, "PRESENT")
                    },
                    modifier = Modifier.testTag("mark_all_present_button")
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Hadir Semua",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Consolidated stats summary card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            StatsSummaryCard(
                currentLabel = "STATUS KELAS $classLevel",
                totalStudents = classStats.totalStudents,
                presentCount = classStats.totalPresent,
                sickCount = classStats.totalSick,
                leaveCount = classStats.totalLeave,
                absentCount = classStats.totalAbsent
            )
        }

        // Students list and search main container card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Main Container Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Students List ($classLevel)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Inner search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search student name...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("student_search_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // List implementation
                if (students.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🕌", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "Siswa tidak ditemukan" else "Daftar siswa kosong",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(students, key = { it.id }) { student ->
                            val record = attendanceRecords[student.id]
                            val activeStatus = record?.status ?: "PRESENT"

                            StudentAttendanceRow(
                                studentName = student.name,
                                studentId = student.id,
                                activeStatus = activeStatus,
                                onStatusChange = { newStatus ->
                                    viewModel.updateAttendance(student.id, newStatus)
                                }
                            )
                        }
                    }
                }

                // Elegant Action Footer Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = {
                            // Beautiful user feedback toast
                            android.widget.Toast.makeText(
                                context,
                                "Presensi Kelas $classLevel Berhasil Disimpan!",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            viewModel.navigateTo(Screen.Home)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(50.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("submit_attendance_button")
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SUBMIT ATTENDANCE",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusIndicatorBadge(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

@Composable
fun StudentAttendanceRow(
    studentName: String,
    studentId: Int,
    activeStatus: String,
    onStatusChange: (String) -> Unit
) {
    // Custom highlights matching the template palette
    val rowBg = when (activeStatus) {
        "SICK" -> ColorSickBg
        "LEAVE" -> ColorLeaveBg
        "ABSENT" -> ColorAbsentBg
        else -> Color.Transparent
    }

    val statusLabelColor = when (activeStatus) {
        "SICK" -> ColorSick
        "ABSENT" -> ColorAbsent
        "LEAVE" -> ColorLeave
        else -> MaterialTheme.colorScheme.outline
    }

    val statusLabelText = when (activeStatus) {
        "SICK" -> "Status: Sick"
        "ABSENT" -> "Status: Absent"
        "LEAVE" -> "Status: Leave"
        else -> "ID: ${202400 + studentId}"
    }

    // Initials Badge text
    val initials = remember(studentName) {
        val parts = studentName.trim().split("\\s+".toRegex())
        val first = parts.firstOrNull()?.firstOrNull()?.toString() ?: ""
        val second = if (parts.size > 1) parts[1].firstOrNull()?.toString() ?: "" else ""
        (first + second).uppercase()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg)
            .testTag("student_card_$studentId")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Initials Logo
                Surface(
                    color = MaterialTheme.colorScheme.secondary,
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                Column {
                    Text(
                        text = studentName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusLabelText,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (activeStatus != "PRESENT") FontWeight.SemiBold else FontWeight.Normal,
                        color = statusLabelColor
                    )
                }
            }

            // Compact Toggler Layout (H | S | I | A) matching test cases fully
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusTogglerButton(
                    label = "H",
                    selected = activeStatus == "PRESENT",
                    activeBgColor = ColorPresent,
                    onClick = { onStatusChange("PRESENT") },
                    modifier = Modifier.testTag("mark_present_button_$studentId")
                )

                StatusTogglerButton(
                    label = "S",
                    selected = activeStatus == "SICK",
                    activeBgColor = ColorSick,
                    onClick = { onStatusChange("SICK") },
                    modifier = Modifier.testTag("mark_sick_button_$studentId")
                )

                StatusTogglerButton(
                    label = "I",
                    selected = activeStatus == "LEAVE",
                    activeBgColor = ColorPresent,
                    onClick = { onStatusChange("LEAVE") },
                    modifier = Modifier.testTag("mark_leave_button_$studentId")
                )

                StatusTogglerButton(
                    label = "A",
                    selected = activeStatus == "ABSENT",
                    activeBgColor = ColorAbsent,
                    onClick = { onStatusChange("ABSENT") },
                    modifier = Modifier.testTag("mark_absent_button_$studentId")
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
    }
}

@Composable
fun StatusTogglerButton(
    label: String,
    selected: Boolean,
    activeBgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (selected) activeBgColor else Color.Transparent,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color.Transparent else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun StudentAttendanceCard(
    studentName: String,
    activeStatus: String,
    onStatusChange: (String) -> Unit,
    studentId: Int
) {
    // Keep compatible helper for other classes if they resolve references
    StudentAttendanceRow(
        studentName = studentName,
        studentId = studentId,
        activeStatus = activeStatus,
        onStatusChange = onStatusChange
    )
}

@Composable
fun AttendanceSelectorButton(
    label: String,
    selected: Boolean,
    activeColor: Color,
    activeBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Keep compatible helper
    StatusTogglerButton(
        label = label.substring(0, 1),
        selected = selected,
        activeBgColor = activeColor,
        onClick = onClick,
        modifier = modifier
    )
}

// Student Management Scren allows custom user lists
@Composable
fun StudentManagementScreen(viewModel: AttendanceViewModel) {
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val classes = remember { listOf("2A", "2B", "2C", "3A", "3B") }
    var selectedClassTab by remember { mutableStateOf("2A") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<Student?>(null) }
    var newStudentName by remember { mutableStateOf("") }
    var editStudentName by remember { mutableStateOf("") }

    val currentClassStudents = remember(allStudents, selectedClassTab) {
        allStudents.filter { it.classLevel == selectedClassTab }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("students_screen_root")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Screen Header title
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 1.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Kelola Siswa",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Tambah, ubah, atau hapus nama murid tiap kelas BTQ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Tabs for each Class
            ScrollableTabRow(
                selectedTabIndex = classes.indexOf(selectedClassTab),
                edgePadding = 16.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.background
            ) {
                classes.forEachIndexed { index, classLevel ->
                    Tab(
                        selected = selectedClassTab == classLevel,
                        onClick = { selectedClassTab = classLevel },
                        text = { Text("Kelas $classLevel", fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Student list under tab
            if (currentClassStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🧑‍🎓", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada siswa di kelas ini",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tambahkan siswa menggunakan tombol dibawah",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(currentClassStudents, key = { it.id }) { student ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = student.name.firstOrNull()?.uppercase() ?: "?",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Text(
                                        text = student.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            showEditDialog = student
                                            editStudentName = student.name
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit Siswa",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteStudent(student) }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Hapus Siswa",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Add Floating Action Button to Add Student
        FloatingActionButton(
            onClick = {
                newStudentName = ""
                showAddDialog = true
            },
            containerColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_student_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Tambah Murid", tint = Color.White)
        }

        // Add dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Tambah Siswa Baru", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Nama Siswa untuk Kelas $selectedClassTab:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newStudentName,
                            onValueChange = { newStudentName = it },
                            placeholder = { Text("Masukkan nama lengkap...") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newStudentName.isNotBlank()) {
                                viewModel.addStudent(newStudentName, selectedClassTab)
                                showAddDialog = false
                            }
                        }
                    ) {
                        Text("Simpan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Edit dialog
        showEditDialog?.let { student ->
            AlertDialog(
                onDismissRequest = { showEditDialog = null },
                title = { Text("Ubah Nama Siswa", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Edit Nama Siswa Kelas ${student.classLevel}:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = editStudentName,
                            onValueChange = { editStudentName = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (editStudentName.isNotBlank()) {
                                viewModel.updateStudent(student.copy(name = editStudentName.trim()))
                                showEditDialog = null
                            }
                        }
                    ) {
                        Text("Ubah", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = null }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}

// Reports View with graphical indicators and scrolling student report summaries
@Composable
fun ReportsScreen(viewModel: AttendanceViewModel) {
    val allStudents by viewModel.allStudents.collectAsStateWithLifecycle()
    val classes = remember { listOf("2A", "2B", "2C", "3A", "3B") }
    
    // Compute student stats aggregations inside scope to save database queries
    // Usually calculated in viewModel, but simple in memory calculation is extremely quick for 100 students
    var selectedClassTab by remember { mutableStateOf("2A") }
    
    val currentClassStudents = remember(allStudents, selectedClassTab) {
        allStudents.filter { it.classLevel == selectedClassTab }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("reports_screen_root"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Text(
                        text = "Ringkasan Laporan BTQ",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Statistik kehadiran kumulatif dan monitoring keaktifan santri/siswa belajar Al-Qur'an.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }
            }
        }

        // Scrollable Class tab selection
        item {
            ScrollableTabRow(
                selectedTabIndex = classes.indexOf(selectedClassTab),
                edgePadding = 0.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.background
            ) {
                classes.forEachIndexed { index, classLevel ->
                    Tab(
                        selected = selectedClassTab == classLevel,
                        onClick = { selectedClassTab = classLevel },
                        text = { Text("Kelas $classLevel", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Daftar Murid Kelas $selectedClassTab",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total: ${currentClassStudents.size} Anak",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (currentClassStudents.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tidak ada siswa tersedia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(currentClassStudents, key = { it.id }) { student ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = student.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mata Pelajaran: BTQ",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }

                        // Compact badge indicating student registration status
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Aktif",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
