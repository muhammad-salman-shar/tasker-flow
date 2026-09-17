package com.neurasamu.build.solo_leveling_tasker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.neurasamu.build.solo_leveling_tasker.ui.alarm.AlarmScreen
import com.neurasamu.build.solo_leveling_tasker.ui.alarm.CreateEditAlarmScreen
import com.neurasamu.build.solo_leveling_tasker.ui.block.AppBlockerScreen
import com.neurasamu.build.solo_leveling_tasker.ui.create.CreateTaskScreen
import com.neurasamu.build.solo_leveling_tasker.ui.home.HomeScreen
import com.neurasamu.build.solo_leveling_tasker.ui.me.MeScreen
import com.neurasamu.build.solo_leveling_tasker.ui.stats.StatsScreen
import com.neurasamu.build.solo_leveling_tasker.ui.tasks.TasksScreen

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    data object Home : Tab("home", "Home", Icons.Filled.Home)
    data object Tasks : Tab("tasks", "Tasks", Icons.Filled.CheckCircle)
    data object Stats : Tab("stats", "Stats", Icons.Filled.BarChart)
    data object Alarm : Tab("alarm", "Alarm", Icons.Filled.Alarm)
    data object Me : Tab("me", "Me", Icons.Filled.Person)
}

const val ROUTE_CREATE = "create"
const val ROUTE_EDIT = "edit/{taskId}"
const val ROUTE_BLOCKER = "blocker"
const val ROUTE_ALARM_CREATE = "alarm_create"
const val ROUTE_ALARM_EDIT = "alarm_edit/{alarmId}"

@Composable
fun MainScreen(vm: MainViewModel) {
    val nav = rememberNavController()
    val tabs = listOf(Tab.Home, Tab.Tasks, Tab.Stats, Tab.Alarm, Tab.Me)
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest = backStack?.destination
    val currentRoute = currentDest?.route
    val hideChrome = currentRoute != ROUTE_CREATE &&
        currentRoute?.startsWith("edit/") != true &&
        currentRoute != ROUTE_BLOCKER &&
        currentRoute != ROUTE_ALARM_CREATE &&
        currentRoute?.startsWith("alarm_edit/") != true
    val showFab = currentRoute == Tab.Alarm.route ||
        currentRoute == Tab.Home.route ||
        currentRoute == Tab.Tasks.route

    Scaffold(
        floatingActionButton = {
            if (showFab) {
                FloatingActionButton(
                    onClick = {
                        if (currentRoute == Tab.Alarm.route) nav.navigate(ROUTE_ALARM_CREATE)
                        else nav.navigate(ROUTE_CREATE)
                    },
                    containerColor = Color(0xFFFFB300),
                    contentColor = Color.Black
                ) {
                    Icon(Icons.Filled.Add, "Add")
                }
            }
        },
        bottomBar = {
            if (hideChrome) {
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
                            label = { Text(tab.label, fontSize = 10.sp) }
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
                TasksScreen(vm, onEdit = { taskId -> nav.navigate("edit/$taskId") })
            }
            composable(Tab.Stats.route) { StatsScreen(vm) }
            composable(Tab.Alarm.route) {
                AlarmScreen(vm, onEdit = { alarmId ->
                    if (alarmId != null) nav.navigate("alarm_edit/$alarmId")
                    else nav.navigate(ROUTE_ALARM_CREATE)
                })
            }
            composable(Tab.Me.route) { MeScreen(vm, onOpenBlocker = { nav.navigate(ROUTE_BLOCKER) }) }
            composable(ROUTE_CREATE) { CreateTaskScreen(vm, onBack = { nav.popBackStack() }) }
            composable(
                route = ROUTE_EDIT,
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) { entry ->
                val taskId = entry.arguments?.getLong("taskId") ?: -1L
                CreateTaskScreen(vm, onBack = { nav.popBackStack() }, editingTaskId = taskId)
            }
            composable(ROUTE_BLOCKER) {
                AppBlockerScreen(vm, onBack = { nav.popBackStack() })
            }
            composable(ROUTE_ALARM_CREATE) {
                CreateEditAlarmScreen(vm, editingAlarmId = null, onBack = { nav.popBackStack() })
            }
            composable(
                route = ROUTE_ALARM_EDIT,
                arguments = listOf(navArgument("alarmId") { type = NavType.LongType })
            ) { entry ->
                val alarmId = entry.arguments?.getLong("alarmId") ?: -1L
                CreateEditAlarmScreen(vm, editingAlarmId = alarmId, onBack = { nav.popBackStack() })
            }
        }
    }
}

