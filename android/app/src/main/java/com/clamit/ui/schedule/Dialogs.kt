package com.clamit.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
            .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    viewModel.setDate(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
                }
                onDismiss()
            }) { Text("Seç") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("İptal") } },
        text = { DatePicker(state = datePickerState) }
    )
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
                TextButton(onClick = { onDismiss() }) {
                    Text("Şablon Seçin")
                }
                HorizontalDivider()
                uiState.templates.forEach { t ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setEntryTemplate(t.id)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Icon(
                            ScheduleIcons.getIconOrDefault(t.icon),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(text = t.name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("İptal") } }
    )
}
