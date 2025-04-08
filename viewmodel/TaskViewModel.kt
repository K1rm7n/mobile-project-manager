@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val projectId: String = checkNotNull(savedStateHandle["projectId"])
    
    // Task list state
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Task form state
    private val _taskTitle = MutableStateFlow("")
    val taskTitle: StateFlow<String> = _taskTitle.asStateFlow()
    
    private val _taskDescription = MutableStateFlow("")
    val taskDescription: StateFlow<String> = _taskDescription.asStateFlow()
    
    private val _taskAssignee = MutableStateFlow<String?>(null)
    val taskAssignee: StateFlow<String?> = _taskAssignee.asStateFlow()
    
    private val _taskPriority = MutableStateFlow(TaskPriority.MEDIUM)
    val taskPriority: StateFlow<TaskPriority> = _taskPriority.asStateFlow()
    
    private val _taskDeadline = MutableStateFlow<Long?>(null)
    val taskDeadline: StateFlow<Long?> = _taskDeadline.asStateFlow()
    
    // Selected task for update
    private val _selectedTask = MutableStateFlow<Task?>(null)
    val selectedTask: StateFlow<Task?> = _selectedTask.asStateFlow()
    
    init {
        loadTasks()
    }
    
    private fun loadTasks() {
        viewModelScope.launch {
            _isLoading.value = true
            taskRepository.getProjectTasks(projectId)
                .catch { e ->
                    _errorMessage.value = e.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
                .collect { taskList ->
                    _tasks.value = taskList
                    _isLoading.value = false
                }
        }
    }
    
    fun setTaskTitle(title: String) {
        _taskTitle.value = title
    }
    
    fun setTaskDescription(description: String) {
        _taskDescription.value = description
    }
    
    fun setTaskAssignee(assigneeId: String?) {
        _taskAssignee.value = assigneeId
    }
    
    fun setTaskPriority(priority: TaskPriority) {
        _taskPriority.value = priority
    }
    
    fun setTaskDeadline(deadline: Long?) {
        _taskDeadline.value = deadline
    }
    
    fun selectTask(task: Task) {
        _selectedTask.value = task
        _taskTitle.value = task.title
        _taskDescription.value = task.description
        _taskAssignee.value = task.assignedTo
        _taskPriority.value = task.priority
        _taskDeadline.value = task.deadline
    }
    
    fun clearForm() {
        _selectedTask.value = null
        _taskTitle.value = ""
        _taskDescription.value = ""
        _taskAssignee.value = null
        _taskPriority.value = TaskPriority.MEDIUM
        _taskDeadline.value = null
    }
    
    fun createTask() {
        viewModelScope.launch {
            _isLoading.value = true
            val newTask = Task(
                id = "",
                projectId = projectId,
                title = _taskTitle.value,
                description = _taskDescription.value,
                assignedTo = _taskAssignee.value,
                createdBy = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                priority = _taskPriority.value,
                deadline = _taskDeadline.value
            )
            
            taskRepository.createTask(newTask)
                .onSuccess {
                    clearForm()
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to create task"
                }
            
            _isLoading.value = false
        }
    }
    
    fun updateTask() {
        viewModelScope.launch {
            _isLoading.value = true
            val taskToUpdate = _selectedTask.value?.copy(
                title = _taskTitle.value,
                description = _taskDescription.value,
                assignedTo = _taskAssignee.value,
                priority = _taskPriority.value,
                deadline = _taskDeadline.value
            ) ?: return@launch
            
            taskRepository.updateTask(taskToUpdate)
                .onSuccess {
                    clearForm()
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to update task"
                }
            
            _isLoading.value = false
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            _isLoading.value = true
            
            taskRepository.deleteTask(task)
                .onSuccess {
                    if (_selectedTask.value?.id == task.id) {
                        clearForm()
                    }
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to delete task"
                }
            
            _isLoading.value = false
        }
    }
    
    fun updateTaskStatus(task: Task, newStatus: TaskStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            
            val updatedTask = task.copy(status = newStatus)
            taskRepository.updateTask(updatedTask)
                .onSuccess {
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to update task status"
                }
            
            _isLoading.value = false
        }
    }
}
