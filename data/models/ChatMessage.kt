data class ChatMessage(
    val id: String = "",
    val projectId: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val attachmentUrl: String? = null
)
