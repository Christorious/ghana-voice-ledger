package com.voiceledger.ghana.presentation.feedback

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onNavigateBack: () -> Unit,
    viewModel: FeedbackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(uiState.isSubmitted) {
        if (uiState.isSubmitted) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.feedback_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Feedback type selection
            FeedbackTypeSection(
                selectedType = uiState.feedbackType,
                onTypeSelected = viewModel::onFeedbackTypeSelected
            )

            // Rating section
            RatingSection(
                rating = uiState.rating,
                onRatingChanged = viewModel::onRatingChanged
            )

            // Category selection
            CategorySection(
                selectedCategory = uiState.category,
                onCategorySelected = viewModel::onCategorySelected
            )

            // Description input
            DescriptionSection(
                description = uiState.description,
                onDescriptionChanged = viewModel::onDescriptionChanged
            )

            // Contact information (optional)
            ContactSection(
                email = uiState.email,
                onEmailChanged = viewModel::onEmailChanged,
                includeDeviceInfo = uiState.includeDeviceInfo,
                onIncludeDeviceInfoChanged = viewModel::onIncludeDeviceInfoChanged
            )

            // Error display
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Submit button
            Button(
                onClick = viewModel::submitFeedback,
                modifier = Modifier.fillMaxWidth(),
                enabled = uiState.canSubmit && !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(stringResource(R.string.submit_feedback))
            }
        }
    }
}

@Composable
private fun FeedbackTypeSection(
    selectedType: FeedbackType,
    onTypeSelected: (FeedbackType) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.feedback_type),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Column(modifier = Modifier.selectableGroup()) {
            FeedbackType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = selectedType == type,
                            onClick = { onTypeSelected(type) },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedType == type,
                        onClick = null
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Icon(
                        imageVector = type.icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Column {
                        Text(
                            text = stringResource(type.titleRes),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = stringResource(type.descriptionRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingSection(
    rating: Int,
    onRatingChanged: (Int) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.overall_rating),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(5) { index ->
                val starIndex = index + 1
                IconButton(
                    onClick = { onRatingChanged(starIndex) }
                ) {
                    Icon(
                        imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Rate $starIndex stars",
                        tint = if (starIndex <= rating) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        if (rating > 0) {
            Text(
                text = getRatingDescription(rating),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategorySection(
    selectedCategory: FeedbackCategory,
    onCategorySelected: (FeedbackCategory) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.feedback_category),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.height(200.dp)
        ) {
            items(FeedbackCategory.entries) { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { onCategorySelected(category) },
                    label = { 
                        Text(
                            text = stringResource(category.titleRes),
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun DescriptionSection(
    description: String,
    onDescriptionChanged: (String) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.feedback_description),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = description,
            onValueChange = onDescriptionChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            placeholder = { Text(stringResource(R.string.feedback_description_placeholder)) },
            maxLines = 5,
            supportingText = {
                Text("${description.length}/500")
            }
        )
    }
}

@Composable
private fun ContactSection(
    email: String,
    onEmailChanged: (String) -> Unit,
    includeDeviceInfo: Boolean,
    onIncludeDeviceInfoChanged: (Boolean) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.contact_information),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.email_optional)) },
            placeholder = { Text("your.email@example.com") },
            leadingIcon = {
                Icon(Icons.Default.Email, contentDescription = null)
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = includeDeviceInfo,
                onCheckedChange = onIncludeDeviceInfoChanged
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.include_device_info),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = stringResource(R.string.include_device_info_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun getRatingDescription(rating: Int): String {
    return when (rating) {
        1 -> stringResource(R.string.rating_very_poor)
        2 -> stringResource(R.string.rating_poor)
        3 -> stringResource(R.string.rating_average)
        4 -> stringResource(R.string.rating_good)
        5 -> stringResource(R.string.rating_excellent)
        else -> ""
    }
}

enum class FeedbackType(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
) {
    BUG_REPORT(
        titleRes = R.string.bug_report,
        descriptionRes = R.string.bug_report_description,
        icon = Icons.Default.BugReport
    ),
    FEATURE_REQUEST(
        titleRes = R.string.feature_request,
        descriptionRes = R.string.feature_request_description,
        icon = Icons.Default.Lightbulb
    ),
    GENERAL_FEEDBACK(
        titleRes = R.string.general_feedback,
        descriptionRes = R.string.general_feedback_description,
        icon = Icons.Default.Feedback
    )
}

enum class FeedbackCategory(
    val titleRes: Int,
    val icon: ImageVector
) {
    VOICE_RECOGNITION(R.string.voice_recognition, Icons.Default.RecordVoiceOver),
    USER_INTERFACE(R.string.user_interface, Icons.Default.Smartphone),
    PERFORMANCE(R.string.performance, Icons.Default.Speed),
    ACCURACY(R.string.accuracy, Icons.Default.Check),
    BATTERY_USAGE(R.string.battery_usage, Icons.Default.BatteryFull),
    OFFLINE_MODE(R.string.offline_mode, Icons.Default.CloudOff),
    DAILY_SUMMARIES(R.string.daily_summaries, Icons.Default.Assessment),
    OTHER(R.string.other, Icons.Default.MoreHoriz)
}