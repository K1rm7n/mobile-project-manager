@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE projectId = :projectId ORDER BY timestamp DESC")
    fun getMessagesByProject(projectId: String): Flow<List<ChatMessageEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)
    
    @Update
    suspend fun updateMessage(message: ChatMessageEntity)
    
    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)
    
    @Query("UPDATE chat_messages SET isRead = 1 WHERE projectId = :projectId AND senderId != :currentUserId")
    suspend fun markMessagesAsRead(projectId: String, currentUserId: String)
}
