@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val createdBy: String,
    val createdAt: Long,
    val deadline: Long?,
    val members: String, // Storing as JSON string
    val status: String,
    val lastSyncedAt: Long = 0
)
