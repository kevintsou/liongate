package com.liongate.budget.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.liongate.budget.presentation.budget.BudgetScreen
import com.liongate.budget.presentation.category.CategoryScreen
import com.liongate.budget.presentation.home.HomeScreen
import com.liongate.budget.presentation.stats.StatsScreen
import com.liongate.budget.presentation.transaction.AddEditTransactionScreen
import com.liongate.budget.presentation.transaction.TransactionListScreen

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "首頁")
    object Transactions : Screen("transactions", "收支")
    object Stats : Screen("stats", "統計")
    object Budget : Screen("budget", "預算")
    object AddTransaction : Screen("add_transaction?transactionId={transactionId}", "新增")
    object Categories : Screen("categories", "類別")

    companion object {
        fun addTransactionRoute(transactionId: Long? = null): String {
            return if (transactionId != null) "add_transaction?transactionId=$transactionId"
            else "add_transaction?transactionId=-1"
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        Triple(Screen.Home, Icons.Default.Home, "首頁"),
        Triple(Screen.Transactions, Icons.Default.List, "收支"),
        Triple(Screen.Stats, Icons.Default.BarChart, "統計"),
        Triple(Screen.Budget, Icons.Default.AccountBalance, "預算")
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomNavItems.any { (screen, _, _) ->
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            }

            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { (screen, icon, label) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == screen.route
                            } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToTransactions = {
                        navController.navigate(Screen.Transactions.route)
                    },
                    onAddTransaction = {
                        navController.navigate(Screen.addTransactionRoute())
                    }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionListScreen(
                    onAddTransaction = {
                        navController.navigate(Screen.addTransactionRoute())
                    },
                    onEditTransaction = { id ->
                        navController.navigate(Screen.addTransactionRoute(id))
                    }
                )
            }
            composable(Screen.Stats.route) {
                StatsScreen()
            }
            composable(Screen.Budget.route) {
                BudgetScreen(
                    onNavigateToCategories = {
                        navController.navigate(Screen.Categories.route)
                    }
                )
            }
            composable(
                route = Screen.AddTransaction.route,
                arguments = listOf(
                    navArgument("transactionId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    }
                )
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId") ?: -1L
                AddEditTransactionScreen(
                    transactionId = if (transactionId == -1L) null else transactionId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Categories.route) {
                CategoryScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
