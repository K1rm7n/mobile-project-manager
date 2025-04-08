data class Project(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val deadline: Long? = null,
    val members: List<String> = emptyList(),
    val status: ProjectStatus = ProjectStatus.ACTIVE
)

enum class ProjectStatus {
    ACTIVE, COMPLETED, ARCHIVED
}
