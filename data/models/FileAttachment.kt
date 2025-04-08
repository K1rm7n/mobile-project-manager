data class FileAttachment(
    val id: String = "",
    val name: String = "",
    val url: String = "",
    val size: Long = 0,
    val type: String = "",
    val uploadedBy: String = "",
    val uploadedAt: Long = System.currentTimeMillis()
)
