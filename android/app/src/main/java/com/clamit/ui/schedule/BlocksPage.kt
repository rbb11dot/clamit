package com.clamit.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.clamit.data.model.TimeBlock
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocksPage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onMenuClick: () -> Unit,
    onNewBlock: () -> Unit,
    onEditBlock: (TimeBlock) -> Unit
) {
    // Library blocks, each annotated with the templates it belongs to (a block
    // can be attached to several templates — many-to-many).
    val libraryBlocksWithTemplates = remember(uiState.libraryBlocks, uiState.templates) {
        uiState.libraryBlocks.map { block ->
            val memberTemplates = uiState.templates.filter { t -> t.blocks.any { it.id == block.id } }
            block to memberTemplates.map { it.name }
        }
    }

    var blockToDelete by remember { mutableStateOf<TimeBlock?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
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
                onClick = onNewBlock
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
            ErrorBanner(error = uiState.error, onDismiss = viewModel::clearError)

			if (libraryBlocksWithTemplates.isEmpty()) {
                // Expressive Empty State
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
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
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
                ) {
                    items(libraryBlocksWithTemplates, key = { (block, _) -> block.id }) { (block, templateNames) ->
                        TimeBlockListItem(
                            block = block,
                            templateNames = templateNames,
                            onEdit = { onEditBlock(block) },
                            onDelete = { blockToDelete = block }
                        )
                    }
                }
            }
        }
    }

    blockToDelete?.let { block ->
        AlertDialog(
            onDismissRequest = { blockToDelete = null },
            title = { Text("Bloğu sil", fontWeight = FontWeight.Bold) },
			text = { Text("\"${block.name}\" kütüphaneden silinecek ve şablonlardaki tüm bağlantıları kaldırılacak. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBlock(block.id)
                        blockToDelete = null
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { blockToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun TimeBlockListItem(
    block: TimeBlock,
    templateNames: List<String>,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon Box
                Surface(
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

                // Template membership badges (a block can belong to several templates)
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = templateNames.joinToString(", ").ifEmpty { "Şablonsuz" },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Edit Button
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Düzenle",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

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

/** Draft row with a stable id so LazyColumn keys keep focus/cursor on the row,
 *  not the position, after a reorder. serverId is the existing subtask's id
 *  when editing, so the backend can keep toggle history on renames. */
private data class SubtaskDraft(val id: Long, val text: String, val serverId: String? = null)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockEditorPage(
    onDismiss: () -> Unit,
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    addToCurrentDay: Boolean = false,
    blockToEdit: TimeBlock? = null,
    editDayBlock: Boolean = false
) {
    val isEdit = blockToEdit != null
    val editingTitle = if (editDayBlock) "Gün Bloğunu Düzenle" else "Zaman Bloğunu Düzenle"

    var name by remember(blockToEdit) { mutableStateOf(blockToEdit?.name ?: "") }
    var blockIcon by remember(blockToEdit) { mutableStateOf(blockToEdit?.icon ?: "alarm") }
    var mode by remember(blockToEdit) { mutableStateOf(blockToEdit?.mode ?: "start_end") }
    var startHour by remember(blockToEdit) { mutableStateOf(blockToEdit?.startTime?.substringBefore(":") ?: "07") }
    var startMin by remember(blockToEdit) { mutableStateOf(blockToEdit?.startTime?.substringAfter(":") ?: "00") }
    var endHour by remember(blockToEdit) {
        mutableStateOf(blockToEdit?.endTime?.substringBefore(":") ?: "07")
    }
    var endMin by remember(blockToEdit) {
        mutableStateOf(blockToEdit?.endTime?.substringAfter(":") ?: "30")
    }
    var duration by remember(blockToEdit) { mutableStateOf(blockToEdit?.durationMin?.toString() ?: "30") }
    var subtasks by remember(blockToEdit) {
        mutableStateOf(
            blockToEdit?.subtasks?.mapIndexed { index, s ->
                SubtaskDraft(id = index.toLong(), text = s.name, serverId = s.id)
            }?.ifEmpty { listOf(SubtaskDraft(0, "")) } ?: listOf(SubtaskDraft(0, ""))
        )
    }
    var nextSubtaskId by remember(blockToEdit) { mutableLongStateOf((blockToEdit?.subtasks?.size ?: 0).toLong() + 1) }
    var saving by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Time field validation (Saat 0-23, Dakika 0-59, Süre > 0)
    val startH = startHour.toIntOrNull()
    val startM = startMin.toIntOrNull()
    val endH = endHour.toIntOrNull()
    val endM = endMin.toIntOrNull()
    val dur = duration.toIntOrNull()
    val timeValid = startH != null && startH in 0..23 &&
        startM != null && startM in 0..59 &&
        (mode != "start_end" || (endH != null && endH in 0..23 && endM != null && endM in 0..59)) &&
        (mode != "start_duration" || (dur != null && dur > 0))

    fun moveSubtask(fromIndex: Int, toIndex: Int) {
        if (toIndex in subtasks.indices && fromIndex in subtasks.indices) {
            val list = subtasks.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            subtasks = list
        }
    }

    fun save() {
        if (saving) return
        if (!timeValid) {
            validationError = "Geçersiz saat. Saat 0-23, dakika 0-59 arasında olmalı."
            return
        }
        validationError = null
        saving = true
        scope.launch {
            val st = "${startHour.padStart(2, '0')}:${startMin.padStart(2, '0')}"
            val et = if (mode == "start_end") "${endHour.padStart(2, '0')}:${endMin.padStart(2, '0')}" else null
            val durMin = if (mode == "start_duration") dur else null
            val drafts = subtasks.filter { it.text.isNotBlank() }

            val ok = when {
                isEdit && editDayBlock -> viewModel.updateEntryBlockSuspended(
                    blockToEdit!!.id, name.trim(), blockIcon, mode, st, et, durMin,
                    drafts.map { it.serverId to it.text.trim() }
                )
                isEdit -> viewModel.updateBlockSuspended(
                    blockToEdit!!.id, name.trim(), blockIcon, mode, st, et, durMin,
                    drafts.map { it.serverId to it.text.trim() }
                )
                else -> {
                    // Create a standalone library block first, then optionally land it
                    // on the current day (home FAB flow).
                    val blockId = viewModel.createBlockSuspended(
                        name.trim(), blockIcon, mode, st, et, durMin, drafts.map { it.text.trim() }
                    )
                    if (blockId != null && addToCurrentDay) {
                        viewModel.addSpecialBlockToCurrentDaySuspended(blockId)
                    }
                    blockId != null
                }
            }

            saving = false
            if (ok) onDismiss()
            // On failure uiState.error is set by the ViewModel; the editor stays open for retry.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEdit) editingTitle else "Zaman Bloğu Oluştur",
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
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Library hint: blocks are shared across templates — attach this block to
            // a template from the template's edit page.
            if (!isEdit && !addToCurrentDay) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Blok kütüphaneye eklenir. Bir şablona eklemek için şablonu düzenleyip + butonunu kullanın.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // Day edit hint: editing a template-linked day detaches it.
            if (editDayBlock && uiState.entry?.templateId != null) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Bu düzenleme günü şablondan ayırır: gün özel güne dönüşür ve şablon etkilenmez.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                    }
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
                    )
                    FilterChip(
                        selected = mode == "start_duration",
                        onClick = { mode = "start_duration" },
                        label = { Text("Başlangıç + Süre") },
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
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = startMin,
                        onValueChange = { if (it.length <= 2) startMin = it },
                        label = { Text("Dakika") },
                        singleLine = true,
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
                            modifier = Modifier.weight(1f)
                        )
                        Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = endMin,
                            onValueChange = { if (it.length <= 2) endMin = it },
                            label = { Text("Dakika") },
                            singleLine = true,
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Subtasks & Reordering
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
                        "${subtasks.count { it.text.isNotBlank() }} adet",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            itemsIndexed(subtasks, key = { _, draft -> draft.id }) { index, subtask ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        // Order Pill Badge
                        Surface(
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
                            value = subtask.text,
                            onValueChange = { n ->
                                subtasks = subtasks.map { if (it.id == subtask.id) it.copy(text = n) else it }
                            },
                            placeholder = { Text("Subtask ${index + 1}") },
                            singleLine = true,
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
                                    subtasks = subtasks.filterNot { it.id == subtask.id }
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
                    onClick = {
                        subtasks = subtasks + SubtaskDraft(nextSubtaskId, "")
                        nextSubtaskId++
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Subtask Ekle", fontWeight = FontWeight.SemiBold)
                }
            }

            // Validation feedback
            validationError?.let { msg ->
                item {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Save Action
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = ::save,
                    enabled = name.isNotBlank() && timeValid && !saving,
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Kaydediliyor…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Kaydet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Shared dismissible error banner used on list pages. */
@Composable
fun ErrorBanner(error: String?, onDismiss: () -> Unit) {
    if (error == null) return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
        ) {
            Icon(Icons.Default.ErrorOutline, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Kapat", modifier = Modifier.size(18.dp))
            }
        }
    }
}
