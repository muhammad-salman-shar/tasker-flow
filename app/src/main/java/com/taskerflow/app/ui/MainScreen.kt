package com.taskerflow.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.taskerflow.app.ui.block.AppBlockerScreen
import com.taskerflow.app.ui.create.CreateTaskScreen
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

const val ROUTE_CREATE = "create"
const val ROUTE_EDIT = "edit/{taskId}"
const val ROUTE_BLOCKER = "blocker"

@Composable
fun MainScreen(vm: MainViewModel) {
    val nav = rememberNavController()
    val tabs = listOf(Tab.Home, Tab.Tasks, Tab.Stats, Tab.Me)
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    val currentRoute = currentDest?.route
    val showFab = currentRoute != ROUTE_CREATE &&
        currentRoute?.startsWith("edit/") != true &&
        currentRoute != ROUTE_BLOCKER

    Scaffold(
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = { nav.navigate(ROUTE_CREATE) },
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Filled.Add, "New Task")
                }
            }
        },
        bottomBar = {
            if (currentRoute != ROUTE_BLOCKER) {
                NavigationBar(containerColor = Color(0xFF0E1018)) {
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
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Home.route) { HomeScreen(vm) }
            composable(Tab.Tasks.route) {
                TasksScreen(
                    vm = vm,
                    onEdit = { taskId -> nav.navigate("edit/$taskId") }
                )
            }
            composable(Tab.Stats.route) { StatsScreen(vm) }
            composable(Tab.Me.route) { MeScreen(vm, onOpenBlocker = { nav.navigate(ROUTE_BLOCKER) }) }
            composable(ROUTE_CREATE) { CreateTaskScreen(vm, onBack = { nav.popBackStack() }) }
            composable(
                route = ROUTE_EDIT,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: -1L
                CreateTaskScreen(
                    vm = vm,
                    onBack = { nav.popBackStack() },
                    editingTaskId = taskId
                )
            }
            composable(ROUTE_BLOCKER) {
                AppBlockerScreen(vm, onBack = { nav.popBackStack() })
            }
        }
    }
}
