package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.bitcoincore.blocks.IPeerSyncListener
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebRawOutput
import io.horizontalsystems.litecoinkit.mweb.network.tasks.GetMwebUtxosTask
import io.horizontalsystems.litecoinkit.mweb.network.tasks.SendMwebTransactionTask
import io.horizontalsystems.litecoinkit.mweb.storage.MwebStorage
import io.horizontalsystems.litecoinkit.mweb.storage.entities.MwebOutput
import java.util.logging.Logger

/**
 * Orchestrates MWEB UTXO syncing.
 *
 * When a peer finishes block sync ([onPeerSynced]), this manager fetches MWEB outputs
 * starting from the last known leaf index. For each completed [GetMwebUtxosTask] it
 * scans the outputs (if a [MwebScanner] is available), persists raw outputs and owned
 * wallet outputs, updates the sync state, and issues the next page request if needed.
 *
 * Only peers advertising NODE_MWEB (service bit 0x20) are used for MWEB requests.
 *
 * When a sync session starts from leaf index 0 (full resync), all returned output IDs
 * are accumulated. On the final page, any previously-unspent wallet output NOT present
 * in the new UTXO set is marked as spent.
 */
class MwebManager(
    private val mwebStorage: MwebStorage,
    private val scanner: MwebScanner?,
    private val lastBlockHashProvider: () -> ByteArray?
) : IPeerSyncListener, IPeerTaskHandler {

    private val logger = Logger.getLogger("MwebManager")

    // Session state for full-resync spend detection
    @Volatile private var currentSyncStartIndex: Long = -1L
    @Volatile private var fullResyncOutputIds: MutableSet<String>? = null

    // Last connected MWEB-capable peer — used for broadcasting spending transactions
    @Volatile private var lastMwebPeer: Peer? = null

    companion object {
        private const val NODE_MWEB = 0x20L
    }

    // IPeerSyncListener
    override fun onPeerSynced(peer: Peer) {
        if (peer.services and NODE_MWEB == 0L) {
            logger.info("MWEB: skipping non-MWEB peer ${peer.host} (services=0x${peer.services.toString(16)})")
            return
        }
        lastMwebPeer = peer
        fetchNextPage(peer)
    }

    /**
     * Broadcasts a serialized MWEB transaction to the last connected MWEB peer.
     * Call after building a transaction with [MwebTransactionBuilder].
     */
    fun broadcastMwebTransaction(mwebTxBytes: ByteArray) {
        val peer = lastMwebPeer ?: throw IllegalStateException("No MWEB peer available — wait for sync to complete")
        peer.addTask(SendMwebTransactionTask(mwebTxBytes))
        logger.info("MWEB: broadcasting transaction (${mwebTxBytes.size} bytes) to ${peer.host}")
    }

    // IPeerTaskHandler
    @Synchronized
    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        if (task !is GetMwebUtxosTask) return false

        val outputs = task.receivedOutputs
        val isFullResync = currentSyncStartIndex == 0L && fullResyncOutputIds != null

        if (outputs.isNotEmpty()) {
            val blockHashHex = task.blockHash.joinToString("") { "%02x".format(it) }

            val (rawOutputs, walletOutputs) = if (scanner != null) {
                scanner.scan(blockHashHex, outputs)
            } else {
                toRawEntities(blockHashHex, outputs) to emptyList()
            }

            mwebStorage.saveOutputs(rawOutputs)
            if (walletOutputs.isNotEmpty()) {
                mwebStorage.saveWalletOutputs(walletOutputs)
            }

            val maxLeaf = outputs.maxOf { it.leafIndex }
            mwebStorage.updateSyncState(maxLeaf)

            if (isFullResync) {
                outputs.forEach { raw ->
                    fullResyncOutputIds!!.add(raw.commitment.joinToString("") { b -> "%02x".format(b) })
                }
            }

            logger.info("MWEB: stored ${rawOutputs.size} outputs (${walletOutputs.size} owned), lastLeaf=$maxLeaf")

            if (outputs.size.toLong() >= GetMwebUtxosTask.PAGE_SIZE) {
                fetchNextPage(peer)
                return true
            }
        }

        // Final page reached — run spend detection if this was a full resync with data
        if (isFullResync && fullResyncOutputIds!!.isNotEmpty()) {
            val seenIds = fullResyncOutputIds!!
            val spentIds = mwebStorage.getUnspentWalletOutputIds().filter { it !in seenIds }
            if (spentIds.isNotEmpty()) {
                mwebStorage.markOutputsAsSpent(spentIds)
                logger.info("MWEB: marked ${spentIds.size} outputs as spent")
            }
            fullResyncOutputIds = null
            currentSyncStartIndex = -1L
        }

        return true
    }

    @Synchronized
    private fun fetchNextPage(peer: Peer) {
        val blockHash = lastBlockHashProvider() ?: return
        val startIndex = mwebStorage.getSyncState()?.let { it.lastLeafIndex + 1 } ?: 0L

        if (startIndex == 0L) {
            currentSyncStartIndex = 0L
            fullResyncOutputIds = mutableSetOf()
        }

        peer.addTask(GetMwebUtxosTask(blockHash, startIndex))
    }

    /** Builds [MwebOutput] entities without ownership scanning (watch-only wallets). */
    private fun toRawEntities(blockHashHex: String, outputs: List<MwebRawOutput>): List<MwebOutput> =
        outputs.map { raw ->
            MwebOutput(
                outputId = raw.commitment.joinToString("") { "%02x".format(it) },
                commitment = raw.commitment,
                senderPubKey = raw.senderPubKey,
                receiverPubKey = raw.receiverPubKey,
                features = raw.features,
                maskedValue = raw.maskedValue,
                maskedNonce = raw.maskedNonce,
                rangeProofSize = raw.rangeProof.size,
                leafIndex = raw.leafIndex,
                blockHash = blockHashHex
            )
        }
}
