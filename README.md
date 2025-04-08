TaskManager - Project Structure
app/
├── build.gradle          # App-level build configuration
├── src/
    ├── main/
        ├── java/com/example/taskmanager/
            ├── data/
            │   ├── local/                 # Room database implementation
            │   │   ├── dao/               # Data access objects
            │   │   │   ├── UserDao.kt
            │   │   │   ├── ProjectDao.kt
            │   │   │   ├── TaskDao.kt
            │   │   │   ├── ChatMessageDao.kt
            │   │   │   └── FileAttachmentDao.kt
            │   │   ├── entity/            # Room entities
            │   │   │   ├── UserEntity.kt
            │   │   │   ├── ProjectEntity.kt
            │   │   │   ├── TaskEntity.kt
            │   │   │   ├── ChatMessageEntity.kt
            │   │   │   └── FileAttachmentEntity.kt
            │   │   └── AppDatabase.kt
            │   ├── remote/                # Firebase implementations
            │   │   ├── auth/              # Authentication service
            │   │   │   └── FirebaseAuthService.kt
            │   │   ├── storage/           # File storage service
            │   │   │   └── FirebaseStorageService.kt
            │   │   ├── database/          # Realtime database service
            │   │   │   └── FirebaseDatabaseService.kt
            │   │   └── fcm/               # Firebase messaging service
            │   │       └── FCMService.kt
            │   ├── model/                 # Data models
            │   │   ├── User.kt
            │   │   ├── Project.kt
            │   │   ├── Task.kt
            │   │   ├── ChatMessage.kt
            │   │   └── FileAttachment.kt
            │   └── repository/            # Repository implementations
            │       ├── UserRepository.kt
            │       ├── ProjectRepository.kt
            │       ├── TaskRepository.kt
            │       ├── ChatRepository.kt
            │       └── FileRepository.kt
            ├── di/                        # Dependency injection
            │   └── AppModule.kt
            ├── ui/
            │   ├── auth/                  # Authentication screens
            │   │   ├── LoginScreen.kt
            │   │   └── RegisterScreen.kt
            │   ├── projects/              # Project management screens
            │   │   ├── ProjectScreen.kt
            │   │   ├── ProjectDetailScreen.kt
            │   │   └── ProjectListScreen.kt
            │   ├── tasks/                 # Task management screens
            │   │   ├── TaskScreen.kt
            │   │   └── TaskDetailScreen.kt
            │   ├── analytics/             # Analytics screens
            │   │   └── AnalyticsScreen.kt
            │   ├── chat/                  # Chat screens
            │   │   └── ChatScreen.kt
            │   ├── files/                 # File management screens
            │   │   └── FileListScreen.kt
            │   ├── components/            # Reusable UI components
            │   │   ├── ProjectItem.kt
            │   │   ├── TaskItem.kt
            │   │   └── UserAvatar.kt
            │   └── theme/                 # App theme
            │       ├── Color.kt
            │       ├── Shape.kt
            │       ├── Theme.kt
            │       └── Type.kt
            ├── util/                      # Utility classes
            │   ├── NetworkUtils.kt
            │   ├── DateUtils.kt
            │   └── ValidationUtils.kt
            ├── viewmodel/                 # ViewModels
            │   ├── AuthViewModel.kt
            │   ├── ProjectViewModel.kt
            │   ├── TaskViewModel.kt
            │   ├── AnalyticsViewModel.kt
            │   ├── ChatViewModel.kt
            │   └── FileViewModel.kt
            ├── worker/                    # Background workers
            │   ├── SyncWorker.kt
            │   └── DeleteWorker.kt
            └── MainActivity.kt
        ├── res/                           # Resources
        └── AndroidManifest.xml
build.gradle                              # Project-level build configuration
