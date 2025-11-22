package com.example.workapp.ui.screens.home

import com.example.workapp.ui.model.JobCategory
import com.example.workapp.ui.model.jobCategories
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.workapp.data.model.Job
import com.example.workapp.data.model.User
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes
import com.example.workapp.ui.theme.StarYellow
import com.example.workapp.ui.viewmodel.AuthViewModel
import com.example.workapp.ui.viewmodel.CraftsmenUiState
import com.example.workapp.ui.viewmodel.CraftsmenViewModel
import com.example.workapp.ui.viewmodel.JobViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Home screen with role-based content display
 * - Regular users: See list of craftsmen
 * - Craftsmen: See recent job listings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel = hiltViewModel(),
    craftsmenViewModel: CraftsmenViewModel = hiltViewModel(),
    jobViewModel: JobViewModel = hiltViewModel(),
    onCraftsmanClick: (String) -> Unit,
    onJobClick: (String) -> Unit = {}
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val isCraftsman = currentUser?.isCraftsman() == true
    
    // Craftsmen view states
    val craftsmenUiState by craftsmenViewModel.uiState.collectAsState()
    val searchQuery by craftsmenViewModel.searchQuery.collectAsState()
    val selectedCategory by craftsmenViewModel.selectedCategory.collectAsState()
    val maxDistance by craftsmenViewModel.maxDistance.collectAsState()
    val categories by craftsmenViewModel.categories.collectAsState()
    
    // Jobs view states (for craftsmen)
    val openJobs by jobViewModel.openJobs.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isCraftsman) "Recent Jobs" else "Find a craftsman",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (isCraftsman)
                                "Latest opportunities for you"
                            else
                                "Hand-picked professionals near you",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isCraftsman) {
            // Show recent jobs for craftsmen
            CraftsmenHomeContent(
                jobs = openJobs,
                padding = padding,
                onJobClick = onJobClick
            )
        } else {
            // Show craftsmen list for regular users
            RegularUserHomeContent(
                uiState = craftsmenUiState,
                searchQuery = searchQuery,
                selectedCategory = selectedCategory,
                maxDistance = maxDistance,
                categories = categories,
                padding = padding,
                onSearchQueryChange = { craftsmenViewModel.searchCraftsmen(it) },
                onClearSearch = { craftsmenViewModel.clearSearch() },
                onCategorySelected = { craftsmenViewModel.filterByCategory(it) },
                onDistanceSelected = { craftsmenViewModel.filterByDistance(it) },
                onClearFilters = { craftsmenViewModel.clearFilters() },
                onCraftsmanClick = onCraftsmanClick
            )
        }
    }
}

