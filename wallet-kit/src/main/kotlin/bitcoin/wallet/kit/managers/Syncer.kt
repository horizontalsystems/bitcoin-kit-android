package bitcoin.wallet.kit.managers

import bitcoin.wallet.kit.blocks.BlockSyncer
import bitcoin.wallet.kit.core.RealmFactory
import bitcoin.wallet.kit.headers.HeaderHandler
import bitcoin.wallet.kit.headers.HeaderSyncer
import bitcoin.wallet.kit.models.Header
import bitcoin.wallet.kit.models.InventoryItem
import bitcoin.wallet.kit.models.MerkleBlock
import bitcoin.wallet.kit.models.Transaction
import bitcoin.wallet.kit.network.NetworkParameters
import bitcoin.wallet.kit.network.PeerGroup
import bitcoin.wallet.kit.transactions.TransactionHandler
import bitcoin.wallet.kit.transactions.TransactionProcessor
import bitcoin.walllet.kit.io.BitcoinInput
import java.util.logging.Level
import java.util.logging.Logger

class Syncer(realmFactory: RealmFactory, peerGroup: PeerGroup, network: NetworkParameters) : PeerGroup.Listener {
    private val logger = Logger.getLogger("Syncer")
    private val headerSyncer = HeaderSyncer(realmFactory, peerGroup, network)
    private val headerHandler = HeaderHandler(realmFactory, network)
    private val blockSyncer = BlockSyncer(realmFactory, peerGroup)
    private val transactionHandler = TransactionHandler(realmFactory, TransactionProcessor(), ProgressSyncer())

    enum class SyncStatus {
        Syncing, Synced, Error
    }

    override fun onReady() {
        try {
            headerSyncer.sync()
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Header Syncer Error", e)
        }

        blockSyncer.enqueueRun()
    }

    override fun onReceiveHeaders(headers: Array<Header>) {
        headerHandler.handle(headers)
    }

    override fun onReceiveMerkleBlock(merkleBlock: MerkleBlock) {
        transactionHandler.handle(merkleBlock.associatedTransactions.toTypedArray(), merkleBlock.header)
    }

    override fun onReceiveTransaction(transaction: Transaction) {
//        TODO("not implemented")
    }

    override fun shouldRequest(inventory: InventoryItem): Boolean {
//        TODO("not implemented")
        return true
    }

    override fun getTransaction(hash: String): Transaction {
        return Transaction(BitcoinInput(byteArrayOf()))
    }
}
