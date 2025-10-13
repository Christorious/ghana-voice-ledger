package com.voiceledger.ghana.presentation.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onOnboardingComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { OnboardingPage.entries.size })
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(uiState.isOnboardingComplete) {
        if (uiState.isOnboardingComplete) {
            onOnboardingComplete()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top bar with skip button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            if (pagerState.currentPage < OnboardingPage.entries.size - 1) {
                TextButton(
                    onClick = { viewModel.completeOnboarding() }
                ) {
                    Text(
                        text = stringResource(R.string.skip),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Pager content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            OnboardingPageContent(
                page = OnboardingPage.entries[page],
                onPermissionGranted = { permission ->
                    viewModel.onPermissionGranted(permission)
                },
                onVoiceEnrollmentComplete = {
                    viewModel.onVoiceEnrollmentComplete()
                }
            )
        }

        // Page indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(OnboardingPage.entries.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 12.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.outline
                        )
                        .padding(horizontal = 4.dp)
                )
            }
        }

        // Navigation buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back button
            if (pagerState.currentPage > 0) {
                OutlinedButton(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                ) {
                    Text(stringResource(R.string.back))
                }
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }

            // Next/Get Started button
            Button(
                onClick = {
                    if (pagerState.currentPage < OnboardingPage.entries.size - 1) {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        viewModel.completeOnboarding()
                    }
                },
                enabled = when (OnboardingPage.entries[pagerState.currentPage]) {
                    OnboardingPage.PERMISSIONS -> uiState.hasRequiredPermissions
                    OnboardingPage.VOICE_SETUP -> uiState.isVoiceEnrolled
                    else -> true
                }
            ) {
                Text(
                    if (pagerState.currentPage < OnboardingPage.entries.size - 1) 
                        stringResource(R.string.next) 
                    else 
                        stringResource(R.string.get_started)
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    onPermissionGranted: (String) -> Unit,
    onVoiceEnrollmentComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icon
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = stringResource(page.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Page-specific content
        when (page) {
            OnboardingPage.WELCOME -> {
                // Welcome content - no additional UI needed
            }
            OnboardingPage.PERMISSIONS -> {
                PermissionsContent(onPermissionGranted = onPermissionGranted)
            }
            OnboardingPage.VOICE_SETUP -> {
                VoiceSetupContent(onVoiceEnrollmentComplete = onVoiceEnrollmentComplete)
            }
            OnboardingPage.FEATURES -> {
                FeaturesContent()
            }
        }
    }
}

@Composable
private fun PermissionsContent(
    onPermissionGranted: (String) -> Unit
) {
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        PermissionCard(
            title = stringResource(R.string.microphone_permission),
            description = stringResource(R.string.microphone_permission_description),
            icon = Icons.Default.Mic,
            onGrantClick = { 
                // Request microphone permission
                onPermissionGranted("android.permission.RECORD_AUDIO")
            }
        )
        
        PermissionCard(
            title = stringResource(R.string.storage_permission),
            description = stringResource(R.string.storage_permission_description),
            icon = Icons.Default.Storage,
            onGrantClick = { 
                // Request storage permission
                onPermissionGranted("android.permission.WRITE_EXTERNAL_STORAGE")
            }
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onGrantClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onGrantClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(R.string.grant_permission))
            }
        }
    }
}

@Composable
private fun VoiceSetupContent(
    onVoiceEnrollmentComplete: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var enrollmentProgress by remember { mutableStateOf(0f) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Voice enrollment progress
        CircularProgressIndicator(
            progress = enrollmentProgress,
            modifier = Modifier.size(80.dp),
            strokeWidth = 6.dp
        )
        
        Text(
            text = stringResource(R.string.voice_enrollment_progress, (enrollmentProgress * 100).toInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        // Recording instructions
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.voice_enrollment_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Record button
        FloatingActionButton(
            onClick = {
                isRecording = !isRecording
                if (isRecording) {
                    // Start voice enrollment
                } else {
                    // Stop voice enrollment
                    if (enrollmentProgress >= 1f) {
                        onVoiceEnrollmentComplete()
                    }
                }
            },
            modifier = Modifier.size(72.dp),
            containerColor = if (isRecording) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) 
                    stringResource(R.string.stop_recording) 
                else 
                    stringResource(R.string.start_recording),
                modifier = Modifier.size(32.dp)
            )
        }
        
        // Simulate progress for demo
        LaunchedEffect(isRecording) {
            if (isRecording) {
                while (isRecording && enrollmentProgress < 1f) {
                    kotlinx.coroutines.delay(100)
                    enrollmentProgress += 0.02f
                }
            }
        }
    }
}

@Composable
private fun FeaturesContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FeatureItem(
            icon = Icons.Default.RecordVoiceOver,
            title = stringResource(R.string.feature_voice_recognition),
            description = stringResource(R.string.feature_voice_recognition_desc)
        )
        
        FeatureItem(
            icon = Icons.Default.Analytics,
            title = stringResource(R.string.feature_daily_summaries),
            description = stringResource(R.string.feature_daily_summaries_desc)
        )
        
        FeatureItem(
            icon = Icons.Default.CloudOff,
            title = stringResource(R.string.feature_offline_mode),
            description = stringResource(R.string.feature_offline_mode_desc)
        )
        
        FeatureItem(
            icon = Icons.Default.Security,
            title = stringResource(R.string.feature_privacy),
            description = stringResource(R.string.feature_privacy_desc)
        )
    }
}

@Composable
private fun FeatureItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class OnboardingPage(
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector
) {
    WELCOME(
        titleRes = R.string.onboarding_welcome_title,
        descriptionRes = R.string.onboarding_welcome_description,
        icon = Icons.Default.Waving_Hand
    ),
    PERMISSIONS(
        titleRes = R.string.onboarding_permissions_title,
        descriptionRes = R.string.onboarding_permissions_description,
        icon = Icons.Default.Security
    ),
    VOICE_SETUP(
        titleRes = R.string.onboarding_voice_setup_title,
        descriptionRes = R.string.onboarding_voice_setup_description,
        icon = Icons.Default.RecordVoiceOver
    ),
    FEATURES(
        titleRes = R.string.onboarding_features_title,
        descriptionRes = R.string.onboarding_features_description,
        icon = Icons.Default.Features
    )
}