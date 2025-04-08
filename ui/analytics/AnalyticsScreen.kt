@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val taskStatusData by viewModel.taskStatusData.collectAsState()
    val projectProgressData by viewModel.projectProgressData.collectAsState()
    val weeklyCompletionData by viewModel.weeklyCompletionData.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analytics") },
                actions = {
                    IconButton(onClick = { viewModel.refreshData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: $errorMessage",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Task Status Pie Chart
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Task Status Overview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (taskStatusData.isEmpty()) {
                            Text(
                                text = "No task data available",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            TaskStatusPieChart(
                                taskStatusData = taskStatusData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // Legend
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                LegendItem(
                                    color = TodoStatusColor,
                                    label = "To Do",
                                    count = taskStatusData[TaskStatus.TODO] ?: 0
                                )
                                LegendItem(
                                    color = InProgressStatusColor,
                                    label = "In Progress",
                                    count = taskStatusData[TaskStatus.IN_PROGRESS] ?: 0
                                )
                                LegendItem(
                                    color = ReviewStatusColor,
                                    label = "In Review",
                                    count = taskStatusData[TaskStatus.REVIEW] ?: 0
                                )
                                LegendItem(
                                    color = CompletedStatusColor,
                                    label = "Completed",
                                    count = taskStatusData[TaskStatus.COMPLETED] ?: 0
                                )
                            }
                        }
                    }
                }
                
                // Project Progress Bar Chart
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Project Progress",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (projectProgressData.isEmpty()) {
                            Text(
                                text = "No project data available",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            ProjectProgressBarChart(
                                progressData = projectProgressData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        }
                    }
                }
                
                // Weekly Task Completion Line Chart
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Weekly Task Completion",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (weeklyCompletionData.isEmpty()) {
                            Text(
                                text = "No completion data available",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        } else {
                            WeeklyCompletionLineChart(
                                completionData = weeklyCompletionData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskStatusPieChart(
    taskStatusData: Map<TaskStatus, Int>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Convert data to pie entries
    val entries = mutableListOf<PieEntry>()
    val colors = mutableListOf<Int>()
    
    if (taskStatusData.containsKey(TaskStatus.TODO)) {
        entries.add(PieEntry(taskStatusData[TaskStatus.TODO]!!.toFloat()))
        colors.add(TodoStatusColor.toArgb())
    }
    
    if (taskStatusData.containsKey(TaskStatus.IN_PROGRESS)) {
        entries.add(PieEntry(taskStatusData[TaskStatus.IN_PROGRESS]!!.toFloat()))
        colors.add(InProgressStatusColor.toArgb())
    }
    
    if (taskStatusData.containsKey(TaskStatus.REVIEW)) {
        entries.add(PieEntry(taskStatusData[TaskStatus.REVIEW]!!.toFloat()))
        colors.add(ReviewStatusColor.toArgb())
    }
    
    if (taskStatusData.containsKey(TaskStatus.COMPLETED)) {
        entries.add(PieEntry(taskStatusData[TaskStatus.COMPLETED]!!.toFloat()))
        colors.add(CompletedStatusColor.toArgb())
    }
    
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setHoleColor(AndroidColor.TRANSPARENT)
                holeRadius = 58f
                setDrawEntryLabels(false)
                setUsePercentValues(true)
                legend.isEnabled = false
                setEntryLabelColor(AndroidColor.WHITE)
                setEntryLabelTextSize(12f)
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "Task Status").apply {
                sliceSpace = 3f
                selectionShift = 5f
                this.colors = colors
                valueTextSize = 14f
                valueTextColor = AndroidColor.WHITE
                valueFormatter = PercentFormatter(chart)
            }
            
            val data = PieData(dataSet)
            chart.data = data
            chart.invalidate()
        },
        modifier = modifier
    )
}

@Composable
fun ProjectProgressBarChart(
    progressData: List<AnalyticsViewModel.ProjectProgress>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Limit to top 5 projects for better visualization
    val displayData = progressData.take(5)
    
    // Convert data to bar entries
    val entries = displayData.mapIndexed { index, progress ->
        BarEntry(index.toFloat(), progress.progressPercentage)
    }
    
    val labels = displayData.map { it.projectName }
    
    AndroidView(
        factory = { context ->
            BarChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    valueFormatter = IndexAxisValueFormatter(labels)
                    setDrawGridLines(false)
                }
                
                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 100f
                    setDrawGridLines(false)
                }
                
                axisRight.isEnabled = false
                setFitBars(true)
            }
        },
        update = { chart ->
            val dataSet = BarDataSet(entries, "Project Progress").apply {
                color = ChartColor1.toArgb()
                valueTextSize = 10f
            }
            
            val data = BarData(dataSet)
            data.barWidth = 0.6f
            
            chart.data = data
            chart.invalidate()
        },
        modifier = modifier
    )
}

@Composable
fun WeeklyCompletionLineChart(
    completionData: List<AnalyticsViewModel.DailyCompletion>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("EEE", Locale.getDefault())
    
    // Convert data to line entries
    val entries = completionData.mapIndexed { index, completion ->
        Entry(index.toFloat(), completion.tasksCompleted.toFloat())
    }
    
    val labels = completionData.map { dateFormat.format(it.date) }
    
    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                legend.isEnabled = false
                
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    valueFormatter = IndexAxisValueFormatter(labels)
                    setDrawGridLines(false)
                }
                
                axisLeft.apply {
                    axisMinimum = 0f
                    setDrawGridLines(false)
                }
                
                axisRight.isEnabled = false
                setDrawGridBackground(false)
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(entries, "Task Completion").apply {
                color = ChartColor2.toArgb()
                lineWidth = 2f
                setDrawCircles(true)
                setDrawCircleHole(true)
                circleRadius = 4f
                circleHoleRadius = 2f
                setCircleColor(ChartColor2.toArgb())
                valueTextSize = 10f
                mode = LineDataSet.Mode.CUBIC_BEZIER
            }
            
            val data = LineData(dataSet)
            chart.data = data
            chart.invalidate()
        },
        modifier = modifier
    )
}

@Composable
fun LegendItem(
    color: Color,
    label: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape),
            color = color
        ) { }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

// Extension function to convert Compose Color to Android Color int
fun Color.toArgb(): Int {
    return AndroidColor.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
