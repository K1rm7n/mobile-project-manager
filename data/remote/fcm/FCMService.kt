class FCMService @Inject constructor(
    private val context: Context
) {
    private val fcm = FirebaseMessaging.getInstance()
    
    suspend fun subscribeToTopic(topic: String): Result<Unit> {
        return try {
            fcm.subscribeToTopic(topic).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun unsubscribeFromTopic(topic: String): Result<Unit> {
        return try {
            fcm.unsubscribeFromTopic(topic).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getToken(): Result<String> {
        return try {
            val token = fcm.token.await()
            Result.success(token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
