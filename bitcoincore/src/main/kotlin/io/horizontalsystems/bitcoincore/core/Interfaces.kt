package io.horizontalsystems.bitcoincore.core

import io.horizontalsystems.bitcoincore.managers.TransactionItem
import io.horizontalsystems.bitcoincore.models.*
import io.horizontalsystems.bitcoincore.storage.FullTransaction
import io.horizontalsystems.bitcoincore.storage.FullTransactionInfo
import io.horizontalsystems.bitcoincore.storage.TransactionWithBlock
import io.horizontalsystems.bitcoincore.storage.UnspentOutput

interface IStorage {

    //  BlockchainState

    val initialRestored: Boolean?
    fun setInitialRestored(isRestored: Boolean)

    //  PeerAddress

    fun getLeastScoreFastestPeerAddressExcludingIps(ips: List<String>): PeerAddress?
    fun increasePeerAddressScore(ip: String)
    fun deletePeerAddress(ip: String)
    fun setPeerAddresses(list: List<PeerAddress>)
    fun setPeerConnectionTime(ip: String, time: Long)

    //  BlockHash

    fun getBlockHashesSortedBySequenceAndHeight(limit: Int): List<BlockHash>
    fun getBlockHashHeaderHashes(): List<ByteArray>
    fun getBlockHashHeaderHashes(except: ByteArray): List<ByteArray>
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
    fun deleteBlocksWithoutTransactions(toHeight: Int)
    fun unstaleAllBlocks()

    //  Transaction

    fun getFullTransactionInfo(transactions: List<TransactionWithBlock>): List<FullTransactionInfo>
    fun getFullTransactionInfo(fromTransaction: Transaction?, limit: Int?): List<FullTransactionInfo>
    fun getFullTransactionInfo(txHash: ByteArray): FullTransactionInfo?

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
    fun getMyOutputs(): List<TransactionOutput>
    fun getOutputsForBloomFilter(blockHeight: Int): List<TransactionOutput>

    // Transaction Input

    fun getTransactionInputs(transaction: Transaction): List<TransactionInput>
    fun getTransactionInputs(txHash: ByteArray): List<TransactionInput>

    // PublicKey

    fun getPublicKeyByScriptHashForP2PWKH(keyHash: ByteArray): PublicKey?
    fun getPublicKeyByKeyOrKeyHash(keyHash: ByteArray): PublicKey?

    fun getPublicKeys(): List<PublicKey>
    fun savePublicKeys(keys: List<PublicKey>)

    //  SentTransaction

    fun getSentTransaction(hash: ByteArray): SentTransaction?
    fun addSentTransaction(transaction: SentTransaction)
    fun updateSentTransaction(transaction: SentTransaction)
}

interface ITransactionInfoConverter {
    fun transactionInfo(transactionForInfo: FullTransactionInfo): TransactionInfo
}

interface IInitialSyncApi {
    fun getTransactions(addresses: List<String>): List<TransactionItem>
}

interface IAddressKeyHashConverter {
    fun convert(keyHash: ByteArray, type: Int): ByteArray
}
