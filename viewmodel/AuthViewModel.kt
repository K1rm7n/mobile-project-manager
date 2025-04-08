@HiltViewModel
class AuthViewModel @Inject constructor(
    private val firebaseAuthService: FirebaseAuthService,
    private val userRepository: UserRepository
) : ViewModel() {
    
    // User state
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    // Auth form states
    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Authentication state
    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    // Error states
    private val _emailError = MutableStateFlow<String?>(null)
    val emailError: StateFlow<String?> = _emailError.asStateFlow()
    
    private val _passwordError = MutableStateFlow<String?>(null)
    val passwordError: StateFlow<String?> = _passwordError.asStateFlow()
    
    private val _nameError = MutableStateFlow<String?>(null)
    val nameError: StateFlow<String?> = _nameError.asStateFlow()
    
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()
    
    init {
        checkAuthState()
    }
    
    private fun checkAuthState() {
        viewModelScope.launch {
            val user = firebaseAuthService.getCurrentUser()
            _isAuthenticated.value = user != null
            
            user?.let { firebaseUser ->
                // Load complete user profile from repository
                userRepository.getCurrentUser(firebaseUser.id)
                    .collect { fullUser ->
                        _currentUser.value = fullUser ?: firebaseUser
                    }
            }
        }
    }
    
    fun setEmail(email: String) {
        _email.value = email
        validateEmail()
    }
    
    fun setPassword(password: String) {
        _password.value = password
        validatePassword()
    }
    
    fun setName(name: String) {
        _name.value = name
        validateName()
    }
    
    private fun validateEmail(): Boolean {
        val result = ValidationUtils.validateEmail(_email.value)
        _emailError.value = if (result is ValidationUtils.ValidationResult.Invalid) {
            result.message
        } else {
            null
        }
        return result is ValidationUtils.ValidationResult.Valid
    }
    
    private fun validatePassword(): Boolean {
        val result = ValidationUtils.validatePassword(_password.value)
        _passwordError.value = if (result is ValidationUtils.ValidationResult.Invalid) {
            result.message
        } else {
            null
        }
        return result is ValidationUtils.ValidationResult.Valid
    }
    
    private fun validateName(): Boolean {
        val result = ValidationUtils.validateName(_name.value)
        _nameError.value = if (result is ValidationUtils.ValidationResult.Invalid) {
            result.message
        } else {
            null
        }
        return result is ValidationUtils.ValidationResult.Valid
    }
    
    fun signIn() {
        if (!validateEmail() || !validatePassword()) {
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            
            firebaseAuthService.signIn(_email.value, _password.value)
                .onSuccess { user ->
                    _isAuthenticated.value = true
                    _currentUser.value = user
                    clearForm()
                }
                .onFailure { e ->
                    _authError.value = e.message ?: "Authentication failed"
                }
            
            _isLoading.value = false
        }
    }
    
    fun signUp() {
        if (!validateEmail() || !validatePassword() || !validateName()) {
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            _authError.value = null
            
            firebaseAuthService.signUp(_name.value, _email.value, _password.value)
                .onSuccess { user ->
                    // Create user profile in database
                    val newUser = User(
                        id = user.id,
                        name = user.name,
                        email = user.email,
                        role = UserRole.MEMBER
                    )
                    
                    userRepository.updateUserProfile(newUser)
                        .onSuccess {
                            _isAuthenticated.value = true
                            _currentUser.value = newUser
                            clearForm()
                        }
                        .onFailure { e ->
                            _authError.value = e.message ?: "Failed to create user profile"
                        }
                }
                .onFailure { e ->
                    _authError.value = e.message ?: "Registration failed"
                }
            
            _isLoading.value = false
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            firebaseAuthService.signOut()
            _isAuthenticated.value = false
            _currentUser.value = null
        }
    }
    
    fun clearForm() {
        _email.value = ""
        _password.value = ""
        _name.value = ""
        _emailError.value = null
        _passwordError.value = null
        _nameError.value = null
        _authError.value = null
    }
    
    fun clearErrors() {
        _emailError.value = null
        _passwordError.value = null
        _nameError.value = null
        _authError.value = null
    }
}
