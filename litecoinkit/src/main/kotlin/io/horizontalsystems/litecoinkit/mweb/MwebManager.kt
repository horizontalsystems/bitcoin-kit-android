package io.horizontalsystems.litecoinkit.mweb

import io.horizontalsystems.bitcoincore.blocks.IPeerSyncListener
import io.horizontalsystems.bitcoincore.extensions.toReversedByteArray
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.task.PeerTask
import io.horizontalsystems.litecoinkit.mweb.network.messages.MwebRawOutput
import io.horizontalsystems.litecoinkit.mweb.network.tasks.GetMwebUtxosTask
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
 */
class MwebManager(
    private val mwebStorage: MwebStorage,
    private val scanner: MwebScanner?,
    private val lastBlockHashProvider: () -> ByteArray?
) : IPeerSyncListener, IPeerTaskHandler {

    private val logger = Logger.getLogger("MwebManager")

    // IPeerSyncListener
    override fun onPeerSynced(peer: Peer) {
        fetchNextPage(peer)
    }

    // IPeerTaskHandler
    override fun handleCompletedTask(peer: Peer, task: PeerTask): Boolean {
        if (task !is GetMwebUtxosTask) return false

        val outputs = task.receivedOutputs
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

            logger.info("MWEB: stored ${rawOutputs.size} outputs (${walletOutputs.size} owned), lastLeaf=$maxLeaf")

            if (outputs.size.toLong() >= GetMwebUtxosTask.PAGE_SIZE) {
                fetchNextPage(peer)
            }
        }

        return true
    }

    private fun fetchNextPage(peer: Peer) {
        val blockHash = lastBlockHashProvider() ?: return
        val startIndex = mwebStorage.getSyncState()?.let { it.lastLeafIndex + 1 } ?: 0L
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