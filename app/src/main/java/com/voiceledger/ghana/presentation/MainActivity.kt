package com.voiceledger.ghana.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voiceledger.ghana.presentation.analytics.TransactionAnalyticsScreen
import com.voiceledger.ghana.presentation.dashboard.DashboardScreen
import com.voiceledger.ghana.presentation.history.HistoryScreen
import com.voiceledger.ghana.presentation.settings.SettingsScreen
import com.voiceledger.ghana.presentation.summary.SummaryPresentationScreen
import com.voiceledger.ghana.presentation.theme.VoiceLedgerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the Ghana Voice Ledger app
 * Handles navigation and permissions
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioPermissionGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        val notificationPermissionGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: true
        
        if (!audioPermissionGranted) {
            // Handle permission denied - show explanation dialog
            showPermissionExplanationDialog()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check and request permissions
        checkAndRequestPermissions()
        
        setContent {
            VoiceLedgerTheme {
                VoiceLedgerApp()
            }
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Audio recording permission (required)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }
        
        // Notification permission (Android 13+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }
    
    private fun showPermissionExplanationDialog() {
        // This would show a dialog explaining why audio permission is needed
        // For now, we'll just finish the activity
        finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceLedgerApp() {
    val navController = rememberNavController()
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val items = listOf(
                    BottomNavItem("dashboard", "Dashboard", Icons.Default.Dashboard),
                    BottomNavItem("analytics", "Analytics", Icons.Default.Insights),
                    BottomNavItem("history", "History", Icons.Default.History),
                    BottomNavItem("settings", "Settings", Icons.Default.Settings)
                )
                
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") {
                DashboardScreen(
                    onNavigateToHistory = {
                        navController.navigate("history")
                    },
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    },
                    onNavigateToSummary = { date ->
                        navController.navigate("summary_presentation/$date")
                    }
                )
            }
            
            composable("analytics") {
                TransactionAnalyticsScreen(
                    onNavigateToSettings = {
                        navController.navigate("settings")
                    }
                )
            }
            
            composable("history") {
                HistoryScreen(
                    onNavigateToTransactionDetail = { transactionId ->
                        // TODO: Navigate to transaction detail screen
                    }
                )
            }
            
            composable("settings") {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            
            composable(
                "summary_presentation/{date}",
                arguments = listOf(navArgument("date") { type = NavType.StringType })
            ) { backStackEntry ->
                val date = backStackEntry.arguments?.getString("date") ?: ""
                SummaryPresentationScreen(
                    summaryDate = date,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)