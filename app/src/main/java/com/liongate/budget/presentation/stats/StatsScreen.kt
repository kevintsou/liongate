package com.liongate.budget.presentation.stats

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.liongate.budget.data.local.dao.CategoryStat
import com.liongate.budget.data.local.dao.MonthlyStat
import com.liongate.budget.domain.model.Category
import com.liongate.budget.util.DateUtils
import com.liongate.budget.util.formatCurrency
import com.liongate.budget.util.toAndroidColor
import com.liongate.budget.util.toComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("統計分析", color = MaterialTheme.colorScheme.onPrimary) },
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
            TabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(selected = uiState.selectedTab == 0, onClick = { viewModel.selectTab(0) }, text = { Text("月報表") })
                Tab(selected = uiState.selectedTab == 1, onClick = { viewModel.selectTab(1) }, text = { Text("年報表") })
            }

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.selectedTab == 0) {
                    // Monthly
                    item {
                        MonthSelector(
                            month = uiState.currentMonth,
                            onPrevious = { viewModel.navigateMonth(false) },
                            onNext = { viewModel.navigateMonth(true) }
                        )
                    }
                    item {
                        MonthlySummaryRow(uiState.monthlyIncome, uiState.monthlyExpense)
                    }
                    if (uiState.monthCategoryStats.isNotEmpty()) {
                        item {
                            SectionCard(title = "支出分類") {
                                ExpensePieChart(
                                    stats = uiState.monthCategoryStats,
                                    categories = uiState.categories
                                )
                            }
                        }
                    }
                    if (uiState.last6Months.isNotEmpty()) {
                        item {
                            SectionCard(title = "近6個月趨勢") {
                                MonthlyBarChart(months = uiState.last6Months)
                            }
                        }
                    }
                    if (uiState.monthCategoryStats.isNotEmpty()) {
                        item {
                            SectionCard(title = "分類明細") {
                                CategoryBreakdownList(
                                    stats = uiState.monthCategoryStats,
                                    categories = uiState.categories,
                                    total = uiState.monthlyExpense
                                )
                            }
                        }
                    }
                    if (uiState.monthCategoryStats.isEmpty() && uiState.last6Months.isEmpty()) {
                        item { EmptyStatsView() }
                    }
                } else {
                    // Yearly
                    item {
                        YearSelector(
                            year = uiState.currentYear,
                            onPrevious = { viewModel.navigateYear(false) },
                            onNext = { viewModel.navigateYear(true) }
                        )
                    }
                    item {
                        MonthlySummaryRow(uiState.yearlyIncome, uiState.yearlyExpense)
                    }
                    if (uiState.yearlyMonths.isNotEmpty()) {
                        item {
                            SectionCard(title = "全年收支") {
                                YearlyBarChart(months = uiState.yearlyMonths, year = uiState.currentYear)
                            }
                        }
                    }
                    if (uiState.yearCategoryStats.isNotEmpty()) {
                        item {
                            SectionCard(title = "年度支出分類 Top 5") {
                                CategoryBreakdownList(
                                    stats = uiState.yearCategoryStats.take(5),
                                    categories = uiState.categories,
                                    total = uiState.yearlyExpense
                                )
                            }
                        }
                    }
                    if (uiState.yearlyMonths.isEmpty()) {
                        item { EmptyStatsView() }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthSelector(month: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "上個月") }
        Text(DateUtils.formatMonthYear(month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "下個月") }
    }
}

@Composable
private fun YearSelector(year: String, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) { Icon(Icons.Default.ChevronLeft, "上一年") }
        Text("${year}年", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        IconButton(onClick = onNext) { Icon(Icons.Default.ChevronRight, "下一年") }
    }
}

