@Composable
fun TaskScreen(
    viewModel: TaskViewModel = hiltViewModel(),
    navigateToTaskDetail: (String) -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val selectedTask by viewModel.selectedTask.collectAsState()
    
    val taskTitle by viewModel.taskTitle.collectAsState()
    val taskDescription by viewModel.taskDescription.collectAsState()
    val taskPriority by viewModel.taskPriority.collectAsState()
    
    var showTaskDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Tasks") },
                actions = {
                    IconButton(onClick = { showTaskDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Task")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                
                TaskList(
                    tasks = tasks,
                    onTaskClick = { task ->
                        navigateToTaskDetail(task.id)
                    },
                    onStatusChange = { task, newStatus ->
                        viewModel.updateTaskStatus(task, newStatus)
                    },
                    onEditClick = { task ->
                        viewModel.selectTask(task)
                        showTaskDialog = true
                    },
                    onDeleteClick = { task ->
                        viewModel.deleteTask(task)
                    }
                )
            }
        }
    }
    
    if (showTaskDialog) {
        TaskFormDialog(
            isUpdate = selectedTask != null,
            title = taskTitle,
            description = taskDescription,
            priority = taskPriority,
            onTitleChange = viewModel::setTaskTitle,
            onDescriptionChange = viewModel::setTaskDescription,
            onPriorityChange = viewModel::setTaskPriority,
            onSave = {
                if (selectedTask != null) {
                    viewModel.updateTask()
                } else {
                    viewModel.createTask()
                }
                showTaskDialog = false
            },
            onDismiss = {
                viewModel.clearForm()
                showTaskDialog = false
            }
        )
    }
}

