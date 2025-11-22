package com.example.workapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.workapp.navigation.NavGraph
import com.example.workapp.navigation.Screen
import com.example.workapp.ui.components.BottomNavigationBar
import com.example.workapp.ui.components.shouldShowBottomBar
import com.example.workapp.ui.theme.WorkAppTheme
import com.example.workapp.ui.viewmodel.AuthViewModel
import com.example.workapp.ui.viewmodel.ChatViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity for the WorkApp
 * Integrates Material 3 bottom navigation with proper state management
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure edge-to-edge with proper window insets
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        
        setContent {
            WorkAppTheme {
                WorkAppNavHost()
            }
        }
    }
}

/**
 * Main navigation host with integrated bottom navigation bar
 * Implements proper navigation state management following Material 3 patterns
 */
@Composable
fun WorkAppNavHost(
    authViewModel: AuthViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUser by authViewModel.currentUser.collectAsState()
    val chatRooms by chatViewModel.chatRooms.collectAsState()
    
    val totalUnreadCount = androidx.compose.runtime.remember(chatRooms, currentUser) {
        if (currentUser == null) 0
        else chatRooms.sumOf { room ->
            if (currentUser!!.id == room.clientId) room.unreadCountClient
            else room.unreadCountCraftsman
        }
    }
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Show bottom bar only on appropriate screens
            if (shouldShowBottomBar(currentRoute)) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigateToDestination = { destination ->
                        navController.navigate(destination.route) {
                            // Always pop up to Home as the root of the bottom navigation.
                            // This guarantees that when navigating from the Create Job
                            // screen back to Search, the craftsman Home screen becomes
                            // the active content.
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    },
                    currentUser = currentUser,
                    unreadMessageCount = totalUnreadCount
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavGraph(
            navController = navController,
            authViewModel = authViewModel
        )
    }
}
