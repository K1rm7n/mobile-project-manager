@HiltViewModel
class ProjectViewModel @Inject constructor(
    private val projectRepository: ProjectRepository
) : ViewModel() {
    
    // Current user ID
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    // Projects list
    private val _projects = MutableStateFlow<List<Project>>(emptyList())
    val projects: StateFlow<List<Project>> = _projects.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadProjects()
    }
    
    private fun loadProjects() {
        if (currentUserId.isEmpty()) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            projectRepository.getUserProjects(currentUserId)
                .catch { e ->
                    _errorMessage.value = e.message ?: "Unknown error occurred"
                    _isLoading.value = false
                }
                .collect { projectList ->
                    _projects.value = projectList
                    _isLoading.value = false
                }
        }
    }
    
    fun createProject(title: String, description: String, deadline: Long?) {
        if (currentUserId.isEmpty()) {
            _errorMessage.value = "User not authenticated"
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            val newProject = Project(
                id = UUID.randomUUID().toString(),
                title = title,
                description = description,
                createdBy = currentUserId,
                createdAt = System.currentTimeMillis(),
                deadline = deadline,
                members = emptyList(),
                status = ProjectStatus.ACTIVE
            )
            
            projectRepository.createProject(newProject)
                .onSuccess { 
                    _errorMessage.value = null 
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to create project"
                }
            
            _isLoading.value = false
        }
    }
    
    fun updateProjectStatus(projectId: String, status: ProjectStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Find the project in the current list
            val project = _projects.value.find { it.id == projectId }
            
            if (project == null) {
                _errorMessage.value = "Project not found"
                _isLoading.value = false
                return@launch
            }
            
            // Update the project with new status
            val updatedProject = project.copy(status = status)
            
            projectRepository.updateProject(updatedProject)
                .onSuccess { 
                    _errorMessage.value = null 
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to update project status"
                }
            
            _isLoading.value = false
        }
    }
    
    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Find the project in the current list
            val project = _projects.value.find { it.id == projectId }
            
            if (project == null) {
                _errorMessage.value = "Project not found"
                _isLoading.value = false
                return@launch
            }
            
            projectRepository.deleteProject(project)
                .onSuccess { 
                    _errorMessage.value = null 
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to delete project"
                }
            
            _isLoading.value = false
        }
    }
    
    fun addProjectMember(projectId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            projectRepository.addProjectMember(projectId, userId)
                .onSuccess { 
                    _errorMessage.value = null 
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to add member to project"
                }
            
            _isLoading.value = false
        }
    }
    
    fun removeProjectMember(projectId: String, userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            projectRepository.removeProjectMember(projectId, userId)
                .onSuccess { 
                    _errorMessage.value = null 
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to remove member from project"
                }
            
            _isLoading.value = false
        }
    }
    
    fun refreshProjects() {
        loadProjects()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
