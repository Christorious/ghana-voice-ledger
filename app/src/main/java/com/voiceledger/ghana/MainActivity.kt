package com.voiceledger.ghana

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.voiceledger.ghana.ui.LedgerScreen
import com.voiceledger.ghana.ui.LedgerViewModel
import com.voiceledger.ghana.ui.theme.VoiceLedgerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: LedgerViewModel by viewModels()

    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spoken = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                if (!spoken.isNullOrBlank()) viewModel.recordFromText(spoken)
            }
        }

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchSpeech()
            } else {
                Toast.makeText(
                    this,
                    "Microphone permission is needed to record by voice",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VoiceLedgerTheme {
                LedgerScreen(viewModel = viewModel, onStartVoice = ::onStartVoice)
            }
        }
    }

    private fun onStartVoice() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) launchSpeech() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun launchSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Say your sale, e.g. \"sold 3 tilapia for 20 cedis\""
            )
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Speech recognition isn't available on this device",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
