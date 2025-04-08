data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.MEMBER,
    val profileImage: String? = null
)

enum class UserRole {
    ADMIN, PROJECT_MANAGER, MEMBER
}
