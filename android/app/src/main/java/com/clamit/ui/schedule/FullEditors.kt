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
fun FullBlockEditor(
    onDismiss: () -> Unit,
    viewModel: ScheduleViewModel
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("alarm") }
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
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Kapat") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon picker
            item {
                Text("İkon", fontWeight = FontWeight.Medium)
                val iconRows = ScheduleIcons.all.chunked(6)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    iconRows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { iconName ->
                                FilterChip(
                                    selected = selectedIcon == iconName,
                                    onClick = { selectedIcon = iconName },
                                    leadingIcon = { Icon(ScheduleIcons.getIconOrDefault(iconName), null, Modifier.size(20.dp)) },
                                    label = { Text("") }
                                )
                            }
                        }
                    }
                }
            }

            // Name
            item {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Blok adı") }, placeholder = { Text("Sabah rutini") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            // Mode
            item {
                Text("Zaman modu", fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = mode == "start_end", onClick = { mode = "start_end" }, label = { Text("Başlangıç + Bitiş") })
                    FilterChip(selected = mode == "start_duration", onClick = { mode = "start_duration" }, label = { Text("Başlangıç + Süre") })
                }
            }

            // Time
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startHour, onValueChange = { if (it.length <= 2) startHour = it }, label = { Text("Saat") }, singleLine = true, modifier = Modifier.weight(1f))
                    Text(":", modifier = Modifier.padding(top = 16.dp))
                    OutlinedTextField(value = startMin, onValueChange = { if (it.length <= 2) startMin = it }, label = { Text("Dk") }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }

            if (mode == "start_end") {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = endHour, onValueChange = { if (it.length <= 2) endHour = it }, label = { Text("Bitiş saati") }, singleLine = true, modifier = Modifier.weight(1f))
                        Text(":", modifier = Modifier.padding(top = 16.dp))
                        OutlinedTextField(value = endMin, onValueChange = { if (it.length <= 2) endMin = it }, label = { Text("Bitiş dk") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }
            } else {
                item {
                    OutlinedTextField(value = duration, onValueChange = { if (it.length <= 3) duration = it }, label = { Text("Süre (dk)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            // Subtasks
            item { Text("Subtask'ler", fontWeight = FontWeight.Medium) }
            items(subtasks.indices.toList(), key = { it }) { index ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⠿", modifier = Modifier.padding(end = 8.dp))
                    OutlinedTextField(value = subtasks[index], onValueChange = { n -> subtasks = subtasks.toMutableList().also { it[index] = n } },
                        placeholder = { Text("Subtask ${index + 1}") }, singleLine = true, modifier = Modifier.weight(1f))
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
                        viewModel.createBlock("", name, selectedIcon, mode, st, et, dur, subtasks.filter { it.isNotBlank() })
                        onDismiss()
                    },
                    enabled = name.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Kaydet") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullTemplateEditor(
    onDismiss: () -> Unit,
    viewModel: ScheduleViewModel
) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("format_list_bulleted") }
    val selectedDays = remember { mutableStateListOf<Int>() }
    val dayNames = listOf("Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gün Şablonu Oluştur") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Kapat") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            item {
                Text("İkon", fontWeight = FontWeight.Medium)
                val iconRows = ScheduleIcons.all.chunked(6)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    iconRows.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            row.forEach { iconName ->
                                FilterChip(
                                    selected = selectedIcon == iconName,
                                    onClick = { selectedIcon = iconName },
                                    leadingIcon = { Icon(ScheduleIcons.getIconOrDefault(iconName), null, Modifier.size(20.dp)) },
                                    label = { Text("") }
                                )
                            }
                        }
                    }
                }
            }

            // Name
            item {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text("Şablon adı") }, placeholder = { Text("Haftaiçi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }

            // Days
            item {
                Text("Tekrarlama günleri", fontWeight = FontWeight.Medium)
                dayNames.forEachIndexed { index, dayName ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = index in selectedDays, onCheckedChange = { c -> if (c) selectedDays.add(index) else selectedDays.remove(index) })
                        Text(dayName)
                    }
                }
            }

            // Save
            item {
                Button(
                    onClick = {
                        viewModel.createTemplate(name, selectedIcon, selectedDays.toList())
                        onDismiss()
                    },
                    enabled = name.isNotBlank() && selectedDays.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Oluştur") }
            }
        }
    }
}
