package com.clamit.ui.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import com.clamit.ui.theme.ClamitColors

@Composable
fun TimeBlockCard(
    block: BlockState,
    onToggleSubtask: (String) -> Unit,
    onSetCompleted: () -> Unit,
    onSetNotCompleted: () -> Unit
) {
    val isCompleted = block.manualStatus == "completed"
    val isInProgress = block.autoStatus == "in_progress"

    // Status node language: filled amber = in progress, filled teal = completed, hollow = pending
    val nodeColor = when {
        isCompleted -> ClamitColors.CompletedTeal
        isInProgress -> ClamitColors.SignalAmber
        else -> ClamitColors.PendingTeal
    }
    val containerColor = when {
        isCompleted -> ClamitColors.CompletedBg
        isInProgress -> ClamitColors.SignalAmberBg
        else -> ClamitColors.PendingBg
    }
    val labelText = when {
        isCompleted -> "Tamamlandı"
        isInProgress -> "Devam ediyor"
        else -> "Bekliyor"
    }

    val animatedBg by animateColorAsState(
        targetValue = if (isInProgress || isCompleted) containerColor
        else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "stopBg"
    )
    val animatedElevation by animateDpAsState(
        targetValue = if (isInProgress) 3.dp else 0.dp,
        label = "stopElevation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(
                width = 1.dp,
                color = if (isInProgress) nodeColor.copy(alpha = 0.55f)
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        elevation = CardDefaults.cardElevation(defaultElevation = animatedElevation)
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            // ===== Rail: time + spine + node =====
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)
            ) {
                Text(
                    text = block.startTime,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                    color = nodeColor
                )
                Spacer(Modifier.height(6.dp))
                // Node on the spine
                Box(
                    modifier = Modifier
                        .size(if (isInProgress) 14.dp else 12.dp)
                        .border(
                            width = if (isCompleted || isInProgress) 0.dp else 2.dp,
                            color = nodeColor,
                            shape = CircleShape
                        )
                        .background(if (isCompleted || isInProgress) nodeColor else Color.Transparent, CircleShape)
                )
                // Spine continues below the node
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(nodeColor.copy(alpha = if (isInProgress) 0.9f else 0.35f))
                )
            }

            Spacer(Modifier.width(14.dp))

            // ===== Stop content =====
            Column(modifier = Modifier.weight(1f)) {
                // Header: icon tile + name + status label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = nodeColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ScheduleIcons.getIconOrDefault(block.icon),
                                contentDescription = null,
                                tint = nodeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = block.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = durationText(block),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    // Status: dot + label (no emoji)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(nodeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(nodeColor, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = labelText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = nodeColor
                        )
                    }
                }

                // Subtasks
                if (block.subtaskStates.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Column {
                        block.subtaskStates.forEach { subtask ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onToggleSubtask(subtask.subtaskId) }
                                    .padding(vertical = 2.dp, horizontal = 2.dp)
                            ) {
                                Checkbox(
                                    checked = subtask.done,
                                    onCheckedChange = { onToggleSubtask(subtask.subtaskId) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = nodeColor,
                                        uncheckedColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = subtask.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (subtask.done) FontWeight.Normal else FontWeight.Medium,
                                    textDecoration = if (subtask.done) TextDecoration.LineThrough else TextDecoration.None,
                                    color = if (subtask.done) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Action row
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (!isCompleted) {
                        FilledTonalButton(
                            onClick = onSetCompleted,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tamamlandı", style = MaterialTheme.typography.labelMedium)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onSetNotCompleted,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Geri al", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun durationText(block: BlockState): String = when {
    block.mode == "start_end" && !block.endTime.isNullOrBlank() ->
        "${block.startTime} - ${block.endTime}"
    block.durationMin != null ->
        "${block.startTime} • ${block.durationMin} dk"
    else -> block.startTime
}
