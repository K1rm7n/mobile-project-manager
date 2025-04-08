@Dao
interface FileAttachmentDao {
    @Query("SELECT * FROM file_attachments WHERE taskId = :taskId")
    fun getFilesByTask(taskId: String): Flow<List<FileAttachmentEntity>>
    
    @Query("SELECT * FROM file_attachments WHERE projectId = :projectId")
    fun getFilesByProject(projectId: String): Flow<List<FileAttachmentEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileAttachmentEntity)
    
    @Update
    suspend fun updateFile(file: FileAttachmentEntity)
    
    @Delete
    suspend fun deleteFile(file: FileAttachmentEntity)
}
