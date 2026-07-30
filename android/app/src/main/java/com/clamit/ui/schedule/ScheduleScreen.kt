package com.clamit.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

enum class SchedulePage { HOME, TEMPLATES, BLOCKS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf(SchedulePage.HOME) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var showBlockEditor by remember { mutableStateOf(false) }
    var showTemplateEditor by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? android.app.Activity

    // BackHandler: close drawer first, then go to HOME, then exit
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    BackHandler(enabled = !drawerState.isOpen && currentPage != SchedulePage.HOME) {
        currentPage = SchedulePage.HOME
    }
    BackHandler(enabled = !drawerState.isOpen && currentPage == SchedulePage.HOME) {
        activity?.finish()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Spacer(Modifier.height(24.dp))
                Text("clamit", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp))
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Schedule") },
                    selected = currentPage == SchedulePage.HOME,
                    onClick = { currentPage = SchedulePage.HOME; scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                    label = { Text("Gün Şablonları") },
                    selected = currentPage == SchedulePage.TEMPLATES,
                    onClick = { currentPage = SchedulePage.TEMPLATES }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                    label = { Text("Zaman Blokları") },
                    selected = currentPage == SchedulePage.BLOCKS,
                    onClick = { currentPage = SchedulePage.BLOCKS }
                )
                Spacer(Modifier.weight(1f))
                Text("v0.1.0", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(28.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) {
        when (currentPage) {
            SchedulePage.HOME -> ScheduleHomePage(
                viewModel = viewModel,
                uiState = uiState,
                onMenuClick = { scope.launch { drawerState.open() } },
                onDatePicker = { showDatePicker = true },
                onTemplatePicker = { showTemplatePicker = true },
                onAddBlock = { showBlockEditor = true }
            )
            SchedulePage.TEMPLATES -> TemplatesPage(
                viewModel = viewModel,
                uiState = uiState,
                onMenuClick = { scope.launch { drawerState.open() } },
                onNewTemplate = { showTemplateEditor = true }
            )
            SchedulePage.BLOCKS -> BlocksPage(
                viewModel = viewModel,
                uiState = uiState,
                onMenuClick = { scope.launch { drawerState.open() } },
                onNewBlock = { showBlockEditor = true }
            )
        }
    }

    // Dialogs & sheets
    if (showDatePicker) DatePickerDialog(viewModel, uiState) { showDatePicker = false }
    if (showTemplatePicker) TemplatePickerDialog(viewModel, uiState) { showTemplatePicker = false }
    if (showBlockEditor) BlockEditorPage(onDismiss = { showBlockEditor = false }, viewModel = viewModel)
    if (showTemplateEditor) TemplateEditorPage(onDismiss = { showTemplateEditor = false }, viewModel = viewModel, uiState = uiState)
}
