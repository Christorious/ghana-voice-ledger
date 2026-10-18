package com.voiceledger.ghana.offline

import android.util.Log
import com.voiceledger.ghana.data.local.entity.Transaction
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The offline-first repository is written against a remote API that does not exist:
 * Voice Ledger is local-only by design, and there is no server to sync a trader's
 * ledger to.
 *
 * This binding makes that explicit instead of leaving the dependency dangling. Reads
 * return nothing; writes log and discard. Nothing silently claims to have synced, and
 * no sale is ever lost to a half-written network path, because there is no network
 * path.
 *
 * If a backend is ever added, replace this binding — do not edit it into a real
 * client, so the "no server" state stays a deliberate, visible choice.
 */
@Singleton
class NoRemoteTransactionApi @Inject constructor() : RemoteTransactionApi {

    override suspend fun getTransaction(id: String): Transaction? = null

    override suspend fun syncTransaction(transaction: Transaction) {
        Log.d(TAG, "No remote backend configured; sync of ${transaction.id} skipped")
    }

    override suspend fun getUpdatedTransactions(since: Long): List<Transaction> = emptyList()

    override suspend fun deleteTransaction(id: String) {
        Log.d(TAG, "No remote backend configured; remote delete of $id skipped")
    }

    private companion object {
        const val TAG = "NoRemoteTransactionApi"
    }
}
