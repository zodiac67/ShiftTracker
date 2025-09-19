package com.example.shifttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.shifttracker.data.ShiftWithProject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel) {
    val ym by vm.currentYM.collectAsState()
    val summary by vm.summary.collectAsState()
    val shifts by vm.shifts.collectAsState()
    val projects by vm.projects.collectAsState()

    var showAddShift by remember { mutableStateOf(false) }
    var showAddProject by remember { mutableStateOf(false) }
    var showContact by remember { mutableStateOf(false) }

    val monthTitle = remember(ym) {
        ym.atDay(1)
            .format(DateTimeFormatter.ofPattern("LLLL yyyy", Locale.getDefault()))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(monthTitle) },
                navigationIcon = { IconButton(onClick = vm::prevMonth) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = {
                    IconButton(onClick = vm::nextMonth) { Icon(Icons.Filled.ArrowForward, null) }
                    IconButton(onClick = { showAddProject = true }) { Icon(Icons.Filled.Settings, null) }
                    IconButton(onClick = { showContact = true }) { Icon(Icons.Filled.Info, null) }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddShift = true }) { Icon(Icons.Filled.Add, null) }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Итоги", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Зарплата", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMoney(summary.totalPay), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Смен: ${summary.count}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (shifts.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Нет смен в этом месяце", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(shifts, key = { it.shift.id }) { item ->
                        ShiftRow(item, onDelete = { vm.deleteShift(item.shift) })
                    }
                }
            }
        }
    }

    if (showAddShift) {
        AddShiftDialog(
            projects = projects,
            onDismiss = { showAddShift = false },
            onSave = { date, projectId, hours, customPay, note ->
                vm.addShift(date, projectId, hours, customPay, note)
                showAddShift = false
            }
        )
    }
    if (showAddProject) {
        AddProjectDialog(
            onDismiss = { showAddProject = false },
            onSave = { n, h, f -> vm.addProject(n, h, f); showAddProject = false }
        )
    }
    if (showContact) {
        ContactDialog(onDismiss = { showContact = false })
    }
}

@Composable
private fun ShiftRow(item: ShiftWithProject, onDelete: () -> Unit) {
    val s = item.shift
    val p = item.project
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(formatDate(s.date), style = MaterialTheme.typography.titleMedium)
                Text(p.name, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val info = buildString {
                    if (s.hours > 0) append("${s.hours} ч • ")
                    if (s.customPay > 0) append("сумма вручную ") else {
                        if (p.hourlyRate > 0) append("по ${formatMoney(p.hourlyRate)}/ч ")
                        else if (p.fixedPerShift > 0) append("фикс за смену ")
                    }
                }
                if (info.isNotBlank()) Text(info.trim(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (s.note.isNotBlank()) Text(s.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onDelete) { Text("Удалить") }
        }
    }
}

@Composable
fun ContactDialog(onDismiss: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val handle = "@Zodiac767"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Связаться с разработчиком") },
        text = { Text("Телеграм: $handle") },
        confirmButton = {
            TextButton(onClick = {
                val tgUri = android.net.Uri.parse("tg://resolve?domain=Zodiac767")
                val tgIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, tgUri)
                tgIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(tgIntent)
                } catch (e: Exception) {
                    val webUri = android.net.Uri.parse("https://t.me/Zodiac767")
                    val webIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, webUri)
                    webIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(webIntent)
                }
            }) { Text("Открыть в Telegram") }
        },
        dismissButton = {
            TextButton(onClick = { clipboard.setText(androidx.compose.ui.text.AnnotatedString(handle)); onDismiss() }) {
                Text("Скопировать ник")
            }
        }
    )
}

private fun formatDate(d: LocalDate): String =
    d.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

private fun formatMoney(v: Double): String =
    "%,.2f".format(v).replace(',', ' ').replace('.', ',') + " ₽"