/**
 * Home content for regular users showing craftsmen list
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegularUserHomeContent(
    uiState: CraftsmenUiState,
    searchQuery: String,
    selectedCategory: String?,
    maxDistance: Double?,
    categories: List<String>,
    padding: PaddingValues,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    onCategorySelected: (String?) -> Unit,
    onDistanceSelected: (Double?) -> Unit,
    onClearFilters: () -> Unit,
    onCraftsmanClick: (String) -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search craftsmen...") },
            leadingIcon = {
                Icon(
                    imageVector = AppIcons.Actions.search,
                    contentDescription = null,
                    modifier = Modifier.size(IconSizes.medium)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = onClearSearch) {
                        Icon(
                            imageVector = AppIcons.Actions.close,
                            contentDescription = "Clear search",
                            modifier = Modifier.size(IconSizes.medium)
                        )
                    }
                } else {
                    IconButton(onClick = { showFilterSheet = true }) {
                        Icon(
                            imageVector = AppIcons.Actions.filter,
                            contentDescription = "Filters",
                            modifier = Modifier.size(IconSizes.medium),
                            tint = if (selectedCategory != null || maxDistance != null) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            singleLine = true
        )

        // Content with Pull to Refresh
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    onClearFilters()
                    onSearchQueryChange("")
                    delay(500)
                    isRefreshing = false
                }
            },
            state = pullToRefreshState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = isRefreshing,
                    modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            modifier = Modifier.fillMaxSize()
        ) {
            when (uiState) {
                is CraftsmenUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is CraftsmenUiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No craftsmen found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            if (selectedCategory != null || maxDistance != null) {
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = onClearFilters) {
                                    Text("Clear Filters")
                                }
                            }
                        }
                    }
                }
                is CraftsmenUiState.Success -> {
                    val craftsmen = uiState.craftsmen
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(craftsmen) { craftsman ->
                            CraftsmanCard(
                                craftsman = craftsman,
                                onClick = { onCraftsmanClick(craftsman.id) }
                            )
                        }
                    }
                }
                is CraftsmenUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.message,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        FilterBottomSheet(
            categories = categories,
            selectedCategory = selectedCategory,
            currentDistance = maxDistance,
            onCategorySelected = onCategorySelected,
            onDistanceSelected = onDistanceSelected,
            onDismiss = { showFilterSheet = false },
            onClear = {
                onClearFilters()
                showFilterSheet = false
            },
            sheetState = sheetState
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    categories: List<String>, // We can ignore this or use it to filter if needed, but for now we use the shared list
    selectedCategory: String?,
    currentDistance: Double?,
    onCategorySelected: (String?) -> Unit,
    onDistanceSelected: (Double?) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    sheetState: SheetState
) {
    var tempCategory by remember { mutableStateOf(selectedCategory) }
    var tempDistance by remember { mutableStateOf(currentDistance ?: 50.0) } // Default 50km if null
    var isDistanceEnabled by remember { mutableStateOf(currentDistance != null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClear) {
                    Text("Clear All")
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // Distance Filter
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Distance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Switch(
                                checked = isDistanceEnabled,
                                onCheckedChange = { isDistanceEnabled = it }
                            )
                        }
                        
                        if (isDistanceEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Within ${tempDistance.toInt()} km",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Slider(
                                value = tempDistance.toFloat(),
                                onValueChange = { tempDistance = it.toDouble() },
                                valueRange = 1f..100f,
                                steps = 99
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Category Filter Header
                        Text(
                            text = "Job Category",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // All Categories Option
                item {
                    CategoryFilterItem(
                        name = "All Categories",
                        icon = AppIcons.Content.work, // Generic icon
                        isSelected = tempCategory == null,
                        onClick = { tempCategory = null }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                // Dynamic Categories from shared list
                items(jobCategories) { category ->
                    CategoryFilterItem(
                        name = category.name,
                        icon = category.icon,
                        isSelected = tempCategory == category.name,
                        onClick = { tempCategory = category.name }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Apply Button
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Button(
                            onClick = {
                                onCategorySelected(tempCategory)
                                onDistanceSelected(if (isDistanceEnabled) tempDistance else null)
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large
                        ) {
                            Text("Apply Filters")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilterItem(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(IconSizes.medium)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
        
        if (isSelected) {
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(IconSizes.small)
            )
        }
    }
}

/**
 * Home content for craftsmen showing recent jobs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CraftsmenHomeContent(
    jobs: List<Job>,
    padding: PaddingValues,
    onJobClick: (String) -> Unit
) {
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                delay(500)
                isRefreshing = false
            }
        },
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                color = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        if (jobs.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Content.work,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .size(IconSizes.large),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "No jobs available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "Check back later for new opportunities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        } else {
            // Jobs list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(jobs) { job ->
                    RecentJobCard(
                        job = job,
                        onClick = { onJobClick(job.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CraftsmanCard(
    craftsman: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            AsyncImage(
                model = craftsman.profileImageUrl ?: "https://via.placeholder.com/150",
                contentDescription = "${craftsman.name} profile",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = craftsman.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = craftsman.craft ?: "Craftsman",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Content.star,
                        contentDescription = "Rating",
                        tint = StarYellow,
                        modifier = Modifier.size(IconSizes.small)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${craftsman.rating ?: 0.0}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(${craftsman.reviewCount ?: 0} reviews)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (craftsman.experience != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${craftsman.experience} years experience",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Card displaying recent job for craftsmen home screen
 */
@Composable
private fun RecentJobCard(
    job: Job,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Job Image (smaller and on the left)
            com.example.workapp.ui.components.JobImage(
                imageUrl = job.imageUrl,
                category = job.category,
                modifier = Modifier
                    .size(100.dp)
                    .clip(MaterialTheme.shapes.small)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Information on the right
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Job title
                Text(
                    text = job.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Category and Location
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.Content.work,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(IconSizes.small),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = job.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }

                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description preview
                Text(
                    text = job.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Budget and Posted date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    job.budget?.let { budget ->
                        Text(
                            text = "PEN ${String.format("%.0f", budget)}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = formatDate(job.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * Format timestamp to relative date string
 */
private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}