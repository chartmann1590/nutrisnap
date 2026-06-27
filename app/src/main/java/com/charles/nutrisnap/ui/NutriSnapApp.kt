package com.charles.nutrisnap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.charles.nutrisnap.data.ThemeMode
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.nav.NutriNavHost
import com.charles.nutrisnap.ui.nav.Routes
import com.charles.nutrisnap.ui.nav.TopLevelTab
import com.charles.nutrisnap.ui.theme.Berry
import com.charles.nutrisnap.ui.theme.Mango
import com.charles.nutrisnap.ui.theme.NutriSnapTheme

@Composable
fun NutriSnapApp(startViewModel: AppStartViewModel = hiltViewModel()) {
    val startRoute by startViewModel.startRoute.collectAsStateWithLifecycle()
    val themeMode by startViewModel.themeMode.collectAsStateWithLifecycle()

    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    NutriSnapTheme(darkTheme = isDark) {
        val route = startRoute
        if (route != null) {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            val showBars = TopLevelTab.entries.any { it.route == currentRoute }

            Box(Modifier.fillMaxSize()) {
                NutriNavHost(
                    navController = navController,
                    startDestination = route,
                    modifier = Modifier.fillMaxSize(),
                )

                if (showBars) {
                    Box(Modifier.align(Alignment.BottomCenter)) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 0.dp,
                        ) {
                            val tabs = TopLevelTab.entries
                            tabs.forEachIndexed { index, tab ->
                                if (index == 2) {
                                    NavigationBarItem(
                                        selected = false,
                                        onClick = {},
                                        enabled = false,
                                        icon = {},
                                    )
                                }
                                val selected = backStackEntry?.destination?.hierarchy
                                    ?.any { it.route == tab.route } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = {
                                        navController.navigate(tab.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                    icon = { Icon(tab.icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, style = MaterialTheme.typography.labelMedium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.surfaceVariant,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }

                        IconButton(
                            onClick = { navController.navigate(Routes.SCAN) },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(bottom = 18.dp)
                                .size(66.dp)
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.background)
                                .padding(5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Brush.linearGradient(listOf(Berry, Mango))),
                        ) {
                            Icon(Icons.Rounded.PhotoCamera, contentDescription = "Scan a meal",
                                tint = Color.White, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }
        } else {
            Splash()
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Pip(size = 120.dp, animated = true)
    }
}