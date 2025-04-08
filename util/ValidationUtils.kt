object ValidationUtils {
    
    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }
    
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Invalid("Email cannot be empty")
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> 
                ValidationResult.Invalid("Please enter a valid email address")
            else -> ValidationResult.Valid
        }
    }
    
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Invalid("Password cannot be empty")
            password.length < 8 -> ValidationResult.Invalid("Password must be at least 8 characters")
            !password.any { it.isDigit() } -> 
                ValidationResult.Invalid("Password must contain at least one digit")
            !password.any { it.isLetter() } -> 
                ValidationResult.Invalid("Password must contain at least one letter")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateName(name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid("Name cannot be empty")
            name.length < 2 -> ValidationResult.Invalid("Name is too short")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateProjectTitle(title: String): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult.Invalid("Project title cannot be empty")
            title.length < 3 -> ValidationResult.Invalid("Project title is too short")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateTaskTitle(title: String): ValidationResult {
        return when {
            title.isBlank() -> ValidationResult.Invalid("Task title cannot be empty")
            title.length < 3 -> ValidationResult.Invalid("Task title is too short")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateDescription(description: String): ValidationResult {
        return when {
            description.isBlank() -> ValidationResult.Invalid("Description cannot be empty")
            else -> ValidationResult.Valid
        }
    }
    
    fun validateDeadline(deadline: Long?): ValidationResult {
        return when {
            deadline == null -> ValidationResult.Valid // Deadline is optional
            deadline < System.currentTimeMillis() -> 
                ValidationResult.Invalid("Deadline cannot be in the past")
            else -> ValidationResult.Valid
        }
    }
}
