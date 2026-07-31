package com.clamit.ui.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clamit.data.model.BlockState

@Composable
fun TimeBlockCard(
    block: BlockState,
    onToggleSubtask: (String) -> Unit,
    onSetCompleted: () -> Unit,
    onSetNotCompleted: () -> Unit
) {
    val isCompleted = block.manualStatus == "completed"
    val isInProgress = block.autoStatus == "in_progress"

    // Material 3 Expressive Tonal Palette Colors
    val statusAccent = when {
        isCompleted -> Color(0xFF2E7D32)     // Mint / Green
        isInProgress -> Color(0xFFE65100)    // Expressive Amber / Orange
        else -> Color(0xFF0288D1)            // Expressive Soft Teal / Blue
    }

    val statusBadgeBg = when {
        isCompleted -> Color(0xFFE8F5E9)
        isInProgress -> Color(0xFFFFF3E0)
        else -> Color(0xFFE1F5FE)
    }

    val cardBg by animateColorAsState(
        targetValue = when {
            isCompleted -> Color(0xFFF1F8E9)
            isInProgress -> Color(0xFFFFF8E1)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        label = "cardBg"
    )

    val cardBorderColor = when {
        isCompleted -> Color(0xFFA5D6A7)
        isInProgress -> Color(0xFFFFCC80)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, cardBorderColor, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isInProgress) 3.dp else 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Status Bar Accent + Icon + Title + Time + Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Status Pillar Strip
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(statusAccent)
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Icon Tile Container
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = statusBadgeBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = block.icon.ifBlank { "⏱️" },
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name & Time Range
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    val durationText = when {
                        block.mode == "start_end" && !block.endTime.isNullOrBlank() ->
                            "${block.startTime} - ${block.endTime}"
                        block.durationMin != null ->
                            "${block.startTime} • ${block.durationMin} dakika"
                        else -> block.startTime
                    }

                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Expressive Pill Badge
                Surface(
                    shape = CircleShape,
                    color = statusBadgeBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusAccent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = when {
                            isCompleted -> "✅ Tamam"
                            isInProgress -> "⚡ Devam"
                            else -> "⏳ Bekliyor"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = statusAccent,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Subtasks Section (if any)
            if (block.subtaskStates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        block.subtaskStates.forEach { subtask ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleSubtask(subtask.subtaskId) }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                Checkbox(
                                    checked = subtask.done,
                                    onCheckedChange = { onToggleSubtask(subtask.subtaskId) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = statusAccent,
                                        uncheckedColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = subtask.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textDecoration = if (subtask.done) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (subtask.done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (subtask.done) FontWeight.Normal else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isCompleted) {
                    FilledTonalButton(
                        onClick = onSetCompleted,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("✓ Tamamlandı", style = MaterialTheme.typography.labelMedium)
                    }
                } else {
                    OutlinedButton(
                        onClick = onSetNotCompleted,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("↩ Geri al", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
