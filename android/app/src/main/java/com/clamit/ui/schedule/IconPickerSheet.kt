package com.clamit.ui.schedule

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IconPickerSheet(
    selectedIcon: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("İkon Seç", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ScheduleIcons.icons) { iconName ->
                    val vector = ScheduleIcons.getIconOrDefault(iconName)
                    FilledTonalIconButton(
                        onClick = { onSelect(iconName); onDismiss() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(vector, contentDescription = iconName, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

// Convenience wrapper: manages state and shows sheet
@Composable
fun IconPickerButton(
    currentIcon: String,
    onIconSelected: (String) -> Unit
) {
    var showSheet by remember { mutableStateOf(false) }

    FilledTonalIconButton(onClick = { showSheet = true }) {
        Icon(ScheduleIcons.getIconOrDefault(currentIcon), contentDescription = "İkon seç")
    }

    if (showSheet) {
        IconPickerSheet(
            selectedIcon = currentIcon,
            onSelect = onIconSelected,
            onDismiss = { showSheet = false }
        )
    }
}
