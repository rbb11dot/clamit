package com.clamit.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clamit.data.model.TimeBlock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocksPage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onMenuClick: () -> Unit,
    onNewBlock: () -> Unit
) {
    // Extract all blocks from templates
    val allBlocksWithTemplates = remember(uiState.templates) {
        uiState.templates.flatMap { template ->
            template.blocks.map { block -> template to block }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü")
                    }
                },
                title = {
                    Text(
                        "Zaman Blokları",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewBlock,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni blok")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Yeni Blok", fontWeight = FontWeight.Bold)
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
        ) {
            if (allBlocksWithTemplates.isEmpty()) {
                // Expressive Empty State
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Henüz zaman bloğu yok.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Aşağıdaki + butonuna basarak yeni bir zaman bloğu oluşturun.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
                ) {
                    items(allBlocksWithTemplates, key = { (_, block) -> block.id }) { (template, block) ->
                        TimeBlockListItem(
                            block = block,
                            templateName = template.name,
                            onDelete = { viewModel.deleteBlock(block.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeBlockListItem(
    block: TimeBlock,
    templateName: String,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon Box
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ScheduleIcons.getIconOrDefault(block.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Mode/Time
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val timeText = if (block.mode == "start_end" && !block.endTime.isNullOrBlank()) {
                        "${block.startTime} - ${block.endTime}"
                    } else if (block.durationMin != null) {
                        "${block.startTime} • ${block.durationMin} dakika"
                    } else {
                        block.startTime
                    }

                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Template Name Badge
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                ) {
                    Text(
                        text = templateName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Delete Button
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Subtask summary pills if present
            if (block.subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.FormatListNumbered,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${block.subtasks.size} subtask: ${block.subtasks.joinToString(", ") { it.name }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ---- Full Screen Block Editor ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorPage(
    onDismiss: () -> Unit,
    viewModel: ScheduleViewModel,
    targetTemplateId: String? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val templates = uiState.templates

    // Auto-resolve templateId under the hood (Point 3: NO template selection UI shown to user)
    val entryTemplateId = uiState.entry?.templateId
    val resolvedTemplateId = remember(targetTemplateId, entryTemplateId, templates) {
        when {
            !targetTemplateId.isNullOrBlank() -> targetTemplateId
            entryTemplateId != null -> entryTemplateId
            templates.isNotEmpty() -> templates.first().id
            else -> ""
        }
    }

    var name by remember { mutableStateOf("") }
    var blockIcon by remember { mutableStateOf("alarm") }
    var mode by remember { mutableStateOf("start_end") }
    var startHour by remember { mutableStateOf("07") }
    var startMin by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("07") }
    var endMin by remember { mutableStateOf("30") }
    var duration by remember { mutableStateOf("30") }
    var subtasks by remember { mutableStateOf(listOf("")) }

    // Helper for reordering subtasks (Point 4)
    fun moveSubtask(fromIndex: Int, toIndex: Int) {
        if (toIndex in subtasks.indices && fromIndex in subtasks.indices) {
            val list = subtasks.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            subtasks = list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                title = {
                    Text(
                        "Zaman Bloğu Oluştur",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon + Name
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconPickerButton(currentIcon = blockIcon) { blockIcon = it }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Blok adı") },
                        placeholder = { Text("Sabah rutini") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Mode Selection
            item {
                Text(
                    "Zaman Modu",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == "start_end",
                        onClick = { mode = "start_end" },
                        label = { Text("Başlangıç + Bitiş") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    FilterChip(
                        selected = mode == "start_duration",
                        onClick = { mode = "start_duration" },
                        label = { Text("Başlangıç + Süre") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Start Time Inputs
            item {
                Text(
                    "Başlangıç Saati",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { if (it.length <= 2) startHour = it },
                        label = { Text("Saat") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = startMin,
                        onValueChange = { if (it.length <= 2) startMin = it },
                        label = { Text("Dakika") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (mode == "start_end") {
                item {
                    Text(
                        "Bitiş Saati",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = endHour,
                            onValueChange = { if (it.length <= 2) endHour = it },
                            label = { Text("Saat") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = endMin,
                            onValueChange = { if (it.length <= 2) endMin = it },
                            label = { Text("Dakika") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                item {
                    Text(
                        "Süre",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { if (it.length <= 3) duration = it },
                        label = { Text("Süre (dakika)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Subtasks & Reordering (Point 4)
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Subtask'ler ve Sıralama",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${subtasks.filter { it.isNotBlank() }.size} adet",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            itemsIndexed(subtasks, key = { index, _ -> index }) { index, subtaskText ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Order Pill Badge
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // TextField
                        OutlinedTextField(
                            value = subtaskText,
                            onValueChange = { n ->
                                val list = subtasks.toMutableList()
                                list[index] = n
                                subtasks = list
                            },
                            placeholder = { Text("Subtask ${index + 1}") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        )

                        // Reorder Up Button
                        IconButton(
                            onClick = { moveSubtask(index, index - 1) },
                            enabled = index > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowUp,
                                contentDescription = "Yukarı taşı",
                                tint = if (index > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }

                        // Reorder Down Button
                        IconButton(
                            onClick = { moveSubtask(index, index + 1) },
                            enabled = index < subtasks.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = "Aşağı taşı",
                                tint = if (index < subtasks.size - 1) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                        }

                        // Remove Subtask
                        if (subtasks.size > 1) {
                            IconButton(
                                onClick = {
                                    val list = subtasks.toMutableList()
                                    list.removeAt(index)
                                    subtasks = list
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Sil",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = { subtasks = subtasks + "" },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Subtask Ekle", fontWeight = FontWeight.SemiBold)
                }
            }

            // Save Action
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        val st = "${startHour.padStart(2, '0')}:${startMin.padStart(2, '0')}"
                        val et = if (mode == "start_end") "${endHour.padStart(2, '0')}:${endMin.padStart(2, '0')}" else null
                        val dur = if (mode == "start_duration") duration.toIntOrNull() else null

                        // If no template exists yet, create a default template first
                        val targetId = if (resolvedTemplateId.isNotBlank()) {
                            resolvedTemplateId
                        } else {
                            viewModel.createTemplate("Genel Şablon", "calendar_today", listOf(0, 1, 2, 3, 4, 5, 6))
                            uiState.templates.firstOrNull()?.id ?: ""
                        }

                        viewModel.createBlock(
                            targetId,
                            name,
                            blockIcon,
                            mode,
                            st,
                            et,
                            dur,
                            subtasks.filter { it.isNotBlank() }
                        )
                        onDismiss()
                    },
                    enabled = name.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Kaydet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
