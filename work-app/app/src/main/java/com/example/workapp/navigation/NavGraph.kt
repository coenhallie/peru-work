package com.example.workapp.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.workapp.ui.screens.chat.ChatListScreen
import com.example.workapp.ui.screens.chat.ChatScreen
import com.example.workapp.ui.screens.professional.ProfessionalDetailScreen
import com.example.workapp.ui.screens.home.HomeScreen
import com.example.workapp.ui.screens.jobs.ApplicationsListScreen
import com.example.workapp.ui.screens.jobs.CreateJobScreen
import com.example.workapp.ui.screens.jobs.EditJobScreen
import com.example.workapp.ui.screens.jobs.JobDetailScreen
import com.example.workapp.ui.screens.jobs.JobsListScreen
import com.example.workapp.ui.screens.jobs.MyApplicationsScreen
import com.example.workapp.ui.screens.profile.EditProfileScreen
import com.example.workapp.ui.screens.profile.ProfileScreen
import com.example.workapp.ui.screens.welcome.WelcomeScreen
import com.example.workapp.ui.viewmodel.AuthState
import com.example.workapp.ui.viewmodel.AuthViewModel

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Home : Screen("home")
    object CreateJob : Screen("create_job")
    object JobsList : Screen("jobs_list")
    object MyJobs : Screen("my_jobs")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object ChatList : Screen("chat")
    object ChatRoom : Screen("chat/{chatRoomId}") {
        fun createRoute(chatRoomId: String) = "chat/$chatRoomId"
    }
    object ProfessionalDetail : Screen("professional/{professionalId}") {
        fun createRoute(professionalId: String) = "professional/$professionalId"
    }
    object EditJob : Screen("edit_job/{jobId}") {
        fun createRoute(jobId: String) = "edit_job/$jobId"
    }
    object JobDetail : Screen("job_detail/{jobId}?chatRoomId={chatRoomId}") {
        fun createRoute(jobId: String, chatRoomId: String? = null) = 
            if (chatRoomId != null) "job_detail/$jobId?chatRoomId=$chatRoomId" else "job_detail/$jobId"
    }
    object ApplicationsList : Screen("applications/{jobId}") {
        fun createRoute(jobId: String) = "applications/$jobId"
    }
    object MyApplications : Screen("my_applications")
}

/**
 * Main navigation graph
 */
