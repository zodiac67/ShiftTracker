package com.example.shifttracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.shifttracker.data.ProjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class PayMode { HOURLY, FIXED, HALF_FIXED }

/** Фикс за смену из проекта (твоё поле называется fixedPerShift). */
private fun ProjectEntity.fixedValueOrZero(): Double {
    return runCatching {
        val f = this::class.java.getDeclaredField("fixedPerShift").apply { isAccessible = true }
        (f.get(this) as? Number)?.toDouble() ?: 0.0
    }.getOrDefault(0.0)
}

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

    var payMode by remember { mutableStateOf(PayMode.HOURLY) }
    var hoursText by remember { mutableStateOf("") }
    var customPayText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(),
        title = { Text("Новая смена") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Выберите дату", style = MaterialTheme.typography.titleMedium)
                DatePicker(state = datePickerState, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = selectedProject?.name.orEmpty(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Проект") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        projects.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.name) },
                                onClick = { selectedProject = p; expanded = false }
                            )
                        }
                    }
                }

                Text("Оплата", style = MaterialTheme.typography.titleSmall)
                val modes = PayMode.values()
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = payMode == mode,
                            onClick = { payMode = mode; error = null },
                            shape = SegmentedButtonDefaults.itemShape(index, modes.size),
                            label = {
                                Text(
                                    when (mode) {
                                        PayMode.HOURLY -> "Часы"
                                        PayMode.FIXED -> "Фикс"
                                        PayMode.HALF_FIXED -> "½ фикс"
                                    }
                                )
                            }
                        )
                    }
                }

                if (payMode == PayMode.HOURLY) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { hoursText = it; error = null },
                        label = { Text("Часы") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = customPayText,
                    onValueChange = { customPayText = it; error = null },
                    label = { Text("Сумма вручную (опционально)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val project = selectedProject
                if (project == null) { error = "Выберите проект"; return@TextButton }

                val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()

                val manual = customPayText.replace(",", ".").toDoubleOrNull()
                val projectFixed = project.fixedValueOrZero()

                val (hours, customPay) = when (payMode) {
                    PayMode.HOURLY -> {
                        val h = hoursText.replace(",", ".").toDoubleOrNull()
                        if (h == null || h <= 0.0) {
                            error = "Укажите количество часов"; return@TextButton
                        }
                        // customPay=0.0 → рассчитает слой данных по ставке проекта
                        h to (manual ?: 0.0)
                    }
                    PayMode.FIXED -> {
                        val pay = manual ?: projectFixed
                        if (pay <= 0.0) {
                            error = "Нет фикс-ставки у проекта. Введите сумму вручную."
                            return@TextButton
                        }
                        1.0 to pay // hours > 0 — чтобы запись не отбрасывалась
                    }
                    PayMode.HALF_FIXED -> {
                        val pay = manual ?: (projectFixed * 0.5)
                        if (pay <= 0.0) {
                            error = "Нет фикс-ставки у проекта. Введите сумму вручную."
                            return@TextButton
                        }
                        1.0 to pay
                    }
                }

                onSave(date, project.id, hours, customPay, note)
                onDismiss()
            }) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
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
                    "Заполни ИЛИ почасовую, ИЛИ фикс — достаточно одного поля.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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