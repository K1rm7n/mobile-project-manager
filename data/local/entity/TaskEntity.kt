@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val description: String,
    val assignedTo: String?,
    val createdBy: String,
    val createdAt: Long,
    val deadline: Long?,
    val priority: String,
    val status: String,
    val lastSyncedAt: Long = 0
)
