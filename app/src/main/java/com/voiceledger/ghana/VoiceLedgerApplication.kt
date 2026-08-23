package com.voiceledger.ghana

import android.app.Application

/**
 * Application entry point for the Ghana Voice Ledger core.
 *
 * This is the rebuilt "core" app: a small, coherent, compiling slice that delivers the
 * central loop — speak or type a sale, parse it, store it in a Room ledger, and see the
 * running daily total and history. The previous 134-file source set was removed because
 * it had never compiled (pervasive merge corruption); the valuable ideas are being
 * reintroduced here on a foundation that actually builds and runs.
 */
class VoiceLedgerApplication : Application()
