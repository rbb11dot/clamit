package com.clamit.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var blockEditorAddToDay by remember { mutableStateOf(false) }
    var showTemplateEditor by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? android.app.Activity

    // Full-screen editors overlay everything: Back must dismiss the editor first,
    // never exit the app or navigate away underneath it. Registered last so it wins.
    BackHandler(enabled = showBlockEditor || showTemplateEditor) {
        showBlockEditor = false
        showTemplateEditor = false
    }
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
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                modifier = Modifier.width(292.dp)
            ) {
                Spacer(Modifier.height(20.dp))
                Text(
                    "clamit",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.3).sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                Text(
                    "Second Brain",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(20.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Schedule", fontWeight = FontWeight.SemiBold) },
                    selected = currentPage == SchedulePage.HOME,
                    onClick = {
                        currentPage = SchedulePage.HOME
                        scope.launch { drawerState.close() }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                    label = { Text("Gün Şablonları", fontWeight = FontWeight.SemiBold) },
                    badge = {
                        if (uiState.templates.isNotEmpty()) {
                            Badge { Text("${uiState.templates.size}") }
                        }
                    },
                    selected = currentPage == SchedulePage.TEMPLATES,
                    onClick = {
                        currentPage = SchedulePage.TEMPLATES
                        scope.launch { drawerState.close() }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                    label = { Text("Zaman Blokları", fontWeight = FontWeight.SemiBold) },
                    selected = currentPage == SchedulePage.BLOCKS,
                    onClick = {
                        currentPage = SchedulePage.BLOCKS
                        scope.launch { drawerState.close() }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Spacer(Modifier.weight(1f))
                Text(
                    "v0.1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp)
                )
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
                onAddBlock = {
                    blockEditorAddToDay = true
                    showBlockEditor = true
                }
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
                onNewBlock = {
                    blockEditorAddToDay = false
                    showBlockEditor = true
                }
            )
        }
    }

    if (showDatePicker) DatePickerDialog(viewModel, uiState) { showDatePicker = false }
    if (showTemplatePicker) TemplatePickerDialog(viewModel, uiState) { showTemplatePicker = false }
    if (showBlockEditor) BlockEditorPage(
        onDismiss = { showBlockEditor = false },
        viewModel = viewModel,
        addToCurrentDay = blockEditorAddToDay
    )
    if (showTemplateEditor) TemplateEditorPage(onDismiss = { showTemplateEditor = false }, viewModel = viewModel, uiState = uiState)
}
