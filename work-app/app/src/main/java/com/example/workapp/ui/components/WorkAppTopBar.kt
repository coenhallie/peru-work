package com.example.workapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkAppTopBar(
    title: String,
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    centered: Boolean = true
) {
    val colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
        containerColor = Color.Transparent,
        titleContentColor = MaterialTheme.colorScheme.onBackground,
        navigationIconContentColor = MaterialTheme.colorScheme.onBackground,
        actionIconContentColor = MaterialTheme.colorScheme.onBackground
    )

    if (centered) {
        CenterAlignedTopAppBar(
            title = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    // If subtitle is null, we add a space to maintain vertical alignment consistency
                    // with screens that have two lines (like JobsListScreen)
                    Text(
                        text = subtitle ?: " ",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (subtitle != null) 
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f) 
                        else 
                            Color.Transparent
                    )
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors,
            modifier = modifier
        )
    } else {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                    Text(
                        text = subtitle ?: " ",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (subtitle != null) 
                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f) 
                        else 
                            Color.Transparent
                    )
                }
            },
            navigationIcon = navigationIcon,
            actions = actions,
            colors = colors, // TopAppBarDefaults.topAppBarColors is compatible with centerAligned colors for basic properties
            modifier = modifier
        )
    }
}
