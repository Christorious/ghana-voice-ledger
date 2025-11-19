package com.voiceledger.ghana.presentation.tutorial

import androidx.compose.animation.*
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
fun TutorialScreen(
    tutorialType: TutorialType,
    onTutorialComplete: () -> Unit,
    viewModel: TutorialViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val tutorials = getTutorialsForType(tutorialType)
    val pagerState = rememberPagerState(pageCount = { tutorials.size })
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(tutorialType) {
        viewModel.startTutorial(tutorialType)
    }
    
    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onTutorialComplete()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = stringResource(getTutorialTitle(tutorialType)),
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onTutorialComplete) {
                        Icon(Icons.Default.Close, contentDescription = "Close tutorial")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.completeTutorial() }
                    ) {
                        Text(stringResource(R.string.skip))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress indicator
            LinearProgressIndicator(
                progress = (pagerState.currentPage + 1).toFloat() / tutorials.size,
                modifier = Modifier.fillMaxWidth()
            )
            
            // Tutorial content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                TutorialPageContent(
                    tutorial = tutorials[page],
                    onActionComplete = { action ->
                        viewModel.onTutorialActionComplete(action)
                    }
                )
            }
            
            // Navigation controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Previous button
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.previous))
                    }
                } else {
                    Spacer(modifier = Modifier.width(100.dp))
                }
                
                // Page indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(tutorials.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(if (pagerState.currentPage == index) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outline
                                )
                        )
                    }
                }
                
                // Next/Complete button
                Button(
                    onClick = {
                        if (pagerState.currentPage < tutorials.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            viewModel.completeTutorial()
                        }
                    }
                ) {
                    Text(
                        if (pagerState.currentPage < tutorials.size - 1)
                            stringResource(R.string.next)
                        else
                            stringResource(R.string.complete)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        if (pagerState.currentPage < tutorials.size - 1)
                            Icons.Default.ArrowForward
                        else
                            Icons.Default.Check,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun TutorialPageContent(
    tutorial: Tutorial,
    onActionComplete: (TutorialAction) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tutorial illustration
        Card(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = tutorial.icon,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = stringResource(tutorial.titleRes),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = stringResource(tutorial.descriptionRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Interactive content based on tutorial type
        when (tutorial.type) {
            TutorialStepType.INFORMATION -> {
                // Just informational content
            }
            TutorialStepType.VOICE_DEMO -> {
                VoiceDemoContent(
                    tutorial = tutorial,
                    onActionComplete = onActionComplete
                )
            }
            TutorialStepType.INTERACTIVE -> {
                InteractiveContent(
                    tutorial = tutorial,
                    onActionComplete = onActionComplete
                )
            }
            TutorialStepType.PRACTICE -> {
                PracticeContent(
                    tutorial = tutorial,
                    onActionComplete = onActionComplete
                )
            }
        }
        
        // Tips section
        if (tutorial.tips.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.tips),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    tutorial.tips.forEach { tipRes ->
                        Text(
                            text = "• ${stringResource(tipRes)}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceDemoContent(
    tutorial: Tutorial,
    onActionComplete: (TutorialAction) -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Play demo button
        FloatingActionButton(
            onClick = {
                isPlaying = !isPlaying
                if (isPlaying) {
                    // Play voice demo
                    onActionComplete(TutorialAction.PLAY_DEMO)
                }
            },
            containerColor = if (isPlaying) 
                MaterialTheme.colorScheme.error 
            else 
                MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Stop demo" else "Play demo"
            )
        }
        
        Text(
            text = if (isPlaying) 
                stringResource(R.string.playing_demo) 
            else 
                stringResource(R.string.tap_to_play_demo),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        // Example phrases
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.example_phrases),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                tutorial.examplePhrases.forEach { phraseRes ->
                    Text(
                        text = "\"${stringResource(phraseRes)}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveContent(
    tutorial: Tutorial,
    onActionComplete: (TutorialAction) -> Unit
) {
    var hasCompleted by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Interactive instruction
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
                Icon(
                    Icons.Default.TouchApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(tutorial.instructionRes ?: R.string.follow_instructions),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Action button
        Button(
            onClick = {
                hasCompleted = true
                onActionComplete(TutorialAction.COMPLETE_STEP)
            },
            enabled = !hasCompleted
        ) {
            if (hasCompleted) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.completed))
            } else {
                Text(stringResource(R.string.try_it_now))
            }
        }
    }
}

@Composable
private fun PracticeContent(
    tutorial: Tutorial,
    onActionComplete: (TutorialAction) -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var practiceComplete by remember { mutableStateOf(false) }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Practice instruction
        Text(
            text = stringResource(R.string.practice_instruction),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        // Record button
        FloatingActionButton(
            onClick = {
                isRecording = !isRecording
                if (isRecording) {
                    onActionComplete(TutorialAction.START_PRACTICE)
                } else {
                    practiceComplete = true
                    onActionComplete(TutorialAction.COMPLETE_PRACTICE)
                }
            },
            containerColor = when {
                practiceComplete -> MaterialTheme.colorScheme.tertiary
                isRecording -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.primary
            }
        ) {
            Icon(
                imageVector = when {
                    practiceComplete -> Icons.Default.Check
                    isRecording -> Icons.Default.Stop
                    else -> Icons.Default.Mic
                },
                contentDescription = when {
                    practiceComplete -> "Practice complete"
                    isRecording -> "Stop recording"
                    else -> "Start recording"
                }
            )
        }
        
        // Status text
        Text(
            text = when {
                practiceComplete -> stringResource(R.string.practice_complete)
                isRecording -> stringResource(R.string.recording_in_progress)
                else -> stringResource(R.string.tap_to_start_practice)
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

private fun getTutorialsForType(type: TutorialType): List<Tutorial> {
    return when (type) {
        TutorialType.FIRST_TIME_USER -> getFirstTimeUserTutorials()
        TutorialType.VOICE_TRAINING -> getVoiceTrainingTutorials()
        TutorialType.TRANSACTION_RECORDING -> getTransactionRecordingTutorials()
        TutorialType.DAILY_SUMMARIES -> getDailySummariesTutorials()
        TutorialType.OFFLINE_MODE -> getOfflineModeTutorials()
    }
}

private fun getTutorialTitle(type: TutorialType): Int {
    return when (type) {
        TutorialType.FIRST_TIME_USER -> R.string.tutorial_first_time_user
        TutorialType.VOICE_TRAINING -> R.string.tutorial_voice_training
        TutorialType.TRANSACTION_RECORDING -> R.string.tutorial_transaction_recording
        TutorialType.DAILY_SUMMARIES -> R.string.tutorial_daily_summaries
        TutorialType.OFFLINE_MODE -> R.string.tutorial_offline_mode
    }
}

// Tutorial data classes and enums would be defined here
data class Tutorial(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val icon: ImageVector,
    val type: TutorialStepType,
    val instructionRes: Int? = null,
    val examplePhrases: List<Int> = emptyList(),
    val tips: List<Int> = emptyList()
)

enum class TutorialType {
    FIRST_TIME_USER,
    VOICE_TRAINING,
    TRANSACTION_RECORDING,
    DAILY_SUMMARIES,
    OFFLINE_MODE
}

enum class TutorialStepType {
    INFORMATION,
    VOICE_DEMO,
    INTERACTIVE,
    PRACTICE
}

enum class TutorialAction {
    PLAY_DEMO,
    COMPLETE_STEP,
    START_PRACTICE,
    COMPLETE_PRACTICE
}

// Helper functions to get tutorial content for each type
private fun getFirstTimeUserTutorials(): List<Tutorial> {
            icon = Icons.Default.RecordVoiceOver,
            type = TutorialStepType.PRACTICE
        ),
        Tutorial(
            id = "first_transaction",
            titleRes = R.string.tutorial_first_transaction_title,
            descriptionRes = R.string.tutorial_first_transaction_description,
            icon = Icons.Default.Add,
            type = TutorialStepType.INTERACTIVE,
            examplePhrases = listOf(
                R.string.example_phrase_1,
                R.string.example_phrase_2
            )
        )
    )
}

private fun getVoiceTrainingTutorials(): List<Tutorial> {
    return listOf(
        Tutorial(
            id = "voice_training_intro",
            titleRes = R.string.voice_training_intro_title,
            descriptionRes = R.string.voice_training_intro_description,
            icon = Icons.Default.School,
            type = TutorialStepType.INFORMATION
        ),
        Tutorial(
            id = "voice_training_practice",
            titleRes = R.string.voice_training_practice_title,
            descriptionRes = R.string.voice_training_practice_description,
            icon = Icons.Default.Mic,
            type = TutorialStepType.PRACTICE,
            tips = listOf(
                R.string.voice_training_tip_1,
                R.string.voice_training_tip_2
            )
        )
    )
}

private fun getTransactionRecordingTutorials(): List<Tutorial> {
    return listOf(
        Tutorial(
            id = "transaction_basics",
            titleRes = R.string.transaction_basics_title,
            descriptionRes = R.string.transaction_basics_description,
            icon = Icons.Default.Receipt,
            type = TutorialStepType.VOICE_DEMO,
            examplePhrases = listOf(
                R.string.transaction_example_1,
                R.string.transaction_example_2,
                R.string.transaction_example_3
            )
        )
    )
}

private fun getDailySummariesTutorials(): List<Tutorial> {
    return listOf(
        Tutorial(
            id = "daily_summaries_intro",
            titleRes = R.string.daily_summaries_intro_title,
            descriptionRes = R.string.daily_summaries_intro_description,
            icon = Icons.Default.Analytics,
            type = TutorialStepType.INFORMATION
        )
    )
}

private fun getOfflineModeTutorials(): List<Tutorial> {
    return listOf(
        Tutorial(
            id = "offline_mode_intro",
            titleRes = R.string.offline_mode_intro_title,
            descriptionRes = R.string.offline_mode_intro_description,
            icon = Icons.Default.CloudOff,
            type = TutorialStepType.INFORMATION
        )
    )
}