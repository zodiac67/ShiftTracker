package com.example.shifttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
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

    // Кнопка "Сохранить" активна только когда выбран проект и заполнено одно из полей: Часы или Сумма
    val canSave by remember {
        derivedStateOf {
            selectedProject != null && (hoursText.isNotBlank() || payText.isNotBlank())
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая смена") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DatePicker(state = datePickerState)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedProject?.name ?: "Выбери проект",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Проект") },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        projects.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    selectedProject = p
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Часы") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = payText,
                    onValueChange = { payText = it },
                    label = { Text("Сумма вручную") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

                    val hours = hoursText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val custom = payText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val pid = selectedProject!!.id // безопасно, т.к. enabled = canSave

                    onSave(date, pid, hours, custom, note)
                    onDismiss() // закрываем диалог после сохранения
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun AddProjectDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, hourly: Double, fixed: Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hourlyText by remember { mutableStateOf("") }
    var fixedText by remember { mutableStateOf("") }

    val canSave by remember {
        derivedStateOf { name.isNotBlank() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hourlyText,
                    onValueChange = { hourlyText = it },
                    label = { Text("Ставка в час") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fixedText,
                    onValueChange = { fixedText = it },
                    label = { Text("Фикс за смену") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Укажи ИЛИ почасовую ставку, ИЛИ фикс — достаточно одного поля.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val hourly = hourlyText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val fixed = fixedText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onSave(name.trim(), hourly, fixed)
                    onDismiss()
                }
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}