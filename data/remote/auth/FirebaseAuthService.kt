class FirebaseAuthService @Inject constructor() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    
    fun getCurrentUser(): User? {
        return auth.currentUser?.let { firebaseUser ->
            User(
                id = firebaseUser.uid,
                name = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: ""
            )
        }
    }
    
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user?.let { firebaseUser ->
                Result.success(
                    User(
                        id = firebaseUser.uid,
                        name = firebaseUser.displayName ?: "",
                        email = firebaseUser.email ?: ""
                    )
                )
            } ?: Result.failure(Exception("Authentication failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun signUp(name: String, email: String, password: String): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            
            // Update display name
            val profileUpdates = userProfileChangeRequest {
                displayName = name
            }
            
            result.user?.updateProfile(profileUpdates)?.await()
            
            result.user?.let { firebaseUser ->
                Result.success(
                    User(
                        id = firebaseUser.uid,
                        name = name,
                        email = email
                    )
                )
            } ?: Result.failure(Exception("Registration failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun signOut() {
        auth.signOut()
    }
}
