class FileRepository @Inject constructor(
    private val fileAttachmentDao: FileAttachmentDao,
    private val firebaseStorage: FirebaseStorageService,
    private val firebaseDatabase: FirebaseDatabaseService,
    private val networkUtils: NetworkUtils
) {
    
    // Get files by task
    fun getFilesByTask(taskId: String): Flow<List<FileAttachment>> {
        // Local data source
        val localFiles = fileAttachmentDao.getFilesByTask(taskId)
        
        // Remote data source (if online)
        if (networkUtils.isNetworkAvailable()) {
            firebaseDatabase.getProjectsReference()
                .child("files")
                .orderByChild("taskId")
                .equalTo(taskId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val files = snapshot.children.mapNotNull { 
                            it.getValue(FileAttachment::class.java) 
                        }
                        
                        // Save to local database
                        CoroutineScope(Dispatchers.IO).launch {
                            files.forEach { file ->
                                fileAttachmentDao.insertFile(file.toEntity())
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FileRepository", "Failed to load files: ${error.message}")
                    }
                })
        }
        
        // Return local data source as flow
        return localFiles.map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // Get files by project
    fun getFilesByProject(projectId: String): Flow<List<FileAttachment>> {
        // Local data source
        val localFiles = fileAttachmentDao.getFilesByProject(projectId)
        
        // Remote data source (if online)
        if (networkUtils.isNetworkAvailable()) {
            firebaseDatabase.getProjectsReference()
                .child("files")
                .orderByChild("projectId")
                .equalTo(projectId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val files = snapshot.children.mapNotNull { 
                            it.getValue(FileAttachment::class.java) 
                        }
                        
                        // Save to local database
                        CoroutineScope(Dispatchers.IO).launch {
                            files.forEach { file ->
                                fileAttachmentDao.insertFile(file.toEntity())
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("FileRepository", "Failed to load files: ${error.message}")
                    }
                })
        }
        
        // Return local data source as flow
        return localFiles.map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // Upload file
    suspend fun uploadFile(uri: Uri, fileName: String, taskId: String?, projectId: String?): Result<FileAttachment> {
        return try {
            // Check if we have either taskId or projectId
            if (taskId == null && projectId == null) {
                return Result.failure(Exception("Either taskId or projectId must be provided"))
            }
            
            val currentUser = FirebaseAuth.getInstance().currentUser?.uid
                ?: return Result.failure(Exception("User not authenticated"))
            
            val fileExtension = fileName.substringAfterLast('.', "")
            val fileType = when (fileExtension.lowercase()) {
                "pdf" -> "application/pdf"
                "doc", "docx" -> "application/msword"
                "xls", "xlsx" -> "application/vnd.ms-excel"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                else -> "application/octet-stream"
            }
            
            // Try to upload to Firebase Storage if online
            if (networkUtils.isNetworkAvailable()) {
                // Path in Firebase Storage
                val path = when {
                    taskId != null -> "files/tasks/$taskId/${UUID.randomUUID()}_$fileName"
                    else -> "files/projects/$projectId/${UUID.randomUUID()}_$fileName"
                }
                
                // Upload file to Firebase Storage
                val downloadUrlResult = firebaseStorage.uploadFile(uri, path)
                
                if (downloadUrlResult.isSuccess) {
                    val downloadUrl = downloadUrlResult.getOrThrow()
                    
                    // Create the file attachment
                    val fileId = UUID.randomUUID().toString()
                    val fileAttachment = FileAttachment(
                        id = fileId,
                        name = fileName,
                        url = downloadUrl,
                        size = uri.getFile()?.length() ?: 0,
                        type = fileType,
                        uploadedBy = currentUser,
                        uploadedAt = System.currentTimeMillis(),
                        taskId = taskId,
                        projectId = projectId
                    )
                    
                    // Save to Firebase Database
                    firebaseDatabase.getProjectsReference()
                        .child("files")
                        .child(fileId)
                        .setValue(fileAttachment)
                        .await()
                    
                    // Save to local database
                    fileAttachmentDao.insertFile(fileAttachment.toEntity())
                    
                    Result.success(fileAttachment)
                } else {
                    Result.failure(downloadUrlResult.exceptionOrNull() ?: Exception("Failed to upload file"))
                }
            } else {
                // Cannot upload when offline
                Result.failure(Exception("Internet connection required to upload files"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Delete file
    suspend fun deleteFile(fileAttachment: FileAttachment): Result<Unit> {
        return try {
            // Try to delete from Firebase first if online
            if (networkUtils.isNetworkAvailable()) {
                // Delete from Firebase Storage
                val path = fileAttachment.url.substringAfter("firebase.com/")
                firebaseStorage.deleteFile(path)
                
                // Delete from Firebase Database
                firebaseDatabase.getProjectsReference()
                    .child("files")
                    .child(fileAttachment.id)
                    .removeValue()
                    .await()
                
                // Delete from local database
                fileAttachmentDao.deleteFile(fileAttachment.toEntity())
                
                Result.success(Unit)
            } else {
                // Delete locally and schedule for sync
                fileAttachmentDao.deleteFile(fileAttachment.toEntity())
                
                // Schedule deletion sync
                WorkManager.getInstance()
                    .enqueueUniqueWork(
                        "delete_file_${fileAttachment.id}",
                        ExistingWorkPolicy.REPLACE,
                        DeleteWorker.buildRequest()
                    )
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helper methods for conversion between domain models and entities
    private fun FileAttachment.toEntity(): FileAttachmentEntity {
        return FileAttachmentEntity(
            id = this.id,
            name = this.name,
            url = this.url,
            size = this.size,
            type = this.type,
            uploadedBy = this.uploadedBy,
            uploadedAt = this.uploadedAt,
            taskId = this.taskId,
            projectId = this.projectId,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
    
    private fun FileAttachmentEntity.toDomain(): FileAttachment {
        return FileAttachment(
            id = this.id,
            name = this.name,
            url = this.url,
            size = this.size,
            type = this.type,
            uploadedBy = this.uploadedBy,
            uploadedAt = this.uploadedAt,
            taskId = this.taskId,
            projectId = this.projectId
        )
    }
}

// Extension function to get File from Uri
fun Uri.getFile(): File? {
    return try {
        File(this.path ?: return null)
    } catch (e: Exception) {
        null
    }
}
