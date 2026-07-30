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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesPage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onMenuClick: () -> Unit,
    onNewTemplate: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Menü") } },
                title = { Text("Gün Şablonları") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTemplate) { Icon(Icons.Default.Add, "Yeni şablon") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.templates, key = { it.id }) { t ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(ScheduleIcons.getIconOrDefault(t.icon), null, Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.titleMedium)
                            val days = t.repeatDays.joinToString(", ") { d ->
                                listOf("Pazar","Pzt","Sal","Çar","Per","Cum","Cmt").getOrElse(d){"?"} }
                            Text(days, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { }) { Icon(Icons.Default.Edit, "Düzenle") }
                    }
                }
            }
        }
    }
}

// ---- Full Screen Template Editor ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorPage(
    onDismiss: () -> Unit,
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState
) {
    var name by remember { mutableStateOf("") }
    var templateIcon by remember { mutableStateOf("format_list_bulleted") }
    val selectedDays = remember { mutableStateListOf<Int>() }
    var selectedBlockIds by remember { mutableStateOf(listOf<String>()) }
    val dayNames = listOf("Pazar","Pazartesi","Salı","Çarşamba","Perşembe","Cuma","Cumartesi")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gün Şablonu Oluştur") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Geri") } }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconPickerButton(currentIcon = templateIcon) { templateIcon = it }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it },
                        label = { Text("Şablon adı") }, placeholder = { Text("Haftaiçi") },
                        singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }

            // Days
            item {
                Text("Tekrarlama günleri", fontWeight = FontWeight.Medium)
                dayNames.forEachIndexed { index, dayName ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = index in selectedDays,
                            onCheckedChange = { c -> if(c) selectedDays.add(index) else selectedDays.remove(index) }
                        )
                        Text(dayName)
                    }
                }
            }

            // Blocks (from existing templates)
            item {
                Text("Zaman blokları", fontWeight = FontWeight.Medium)
                if (uiState.templates.isEmpty()) {
                    Text("Henüz blok yok. Önce bir şablona blok ekleyin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Save
            item {
                Button(
                    onClick = {
                        viewModel.createTemplate(name, templateIcon, selectedDays.toList())
                        onDismiss()
                    },
                    enabled = name.isNotBlank() && selectedDays.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Oluştur") }
            }
        }
    }
}
