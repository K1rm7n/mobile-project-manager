@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val senderId: String,
    val content: String,
    val timestamp: Long,
    val isRead: Boolean,
    val attachmentUrl: String?,
    val lastSyncedAt: Long = 0
)
