package com.example.shifttracker.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shifttracker.data.ProjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShiftDialog(
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, projectId: Long, hours: Double, customPay: Double, note: String) -> Unit
) {
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
    var expanded by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }
    var hoursText by remember { mutableStateOf("") }
    var payText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая смена") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DatePicker(state = datePickerState)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedProject?.name ?: "Выбери проект",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Проект") },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        projects.forEach { p ->
                            DropdownMenuItem(text = { Text(p.name) }, onClick = { selectedProject = p; expanded = false })
                        }
                    }
                }
                OutlinedTextField(value = hoursText, onValueChange = { hoursText = it }, label = { Text("Часы") },
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = payText, onValueChange = { payText = it }, label = { Text("Сумма вручную") },
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Заметка") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                val hours = hoursText.replace(",", ".").toDoubleOrNull() ?: 0.0
                val custom = payText.replace(",", ".").toDoubleOrNull() ?: 0.0
                val pid = selectedProject?.id ?: 0L
                if (pid != 0L) onSave(date, pid, hours, custom, note)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun AddProjectDialog(onDismiss: () -> Unit, onSave: (name: String, hourly: Double, fixed: Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var hourlyText by remember { mutableStateOf("") }
    var fixedText by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = hourlyText, onValueChange = { hourlyText = it }, label = { Text("Ставка в час") },
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fixedText, onValueChange = { fixedText = it }, label = { Text("Фикс за смену") },
                    keyboardOptions = androidx.compose.ui.text.input.KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Text("Укажи ИЛИ почасовую ставку, ИЛИ фикс — достаточно одного поля.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val hourly = hourlyText.replace(",", ".").toDoubleOrNull() ?: 0.0
                val fixed = fixedText.replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) onSave(name.trim(), hourly, fixed)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
