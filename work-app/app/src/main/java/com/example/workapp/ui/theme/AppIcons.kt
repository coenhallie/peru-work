package com.example.workapp.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Work
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized icon system for WorkApp
 * All Material Design 3 icons used across the application
 * 
 * Benefits:
 * - Single source of truth for all icons
 * - Easy to update icons app-wide
 * - Type-safe icon references
 * - Prevents duplicate imports
 * - Clear icon variant usage (filled vs outlined)
 */
object AppIcons {
    
    /**
     * Navigation icons used in bottom navigation and app bar
     */
    object Navigation {
        val home = Icons.Filled.Search
        val homeOutlined = Icons.Outlined.Search
        
        val createJob = Icons.Filled.Add
        val createJobOutlined = Icons.Outlined.Add
        
        val myJobs = Icons.Filled.Work
        val myJobsOutlined = Icons.Outlined.Work
        
        val profile = Icons.Filled.Person
        val profileOutlined = Icons.Outlined.Person
        
        val chat = Icons.AutoMirrored.Filled.Chat
        val chatOutlined = Icons.AutoMirrored.Outlined.Chat
        
        val back = Icons.AutoMirrored.Filled.ArrowBack
    }
    
    /**
     * Action icons - user interactions and commands
     */
    object Actions {
        val add = Icons.Filled.Add
        val edit = Icons.Filled.Edit
        val delete = Icons.Filled.Delete
        val logout = Icons.AutoMirrored.Filled.Logout
        val search = Icons.Filled.Search
        val filter = Icons.Filled.FilterList
        val close = Icons.Filled.Close
        val settings = Icons.Filled.Settings
        val camera = Icons.Filled.CameraAlt
        val save = Icons.Filled.Save
        val send = Icons.AutoMirrored.Filled.Send
    }
    
    /**
     * Content icons - information display
     */
    object Content {
        val email = Icons.Filled.Email
        val phone = Icons.Filled.Phone
        val location = Icons.Filled.LocationOn
        val place = Icons.Filled.Place
        val work = Icons.Filled.Work
        val star = Icons.Filled.Star
        val person = Icons.Filled.Person
        val build = Icons.Filled.Build
        val language = Icons.Filled.Language
        val description = Icons.Filled.Description
        val payment = Icons.Filled.AttachMoney
        val schedule = Icons.Filled.Schedule
        val calendar = Icons.Filled.CalendarToday
    }
    
    /**
     * Form icons - input fields and authentication
     */
    object Form {
        val email = Icons.Filled.Email
        val lock = Icons.Filled.Lock
        val person = Icons.Filled.Person
        val phone = Icons.Filled.Phone
        val visibility = Icons.Filled.Visibility
        val visibilityOff = Icons.Filled.VisibilityOff
        val location = Icons.Filled.Place
        val work = Icons.Filled.Work
    }
    
    /**
     * Get navigation icon based on route and selection state
     * 
     * @param route The navigation destination route
     * @param selected Whether the destination is currently selected
     * @return The appropriate icon variant (filled for selected, outlined for unselected)
     */
    fun getNavigationIcon(route: String, selected: Boolean): ImageVector {
        return when (route) {
            "home" -> if (selected) Navigation.home else Navigation.homeOutlined
            "create_job" -> if (selected) Navigation.createJob else Navigation.createJobOutlined
            "my_jobs" -> if (selected) Navigation.myJobs else Navigation.myJobsOutlined
            "profile" -> if (selected) Navigation.profile else Navigation.profileOutlined
            "chat" -> if (selected) Navigation.chat else Navigation.chatOutlined
            else -> Navigation.home
        }
    }
}

/**
 * Standard icon sizes following Material Design 3 guidelines
 * 
 * Use these constants instead of hardcoding dp values throughout the app
 * This ensures consistency and makes it easy to adjust sizes app-wide
 */
object IconSizes {
    /**
     * Small icons - typically used inline with text
     */
    val small: Dp = 18.dp
    
    /**
     * Medium icons - the standard size for most UI icons
     * This is the default Material Design icon size
     */
    val medium: Dp = 24.dp
    
    /**
     * Large icons - for prominent actions or headers
     */
    val large: Dp = 36.dp
    
    /**
     * Extra large icons - for large touch targets or decorative icons
     */
    val extraLarge: Dp = 48.dp
}