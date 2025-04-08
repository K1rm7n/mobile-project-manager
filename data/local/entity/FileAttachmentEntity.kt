@Entity(tableName = "file_attachments")
data class FileAttachmentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val size: Long,
    val type: String,
    val uploadedBy: String,
    val uploadedAt: Long,
    val taskId: String?,
    val projectId: String?,
    val lastSyncedAt: Long = 0
)
