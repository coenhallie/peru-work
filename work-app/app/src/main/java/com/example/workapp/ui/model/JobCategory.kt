package com.example.workapp.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.ElectricalServices
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.HomeRepairService
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

data class JobCategory(
    val name: String,
    val icon: ImageVector
)

val jobCategories = listOf(
    JobCategory("Construcción / Albañilería", Icons.Filled.Construction),
    JobCategory("Pintura", Icons.Filled.Brush),
    JobCategory("Gasfitería", Icons.Filled.Plumbing),
    JobCategory("Electricidad", Icons.Filled.ElectricalServices),
    JobCategory("Carpintería", Icons.Filled.HomeRepairService),
    JobCategory("Limpieza", Icons.Filled.CleaningServices),
    JobCategory("Artesanía", Icons.Filled.Palette),
    JobCategory("Ventas", Icons.Filled.ShoppingBag),
    JobCategory("Jardinería", Icons.Filled.Grass),
    JobCategory("Mecánica", Icons.Filled.Build),
    JobCategory("Costura / Textiles", Icons.Filled.Sell),
    JobCategory("Otros", Icons.Filled.MoreHoriz)
)
