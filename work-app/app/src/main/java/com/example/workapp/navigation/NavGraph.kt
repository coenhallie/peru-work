package com.example.workapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.workapp.ui.screens.chat.ChatListScreen
import com.example.workapp.ui.screens.chat.ChatScreen
import com.example.workapp.ui.screens.craftsman.CraftsmanDetailScreen
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
    object CraftsmanDetail : Screen("craftsman/{craftsmanId}") {
        fun createRoute(craftsmanId: String) = "craftsman/$craftsmanId"
    }
    object EditJob : Screen("edit_job/{jobId}") {
        fun createRoute(jobId: String) = "edit_job/$jobId"
    }
    object JobDetail : Screen("job_detail/{jobId}") {
        fun createRoute(jobId: String) = "job_detail/$jobId"
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
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()

    // Determine start destination based on auth state
    val startDestination = when (authState) {
        is AuthState.Authenticated -> Screen.Home.route
        else -> Screen.Welcome.route
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

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            if (initialState.destination.route in bottomNavRoutes && targetState.destination.route in bottomNavRoutes) {
                fadeIn(animationSpec = tween(300))
            } else {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                )
            }
        },
        exitTransition = {
            if (initialState.destination.route in bottomNavRoutes && targetState.destination.route in bottomNavRoutes) {
                fadeOut(animationSpec = tween(300))
            } else {
                slideOutHorizontally(
                    targetOffsetX = { -1000 },
                    animationSpec = tween(300)
                )
            }
        },
        popEnterTransition = {
            if (initialState.destination.route in bottomNavRoutes && targetState.destination.route in bottomNavRoutes) {
                fadeIn(animationSpec = tween(300))
            } else {
                slideInHorizontally(
                    initialOffsetX = { -1000 },
                    animationSpec = tween(300)
                )
            }
        },
        popExitTransition = {
            if (initialState.destination.route in bottomNavRoutes && targetState.destination.route in bottomNavRoutes) {
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
            HomeScreen(
                authViewModel = authViewModel,
                onCraftsmanClick = { craftsmanId ->
                    navController.navigate(Screen.CraftsmanDetail.createRoute(craftsmanId))
                },
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                }
            )
        }

        composable(Screen.CreateJob.route) {
            CreateJobScreen(
                onJobCreated = {
                    navController.navigate(Screen.JobsList.route) {
                        popUpTo(Screen.CreateJob.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.JobsList.route) {
            val currentUser = (authState as? AuthState.Authenticated)?.user
            val isCraftsman = currentUser?.isCraftsman() == true
            
            JobsListScreen(
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                },
                onEditJob = { jobId ->
                    navController.navigate(Screen.EditJob.createRoute(jobId))
                },
                currentUserId = currentUser?.id,
                showMyJobs = false,
                isCraftsman = isCraftsman
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
                authViewModel = authViewModel
            )
        }

        composable(Screen.MyJobs.route) {
            val currentUser = (authState as? AuthState.Authenticated)?.user
            val isCraftsman = currentUser?.isCraftsman() == true
            
            // For craftsmen: Show their applications with status
            // For regular users: Show jobs they posted
            JobsListScreen(
                onJobClick = { jobId ->
                    navController.navigate(Screen.JobDetail.createRoute(jobId))
                },
                onEditJob = { jobId ->
                    navController.navigate(Screen.EditJob.createRoute(jobId))
                },
                currentUserId = currentUser?.id,
                showMyJobs = !isCraftsman,  // Only show "my posted jobs" for non-craftsmen
                isCraftsman = isCraftsman
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
            route = Screen.CraftsmanDetail.route,
            arguments = listOf(
                navArgument("craftsmanId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val craftsmanId = backStackEntry.arguments?.getString("craftsmanId") ?: return@composable
            CraftsmanDetailScreen(
                craftsmanId = craftsmanId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.JobDetail.route,
            arguments = listOf(
                navArgument("jobId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val jobId = backStackEntry.arguments?.getString("jobId") ?: return@composable
            val currentUser = (authState as? AuthState.Authenticated)?.user
            JobDetailScreen(
                jobId = jobId,
                currentUserId = currentUser?.id,
                isCraftsman = currentUser?.isCraftsman() == true,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToApplications = { jobId ->
                    navController.navigate(Screen.ApplicationsList.createRoute(jobId))
                }
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