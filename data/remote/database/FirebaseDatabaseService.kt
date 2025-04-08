class FirebaseDatabaseService @Inject constructor() {
    private val database = FirebaseDatabase.getInstance().reference
    
    fun getProjectsReference() = database.child("projects")
    fun getTasksReference() = database.child("tasks")
    fun getChatMessagesReference() = database.child("chat_messages")
    fun getUsersReference() = database.child("users")
    
    suspend fun saveProject(project: Project): Result<Unit> {
        return try {
            val projectRef = if (project.id.isEmpty()) {
                database.child("projects").push()
            } else {
                database.child("projects").child(project.id)
            }
            
            val projectId = project.id.ifEmpty { projectRef.key!! }
            val updatedProject = project.copy(id = projectId)
            
            projectRef.setValue(updatedProject).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun saveTask(task: Task): Result<Unit> {
        return try {
            val taskRef = if (task.id.isEmpty()) {
                database.child("tasks").push()
            } else {
                database.child("tasks").child(task.id)
            }
            
            val taskId = task.id.ifEmpty { taskRef.key!! }
            val updatedTask = task.copy(id = taskId)
            
            taskRef.setValue(updatedTask).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun sendChatMessage(message: ChatMessage): Result<Unit> {
        return try {
            val msgRef = if (message.id.isEmpty()) {
                database.child("chat_messages").push()
            } else {
                database.child("chat_messages").child(message.id)
            }
            
            val msgId = message.id.ifEmpty { msgRef.key!! }
            val updatedMessage = message.copy(id = msgId)
            
            msgRef.setValue(updatedMessage).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
