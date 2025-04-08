@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: AppDatabase,
    private val firebaseDatabaseService: FirebaseDatabaseService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check if user is authenticated
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return Result.failure()
        
        try {
            // Sync projects
            syncProjects()
            
            // Sync tasks
            syncTasks()
            
            // Sync chat messages
            syncChatMessages()
            
            // Sync files
            syncFiles()
            
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
    
    private suspend fun syncProjects() {
        // Get all projects that need to be synced
        val projects = database.projectDao().getAllProjects().first()
            .filter { it.lastSyncedAt < System.currentTimeMillis() - SYNC_THRESHOLD }
        
        for (projectEntity in projects) {
            try {
                val project = projectEntity.toDomain()
                firebaseDatabaseService.saveProject(project)
                
                // Update last synced time
                database.projectDao().updateProject(
                    projectEntity.copy(lastSyncedAt = System.currentTimeMillis())
                )
            } catch (e: Exception) {
                // If sync fails for one entity, continue with others
                continue
            }
        }
    }
    
    private suspend fun syncTasks() {
        // Get all tasks that need to be synced
        val tasks = database.taskDao().getAllTasks().first()
            .filter { it.lastSyncedAt < System.currentTimeMillis() - SYNC_THRESHOLD }
        
        for (taskEntity in tasks) {
            try {
                val task = taskEntity.toDomain()
                firebaseDatabaseService.saveTask(task)
                
                // Update last synced time
                database.taskDao().updateTask(
                    taskEntity.copy(lastSyncedAt = System.currentTimeMillis())
                )
            } catch (e: Exception) {
                continue
            }
        }
    }
    
    private suspend fun syncChatMessages() {
        // Get all messages that need to be synced
        val messages = database.chatMessageDao().getAllMessages().first()
            .filter { it.lastSyncedAt < System.currentTimeMillis() - SYNC_THRESHOLD }
        
        for (messageEntity in messages) {
            try {
                val message = messageEntity.toDomain()
                firebaseDatabaseService.sendChatMessage(message)
                
                // Update last synced time
                database.chatMessageDao().updateMessage(
                    messageEntity.copy(lastSyncedAt = System.currentTimeMillis())
                )
            } catch (e: Exception) {
                continue
            }
        }
    }
    
    private suspend fun syncFiles() {
        // For files, we can only update metadata since the actual file upload
        // requires the original file URI which we don't have in the worker
        val files = database.fileAttachmentDao().getAllFiles().first()
            .filter { it.lastSyncedAt < System.currentTimeMillis() - SYNC_THRESHOLD }
        
        for (fileEntity in files) {
            try {
                val file = fileEntity.toDomain()
                
                // Only update file metadata in Realtime Database
                firebaseDatabaseService.getProjectsReference()
                    .child("files")
                    .child(file.id)
                    .setValue(file)
                    .await()
                
                // Update last synced time
                database.fileAttachmentDao().updateFile(
                    fileEntity.copy(lastSyncedAt = System.currentTimeMillis())
                )
            } catch (e: Exception) {
                continue
            }
        }
    }
    
    companion object {
        private const val SYNC_THRESHOLD = 15 * 60 * 1000 // 15 minutes
        
        fun buildRequest() = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.MINUTES
            )
            .build()
    }
}
