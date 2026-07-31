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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleHomePage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onMenuClick: () -> Unit,
    onDatePicker: () -> Unit,
    onTemplatePicker: () -> Unit,
    onAddBlock: () -> Unit
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr"))
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("tr"))

    val activeTemplateName = uiState.entry?.templateName
        ?: if (uiState.entry?.isSpecial == true) "Özel Gün" else "Şablonsuz"

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü")
                    }
                },
                title = {
                    Text(
                        "clamit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                },
                actions = {
                    // Template chip
                    FilterChip(
                        selected = true,
                        onClick = onTemplatePicker,
                        label = {
                            Text(
                                activeTemplateName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Şablon seç",
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            selectedLabelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = true,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = viewModel::goToToday) {
                        Icon(Icons.Default.Today, contentDescription = "Bugün")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddBlock,
                shape = RoundedCornerShape(18.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Blok ekle")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ===== Date band: prev — big day numeral + month/year + weekday — next =====
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp)
                ) {
                    // Prev day
                    IconButton(
                        onClick = viewModel::goToPreviousDay,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Önceki gün",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Date core
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onDatePicker)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.currentDate.dayOfMonth.toString().padStart(2, '0'),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-1).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = uiState.currentDate.format(monthFormatter),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = uiState.currentDate.format(dayFormatter),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Next day
                    IconButton(
                        onClick = viewModel::goToNextDay,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Sonraki gün",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // ===== Content =====
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                when {
                    uiState.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    uiState.error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.WifiOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Hata oluştu",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = uiState.error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = viewModel::load,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Tekrar Dene")
                            }
                        }
                    }

                    else -> {
                        val entry = uiState.entry
                        if (entry == null) {
                            Text("Yükleniyor...", Modifier.align(Alignment.Center))
                        } else if (entry.blocks.isEmpty()) {
                            EmptyDayState(onAddBlock = onAddBlock)
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(top = 10.dp, bottom = 96.dp)
                            ) {
                                items(entry.blocks, key = { it.timeBlockId }) { block ->
                                    TimeBlockCard(
                                        block = block,
                                        onToggleSubtask = { viewModel.toggleSubtask(block.timeBlockId, it) },
                                        onSetCompleted = { viewModel.setManualStatus(block.timeBlockId, "completed") },
                                        onSetNotCompleted = { viewModel.setManualStatus(block.timeBlockId, "not_completed") }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayState(onAddBlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Bu gün için zaman bloğu yok.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Bir şablon seçin ya da aşağıdaki + butonuyla blok ekleyin.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onAddBlock) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Blok Ekle", fontWeight = FontWeight.SemiBold)
        }
    }
}