@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel(),
    paddingValues: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues(0.dp),
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    
    // Track if this is the initial load - we fade in once auth state is determined
    var isInitialLoad by remember { mutableStateOf(true) }
    var showContent by remember { mutableStateOf(false) }
    
    // Determine start destination based on auth state
    val startDestination = when (authState) {
        is AuthState.Authenticated -> Screen.Home.route
        is AuthState.Initial, is AuthState.WaitingForBiometric -> Screen.Home.route // Placeholder
        else -> Screen.Welcome.route
    }
    
    // Show content with fade-in once auth state is determined (not Initial or WaitingForBiometric)
    LaunchedEffect(authState) {
        if (authState !is AuthState.Initial && authState !is AuthState.WaitingForBiometric) {
            showContent = true
            isInitialLoad = false
        }
    }
    
    // Show a blank background while auth state is being determined or waiting for biometric
    if (authState is AuthState.Initial || authState is AuthState.WaitingForBiometric) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            // Optionally show a loading indicator here
            // For now, just show the background color for a smooth transition
        }
        return
    }

    // Define bottom navigation routes
    val bottomNavRoutes = listOf(
        Screen.Home.route,
        Screen.JobsList.route,
        Screen.MyJobs.route,
        Screen.Profile.route,
        Screen.CreateJob.route,
        Screen.CreateJob.route,
        Screen.MyApplications.route,
        Screen.ChatList.route
    )

    // Fade in the entire NavHost for smooth initial appearance
    AnimatedVisibility(
        visible = showContent,
        enter = fadeIn(animationSpec = tween(300))
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier,
            enterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                
                // Use fade for:
                // 1. Initial app launch (no previous route or same as target - initial composition)
                // 2. Navigation between bottom nav routes
                val isInitialNavigation = initialRoute == null ||
                    initialRoute == targetRoute ||
                    initialRoute == startDestination && targetRoute == startDestination
                val isBetweenBottomNavRoutes = initialRoute in bottomNavRoutes && targetRoute in bottomNavRoutes
                
                if (isInitialNavigation || isBetweenBottomNavRoutes) {
                    fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { 1000 },
                        animationSpec = tween(300)
                    )
                }
            },
            exitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                
                val isInitialNavigation = initialRoute == null ||
                    initialRoute == targetRoute
                val isBetweenBottomNavRoutes = initialRoute in bottomNavRoutes && targetRoute in bottomNavRoutes
                
                if (isInitialNavigation || isBetweenBottomNavRoutes) {
                    fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { -1000 },
                        animationSpec = tween(300)
                    )
                }
            },
            popEnterTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                
                val isBetweenBottomNavRoutes = initialRoute in bottomNavRoutes && targetRoute in bottomNavRoutes
                
                if (isBetweenBottomNavRoutes) {
                    fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -1000 },
                        animationSpec = tween(300)
                    )
                }
            },
            popExitTransition = {
                val initialRoute = initialState.destination.route
                val targetRoute = targetState.destination.route
                
                val isBetweenBottomNavRoutes = initialRoute in bottomNavRoutes && targetRoute in bottomNavRoutes
                
                if (isBetweenBottomNavRoutes) {
                    fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { 1000 },
                        animationSpec = tween(300)
                    )
                }
            }
        ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                viewModel = authViewModel,
                onAuthSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val currentUser = (authState as? AuthState.Authenticated)?.user
            val isProfessional = currentUser?.isProfessional() == true
            
            if (isProfessional) {
                // For professionals, Home is the "Available Jobs" page
                JobsListScreen(
                    onJobClick = { jobId ->
                        navController.navigate(Screen.JobDetail.createRoute(jobId))
                    },
                    onEditJob = { jobId ->
                        navController.navigate(Screen.EditJob.createRoute(jobId))
                    },
                    onViewApplications = { jobId ->
                        navController.navigate(Screen.ApplicationsList.createRoute(jobId))
                    },
                    currentUserId = currentUser?.id,
                    showMyJobs = false,
                    showAvailableJobs = true,
                    isProfessional = true
                )
            } else {
                // For clients, Home is the "Find a professional" page
                HomeScreen(
                    authViewModel = authViewModel,
                    onProfessionalClick = { professionalId ->
                        navController.navigate(Screen.ProfessionalDetail.createRoute(professionalId))
                    },
                    onJobClick = { jobId ->
                        navController.navigate(Screen.JobDetail.createRoute(jobId))
                    }
                )
            }
        }

        composable(Screen.CreateJob.route) {
            CreateJobScreen(
                onJobCreated = {
                    navController.navigate(Screen.MyJobs.route) {
                        popUpTo(Screen.CreateJob.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.JobsList.route) {
            val currentUser = (authState as? AuthState.Authenticated)?.user
            val isProfessional = currentUser?.isProfessional() == true
            
            JobsListScreen(
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                },
                onEditJob = { jobId ->
                    navController.navigate(Screen.EditJob.createRoute(jobId))
                },
                onViewApplications = { jobId ->
                    navController.navigate(Screen.ApplicationsList.createRoute(jobId))
                },
                currentUserId = currentUser?.id,
                showMyJobs = false,
                isProfessional = isProfessional
            )
        }
        
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onNavigateToChat = { chatRoomId ->
                    navController.navigate(Screen.ChatRoom.createRoute(chatRoomId))
                },
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        navController.navigate(Screen.Home.route)
                    }
                },
                authViewModel = authViewModel
            )
        }
        
        composable(
            route = Screen.ChatRoom.route,
            arguments = listOf(
                navArgument("chatRoomId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: return@composable
            ChatScreen(
                chatRoomId = chatRoomId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToJob = { jobId, chatRoomId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId, chatRoomId))
                },
                authViewModel = authViewModel,
                modifier = Modifier.padding(bottom = paddingValues.calculateBottomPadding())
            )
        }

        composable(Screen.MyJobs.route) {
            val currentUser = (authState as? AuthState.Authenticated)?.user
            val isProfessional = currentUser?.isProfessional() == true
            
            // For professionals: Show their applications with status
            // For regular users: Show jobs they posted
            JobsListScreen(
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                },
                onEditJob = { jobId ->
                    navController.navigate(Screen.EditJob.createRoute(jobId))
                },
                onViewApplications = { jobId ->
                    navController.navigate(Screen.ApplicationsList.createRoute(jobId))
                },
                currentUserId = currentUser?.id,
                showMyJobs = !isProfessional,  // Only show "my posted jobs" for non-professionals
                isProfessional = isProfessional
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.EditJob.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            EditJobScreen(
                jobId = jobId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.ProfessionalDetail.route,
            arguments = listOf(
                navArgument("professionalId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val professionalId = backStackEntry.arguments?.getString("professionalId") ?: return@composable
            ProfessionalDetailScreen(
                professionalId = professionalId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToChat = { chatRoomId ->
                    navController.navigate(Screen.ChatRoom.createRoute(chatRoomId))
                },
                authViewModel = authViewModel
            )
        }

        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType },
                navArgument("chatRoomId") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            val chatRoomId = backStackEntry.arguments?.getString("chatRoomId")
            val currentUser = (authState as? AuthState.Authenticated)?.user
            JobDetailScreen(
                jobId = jobId,
                currentUserId = currentUser?.id,
                isProfessional = currentUser?.isProfessional() == true,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToApplications = { jobId ->
                    navController.navigate(Screen.ApplicationsList.createRoute(jobId))
                },
                onEditJob = { jobId ->
                    navController.navigate(Screen.EditJob.createRoute(jobId))
                },
                chatRoomId = chatRoomId
            )
        }

        composable(
            route = Screen.ApplicationsList.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            ApplicationsListScreen(
                jobId = jobId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.MyApplications.route) {
            MyApplicationsScreen(
                onNavigateBack = { navController.popBackStack() },
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                }
            )
        }
        }
    }
}