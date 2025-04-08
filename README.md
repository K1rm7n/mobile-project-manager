Project Structure

app/
├── build.gradle          # App-level build configuration
├── src/
    ├── main/
        ├── java/com/example/taskmanager/
            ├── data/
            │   ├── local/           # Room database implementation
            │   │   ├── dao/         # Data access objects
            │   │   ├── entity/      # Room entities
            │   │   └── AppDatabase.kt
            │   ├── remote/          # Firebase implementations
            │   │   ├── auth/        # Authentication service
            │   │   ├── storage/     # File storage service
            │   │   ├── database/    # Realtime database service
            │   │   └── fcm/         # Firebase messaging service
            │   ├── model/           # Data models
            │   └── repository/      # Repository implementations
            ├── di/                  # Dependency injection
            ├── ui/
            │   ├── auth/            # Authentication screens
            │   ├── projects/        # Project management screens
            │   ├── tasks/           # Task management screens
            │   ├── analytics/       # Analytics screens
            │   ├── chat/            # Chat screens
            │   ├── files/           # File management screens
            │   ├── components/      # Reusable UI components
            │   └── theme/           # App theme
            ├── util/                # Utility classes
            ├── viewmodel/           # ViewModels
            └── MainActivity.kt
        ├── res/                     # Resources
        └── AndroidManifest.xml
├── build.gradle                     # Project-level build configuration
