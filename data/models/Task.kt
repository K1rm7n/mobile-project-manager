data class Task(
    val id: String = "",
    val projectId: String = "",
    val title: String = "",
    val description: String = "",
    val assignedTo: String? = null,
    val createdBy: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val deadline: Long? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val attachments: List<String> = emptyList()
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH, URGENT
}

enum class TaskStatus {
    TODO, IN_PROGRESS, REVIEW, COMPLETED
}
