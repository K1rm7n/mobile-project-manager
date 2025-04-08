@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerApp(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Get authentication state
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    
    // Define navigation routes
    sealed class Screen(val route: String) {
        object Login : Screen("login")
        object Register : Screen("register")
        object ProjectList : Screen("projects")
        object ProjectDetail : Screen("projects/{projectId}") {
            fun createRoute(projectId: String) = "projects/$projectId"
        }
        object TaskDetail : Screen("projects/{projectId}/tasks/{taskId}") {
            fun createRoute(projectId: String, taskId: String) = "projects/$projectId/tasks/$taskId"
        }
        object Analytics : Screen("analytics")
        object Chat : Screen("projects/{projectId}/chat") {
            fun createRoute(projectId: String) = "projects/$projectId/chat"
        }
        object Files : Screen("projects/{projectId}/files") {
            fun createRoute(projectId: String) = "projects/$projectId/files"
        }
        object TaskFiles : Screen("projects/{projectId}/tasks/{taskId}/files") {
            fun createRoute(projectId: String, taskId: String) = "projects/$projectId/tasks/$taskId/files"
        }
    }
    
    Scaffold(
        topBar = {
            if (isAuthenticated) {
                TaskManagerTopBar(
                    onNavigateUp = { navController.navigateUp() },
                    canNavigateUp = navController.previousBackStackEntry != null,
                    onLogout = { authViewModel.signOut() }
                )
            }
        },
        bottomBar = {
            if (isAuthenticated) {
                TaskManagerBottomBar(
                    onNavigateToProjects = { navController.navigate(Screen.ProjectList.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated) Screen.ProjectList.route else Screen.Login.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            // Authentication
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { navController.navigate(Screen.ProjectList.route) },
                    onNavigateToRegister = { navController.navigate(Screen.Register.route) }
                )
            }
            
            composable(Screen.Register.route) {
                RegisterScreen(
                    onRegisterSuccess = { navController.navigate(Screen.ProjectList.route) },
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            
            // Projects
            composable(Screen.ProjectList.route) {
                ProjectListScreen(
                    onProjectClick = { projectId ->
                        navController.navigate(Screen.ProjectDetail.createRoute(projectId))
                    }
                )
            }
            
            composable(
                route = Screen.ProjectDetail.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                ProjectDetailScreen(
                    projectId = projectId,
                    onTaskClick = { taskId ->
                        navController.navigate(Screen.TaskDetail.createRoute(projectId, taskId))
                    },
                    onChatClick = {
                        navController.navigate(Screen.Chat.createRoute(projectId))
                    },
                    onFilesClick = {
                        navController.navigate(Screen.Files.createRoute(projectId))
                    }
                )
            }
            
            // Tasks
            composable(
                route = Screen.TaskDetail.route,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("taskId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                TaskDetailScreen(
                    projectId = projectId,
                    taskId = taskId,
                    onFilesClick = {
                        navController.navigate(Screen.TaskFiles.createRoute(projectId, taskId))
                    }
                )
            }
            
            // Analytics
            composable(Screen.Analytics.route) {
                AnalyticsScreen()
            }
            
            // Chat
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                ChatScreen(projectId = projectId)
            }
            
            // Files
            composable(
                route = Screen.Files.route,
                arguments = listOf(navArgument("projectId") { type = NavType.StringType })
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                FileListScreen(projectId = projectId)
            }
            
            composable(
                route = Screen.TaskFiles.route,
                arguments = listOf(
                    navArgument("projectId") { type = NavType.StringType },
                    navArgument("taskId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
                FileListScreen(projectId = projectId, taskId = taskId)
            }
        }
    }
}
