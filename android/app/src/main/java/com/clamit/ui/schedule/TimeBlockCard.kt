package com.clamit.ui.schedule

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.clamit.data.model.BlockState

@Composable
fun TimeBlockCard(
    block: BlockState,
    onToggleSubtask: (String) -> Unit,
    onSetCompleted: () -> Unit,
    onSetNotCompleted: () -> Unit
) {
    val statusColor = when (block.autoStatus) {
        "in_progress" -> Color(0xFFFFA726) // turuncu
        "completed" -> Color(0xFF66BB6A)   // yeşil
        else -> Color(0xFF90A4AE)           // gri (pending)
    }

    val backgroundColor by animateColorAsState(
        targetValue = if (block.manualStatus == "completed") Color(0xFFE8F5E9)
        else if (block.autoStatus == "in_progress") Color(0xFFFFF3E0)
        else MaterialTheme.colorScheme.surface
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header: time + icon + name + status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Time indicator
                Text(
                    text = block.startTime,
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Icon
                Text(text = block.icon, style = MaterialTheme.typography.titleLarge)

                Spacer(modifier = Modifier.width(8.dp))

                // Name
                Text(
                    text = block.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                // Duration
                val durationText = if (block.mode == "start_end" && block.endTime != null) {
                    "${block.startTime} - ${block.endTime}"
                } else if (block.durationMin != null) {
                    "${block.durationMin}dk"
                } else ""

                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Auto status badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = statusColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = when (block.autoStatus) {
                            "in_progress" -> "🔵 Devam"
                            "completed" -> "✅ Zaman doldu"
                            else -> "⏳ Bekliyor"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timeline line + Subtasks
            Row(modifier = Modifier.fillMaxWidth()) {
                // Timeline vertical line
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height((block.subtaskStates.size * 36 + 10).dp)
                        .background(statusColor, RoundedCornerShape(1.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Subtasks
                Column(modifier = Modifier.weight(1f)) {
                    block.subtaskStates.forEach { subtask ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleSubtask(subtask.subtaskId) }
                                .padding(vertical = 6.dp)
                        ) {
                            Checkbox(
                                checked = subtask.done,
                                onCheckedChange = { onToggleSubtask(subtask.subtaskId) }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = subtask.name,
                                style = MaterialTheme.typography.bodyMedium,
                                textDecoration = if (subtask.done) TextDecoration.LineThrough
                                    else TextDecoration.None,
                                color = if (subtask.done) MaterialTheme.colorScheme.onSurfaceVariant
                                    else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Manual status buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (block.manualStatus != "completed") {
                    FilledTonalButton(
                        onClick = onSetCompleted,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("✓ Tamamlandı", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (block.manualStatus == "completed") {
                    OutlinedButton(
                        onClick = onSetNotCompleted,
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("↩ Geri al", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
