package com.clamit.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.clamit.data.model.DayTemplate
import com.clamit.data.model.TimeBlock
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesPage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onMenuClick: () -> Unit,
    onNewTemplate: () -> Unit
) {
    var editingTemplate by remember { mutableStateOf<DayTemplate?>(null) }
    var templateToDelete by remember { mutableStateOf<DayTemplate?>(null) }

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
                        "Gün Şablonları",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewTemplate
            ) {
                Icon(Icons.Default.Add, contentDescription = "Yeni şablon")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Yeni Şablon", fontWeight = FontWeight.Bold)
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
            if (uiState.templates.isEmpty()) {
                // Expressive Empty State
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.size(96.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.FormatListBulleted,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Henüz gün şablonu oluşturulmadı.",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tekrarlayan rutinlerinizi tanımlamak için yeni bir şablon ekleyin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp)
                ) {
                    items(uiState.templates, key = { it.id }) { template ->
                        TemplateCardItem(
                            template = template,
                            onEdit = { editingTemplate = template },
                            onDelete = { templateToDelete = template }
                        )
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

    templateToDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { templateToDelete = null },
            title = { Text("Şablonu sil", fontWeight = FontWeight.Bold) },
            text = { Text("\"${template.name}\" silinecek. Bu şablonu kullanan günler bağımsız güne dönüşür; işlem geri alınamaz.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteTemplate(template.id)
                        templateToDelete = null
                    }
                ) {
                    Text("Sil", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { templateToDelete = null }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

@Composable
private fun TemplateCardItem(
    template: DayTemplate,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dayAbbreviations = listOf("Pzr", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Icon Tile
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = ScheduleIcons.getIconOrDefault(template.icon),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Block Count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val blockCountText = if (template.blocks.isNotEmpty()) {
                        "${template.blocks.size} zaman bloğu"
                    } else {
                        "Blok henüz eklenmedi"
                    }

                    Text(
                        text = blockCountText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

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

            Spacer(modifier = Modifier.height(12.dp))

            // Days Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                dayAbbreviations.forEachIndexed { index, shortName ->
                    val isSelected = index in template.repeatDays
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = shortName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
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
    var showBlockPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    // Special-day adoption: after a successful save, days materialized as
    // special before this template existed are offered for conversion.
    var adoptableDates by remember { mutableStateOf<List<String>?>(null) }
    var showConflictDialog by remember { mutableStateOf(false) }
    var showConflictPicker by remember { mutableStateOf(false) }
    var savedTemplateId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Blocks picked for a not-yet-created template (create mode).
    var pendingBlocks by remember { mutableStateOf(listOf<TimeBlock>()) }

    // The template's current blocks, refreshed from uiState after each attach.
    val currentBlocks = remember(templateToEdit, uiState.templates) {
        uiState.templates.find { it.id == templateToEdit?.id }?.blocks ?: templateToEdit?.blocks ?: emptyList()
    }
    // Section rows: live membership when editing, selected-for-attach when creating.
    val sectionBlocks = if (templateToEdit != null) currentBlocks else pendingBlocks
    val availableBlocks = remember(sectionBlocks, uiState.libraryBlocks) {
        uiState.libraryBlocks.filter { lib -> sectionBlocks.none { it.id == lib.id } }
    }

    val dayNames = listOf("Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (templateToEdit != null) "Şablonu Düzenle" else "Gün Şablonu Oluştur",
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Icon + Name
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconPickerButton(currentIcon = templateIcon) { templateIcon = it }
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Şablon adı") },
                        placeholder = { Text("Haftaiçi") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Days Selection Chips
            item {
                Text(
                    "Tekrarlama Günleri",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        dayNames.forEachIndexed { index, dayName ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (index in selectedDays) selectedDays.remove(index)
                                        else selectedDays.add(index)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                Checkbox(
                                    checked = index in selectedDays,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedDays.add(index)
                                        else selectedDays.remove(index)
                                    }
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    dayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (index in selectedDays) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }

			// Block management (edit mode only — a new template has no id yet).
			item {
				Text(
					"Zaman Blokları",
					style = MaterialTheme.typography.titleSmall,
					fontWeight = FontWeight.Bold
				)
				Spacer(Modifier.height(8.dp))
				Card(
					colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
				) {
					Column(modifier = Modifier.padding(12.dp)) {
						if (sectionBlocks.isEmpty()) {
							Text(
								if (templateToEdit != null) "Bu şablonda henüz blok yok." else "Henüz blok seçilmedi.",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						} else {
							sectionBlocks.forEach { block ->
								Row(
									verticalAlignment = Alignment.CenterVertically,
									modifier = Modifier
											.fillMaxWidth()
											.padding(vertical = 2.dp)
								) {
									Icon(
										ScheduleIcons.getIconOrDefault(block.icon),
										contentDescription = null,
										tint = MaterialTheme.colorScheme.primary,
										modifier = Modifier.size(20.dp)
									)
									Spacer(Modifier.width(10.dp))
									Text(
										block.name,
										style = MaterialTheme.typography.bodyMedium,
										modifier = Modifier.weight(1f)
									)
									IconButton(
										onClick = {
											if (templateToEdit != null) {
												viewModel.removeTemplateBlock(templateToEdit.id, block.id)
											} else {
												pendingBlocks = pendingBlocks.filterNot { it.id == block.id }
											}
										},
										modifier = Modifier.size(32.dp)
									) {
										Icon(
											Icons.Default.Close,
											contentDescription = "Şablondan kaldır",
											tint = MaterialTheme.colorScheme.error,
											modifier = Modifier.size(18.dp)
										)
									}
								}
							}
						}
						Spacer(Modifier.height(4.dp))
						OutlinedButton(
							onClick = { showBlockPicker = true },
							modifier = Modifier.fillMaxWidth()
						) {
							Icon(Icons.Default.Add, contentDescription = null)
							Spacer(Modifier.width(6.dp))
							Text("Blok Ekle", fontWeight = FontWeight.SemiBold)
						}
					}
				}
			}

			// Save Action
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (!saving) {
                            saving = true
                            scope.launch {
                                val tid: String? = if (templateToEdit != null) {
                                    val ok = viewModel.updateTemplateSuspended(
                                        templateToEdit.id, name, templateIcon, selectedDays.toList()
                                    )
                                    if (ok) templateToEdit.id else null
                                } else {
                                    val newId = viewModel.createTemplateSuspended(name, templateIcon, selectedDays.toList())
                                    if (newId != null) {
                                        viewModel.attachBlocksSuspended(newId, pendingBlocks.map { it.id })
                                    }
                                    newId
                                }
                                saving = false
                                if (tid != null) {
                                    // Offer adoption when pre-existing special days match the
                                    // selected repeat days; otherwise close straight away.
                                    val dates = viewModel.fetchSpecialDaysSuspended(tid)
                                    if (dates.isNotEmpty()) {
                                        savedTemplateId = tid
                                        adoptableDates = dates
                                        showConflictDialog = true
                                    } else {
                                        onDismiss()
                                    }
                                }
                                // On failure the ViewModel surfaces the error; the editor stays open.
                            }
                        }
                    },
                    enabled = name.isNotBlank() && selectedDays.isNotEmpty() && !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        when {
                            saving -> "Kaydediliyor…"
                            templateToEdit != null -> "Kaydet"
                            else -> "Oluştur"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showBlockPicker) {
        AlertDialog(
            onDismissRequest = { showBlockPicker = false },
            title = { Text("Blok Ekle", fontWeight = FontWeight.Bold) },
            text = {
                if (availableBlocks.isEmpty()) {
                    Text("Kütüphanede eklenebilecek blok yok. Önce Zaman Blokları sayfasından blok oluşturun.")
                } else {
                    Column {
                        availableBlocks.forEach { block ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (templateToEdit != null) {
                                            viewModel.addTemplateBlock(templateToEdit.id, block.id)
                                        } else {
                                            pendingBlocks = pendingBlocks + block
                                        }
                                        showBlockPicker = false
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Icon(
                                    ScheduleIcons.getIconOrDefault(block.icon),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(block.name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockPicker = false }) { Text("Kapat") }
            }
        )
    }

    // Three-way choice for adoptable special days (shown after a successful save).
    // The dialog is a separate window: it must be dismissed before the picker
    // page opens, or it would float above the full-screen page.
    if (showConflictDialog) adoptableDates?.let { dates ->
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Özel günler bulundu", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Seçilen tekrarlanan günlerde özel gün olarak kaydedilmiş ${dates.size} gün var. " +
                        "Bu günlere şablon uygulansın mı? Uygulanan günlerin mevcut içeriği şablonla değiştirilir."
                )
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showConflictDialog = false
                            scope.launch {
                                savedTemplateId?.let { viewModel.applyTemplateToDatesSuspended(it, dates) }
                                onDismiss()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Evet", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = {
                            showConflictDialog = false
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Hayır", fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(
                        onClick = {
                            showConflictDialog = false
                            showConflictPicker = true
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("Uygulanacak günleri seç", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        )
    }

    // Full-page day picker: back returns to the editor, save applies and closes.
    if (showConflictPicker) adoptableDates?.let { dates ->
        ConflictDaysPage(
            dates = dates,
            onBack = { showConflictPicker = false },
            onApply = { chosen ->
                showConflictPicker = false
                scope.launch {
                    savedTemplateId?.let { viewModel.applyTemplateToDatesSuspended(it, chosen) }
                    onDismiss()
                }
            }
        )
    }
}

// ---- Full Screen Adoptable-Day Picker ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConflictDaysPage(
    dates: List<String>,
    onApply: (List<String>) -> Unit,
    onBack: () -> Unit
) {
    val selected = remember { mutableStateListOf<String>().apply { addAll(dates) } }
    val dayFormatter = remember {
        DateTimeFormatter.ofPattern("d MMMM yyyy, EEEE", Locale("tr"))
    }
    // Back always returns to the template editor; the editor stays open.
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Uygulanacak Günler",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                "Şablon uygulanacak günleri seç. İşaretlenen günlerin mevcut içeriği şablonla değiştirilir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(dates, key = { it }) { date ->
                    val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (date in selected) selected.remove(date) else selected.add(date)
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                    ) {
                        Checkbox(
                            checked = date in selected,
                            onCheckedChange = { checked ->
                                if (checked) selected.add(date) else selected.remove(date)
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            parsed?.format(dayFormatter) ?: date,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (date in selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { onApply(selected.toList()) },
                enabled = selected.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    "Kaydet (${selected.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
