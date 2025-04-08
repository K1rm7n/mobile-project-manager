@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // Database
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    // DAOs
    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao = database.chatMessageDao()

    @Provides
    fun provideFileAttachmentDao(database: AppDatabase): FileAttachmentDao = database.fileAttachmentDao()

    // Remote Services
    @Provides
    @Singleton
    fun provideFirebaseAuthService(): FirebaseAuthService = FirebaseAuthService()

    @Provides
    @Singleton
    fun provideFirebaseDatabaseService(): FirebaseDatabaseService = FirebaseDatabaseService()

    @Provides
    @Singleton
    fun provideFirebaseStorageService(): FirebaseStorageService = FirebaseStorageService()

    @Provides
    @Singleton
    fun provideFCMService(@ApplicationContext context: Context): FCMService = FCMService(context)

    // Utils
    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils = NetworkUtils(context)

    // Repositories
    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        firebaseDatabaseService: FirebaseDatabaseService,
        networkUtils: NetworkUtils
    ): UserRepository = UserRepository(userDao, firebaseDatabaseService, networkUtils)

    @Provides
    @Singleton
    fun provideProjectRepository(
        projectDao: ProjectDao,
        taskDao: TaskDao,
        firebaseDatabaseService: FirebaseDatabaseService,
        networkUtils: NetworkUtils
    ): ProjectRepository = ProjectRepository(projectDao, taskDao, firebaseDatabaseService, networkUtils)

    @Provides
    @Singleton
    fun provideTaskRepository(
        taskDao: TaskDao,
        firebaseDatabaseService: FirebaseDatabaseService,
        networkUtils: NetworkUtils
    ): TaskRepository = TaskRepository(taskDao, firebaseDatabaseService, networkUtils)

    @Provides
    @Singleton
    fun provideChatRepository(
        chatMessageDao: ChatMessageDao,
        firebaseDatabaseService: FirebaseDatabaseService,
        networkUtils: NetworkUtils
    ): ChatRepository = ChatRepository(chatMessageDao, firebaseDatabaseService, networkUtils)

    @Provides
    @Singleton
    fun provideFileRepository(
        fileAttachmentDao: FileAttachmentDao,
        firebaseStorageService: FirebaseStorageService,
        firebaseDatabaseService: FirebaseDatabaseService,
        networkUtils: NetworkUtils
    ): FileRepository = FileRepository(fileAttachmentDao, firebaseStorageService, firebaseDatabaseService, networkUtils)
}
