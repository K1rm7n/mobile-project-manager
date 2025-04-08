@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects")
    fun getAllProjects(): Flow<List<ProjectEntity>>
    
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun getProjectById(projectId: String): Flow<ProjectEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)
    
    @Update
    suspend fun updateProject(project: ProjectEntity)
    
    @Delete
    suspend fun deleteProject(project: ProjectEntity)
    
    @Query("SELECT * FROM projects WHERE createdBy = :userId OR members LIKE '%' || :userId || '%'")
    fun getProjectsForUser(userId: String): Flow<List<ProjectEntity>>
}