@Composable
fun TaskList(
    tasks: List<Task>,
    onTaskClick: (Task) -> Unit,
    onStatusChange: (Task, TaskStatus) -> Unit,
    onEditClick: (Task) -> Unit,
    onDeleteClick: (Task) -> Unit
) {
    val groupedTasks = tasks.groupBy { it.status }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TaskStatus.values().forEach { status ->
            val tasksInStatus = groupedTasks[status] ?: emptyList()
            
            if (tasksInStatus.isNotEmpty()) {
                item {
                    Text(
                        text = status.name.replace('_', ' '),
                        style = MaterialTheme.typography.h6,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                items(tasksInStatus) { task ->
                    TaskItem(
                        task = task,
                        onClick = { onTaskClick(task) },
                        onStatusChange = { newStatus -> onStatusChange(task, newStatus) },
                        onEditClick = { onEditClick(task) },
                        onDeleteClick = { onDeleteClick(task) }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.h6,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Box {
                    IconButton(onClick = { showContextMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showContextMenu,
                        onDismissRequest = { showContextMenu = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            showStatusMenu = true
                            showContextMenu = false
                        }) {
                            Text("Change Status")
                        }
                        
                        DropdownMenuItem(onClick = {
                            onEditClick()
                            showContextMenu = false
                        }) {
                            Text("Edit")
                        }
                        
                        DropdownMenuItem(onClick = {
                            onDeleteClick()
                            showContextMenu = false
                        }) {
                            Text("Delete")
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        TaskStatus.values().forEach { status ->
                            DropdownMenuItem(onClick = {
                                onStatusChange(status)
                                showStatusMenu = false
                            }) {
                                Text(status.name.replace('_', ' '))
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = task.description,
                style = MaterialTheme.typography.body2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PriorityChip(priority = task.priority)
                
                task.deadline?.let { deadline ->
                    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
                    val deadlineText = remember(deadline) { formatter.format(Date(deadline)) }
                    
                    Text(
                        text = "Due: $deadlineText",
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        }
    }
}

@Composable
fun PriorityChip(priority: TaskPriority) {
    val (backgroundColor, contentColor) = when (priority) {
        TaskPriority.LOW -> MaterialTheme.colors.primary.copy(alpha = 0.1f) to MaterialTheme.colors.primary
        TaskPriority.MEDIUM -> MaterialTheme.colors.secondary.copy(alpha = 0.1f) to MaterialTheme.colors.secondary
        TaskPriority.HIGH -> Color(0xFFFFA000).copy(alpha = 0.1f) to Color(0xFFFFA000)
        TaskPriority.URGENT -> MaterialTheme.colors.error.copy(alpha = 0.1f) to MaterialTheme.colors.error
    }
    
    Surface(
        color = backgroundColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = priority.name,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun TaskFormDialog(
    isUpdate: Boolean,
    title: String,
    description: String,
    priority: TaskPriority,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = if (isUpdate) "Edit Task" else "Create Task")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Priority", style = MaterialTheme.typography.subtitle1)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaskPriority.values().forEach { priorityOption ->
                        val isSelected = priority == priorityOption
                        val backgroundColor = if (isSelected) {
                            when (priorityOption) {
                                TaskPriority.LOW -> MaterialTheme.colors.primary
                                TaskPriority.MEDIUM -> MaterialTheme.colors.secondary
                                TaskPriority.HIGH -> Color(0xFFFFA000)
                                TaskPriority.URGENT -> MaterialTheme.colors.error
                            }
                        } else {
                            MaterialTheme.colors.surface
                        }
                        
                        val textColor = if (isSelected) {
                            Color.White
                        } else {
                            MaterialTheme.colors.onSurface
                        }
                        
                        FilterChip(
                            selected = isSelected,
                            onClick = { onPriorityChange(priorityOption) },
                            modifier = Modifier.weight(1f),
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            } else null
                        ) {
                            Text(
                                text = priorityOption.name,
                                color = textColor
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = title.isNotBlank()
            ) {
                Text(text = if (isUpdate) "Update" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Dashboard analytics UI
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val taskStatusData by viewModel.taskStatusData.collectAsState()
    val taskPriorityData by viewModel.taskPriorityData.collectAsState()
    val taskCompletionTrend by viewModel.taskCompletionTrend.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Project Analytics") }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Task Status",
                                style = MaterialTheme.typography.h6
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Using MPAndroidChart within Jetpack Compose
                            AndroidView(
                                factory = { context ->
                                    val pieChart = PieChart(context)
                                    pieChart.description.isEnabled = false
                                    pieChart.legend.isEnabled = true
                                    pieChart.holeRadius = 40f
                                    pieChart.setEntryLabelColor(Color.Black.toArgb())
                                    pieChart.setUsePercentValues(true)
                                    pieChart
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                update = { pieChart ->
                                    // Convert task status data to pie chart entries
                                    val entries = taskStatusData.map { (status, count) ->
                                        PieEntry(count.toFloat(), status.name)
                                    }
                                    
                                    val dataSet = PieDataSet(entries, "Task Status")
                                    dataSet.colors = listOf(
                                        Color.Blue.toArgb(),
                                        Color.Green.toArgb(),
                                        Color.Yellow.toArgb(),
                                        Color.Red.toArgb()
                                    )
                                    dataSet.valueTextSize = 12f
                                    dataSet.valueFormatter = PercentFormatter(pieChart)
                                    
                                    val pieData = PieData(dataSet)
                                    pieChart.data = pieData
                                    pieChart.invalidate()
                                }
                            )
                        }
                    }
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Task Priority",
                                style = MaterialTheme.typography.h6
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Using MPAndroidChart within Jetpack Compose
                            AndroidView(
                                factory = { context ->
                                    val barChart = BarChart(context)
                                    barChart.description.isEnabled = false
                                    barChart.setDrawGridBackground(false)
                                    barChart.setDrawBarShadow(false)
                                    barChart.setDrawValueAboveBar(true)
                                    barChart
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                update = { barChart ->
                                    // Convert task priority data to bar chart entries
                                    val entries = taskPriorityData.map { (priority, count) ->
                                        BarEntry(priority.ordinal.toFloat(), count.toFloat())
                                    }
                                    
                                    val dataSet = BarDataSet(entries, "Task Priority")
                                    dataSet.colors = listOf(
                                        Color.Blue.toArgb(),
                                        Color.Green.toArgb(),
                                        Color.Yellow.toArgb(),
                                        Color.Red.toArgb()
                                    )
                                    
                                    val barData = BarData(dataSet)
                                    barChart.data = barData
                                    
                                    // Configure x-axis
                                    val xAxis = barChart.xAxis
                                    xAxis.valueFormatter = IndexAxisValueFormatter(
                                        TaskPriority.values().map { it.name }
                                    )
                                    xAxis.position = XAxisPosition.BOTTOM
                                    xAxis.setDrawGridLines(false)
                                    
                                    barChart.invalidate()
                                }
                            )
                        }
                    }
                }
                
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 4.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Task Completion Trend",
                                style = MaterialTheme.typography.h6
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Using MPAndroidChart within Jetpack Compose
                            AndroidView(
                                factory = { context ->
                                    val lineChart = LineChart(context)
                                    lineChart.description.isEnabled = false
                                    lineChart.setDrawGridBackground(false)
                                    lineChart
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                update = { lineChart ->
                                    // Convert task completion trend data to line chart entries
                                    val entries = taskCompletionTrend.mapIndexed { index, count ->
                                        Entry(index.toFloat(), count.toFloat())
                                    }
                                    
                                    val dataSet = LineDataSet(entries, "Tasks Completed")
                                    dataSet.color = Color.Blue.toArgb()
                                    dataSet.setCircleColor(Color.Blue.toArgb())
                                    dataSet.lineWidth = 2f
                                    dataSet.circleRadius = 4f
                                    dataSet.setDrawCircleHole(false)
                                    dataSet.valueTextSize = 10f
                                    
                                    val lineData = LineData(dataSet)
                                    lineChart.data = lineData
                                    lineChart.invalidate()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Chat UI for team communication
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val messageText by viewModel.messageText.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Chat") }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                elevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = viewModel::setMessageText,
                        placeholder = { Text("Type a message") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = viewModel::sendMessage,
                        enabled = messageText.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading && messages.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    reverseLayout = true
                ) {
                    items(messages) { message ->
                        ChatMessageItem(
                            message = message,
                            isCurrentUser = message.senderId == user?.id,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    isCurrentUser: Boolean,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isCurrentUser) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.surface
    }
    
    val contentColor = if (isCurrentUser) {
        MaterialTheme.colors.onPrimary
    } else {
        MaterialTheme.colors.onSurface
    }
    
    val alignment = if (isCurrentUser) {
        Alignment.End
    } else {
        Alignment.Start
    }
    
    val dateFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = remember(message.timestamp) {
        dateFormatter.format(Date(message.timestamp))
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = backgroundColor,
            contentColor = contentColor,
            elevation = 1.dp,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.body1
                )
                
                Text(
                    text = timeString,
                    style = MaterialTheme.typography.caption,
                    modifier = Modifier.align(Alignment.End)
                )
                
                message.attachmentUrl?.let { url ->
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.LightGray)
                            .clickable { /* Open attachment */ }
                    ) {
                        // Display attachment preview or icon based on type
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attachment",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}
