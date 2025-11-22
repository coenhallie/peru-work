package com.example.workapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.workapp.data.model.User
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes

/**
 * Bottom navigation destinations for the app
 * Follows Material 3 navigation patterns
 */
sealed class BottomNavDestination(
    val route: String,
    val label: String,
    val contentDescription: String
) {
    object Home : BottomNavDestination(
        route = "home",
        label = "Search",
        contentDescription = "Search for craftsmen"
    )

    object CreateJob : BottomNavDestination(
        route = "create_job",
        label = "Create",
        contentDescription = "Create a job listing"
    )

    data class Listings(
        val isCraftsman: Boolean = false
    ) : BottomNavDestination(
        route = "my_jobs",
        label = if (isCraftsman) "Jobs" else "My Jobs",
        contentDescription = if (isCraftsman) "Browse available jobs" else "View and manage your job listings"
    )

    object Profile : BottomNavDestination(
        route = "profile",
        label = "Profile",
        contentDescription = "View your profile"
    )

    object Chat : BottomNavDestination(
        route = "chat",
        label = "Chat",
        contentDescription = "View your messages"
    )
}

/**
 * Material 3 Bottom Navigation Bar with integrated FAB
 * 
 * Design Decisions:
 * - Uses NavigationBar (Material 3 component) instead of BottomNavigation (Material 2)
 * - Centered FAB for primary action (Create Job) following Material You guidelines
 * - Implements proper touch targets (48dp minimum) for accessibility
 * - Uses dynamic color theming from Material Theme
 * - Includes semantic labels for screen readers
 * - Proper state indicators (selected/unselected) with icon changes
 * - Elevation follows Material 3 spec (3dp for navigation bar, 6dp for FAB)
 * - Ripple effects are automatically handled by Material 3 components
 * 
 * @param currentRoute The currently selected route
 * @param onNavigateToDestination Callback when a destination is selected
 * @param modifier Optional modifier for the container
 * @param visible Controls visibility with animation
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigateToDestination: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    visible: Boolean = true,
    currentUser: User? = null,
    unreadMessageCount: Int = 0,
    applicationCount: Int = 0
) {
    // Create destinations based on user role
    // IMPORTANT: Craftsmen should NOT see the Create button
    // Only clients (non-craftsmen) can create job listings
    val isCraftsman = currentUser?.isCraftsman() == true
    
    // Explicitly check roleString as additional safety measure
    val roleIsCraftsman = currentUser?.roleString == "CRAFTSMAN"
    val shouldShowCreateButton = currentUser != null && !isCraftsman && !roleIsCraftsman
    
    val destinations = if (isCraftsman || roleIsCraftsman) {
        // Craftsman navigation: Home (Search), Jobs (My Applications), Chat, Profile
        listOf(
            BottomNavDestination.Home,
            BottomNavDestination.Listings(isCraftsman = true),
            BottomNavDestination.Chat,
            BottomNavDestination.Profile
        )
    } else {
        // Client navigation: Home (Search), Create, My Jobs, Chat, Profile
        listOf(
            BottomNavDestination.Home,
            BottomNavDestination.CreateJob,
            BottomNavDestination.Listings(isCraftsman = false),
            BottomNavDestination.Chat,
            BottomNavDestination.Profile
        )
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                destinations.forEach { destination ->
                    val selected = currentRoute == destination.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = { onNavigateToDestination(destination) },
                        icon = {
                            if (destination is BottomNavDestination.Chat && unreadMessageCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(text = unreadMessageCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = AppIcons.getNavigationIcon(
                                            route = destination.route,
                                            selected = selected
                                        ),
                                        contentDescription = destination.contentDescription,
                                        modifier = Modifier.size(IconSizes.medium)
                                    )
                                }
                            } else if (destination is BottomNavDestination.Listings && !destination.isCraftsman && applicationCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(text = applicationCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = AppIcons.getNavigationIcon(
                                            route = destination.route,
                                            selected = selected
                                        ),
                                        contentDescription = destination.contentDescription,
                                        modifier = Modifier.size(IconSizes.medium)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = AppIcons.getNavigationIcon(
                                        route = destination.route,
                                        selected = selected
                                    ),
                                    contentDescription = destination.contentDescription,
                                    modifier = Modifier.size(IconSizes.medium)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = destination.label,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.semantics {
                            contentDescription = destination.contentDescription
                        }
                    )
                }
            }
        }
    }
}

/**
 * Helper function to determine if bottom navigation should be shown
 * Based on Material 3 guidelines, bottom nav should be hidden on certain screens
 * like authentication, welcome, or detail screens
 */
fun shouldShowBottomBar(currentRoute: String?): Boolean {
    return when (currentRoute) {
        "home", "profile", "create_job", "jobs_list", "my_jobs" -> true
        else -> false
    }
}
