@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val name by viewModel.name.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val nameError by viewModel.nameError.collectAsState()
    val emailError by viewModel.emailError.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val authError by viewModel.authError.collectAsState()
    
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            onRegisterSuccess()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Sign up to get started",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Name field
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.setName(it) },
            label = { Text("Full Name") },
            isError = nameError != null,
            supportingText = { nameError?.let { Text(it) } },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Email field
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.setEmail(it) },
            label = { Text("Email") },
            isError = emailError != null,
            supportingText = { emailError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.setPassword(it) },
            label = { Text("Password") },
            isError = passwordError != null,
            supportingText = { passwordError?.let { Text(it) } },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        
        // Authentication error
        if (authError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = authError ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Register button
        Button(
            onClick = { viewModel.signUp() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Sign Up")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Login link
        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Already have an account? Sign In")
        }
    }
}
