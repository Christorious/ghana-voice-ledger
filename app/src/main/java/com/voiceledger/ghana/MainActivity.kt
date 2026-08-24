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
import com.voiceledger.ghana.ui.AppRoot
import com.voiceledger.ghana.ui.CreditViewModel
import com.voiceledger.ghana.ui.InsightsViewModel
import com.voiceledger.ghana.ui.LedgerViewModel
import com.voiceledger.ghana.ui.theme.VoiceLedgerTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val ledgerViewModel: LedgerViewModel by viewModels()
    private val insightsViewModel: InsightsViewModel by viewModels()
    private val creditViewModel: CreditViewModel by viewModels()

    /** Where the next voice result should be delivered (set just before launching). */
    private var pendingVoiceTarget: ((String) -> Unit)? = null

    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val target = pendingVoiceTarget
            pendingVoiceTarget = null
            if (result.resultCode == Activity.RESULT_OK && target != null) {
                val spoken = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.firstOrNull()
                if (!spoken.isNullOrBlank()) target(spoken)
            }
        }

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                launchSpeech()
            } else {
                pendingVoiceTarget = null
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
                AppRoot(
                    ledgerViewModel = ledgerViewModel,
                    insightsViewModel = insightsViewModel,
                    creditViewModel = creditViewModel,
                    onRecordSale = { startVoice(ledgerViewModel::beginFromText) },
                    onRecordCredit = { startVoice(creditViewModel::beginCreditFromText) }
                )
            }
        }
    }

    private fun startVoice(target: (String) -> Unit) {
        pendingVoiceTarget = target
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
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            pendingVoiceTarget = null
            Toast.makeText(
                this,
                "Speech recognition isn't available on this device",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
