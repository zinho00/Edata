package com.example.edata

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.edata.R
import com.example.edata.ui.theme.EdataTheme
import androidx.core.content.FileProvider
import com.example.edata.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EdataTheme {
                HomeScreen()
            }
        }
    }
}

data class CareInfo(
    val babyFeedingTime: String = "",
    val babyFeedingCount: String = "",
    val babyMilkAmount: String = "",
    val babyWaterCount: String = "",
    val babyWaterAmount: String = "",
    val babyExcretionCount: String = "",
    val babyAbnormal: String = "",
    val momUrinationCount: String = "",
    val momUrinationAmount: String = "",
    val momWipeCount: String = "",
    val momOther: String = ""
)

data class CareEntry(
    val id: Int,
    val title: String,
    val createdAt: LocalDateTime,
    val careInfo: CareInfo = CareInfo()
)

data class HomeItem(
    val id: Int,
    val title: String,
    val createdAt: LocalDateTime,
    val color: Long = 0L,
    val entries: List<CareEntry> = emptyList()
)

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val items = remember { mutableStateListOf<HomeItem>() }
    var showDialog by remember { mutableStateOf(false) }
    var newItemTitle by remember { mutableStateOf("") }
    var nextItemId by remember { mutableStateOf(0) }
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }
    var selectedEntryIndex by remember { mutableStateOf<Int?>(null) }
    var itemPendingDeletion by remember { mutableStateOf<HomeItem?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    val selectedForExport = remember { mutableStateListOf<Int>() }
    var isExporting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var lastExportedFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(context) {
        val (savedItems, savedNextId) = loadHomeData(context)
        val (itemsWithColor, colorUpdated) = ensureItemColors(savedItems)
        items.clear()
        items.addAll(itemsWithColor.sortedByDescending { it.createdAt })
        val computedNextId = (itemsWithColor.maxOfOrNull { it.id } ?: -1) + 1
        nextItemId = max(savedNextId, computedNextId)
        if (colorUpdated) {
            saveHomeData(context, items, nextItemId)
        }
    }

    val itemKeys = items.map { it.id to it.color }
    val colorAssignments = remember(itemKeys) { assignCardColors(items) }

    val itemIndex = selectedItemIndex
    val entryIndex = selectedEntryIndex

    val navigateHome: () -> Unit = {
        selectedEntryIndex = null
        selectedItemIndex = null
    }
    val navigateBack: () -> Unit = {
        when {
            selectedEntryIndex != null -> selectedEntryIndex = null
            selectedItemIndex != null -> {
                selectedItemIndex = null
                selectedEntryIndex = null
            }
        }
    }

    when {
        itemIndex != null && entryIndex != null -> {
            val parent = items.getOrNull(itemIndex)
            val entry = parent?.entries?.getOrNull(entryIndex)
            if (parent == null || entry == null) {
                selectedEntryIndex = null
            } else {
                CareDetailScreen(
                    parentTitle = parent.title,
                    entry = entry,
                    onNavigateBack = navigateBack,
                    onNavigateHome = navigateHome,
                    onSave = { info ->
                        val updatedParent = items[itemIndex]
                        val updatedEntries = updatedParent.entries.toMutableList()
                        if (entryIndex in updatedEntries.indices) {
                            updatedEntries[entryIndex] = updatedEntries[entryIndex].copy(careInfo = info)
                            items[itemIndex] = updatedParent.copy(entries = updatedEntries)
                            saveHomeData(context, items, nextItemId)
                        }
                        navigateBack()
                    }
                )
            }
        }

        itemIndex != null -> {
            val parent = items.getOrNull(itemIndex)
            if (parent == null) {
                selectedItemIndex = null
                selectedEntryIndex = null
            } else {
                EntryListScreen(
                    item = parent,
                    itemColor = colorAssignments[parent.id]
                        ?: MaterialTheme.colorScheme.surface,
                    onNavigateBack = navigateBack,
                    onNavigateHome = navigateHome,
                    onAddEntry = {
                        val currentParent = items[itemIndex]
                        val now = LocalDateTime.now()
                        val newEntry = CareEntry(
                            id = (currentParent.entries.maxOfOrNull { it.id } ?: -1) + 1,
                            title = now.format(displayFormatter),
                            createdAt = now
                        )
                        val updatedEntries = (currentParent.entries + newEntry)
                            .sortedByDescending { it.createdAt }
                        items[itemIndex] = currentParent.copy(entries = updatedEntries)
                        saveHomeData(context, items, nextItemId)
                    },
                    onDeleteEntry = { entry ->
                        val latestParent = items[itemIndex]
                        val entryIndexToRemove =
                            latestParent.entries.indexOfFirst { it.id == entry.id }
                        if (entryIndexToRemove != -1) {
                            val updatedEntries = latestParent.entries
                                .toMutableList()
                                .also { it.removeAt(entryIndexToRemove) }
                            items[itemIndex] = latestParent.copy(entries = updatedEntries)
                            if (selectedEntryIndex != null) {
                                val currentSelected = selectedEntryIndex!!
                                when {
                                    currentSelected == entryIndexToRemove -> selectedEntryIndex = null
                                    currentSelected > entryIndexToRemove -> selectedEntryIndex = currentSelected - 1
                                }
                            }
                            saveHomeData(context, items, nextItemId)
                        }
                    },
                    onEntryClick = { entry ->
                        val latestParent = items[itemIndex]
                        val index = latestParent.entries.indexOfFirst { it.id == entry.id }
                        if (index != -1) {
                            selectedEntryIndex = index
                        }
                    }
                )
            }
        }

        else -> {
            var isMenuOpen by remember { mutableStateOf(false) }
            Scaffold(
                modifier = modifier.fillMaxSize(),
                topBar = {
                    HomeTopBar(
                        isMenuOpen = isMenuOpen,
                        onMenuClick = { isMenuOpen = true },
                        onDismissMenu = { isMenuOpen = false },
                        isShareEnabled = lastExportedFile?.exists() == true,
                        onExportClick = {
                            isMenuOpen = false
                            if (items.isEmpty()) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.export_dialog_empty),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                if (!isExporting) {
                                    selectedForExport.clear()
                                }
                                showExportDialog = true
                            }
                        },
                        onShareClick = {
                            isMenuOpen = false
                            val file = lastExportedFile
                            if (file == null || !file.exists()) {
                                lastExportedFile = null
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.share_no_recent_export),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val launched = shareFileViaWeChat(context, file)
                                val messageRes = if (launched) {
                                    R.string.share_launch_wechat
                                } else {
                                    R.string.share_failed_wechat_not_installed
                                }
                                Toast.makeText(
                                    context,
                                    context.getString(messageRes),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(onClick = { showDialog = true }) {
                        Text(text = "+")
                    }
                },
                bottomBar = {
                    PersistentNavigationBar(
                        onHomeClick = navigateHome,
                        onBackClick = navigateBack
                    )
                }
            ) { innerPadding ->
                HomeItemList(
                    items = items,
                    colorAssignments = colorAssignments,
                    contentPadding = innerPadding,
                    onItemClick = { item ->
                        val index = items.indexOfFirst { it.id == item.id }
                        if (index != -1) {
                            selectedItemIndex = index
                            selectedEntryIndex = null
                        }
                    },
                    onDeleteItem = { item ->
                        itemPendingDeletion = item
                    }
                )

                if (showExportDialog) {
                    ExportDialog(
                        items = items,
                        selectedIds = selectedForExport.toSet(),
                        isExporting = isExporting,
                        onToggleItem = { itemId ->
                            if (selectedForExport.contains(itemId)) {
                                selectedForExport.remove(itemId)
                            } else {
                                selectedForExport.add(itemId)
                            }
                        },
                        onDismiss = {
                            if (!isExporting) {
                                showExportDialog = false
                                selectedForExport.clear()
                            }
                        },
                        onConfirm = {
                            if (!isExporting) {
                                if (selectedForExport.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.export_no_selection),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    val selectedItems = items.filter { selectedForExport.contains(it.id) }
                                    if (selectedItems.isEmpty()) {
                                        Toast.makeText(
                                            context,
                                            context.getString(R.string.export_no_selection),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        isExporting = true
                                        coroutineScope.launch {
                                            val result = withContext(Dispatchers.IO) {
                                                runCatching { exportHomeItemsToExcel(context, selectedItems) }
                                            }
                                            isExporting = false
                                            showExportDialog = false
                                            selectedForExport.clear()
                                            result.fold(
                                                onSuccess = { file ->
                                                    lastExportedFile = file
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(
                                                            R.string.export_success_toast,
                                                            file.absolutePath
                                                        ),
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                },
                                                onFailure = {
                                                    val detail = it.localizedMessage
                                                        ?.takeIf { message -> message.isNotBlank() }
                                                        ?: context.getString(R.string.export_failed_unknown)
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.export_failed, detail),
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                if (showDialog) {
                    AddItemDialog(
                        title = newItemTitle,
                        onTitleChange = { newItemTitle = it },
                        onDismiss = {
                            showDialog = false
                            newItemTitle = ""
                        },
                        onConfirm = {
                            if (newItemTitle.isNotBlank()) {
                                val colorValue = generateCardColor(
                                    items.mapNotNull { existing ->
                                        existing.color.takeIf { value -> value != 0L }
                                    }.toSet()
                                )
                                items.add(
                                    HomeItem(
                                        id = nextItemId,
                                        title = newItemTitle.trim(),
                                        createdAt = LocalDateTime.now(),
                                        color = colorValue                                    )
                                )
                                items.sortByDescending { it.createdAt }
                                nextItemId += 1
                                saveHomeData(context, items, nextItemId)
                                showDialog = false
                                newItemTitle = ""
                            }
                        }
                    )
                }

                val pendingDeletionItem = itemPendingDeletion
                if (pendingDeletionItem != null) {
                    AlertDialog(
                        onDismissRequest = { itemPendingDeletion = null },
                        title = { Text(text = "确认删除") },
                        text = {
                            Text(text = "确定要删除\"${pendingDeletionItem.title}\"吗？")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val index = items.indexOfFirst { it.id == pendingDeletionItem.id }
                                    if (index != -1) {
                                        items.removeAt(index)
                                        if (selectedItemIndex != null) {
                                            val currentSelected = selectedItemIndex!!
                                            when {
                                                currentSelected == index -> {
                                                    selectedItemIndex = null
                                                    selectedEntryIndex = null
                                                }
                                                currentSelected > index -> selectedItemIndex = currentSelected - 1
                                            }
                                        }
                                        saveHomeData(context, items, nextItemId)
                                    }
                                    itemPendingDeletion = null
                                }
                            ) {
                                Text(text = "删除")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { itemPendingDeletion = null }) {
                                Text(text = "取消")
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    modifier: Modifier = Modifier,
    isMenuOpen: Boolean,
    onMenuClick: () -> Unit,
    onDismissMenu: () -> Unit,
    isShareEnabled: Boolean,
    onExportClick: () -> Unit,
    onShareClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(text = stringResource(id = R.string.app_name)) },
        actions = {
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = stringResource(id = R.string.open_menu)
                    )
                }
                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = onDismissMenu
                ) {
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.menu_export)) },
                        onClick = {
                            onExportClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(text = stringResource(id = R.string.menu_share_wechat)) },
                        enabled = isShareEnabled,
                        onClick = onShareClick
                    )
                }
            }
        }
    )
}

@Composable
fun ExportDialog(
    items: List<HomeItem>,
    selectedIds: Set<Int>,
    isExporting: Boolean,
    onToggleItem: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(id = R.string.export_dialog_title)) },
        text = {
            if (items.isEmpty()) {
                Text(text = stringResource(id = R.string.export_dialog_empty))
            } else {
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = selectedIds.contains(item.id),
                                onCheckedChange = { onToggleItem(item.id) },
                                enabled = !isExporting
                            )
                            Text(
                                text = item.title,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    id = R.string.export_entry_count,
                                    item.entries.size
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = items.isNotEmpty() && !isExporting
            ) {
                if (isExporting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(text = stringResource(id = R.string.export_dialog_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting
            ) {
                Text(text = stringResource(id = R.string.export_dialog_cancel))
            }
        }
    )
}

@Composable
fun HomeItemList(
    items: List<HomeItem>,
    colorAssignments: Map<Int, Color>,
    contentPadding: PaddingValues,
    onItemClick: (HomeItem) -> Unit,
    onDeleteItem: (HomeItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            ItemCard(
                title = item.title,
                createdAt = item.createdAt,
                backgroundColor = colorAssignments[item.id] ?: MaterialTheme.colorScheme.surface,
                textColor = Color.White,
                onClick = { onItemClick(item) },
                onLongPress = { onDeleteItem(item) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemCard(
    title: String,
    createdAt: LocalDateTime,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    showFormattedTime: Boolean = true
) {
    val formattedTime = remember(createdAt) {
        createdAt.format(displayFormatter)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongPress?.invoke() }
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = textColor
            )
            if (showFormattedTime) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor.copy(alpha = 0.9f)
                )
            }
        }
    }
}

private fun assignCardColors(items: List<HomeItem>): Map<Int, Color> {
    if (items.isEmpty()) return emptyMap()

    return items.associate { item ->
        val color = item.color.takeIf { it != 0L }?.let { stored -> Color(stored.toULong()) }
            ?: cardColorPalette.random()
        item.id to color
    }
}

private fun ensureItemColors(items: List<HomeItem>): Pair<List<HomeItem>, Boolean> {
    if (items.isEmpty()) return emptyList<HomeItem>() to false

    val usedColors = mutableSetOf<Long>()
    var updated = false

    val updatedItems = items.map { item ->
        val existingColor = item.color.takeIf { it != 0L }
        val colorValue = existingColor ?: generateCardColor(usedColors)
        if (existingColor == null) {
            updated = true
        }
        usedColors += colorValue
        if (item.color == colorValue) item else item.copy(color = colorValue)
    }
    return updatedItems to updated
}

private fun generateCardColor(excludedColors: Set<Long>): Long {
    val filteredPalette = cardColorPalette.filter { it.value.toLong() !in excludedColors }
    val selectedColor = if (filteredPalette.isNotEmpty()) {
        filteredPalette.random()
    } else {
        cardColorPalette.random()
    }
    return selectedColor.value.toLong()
}

private val cardColorPalette = listOf(
    Color(0xFF3F51B5), // Indigo
    Color(0xFF009688), // Teal
    Color(0xFF607D8B), // Blue Grey
    Color(0xFFF57C00), // Orange
    Color(0xFF8E24AA), // Purple
    Color(0xFF43A047), // Green
    Color(0xFF3949AB), // Deep Purple
    Color(0xFF00897B)  // Teal variant
)

@Composable
fun PersistentNavigationBar(
    onHomeClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        tonalElevation = 3.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            val buttonSpacing = 12.dp
            val buttonWidth = ((maxWidth - buttonSpacing) / 2).coerceAtLeast(0.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                Button(
                    modifier = Modifier.width(buttonWidth),
                    onClick = onHomeClick
                ) {
                    Text(text = "首页")
                }
                Button(
                    modifier = Modifier.width(buttonWidth),
                    onClick = onBackClick
                ) {
                    Text(text = "返回")
                }
            }
        }
    }
}

@Composable
fun EntryListScreen(
    item: HomeItem,
    itemColor: Color,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onAddEntry: () -> Unit,
    onDeleteEntry: (CareEntry) -> Unit,
    onEntryClick: (CareEntry) -> Unit
) {
    var entryPendingDeletion by remember { mutableStateOf<CareEntry?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Text(text = "+")
            }
        },
        bottomBar = {
            PersistentNavigationBar(
                onHomeClick = onNavigateHome,
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            }

            if (item.entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "暂无记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(item.entries) { entry ->
                    ItemCard(
                        title = entry.title,
                        createdAt = entry.createdAt,
                        backgroundColor = itemColor,
                        textColor = Color.White,
                        onClick = { onEntryClick(entry) },
                        onLongPress = { entryPendingDeletion = entry },
                        showFormattedTime = false
                    )
                }
            }
        }
    }

    val pendingDeletionEntry = entryPendingDeletion
    if (pendingDeletionEntry != null) {
        AlertDialog(
            onDismissRequest = { entryPendingDeletion = null },
            title = { Text(text = "确认删除") },
            text = {
                Text(text = "确定要删除\"${pendingDeletionEntry.title}\"吗？")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteEntry(pendingDeletionEntry)
                        entryPendingDeletion = null
                    }
                ) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { entryPendingDeletion = null }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

@Composable
fun CareDetailScreen(
    parentTitle: String,
    entry: CareEntry,
    onNavigateBack: () -> Unit,
    onNavigateHome: () -> Unit,
    onSave: (CareInfo) -> Unit
) {
    var babyFeedingTime by remember(entry) { mutableStateOf(entry.careInfo.babyFeedingTime) }
    var babyFeedingCount by remember(entry) { mutableStateOf(entry.careInfo.babyFeedingCount) }
    var babyMilkAmount by remember(entry) { mutableStateOf(entry.careInfo.babyMilkAmount) }
    var babyWaterCount by remember(entry) { mutableStateOf(entry.careInfo.babyWaterCount) }
    var babyWaterAmount by remember(entry) { mutableStateOf(entry.careInfo.babyWaterAmount) }
    var babyExcretionCount by remember(entry) { mutableStateOf(entry.careInfo.babyExcretionCount) }
    var babyAbnormal by remember(entry) { mutableStateOf(entry.careInfo.babyAbnormal) }
    var momUrinationCount by remember(entry) { mutableStateOf(entry.careInfo.momUrinationCount) }
    var momUrinationAmount by remember(entry) { mutableStateOf(entry.careInfo.momUrinationAmount) }
    var momWipeCount by remember(entry) { mutableStateOf(entry.careInfo.momWipeCount) }
    var momOther by remember(entry) { mutableStateOf(entry.careInfo.momOther) }

    Scaffold(
        bottomBar = {
            PersistentNavigationBar(
                onHomeClick = onNavigateHome,
                onBackClick = onNavigateBack
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = parentTitle,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "宝宝护理",
                style = MaterialTheme.typography.titleMedium
            )
            TextField(
                value = babyFeedingTime,
                onValueChange = { babyFeedingTime = it },
                label = { Text("喂奶时间") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = babyFeedingCount,
                onValueChange = { babyFeedingCount = it },
                label = { Text("喂奶次数") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = babyMilkAmount,
                onValueChange = { babyMilkAmount = it },
                label = { Text("奶量") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = babyWaterCount,
                onValueChange = { babyWaterCount = it },
                label = { Text("喂水次数") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = babyWaterAmount,
                onValueChange = { babyWaterAmount = it },
                label = { Text("水量") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = babyExcretionCount,
                onValueChange = { babyExcretionCount = it },
                label = { Text("大小便次数") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = babyAbnormal,
                onValueChange = { babyAbnormal = it },
                label = { Text("有无异常") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "宝妈护理",
                style = MaterialTheme.typography.titleMedium
            )
            TextField(
                value = momUrinationCount,
                onValueChange = { momUrinationCount = it },
                label = { Text("排尿次数") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = momUrinationAmount,
                onValueChange = { momUrinationAmount = it },
                label = { Text("排尿量") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = momWipeCount,
                onValueChange = { momWipeCount = it },
                label = { Text("擦身次数") },
                modifier = Modifier.fillMaxWidth()
            )
            TextField(
                value = momOther,
                onValueChange = { momOther = it },
                label = { Text("其它") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        CareInfo(
                            babyFeedingTime = babyFeedingTime,
                            babyFeedingCount = babyFeedingCount,
                            babyMilkAmount = babyMilkAmount,
                            babyWaterCount = babyWaterCount,
                            babyWaterAmount = babyWaterAmount,
                            babyExcretionCount = babyExcretionCount,
                            babyAbnormal = babyAbnormal,
                            momUrinationCount = momUrinationCount,
                            momUrinationAmount = momUrinationAmount,
                            momWipeCount = momWipeCount,
                            momOther = momOther
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "保存")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "创建新项目", textAlign = TextAlign.Start)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    label = { Text("请输入客户名称") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    EdataTheme {
        HomeScreen()
    }
}

private const val PREFS_NAME = "home_items_prefs"
private const val KEY_ITEMS = "home_items"
private const val KEY_NEXT_ID = "next_item_id"
private val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
private val storageFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
private val exportFileNameFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
private val exportHeaders = listOf(
    "记录",
    "喂奶时间",
    "喂奶次数",
    "奶量",
    "喂水次数",
    "水量",
    "宝宝大小便次数",
    "有无异常",
    "宝妈排尿次数",
    "排尿量",
    "擦身次数",
    "其他情况"
)

private fun saveHomeData(context: Context, items: List<HomeItem>, nextItemId: Int) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val editor = prefs.edit()
    editor.putString(KEY_ITEMS, items.toJsonString())
    editor.putInt(KEY_NEXT_ID, nextItemId)
    editor.apply()
}

private fun loadHomeData(context: Context): Pair<List<HomeItem>, Int> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val itemsJson = prefs.getString(KEY_ITEMS, null)
    val nextId = prefs.getInt(KEY_NEXT_ID, 0)
    val items = parseHomeItems(itemsJson)
    return items to nextId
}

private fun exportHomeItemsToExcel(context: Context, items: List<HomeItem>): File {
    require(items.isNotEmpty()) { "No items selected for export" }
    val workbook = XSSFWorkbook()
    val sheet = workbook.createSheet("护理记录")

    val titleStyle = workbook.createCellStyle().apply {
        val font = workbook.createFont()
        font.bold = true
        font.fontHeightInPoints = 14
        setFont(font)
    }

    val headerStyle = workbook.createCellStyle().apply {
        val font = workbook.createFont()
        font.bold = true
        setFont(font)
        wrapText = true
        alignment = HorizontalAlignment.CENTER
    }

    var rowIndex = 0
    items.forEach { item ->
        val titleRow = sheet.createRow(rowIndex++)
        val titleCell = titleRow.createCell(0)
        titleCell.setCellValue(item.title)
        titleCell.cellStyle = titleStyle
        sheet.addMergedRegion(
            CellRangeAddress(
                titleRow.rowNum,
                titleRow.rowNum,
                0,
                exportHeaders.lastIndex
            )
        )

        val headerRow = sheet.createRow(rowIndex++)
        exportHeaders.forEachIndexed { index, header ->
            val cell = headerRow.createCell(index)
            cell.setCellValue(header)
            cell.cellStyle = headerStyle
        }

        if (item.entries.isEmpty()) {
            val row = sheet.createRow(rowIndex++)
            row.createCell(0).setCellValue("暂无记录")
        } else {
            item.entries
                .sortedBy { it.createdAt }
                .forEach { entry ->
                    val row = sheet.createRow(rowIndex++)
                    val info = entry.careInfo
                    row.createCell(0).setCellValue(entry.title)
                    row.createCell(1).setCellValue(info.babyFeedingTime)
                    row.createCell(2).setCellValue(info.babyFeedingCount)
                    row.createCell(3).setCellValue(info.babyMilkAmount)
                    row.createCell(4).setCellValue(info.babyWaterCount)
                    row.createCell(5).setCellValue(info.babyWaterAmount)
                    row.createCell(6).setCellValue(info.babyExcretionCount)
                    row.createCell(7).setCellValue(info.babyAbnormal)
                    row.createCell(8).setCellValue(info.momUrinationCount)
                    row.createCell(9).setCellValue(info.momUrinationAmount)
                    row.createCell(10).setCellValue(info.momWipeCount)
                    row.createCell(11).setCellValue(info.momOther)
                }
        }

        rowIndex++
    }

    exportHeaders.indices.forEach { index ->
        sheet.autoSizeColumn(index)
    }

    val directory = context.getExternalFilesDir(null) ?: context.filesDir
    if (!directory.exists()) {
        directory.mkdirs()
    }
    val fileName = "care_export_${LocalDateTime.now().format(exportFileNameFormatter)}.xlsx"
    val file = File(directory, fileName)
    FileOutputStream(file).use { output ->
        workbook.write(output)
    }
    workbook.close()
    return file
}

private fun shareFileViaWeChat(context: Context, file: File): Boolean {
    val weChatPackage = "com.tencent.mm"
    if (!isPackageInstalled(context, weChatPackage)) {
        return false
    }

    val authority = "${BuildConfig.APPLICATION_ID}.fileprovider"
    val uri = FileProvider.getUriForFile(context, authority, file)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        putExtra(Intent.EXTRA_STREAM, uri)
        `package` = weChatPackage
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    val resolvedActivities = context.packageManager.queryIntentActivities(
        shareIntent,
        PackageManager.MATCH_DEFAULT_ONLY
    )
    resolvedActivities.forEach { info ->
        context.grantUriPermission(
            info.activityInfo.packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    }

    return try {
        context.startActivity(shareIntent)
        true
    } catch (error: ActivityNotFoundException) {
        false
    }
}

private fun isPackageInstalled(context: Context, packageName: String): Boolean {
    return runCatching {
        val packageManager = context.packageManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
    }.isSuccess
}

private fun List<HomeItem>.toJsonString(): String {
    val array = JSONArray()
    for (item in this) {
        array.put(item.toJson())
    }
    return array.toString()
}

private fun parseHomeItems(json: String?): List<HomeItem> {
    if (json.isNullOrBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val obj = array.optJSONObject(index) ?: continue
                val id = obj.optInt("id", index)
                val title = obj.optString("title", "")
                val createdAtString = obj.optString("createdAt", "")
                val createdAt = runCatching {
                    LocalDateTime.parse(createdAtString, storageFormatter)
                }.getOrElse { LocalDateTime.now() }
                val color = if (obj.has("color")) obj.optLong("color") else 0L
                val entries = obj.optJSONArray("entries")?.let { arrayEntries ->
                    buildList {
                        for (entryIndex in 0 until arrayEntries.length()) {
                            val entryObj = arrayEntries.optJSONObject(entryIndex) ?: continue
                            add(entryObj.toCareEntry())
                        }
                    }.sortedByDescending { it.createdAt }
                } ?: obj.optJSONObject("careInfo")?.let { legacyInfo ->
                    val info = legacyInfo.toCareInfo()
                    listOf(
                        CareEntry(
                            id = 0,
                            title = createdAt.format(displayFormatter),
                            createdAt = createdAt,
                            careInfo = info
                        )
                    )
                } ?: emptyList()
                add(
                    HomeItem(
                        id = id,
                        title = title,
                        createdAt = createdAt,
                        color = color,
                        entries = entries
                    )
                )
            }
        }.sortedByDescending { it.createdAt }
    } catch (_: JSONException) {
        emptyList()
    }
}

private fun JSONObject.toCareInfo(): CareInfo {
    return CareInfo(
        babyFeedingTime = optString("babyFeedingTime", ""),
        babyFeedingCount = optString("babyFeedingCount", ""),
        babyMilkAmount = optString("babyMilkAmount", ""),
        babyWaterCount = optString("babyWaterCount", ""),
        babyWaterAmount = optString("babyWaterAmount", ""),
        babyExcretionCount = optString("babyExcretionCount", ""),
        babyAbnormal = optString("babyAbnormal", ""),
        momUrinationCount = optString("momUrinationCount", ""),
        momUrinationAmount = optString("momUrinationAmount", ""),
        momWipeCount = optString("momWipeCount", ""),
        momOther = optString("momOther", "")
    )
}

private fun HomeItem.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("title", title)
        put("createdAt", createdAt.format(storageFormatter))
        put("color", color)
        put("entries", JSONArray().apply {
            for (entry in entries) {
                put(entry.toJson())
            }
        })
    }
}

private fun CareInfo.toJson(): JSONObject {
    return JSONObject().apply {
        put("babyFeedingTime", babyFeedingTime)
        put("babyFeedingCount", babyFeedingCount)
        put("babyMilkAmount", babyMilkAmount)
        put("babyWaterCount", babyWaterCount)
        put("babyWaterAmount", babyWaterAmount)
        put("babyExcretionCount", babyExcretionCount)
        put("babyAbnormal", babyAbnormal)
        put("momUrinationCount", momUrinationCount)
        put("momUrinationAmount", momUrinationAmount)
        put("momWipeCount", momWipeCount)
        put("momOther", momOther)
    }
}

private fun JSONObject.toCareEntry(): CareEntry {
    val id = optInt("id", 0)
    val title = optString("title", "")
    val createdAtString = optString("createdAt", "")
    val createdAt = runCatching {
        LocalDateTime.parse(createdAtString, storageFormatter)
    }.getOrElse { LocalDateTime.now() }
    val careInfo = optJSONObject("careInfo")?.toCareInfo() ?: CareInfo()
    return CareEntry(
        id = id,
        title = if (title.isNotBlank()) title else createdAt.format(displayFormatter),
        createdAt = createdAt,
        careInfo = careInfo
    )
}

private fun CareEntry.toJson(): JSONObject {
    return JSONObject().apply {
        put("id", id)
        put("title", title)
        put("createdAt", createdAt.format(storageFormatter))
        put("careInfo", careInfo.toJson())
    }
}