package com.liongate.budget.presentation.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.liongate.budget.util.DateUtils
import com.liongate.budget.util.formatCurrency
import com.liongate.budget.util.toComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    onNavigateToCategories: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var editingItem by remember { mutableStateOf<BudgetItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("預算管理", color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Month navigation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateMonth(false) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "上個月")
                }
                Text(
                    DateUtils.formatMonthYear(uiState.currentMonth),
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = { viewModel.navigateMonth(true) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "下個月")
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.budgetItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("尚無支出分類", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = onNavigateToCategories) { Text("前往建立分類") }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(uiState.budgetItems) { item ->
                        BudgetItemCard(
                            item = item,
                            onSetBudget = { editingItem = item },
                            onRemoveBudget = { viewModel.removeBudget(item.category.id) }
                        )
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }

    editingItem?.let { item ->
        SetBudgetDialog(
            item = item,
            onDismiss = { editingItem = null },
            onConfirm = { amount ->
                viewModel.setBudget(item.category.id, amount)
                editingItem = null
            }
        )
    }
}

@Composable
private fun BudgetItemCard(
    item: BudgetItem,
    onSetBudget: () -> Unit,
    onRemoveBudget: () -> Unit
) {
    val progress = (item.percentUsed / 100f).coerceIn(0f, 1f)
    val progressColor = when {
        item.isOverBudget -> Color(0xFFEA4335)
        item.percentUsed >= 70 -> Color(0xFFFF9800)
        else -> Color(0xFF34A853)
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(item.category.colorHex.toComposeColor().copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) { Text(item.category.icon, fontSize = 20.sp) }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        item.category.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row {
                    if (item.budget != null) {
                        IconButton(onClick = onRemoveBudget, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "移除預算", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                        }
                    }
                    IconButton(onClick = onSetBudget, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "設定預算", modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (item.budget != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("已花費", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.spent.formatCurrency(), fontWeight = FontWeight.Bold, color = progressColor)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("預算上限", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.budget.limitAmount.formatCurrency(), fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.15f)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${item.percentUsed.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = progressColor
                    )
                    val remaining = item.budget.limitAmount - item.spent
                    Text(
                        if (item.isOverBudget) "超出 ${(-remaining).formatCurrency()}" else "剩餘 ${remaining.formatCurrency()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("已花費: ${item.spent.formatCurrency()}", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onSetBudget, contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Text("設定預算", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetBudgetDialog(
    item: BudgetItem,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(item.budget?.limitAmount?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("設定「${item.category.name}」預算") },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("預算金額 (NT$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { amountText.toDoubleOrNull()?.let { if (it > 0) onConfirm(it) } },
                enabled = amountText.toDoubleOrNull() != null && amountText.toDoubleOrNull()!! > 0
            ) { Text("確定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
