package com.example.shifttracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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

    var menuOpen by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }

    // режим ввода: почасово или фикс
    var useFixed by remember { mutableStateOf(false) }

    var hoursText by remember { mutableStateOf("") }
    var payText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая смена") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp) // чтобы календарь помещался на экране
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Выберите дату", style = MaterialTheme.typography.titleMedium)
                DatePicker(state = datePickerState)

                // Выбор проекта (без Exposed* API)
                Box {
                    OutlinedTextField(
                        value = selectedProject?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Проект") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { menuOpen = true }
                    )
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        projects.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = {
                                    selectedProject = p
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }

                // Переключатель режима + «½ фикса»
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !useFixed,
                        onClick = { useFixed = false },
                        label = { Text("Часы") }
                    )
                    FilterChip(
                        selected = useFixed,
                        onClick = { useFixed = true },
                        label = { Text("Фикс") }
                    )
                    if (useFixed) {
                        AssistChip(
                            onClick = {
                                val v = payText.replace(',', '.').toDoubleOrNull() ?: 0.0
                                val half = if (v > 0) v / 2.0 else 0.0
                                payText = if (half == 0.0) "" else half.toString()
                            },
                            label = { Text("½ фикса") }
                        )
                    }
                }

                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Часы") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = !useFixed,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = payText,
                    onValueChange = { payText = it },
                    label = { Text("Сумма (фикс)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    enabled = useFixed,
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
            TextButton(onClick = {
                val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

                val projectId = selectedProject?.id ?: 0L
                val hours = hoursText.replace(',', '.').toDoubleOrNull() ?: 0.0
                val fixedPay = payText.replace(',', '.').toDoubleOrNull() ?: 0.0

                if (projectId != 0L) {
                    onSave(date, projectId, hours, if (useFixed) fixedPay else 0.0, note)
                }
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый проект") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
            TextButton(onClick = {
                val hourly = hourlyText.replace(',', '.').toDoubleOrNull() ?: 0.0
                val fixed = fixedText.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (name.isNotBlank()) onSave(name.trim(), hourly, fixed)
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}