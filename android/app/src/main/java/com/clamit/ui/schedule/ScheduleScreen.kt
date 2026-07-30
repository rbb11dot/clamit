package com.clamit.ui.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var page by remember { mutableIntStateOf(0) } // 0=schedule, 1=templates, 2=blocks
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showDatePicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var showBlockEditor by remember { mutableStateOf(false) }
    var showTemplateEditor by remember { mutableStateOf(false) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text("clamit", style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp))
                Divider()
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("Schedule") },
                    selected = page == 0,
                    onClick = { page = 0; scope.launch { drawerState.close() } }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.FormatListBulleted, contentDescription = null) },
                    label = { Text("Gün Şablonları") },
                    selected = page == 1,
                    onClick = { page = 1 }
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Timer, contentDescription = null) },
                    label = { Text("Zaman Blokları") },
                    selected = page == 2,
                    onClick = { page = 2 }
                )
                Divider()
                Text("v0.1.0", style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(28.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    ) {
        when (page) {
            0 -> SchedulePage(viewModel, uiState, drawerState,
                onDatePicker = { showDatePicker = true },
                onBlockEdit = { showBlockEditor = true },
                onTemplatePicker = { showTemplatePicker = true })
            1 -> TemplatesPage(viewModel, uiState, drawerState,
                onNewTemplate = { showTemplateEditor = true })
            2 -> BlocksPage(viewModel, uiState, drawerState,
                onNewBlock = { showBlockEditor = true })
        }
    }

    if (showDatePicker) DatePickerDialog(viewModel, uiState) { showDatePicker = false }
    if (showTemplatePicker) TemplatePickerDialog(viewModel, uiState) { showTemplatePicker = false }
    if (showBlockEditor) FullBlockEditor(onDismiss = { showBlockEditor = false }, viewModel = viewModel)
    if (showTemplateEditor) FullTemplateEditor(onDismiss = { showTemplateEditor = false }, viewModel = viewModel)
}

// ---- Schedule Home Page ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchedulePage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    drawerState: DrawerState,
    onDatePicker: () -> Unit,
    onBlockEdit: () -> Unit,
    onTemplatePicker: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("tr"))
    val dayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale("tr"))

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Menü")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()) {
                        IconButton(onClick = viewModel::goToPreviousDay, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowBack, "Önceki", modifier = Modifier.size(20.dp))
                        }
                        Column(Modifier.clickable(onClick = onDatePicker), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.currentDate.format(formatter), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                            Text(uiState.currentDate.format(dayFormatter), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                        IconButton(onClick = viewModel::goToNextDay, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ArrowForward, "Sonraki", modifier = Modifier.size(20.dp))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onTemplatePicker) { Icon(Icons.Default.Schedule, "Şablon seç") }
                    IconButton(onClick = viewModel::goToToday) { Icon(Icons.Default.Today, "Bugün") }
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(onClick = onBlockEdit) { Icon(Icons.Default.Add, "Blok ekle") }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when {
                uiState.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                uiState.error != null -> Text("Hata: ${uiState.error}", color = MaterialTheme.colorScheme.error, Modifier.align(Alignment.Center))
                else -> {
                    val entry = uiState.entry
                    if (entry == null) Text("Yükleniyor...", Modifier.align(Alignment.Center))
                    else if (entry.blocks.isEmpty()) Text("Bu gün için zaman bloğu yok.\n+ butonu ile ekleyin.", Modifier.align(Alignment.Center))
                    else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

// ---- Templates Page ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TemplatesPage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    drawerState: DrawerState,
    onNewTemplate: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, "Menü")
                    }
                },
                title = { Text("Gün Şablonları") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTemplate) { Icon(Icons.Default.Add, "Yeni şablon") }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(uiState.templates, key = { it.id }) { t ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Icon(ScheduleIcons.getIconOrDefault(t.icon), null, Modifier.size(32.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(t.name, style = MaterialTheme.typography.titleMedium)
                            val days = t.repeatDays.joinToString(", ") { d ->
                                listOf("Pazar","Pzt","Salı","Çar","Per","Cuma","Cmt").getOrElse(d){"?"} }
                            Text(days, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { /* TODO: open detail */ }) { Icon(Icons.Default.Edit, "Düzenle") }
                    }
                }
            }
        }
    }
}

// ---- Blocks Page ----

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BlocksPage(
    viewModel: ScheduleViewModel,
    uiState: ScheduleUiState,
    drawerState: DrawerState,
    onNewBlock: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(Icons.Default.Menu, "Menü")
                    }
                },
                title = { Text("Zaman Blokları") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewBlock) { Icon(Icons.Default.Add, "Yeni blok") }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Text(
                "Zaman blokları şablonlar içinde yönetilir.\nBir şablona blok eklemek için Şablonlar sayfasını kullanın.",
                Modifier.align(Alignment.Center).padding(32.dp)
            )
        }
    }
}
