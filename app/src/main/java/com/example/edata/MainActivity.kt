package com.example.edata

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.edata.ui.theme.EdataTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

data class HomeItem(
    val id: Int,
    val title: String,
    val createdAt: LocalDateTime,
    val careInfo: CareInfo = CareInfo()
)
@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val items = remember { mutableStateListOf<HomeItem>() }
    var showDialog by remember { mutableStateOf(false) }
    var newItemTitle by remember { mutableStateOf("") }
    var nextItemId by remember { mutableStateOf(0) }
    var selectedItemIndex by remember { mutableStateOf<Int?>(null) }

    val detailIndex = selectedItemIndex

    if (detailIndex != null) {
        CareDetailScreen(
            item = items[detailIndex],
            onBack = { selectedItemIndex = null },
            onSave = { info ->
                items[detailIndex] = items[detailIndex].copy(careInfo = info)
                selectedItemIndex = null
            }
        )
    } else {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            floatingActionButton = {
                FloatingActionButton(onClick = { showDialog = true }) {
                    Text(text = "+")
                }
            }
        ) { innerPadding ->
            ItemList(
                items = items,
                contentPadding = innerPadding,
                onItemClick = { item ->
                    val index = items.indexOfFirst { it.id == item.id }
                    if (index != -1) {
                        selectedItemIndex = index
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
                            items.add(
                                HomeItem(
                                    id = nextItemId,
                                    title = newItemTitle.trim(),
                                    createdAt = LocalDateTime.now()
                                )
                            )
                            nextItemId += 1
                            showDialog = false
                            newItemTitle = ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ItemList(
    items: List<HomeItem>,
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
                item = item,
                onClick = { onItemClick(item) }
            )        }
    }
}

@Composable
fun ItemCard(item: HomeItem, onClick: () -> Unit) {
    val formattedTime = remember(item.createdAt) {
        item.createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CareDetailScreen(
    item: HomeItem,
    onBack: () -> Unit,
    onSave: (CareInfo) -> Unit
) {
    var babyFeedingTime by remember(item) { mutableStateOf(item.careInfo.babyFeedingTime) }
    var babyFeedingCount by remember(item) { mutableStateOf(item.careInfo.babyFeedingCount) }
    var babyMilkAmount by remember(item) { mutableStateOf(item.careInfo.babyMilkAmount) }
    var babyWaterCount by remember(item) { mutableStateOf(item.careInfo.babyWaterCount) }
    var babyWaterAmount by remember(item) { mutableStateOf(item.careInfo.babyWaterAmount) }
    var babyExcretionCount by remember(item) { mutableStateOf(item.careInfo.babyExcretionCount) }
    var babyAbnormal by remember(item) { mutableStateOf(item.careInfo.babyAbnormal) }
    var momUrinationCount by remember(item) { mutableStateOf(item.careInfo.momUrinationCount) }
    var momUrinationAmount by remember(item) { mutableStateOf(item.careInfo.momUrinationAmount) }
    var momWipeCount by remember(item) { mutableStateOf(item.careInfo.momWipeCount) }
    var momOther by remember(item) { mutableStateOf(item.careInfo.momOther) }

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
            text = item.title,
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