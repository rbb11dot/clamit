package com.clamit.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.currentDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    viewModel.setDate(
                        Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    )
                }
                onDismiss()
            }) { Text("Seç") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
fun TemplatePickerDialog(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gün Şablonu Seç") },
        text = {
            Column {
                TextButton(onClick = {
                    viewModel.setEntryTemplate(null)
                    onDismiss()
                }) { Text("Özel Gün (şablonsuz)") }
                Divider()
                uiState.templates.forEach { t ->
                    TextButton(onClick = {
                        viewModel.setEntryTemplate(t.id)
                        onDismiss()
                    }) {
                        Icon(ScheduleIcons.getIconOrDefault(t.icon), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(t.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
