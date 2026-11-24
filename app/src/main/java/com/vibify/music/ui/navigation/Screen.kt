package com.vibify.music.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Search : Screen(
        route = "search",
        title = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )

    object Podcasts : Screen(
        route = "podcasts",
        title = "Podcasts",
        selectedIcon = Icons.Filled.Podcasts,
        unselectedIcon = Icons.Outlined.Podcasts
    )

    object Kahani : Screen(
        route = "kahani",
        title = "Kahani",
        selectedIcon = Icons.AutoMirrored.Filled.MenuBook,
        unselectedIcon = Icons.AutoMirrored.Outlined.MenuBook
    )

    object Library : Screen(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.LibraryMusic,
        unselectedIcon = Icons.Outlined.LibraryMusic
    )

    object Queue : Screen(
        route = "queue",
        title = "Queue",
        selectedIcon = Icons.Filled.QueueMusic,
        unselectedIcon = Icons.Outlined.QueueMusic
    )
}
