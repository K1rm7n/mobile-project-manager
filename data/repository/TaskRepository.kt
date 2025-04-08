class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
    private val firebaseDatabase: FirebaseDatabaseService,
    private val networkUtils: NetworkUtils
) {
    // Get all tasks for a project
    fun getProjectTasks(projectId: String): Flow<List<Task>> {
        // Local data source
        val localTasks = taskDao.getTasksByProject(projectId)
        
        // Remote data source (if online)
        if (networkUtils.isNetworkAvailable()) {
            firebaseDatabase.getTasksReference()
                .orderByChild("projectId")
                .equalTo(projectId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val tasks = snapshot.children.mapNotNull { it.getValue(Task::class.java) }
                        
                        // Save to local database
                        CoroutineScope(Dispatchers.IO).launch {
                            tasks.forEach { task ->
                                taskDao.insertTask(task.toEntity())
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("TaskRepository", "Failed to load tasks: ${error.message}")
                    }
                })
        }
        
        // Return local data source as flow
        return localTasks.map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // Create a new task
    suspend fun createTask(task: Task): Result<Unit> {
        return try {
            // Try to save to Firebase first if online
            if (networkUtils.isNetworkAvailable()) {
                val result = firebaseDatabase.saveTask(task)
                if (result.isSuccess) {
                    // Save to local database
                    taskDao.insertTask(task.toEntity())
                    Result.success(Unit)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to save task"))
                }
            } else {
                // Save offline and sync later
                val taskToSave = if (task.id.isEmpty()) {
                    task.copy(id = UUID.randomUUID().toString())
                } else {
                    task
                }
                taskDao.insertTask(taskToSave.toEntity())
                
                // Schedule sync
                WorkManager.getInstance()
                    .enqueueUniqueWork(
                        "sync_task_${taskToSave.id}",
                        ExistingWorkPolicy.REPLACE,
                        SyncWorker.buildRequest()
                    )
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update an existing task
    suspend fun updateTask(task: Task): Result<Unit> {
        return try {
            // Try to update on Firebase first if online
            if (networkUtils.isNetworkAvailable()) {
                val result = firebaseDatabase.saveTask(task)
                if (result.isSuccess) {
                    // Update local database
                    taskDao.updateTask(task.toEntity())
                    Result.success(Unit)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to update task"))
                }
            } else {
                // Update offline and sync later
                taskDao.updateTask(task.toEntity())
                
                // Schedule sync
                WorkManager.getInstance()
                    .enqueueUniqueWork(
                        "sync_task_${task.id}",
                        ExistingWorkPolicy.REPLACE,
                        SyncWorker.buildRequest()
                    )
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Delete a task
    suspend fun deleteTask(task: Task): Result<Unit> {
        return try {
            // Try to delete from Firebase first if online
            if (networkUtils.isNetworkAvailable()) {
                val result = firebaseDatabase.getTasksReference()
                    .child(task.id)
                    .removeValue()
                    .await()
                
                // Delete from local database
                taskDao.deleteTask(task.toEntity())
                Result.success(Unit)
            } else {
                // Delete locally and schedule for sync
                taskDao.deleteTask(task.toEntity())
                
                // Schedule deletion sync
                WorkManager.getInstance()
                    .enqueueUniqueWork(
                        "delete_task_${task.id}",
                        ExistingWorkPolicy.REPLACE,
                        DeleteWorker.buildRequest(task)
                    )
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helpers for converting between domain models and entities
    private fun Task.toEntity(): TaskEntity {
        return TaskEntity(
            id = this.id,
            projectId = this.projectId,
            title = this.title,
            description = this.description,
            assignedTo = this.assignedTo,
            createdBy = this.createdBy,
            createdAt = this.createdAt,
            deadline = this.deadline,
            priority = this.priority.name,
            status = this.status.name,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
    
    private fun TaskEntity.toDomain(): Task {
        return Task(
            id = this.id,
            projectId = this.projectId,
            title = this.title,
            description = this.description,
            assignedTo = this.assignedTo,
            createdBy = this.createdBy,
            createdAt = this.createdAt,
            deadline = this.deadline,
            priority = TaskPriority.valueOf(this.priority),
            status = TaskStatus.valueOf(this.status)
        )
    }
}
