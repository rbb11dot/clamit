package com.clamit.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, mode: String, startTime: String, endTime: String?, durationMin: Int?, subtasks: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("☕") }
    var mode by remember { mutableStateOf("start_end") }
    var startHour by remember { mutableStateOf("07") }
    var startMin by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("07") }
    var endMin by remember { mutableStateOf("30") }
    var duration by remember { mutableStateOf("30") }
    var subtasks by remember { mutableStateOf(listOf("")) }

    val icons = listOf("☕", "💼", "📚", "🏋️", "🧘", "🍽️", "🛌", "🚌", "💻", "🎯", "📝", "🎵", "🌅", "🛁", "🐕", "📞")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zaman Bloğu Oluştur", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Icon picker
                item {
                    Text("İkon", style = MaterialTheme.typography.labelLarge)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        icons.take(8).forEach { emoji ->
                            FilterChip(
                                selected = icon == emoji,
                                onClick = { icon = emoji },
                                label = { Text(emoji) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        icons.drop(8).forEach { emoji ->
                            FilterChip(
                                selected = icon == emoji,
                                onClick = { icon = emoji },
                                label = { Text(emoji) }
                            )
                        }
                    }
                }

                // Name
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Blok adı") },
                        placeholder = { Text("Sabah rutini") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Mode selector
                item {
                    Text("Zaman modu", style = MaterialTheme.typography.labelLarge)
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

                // Time inputs
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startHour,
                            onValueChange = { if (it.length <= 2) startHour = it },
                            label = { Text("Saat") },
                            placeholder = { Text("07") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Text(":", modifier = Modifier.padding(top = 16.dp))
                        OutlinedTextField(
                            value = startMin,
                            onValueChange = { if (it.length <= 2) startMin = it },
                            label = { Text("Dakika") },
                            placeholder = { Text("00") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (mode == "start_end") {
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = endHour,
                                onValueChange = { if (it.length <= 2) endHour = it },
                                label = { Text("Bitiş saati") },
                                placeholder = { Text("07") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Text(":", modifier = Modifier.padding(top = 16.dp))
                            OutlinedTextField(
                                value = endMin,
                                onValueChange = { if (it.length <= 2) endMin = it },
                                label = { Text("Bitiş dk") },
                                placeholder = { Text("30") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    item {
                        OutlinedTextField(
                            value = duration,
                            onValueChange = { if (it.length <= 3) duration = it },
                            label = { Text("Süre (dakika)") },
                            placeholder = { Text("30") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Subtasks
                item {
                    Text(
                        "Subtask'ler",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                items(subtasks.indices.toList(), key = { it }) { index ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Drag handle hint
                        Text("⠿", modifier = Modifier.padding(end = 8.dp))
                        OutlinedTextField(
                            value = subtasks[index],
                            onValueChange = { newVal ->
                                subtasks = subtasks.toMutableList().also { it[index] = newVal }
                            },
                            placeholder = { Text("Subtask ${index + 1}") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        if (subtasks.size > 1) {
                            IconButton(onClick = {
                                subtasks = subtasks.toMutableList().also { it.removeAt(index) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Sil")
                            }
                        }
                    }
                }

                item {
                    TextButton(onClick = {
                        subtasks = subtasks + ""
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("Subtask ekle")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val startTime = "${startHour.padStart(2, '0')}:${startMin.padStart(2, '0')}"
                    val endTimeVal = if (mode == "start_end") "${endHour.padStart(2, '0')}:${endMin.padStart(2, '0')}" else null
                    val durationVal = if (mode == "start_duration") duration.toIntOrNull() else null
                    val validSubtasks = subtasks.filter { it.isNotBlank() }
                    onSave(name, icon, mode, startTime, endTimeVal, durationVal, validSubtasks)
                    onDismiss()
                },
                enabled = name.isNotBlank()
            ) { Text("Oluştur") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
