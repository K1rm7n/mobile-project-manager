@HiltViewModel
class FileViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Current project ID and task ID (may be null)
    private val projectId: String? = savedStateHandle["projectId"]
    private val taskId: String? = savedStateHandle["taskId"]
    
    // File list state
    private val _files = MutableStateFlow<List<FileAttachment>>(emptyList())
    val files: StateFlow<List<FileAttachment>> = _files.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Upload progress
    private val _uploadProgress = MutableStateFlow<Float?>(null)
    val uploadProgress: StateFlow<Float?> = _uploadProgress.asStateFlow()
    
    init {
        loadFiles()
    }
    
    private fun loadFiles() {
        viewModelScope.launch {
            _isLoading.value = true
            
            when {
                // If task ID is provided, load files for that task
                taskId != null -> {
                    fileRepository.getFilesByTask(taskId)
                        .catch { e ->
                            _errorMessage.value = e.message ?: "Unknown error occurred"
                            _isLoading.value = false
                        }
                        .collect { fileList ->
                            _files.value = fileList
                            _isLoading.value = false
                        }
                }
                // If project ID is provided, load files for that project
                projectId != null -> {
                    fileRepository.getFilesByProject(projectId)
                        .catch { e ->
                            _errorMessage.value = e.message ?: "Unknown error occurred"
                            _isLoading.value = false
                        }
                        .collect { fileList ->
                            _files.value = fileList
                            _isLoading.value = false
                        }
                }
                // Neither project ID nor task ID provided
                else -> {
                    _errorMessage.value = "Missing project or task ID"
                    _isLoading.value = false
                }
            }
        }
    }
    
    fun uploadFile(uri: Uri, fileName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _uploadProgress.value = 0f
            
            fileRepository.uploadFile(uri, fileName, taskId, projectId)
                .onSuccess { 
                    _uploadProgress.value = null
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to upload file"
                    _uploadProgress.value = null
                }
            
            _isLoading.value = false
        }
    }
    
    fun deleteFile(file: FileAttachment) {
        viewModelScope.launch {
            _isLoading.value = true
            
            fileRepository.deleteFile(file)
                .onSuccess {
                    _errorMessage.value = null
                }
                .onFailure { e ->
                    _errorMessage.value = e.message ?: "Failed to delete file"
                }
            
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
