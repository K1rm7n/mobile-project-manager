@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectDialog(
    onDismiss: () -> Unit,
    onCreateProject: (title: String, description: String, deadline: Long?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var titleError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }
    
    fun validateForm(): Boolean {
        val titleValidation = ValidationUtils.validateProjectTitle(title)
        val descriptionValidation = ValidationUtils.validateDescription(description)
        val deadlineValidation = ValidationUtils.validateDeadline(deadline)
        
        titleError = if (titleValidation is ValidationUtils.ValidationResult.Invalid) {
            titleValidation.message
        } else {
            null
        }
        
        descriptionError = if (descriptionValidation is ValidationUtils.ValidationResult.Invalid) {
            descriptionValidation.message
        } else {
            null
        }
        
        if (deadlineValidation is ValidationUtils.ValidationResult.Invalid) {
            // Show deadline error
            return false
        }
        
        return titleError == null && descriptionError == null
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Create New Project",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Project Title") },
                    isError = titleError != null,
                    supportingText = { titleError?.let { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    isError = descriptionError != null,
                    supportingText = { descriptionError?.let { Text(it) } },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select Deadline"
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = deadline?.let { "Deadline: ${DateUtils.formatDate(it)}" } 
                            ?: "Set deadline (optional)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (validateForm()) {
                                onCreateProject(title, description, deadline)
                            }
                        }
                    ) {
                        Text("Create Project")
                    }
                }
            }
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismiss = { showDatePicker = false },
            onDateSelected = { selectedDate ->
                deadline = selectedDate
                showDatePicker = false
            },
            initialDate = deadline ?: System.currentTimeMillis()
        )
    }
}

@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (Long) -> Unit,
    initialDate: Long
) {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = initialDate
    }
    
    var year by remember { mutableStateOf(calendar.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(calendar.get(Calendar.MONTH)) }
    var day by remember { mutableStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Select Deadline",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Simplified date picker UI for the example
                // In a real app, you'd use DatePicker from Material3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Month selector
                    NumberPicker(
                        value = month + 1,
                        onValueChange = { month = it - 1 },
                        range = 1..12,
                        label = "Month"
                    )
                    
                    // Day selector
                    val daysInMonth = when (month + 1) {
                        4, 6, 9, 11 -> 30
                        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
                        else -> 31
                    }
                    
                    NumberPicker(
                        value = day.coerceIn(1, daysInMonth),
                        onValueChange = { day = it },
                        range = 1..daysInMonth,
                        label = "Day"
                    )
                    
                    // Year selector
                    NumberPicker(
                        value = year,
                        onValueChange = { year = it },
                        range = calendar.get(Calendar.YEAR)..calendar.get(Calendar.YEAR) + 10,
                        label = "Year"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            calendar.set(year, month, day)
                            onDateSelected(calendar.timeInMillis)
                        }
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Composable
fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Button(
            onClick = { 
                if (value < range.last) onValueChange(value + 1) 
            },
            modifier = Modifier.width(64.dp)
        ) {
            Text("▲")
        }
        
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleMedium
        )
        
        Button(
            onClick = { 
                if (value > range.first) onValueChange(value - 1) 
            },
            modifier = Modifier.width(64.dp)
        ) {
            Text("▼")
        }
    }
}
