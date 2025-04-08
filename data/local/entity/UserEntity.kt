@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val role: String,
    val profileImage: String?,
    val lastSyncedAt: Long = 0
)
