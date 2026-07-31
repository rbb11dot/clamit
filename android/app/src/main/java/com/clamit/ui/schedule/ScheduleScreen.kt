package com.clamit.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                // Expressive Drawer Header Card
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("⚡", fontSize = 24.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                "clamit",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Second Brain",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(8.dp))

                // Navigation Items
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Schedule", fontWeight = FontWeight.SemiBold) },
                    selected = currentPage == SchedulePage.HOME,
                    onClick = {
                        currentPage = SchedulePage.HOME
                        scope.launch { drawerState.close() }
                    },
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
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )

                Spacer(Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "clamit v0.1.0 • Termux Edition",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
