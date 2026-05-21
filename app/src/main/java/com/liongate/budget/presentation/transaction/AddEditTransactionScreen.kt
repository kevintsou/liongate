package com.liongate.budget.presentation.transaction

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.liongate.budget.domain.model.Category
import com.liongate.budget.domain.model.TransactionType
import com.liongate.budget.util.DateUtils
import com.liongate.budget.util.toComposeColor
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTransactionScreen(
    transactionId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: TransactionViewModel = hiltViewModel()
) {
    val addEditState by viewModel.addEditState.collectAsState()

    var amount by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadTransaction(transactionId)
        }
    }

    LaunchedEffect(addEditState.transaction) {
        if (!isInitialized && addEditState.transaction != null) {
            val tx = addEditState.transaction!!
            amount = tx.amount.toBigDecimal().stripTrailingZeros().toPlainString()
            selectedType = tx.type
            selectedCategoryId = tx.categoryId
            description = tx.description
            selectedDate = tx.date
            isInitialized = true
        }
    }

    LaunchedEffect(addEditState.isSaved) {
        if (addEditState.isSaved) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }

    val visibleCategories = addEditState.categories.filter {
        it.type.name == selectedType.name || it.type.name == "BOTH"
    }

    if (selectedCategoryId == null && visibleCategories.isNotEmpty()) {
        selectedCategoryId = visibleCategories.first().id
    }

    val incomeColor = Color(0xFF34A853)
    val expenseColor = Color(0xFFEA4335)
    val activeColor = if (selectedType == TransactionType.INCOME) incomeColor else expenseColor

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (transactionId != null) "編輯記錄" else "新增記錄",
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "返回",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Type toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(TransactionType.EXPENSE to "支出", TransactionType.INCOME to "收入").forEach { (type, label) ->
                    val isSelected = selectedType == type
                    val bgColor by animateColorAsState(
                        if (isSelected) if (type == TransactionType.INCOME) incomeColor else expenseColor
                        else Color.Transparent,
                        label = "typeColor"
                    )
                    val textColor by animateColorAsState(
                        if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "textColor"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable {
                                selectedType = type
                                selectedCategoryId = null
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = textColor, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Amount display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { v ->
                        val filtered = v.filter { it.isDigit() || it == '.' }
                        val parts = filtered.split(".")
                        if (parts.size <= 2 && (parts.getOrNull(1)?.length ?: 0) <= 2) {
                            amount = filtered
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = activeColor,
                        textAlign = TextAlign.Center
                    ),
                    placeholder = {
                        Text(
                            "0",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            fontSize = 36.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    },
                    prefix = {
                        Text(
                            "NT$ ",
                            fontSize = 18.sp,
                            color = activeColor,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = activeColor,
                        unfocusedBorderColor = activeColor.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category section
            Text(
                "分類",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(visibleCategories) { category ->
                    CategoryGridItem(
                        category = category,
                        isSelected = selectedCategoryId == category.id,
                        activeColor = activeColor,
                        onClick = { selectedCategoryId = category.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date picker
            Text(
                "日期",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { showDatePicker = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        DateUtils.formatFullDate(selectedDate),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = "選擇日期",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text(
                "備注",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("選填備注...") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Save button
            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull() ?: return@Button
                    val catId = selectedCategoryId ?: return@Button
                    if (amountValue <= 0) return@Button
                    viewModel.saveTransaction(
                        id = transactionId,
                        amount = amountValue,
                        type = selectedType,
                        categoryId = catId,
                        description = description,
                        date = selectedDate
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                enabled = amount.toDoubleOrNull() != null && amount.toDoubleOrNull()!! > 0 && selectedCategoryId != null
            ) {
                Text("儲存", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("確定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun CategoryGridItem(
    category: Category,
    isSelected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) activeColor else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .background(
                if (isSelected) activeColor.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(category.colorHex.toComposeColor().copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text(category.icon, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            category.name,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            maxLines = 1,
            color = if (isSelected) activeColor else MaterialTheme.colorScheme.onSurface
        )
    }
}
