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
import androidx.compose.ui.res.stringResource
import com.example.workapp.R

/**
 * Bottom navigation destinations for the app
 * Follows Material 3 navigation patterns
 */
sealed class BottomNavDestination(
    val route: String,
    val labelRes: Int,
    val contentDescriptionRes: Int
) {
    data class Home(
        val isProfessional: Boolean = false
    ) : BottomNavDestination(
        route = "home",
        labelRes = if (isProfessional) R.string.nav_explore else R.string.nav_search,
        contentDescriptionRes = if (isProfessional) R.string.nav_explore_desc else R.string.nav_search_desc
    )

    object CreateJob : BottomNavDestination(
        route = "create_job",
        labelRes = R.string.nav_create,
        contentDescriptionRes = R.string.nav_create_desc
    )

    data class Listings(
        val isProfessional: Boolean = false
    ) : BottomNavDestination(
        route = "my_jobs",
        labelRes = if (isProfessional) R.string.nav_applied else R.string.nav_posted,
        contentDescriptionRes = if (isProfessional) R.string.nav_applied_desc else R.string.nav_posted_desc
    )

    object Profile : BottomNavDestination(
        route = "profile",
        labelRes = R.string.profile_title,
        contentDescriptionRes = R.string.nav_profile_desc
    )

    object Chat : BottomNavDestination(
        route = "chat",
        labelRes = R.string.nav_chat,
        contentDescriptionRes = R.string.nav_chat_desc
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
    applicationCount: Int = 0,
    professionalUnreadCount: Int = 0
) {
    // Create destinations based on user role
    // IMPORTANT: Professionals should NOT see the Create button
    // Only clients (non-professionals) can create job listings
    val isProfessional = currentUser?.isProfessional() == true
    
    // Explicitly check roleString as additional safety measure
    val roleIsProfessional = currentUser?.roleString == "PROFESSIONAL" || currentUser?.roleString == "CRAFTSMAN"
    val shouldShowCreateButton = currentUser != null && !isProfessional && !roleIsProfessional
    
    val destinations = if (isProfessional || roleIsProfessional) {
        // Professional navigation: Home (Search), Jobs (My Applications), Chat, Profile
        listOf(
            BottomNavDestination.Home(isProfessional = true),
            BottomNavDestination.Listings(isProfessional = true),
            BottomNavDestination.Chat,
            BottomNavDestination.Profile
        )
    } else {
        // Client navigation: Home (Search), Create, My Jobs, Chat, Profile
        listOf(
            BottomNavDestination.Home(isProfessional = false),
            BottomNavDestination.CreateJob,
            BottomNavDestination.Listings(isProfessional = false),
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
                                        contentDescription = stringResource(destination.contentDescriptionRes),
                                        modifier = Modifier.size(IconSizes.medium)
                                    )
                                }
                            } else if (destination is BottomNavDestination.Listings && !destination.isProfessional && applicationCount > 0) {
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
                                        contentDescription = stringResource(destination.contentDescriptionRes),
                                        modifier = Modifier.size(IconSizes.medium)
                                    )
                                }
                            } else if (destination is BottomNavDestination.Listings && destination.isProfessional && professionalUnreadCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge {
                                            Text(text = professionalUnreadCount.toString())
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = AppIcons.getNavigationIcon(
                                            route = destination.route,
                                            selected = selected
                                        ),
                                        contentDescription = stringResource(destination.contentDescriptionRes),
                                        modifier = Modifier.size(IconSizes.medium)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = AppIcons.getNavigationIcon(
                                        route = destination.route,
                                        selected = selected
                                    ),
                                    contentDescription = stringResource(destination.contentDescriptionRes),
                                    modifier = Modifier.size(IconSizes.medium)
                                )
                            }
                        },
                        label = {
                            Text(
                                text = stringResource(destination.labelRes),
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
                            contentDescription = "" // Content description is handled by the Icon
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
        "home", "profile", "create_job", "jobs_list", "my_jobs", "chat", "chat/{chatRoomId}" -> true
        else -> false
    }
}
