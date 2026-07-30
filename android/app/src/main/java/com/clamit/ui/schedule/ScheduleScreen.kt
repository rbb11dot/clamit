package com.clamit.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            ScheduleTopBar(
                currentDate = uiState.currentDate,
                templateName = when {
                    uiState.entry?.isSpecial == true -> "Özel Gün"
                    uiState.entry?.templateName != null -> uiState.entry!!.templateName!!
                    else -> "Şablon Seçin"
                },
                onPreviousDay = viewModel::goToPreviousDay,
                onNextDay = viewModel::goToNextDay,
                onToday = viewModel::goToToday,
                onDateClick = { showDatePicker = true },
                onTemplateClick = { showTemplatePicker = true }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null -> {
                    Text(
                        text = "Hata: ${uiState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.entry != null -> {
                    ScheduleEntryContent(
                        entry = uiState.entry!!,
                        onToggleSubtask = { blockId, subtaskId ->
                            viewModel.toggleSubtask(blockId, subtaskId)
                        },
                        onSetManualStatus = { blockId, status ->
                            viewModel.setManualStatus(blockId, status)
                        }
                    )
                }
                else -> {
                    Text(
                        text = "Yükleniyor...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.currentDate
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.setDate(date)
                    }
                    showDatePicker = false
                }) { Text("Seç") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("İptal") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Template picker dialog
    if (showTemplatePicker) {
        AlertDialog(
            onDismissRequest = { showTemplatePicker = false },
            title = { Text("Gün Şablonu Seç") },
            text = {
                Column {
                    TextButton(onClick = {
                        viewModel.setEntryTemplate(null)
                        showTemplatePicker = false
                    }) {
                        Text("Özel Gün (şablonsuz)")
                    }
                    Divider()
                    uiState.templates.forEach { template ->
                        TextButton(onClick = {
                            viewModel.setEntryTemplate(template.id)
                            showTemplatePicker = false
                        }) {
                            Text("${template.icon} ${template.name}")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplatePicker = false }) { Text("İptal") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTopBar(
    currentDate: LocalDate,
    templateName: String,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onToday: () -> Unit,
    onDateClick: () -> Unit,
    onTemplateClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("tr"))

    Column {
        TopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentDate.format(formatter),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = currentDate.format(dayFormatter),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            navigationIcon = {
                Row {
                    IconButton(onClick = onPreviousDay) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Önceki gün")
                    }
                    TextButton(onClick = onDateClick) {
                        Text("📅")
                    }
                }
            },
            actions = {
                IconButton(onClick = onToday) {
                    Icon(Icons.Default.Today, contentDescription = "Bugün")
                }
                IconButton(onClick = onNextDay) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Sonraki gün")
                }
            }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            AssistantChip(
                onClick = onTemplateClick,
                label = { Text(templateName) },
                leadingIcon = {
                    if (templateName == "Özel Gün") Text("📌") else Text("📋")
                }
            )
        }
        Divider()
    }
}

@Composable
fun ScheduleEntryContent(
    entry: com.clamit.data.model.ScheduleEntry,
    onToggleSubtask: (String, String) -> Unit,
    onSetManualStatus: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (entry.blocks.isEmpty()) {
            item {
                Text(
                    text = "Bu gün için planlanmış zaman bloğu yok.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 32.dp)
                )
            }
        }

        items(entry.blocks) { block ->
            TimeBlockCard(
                block = block,
                onToggleSubtask = { subtaskId -> onToggleSubtask(block.timeBlockId, subtaskId) },
                onSetCompleted = { onSetManualStatus(block.timeBlockId, "completed") },
                onSetNotCompleted = { onSetManualStatus(block.timeBlockId, "not_completed") }
            )
        }
    }
}
