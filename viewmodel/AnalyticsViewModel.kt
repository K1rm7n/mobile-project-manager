@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    // User ID
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    // Projects data
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    
    // Tasks data
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    // Task status count data for pie chart
    private val _taskStatusData = MutableStateFlow<Map<TaskStatus, Int>>(emptyMap())
    val taskStatusData: StateFlow<Map<TaskStatus, Int>> = _taskStatusData.asStateFlow()
    
    // Project progress data for bar chart
    private val _projectProgressData = MutableStateFlow<List<ProjectProgress>>(emptyList())
    val projectProgressData: StateFlow<List<ProjectProgress>> = _projectProgressData.asStateFlow()
    
    // Weekly task completion data for line chart
    private val _weeklyCompletionData = MutableStateFlow<List<DailyCompletion>>(emptyList())
    val weeklyCompletionData: StateFlow<List<DailyCompletion>> = _weeklyCompletionData.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Load projects
                projectRepository.getUserProjects(currentUserId)
                    .collect { projectList ->
                        _projects.value = projectList
                        
                        // Load tasks for all projects
                        val allTasks = mutableListOf<Task>()
                        
                        for (project in projectList) {
                            val tasks = taskRepository.getProjectTasks(project.id).first()
                            allTasks.addAll(tasks)
                        }
                        
                        _tasks.value = allTasks
                        
                        // Calculate analytics data
                        calculateTaskStatusData(allTasks)
                        calculateProjectProgressData(projectList, allTasks)
                        calculateWeeklyCompletionData(allTasks)
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load analytics data"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun calculateTaskStatusData(tasks: List<Task>) {
        val statusCounts = tasks.groupBy { it.status }
            .mapValues { it.value.size }
        
        _taskStatusData.value = statusCounts
    }
    
    private fun calculateProjectProgressData(projects: List<Project>, tasks: List<Task>) {
        val projectProgresses = projects.map { project ->
            val projectTasks = tasks.filter { it.projectId == project.id }
            val totalTasks = projectTasks.size
            val completedTasks = projectTasks.count { it.status == TaskStatus.COMPLETED }
            val progressPercentage = if (totalTasks > 0) {
                (completedTasks.toFloat() / totalTasks) * 100
            } else {
                0f
            }
            
            ProjectProgress(
                projectId = project.id,
                projectName = project.title,
                progressPercentage = progressPercentage,
                totalTasks = totalTasks,
                completedTasks = completedTasks
            )
        }
        
        _projectProgressData.value = projectProgresses
    }
    
    private fun calculateWeeklyCompletionData(tasks: List<Task>) {
        // Get start and end of the current week
        val now = System.currentTimeMillis()
        val startOfWeek = DateUtils.getStartOfWeek(now)
        val endOfWeek = DateUtils.getEndOfWeek(now)
        
        // Create a map for all days of the week
        val calendar = Calendar.getInstance()
        val dailyData = mutableListOf<DailyCompletion>()
        
        // Populate with all days of the current week
        calendar.timeInMillis = startOfWeek
        for (i in 0..6) {
            val dayStart = DateUtils.getStartOfDay(calendar.timeInMillis)
            val dayEnd = DateUtils.getEndOfDay(calendar.timeInMillis)
            
            val completedCount = tasks.count { 
                it.status == TaskStatus.COMPLETED && 
                it.createdAt >= dayStart && 
                it.createdAt <= dayEnd
            }
            
            dailyData.add(
                DailyCompletion(
                    date = Date(dayStart),
                    tasksCompleted = completedCount
                )
            )
            
            // Move to next day
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        _weeklyCompletionData.value = dailyData
    }
    
    fun refreshData() {
        loadData()
    }
    
    // Data classes for analytics
    data class ProjectProgress(
        val projectId: String,
        val projectName: String,
        val progressPercentage: Float,
        val totalTasks: Int,
        val completedTasks: Int
    )
    
    data class DailyCompletion(
        val date: Date,
        val tasksCompleted: Int
    )
}
