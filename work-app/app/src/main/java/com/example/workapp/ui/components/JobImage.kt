package com.example.workapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.workapp.ui.theme.AppIcons
import com.example.workapp.ui.theme.IconSizes

/**
 * Reusable component for displaying job images with consistent fallback logic and styling.
 * Handles loading states, errors, and category-based mock images.
 */
@Composable
fun JobImage(
    imageUrl: String?,
    category: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val displayImage = if (!imageUrl.isNullOrEmpty()) imageUrl else getMockImageForCategory(category)
    
    // Create a placeholder painter using the work icon
    Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(displayImage)
                .crossfade(true)
                .build(),
            contentDescription = "Job Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale
        )
    }
}

/**
 * Get a mock image based on category (fallback)
 */
private fun getMockImageForCategory(category: String): String {
    return when {
        category.contains("Plumb", ignoreCase = true) -> "https://images.unsplash.com/photo-1581244277943-fe4a9c777189?q=80&w=1000&auto=format&fit=crop"
        category.contains("Electr", ignoreCase = true) -> "https://images.unsplash.com/photo-1621905251189-08b45d6a269e?q=80&w=1000&auto=format&fit=crop"
        category.contains("Construct", ignoreCase = true) -> "https://images.unsplash.com/photo-1581578731117-104f2a863cc2?q=80&w=1000&auto=format&fit=crop"
        category.contains("Weld", ignoreCase = true) -> "https://images.unsplash.com/photo-1504328345606-18bbc8c9d7d1?q=80&w=1000&auto=format&fit=crop"
        category.contains("Carpent", ignoreCase = true) -> "https://images.unsplash.com/photo-1599696847727-920005c5090a?q=80&w=1000&auto=format&fit=crop"
        category.contains("Paint", ignoreCase = true) -> "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?q=80&w=1000&auto=format&fit=crop"
        category.contains("Garden", ignoreCase = true) -> "https://images.unsplash.com/photo-1558904541-efa843a96f01?q=80&w=1000&auto=format&fit=crop"
        else -> "https://images.unsplash.com/photo-1581578731117-104f2a863cc2?q=80&w=1000&auto=format&fit=crop" // Default construction image
    }
}