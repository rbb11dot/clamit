package com.clamit.ui.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clamit.data.model.BlockState
import com.clamit.ui.theme.ClamitStatusColors

@Composable
fun TimeBlockCard(
    block: BlockState,
    onToggleSubtask: (String) -> Unit,
    onSetCompleted: () -> Unit,
    onSetNotCompleted: () -> Unit,
    onRemoveFromDay: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null
) {
    var confirmRemove by remember { mutableStateOf(false) }
    val isCompleted = block.manualStatus == "completed"
    val isInProgress = block.autoStatus == "in_progress"

    // Status node language: filled amber = in progress, filled teal = completed, hollow = pending
    val nodeColor = when {
        isCompleted -> ClamitStatusColors.CompletedTeal
        isInProgress -> ClamitStatusColors.SignalAmber
        else -> ClamitStatusColors.PendingTeal
    }
    val containerColor = when {
        isCompleted -> ClamitStatusColors.CompletedBg
        isInProgress -> ClamitStatusColors.SignalAmberBg
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }
    val labelText = when {
        isCompleted -> "Tamamlandı"
        isInProgress -> "Devam ediyor"
        else -> "Bekliyor"
    }

    // DESIGN.md rule 7: one card language — hairline outlineVariant border over
    // surfaceContainerLow; the in-progress stop is the only elevated surface.
    val animatedBg by animateColorAsState(targetValue = containerColor, label = "stopBg")
    val animatedBorder by animateColorAsState(
        targetValue = if (isInProgress) ClamitStatusColors.SignalAmber.copy(alpha = 0.65f)
        else MaterialTheme.colorScheme.outlineVariant,
        label = "stopBorder"
    )

    // Motion: the active node breathes so the "now" stop reads at a glance.
    val pulse by rememberInfiniteTransition(label = "stopPulse").animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "nodePulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = animatedBg),
        border = BorderStroke(1.dp, animatedBorder),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isInProgress) 2.dp else 0.dp
        )
    ) {
        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
            // ===== Rail: continuous spine + time + node =====
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(44.dp)
            ) {
                // spine above the stop, reaching the card edge
                Box(Modifier.width(2.dp).height(14.dp).background(spineColor(isInProgress, nodeColor)))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = block.startTime,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.sp,
                    color = nodeColor,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    softWrap = true
                )
                Spacer(Modifier.height(4.dp))
                // spine into the node
                Box(Modifier.width(2.dp).height(8.dp).background(spineColor(isInProgress, nodeColor)))
                // node on the spine
                if (isInProgress) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(20.dp)) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(ClamitStatusColors.SignalAmber.copy(alpha = 0.22f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                                .background(nodeColor, CircleShape)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(if (isCompleted) 12.dp else 10.dp)
                            .border(
                                width = if (isCompleted) 0.dp else 2.dp,
                                color = nodeColor,
                                shape = CircleShape
                            )
                            .background(if (isCompleted) nodeColor else Color.Transparent, CircleShape)
                    )
                }
                // spine continues below the node to the card edge
                Box(Modifier.width(2.dp).weight(1f).background(spineColor(isInProgress, nodeColor)))
            }

            Spacer(Modifier.width(14.dp))

            // ===== Stop content =====
            Column(modifier = Modifier.weight(1f)) {
                // Header: icon tile + name + status label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = nodeColor.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = ScheduleIcons.getIconOrDefault(block.icon),
                                contentDescription = null,
                                tint = nodeColor,
                                modifier = Modifier.size(21.dp)
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

                    // Edit: day blocks only are editable here (library edits live on the
                    // Zaman Blokları page). A template-linked day detaches on save.
                    if (onEdit != null) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Bloğu düzenle",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Remove from day (special days only): re-addable from the library
                    if (onRemoveFromDay != null) {
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = { confirmRemove = true }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Günden kaldır",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
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
                                    .clip(MaterialTheme.shapes.small)
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
                                    color = if (subtask.done) MaterialTheme.colorScheme.onSurfaceVariant
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

    if (confirmRemove && onRemoveFromDay != null) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text("Günden kaldır", fontWeight = FontWeight.Bold) },
            text = { Text("\"${block.name}\" bu günden kaldırılacak. Blok kütüphanede durmaya devam eder. Bu günün durumu (tamamlanma/toggle) kaybolur.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmRemove = false
                        onRemoveFromDay()
                    }
                ) {
                    Text("Kaldır", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }
}

private fun spineColor(isInProgress: Boolean, nodeColor: Color): Color =
    nodeColor.copy(alpha = if (isInProgress) 0.9f else 0.35f)

private fun durationText(block: BlockState): String = when {
    block.mode == "start_end" && !block.endTime.isNullOrBlank() ->
        "${block.startTime} - ${block.endTime}"
    block.durationMin != null ->
        "${block.startTime} • ${block.durationMin} dk"
    else -> block.startTime
}
