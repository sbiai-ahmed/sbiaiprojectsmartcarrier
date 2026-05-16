package com.example.myapplication.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.database.AppRepository
import com.example.myapplication.models.UserRole

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object AdminDashboard : Screen("admin_dashboard")
    object Reception : Screen("employee_reception")
    object Search : Screen("search")
    object RepairsList : Screen("repairs_list")
    object UserManagement : Screen("user_management")
    object Reports : Screen("reports")
    object Expenses : Screen("expenses")
    object Inventory : Screen("inventory")
    object Purchases : Screen("purchases")
    object Suppliers : Screen("suppliers")
    object Sales : Screen("sales")
    object EditDevice : Screen("edit_device/{deviceId}") {
        fun createRoute(deviceId: String) = "edit_device/$deviceId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Login.route) {
        composable(Screen.Login.route) {
            LoginScreen(onLoginSuccess = { user ->
                AppRepository.currentUser = user

                if (user.role == UserRole.ADMIN) {
                    navController.navigate(Screen.AdminDashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                } else {
                    navController.navigate(Screen.Reception.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            })
        }
        
        composable(Screen.AdminDashboard.route) {
            AdminDashboard(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.AdminDashboard.route) { inclusive = true }
                    }
                },
                onNavigateToUsers = {
                    navController.navigate(Screen.UserManagement.route) { launchSingleTop = true }
                },
                onNavigateToReports = {
                    navController.navigate(Screen.Reports.route) { launchSingleTop = true }
                },
                onNavigateToExpenses = {
                    navController.navigate(Screen.Expenses.route) { launchSingleTop = true }
                },
                onNavigateToInventory = {
                    navController.navigate(Screen.Inventory.route) { launchSingleTop = true }
                },
                onNavigateToPurchases = {
                    navController.navigate(Screen.Purchases.route) { launchSingleTop = true }
                },
                onNavigateToSuppliers = {
                    navController.navigate(Screen.Suppliers.route) { launchSingleTop = true }
                },
                onNavigateToSales = {
                    navController.navigate(Screen.Sales.route) { launchSingleTop = true }
                }
            )
        }
        
        composable(Screen.Reports.route) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Expenses.route) {
            ExpensesScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Inventory.route) {
            InventoryScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Purchases.route) {
            PurchasesScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Suppliers.route) {
            SupplierStatementScreen(onBack = { navController.popBackStack() })
        }
        
        composable(Screen.UserManagement.route) {
            UserManagementScreen(onBack = { navController.popBackStack() })
        }
        
        composable(Screen.Reception.route) {
            ReceptionScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Reception.route) { inclusive = true }
                    }
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route) { launchSingleTop = true }
                },
                onNavigateToList = {
                    navController.navigate(Screen.RepairsList.route) { launchSingleTop = true }
                },
                onNavigateToSales = {
                    navController.navigate(Screen.Sales.route) { launchSingleTop = true }
                }
            )
        }

        composable(Screen.Sales.route) {
            SalesScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onEditDevice = { id -> 
                    navController.navigate(Screen.EditDevice.createRoute(id)) {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.EditDevice.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { entry ->
            val deviceId = entry.savedStateHandle.get<String>("deviceId") ?: ""
            
            EditDeviceScreen(
                deviceId = deviceId,
                onSaveSuccess = { 
                    navController.popBackStack() 
                },
                onBack = { 
                    navController.popBackStack() 
                }
            )
        }

        composable(Screen.RepairsList.route) {
            RepairsListScreen(
                onBack = {
                    navController.popBackStack()
                },
                onEditDevice = { id ->
                    navController.navigate(Screen.EditDevice.createRoute(id)) {
                        launchSingleTop = true
                    }
                }
            )
        }
    }
}
