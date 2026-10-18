package com.example.ssmapplication3.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Menu
import com.example.ssmapplication3.feature.myday.DepartmentDashboard
import com.example.ssmapplication3.feature.myday.DepartmentDashboardViewModel
import com.example.ssmapplication3.feature.workspace.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.ssmapplication3.feature.stores.StoresRepository
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ssmapplication3.core.auth.SessionManager
import com.example.ssmapplication3.core.auth.SessionState
import com.example.ssmapplication3.core.security.CapabilitiesManager
import com.example.ssmapplication3.core.ui.AccessDeniedView
import com.example.ssmapplication3.core.ui.EmptyView
import com.example.ssmapplication3.core.ui.LoadingView
import com.example.ssmapplication3.feature.auth.LoginScreen
import com.example.ssmapplication3.feature.auth.LoginViewModel
import com.example.ssmapplication3.feature.myday.MyDayScreen
import com.example.ssmapplication3.feature.myday.MyDayViewModel
import com.example.ssmapplication3.feature.procurement.ProcurementScreen
import com.example.ssmapplication3.feature.procurement.ProcurementViewModel
import com.example.ssmapplication3.feature.receiving.ReceivingDraftScreen
import com.example.ssmapplication3.feature.receiving.ReceivingDraftViewModel
import com.example.ssmapplication3.feature.receiving.FinalGrnRepository
import com.example.ssmapplication3.feature.inventory.InventoryScreen
import com.example.ssmapplication3.feature.inventory.InventoryViewModel
import com.example.ssmapplication3.feature.stores.returns.MaterialReturnsScreen
import com.example.ssmapplication3.feature.stores.returns.MaterialReturnsViewModel
import com.example.ssmapplication3.feature.stores.issues.MaterialIssueScreen
import com.example.ssmapplication3.feature.stores.issues.MaterialIssueViewModel
import com.example.ssmapplication3.feature.stores.adjustments.StockAdjustmentScreen
import com.example.ssmapplication3.feature.stores.adjustments.StockAdjustmentViewModel
import com.example.ssmapplication3.feature.stores.transfers.StockTransferScreen
import com.example.ssmapplication3.feature.stores.transfers.StockTransferViewModel
import com.example.ssmapplication3.feature.scanner.ScannerScreen
import com.example.ssmapplication3.feature.mywork.MyWorkScreen
import com.example.ssmapplication3.feature.mywork.MyWorkViewModel
import com.example.ssmapplication3.feature.notifications.NotificationsScreen
import com.example.ssmapplication3.feature.notifications.NotificationsViewModel
import com.example.ssmapplication3.feature.projects.ProjectsScreen
import com.example.ssmapplication3.feature.projects.ProjectDetailScreen
import com.example.ssmapplication3.feature.projects.ProjectsViewModel
import com.example.ssmapplication3.feature.documents.EngineeringDocumentsScreen
import com.example.ssmapplication3.feature.hr.HrEmployeesScreen
import com.example.ssmapplication3.feature.hr.HrEmployeesViewModel
import com.example.ssmapplication3.feature.facility.FacilityAssetsScreen
import com.example.ssmapplication3.feature.procurement.PurchaseDraftReviewScreen
import kotlinx.coroutines.launch

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val sessionState by sessionManager.sessionState.collectAsState()
    val scope = rememberCoroutineScope()
    val loginViewModel = remember(sessionManager) { LoginViewModel(sessionManager) }

    when (val state = sessionState) {
        is SessionState.Initializing -> LoadingView(message = "Restoring session...")
        is SessionState.Unauthenticated, is SessionState.SessionExpired, is SessionState.Error,
        is SessionState.AccessDenied, is SessionState.AwaitingGoogle -> {
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {}
            )
        }
        is SessionState.Suspended -> AccessDeniedView(reason = "Your employee account has been suspended.")
        is SessionState.Authenticating -> LoadingView(message = "Authenticating profile...")
        is SessionState.Authenticated -> key(state.userProfile.id) {
            val navController = rememberNavController()
            val context = LocalContext.current.applicationContext
            val stores = remember { StoresRepository(context) }
            val myDayViewModel = remember(sessionManager) { MyDayViewModel(sessionManager) }
            val profile = state.userProfile
            val isAdmin = "admin" in profile.roles
            val workspaceViewModel: WorkspaceViewModel = viewModel(key = "workspace-${profile.id}")
            val departments by workspaceViewModel.departments.collectAsState()
            val departmentError by workspaceViewModel.error.collectAsState()
            var selectedDepartmentId by rememberSaveable(profile.id) { mutableStateOf<String?>(null) }
            var chooseDepartment by remember { mutableStateOf(false) }
            LaunchedEffect(profile.id) { workspaceViewModel.load() }
            val ownDepartment = departments.firstOrNull { it.id == profile.departmentId }?.name
                ?: profile.departmentName.orEmpty()
            val workspace = if (isAdmin) departments.firstOrNull { it.id == selectedDepartmentId }?.name
                ?: ownDepartment else ownDepartment

            val selectedDepartment = departments.firstOrNull { it.id == (if (isAdmin) selectedDepartmentId ?: profile.departmentId else profile.departmentId) }
            val effectiveDepartmentId = selectedDepartment?.id ?: profile.departmentId
            val departmentAliases = selectedDepartment?.aliases.orEmpty()
            val dashboard: DepartmentDashboardViewModel = viewModel(key = "dashboard-${profile.id}")
            val notifications: NotificationsViewModel = viewModel(key = "notifications-${profile.id}")
            val unreadNotifications by notifications.unreadCount.collectAsState()
            val bottomNavItems = listOf(Screen.MyDay, Screen.DepartmentWork, Screen.Search, Screen.Drive, Screen.More)

            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(title = { Text(workspace.ifBlank { "My workspace" }, style = MaterialTheme.typography.titleMedium) },
                        actions = {
                            Box {
                                IconButton(onClick = { navController.navigate(Screen.Notifications.route) { launchSingleTop = true } }) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                }
                                if (unreadNotifications > 0) Badge(Modifier.align(androidx.compose.ui.Alignment.TopEnd)) { Text(unreadNotifications.coerceAtMost(99).toString()) }
                            }
                            if (isAdmin) Box {
                                IconButton(onClick = { chooseDepartment = true; workspaceViewModel.load() }) {
                                    Icon(Icons.Default.Settings, contentDescription = "Select department")
                                }
                                DropdownMenu(expanded = chooseDepartment, onDismissRequest = { chooseDepartment = false }) {
                                    DropdownMenuItem(text = { Text("My department") }, onClick = {
                                        selectedDepartmentId = null
                                        chooseDepartment = false
                                        navController.navigate(Screen.MyDay.route) { popUpTo(Screen.MyDay.route) { inclusive = true }; launchSingleTop = true }
                                    })
                                    departments.forEach { department ->
                                        DropdownMenuItem(text = { Text(if (department.id == selectedDepartmentId) "✓ ${department.name}" else department.name) }, onClick = {
                                            selectedDepartmentId = department.id
                                            chooseDepartment = false
                                            navController.navigate(Screen.MyDay.route) { popUpTo(Screen.MyDay.route) { inclusive = true }; launchSingleTop = true }
                                        })
                                    }
                                    departmentError?.let { message ->
                                        DropdownMenuItem(text = { Text(message) }, onClick = workspaceViewModel::load)
                                    }
                                }
                            }
                        })
                },
                bottomBar = {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentRoute = navBackStackEntry?.destination?.route

                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                selected = currentRoute == screen.route,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id)
                                        launchSingleTop = true
                                    }
                                },
                                label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                                icon = { Icon(when (screen) {
                                    Screen.MyDay -> Icons.Default.Home
                                    Screen.Search -> Icons.Default.Search
                                    Screen.More -> Icons.Default.Menu
                                    else -> Icons.Default.List
                                }, contentDescription = null) }
                            )
                        }
                    }
                },
                modifier = modifier
            ) { innerPadding ->
                val currentEntry by navController.currentBackStackEntryAsState()
                val route = currentEntry?.destination?.route ?: Screen.MyDay.route
                if (!WorkspacePolicy.allows(profile, workspace, route)) {
                    androidx.compose.foundation.layout.Column(Modifier.padding(innerPadding)) {
                        Text("This page is not available in this department view.")
                        TextButton(onClick = { navController.popBackStack(Screen.MyDay.route, false) }) { Text("Back to dashboard") }
                    }
                } else NavHost(
                    navController = navController,
                    startDestination = Screen.MyDay.route,
                    modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)
                ) {
                    composable(Screen.MyDay.route) {
                        DepartmentDashboard(dashboard, effectiveDepartmentId, workspace, departmentAliases,
                            onProject = { navController.navigate("project/$it") })
                    }
                    composable(Screen.DepartmentWork.route) {
                        when {
                            WorkspacePolicy.allows(profile, workspace, Screen.Projects.route) ->
                                ProjectsScreen(viewModel = viewModel(), onProjectClick = { navController.navigate("project/$it") })
                            WorkspacePolicy.allows(profile, workspace, Screen.HrEmployees.route) ->
                                HrEmployeesScreen(viewModel = viewModel())
                            WorkspacePolicy.category(workspace) in setOf("procurement", "stores") && WorkspacePolicy.allows(profile, workspace, Screen.Procurement.route) ->
                                ProcurementScreen(viewModel = viewModel(), onReceivingClick = { navController.navigate(Screen.ReceivingDraft.route) }, onInventoryClick = { navController.navigate(Screen.Inventory.route) })
                            else -> MyWorkScreen(viewModel = viewModel())
                        }
                    }
                    composable(Screen.Search.route) {
                        WorkspaceSearchScreen(profile, workspace, dashboard, effectiveDepartmentId, departmentAliases,
                            onNavigate = { navController.navigate(it) })
                    }
                    composable(Screen.Drive.route) {
                        DepartmentDriveScreen(viewModel = viewModel(), department = workspace,
                            departmentId = effectiveDepartmentId, onProjectClick = { navController.navigate("project/$it") })
                    }

                    composable(Screen.Projects.route) { ProjectsScreen(viewModel = remember { ProjectsViewModel() }, onProjectClick = { id -> navController.navigate("project/$id") }) }
                    composable(Screen.ProjectDetail.route) { entry ->
                        val projectId = entry.arguments?.getString("id").orEmpty()
                        ProjectDetailScreen(projectId,
                            onDocumentsClick = { if (projectId.isNotBlank()) navController.navigate("project/$projectId/documents") },
                            onProcurementClick = { navController.navigate(Screen.Procurement.route) })
                    }
                    composable(Screen.ProjectDocuments.route) { entry ->
                        val projectId = entry.arguments?.getString("id")
                        if (projectId.isNullOrBlank()) EmptyView("Project not selected", "Open documents from a visible project.")
                        else EngineeringDocumentsScreen(viewModel = viewModel(), department = workspace,
                            departmentId = effectiveDepartmentId, projectId = projectId)
                    }
                    composable(Screen.EngineeringDocuments.route) {
                        EngineeringDocumentsScreen(viewModel = viewModel(), department = workspace,
                            departmentId = effectiveDepartmentId)
                    }
                    composable(Screen.HrEmployees.route) { HrEmployeesScreen(viewModel = remember { HrEmployeesViewModel() }) }
                    composable(Screen.MyWork.route) {
                        MyWorkScreen(viewModel = remember { MyWorkViewModel() })
                    }
                    composable(Screen.Scanner.route) { ScannerScreen(viewModel = remember { InventoryViewModel() }) }
                    composable(Screen.Notifications.route) {
                        NotificationsScreen(viewModel = notifications)
                    }
                    composable(Screen.More.route) {
                        WorkspaceMoreScreen(profile, workspace, onNavigate = { navController.navigate(it) },
                            onRefresh = { scope.launch { sessionManager.refreshIdentity(); workspaceViewModel.load(); dashboard.refresh() } },
                            onLogout = { scope.launch { sessionManager.logout() } })
                    }
                    composable(Screen.Procurement.route) { ProcurementScreen(viewModel = remember { ProcurementViewModel() }, onReceivingClick = { navController.navigate(Screen.ReceivingDraft.route) }, onInventoryClick = { navController.navigate(Screen.Inventory.route) }, onReviewClick = { navController.navigate(Screen.PurchaseDraftReview.route) }) }
                    composable(Screen.ReceivingDraft.route) { ReceivingDraftScreen(viewModel = viewModel(factory = viewModelFactory { initializer { ReceivingDraftViewModel(com.example.ssmapplication3.core.sync.SyncCoordinator.get(context), state.userProfile.id, FinalGrnRepository(context)) } })) }
                    composable(Screen.Inventory.route) { InventoryScreen(viewModel = remember { InventoryViewModel() }, onReturnsClick = { navController.navigate(Screen.MaterialReturns.route) }) }
                    composable(Screen.MaterialReturns.route) { MaterialReturnsScreen(viewModel = viewModel(factory = viewModelFactory { initializer { MaterialReturnsViewModel(stores) } })) }
                    composable(Screen.MaterialIssue.route) { MaterialIssueScreen(viewModel = viewModel(factory = viewModelFactory { initializer { MaterialIssueViewModel(stores) } })) }
                    composable(Screen.StockAdjustment.route) { StockAdjustmentScreen(viewModel = viewModel(factory = viewModelFactory { initializer { StockAdjustmentViewModel(stores) } })) }
                    composable(Screen.StockTransfer.route) { StockTransferScreen(viewModel = viewModel(factory = viewModelFactory { initializer { StockTransferViewModel(stores) } })) }
                    composable(Screen.Assets.route) { FacilityAssetsScreen(viewModel = viewModel(), onScanClick = { navController.navigate(Screen.Scanner.route) }) }
                    composable(Screen.PurchaseDraftReview.route) { PurchaseDraftReviewScreen(viewModel = viewModel(), onNavigateReceiving = { navController.navigate(Screen.ReceivingDraft.route) }) }
                }
            }
        }
    }
}
