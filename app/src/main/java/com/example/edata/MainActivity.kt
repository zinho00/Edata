package com.example.edata

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.edata.ui.theme.EdataTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.max
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

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
    val context = LocalContext.current

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
                    onBack = { selectedEntryIndex = null },
                    onSave = { info ->
                        val updatedParent = items[itemIndex]
                        val updatedEntries = updatedParent.entries.toMutableList()
                        if (entryIndex in updatedEntries.indices) {
                            updatedEntries[entryIndex] = updatedEntries[entryIndex].copy(careInfo = info)
                            items[itemIndex] = updatedParent.copy(entries = updatedEntries)
                            saveHomeData(context, items, nextItemId)
                        }
                        selectedEntryIndex = null
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
                    onBack = {
                        selectedItemIndex = null
                        selectedEntryIndex = null
                    },
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
            Scaffold(
                modifier = modifier.fillMaxSize(),
                floatingActionButton = {
                    FloatingActionButton(onClick = { showDialog = true }) {
                        Text(text = "+")
                    }
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
                    }
                )

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
            }
        }
    }
}

@Composable
fun HomeItemList(
    items: List<HomeItem>,
    colorAssignments: Map<Int, Color>,
    contentPadding: PaddingValues,
    onItemClick: (HomeItem) -> Unit
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
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
fun ItemCard(
    title: String,
    createdAt: LocalDateTime,
    backgroundColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    val formattedTime = remember(createdAt) {
        createdAt.format(displayFormatter)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodyMedium,
                color = textColor.copy(alpha = 0.9f)
            )
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
fun EntryListScreen(
    item: HomeItem,
    itemColor: Color,
    onBack: () -> Unit,
    onAddEntry: () -> Unit,
    onEntryClick: (CareEntry) -> Unit
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddEntry) {
                Text(text = "+")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBack) {
                    Text(text = "返回")
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (item.entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                EntryList(
                    entries = item.entries,
                    cardColor = itemColor,
                    textColor = Color.White,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    onEntryClick = onEntryClick
                )
            }
        }
    }
}

@Composable
fun EntryList(
    entries: List<CareEntry>,
    cardColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onEntryClick: (CareEntry) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(entries) { entry ->
            ItemCard(
                title = entry.title,
                createdAt = entry.createdAt,
                backgroundColor = cardColor,
                textColor = textColor,
                onClick = { onEntryClick(entry) }
            )
        }
    }
}

@Composable
fun CareDetailScreen(
    parentTitle: String,
    entry: CareEntry,
    onBack: () -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) {
            Text(text = "返回")
        }
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
            Text(text = "创建 Item", textAlign = TextAlign.Start)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    label = { Text("名称") }
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