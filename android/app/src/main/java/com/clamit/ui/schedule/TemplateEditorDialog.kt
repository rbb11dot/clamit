package com.clamit.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorDialog(
    availableBlocks: List<com.clamit.data.model.TimeBlock>,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, repeatDays: List<Int>, blockIds: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("📋") }
    val selectedDays = remember { mutableStateListOf<Int>() }
    val selectedBlockIds = remember { mutableStateListOf<String>() }

    val dayNames = listOf("Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
    val icons = listOf("📋", "💼", "🏠", "🎓", "💪", "🧘", "🌅", "🎯")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gün Şablonu Oluştur", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
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
                        icons.forEach { emoji ->
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
                        label = { Text("Şablon adı") },
                        placeholder = { Text("Haftaiçi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Repeat days
                item {
                    Text("Tekrarlama günleri", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        dayNames.forEachIndexed { index, dayName ->
                            FilterChip(
                                selected = index in selectedDays,
                                onClick = {
                                    if (index in selectedDays) selectedDays.remove(index)
                                    else selectedDays.add(index)
                                },
                                label = { Text(dayName) }
                            )
                        }
                    }
                }

                // Available blocks
                if (availableBlocks.isNotEmpty()) {
                    item {
                        Text("Zaman blokları", style = MaterialTheme.typography.labelLarge)
                    }
                    items(availableBlocks) { block ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(
                                checked = block.id in selectedBlockIds,
                                onCheckedChange = { checked ->
                                    if (checked) selectedBlockIds.add(block.id)
                                    else selectedBlockIds.remove(block.id)
                                }
                            )
                            Text("${block.icon} ${block.name}", modifier = Modifier.weight(1f))
                            Text(
                                text = block.startTime,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    item {
                        Text(
                            "Henüz zaman bloğu oluşturulmamış. Önce blok oluşturun.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(name, icon, selectedDays.toList(), selectedBlockIds.toList())
                    onDismiss()
                },
                enabled = name.isNotBlank() && selectedDays.isNotEmpty()
            ) { Text("Oluştur") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("İptal") }
        }
    )
}
