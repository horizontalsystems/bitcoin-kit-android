package io.horizontalsystems.bitcoinkit.core

import io.horizontalsystems.bitcoinkit.models.*
import io.realm.Realm
import io.realm.RealmResults

interface IStorage {
    //  realm

    fun inTransaction(callback: (Realm) -> Unit)
    fun realmInstance(callback: (Realm) -> Unit)

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
    fun getBlock(headerHash: ByteArray): Block?
    fun getBlocks(heightGreaterThan: Int, sortedBy: String, limit: Int): List<Block>
    fun getBlocks(realm: Realm, hashHexes: List<String>): RealmResults<Block>
    fun blocksCount(headerHexes: List<String>? = null): Int
    fun saveBlock(block: Block)
    fun lastBlock(): Block?

    //  Transaction

    fun getNewTransactions(): List<Transaction>
    fun getNewTransaction(hashHex: String): Transaction?
    fun isTransactionExists(hash: ByteArray): Boolean

    //  SentTransaction

    fun getSentTransaction(hashHex: String): SentTransaction?
    fun addSentTransaction(transaction: SentTransaction)
    fun updateSentTransaction(transaction: SentTransaction)

    fun clear()
}
