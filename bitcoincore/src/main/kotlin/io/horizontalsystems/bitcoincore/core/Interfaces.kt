package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.apisync.model.TransactionItem
import io.horizontalsystems.bitcoincore.blocks.IPeerSyncListener
import io.horizontalsystems.bitcoincore.models.Block
import io.horizontalsystems.bitcoincore.models.BlockHash
import io.horizontalsystems.bitcoincore.models.BlockHashPublicKey
import io.horizontalsystems.bitcoincore.models.InvalidTransaction
import io.horizontalsystems.bitcoincore.models.PeerAddress
import io.horizontalsystems.bitcoincore.models.PublicKey
import io.horizontalsystems.bitcoincore.models.SentTransaction
import io.horizontalsystems.bitcoincore.models.Transaction
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.models.TransactionFilterType
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.TransactionInput
import io.horizontalsystems.bitcoincore.models.TransactionOutput
import io.horizontalsystems.bitcoincore.network.peer.IInventoryItemsHandler
import io.horizontalsystems.bitcoincore.network.peer.IPeerTaskHandler
import io.horizontalsystems.bitcoincore.network.peer.Peer
import io.horizontalsystems.bitcoincore.network.peer.PeerGroup
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo
import io.horizontalsystems.bitcoincore.storage.PublicKeyWithUsedState
import io.horizontalsystems.bitcoincore.storage.TransactionWithBlock
import io.horizontalsystems.bitcoincore.storage.UnspentOutput
import io.horizontalsystems.bitcoincore.transactions.builder.MutableTransaction
import io.horizontalsystems.bitcoincore.transactions.scripts.ScriptType
import io.horizontalsystems.hdwalletkit.HDKey

interface IStorage {

    //  BlockchainState

    val initialRestored: Boolean?
    fun setInitialRestored(isRestored: Boolean)

    //  PeerAddress

    fun getLeastScoreFastestPeerAddressExcludingIps(ips: List<String>): PeerAddress?
    fun deletePeerAddress(ip: String)
    fun setPeerAddresses(list: List<PeerAddress>)
    fun markConnected(ip: String, time: Long)

    //  BlockHash

    fun getBlockHashesSortedBySequenceAndHeight(limit: Int): List<BlockHash>
    fun getBlockHashHeaderHashes(): List<ByteArray>
    fun getBlockHashHeaderHashes(except: List<ByteArray>): List<ByteArray>
    fun getLastBlockHash(): BlockHash?

    fun getApiBlockHashesCount(): Int
    fun getBlockchainBlockHashes(): List<BlockHash>
    fun getLastBlockchainBlockHash(): BlockHash?
    fun deleteBlockchainBlockHashes()
    fun deleteBlockHash(hash: ByteArray)
    fun addBlockHashes(hashes: List<BlockHash>)
    fun addBockHashPublicKeys(blockHashPublicKeys: List<BlockHashPublicKey>)

    //  Block

    fun getBlockByHeightStalePrioritized(height: Int): Block?
    fun getBlock(height: Int): Block?
    fun getBlock(hashHash: ByteArray): Block?
    fun getBlock(stale: Boolean, sortedHeight: String): Block?

    fun getBlocks(stale: Boolean): List<Block>
    fun getBlocks(heightGreaterThan: Int, sortedBy: String, limit: Int): List<Block>
    fun getBlocks(heightGreaterOrEqualTo: Int, stale: Boolean): List<Block>
    fun getBlocks(hashes: List<ByteArray>): List<Block>
    fun getBlocksChunk(fromHeight: Int, toHeight: Int): List<Block>

    fun addBlock(block: Block)
    fun saveBlock(block: Block)
    fun setBlockPartial(headerHash: ByteArray)

    fun blocksCount(headerHashes: List<ByteArray>? = null): Int
    fun lastBlock(): Block?
    fun downloadedTransactionsBestBlockHeight(): Int
    fun updateBlock(staleBlock: Block)
    fun deleteBlocks(blocks: List<Block>)
    fun deleteBlocksWithoutTransactions(toHeight: Int)
    fun unstaleAllBlocks()
    fun timestamps(from: Int, to: Int): List<Long>

    //  Transaction

    fun getFullTransactionInfo(transactions: List<TransactionWithBlock>): List<FullTransactionInfo>
    fun getFullTransactionInfo(fromTransaction: Transaction?, type: TransactionFilterType?, limit: Int?): List<FullTransactionInfo>
    fun getFullTransactionInfo(txHash: ByteArray): FullTransactionInfo?

    fun getTransaction(hash: ByteArray): Transaction?
    fun getFullTransaction(hash: ByteArray): FullTransaction?
    fun getValidOrInvalidTransaction(uid: String): Transaction?
    fun getTransactionOfOutput(output: TransactionOutput): Transaction?
    fun addTransaction(transaction: FullTransaction)
    fun updateTransaction(transaction: Transaction)
    fun updateTransaction(transaction: FullTransaction)
    fun getBlockTransactions(block: Block): List<Transaction>
    fun getNewTransactions(): List<FullTransaction>
    fun getNewTransaction(hash: ByteArray): Transaction?
    fun isRelayedTransactionExists(hash: ByteArray): Boolean
    fun isTransactionExists(hash: ByteArray): Boolean
    fun getConflictingTransactions(transaction: FullTransaction): List<Transaction>
    fun getIncomingPendingTxHashes(): List<ByteArray>
    fun incomingPendingTransactionsExist(): Boolean

    // InvalidTransaction

