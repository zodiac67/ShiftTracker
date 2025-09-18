package com.example.shifttracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shifttracker.data.ProjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

private enum class PayMode { FIXED, HALF_FIXED, HOURLY }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddShiftDialog(
    projects: List<ProjectEntity>,
    onDismiss: () -> Unit,
    onSave: (date: LocalDate, projectId: Long, hours: Double, customPay: Double, note: String) -> Unit
) {
    val datePickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    var expanded by remember { mutableStateOf(false) }
    var selectedProject by remember { mutableStateOf(projects.firstOrNull()) }

    var payMode by remember { mutableStateOf(PayMode.FIXED) }
    var hoursText by remember { mutableStateOf("") }
    var payText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var alsoNextDay by remember { mutableStateOf(false) }

    // Автоподстановка суммы/очистка полей при смене проекта или режима
    LaunchedEffect(selectedProject, payMode) {
        val fixed = selectedProject?.fixed ?: 0.0
        when (payMode) {
            PayMode.FIXED -> {
                if (fixed > 0) payText = fixed.clean()
                hoursText = ""
            }
            PayMode.HALF_FIXED -> {
                val half = if (fixed > 0) fixed / 2 else 0.0
                if (half > 0) payText = half.clean()
                hoursText = ""
            }
            PayMode.HOURLY -> {
                payText = ""
            }
        }
    }

    val canSave by remember {
        derivedStateOf {
            selectedProject != null &&
            when (payMode) {
                PayMode.HOURLY -> hoursText.isNotBlank()
                else -> payText.isNotBlank()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая смена") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Календарь в скролле — все дни доступны и ничего не «режется»
                DatePicker(state = datePickerState)

                // Проект
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
                                onClick = { selectedProject = p; expanded = false }
                            )
                        }
                    }
                }

                // Режим оплаты
                Text("Тип оплаты", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = payMode == PayMode.FIXED,
                        onClick = { payMode = PayMode.FIXED },
                        label = { Text("Фикс") }
                    )
                    FilterChip(
                        selected = payMode == PayMode.HALF_FIXED,
                        onClick = { payMode = PayMode.HALF_FIXED },
                        label = { Text("1/2 фикс") }
                    )
                    FilterChip(
                        selected = payMode == PayMode.HOURLY,
                        onClick = { payMode = PayMode.HOURLY },
                        label = { Text("Почасовая") }
                    )
                }

                // Часы — актуально только для почасовой
                OutlinedTextField(
                    value = hoursText,
                    onValueChange = { hoursText = it },
                    label = { Text("Часы") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = payMode == PayMode.HOURLY,
                    placeholder = {
                        val rate = selectedProject?.hourly ?: 0.0
                        if (payMode == PayMode.HOURLY && rate > 0) Text("Ставка ${rate.clean()} ₽/час")
                    }
                )

                // Сумма — для фикс/полуфикс; можно править вручную
                OutlinedTextField(
                    value = payText,
                    onValueChange = { payText = it },
                    label = { Text("Сумма (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = payMode != PayMode.HOURLY
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка") },
                    modifier = Modifier.fillMaxWidth()
                )

                Divider()

                Row(modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = alsoNextDay, onCheckedChange = { alsoNextDay = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Добавить такую же смену на следующий день")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val millis = datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    val pid = selectedProject!!.id

                    val hours = hoursText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    val custom = payText.replace(',', '.').toDoubleOrNull() ?: 0.0

                    onSave(date, pid, hours, custom, note)
                    if (alsoNextDay) {
                        onSave(date.plusDays(1), pid, hours, custom, note)
                    }
                    onDismiss()
                }
            ) { Text("Сохранить") }
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
    val canSave by remember { derivedStateOf { name.isNotBlank() } }

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
                    label = { Text("Ставка в час (₽)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fixedText,
                    onValueChange = { fixedText = it },
                    label = { Text("Фикс за смену (₽)") },
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
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

/* -------- helpers -------- */

private fun Double.clean(): String {
    val s = toString()
    return if (s.endsWith(".0")) s.dropLast(2) else s
}