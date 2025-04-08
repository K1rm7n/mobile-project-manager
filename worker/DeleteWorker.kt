@HiltWorker
class DeleteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val firebaseDatabaseService: FirebaseDatabaseService,
    private val firebaseStorageService: FirebaseStorageService
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        // Check if user is authenticated
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return Result.failure()
        
        try {
            // Get entity type and data from input data
            val entityType = inputData.getString(KEY_ENTITY_TYPE) ?: return Result.failure()
            val entityJson = inputData.getString(KEY_ENTITY_DATA) ?: return Result.failure()
            
            when (entityType) {
                TYPE_PROJECT -> deleteProject(entityJson)
                TYPE_TASK -> deleteTask(entityJson)
                TYPE_FILE -> deleteFile(entityJson)
                else -> return Result.failure()
            }
            
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }
    
    private suspend fun deleteProject(projectJson: String) {
        val project = Gson().fromJson(projectJson, Project::class.java)
        
        // Delete the project from Firebase
        firebaseDatabaseService.getProjectsReference()
            .child(project.id)
            .removeValue()
            .await()
        
        // Also delete all tasks associated with this project
        firebaseDatabaseService.getTasksReference()
            .orderByChild("projectId")
            .equalTo(project.id)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (taskSnapshot in snapshot.children) {
                        taskSnapshot.ref.removeValue()
                    }
                }
                
                override fun onCancelled(error: DatabaseError) {
                    // Just log, don't fail the worker
                    Log.e("DeleteWorker", "Failed to delete associated tasks: ${error.message}")
                }
            })
    }
    
    private suspend fun deleteTask(taskJson: String) {
        val task = Gson().fromJson(taskJson, Task::class.java)
        
        // Delete the task from Firebase
        firebaseDatabaseService.getTasksReference()
            .child(task.id)
            .removeValue()
            .await()
    }
    
    private suspend fun deleteFile(fileJson: String) {
        val file = Gson().fromJson(fileJson, FileAttachment::class.java)
        
        // Delete the file metadata from Firebase Database
        firebaseDatabaseService.getProjectsReference()
            .child("files")
            .child(file.id)
            .removeValue()
            .await()
        
        // Try to delete the actual file from Firebase Storage
        try {
            val path = file.url.substringAfter("firebase.com/")
            firebaseStorageService.deleteFile(path)
        } catch (e: Exception) {
            // Log but don't fail if we can't delete the storage file
            Log.e("DeleteWorker", "Failed to delete file from storage: ${e.message}")
        }
    }
    
    companion object {
        private const val KEY_ENTITY_TYPE = "entity_type"
        private const val KEY_ENTITY_DATA = "entity_data"
        
        private const val TYPE_PROJECT = "project"
        private const val TYPE_TASK = "task"
        private const val TYPE_FILE = "file"
        
        fun buildRequest(entity: Any) = OneTimeWorkRequestBuilder<DeleteWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setInputData(
                Data.Builder()
                    .putString(KEY_ENTITY_TYPE, getEntityType(entity))
                    .putString(KEY_ENTITY_DATA, Gson().toJson(entity))
                    .build()
            )
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.MINUTES
            )
            .build()
        
        private fun getEntityType(entity: Any): String {
            return when (entity) {
                is Project -> TYPE_PROJECT
                is Task -> TYPE_TASK
                is FileAttachment -> TYPE_FILE
                else -> throw IllegalArgumentException("Unsupported entity type")
            }
        }
    }
}
