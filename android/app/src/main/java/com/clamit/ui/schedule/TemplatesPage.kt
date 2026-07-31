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
import com.clamit.data.model.DayTemplate
import com.clamit.data.model.TimeBlock
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesPage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onMenuClick: () -> Unit,
    onNewTemplate: () -> Unit
) {
    var editingTemplate by remember { mutableStateOf<DayTemplate?>(null) }

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
                        IconButton(onClick = { editingTemplate = t }) { Icon(Icons.Default.Edit, "Düzenle") }
                    }
                }
            }
        }
    }

    if (editingTemplate != null) {
        TemplateEditorPage(
            onDismiss = { editingTemplate = null },
            viewModel = viewModel,
            uiState = uiState,
            templateToEdit = editingTemplate
        )
    }
}

// ---- Full Screen Template Editor ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorPage(
    onDismiss: () -> Unit,
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    templateToEdit: DayTemplate? = null
) {
    var name by remember(templateToEdit) { mutableStateOf(templateToEdit?.name ?: "") }
    var templateIcon by remember(templateToEdit) { mutableStateOf(templateToEdit?.icon ?: "format_list_bulleted") }
    val selectedDays = remember(templateToEdit) {
        mutableStateListOf<Int>().apply {
            templateToEdit?.repeatDays?.let { addAll(it) }
        }
    }
    val selectedBlockIds = remember(templateToEdit) {
        mutableStateListOf<String>().apply {
            templateToEdit?.blocks?.map { it.id }?.let { addAll(it) }
        }
    }
    val dayNames = listOf("Pazar","Pazartesi","Salı","Çarşamba","Perşembe","Cuma","Cumartesi")

    val existingBlocks = remember(uiState.templates, templateToEdit) {
        val blocksFromTemplates = uiState.templates.flatMap { it.blocks }
        val blocksFromEditing = templateToEdit?.blocks ?: emptyList()
        (blocksFromTemplates + blocksFromEditing).distinctBy { it.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (templateToEdit != null) "Şablonu Düzenle" else "Gün Şablonu Oluştur") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.ArrowBack, "Geri") } }
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
            }
            if (existingBlocks.isEmpty()) {
                item {
                    Text("Henüz blok yok. Önce bir şablona blok ekleyin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(existingBlocks, key = { it.id }) { block ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (block.id in selectedBlockIds) {
                                    selectedBlockIds.remove(block.id)
                                } else {
                                    selectedBlockIds.add(block.id)
                                }
                            }
                    ) {
                        Checkbox(
                            checked = block.id in selectedBlockIds,
                            onCheckedChange = { checked ->
                                if (checked) selectedBlockIds.add(block.id)
                                else selectedBlockIds.remove(block.id)
                            }
                        )
                        Icon(
                            ScheduleIcons.getIconOrDefault(block.icon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(block.name, style = MaterialTheme.typography.bodyMedium)
                            if (block.startTime.isNotBlank()) {
                                val timeText = if (block.endTime != null) "${block.startTime} - ${block.endTime}" else "${block.startTime} (${block.durationMin ?: 0} dk)"
                                Text(timeText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            // Save
            item {
                Button(
                    onClick = {
                        if (templateToEdit != null) {
                            viewModel.updateTemplate(templateToEdit.id, name, templateIcon, selectedDays.toList())
                        } else {
                            viewModel.createTemplate(name, templateIcon, selectedDays.toList())
                        }
                        onDismiss()
                    },
                    enabled = name.isNotBlank() && selectedDays.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (templateToEdit != null) "Kaydet" else "Oluştur") }
            }
        }
    }
}