@Composable
private fun MonthlySummaryRow(income: Double, expense: Double) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryChip(label = "收入", amount = income, color = Color(0xFF34A853), modifier = Modifier.weight(1f))
        SummaryChip(label = "支出", amount = expense, color = Color(0xFFEA4335), modifier = Modifier.weight(1f))
        SummaryChip(label = "結餘", amount = income - expense, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SummaryChip(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Spacer(Modifier.height(2.dp))
            Text(
                amount.formatCurrency(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ExpensePieChart(stats: List<CategoryStat>, categories: List<Category>) {
    val categoryMap = categories.associateBy { it.id }
    val entries = stats.map { stat ->
        val cat = categoryMap[stat.categoryId]
        PieEntry(stat.total.toFloat(), cat?.name ?: "")
    }
    val colors = stats.map { stat ->
        categoryMap[stat.categoryId]?.colorHex?.toAndroidColor() ?: AndroidColor.GRAY
    }

    AndroidView(
        factory = { ctx ->
            PieChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = false
                isDrawHoleEnabled = true
                holeRadius = 55f
                transparentCircleRadius = 60f
                setTransparentCircleAlpha(50)
                setDrawEntryLabels(false)
                setUsePercentValues(true)
                isRotationEnabled = true
                setHoleColor(AndroidColor.TRANSPARENT)
                animateY(600)
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "").apply {
                this.colors = colors
                sliceSpace = 2f
                setDrawValues(true)
                valueTextSize = 10f
                valueTextColor = AndroidColor.WHITE
            }
            chart.data = PieData(dataSet)
            chart.invalidate()
        },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@Composable
private fun MonthlyBarChart(months: List<MonthlyStat>) {
    val labels = months.map { it.month.substring(5) } // "MM"
    AndroidView(
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = true
                setDrawGridBackground(false)
                setDrawBorders(false)
                setPinchZoom(false)
                setScaleEnabled(false)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                    textSize = 10f
                }
                axisLeft.apply {
                    setDrawGridLines(true)
                    axisMinimum = 0f
                    textSize = 9f
                }
                axisRight.isEnabled = false
                animateY(600)
            }
        },
        update = { chart ->
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels.toTypedArray())
            val incomeEntries = months.mapIndexed { i, m -> BarEntry(i.toFloat(), m.totalIncome.toFloat()) }
            val expenseEntries = months.mapIndexed { i, m -> BarEntry(i.toFloat(), m.totalExpense.toFloat()) }
            val incomeSet = BarDataSet(incomeEntries, "收入").apply { color = AndroidColor.parseColor("#34A853"); setDrawValues(false) }
            val expenseSet = BarDataSet(expenseEntries, "支出").apply { color = AndroidColor.parseColor("#EA4335"); setDrawValues(false) }
            val groupSpace = 0.3f; val barSpace = 0.02f; val barWidth = 0.33f
            val data = BarData(incomeSet, expenseSet).apply { this.barWidth = barWidth }
            chart.data = data
            chart.groupBars(0f, groupSpace, barSpace)
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = data.getGroupWidth(groupSpace, barSpace) * months.size
            chart.invalidate()
        },
        modifier = Modifier.fillMaxWidth().height(200.dp)
    )
}

@Composable
private fun YearlyBarChart(months: List<MonthlyStat>, year: String) {
    val allMonths = (1..12).map { m -> "${year}-${m.toString().padStart(2, '0')}" }
    val statsMap = months.associateBy { it.month }
    val labels = (1..12).map { "${it}月" }
    AndroidView(
        factory = { ctx ->
            BarChart(ctx).apply {
                description.isEnabled = false
                legend.isEnabled = true
                setDrawGridBackground(false)
                setDrawBorders(false)
                setPinchZoom(false)
                setScaleEnabled(false)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    setDrawGridLines(false)
                    textSize = 9f
                    labelCount = 12
                }
                axisLeft.apply { setDrawGridLines(true); axisMinimum = 0f; textSize = 9f }
                axisRight.isEnabled = false
                animateY(800)
            }
        },
        update = { chart ->
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(labels.toTypedArray())
            val incomeEntries = allMonths.mapIndexed { i, m -> BarEntry(i.toFloat(), statsMap[m]?.totalIncome?.toFloat() ?: 0f) }
            val expenseEntries = allMonths.mapIndexed { i, m -> BarEntry(i.toFloat(), statsMap[m]?.totalExpense?.toFloat() ?: 0f) }
            val incomeSet = BarDataSet(incomeEntries, "收入").apply { color = AndroidColor.parseColor("#34A853"); setDrawValues(false) }
            val expenseSet = BarDataSet(expenseEntries, "支出").apply { color = AndroidColor.parseColor("#EA4335"); setDrawValues(false) }
            val groupSpace = 0.3f; val barSpace = 0.02f; val barWidth = 0.33f
            val data = BarData(incomeSet, expenseSet).apply { this.barWidth = barWidth }
            chart.data = data
            chart.groupBars(0f, groupSpace, barSpace)
            chart.xAxis.axisMinimum = 0f
            chart.xAxis.axisMaximum = data.getGroupWidth(groupSpace, barSpace) * 12
            chart.invalidate()
        },
        modifier = Modifier.fillMaxWidth().height(220.dp)
    )
}

@Composable
private fun CategoryBreakdownList(stats: List<CategoryStat>, categories: List<Category>, total: Double) {
    val categoryMap = categories.associateBy { it.id }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { stat ->
            val cat = categoryMap[stat.categoryId] ?: return@forEach
            val percent = if (total > 0) (stat.total / total * 100).toInt() else 0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(cat.colorHex.toComposeColor().copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) { Text(cat.icon, fontSize = 16.sp) }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text(cat.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        Text("${percent}%  ${stat.total.formatCurrency()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (percent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                        color = cat.colorHex.toComposeColor(),
                        trackColor = cat.colorHex.toComposeColor().copy(alpha = 0.15f)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStatsView() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("本期無記錄", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
