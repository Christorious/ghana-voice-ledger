package com.voiceledger.ghana.presentation.summary

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voiceledger.ghana.domain.service.PresentationSection
import com.voiceledger.ghana.domain.service.SectionType
import com.voiceledger.ghana.domain.service.SummaryPresentation
import com.voiceledger.ghana.presentation.theme.VoiceLedgerTheme

/**
 * Screen for presenting daily summaries with voice output
 * Displays formatted summary sections with voice controls
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryPresentationScreen(
    summaryDate: String,
    onNavigateBack: () -> Unit = {},
    viewModel: SummaryPresentationViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presentationState by viewModel.presentationState.collectAsStateWithLifecycle()
    
    // Load summary when screen opens
    LaunchedEffect(summaryDate) {
        viewModel.loadSummary(summaryDate)
    }
    
    // Handle messages and errors
    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            viewModel.clearMessage()
        }
    }
    
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            viewModel.clearError()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Daily Summary",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // Language selection
                var showLanguageMenu by remember { mutableStateOf(false) }
                
                Box {
                    IconButton(onClick = { showLanguageMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Select language"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("English") },
                            onClick = {
                                viewModel.setLanguage("en")
                                showLanguageMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Twi") },
                            onClick = {
                                viewModel.setLanguage("tw")
                                showLanguageMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Ga") },
                            onClick = {
                                viewModel.setLanguage("ga")
                                showLanguageMenu = false
                            }
                        )
                    }
                }
            }
        )
        
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Generating summary...")
                }
            }
        } else if (uiState.presentation != null) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Voice controls
                item {
                    VoiceControlsCard(
                        isInitialized = presentationState.isInitialized,
                        isSpeaking = presentationState.isSpeaking,
                        duration = uiState.presentation.duration,
                        onPlayPause = {
                            if (presentationState.isSpeaking) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakSummary()
                            }
                        },
                        onSpeedChange = viewModel::setSpeechRate,
                        onPitchChange = viewModel::setSpeechPitch
                    )
                }
                
                // Summary sections
                items(uiState.presentation.sections) { section ->
                    SummarySection(
                        section = section,
                        language = uiState.presentation.language
                    )
                }
            }
        } else if (uiState.error != null) {
            ErrorCard(
                error = uiState.error,
                onRetry = { viewModel.loadSummary(summaryDate) }
            )
        } else {
            EmptyStateCard()
        }
    }
}

@Composable
private fun VoiceControlsCard(
    isInitialized: Boolean,
    isSpeaking: Boolean,
    duration: Int,
    onPlayPause: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Voice Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Text(
                    text = "${duration}s",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play/Pause button
                FloatingActionButton(
                    onClick = onPlayPause,
                    modifier = Modifier.size(56.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isSpeaking) "Pause" else "Play",
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Column(
                    modifier = Modifier.weight(1f).padding(start = 16.dp)
                ) {
                    // Speech rate control
                    var speechRate by remember { mutableStateOf(1.0f) }
                    
                    Text(
                        text = "Speed: ${String.format("%.1f", speechRate)}x",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    Slider(
                        value = speechRate,
                        onValueChange = { 
                            speechRate = it
                            onSpeedChange(it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 6
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Speech pitch control
                    var speechPitch by remember { mutableStateOf(1.0f) }
                    
                    Text(
                        text = "Pitch: ${String.format("%.1f", speechPitch)}x",
                        style = MaterialTheme.typography.labelMedium
                    )
                    
                    Slider(
                        value = speechPitch,
                        onValueChange = { 
                            speechPitch = it
                            onPitchChange(it)
                        },
                        valueRange = 0.5f..2.0f,
                        steps = 6
                    )
                }
            }
            
            if (!isInitialized) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Voice synthesis not available",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun SummarySection(
    section: PresentationSection,
    language: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = getSectionIcon(section.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Text(
                text = section.content,
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
            )
        }
    }
}

@Composable
private fun ErrorCard(
    error: String,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Error Loading Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.SpeakerNotes,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Summary Available",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Generate a daily summary first to view it here.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getSectionIcon(type: SectionType): ImageVector {
    return when (type) {
        SectionType.HEADER -> Icons.Default.Today
        SectionType.METRICS -> Icons.Default.Analytics
        SectionType.PRODUCTS -> Icons.Default.Inventory
        SectionType.CUSTOMERS -> Icons.Default.People
        SectionType.TIME -> Icons.Default.Schedule
        SectionType.COMPARISON -> Icons.Default.TrendingUp
        SectionType.RECOMMENDATIONS -> Icons.Default.Lightbulb
    }
}

@Preview(showBackground = true)
@Composable
private fun SummaryPresentationScreenPreview() {
    VoiceLedgerTheme {
        // Preview would need mock data
    }
}