package com.taskerflow.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.taskerflow.app.ui.home.HomeScreen
import com.taskerflow.app.ui.me.MeScreen
import com.taskerflow.app.ui.stats.StatsScreen
import com.taskerflow.app.ui.tasks.TasksScreen

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Tab("home", "Home", Icons.Filled.Home)
    data object Tasks : Tab("tasks", "Tasks", Icons.Filled.CheckCircle)
    data object Stats : Tab("stats", "Stats", Icons.Filled.BarChart)
    data object Me : Tab("me", "Me", Icons.Filled.Person)
}

@Composable
fun MainScreen(vm: MainViewModel) {
    val nav = rememberNavController()
    val tabs = listOf(Tab.Home, Tab.Tasks, Tab.Stats, Tab.Me)
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDest?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            nav.navigate(tab.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Home.route) { HomeScreen(vm) }
            composable(Tab.Tasks.route) { TasksScreen(vm) }
            composable(Tab.Stats.route) { StatsScreen(vm) }
            composable(Tab.Me.route) { MeScreen(vm) }
        }
    }
}
