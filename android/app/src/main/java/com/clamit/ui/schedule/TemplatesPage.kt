package com.clamit.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var templateToDelete by remember { mutableStateOf<DayTemplate?>(null) }

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
                        "Gün Şablonları",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewTemplate,
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                elevation = FloatingActionButtonDefaults.elevation(6.dp)
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
                        shape = RoundedCornerShape(24.dp),
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
                // Icon Tile
                Surface(
                    shape = RoundedCornerShape(14.dp),
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
                        shape = CircleShape,
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

    // The template's current blocks, refreshed from uiState after each attach.
    val currentBlocks = remember(templateToEdit, uiState.templates) {
        uiState.templates.find { it.id == templateToEdit?.id }?.blocks ?: templateToEdit?.blocks ?: emptyList()
    }
    val availableBlocks = remember(currentBlocks, uiState.libraryBlocks) {
        uiState.libraryBlocks.filter { lib -> currentBlocks.none { it.id == lib.id } }
    }

    val dayNames = listOf("Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi")

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
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
                        shape = RoundedCornerShape(14.dp),
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
                    shape = RoundedCornerShape(16.dp),
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
			if (templateToEdit != null) {
				item {
					Text(
						"Zaman Blokları",
						style = MaterialTheme.typography.titleSmall,
						fontWeight = FontWeight.Bold
					)
					Spacer(Modifier.height(8.dp))
					Card(
						shape = RoundedCornerShape(16.dp),
						colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
					) {
						Column(modifier = Modifier.padding(12.dp)) {
							if (currentBlocks.isEmpty()) {
								Text(
									"Bu şablonda henüz blok yok.",
									style = MaterialTheme.typography.bodySmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
							} else {
								currentBlocks.forEach { block ->
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
											onClick = { viewModel.removeTemplateBlock(templateToEdit.id, block.id) },
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
								shape = RoundedCornerShape(12.dp),
								modifier = Modifier.fillMaxWidth()
							) {
								Icon(Icons.Default.Add, contentDescription = null)
								Spacer(Modifier.width(6.dp))
								Text("Blok Ekle", fontWeight = FontWeight.SemiBold)
							}
						}
					}
				}
			}

			// Save Action
            item {
                Spacer(Modifier.height(8.dp))
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        if (templateToEdit != null) "Kaydet" else "Oluştur",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (showBlockPicker && templateToEdit != null) {
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
                                        viewModel.addTemplateBlock(templateToEdit.id, block.id)
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
}
