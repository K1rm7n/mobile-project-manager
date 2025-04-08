@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Current user ID
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    // Current project ID
    private val projectId: String = checkNotNull(savedStateHandle["projectId"])
    
    // Message input
    private val _messageText = MutableStateFlow("")
    val messageText: StateFlow<String> = _messageText.asStateFlow()
    
    // Messages list
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    
    // User map (for display names and avatars)
    private val _users = MutableStateFlow<Map<String, User>>(emptyMap())
    val users: StateFlow<Map<String, User>> = _users.asStateFlow()
    
    // File attachment for message
    private val _attachmentUri = MutableStateFlow<String?>(null)
    val attachmentUri: StateFlow<String?> = _attachmentUri.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    init {
        loadMessages()
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadMessages() {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                // Get messages for current project
                chatRepository.getProjectMessages(projectId)
                    .catch { e ->
                        _errorMessage.value = e.message ?: "Unknown error occurred"
                        _isLoading.value = false
                    }
                    .collect { messageList ->
                        _messages.value = messageList
                        
                        // Mark messages as read
                        chatRepository.markMessagesAsRead(projectId, currentUserId)
                        
                        // Extract all unique sender IDs
                        val senderIds = messageList.map { it.senderId }.distinct()
                        
                        // Load user info for all senders
                        if (senderIds.isNotEmpty()) {
                            userRepository.getUsersByIds(senderIds)
                                .collect { userList ->
                                    _users.value = userList.associateBy { it.id }
                                }
                        }
                        
                        _isLoading.value = false
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load messages"
                _isLoading.value = false
            }
        }
    }
    
    fun setMessageText(text: String) {
        _messageText.value = text
    }
    
    fun setAttachment(uri: String?) {
        _attachmentUri.value = uri
    }
    
    fun sendMessage() {
        val text = _messageText.value.trim()
        if (text.isEmpty() && _attachmentUri.value == null) {
            return // Don't send empty messages
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val message = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    projectId = projectId,
                    senderId = currentUserId,
                    content = text,
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    attachmentUrl = _attachmentUri.value
                )
                
                chatRepository.sendMessage(message)
                    .onSuccess {
                        // Clear message input and attachment
                        _messageText.value = ""
                        _attachmentUri.value = null
                        _errorMessage.value = null
                    }
                    .onFailure { e ->
                        _errorMessage.value = e.message ?: "Failed to send message"
                    }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to send message"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun markMessagesAsRead() {
        viewModelScope.launch {
            try {
                chatRepository.markMessagesAsRead(projectId, currentUserId)
            } catch (e: Exception) {
                // Just log, don't show error to user
                Log.e("ChatViewModel", "Error marking messages as read: ${e.message}")
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
    
    // Get user name from ID
    fun getUserName(userId: String): String {
        return _users.value[userId]?.name ?: "Unknown User"
    }
    
    // Get user avatar from ID
    fun getUserAvatar(userId: String): String? {
        return _users.value[userId]?.profileImage
    }
    
    // Check if message is from current user
    fun isCurrentUser(message: ChatMessage): Boolean {
        return message.senderId == currentUserId
    }
}
