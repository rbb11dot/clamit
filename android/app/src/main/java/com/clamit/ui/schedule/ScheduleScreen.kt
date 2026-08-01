package com.clamit.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
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
import com.clamit.data.model.TimeBlock
import kotlinx.coroutines.launch

enum class SchedulePage { HOME, TEMPLATES, BLOCKS }

/** What the block editor should do: create (block=null) or edit a block,
 *  and whether the edit targets the current day (day-owned copy, detaches
 *  template-linked days) or the library. */
data class BlockEditorRequest(
    val block: TimeBlock? = null,
    val addToCurrentDay: Boolean = false,
    val editDay: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableStateOf(SchedulePage.HOME) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTemplatePicker by remember { mutableStateOf(false) }
    var blockEditorRequest by remember { mutableStateOf<BlockEditorRequest?>(null) }
    var showTemplateEditor by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? android.app.Activity

    // Back navigation. The editor handler is registered LAST because Compose
    // gives the most recently composed enabled BackHandler precedence — editors
    // overlay everything, so Back must dismiss them before page navigation.
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    BackHandler(enabled = !drawerState.isOpen && currentPage != SchedulePage.HOME) {
        currentPage = SchedulePage.HOME
    }
    BackHandler(enabled = !drawerState.isOpen && currentPage == SchedulePage.HOME) {
        activity?.finish()
    }
    BackHandler(enabled = blockEditorRequest != null || showTemplateEditor) {
        blockEditorRequest = null
        showTemplateEditor = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
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
                onEditBlock = { block ->
                    blockEditorRequest = BlockEditorRequest(block = block, editDay = true)
                },
                onAddBlock = {
                    blockEditorRequest = BlockEditorRequest(addToCurrentDay = true)
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
                onNewBlock = { blockEditorRequest = BlockEditorRequest() },
                onEditBlock = { block -> blockEditorRequest = BlockEditorRequest(block = block) }
            )
        }
    }

    if (showDatePicker) DatePickerDialog(viewModel, uiState) { showDatePicker = false }
    if (showTemplatePicker) TemplatePickerDialog(viewModel, uiState) { showTemplatePicker = false }
    blockEditorRequest?.let { req ->
        BlockEditorPage(
            onDismiss = { blockEditorRequest = null },
            viewModel = viewModel,
            uiState = uiState,
            addToCurrentDay = req.addToCurrentDay,
            blockToEdit = req.block,
            editDayBlock = req.editDay
        )
    }
    if (showTemplateEditor) TemplateEditorPage(onDismiss = { showTemplateEditor = false }, viewModel = viewModel, uiState = uiState)
}
