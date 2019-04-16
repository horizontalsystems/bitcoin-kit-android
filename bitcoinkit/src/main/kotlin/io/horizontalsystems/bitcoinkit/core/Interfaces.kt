package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.models.*
import io.horizontalsystems.bitcoinkit.storage.FullTransaction
import io.horizontalsystems.bitcoinkit.storage.InputWithBlock
import io.horizontalsystems.bitcoinkit.storage.OutputWithPublicKey
import io.horizontalsystems.bitcoinkit.storage.UnspentOutput

interface IStorage {

    //  FeeRate

    val feeRate: FeeRate?
    fun setFeeRate(feeRate: FeeRate)

    //  BlockchainState

    val initialRestored: Boolean?
    fun setInitialRestored(isRestored: Boolean)

    //  PeerAddress

    fun getLeastScorePeerAddressExcludingIps(ips: List<String>): PeerAddress?
    fun getExistingPeerAddress(ips: List<String>): List<PeerAddress>
    fun increasePeerAddressScore(ip: String)
    fun deletePeerAddress(ip: String)
    fun setPeerAddresses(list: List<PeerAddress>)

    //  BlockHash

    fun getBlockHashesSortedBySequenceAndHeight(limit: Int): List<BlockHash>
    fun getBlockHashHeaderHashes(): List<ByteArray>
    fun getBlockHashHeaderHashHexes(except: String): List<String>
    fun getLastBlockHash(): BlockHash?

    fun getBlockchainBlockHashes(): List<BlockHash>
    fun getLastBlockchainBlockHash(): BlockHash?
    fun deleteBlockchainBlockHashes()
    fun deleteBlockHash(hashHex: String)
    fun addBlockHashes(hashes: List<BlockHash>)

    //  Block

    fun getBlock(height: Int): Block?
    fun getBlock(hashHex: String): Block?
    fun getBlock(stale: Boolean, sortedHeight: String): Block?

    fun getBlocks(stale: Boolean): List<Block>
    fun getBlocks(heightGreaterThan: Int, sortedBy: String, limit: Int): List<Block>
    fun getBlocks(heightGreaterOrEqualTo: Int, stale: Boolean): List<Block>
    fun getBlocks(hashHexes: List<String>): List<Block>
    fun getBlocksChunk(fromHeight: Int, toHeight: Int): List<Block>

    fun addBlock(block: Block)
    fun saveBlock(block: Block)

    fun blocksCount(headerHexes: List<String>? = null): Int
    fun lastBlock(): Block?
    fun updateBlock(staleBlock: Block)
    fun deleteBlocks(blocks: List<Block>)

    //  Transaction

    fun getTransactionsSortedTimestampAndOrdered(): List<Transaction>

    fun getTransaction(hashHex: String): Transaction?
    fun getTransactionOfOutput(output: TransactionOutput): Transaction?
    fun addTransaction(transaction: FullTransaction)
    fun updateTransaction(transaction: Transaction)
    fun getBlockTransactions(block: Block): List<Transaction>
    fun getNewTransactions(): List<FullTransaction>
    fun getNewTransaction(hashHex: String): Transaction?
    fun isTransactionExists(hash: ByteArray): Boolean

    //  Transaction Output

    fun getUnspentOutputs(): List<UnspentOutput>
    fun getPreviousOutput(input: TransactionInput): TransactionOutput?
    fun getTransactionOutputs(transaction: Transaction): List<TransactionOutput>
    fun getOutputsWithPublicKeys(): List<OutputWithPublicKey>
    fun getOutputsOfPublicKey(publicKey: PublicKey): List<TransactionOutput>

    // Transaction Input

    fun getInputsWithBlock(output: TransactionOutput): List<InputWithBlock>
    fun getTransactionInputs(transaction: Transaction): List<TransactionInput>

    // PublicKey

    fun getPublicKey(byPath: String): PublicKey?
    fun getPublicKeyByHash(keyHash: ByteArray, isWPKH: Boolean = false): PublicKey?
    fun getPublicKeys(): List<PublicKey>
    fun hasInputs(ofOutput: TransactionOutput): Boolean
    fun savePublicKeys(keys: List<PublicKey>)

    //  SentTransaction

    fun getSentTransaction(hashHex: String): SentTransaction?
    fun addSentTransaction(transaction: SentTransaction)
    fun updateSentTransaction(transaction: SentTransaction)

    fun clear()

}
