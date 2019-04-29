package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.*

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
    fun getBlockHashHeaderHashes(except: ByteArray): List< ByteArray>
    fun getLastBlockHash(): BlockHash?

    fun getBlockchainBlockHashes(): List<BlockHash>
    fun getLastBlockchainBlockHash(): BlockHash?
    fun deleteBlockchainBlockHashes()
    fun deleteBlockHash(hash: ByteArray)
    fun addBlockHashes(hashes: List<BlockHash>)

    //  Block

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

    fun blocksCount(headerHashes: List<ByteArray>? = null): Int
    fun lastBlock(): Block?
    fun updateBlock(staleBlock: Block)
    fun deleteBlocks(blocks: List<Block>)

    //  Transaction

    fun getFullTransactionInfo(transactions: List<TransactionWithBlock>): List<FullTransactionInfo>
    fun getFullTransactionInfo(fromTransaction: Transaction?, limit: Int?): List<FullTransactionInfo>

    fun getTransaction(hash: ByteArray): Transaction?
    fun getTransactionOfOutput(output: TransactionOutput): Transaction?
    fun addTransaction(transaction: FullTransaction)
    fun updateTransaction(transaction: Transaction)
    fun getBlockTransactions(block: Block): List<Transaction>
    fun getNewTransactions(): List<FullTransaction>
    fun getNewTransaction(hash: ByteArray): Transaction?
    fun isTransactionExists(hash: ByteArray): Boolean

    //  Transaction Output

    fun getUnspentOutputs(): List<UnspentOutput>
    fun getPreviousOutput(input: TransactionInput): TransactionOutput?
    fun getTransactionOutputs(transaction: Transaction): List<TransactionOutput>
    fun getOutputsOfPublicKey(publicKey: PublicKey): List<TransactionOutput>
    fun getMyOutputs(): List<FullOutputInfo>

    // Transaction Input

    fun previousOutputWithPubKeyExists(input: TransactionInput): Boolean
    fun getTransactionInputs(transaction: Transaction): List<TransactionInput>

    // PublicKey

    fun getPublicKeyByHash(keyHash: ByteArray, isWPKH: Boolean = false): PublicKey?
    fun getPublicKeys(): List<PublicKey>
    fun savePublicKeys(keys: List<PublicKey>)

    //  SentTransaction

    fun getSentTransaction(hash: ByteArray): SentTransaction?
    fun addSentTransaction(transaction: SentTransaction)
    fun updateSentTransaction(transaction: SentTransaction)

    fun clear()

}
