package com.clamit.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocksPage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onMenuClick: () -> Unit,
    onNewBlock: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menü") } },
                title = { Text("Zaman Blokları") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewBlock) { Icon(Icons.Default.Add, "Yeni blok") }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Zaman blokları şablonlar içinde yönetilir.",
                style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Text("Bir şablon oluşturup içine blok ekleyin.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ---- Full Screen Block Editor ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorPage(
    onDismiss: () -> Unit,
    viewModel: ScheduleViewModel
) {
    var name by remember { mutableStateOf("") }
    var blockIcon by remember { mutableStateOf("alarm") }
    var mode by remember { mutableStateOf("start_end") }
    var startHour by remember { mutableStateOf("07") }
    var startMin by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("07") }
    var endMin by remember { mutableStateOf("30") }
    var duration by remember { mutableStateOf("30") }
    var subtasks by remember { mutableStateOf(listOf("")) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Zaman Bloğu Oluştur") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "Geri") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon + Name
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconPickerButton(currentIcon = blockIcon) { blockIcon = it }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        label = { Text("Blok adı") }, placeholder = { Text("Sabah rutini") },
                        singleLine = true, modifier = Modifier.weight(1f))
                }
            }

            // Mode
            item {
                Text("Zaman modu", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "start_end",
                        onClick = { mode = "start_end" },
                        label = { Text("Başlangıç + Bitiş") }
                    )
                    FilterChip(
                        selected = mode == "start_duration",
                        onClick = { mode = "start_duration" },
                        label = { Text("Başlangıç + Süre") }
                    )
                }
            }

            // Start time
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startHour, onValueChange = { if (it.length <= 2) startHour = it },
                        label = { Text("Saat") }, singleLine = true, modifier = Modifier.weight(1f))
                    Text(":", modifier = Modifier.padding(top = 24.dp))
                    OutlinedTextField(value = startMin, onValueChange = { if (it.length <= 2) startMin = it },
                        label = { Text("Dk") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }

            if (mode == "start_end") {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = endHour, onValueChange = { if (it.length <= 2) endHour = it },
                            label = { Text("Bitiş saati") }, singleLine = true, modifier = Modifier.weight(1f))
                        Text(":", modifier = Modifier.padding(top = 24.dp))
                        OutlinedTextField(value = endMin, onValueChange = { if (it.length <= 2) endMin = it },
                            label = { Text("Dk") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            } else {
                item {
                    OutlinedTextField(value = duration, onValueChange = { if (it.length <= 3) duration = it },
                        label = { Text("Süre (dakika)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            // Subtasks
            item { Text("Subtask'ler", fontWeight = FontWeight.Medium) }
            items(subtasks.indices.toList(), key = { it }) { index ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⠿", modifier = Modifier.padding(end = 8.dp))
                    OutlinedTextField(
                        value = subtasks[index],
                        onValueChange = { n -> subtasks = subtasks.toMutableList().also { it[index] = n } },
                        placeholder = { Text("Subtask ${index + 1}") }, singleLine = true,
                        modifier = Modifier.weight(1f))
                    if (subtasks.size > 1) {
                        IconButton(onClick = { subtasks = subtasks.toMutableList().also { it.removeAt(index) } }) {
                            Icon(Icons.Default.Close, "Sil")
                        }
                    }
                }
            }
            item {
                TextButton(onClick = { subtasks = subtasks + "" }) {
                    Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Subtask ekle")
                }
            }

            // Save
            item {
                Button(
                    onClick = {
                        val st = "${startHour.padStart(2,'0')}:${startMin.padStart(2,'0')}"
                        val et = if (mode == "start_end") "${endHour.padStart(2,'0')}:${endMin.padStart(2,'0')}" else null
                        val dur = if (mode == "start_duration") duration.toIntOrNull() else null
                        viewModel.createBlock("", name, blockIcon, mode, st, et, dur, subtasks.filter { it.isNotBlank() })
                        onDismiss()
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kaydet") }
            }
        }
    }
}
