package io.horizontalsystems.litecoinmwebkit

sealed class MwebSyncState {
    object Synced : MwebSyncState()
    class NotSynced(val exception: Throwable) : MwebSyncState()

    /**
     * [progress] in 0..1 estimates the share of the header chain the daemon
     * holds; [stage] tells which part of the sync is running.
     */
    class Syncing(val progress: Double, val stage: Stage) : MwebSyncState()

    enum class Stage { Starting, BlockHeaders, MwebData }

    override fun equals(other: Any?) = when {
        this is Synced && other is Synced -> true
        this is NotSynced && other is NotSynced -> exception == other.exception
        this is Syncing && other is Syncing -> progress == other.progress && stage == other.stage
        else -> false
    }

    override fun hashCode() = javaClass.hashCode()

    override fun toString() = when (this) {
        is Synced -> "Synced"
        is NotSynced -> "NotSynced(${exception.message})"
        is Syncing -> "Syncing(${"%.3f".format(progress)}, $stage)"
    }
}

/**
 * Balance in litoshi. [spendable] counts confirmed outputs not used by a
 * pending send; [unconfirmed] counts outputs seen in the mempool, including
 * change of own pending sends.
 */
data class MwebBalance(val spendable: Long, val unconfirmed: Long) {
    val total get() = spendable + unconfirmed
}

enum class MwebTransactionType { Incoming, Outgoing, PegIn, PegOut, SpentElsewhere }

enum class MwebTransactionStatus { Pending, Confirmed, Failed }

data class MwebTransactionInfo(
    val uid: String,
    val type: MwebTransactionType,
    /** Value received, or sent to the recipient, in litoshi. */
    val amount: Long,
    /** Total fee paid, when this wallet created the transaction. */
    val fee: Long?,
    /** Block time, or null for an output confirmed below the header checkpoint, whose block time is unknown. */
    val timestamp: Long?,
    /** Block height, null while pending. */
    val blockHeight: Int?,
    val status: MwebTransactionStatus,
    val toAddress: String?,
    val ownAddress: String?,
    /** Litecoin transaction hash of a peg-in or peg-out. */
    val txHash: String?,
    val inputIds: List<String>,
    val outputIds: List<String>,
)

/** A spendable MWEB output as exposed to callers. */
data class MwebUtxoInfo(
    val outputId: String,
    val value: Long,
    val address: String,
    val addressIndex: Int,
    val height: Int,
    val blockTime: Long,
)

sealed class MwebSendError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidAddress(message: String) : MwebSendError(message)
    class InsufficientBalance : MwebSendError("Insufficient balance")
    class NotSynced : MwebSendError("Daemon not synced")
    class DustAmount : MwebSendError("Amount below dust limit")
    class WatchOnly : MwebSendError("Spend secret not available")

    /** The daemon rejected the transaction; nothing was broadcast and the recorded row is marked failed. */
    class BroadcastRejected(message: String, cause: Throwable? = null) : MwebSendError(message, cause)

    /**
     * The daemon could not be reached or did not answer in time, so the
     * transaction may or may not have been broadcast. The row stays pending
     * and its inputs stay reserved until the network settles it.
     */
    class BroadcastUnknown(cause: Throwable) : MwebSendError("Broadcast result unknown: ${cause.message}", cause)
}
