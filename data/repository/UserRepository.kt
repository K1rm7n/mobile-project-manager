class UserRepository @Inject constructor(
    private val userDao: UserDao,
    private val firebaseDatabase: FirebaseDatabaseService,
    private val networkUtils: NetworkUtils
) {
    // Get current user's profile
    fun getCurrentUser(userId: String): Flow<User?> {
        // Local data source
        val localUser = userDao.getUserById(userId)
        
        // Remote data source (if online)
        if (networkUtils.isNetworkAvailable()) {
            firebaseDatabase.getUsersReference()
                .child(userId)
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        
                        // Save to local database
                        user?.let {
                            CoroutineScope(Dispatchers.IO).launch {
                                userDao.insertUser(it.toEntity())
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("UserRepository", "Failed to load user: ${error.message}")
                    }
                })
        }
        
        // Return local data source as flow
        return localUser.map { it?.toDomain() }
    }
    
    // Get users by their IDs (e.g., project members)
    fun getUsersByIds(userIds: List<String>): Flow<List<User>> {
        // Local data source
        val localUsers = userDao.getUsersByIds(userIds)
        
        // Remote data source (if online)
        if (networkUtils.isNetworkAvailable()) {
            val usersRef = firebaseDatabase.getUsersReference()
            
            userIds.forEach { userId ->
                usersRef.child(userId).addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val user = snapshot.getValue(User::class.java)
                        
                        // Save to local database
                        user?.let {
                            CoroutineScope(Dispatchers.IO).launch {
                                userDao.insertUser(it.toEntity())
                            }
                        }
                    }
                    
                    override fun onCancelled(error: DatabaseError) {
                        Log.e("UserRepository", "Failed to load user: ${error.message}")
                    }
                })
            }
        }
        
        // Return local data source as flow
        return localUsers.map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    // Update user profile
    suspend fun updateUserProfile(user: User): Result<Unit> {
        return try {
            // Try to update on Firebase first if online
            if (networkUtils.isNetworkAvailable()) {
                val result = firebaseDatabase.getUsersReference()
                    .child(user.id)
                    .setValue(user)
                    .await()
                
                // Update local database
                userDao.insertUser(user.toEntity())
                Result.success(Unit)
            } else {
                // Update offline and sync later
                userDao.insertUser(user.toEntity())
                
                // Schedule sync
                WorkManager.getInstance()
                    .enqueueUniqueWork(
                        "sync_user_${user.id}",
                        ExistingWorkPolicy.REPLACE,
                        SyncWorker.buildRequest()
                    )
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helper methods for conversion between domain models and entities
    private fun User.toEntity(): UserEntity {
        return UserEntity(
            id = this.id,
            name = this.name,
            email = this.email,
            role = this.role.name,
            profileImage = this.profileImage,
            lastSyncedAt = System.currentTimeMillis()
        )
    }
    
    private fun UserEntity.toDomain(): User {
        return User(
            id = this.id,
            name = this.name,
            email = this.email,
            role = UserRole.valueOf(this.role),
            profileImage = this.profileImage
        )
    }
}
