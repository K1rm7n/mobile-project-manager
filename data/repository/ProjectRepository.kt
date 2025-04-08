class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val taskDao: TaskDao,
    private val firebaseDatabase: FirebaseDatabaseService,
    private val networkUtils: NetworkUtils
) {
    // Get all projects for a user
    fun getUserProjects(userId: String): Flow<List<Project>> {
        // Local data source
        val localProjects = projectDao.getProjectsForUser(userId)
        
        // Remote data source (if online)
        if (networkUtils.isNetworkAvailable()) {
            firebaseDatabase.getProjectsReference()
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val projects = snapshot.children.mapNotNull { 
                            it.getValue(Project::class.java)
                        }.filter { project ->
                            project.createdBy == userId || project.members.contains(userId)
                        }
                        
                        // Save to local database
                        CoroutineScope(Dispatchers.IO).launch {
                            projects.forEach { project ->
                                projectDao.insertProject(project.toEntity())
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ProjectRepository", "Failed to load projects: ${error.message}")
                    }
                })
        }
        
        // Return local data source as flow
        return localProjects.map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // Get project by ID
    fun getProjectById(projectId: String): Flow<Project?> {
        // Local data source
        val localProject = projectDao.getProjectById(projectId)
        
        // Remote data source (if online)
        if (networkUtils.isNetworkAvailable()) {
            firebaseDatabase.getProjectsReference()
                .child(projectId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val project = snapshot.getValue(Project::class.java)
                        
                        // Save to local database
                        project?.let {
                            CoroutineScope(Dispatchers.IO).launch {
                                projectDao.insertProject(it.toEntity())
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("ProjectRepository", "Failed to load project: ${error.message}")
                    }
                })
        }
        
        // Return local data source as flow
        return localProject.map { it?.toDomain() }
    }
    
    // Create a new project
    suspend fun createProject(project: Project): Result<String> {
        return try {
            // Try to save to Firebase first if online
            if (networkUtils.isNetworkAvailable()) {
                val result = firebaseDatabase.saveProject(project)
                if (result.isSuccess) {
                    // Save to local database
                    projectDao.insertProject(project.toEntity())
                    Result.success(project.id)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to save project"))
                }
            } else {
                // Save offline and sync later
                val projectToSave = if (project.id.isEmpty()) {
                    project.copy(id = UUID.randomUUID().toString())
                } else {
                    project
                }
                projectDao.insertProject(projectToSave.toEntity())
                
                // Schedule sync
                WorkManager.getInstance()
                    .enqueueUniqueWork(
                        "sync_project_${projectToSave.id}",
                        ExistingWorkPolicy.REPLACE,
                        SyncWorker.buildRequest()
                    )
                
                Result.success(projectToSave.id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Update an existing project
    suspend fun updateProject(project: Project): Result<Unit> {
        return try {
            // Try to update on Firebase first if online
            if (networkUtils.isNetworkAvailable()) {
                val result = firebaseDatabase.saveProject(project)
                if (result.isSuccess) {
                    // Update local database
                    projectDao.updateProject(project.toEntity())
                    Result.success(Unit)
                } else {
                    Result.failure(result.exceptionOrNull() ?: Exception("Failed to update project"))
                }
            } else {
                // Update offline and sync later
                projectDao.updateProject(project.toEntity())
                
                // Schedule sync
                WorkManager.getInstance()
                    .enqueueUniqueWork(
                        "sync_project_${project.id}",
                        ExistingWorkPolicy.REPLACE,
                        SyncWorker.buildRequest()
                    )
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Delete a project
    suspend fun deleteProject(project: Project): Result<Unit> {
        return try {
            // Try to delete from Firebase first if online
            if (networkUtils.isNetworkAvailable()) {
                val result = firebaseDatabase.getProjectsReference()
                    .child(project.id)
                    .removeValue()
                    .await()
                
                // Delete from local database
                projectDao.deleteProject(project.toEntity())
                
                // Delete all associated tasks
                taskDao.deleteTasksByProject(project.id)
                
                Result.success(Unit)
            } else {
                // Delete locally and schedule for sync
                projectDao.deleteProject(project.toEntity())
                taskDao.deleteTasksByProject(project.id)
                
                // Schedule deletion sync
                WorkManager.getInstance()
                    .enqueueUniqueWork(
                        "delete_project_${project.id}",
                        ExistingWorkPolicy.REPLACE,
                        DeleteWorker.buildRequest(project)
                    )
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Add a member to project
    suspend fun addProjectMember(projectId: String, userId: String): Result<Unit> {
        return try {
            val projectFlow = getProjectById(projectId)
            val project = projectFlow.first()
            
            project?.let {
                if (!it.members.contains(userId)) {
                    val updatedMembers = it.members.toMutableList().apply {
                        add(userId)
                    }
                    
                    val updatedProject = it.copy(members = updatedMembers)
                    updateProject(updatedProject)
                    
                    // Subscribe the user to FCM topic for this project
                    val fcmService = FCMService(applicationContext)
                    fcmService.subscribeToTopic("project_${projectId}")
                    
                    Result.success(Unit)
                } else {
                    Result.success(Unit) // Member already exists
                }
            } ?: Result.failure(Exception("Project not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Remove a member from project
    suspend fun removeProjectMember(projectId: String, userId: String): Result<Unit> {
        return try {
            val projectFlow = getProjectById(projectId)
            val project = projectFlow.first()
            
            project?.let {
                if (it.members.contains(userId)) {
                    val updatedMembers = it.members.toMutableList().apply {
                        remove(userId)
                    }
                    
                    val updatedProject = it.copy(members = updatedMembers)
                    updateProject(updatedProject)
                    
                    // Unsubscribe the user from FCM topic for this project
                    val fcmService = FCMService(applicationContext)
                    fcmService.unsubscribeFromTopic("project_${projectId}")
                    
                    Result.success(Unit)
                } else {
                    Result.success(Unit) // Member already removed
                }
            } ?: Result.failure(Exception("Project not found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helper methods for conversion between domain models and entities
    private fun Project.toEntity(): ProjectEntity {
        val json = Gson().toJson(this.members)
        return ProjectEntity(
            id = this.id,
            title = this.title,
            description = this.description,
            createdBy = this.createdBy,
            createdAt = this.createdAt,
            deadline = this.deadline,
            members = json,
            status = this.status.name,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
    
    private fun ProjectEntity.toDomain(): Project {
        val membersList = try {
            val type = object : TypeToken<List<String>>() {}.type
            Gson().fromJson<List<String>>(this.members, type)
        } catch (e: Exception) {
            emptyList<String>()
        }
        
        return Project(
            id = this.id,
            title = this.title,
            description = this.description,
            createdBy = this.createdBy,
            createdAt = this.createdAt,
            deadline = this.deadline,
            members = membersList,
            status = ProjectStatus.valueOf(this.status)
        )
    }
}
