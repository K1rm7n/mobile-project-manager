class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao,
    private val firebaseDatabase: FirebaseDatabaseService,
    private val networkUtils: NetworkUtils
) {
    // Get all messages for a project
    fun getProjectMessages(projectId: String): Flow<List<ChatMessage>> {
        // Local data source
        val localMessages = chatMessageDao.getMessagesByProject(projectId)
        
        // Remote data source (if online)
        if (networkUtils.isNetworkAvailable()) {
            firebaseDatabase.getChatMessagesReference()
                .orderByChild("projectId")
                .equalTo(projectId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val messages = snapshot.children.mapNotNull { 
                            it.getValue(ChatMessage::class.java) 
                        }
                        
                        // Save to local database
                        CoroutineScope(Dispatchers.IO).launch {
                            messages.forEach { message ->
                                chatMessageDao.insertMessage(message.toEntity())
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ChatRepository", "Failed to load messages: ${error.message}")
                    }
                })
        }
        
        // Return local data source as flow
        return localMessages.map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // Send a new message
    suspend fun sendMessage(message: ChatMessage): Result<Unit> {
        return try {
            // Try to save to Firebase first if online
            if (networkUtils.isNetworkAvailable()) {
                val result = firebaseDatabase.sendChatMessage(message)
                if (result.isSuccess) {
                    // Save to local database
                    chatMessageDao.insertMessage(message.toEntity())
                    Result.success(Unit)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to send message"))
                }
            } else {
                // Save offline and sync later
                val messageToSave = if (message.id.isEmpty()) {
                    message.copy(id = UUID.randomUUID().toString())
                } else {
                    message
                }
                chatMessageDao.insertMessage(messageToSave.toEntity())
                
                // Schedule sync
                WorkManager.getInstance()
                    .enqueueUniqueWork(
                        "sync_message_${messageToSave.id}",
                        ExistingWorkPolicy.REPLACE,
                        SyncWorker.buildRequest()
                    )
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Mark messages as read
    suspend fun markMessagesAsRead(projectId: String, currentUserId: String): Result<Unit> {
        return try {
            chatMessageDao.markMessagesAsRead(projectId, currentUserId)
            
            // Update on Firebase if online
            if (networkUtils.isNetworkAvailable()) {
                firebaseDatabase.getChatMessagesReference()
                    .orderByChild("projectId")
                    .equalTo(projectId)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.children.forEach { messageSnapshot ->
                                val message = messageSnapshot.getValue(ChatMessage::class.java)
                                if (message != null && message.senderId != currentUserId && !message.isRead) {
                                    messageSnapshot.ref.child("isRead").setValue(true)
                                }
                            }
                        }
                        
                        override fun onCancelled(error: DatabaseError) {
                            Log.e("ChatRepository", "Failed to mark messages as read: ${error.message}")
                        }
                    })
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helper methods for conversion between domain models and entities
    private fun ChatMessage.toEntity(): ChatMessageEntity {
        return ChatMessageEntity(
            id = this.id,
            projectId = this.projectId,
            senderId = this.senderId,
            content = this.content,
            timestamp = this.timestamp,
            isRead = this.isRead,
            attachmentUrl = this.attachmentUrl,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
    
    private fun ChatMessageEntity.toDomain(): ChatMessage {
        return ChatMessage(
            id = this.id,
            projectId = this.projectId,
            senderId = this.senderId,
            content = this.content,
            timestamp = this.timestamp,
            isRead = this.isRead,
            attachmentUrl = this.attachmentUrl
        )
    }
}
