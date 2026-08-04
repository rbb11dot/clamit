package com.clamit.ui.schedule

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.clamit.data.model.BlockState
import com.clamit.data.model.Subtask
import com.clamit.data.model.TimeBlock
import com.clamit.ui.theme.ClamitStatusColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ScheduleHomePage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    onMenuClick: () -> Unit,
    onDatePicker: () -> Unit,
    onTemplatePicker: () -> Unit,
    onEditBlock: (TimeBlock) -> Unit,
    onAddBlock: () -> Unit
) {
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("tr"))
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("tr"))

    val activeTemplateName = uiState.entry?.templateName
        ?: if (uiState.entry?.isSpecial == true) "Özel Gün" else "Şablonsuz"

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
                        "clamit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.3).sp
                    )
                },
                actions = {
                    // Template chip: tappable label showing the active template name.
                    // Shape-morphing FilterChip; selected state stays off (tapping opens
                    // the picker instead of toggling).
                    FilterChip(
                        selected = false,
                        onClick = onTemplatePicker,
                        label = {
                            Text(
                                activeTemplateName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        shapes = FilterChipDefaults.shapes(),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Schedule,
                                contentDescription = "Şablon seç",
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        modifier = Modifier.height(32.dp)
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
                onClick = onAddBlock
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
            // ===== Hero date band: prev — month/year + big day numeral + weekday — next =====
            // The numeral is the typographic anchor of the rail (DESIGN.md THESIS);
            // both arrows are always visible (DESIGN.md rule 4). Today gets an amber dot.
            val isToday = uiState.currentDate == LocalDate.now()
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    // Prev day
                    IconButton(
                        onClick = viewModel::goToPreviousDay,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Önceki gün",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Date core
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onDatePicker)
                            .padding(horizontal = 4.dp)
                    ) {
                        Text(
                            text = uiState.currentDate.format(monthFormatter).uppercase(Locale("tr")),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.6.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = uiState.currentDate.dayOfMonth.toString().padStart(2, '0'),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-1.5).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (isToday) {
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .size(9.dp)
                                        .background(ClamitStatusColors.SignalAmber, CircleShape)
                                )
                            }
                        }
                        Text(
                            text = uiState.currentDate.format(dayFormatter),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Next day
                    IconButton(
                        onClick = viewModel::goToNextDay,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "Sonraki gün",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                // Day changes crossfade so the rail never flashes blank between loads.
                AnimatedContent(
                    targetState = uiState.currentDate,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(280)) togetherWith fadeOut(animationSpec = tween(220))
                    },
                    label = "dayChange"
                ) {
                    Box(Modifier.fillMaxSize()) {
                        when {
                            uiState.isLoading -> {
                                LoadingIndicator(
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
                                        onClick = viewModel::load
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Tekrar Dene")
                                    }
                                }
                            }

                            else -> {
                                val entry = uiState.entry
                                if (entry == null || entry.blocks.isEmpty()) {
                                    EmptyDayState(onAddBlock = onAddBlock)
                                } else {
                                    LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
                                    ) {
                                        items(entry.blocks, key = { it.timeBlockId }) { block ->
                                            TimeBlockCard(
                                                block = block,
                                                onToggleSubtask = { viewModel.toggleSubtask(block.timeBlockId, it) },
                                                onSetCompleted = { viewModel.setManualStatus(block.timeBlockId, "completed") },
                                                onSetNotCompleted = { viewModel.setManualStatus(block.timeBlockId, "not_completed") },
                                                onRemoveFromDay = if (entry.isSpecial) {
                                                    { viewModel.removeSpecialBlockFromDay(block.timeBlockId) }
                                                } else {
                                                    null
                                                },
                                                onEdit = { onEditBlock(block.toTimeBlock()) }
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
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.size(76.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
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

/** Maps a day's block state to an editable TimeBlock (used by the day editor). */
private fun BlockState.toTimeBlock(): TimeBlock = TimeBlock(
    id = timeBlockId,
    name = name,
    icon = icon,
    mode = mode,
    startTime = startTime,
    endTime = endTime,
    durationMin = durationMin,
    blockOrder = blockOrder,
    subtasks = subtaskStates.mapIndexed { index, s ->
        Subtask(id = s.subtaskId, timeBlockId = timeBlockId, name = s.name, subtaskOrder = index)
    }
)
