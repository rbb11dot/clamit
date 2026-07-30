package com.clamit.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleHomePage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onMenuClick: () -> Unit,
    onDatePicker: () -> Unit,
    onTemplatePicker: () -> Unit,
    onAddBlock: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("tr"))

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = "Menü") }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        IconButton(onClick = viewModel::goToPreviousDay, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Önceki", modifier = Modifier.size(20.dp))
                        }
                        Column(
                            modifier = Modifier.clickable(onClick = onDatePicker),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(uiState.currentDate.format(formatter), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                            Text(uiState.currentDate.format(dayFormatter), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                        IconButton(onClick = viewModel::goToNextDay, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Sonraki", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onTemplatePicker) { Icon(Icons.Default.Schedule, "Şablon seç") }
                    IconButton(onClick = viewModel::goToToday) { Icon(Icons.Default.CalendarToday, "Bugün") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddBlock) { Icon(Icons.Default.Add, "Blok ekle") }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.error != null -> Text(text = "Hata: ${uiState.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                else -> {
                    val entry = uiState.entry
                    if (entry == null) {
                        Text("Yükleniyor...", Modifier.align(Alignment.Center))
                    } else if (entry.blocks.isEmpty()) {
                        Column(
                            Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                                            Text("Bu gün için zaman bloğu yok.", style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(8.dp))
                            Text("Butonuna basarak blok ekleyin.", style = MaterialTheme.typography.bodyMedium)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(entry.blocks, key = { it.timeBlockId }) { block ->
                                TimeBlockCard(
                                    block = block,
                                    onToggleSubtask = { viewModel.toggleSubtask(block.timeBlockId, it) },
                                    onSetCompleted = { viewModel.setManualStatus(block.timeBlockId, "completed") },
                                    onSetNotCompleted = { viewModel.setManualStatus(block.timeBlockId, "not_completed") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
