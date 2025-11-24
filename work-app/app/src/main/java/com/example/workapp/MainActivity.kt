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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.example.workapp.ui.viewmodel.JobViewModel
import dagger.hilt.android.AndroidEntryPoint
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.android.gms.tasks.OnCompleteListener

/**
 * Main activity for the WorkApp
 * Integrates Material 3 bottom navigation with proper state management
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private var notificationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure edge-to-edge with proper window insets
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()

        // Check for notification intent on launch
        notificationRoute = getNotificationRouteFromIntent(intent)
        
        setContent {
            WorkAppTheme {
                WorkAppNavHost(
                    notificationRoute = notificationRoute,
                    onNotificationHandled = { notificationRoute = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // Handle notification intent when app is already running
        val route = getNotificationRouteFromIntent(intent)
        if (route != null) {
            notificationRoute = route
        }
    }

    private fun getNotificationRouteFromIntent(intent: android.content.Intent?): String? {
        intent?.extras?.let { extras ->
            val action = extras.getString("action")
            if (action == "JOB_APPLICATION") {
                val jobId = extras.getString("jobId")
                if (jobId != null) {
                    return Screen.ApplicationsList.createRoute(jobId)
                }
            }
            if (action == "NEW_MESSAGE") {
                val chatId = extras.getString("chatId")
                if (chatId != null && chatId.isNotEmpty()) {
                    return Screen.ChatRoom.createRoute(chatId)
                }
            }
        }
        return null
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // FCM SDK (and your app) can post notifications.
            Log.d(TAG, "Notification permission granted")
        } else {
            // TODO: Inform user that that your app will not show notifications.
            Log.w(TAG, "Notification permission denied")
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // TODO: Display an educational UI explaining to the user the features that will be enabled
                //       by them granting the POST_NOTIFICATION permission. This UI should provide the user
                //       "OK" and "No thanks" buttons. If the user selects "OK," directly request the permission.
                //       If the user selects "No thanks," allow the user to continue without notifications.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        askNotificationPermission()
        
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@OnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result

            // Log and toast
            Log.d(TAG, "FCM Token: $token")
        })
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

/**
 * Main navigation host with integrated bottom navigation bar
 * Implements proper navigation state management following Material 3 patterns
 */
@Composable
fun WorkAppNavHost(
    authViewModel: AuthViewModel = hiltViewModel(),
    chatViewModel: ChatViewModel = hiltViewModel(),
    jobViewModel: JobViewModel = hiltViewModel(),
    notificationRoute: String? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentUser by authViewModel.currentUser.collectAsState()
    val chatRooms by chatViewModel.chatRooms.collectAsState()
    val totalApplicationCount by jobViewModel.totalApplicationCount.collectAsState()
    
    val totalUnreadCount = androidx.compose.runtime.remember(chatRooms, currentUser) {
        if (currentUser == null) 0
        else chatRooms.sumOf { room ->
            if (currentUser!!.id == room.clientId) room.unreadCountClient
            else room.unreadCountProfessional
        }
    }

    // Sync FCM token when user logs in
    androidx.compose.runtime.LaunchedEffect(currentUser) {
        if (currentUser != null) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    authViewModel.updateFCMToken(token)
                }
            }
        }
    }

    // Handle notification navigation
    androidx.compose.runtime.LaunchedEffect(notificationRoute, currentUser) {
        if (notificationRoute != null && currentUser != null) {
            navController.navigate(notificationRoute)
            onNotificationHandled()
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
                    unreadMessageCount = totalUnreadCount,
                    applicationCount = totalApplicationCount
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
