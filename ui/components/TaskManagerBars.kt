@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskManagerTopBar(
    title: String = "Task Manager",
    canNavigateUp: Boolean = false,
    onNavigateUp: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (canNavigateUp) {
                IconButton(onClick = onNavigateUp) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Navigate Up"
                    )
                }
            }
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Logout"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
        )
    )
}

@Composable
fun TaskManagerBottomBar(
    onNavigateToProjects: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    var selectedItem by remember { mutableStateOf(0) }
    
    NavigationBar {
        NavigationBarItem(
            icon = { 
                Icon(
                    imageVector = Icons.Outlined.Dashboard,
                    contentDescription = "Projects"
                )
            },
            label = { Text("Projects") },
            selected = selectedItem == 0,
            onClick = {
                selectedItem = 0
                onNavigateToProjects()
            }
        )
        
        NavigationBarItem(
            icon = { 
                Icon(
                    imageVector = Icons.Outlined.Analytics,
                    contentDescription = "Analytics"
                )
            },
            label = { Text("Analytics") },
            selected = selectedItem == 1,
            onClick = {
                selectedItem = 1
                onNavigateToAnalytics()
            }
        )
    }
}