    fun getInvalidTransaction(hash: ByteArray): InvalidTransaction?
    fun moveTransactionToInvalidTransactions(invalidTransactions: List<InvalidTransaction>)
    fun moveInvalidTransactionToTransactions(invalidTransaction: InvalidTransaction, toTransactions: FullTransaction)
    fun deleteAllInvalidTransactions()

    //  Transaction Output

    fun getUnspentOutputs(): List<UnspentOutput>
    fun getPreviousOutput(input: TransactionInput): TransactionOutput?
    fun getOutput(transactionHash: ByteArray, index: Int): TransactionOutput?
    fun getTransactionOutputs(transaction: Transaction): List<TransactionOutput>
    fun getOutputsOfPublicKey(publicKey: PublicKey): List<TransactionOutput>
    fun getMyOutputs(): List<TransactionOutput>
    fun getOutputsForBloomFilter(blockHeight: Int, irregularScriptTypes: List<ScriptType>): List<TransactionOutput>

    // Transaction Input

    fun getTransactionInputs(transaction: Transaction): List<TransactionInput>
    fun getTransactionInputs(txHash: ByteArray): List<TransactionInput>
    fun getTransactionInputs(txHashes: List<ByteArray>): List<TransactionInput>
    fun getTransactionInput(previousOutputTxHash: ByteArray, previousOutputIndex: Long): TransactionInput?
    fun getTransactionInputsByPrevOutputTxHash(txHash: ByteArray): List<TransactionInput>

    // PublicKey

    fun getPublicKeyByScriptHashForP2PWKH(keyHash: ByteArray): PublicKey?
    fun getPublicKeyByKeyOrKeyHash(keyHash: ByteArray): PublicKey?
    fun getPublicKeyByHashP2TR(hashP2TR: ByteArray): PublicKey?

    fun getPublicKeys(): List<PublicKey>
    fun getPublicKeysUsed(): List<PublicKey>
    fun getPublicKeysUnused(): List<PublicKey>
    fun getPublicKeysWithUsedState(): List<PublicKeyWithUsedState>
    fun savePublicKeys(keys: List<PublicKey>)

    //  SentTransaction

    fun getSentTransaction(hash: ByteArray): SentTransaction?
    fun addSentTransaction(transaction: SentTransaction)
    fun updateSentTransaction(transaction: SentTransaction)
    fun deleteSentTransaction(transaction: SentTransaction)
}

interface ITransactionInfoConverter {
    var baseConverter: BaseTransactionInfoConverter

    fun transactionInfo(fullTransactionInfo: FullTransactionInfo): TransactionInfo
}

interface IApiTransactionProvider {
    fun transactions(addresses: List<String>, stopHeight: Int?): List<TransactionItem>
}

interface IPeerAddressManager {
    val listener: IPeerAddressManagerListener?
    val hasFreshIps: Boolean
    fun getIp(): String?
    fun addIps(ips: List<String>)
    fun markFailed(ip: String)
    fun markSuccess(ip: String)
    fun markConnected(peer: Peer)
}

interface IPeerAddressManagerListener {
    fun onAddAddress()
}

interface IConnectionManager {
    val listener: IConnectionManagerListener?
    val isConnected: Boolean
    fun onEnterForeground()
    fun onEnterBackground()
}

interface IConnectionManagerListener {
    fun onConnectionChange(isConnected: Boolean)
}

interface IRecipientSetter {
    fun setRecipient(
        mutableTransaction: MutableTransaction,
        toAddress: String,
        value: Long,
        pluginData: Map<Byte, IPluginData>,
        skipChecking: Boolean
    )
}

interface ITransactionDataSorterFactory {
    fun sorter(type: TransactionDataSortType): ITransactionDataSorter
}

interface ITransactionDataSorter {
    fun sortOutputs(outputs: List<TransactionOutput>): List<TransactionOutput>
    fun sortUnspents(unspents: List<UnspentOutput>): List<UnspentOutput>
}

interface IKitStateListener {
    fun onKitStateUpdate(state: BitcoinCore.KitState)
}

interface IInitialDownload : IPeerTaskHandler, IInventoryItemsHandler, PeerGroup.Listener {
    var listener: IBlockSyncListener?
    val syncPeer: Peer?
    val syncedPeers: List<Peer>

    fun addPeerSyncListener(peerSyncListener: IPeerSyncListener)
}

interface IApiSyncer {
    var listener: IApiSyncerListener?
    val willSync: Boolean

    fun sync()
    fun syncLastBlock()
    fun terminate()
}

interface IApiSyncerListener {
    fun onSyncSuccess()
    fun onTransactionsFound(count: Int)
    fun onSyncFailed(error: Throwable)
}

interface IBlockSyncListener {
    fun onBlockSyncFinished()
    fun onCurrentBestBlockHeightUpdate(height: Int, maxBlockHeight: Int)
    fun onBlockForceAdded()
}

interface IPrivateWallet {
    fun privateKey(account: Int, index: Int, external: Boolean): HDKey
}

interface IAccountWallet {
    val gapLimit: Int

    fun publicKey(index: Int, external: Boolean): PublicKey
    fun publicKeys(indices: IntRange, external: Boolean): List<PublicKey>
}

interface IPublicKeyManager {
    fun changePublicKey(): PublicKey
    fun receivePublicKey(): PublicKey
    fun usedPublicKeys(): List<PublicKey>
    fun fillGap()
    fun addKeys(keys: List<PublicKey>)
    fun gapShifts(): Boolean
    fun getPublicKeyByPath(path: String): PublicKey
}
