class FirebaseStorageService @Inject constructor() {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
    suspend fun uploadFile(uri: Uri, path: String): Result<String> {
        return try {
            val fileRef = storageRef.child(path)
            val uploadTask = fileRef.putFile(uri).await()
            val downloadUrl = fileRef.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteFile(path: String): Result<Unit> {
        return try {
            val fileRef = storageRef.child(path)
            fileRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
